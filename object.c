/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN
#include "header.h"

static PropertyMap *extend_property_map(Context *ctx, PropertyMap *prev,
                                        JSValue prop_name,  Attribute attr);
static void object_grow_shape(Context *ctx, JSValue obj, Shape *os);

/* Profiling */
#define HC_PROF_ENTER_SHAPE(os)
#define HC_PROF_LEAVE_SHAPE(os)

/* PROPERTY OPERATION **************************************************/

/**
 * obtains the index of a property of a JSObject
 * If the specified property does not exist, returns -1.
 *
 * Note that if hash_get_with_attribute returns HASH_GET_SUCCESS,
 * the property map of the object contains either the index
 * of JSValue array (as a fixnum) or the pointer to the next property map.
 * These two cases can be distinguished by investigating the ``transition
 * bit'' of the obtained attribute.  For the latter case, the pointer to
 * the next property map is returned through ``next_map'' parameter if
 * it is not NULL.
 */
static int prop_index(JSObject *p, JSValue name, Attribute *attrp,
                      PropertyMap **next_map)
{
  HashData retv;
  int result;
  Attribute attr;

  result = hash_get_with_attribute(p->shape->pm->map, name, &retv, &attr);
  if (result == HASH_GET_FAILED) {
    if (next_map != NULL)
      *next_map = NULL;
    return -1;
  }
  if (is_transition(attr)) {
    if (next_map != NULL)
      *next_map = (PropertyMap *) retv;
    return -1;
  } else {
    *attrp = attr;
    return (int) retv;
  }
}

/**
 * Initialise a pre-defined property
 * NOTE: Pre-defined properties should be registered in the map and
 *       object should be large enough for them to be embedded.
 *       Thus, no memory allocation takes place.
 */
static void init_prop(JSObject *p, JSValue name, JSValue value)
{
  Attribute attr;
  int index = prop_index(p, name, &attr, NULL);

  assert(index != -1);
  assert(index < p->shape->n_embedded_slots);

  p->eprop[index] = value;
}

/**
 * Set an object's property value with its attribute.
 * If the property is not defined in the object, it registers
 * the property to the property map.
 * set_prop fails if the object has a property of the same name
 * with a conflicting attribute.
 */
void set_prop_(Context *ctx, JSValue obj, JSValue name, JSValue v,
               Attribute att, int skip_setter)
{
  PropertyMap *next_pm;
  int index;
  Attribute attr;

  assert(is_jsobject(obj));

  if (!skip_setter) {
    /* __proto__ is stored in the dedicated field of property map */
    if (name == gconsts.g_string___proto__) {
      JSValue default___proto__ = object_get_shape(obj)->pm->__proto__;
      if (default___proto__ == v)
        return;
    }
  }

  index = prop_index(remove_jsobject_tag(obj), name, &attr, &next_pm);
  if (index == -1) {
    /* Current map does not have the property named `name'.
     * Remark: Hidden class related objects must be GC_PUSHed because
     *         pointers between them are regarded weak.
     * Note: Only properties added during initialisation has special
     *       attributes. Thus, now there is no risk of failure due to
     *       conflict of attribute. */
    Shape *current_os = object_get_shape(obj);
    Shape *next_os;
    GC_PUSH4(obj, name, v, current_os);
    /* 1. If there is not next property map, create it. */
    if (next_pm == NULL)
      next_pm = extend_property_map(ctx, current_os->pm, name, att);
    GC_PUSH(next_pm);
    /* 2. Find the shape that is compatible to the current shape. */
    for (next_os = next_pm->shapes; next_os != NULL; next_os = next_os->next)
      if (next_os->n_embedded_slots == current_os->n_embedded_slots &&
          next_os->n_extension_slots == current_os->n_extension_slots)
        break;
    /* 3. If there is not compatible shape, create it. */
    if (next_os == NULL) {
      size_t need_slots = next_pm->n_props;
      size_t n_embedded = current_os->n_embedded_slots;
      size_t n_extension = current_os->n_extension_slots;
      /* compute new size of extension array */
      if (n_embedded + n_extension - (n_extension == 0 ? 0 : 1) < need_slots)
        n_extension = need_slots - (n_embedded - 1);
      next_os = new_object_shape(ctx, DEBUG_NAME("(extend)"), next_pm,
                                 n_embedded, n_extension);
    }
    GC_PUSH(next_os);
    /* 4. Change the shape of object if necessary and installs the new shape.
     *    This may cause reallocation of the extension array. */
    object_grow_shape(ctx, obj, next_os);
    /* 5. Set `index'.  It should be equal to the number of properties
     *    registered to the previous property map.
     *    Note: The current property map may have extra properties to skip
     *          intermediate property map in the transition graph. */
    index = current_os->pm->n_props;
    GC_POP6(next_os, next_pm, current_os, v, name, obj);
  }

  object_set_prop(obj, index, v);
}

