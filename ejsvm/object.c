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
static void property_map_add_transition(Context *ctx, PropertyMap *pm,
                                        JSValue name, PropertyMap *dest);
static void object_grow_shape(Context *ctx, JSValue obj, Shape *os);
#ifdef HC_PROF
static void hcprof_add_root_property_map(PropertyMap *pm);
#endif /* HC_PROF */

/* Profiling */
#ifdef HC_PROF
static void hcprof_enter_shape(Shape *os)
{
  PropertyMap *pm = os->pm;
  pm->n_enter++;
}

static void hcprof_leave_shape(Shape *os)
{
  PropertyMap *pm = os->pm;
  pm->n_leave++;
}

#define HC_PROF_ENTER_SHAPE(os) hcprof_enter_shape(os)
#define HC_PROF_LEAVE_SHAPE(os) hcprof_leave_shape(os)
#else /* HC_PROF */
#define HC_PROF_ENTER_SHAPE(os)
#define HC_PROF_LEAVE_SHAPE(os)
#endif /* HC_PROF */

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
      *next_map = retv.u.pm;
    return -1;
  } else {
    *attrp = attr;
    return retv.u.index;
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
#ifdef INLINE_CACHE
void set_prop_(Context *ctx, JSValue obj, JSValue name, JSValue v,
               Attribute att, int skip_setter, InlineCache *ic)
#else /* INLINE_CACHE */
void set_prop_(Context *ctx, JSValue obj, JSValue name, JSValue v,
               Attribute att, int skip_setter)
#endif /* INLINE_CACHE */
{
  PropertyMap *next_pm;
  int index;
  Attribute attr;

#ifdef VERBOSE_SET_PROP
#define PRINT(x...) printf(x)
#else /* VERBOSE_SET_PROP */
#define PRINT(x...)
#endif /* VERBOSE_SET_PROP */

  assert(is_jsobject(obj));
  assert(is_string(name));

  PRINT("set_prop shape %p PM %p prop %s (%llx) %s\n",
        object_get_shape(obj), object_get_shape(obj)->pm,
        string_to_cstr(name), name, skip_setter ? "(skip setter)" : "");

  if (!skip_setter) {
    /* __proto__ is stored in the dedicated field of property map */
    if (name == gconsts.g_string___proto__) {
      JSValue default___proto__ = object_get_shape(obj)->pm->__proto__;
      if (default___proto__ == v)
        return;
    }
  }

  index = prop_index(jsv_to_jsobject(obj), name, &attr, &next_pm);
  if (index == -1) {
    /* Current map does not have the property named `name'.
     * Remark: Hidden class related objects must be GC_PUSHed because
     *         pointers between them are regarded weak.
     * Note: Only properties added during initialisation has special
     *       attributes. Thus, now there is no risk of failure due to
     *       conflict of attribute. */
    Shape *current_os = object_get_shape(obj);
    Shape *next_os;
    size_t need_slots, n_embedded, n_extension;

    GC_PUSH4(obj, name, v, current_os);
    /* 1. If there is not next property map, create it. */
    if (next_pm == NULL) {
      next_pm = extend_property_map(ctx, current_os->pm, name, att);
      PRINT("  new property (new PM %p is created)\n", next_pm);
    } else
      PRINT("  new property (cached PM %p is used)\n", next_pm);
    GC_PUSH(next_pm);

    /* 2. Find the shape that is compatible to the current shape. */
    need_slots = next_pm->n_props;
    n_embedded = current_os->n_embedded_slots;
    n_extension = current_os->n_extension_slots;
    /* compute new size of extension array */
    if (n_embedded + n_extension - (n_extension == 0 ? 0 : 1) < need_slots)
      n_extension = need_slots - (n_embedded - 1);
    PRINT("  finding shape for PM %p (n_props = %d) EM/EX %lu %lu\n",
          next_pm, next_pm->n_props, n_embedded, n_extension);
    for (next_os = next_pm->shapes; next_os != NULL; next_os = next_os->next)
      if (next_os->n_embedded_slots == n_embedded &&
          next_os->n_extension_slots == n_extension) {
        PRINT("    found: %p\n", next_os);
        break;
      } else
        PRINT("    not the one %p: EM/EX %d %d\n",
              next_os, next_os->n_embedded_slots, next_os->n_extension_slots);

    /* 3. If there is not compatible shape, create it. */
    if (next_os == NULL) {
      next_os = new_object_shape(ctx, DEBUG_NAME("(extend)"), next_pm,
                                 n_embedded, n_extension);
      PRINT("  create new shape %p EM/EX %lu %lu\n",
            next_os, n_embedded, n_extension);
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

#ifdef INLINE_CACHE
  if (ic != NULL && ic->shape == NULL &&
      object_get_shape(obj)->n_extension_slots == 0) {
    ic->shape = object_get_shape(obj);
    ic->prop_name = name;
    ic->index = index;
  }
#endif /* INLINE_CACHE */
  object_set_prop(obj, index, v);

#undef PRINT  /* VERBOSE_SET_PROP */
}

/**
 * Get property of the object. This does not follow the property chain.
 * If the object does not have the property, it returns JS_EMPTY.
 */
#ifdef INLINE_CACHE
JSValue get_prop_with_ic(JSValue obj, JSValue name, InlineCache *ic)
#else /* INLINE_CACHE */
JSValue get_prop(JSValue obj, JSValue name)
#endif /* INLINE_CACHE */
{
  int index;
  Attribute attr;

  assert(is_jsobject(obj));

  if (name == gconsts.g_string___proto__) {
    JSValue __proto__ = object_get_shape(obj)->pm->__proto__;
    if (__proto__ != JS_EMPTY)
      return __proto__;
  }
  index = prop_index(jsv_to_jsobject(obj), name, &attr, NULL);
  if (index == -1 || is_system_prop(attr))
    return JS_EMPTY;
#ifdef INLINE_CACHE
  if (ic != NULL && ic->shape == NULL &&
      object_get_shape(obj)->n_extension_slots == 0) {
    ic->shape = object_get_shape(obj);
    ic->prop_name = name;
    ic->index = index;
  }
#endif /* INLINE_CACHE */
  return object_get_prop(obj, index);
}

static JSValue get_system_prop(JSValue obj, JSValue name)
{
  int index;
  Attribute attr;

  assert(is_jsobject(obj));

  index = prop_index(jsv_to_jsobject(obj), name, &attr, NULL);
  if (index == -1)
    return JS_EMPTY;
  assert(is_system_prop(attr));  /* or system property is overwritten */
  return object_get_prop(obj, index);
}

static JSValue get_array_element_no_proto(JSValue array, cint index)
{
  assert(is_array(array));
  if (0 <= index &&
      index < number_to_double(get_jsarray_length(array)) &&
      index < get_jsarray_size(array))
    return get_jsarray_body(array)[index];
  return JS_EMPTY;
}

/**
 * Search for the property by following the prototype chain if necessary.
 * If the property is not defined in any object on the chain, it returns
 * JS_UNDEFINED.
 */
#ifdef INLINE_CACHE
JSValue get_prop_prototype_chain_with_ic(JSValue obj, JSValue name,
                                         InlineCache *ic)
#else /* INLINE_CACHE */
JSValue get_prop_prototype_chain(JSValue obj, JSValue name)
#endif /* INLINE_CACHE */
{

#ifdef INLINE_CACHE
  if (is_object(obj)) {
    JSValue ret = get_prop_with_ic(obj, name, ic);
    if (ret != JS_EMPTY)
      return ret;
    obj = get_prop(obj, gconsts.g_string___proto__);
  }
#endif /* INLINE_CACHE */
  while (is_jsobject(obj)) {
    JSValue ret = get_prop(obj, name);
    if (ret != JS_EMPTY)
      return ret;
    if (is_array(obj)) {
      JSValue num = string_to_number(NULL, name);
      if (is_fixnum(num))
        ret = get_array_element_no_proto(obj, fixnum_to_cint(num));
      if (ret != JS_EMPTY)
        return ret;
    }
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
                                   HTag htag)
{
  size_t n_embedded = os->n_embedded_slots;
  size_t size = sizeof(JSObject) + sizeof(JSValue) * n_embedded;
  JSObject *p = (JSObject *) gc_malloc(ctx, size, htag.v);
  int i;

  p->shape = os;
#ifdef ALLOC_SITE_CACHE
  p->alloc_site = NULL;
#endif /* ALLOC_SITE_CACHE */
  for (i = 0; i < n_embedded; i++)
    p->eprop[i] = JS_EMPTY;

#ifdef DEBUG
  p->name = name;
#endif /* DEBUG */

  HC_PROF_ENTER_SHAPE(os);

  return p;
}

JSValue new_simple_object(Context *ctx, char *name, Shape *os)
{
  JSObject *p;

  assert(os->pm->n_special_props == OBJECT_SPECIAL_PROPS);

  p = allocate_jsobject(ctx, name, os, HTAG_SIMPLE_OBJECT);

  assert(Object_num_builtin_props +
         Object_num_double_props + Object_num_gconsts_props == 0);

  return ptr_to_normal_simple_object(p);
}

JSValue new_array_object(Context *ctx, char *name, Shape *os, size_t size)
{
  JSObject *p;
  JSValue *array_data;
  int i;

  assert(os->pm->n_special_props == ARRAY_SPECIAL_PROPS);

  p = allocate_jsobject(ctx, name, os, HTAG_ARRAY);
  set_array_ptr_body(p, NULL);  /* tell GC not to follow this pointer */

  GC_PUSH(p);
  array_data =
    (JSValue *) gc_malloc(ctx, size * sizeof(JSValue), CELLT_ARRAY_DATA);
  GC_POP(p);
  for (i = 0; i < size; i++)
    array_data[i] = JS_UNDEFINED;

  set_array_ptr_body(p, array_data);
  set_array_ptr_size(p, size);
  set_array_ptr_length(p, cint_to_number(ctx, (cint) size));

  assert(Array_num_builtin_props +
         Array_num_double_props + Array_num_gconsts_props == 1);

  return ptr_to_normal_array(p);
}


JSValue new_function_object(Context *ctx, char *name, Shape *os, int ft_index)
{
  JSObject *p;
  JSValue prototype;

  assert(os->pm->n_special_props == FUNCTION_SPECIAL_PROPS);

  prototype = new_simple_object(ctx, DEBUG_NAME("(prototype)"),
                                gshapes.g_shape_Object);

  GC_PUSH(prototype);
  p = allocate_jsobject(ctx, name, os, HTAG_FUNCTION);
  GC_POP(prototype);

  set_function_ptr_table_entry(p, &(ctx->function_table[ft_index]));
  set_function_ptr_environment(p, get_lp(ctx));

  assert(Function_num_builtin_props +
         Function_num_double_props + Function_num_gconsts_props == 1);
  init_prop(p, gconsts.g_string_prototype, prototype);

  return ptr_to_normal_function(p);
}

JSValue new_builtin_object(Context *ctx, char *name, Shape *os,
                           builtin_function_t cfun, builtin_function_t cctor,
                           int na)
{
  JSObject *p;

  assert(os->pm->n_special_props == BUILTIN_SPECIAL_PROPS);

  p = allocate_jsobject(ctx, name, os, HTAG_BUILTIN);

  set_builtin_ptr_body(p, cfun);
  set_builtin_ptr_constructor(p, cctor);
  set_builtin_ptr_nargs(p, na);

  assert(Builtin_num_builtin_props +
         Builtin_num_double_props + Builtin_num_gconsts_props == 0);

  return ptr_to_normal_builtin(p);
}

JSValue new_number_object(Context *ctx, char *name, Shape *os, JSValue v)
{
  JSObject *p;

  assert(os->pm->n_special_props == NUMBER_SPECIAL_PROPS);
  assert(is_fixnum(v) || is_flonum(v));

  GC_PUSH(v);
  p = allocate_jsobject(ctx, name, os, HTAG_BOXED_NUMBER);
  GC_POP(v);

  set_number_object_ptr_value(p, v);

  assert(Number_num_builtin_props +
         Number_num_double_props + Number_num_gconsts_props == 0);

  return ptr_to_normal_number_object(p);
}

JSValue new_string_object(Context *ctx, char *name, Shape *os, JSValue v)
{
  JSObject *p;

  assert(os->pm->n_special_props == STRING_SPECIAL_PROPS);
  assert(is_string(v));

  GC_PUSH(v);
  p = allocate_jsobject(ctx, name, os, HTAG_BOXED_STRING);
  GC_POP(v);

  set_string_object_ptr_value(p, v);

  assert(String_num_builtin_props +
         String_num_double_props + String_num_gconsts_props == 1);
  init_prop(p, gconsts.g_string_length,
            uint32_to_number(ctx, string_length(v)));

  return ptr_to_normal_string_object(p);
}


JSValue new_boolean_object(Context *ctx, char *name, Shape *os, JSValue v)
{
  JSObject *p;

  assert(os->pm->n_special_props == STRING_SPECIAL_PROPS);
  assert(is_boolean(v));

  GC_PUSH(v);
  p = allocate_jsobject(ctx, name, os, HTAG_BOXED_BOOLEAN);
  GC_POP(v);

  set_boolean_object_ptr_value(p, v);

  return ptr_to_normal_boolean_object(p);
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
                              int n_user_special_props,
                              JSValue __proto__, PropertyMap *prev)
{
  HashTable   *hash;
  PropertyMap *m;

  assert(ctx != NULL);

  GC_PUSH2(__proto__, prev);
  hash = hash_create(ctx, n_props - n_special_props);
  GC_PUSH(hash);
  m = (PropertyMap *) gc_malloc(ctx, sizeof(PropertyMap), CELLT_PROPERTY_MAP);
  GC_POP3(hash, prev, __proto__);

  m->map       = hash;
  m->prev      = prev;
  m->shapes    = NULL;
  m->__proto__ = __proto__;
  m->n_props   = n_props;
  m->n_special_props = n_special_props;
#ifdef HC_SKIP_INTERNAL
  m->n_transitions = 0;
#endif /* HC_SKIP_INTERNAL */

#ifdef DEBUG
  m->name = name;
  m->n_user_special_props = n_user_special_props;
#endif /* DEBUG */
#ifdef HC_PROF
  m->n_enter = 0;
  m->n_leave = 0;
  if (prev == gpms.g_property_map_root)
    hcprof_add_root_property_map(m);
#endif /* HC_PROF */

#ifdef HC_SKIP_INTERNAL
  GC_PUSH(m);
  /* avoid registration of the root map, whose prev is NULL.
   * Note gpms.g_property_map_root is NULL before the root map is created.
   */
  if (prev != NULL && prev == gpms.g_property_map_root) {
    PropertyMapList *p =
      (PropertyMapList*) gc_malloc(ctx, sizeof(PropertyMapList),
                                   CELLT_PROPERTY_MAP_LIST);
    p->pm = m;
    p->next = ctx->property_map_roots;
    ctx->property_map_roots = p;
  }
  GC_POP(m);
#endif /* HC_SKIP_INTERNAL */
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
#ifdef DEBUG
                       prev->n_user_special_props,
#else /* DEBUG */
                       0,
#endif /* DEBUG */
                       __proto__, prev);
  GC_PUSH(m);

  /* 2. Copy existing entries */
#ifdef DEBUG
  {
    int n = hash_copy(ctx, prev->map, m->map);
    assert(n == prev->n_props - prev->n_special_props +
           prev->n_user_special_props);
  }
#else /* DEBUG */
  hash_copy(ctx, prev->map, m->map);
#endif /* DEBUG */

  /* 3. Add property */
  property_map_add_property_entry(ctx, m, prop_name, index, attr);
 
  /* 4. Create an edge from prev to new property map. */
  property_map_add_transition(ctx, prev, prop_name, m);

  GC_POP3(m, prop_name, prev);

  return m;
}

void property_map_add_property_entry(Context *ctx, PropertyMap *pm,
                                     JSValue name, int index, Attribute attr)
{
  HashData data;
  data.u.index = index;
  hash_put_with_attribute(ctx, pm->map, name, data, attr);
}

static void property_map_add_transition(Context *ctx, PropertyMap *pm,
                                        JSValue name, PropertyMap *dest)
{
  HashData data;
  data.u.pm = dest;
#ifdef HC_SKIP_INTERNAL
  {
    uint16_t current_n_trans = pm->n_transitions;
    pm->n_transitions = PM_N_TRANS_UNSURE;
    hash_put_with_attribute(ctx, pm->map, name, data,
                            ATTR_NONE | ATTR_TRANSITION);
    pm->n_transitions = current_n_trans + 1;
  }
#else /* HC_SKIP_INTERNAL */
  hash_put_with_attribute(ctx, pm->map, name, data,
                          ATTR_NONE | ATTR_TRANSITION);
#endif /* HC_SKIP_INTERNAL */
}

Shape *new_object_shape(Context *ctx, char *name, PropertyMap *pm,
                        int num_embedded, int num_extension)
{
  Shape *s;
  Shape **pp;

  assert(num_embedded > 0);

  s = (Shape *) gc_malloc(ctx, sizeof(Shape), CELLT_SHAPE);
  s->pm = pm;
  s->n_embedded_slots  = num_embedded;
  s->n_extension_slots = num_extension;

  /* Insert `s' into the `shapes' list of the property map.
   * The list is sorted from more `n_embedded_slots' to less.
   */
  for (pp = &pm->shapes; ; pp = &(*pp)->next) {
    Shape *p = *pp;
    if (p == NULL || p->n_embedded_slots < num_embedded) {
      s->next = *pp;
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

  p = jsv_to_jsobject(obj);

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
                                      CELLT_PROP);
    if (current_size == 0) {
      extension[0] = p->eprop[extension_index];
      i = 1;
    } else {
      JSValue *current_extension =
        jsv_to_extension_prop(p->eprop[extension_index]);
      for (i = 0; i < current_size; i++)
        extension[i] = current_extension[i];
    }
    for (; i < new_size; i++)
      extension[i] = JS_UNDEFINED;
    p->eprop[extension_index] = (JSValue) (uintjsv_t) (uintptr_t) extension;
  }

  /* 2. Assign new shape */
  p->shape = os;

  HC_PROF_ENTER_SHAPE(os);
  GC_POP2(os, p);
}


#ifdef ALLOC_SITE_CACHE
static Shape *get_cached_shape(Context *ctx, AllocSite *as,
                               JSValue __proto__, int n_special)
{
  /* 1. check if cache is available and compatible */
  if (as != NULL && as->pm &&
      (as->pm->__proto__ == __proto__ || as->pm->__proto__ == JS_EMPTY)) {
    PropertyMap *pm = as->pm;
    assert(pm->n_special_props == n_special);
    if (as->shape == NULL) {
      size_t n_embedded = pm->n_props;
      /* at least one normal embeded slot */
      if (pm->n_props == pm->n_special_props)
        n_embedded += 1;
      /* 2. if cached Shape is not available, find shape created for
       *    other alloction site. */
      if (pm->shapes != NULL && pm->shapes->n_embedded_slots == n_embedded)
        as->shape = pm->shapes;
      else
        /* 3. if there is not, create it */
        as->shape = new_object_shape(ctx, DEBUG_NAME("(prealloc)"), as->pm,
                                     n_embedded, 0);
    }
    return as->shape;
  }
  return NULL;
}
#endif /* ALLOC_SITE_CACHE */

/**
 * The most normal way to create an object.
 * Called from ``new'' instruction.
 */
#ifdef ALLOC_SITE_CACHE
JSValue create_simple_object_with_constructor(Context *ctx, JSValue ctor,
                                              AllocSite *as)
#else /* ALLOC_SITE_CACHE */
JSValue create_simple_object_with_constructor(Context *ctx, JSValue ctor)
#endif /* ALLOC_SITE_CACHE */
{
  JSValue prototype, obj;
  Shape *os;

  assert(is_function(ctor));

  prototype = get_prop(ctor, gconsts.g_string_prototype);
  if (!is_jsobject(prototype))
    prototype = gconsts.g_prototype_Object;

#ifdef ALLOC_SITE_CACHE
  os = get_cached_shape(ctx, as, prototype, OBJECT_SPECIAL_PROPS);
  if (os == NULL)
#endif /* ALLOC_SITE_CACHE */
    {
      JSValue retv;
      PropertyMap *pm;
      /* 1. If `prototype' is valid, find the property map */
      retv = get_system_prop(prototype, gconsts.g_string___property_map__);
      if (retv != JS_EMPTY)
        pm = jsv_to_property_map(retv);
      else {
        /* 2. If there is not, create it. */
        int n_props = 0;
        int n_embedded = OBJECT_SPECIAL_PROPS + 1; /* at least 1 normal slot */
        GC_PUSH(prototype);
        pm = new_property_map(ctx, DEBUG_NAME("(new)"),
                              OBJECT_SPECIAL_PROPS, n_props,
                              OBJECT_USPECIAL_PROPS, prototype,
                              gpms.g_property_map_root);
        GC_PUSH(pm);
        pm->shapes = new_object_shape(ctx, DEBUG_NAME("(new)"),
                                      pm, n_embedded, 0);
        assert(Object_num_builtin_props +
               Object_num_double_props + Object_num_gconsts_props == 0);

        /* 3. Create a link from the prototype object to the PM so that
         *    this function can find it in the following calls. */
        set_prop(ctx, prototype, gconsts.g_string___property_map__,
                 (JSValue) (uintjsv_t) (uintptr_t) pm, ATTR_SYSTEM);
        GC_POP2(pm, prototype);
      }
      /* 4. Obtain the shape of the PM. There should be a single shape, if any,
       *    because the PM is an entrypoint. */
      os = pm->shapes;
    }

  obj = new_simple_object(ctx, DEBUG_NAME("inst:new"), os);
#ifdef ALLOC_SITE_CACHE
  if (os->pm->__proto__ == JS_EMPTY)
    set_prop(ctx, obj, gconsts.g_string___proto__, prototype, ATTR_NONE);
  object_set_alloc_site(obj, as);
#endif /* ALLOC_SITE_CACHE */
  return obj;
}

#ifdef ALLOC_SITE_CACHE
JSValue create_array_object(Context *ctx, char *name, size_t size)
{
  JSValue obj;
  AllocSite *as = &ctx->spreg.cf->insns[ctx->spreg.pc].alloc_site;
  Shape *os = get_cached_shape(ctx, as, gconsts.g_prototype_Array,
                               ARRAY_SPECIAL_PROPS);
  if (os == NULL)
    os = gshapes.g_shape_Array;
  obj = new_array_object(ctx, name, os, size);
  object_set_alloc_site(obj, as);
  return obj;
}
#endif /* ALLOC_SITE_CACHE */

#ifdef ALLOC_SITE_CACHE
void init_alloc_site(AllocSite *alloc_site)
{
  alloc_site->shape = NULL;
  alloc_site->pm = NULL;
  alloc_site->polymorphic = 0;
}
#endif /* ALLOC_SITE_CACHE */

#ifdef INLINE_CACHE
void init_inline_cache(InlineCache *ic)
{
  ic->shape = NULL;
  ic->prop_name = JS_EMPTY;
  ic->index = 0;
}
#endif /* INLINE_CACHE */

#include "object-compat.c"

#ifdef HC_PROF
struct root_property_map {
  /* malloc structure */
  PropertyMap *pm;
  struct root_property_map *next;
};
/* exprot to GC */
struct root_property_map *root_property_map;

static void hcprof_add_root_property_map(PropertyMap *pm)
{
  struct root_property_map *e =
    (struct root_property_map *) malloc(sizeof(struct root_property_map));
  e->pm = pm;
  e->next = root_property_map;
  root_property_map = e;
}

static void print_property_map(char *key, PropertyMap *pm)
{
  Shape *os;
  if (key == NULL)
    key = "(root)";
  printf("======== %s start ========\n", key);
  printf("HC: %p %p %p %d %d %d %s %s\n",
         pm,
         pm->prev,
         pm->shapes,
         pm->n_props,
         pm->n_enter,
         pm->n_leave,
         key,
#ifdef DEBUG
         pm->name
#else /* DEBUG */
         ""
#endif /* DEBUG */
         );
  for (os = pm->shapes; os != NULL; os = os->next) {
    printf("SHAPE: %p %p %d %d %s\n",
           os,
           os->next,
           os->n_embedded_slots,
           os->n_extension_slots,
#ifdef DEBUG
           os->name
#else /* DEBUG */
           ""
#endif /* DEBUG */
           );
  }
  print_hash_table(pm->map);
  printf("======== %s end ========\n", key);
}

static void print_property_map_recursive(char *key, PropertyMap *pm)
{
  HashIterator iter;
  HashCell *p;

  print_property_map(key, pm);
  iter = createHashIterator(pm->map);
  while(nextHashCell(pm->map, &iter, &p) != FAIL)
    if (is_transition(p->entry.attr))
      print_property_map_recursive(string_to_cstr(p->entry.key),
                                   p->entry.data.u.pm);
}

void hcprof_print_all_hidden_class(void)
{
  struct root_property_map *e;
  print_property_map_recursive(NULL, gpms.g_property_map_root);
  for (e = root_property_map; e != NULL; e = e->next)
    print_property_map_recursive(NULL, e->pm);
}
#endif /* HC_PROF */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