/**
 * Get property of the object. This does not follow the property chain.
 * If the object does not have the property, it returns JS_EMPTY.
 */
JSValue get_prop(JSValue obj, JSValue name)
{
  int index;
  Attribute attr;

  assert(is_jsobject(obj));

  if (name == gconsts.g_string___proto__) {
    JSValue __proto__ = object_get_shape(obj)->pm->__proto__;
    if (__proto__ != JS_EMPTY)
      return __proto__;
  }
  index = prop_index(remove_jsobject_tag(obj), name, &attr, NULL);
  if (index == -1 || is_system_prop(attr))
    return JS_EMPTY;
  return object_get_prop(obj, index);
}

static JSValue get_system_prop(JSValue obj, JSValue name)
{
  int index;
  Attribute attr;

  assert(is_jsobject(obj));

  index = prop_index(remove_jsobject_tag(obj), name, &attr, NULL);
  if (index == -1)
    return JS_EMPTY;
  assert(is_system_prop(attr));  /* or system property is overwritten */
  return object_get_prop(obj, index);
}

/**
 * Search for the property by following the prototype chain if necessary.
 * If the property is not defined in any object on the chain, it returns
 * JS_UNDEFINED.
 */
JSValue get_prop_prototype_chain(JSValue obj, JSValue name)
{
  while (is_jsobject(obj)) {
    JSValue ret;
    ret = get_prop(obj, name);
    if (ret != JS_EMPTY)
      return ret;
    obj = get_prop(obj, gconsts.g_string___proto__);
  }
  return JS_UNDEFINED;
}

/* OBJECT CONSTRUCTOR **************************************************/

/**
 * Allocate memory for JSObject.
 * Initialise common and property fields of JSObject.
 */
static JSObject *allocate_jsobject(Context *ctx, char *name, Shape *os,
                                   cell_type_t htag)
{
  size_t n_embedded = os->n_embedded_slots;
  size_t size =
    sizeof(JSObject) + sizeof(JSValue) * (n_embedded - JSOBJECT_MIN_EMBEDDED);
  JSObject *p = (JSObject *) gc_malloc(ctx, size, htag);
  int i;

  p->shape = os;
#ifdef ALLOC_SITE_CACHE
  p->alloc_site = NULL;
#endif /* ALLOC_SITE_CACHE */
  for (i = 0; i < n_embedded; i++)
    p->eprop[i] = JS_UNDEFINED;

#ifdef DEBUG
  p->name = name;
#endif /* DEBUG */

  return p;
}

JSValue new_simple_object(Context *ctx, char *name, Shape *os)
{
  JSObject *p;

  assert(os->pm->n_special_props = OBJECT_SPECIAL_PROPS);

  p = allocate_jsobject(ctx, name, os, HTAG_SIMPLE_OBJECT);

  assert(Object_num_builtin_props +
         Object_num_double_props + Object_num_gconsts_props == 0);

  return put_simple_object_tag(p);
}

JSValue new_array_object(Context *ctx, char *name, Shape *os, size_t size)
{
  JSObject *p;
  JSValue *array_data;
  int i;

  assert(os->pm->n_special_props == ARRAY_SPECIAL_PROPS);


  p = allocate_jsobject(ctx, name, os, HTAG_ARRAY);
  array_ptr_body(p) = NULL;  /* tell GC not to follow this pointer */

  GC_PUSH(p);
  array_data =
    (JSValue *) gc_malloc(ctx, size * sizeof(JSValue), HTAG_ARRAY_DATA);
  GC_POP(p);
  for (i = 0; i < size; i++)
    array_data[i] = JS_UNDEFINED;

  array_ptr_body(p)   = array_data;
  array_ptr_size(p)   = size;
  array_ptr_length(p) = size;

  assert(Array_num_builtin_props +
         Array_num_double_props + Array_num_gconsts_props == 1);
  init_prop(p, gconsts.g_string_length, int_to_fixnum(size));

  return put_array_tag(p);
}

JSValue new_function_object(Context *ctx, char *name, Shape *os, int ft_index)
{
  JSObject *p;
  JSValue prototype;

  assert(os->pm->n_special_props == FUNCTION_SPECIAL_PROPS);

  prototype = new_simple_object(ctx, DEBUG_NAME("(prototype)"),
                                gconsts.g_shape_Object);

  GC_PUSH(prototype);
  p = allocate_jsobject(ctx, name, os, HTAG_FUNCTION);
  GC_POP(prototype);

  function_ptr_table_entry(p) = &(ctx->function_table[ft_index]);
  function_ptr_environment(p) = get_lp(ctx);

  assert(Function_num_builtin_props +
         Function_num_double_props + Function_num_gconsts_props == 1);
  init_prop(p, gconsts.g_string_prototype, prototype);

  return put_function_tag(p);
}

JSValue new_builtin_object(Context *ctx, char *name, Shape *os,
                           builtin_function_t cfun, builtin_function_t cctor,
                           int na)
{
  JSObject *p;

  assert(os->pm->n_special_props == BUILTIN_SPECIAL_PROPS);

  p = allocate_jsobject(ctx, name, os, HTAG_BUILTIN);

  builtin_ptr_body(p)        = cfun;
  builtin_ptr_constructor(p) = cctor;
  builtin_ptr_n_args(p)      = na;

  assert(Builtin_num_builtin_props +
         Builtin_num_double_props + Builtin_num_gconsts_props == 0);

  return put_builtin_tag(p);
}

JSValue new_number_object(Context *ctx, char *name, Shape *os, JSValue v)
{
  JSObject *p;

  assert(os->pm->n_special_props == NUMBER_SPECIAL_PROPS);
  assert(is_fixnum(v) || is_flonum(v));

  GC_PUSH(v);
  p = allocate_jsobject(ctx, name, os, HTAG_BOXED_NUMBER);
  GC_POP(v);

  number_object_ptr_value(p) = v;

  assert(Number_num_builtin_props +
         Number_num_double_props + Number_num_gconsts_props == 0);

  return put_number_object_tag(p);
}

JSValue new_string_object(Context *ctx, char *name, Shape *os, JSValue v)
{
  JSObject *p;

  assert(os->pm->n_special_props == STRING_SPECIAL_PROPS);
  assert(is_string(v));

  GC_PUSH(v);
  p = allocate_jsobject(ctx, name, os, HTAG_BOXED_STRING);
  GC_POP(v);

  string_object_ptr_value(p) = v;

  assert(String_num_builtin_props +
         String_num_double_props + String_num_gconsts_props == 1);
  init_prop(p, gconsts.g_string_length, string_length(v));

  return put_string_object_tag(p);
}


JSValue new_boolean_object(Context *ctx, char *name, Shape *os, JSValue v)
{
  JSObject *p;

  assert(os->pm->n_special_props == STRING_SPECIAL_PROPS);
  assert(is_boolean(v));

  GC_PUSH(v);
  p = allocate_jsobject(ctx, name, os, HTAG_BOXED_BOOLEAN);
  GC_POP(v);

  boolean_object_ptr_value(p) = v;

  return put_boolean_object_tag(p);
}

#ifdef USE_REGEXP
JSValue new_regexp_object(Context *ctx, char *name, char *pat, int flag)
{
  JSObject *p;

  assert(os->pm->n_special_props == REGEXP_NUM_SPECIAL);

  p = allocate_jsobject(ctx, name, os, HTAG_REGEXP);

  /* pattern field is set in set_regexp_members */
  regexp_reg(p) = NULL;
  regexp_global(p) = false;
  regexp_ignorecase(p) = false;
  regexp_multiline(p) = false;
  regexp_lastindex(p) = 0;

  TODO TODO  TODO TODO  TODO TODO  TODO TODO  TODO TODO  TODO TODO
  
  return
    (set_regexp_members(ctx, ret, pat, flag) == SUCCESS)? ret: JS_UNDEFINED;

}
#endif /* USE_REGEXP */

/* HIDDEN CLASS *******************************************************/

PropertyMap *new_property_map(Context *ctx, char *name,
                              int n_special_props, int n_props,
                              JSValue __proto__, PropertyMap *prev)
{
  HashTable   *hash;
  PropertyMap *m;

  GC_PUSH2(__proto__, prev);
  hash = malloc_hashtable(ctx);
  GC_PUSH(hash);
  hash_create(ctx, hash, n_props - n_special_props);
  m = (PropertyMap *) gc_malloc(ctx, sizeof(PropertyMap), HTAG_PROPERTY_MAP);
  GC_POP3(hash, prev, __proto__);

  m->map       = hash;
  m->prev      = prev;
  m->shapes    = NULL;
  m->__proto__ = __proto__;
  m->n_props   = n_props;
  m->n_special_props = n_special_props;

#ifdef DEBUG
  m->name = name;
#endif /* DEBUG */

  return m;
}

/**
 * Create a new property map by extending an exispting property map
 * with a new property name. The index for the new property is the
 * next number to the largest used one.
 * Then, set up edges of the transition graph.
 */
static PropertyMap *extend_property_map(Context *ctx, PropertyMap *prev,
                                        JSValue prop_name,  Attribute attr)
{
  PropertyMap *m;
  int index = prev->n_props;
  JSValue __proto__;

  GC_PUSH2(prev, prop_name);

  /* 1. Create property map */
  if (prop_name == gconsts.g_string___proto__)
    __proto__ = JS_EMPTY;
  else
    __proto__ = prev->__proto__;
  m = new_property_map(ctx, DEBUG_NAME("(extended)"),
                       prev->n_special_props, prev->n_props + 1,
                       __proto__, prev);
  GC_PUSH(m);

  /* 2. Copy existing entries */
  hash_copy(ctx, prev->map, m->map);

  /* 3. Add property */
  hash_put_with_attribute(ctx, m->map, prop_name, index, attr);

  /* 4. Create an edge from prev to new property map. */
  hash_put_with_attribute(ctx, prev->map, prop_name, (HashData) m,
                          ATTR_NONE | ATTR_TRANSITION);

  GC_POP3(m, prop_name, prev);

  return m;
}

Shape *new_object_shape(Context *ctx, char *name, PropertyMap *pm,
                        int num_embedded, int num_extension)
{
  Shape *s;
  Shape **pp;

  s = (Shape *) gc_malloc(ctx, sizeof(Shape), HTAG_SHAPE);
  s->pm = pm;
  s->n_embedded_slots  = num_embedded;
  s->n_extension_slots = num_extension;

  /* Insert `s' into the `shapes' list of the property map.
   * The list is sorted from more `n_embedded_slots' to less.
   */
  for (pp = &pm->shapes; ; pp = &(*pp)->next) {
    Shape *p = *pp;
    if (p == NULL || p->n_embedded_slots < num_embedded) {
      *pp = s;
      break;
    }
  }

#ifdef DEBUG
  s->name = name;
#endif /* DEBUG */

  return s;
}

/**
 * Reallocate extension array if necessary.
 * Assign new shape to the object.
 */
static void object_grow_shape(Context *ctx, JSValue obj, Shape *os)
{
  size_t current_size, new_size;
  int extension_index;
  JSObject *p;

  p = remove_jsobject_tag(obj);

  GC_PUSH2(p, os);
  HC_PROF_LEAVE_SHAPE(p->shape);

  current_size = p->shape->n_extension_slots;
  new_size = os->n_extension_slots;
  extension_index = os->n_embedded_slots - 1;

  /* 1. Reallocate extension array if necessary. */
  if (current_size < new_size) {
    JSValue *extension;
    int i;
    extension = (JSValue *) gc_malloc(ctx, sizeof(JSValue) * new_size,
                                      HTAG_PROP);
    if (current_size == 0) {
      extension[0] = p->eprop[extension_index];
      i = 1;
    } else {
      JSValue *current_extension = (JSValue *) p->eprop[extension_index];
      for (i = 0; i < current_size; i++)
        extension[i] = current_extension[i];
    }
    for (; i < new_size; i++)
      extension[i] = JS_UNDEFINED;
    p->eprop[extension_index] = (JSValue) extension;
  }

  /* 2. Assign new shape */
  p->shape = os;

  HC_PROF_ENTER_SHAPE(os);
  GC_POP2(os, p);
}

/**
 * The most normal way to create an object.
 * Called from ``new'' instruction.
 */
JSValue create_simple_object_with_constructor(Context *ctx, JSValue ctor)
{
  JSValue prototype, obj;
  Shape *os;

  assert(is_function(ctor));

  prototype = get_prop(ctor, gconsts.g_string_prototype);
  if (is_jsobject(prototype)) {
    JSValue retv;
    PropertyMap *pm;
    /* 1. If `prototype' is valid, find the property map */
    retv = get_system_prop(prototype, gconsts.g_string___property_map__);
    if (retv != JS_EMPTY)
      pm = (PropertyMap *) retv;
    else {
      /* 2. If there is not, create it. */
      int n_props = 0;
      int n_embedded = OBJECT_SPECIAL_PROPS + 1; /* at least 1 normal slot */
      GC_PUSH(prototype);
      pm = new_property_map(ctx, DEBUG_NAME("(new_prototype)"),
                            OBJECT_SPECIAL_PROPS, n_props, prototype,
                            gconsts.g_property_map_root);
      GC_PUSH(pm);
      pm->shapes = new_object_shape(ctx, DEBUG_NAME("(new_prototype)"),
                                    pm, n_embedded, 0);
      assert(Object_num_builtin_props +
             Object_num_double_props + Object_num_gconsts_props == 0);

      /* 3. Create a link from the prototype object to the PM so that
       *    this function can find it in the following calls. */
      set_prop(ctx, prototype, gconsts.g_string___property_map__,
               (JSValue) pm, ATTR_SYSTEM);
      GC_POP2(pm, prototype);
    }
    /* 4. Obtain the shape of the PM. There should be a single shape, if any,
     *    because the PM is an entrypoint. */
    os = pm->shapes;
  } else
    os = gconsts.g_shape_Object;

  obj = new_simple_object(ctx, DEBUG_NAME("inst:new"), os);

  return obj;
}

#ifdef ALLOC_SITE_CACHE
void init_alloc_site(AllocSite *alloc_site)
{
  alloc_site->hc = NULL;
  alloc_site->preformed_hc = NULL;
  alloc_site->next_affected = NULL;
  alloc_site->polymorphic = 0;
}
#endif /* ALLOC_SITE_CACHE */

#include "object-compat.c"

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
