/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN extern
#include "header.h"

#ifdef VERBOSE_HC
int sprint_property_map(char *start, PropertyMap *pm);
#endif /* VERBOSE_HC */

static
JSValue get_system_prop(JSValue obj, JSValue name) __attribute__((unused));

static PropertyMap *extend_property_map(Context *ctx, PropertyMap *prev,
                                        JSValue prop_name,  Attribute attr);
static void object_grow_shape(Context *ctx, JSValue obj, Shape *os);
#ifdef HC_PROF
static void hcprof_add_root_property_map(PropertyMap *pm);
#endif /* HC_PROF */

/* Profiling */
static inline void hcprof_enter_shape(Shape *os)
{
#if defined(HC_PROF) || defined(HC_SKIP_INTERNAL)
  {
    PropertyMap *pm = os->pm;
    pm->n_enter++;
  }
#endif /* HC_PROF || HC_SKIP_INTERNAL */
#if defined (HC_PROF) || defined(ALLOC_SITE_CACHE)
  os->n_enter++;
#endif /* HC_PROF || ALLOC_SITE_CACHE */
#ifdef AS_PROF
  if (os->alloc_site != NULL)
    os->alloc_site->transition++;
#endif /* AS_PROF */
}

static inline void hcprof_leave_shape(Shape *os)
{
#if defined(HC_PROF) || defined(HC_SKIP_INTERNAL)
  {
    PropertyMap *pm = os->pm;
    pm->n_leave++;
  }
#endif /* HC_PROF || HC_SKIP_INTERNAL */
#if defined(HC_PROF) || defined(ALLOC_SITE_CACHE)
  os->n_leave++;
#endif /* HC_PROF || ALLOC_SITE_CACHE */
}

#define HC_PROF_ENTER_SHAPE(os) hcprof_enter_shape(os)
#define HC_PROF_LEAVE_SHAPE(os) hcprof_leave_shape(os)

#ifdef SHAPE_PROF
int shape_search_trial = 0;
int shape_search_count = 0;
int shape_search_success = 0;
#endif /* SHAPE_PROF */

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
    size_t n_embedded, n_extension;

    GC_PUSH4(obj, name, v, current_os);
    /* 1. If there is not next property map, create it. */
    if (next_pm == NULL) {
      next_pm = extend_property_map(ctx, current_os->pm, name, att);
#ifdef DUMP_HCG
      if (skip_setter)
        next_pm->is_builtin = 1;
#endif /* DUMP_HCG */
      PRINT("  new property (new PM %p is created)\n", next_pm);
    } else
#ifdef HC_SKIP_INTERNAL
      /* If the next property map is transient, take the next */
      while (next_pm->transient) {
        HashTransitionIterator iter =
          createHashTransitionIterator(next_pm->map);
        HashTransitionCell *cell;
        int ret __attribute__((unused));
        assert(next_pm->n_transitions == 1);
        ret = nextHashTransitionCell(next_pm->map, &iter, &cell);
        assert(ret != FAIL);
        next_pm = hash_transition_cell_pm(cell);
      }
#endif /* HC_SKIP_INTERNAL */
      PRINT("  new property (cached PM %p is used)\n", next_pm);
    GC_PUSH(next_pm);

    /* 2. Find the shape that is compatible to the current shape. */
    n_embedded = current_os->n_embedded_slots;
    n_extension = current_os->n_extension_slots;
    next_os = NULL;
    if (next_os == NULL) {
      /* 2.2  Find from the shape list of the next PM. */
      size_t need_slots = next_pm->n_props;
      /* compute new size of extension array */
      if (n_embedded + n_extension - (n_extension == 0 ? 0 : 1) < need_slots)
        n_extension = need_slots - (n_embedded - 1);
      PRINT("  finding shape for PM %p (n_props = %d) EM/EX %lu %lu\n",
            next_pm, next_pm->n_props, n_embedded, n_extension);
      next_os = next_pm->shapes;
#ifdef SHAPE_PROF
      shape_search_count++;
#endif /* SHAPE_PROF */
      while (next_os != NULL) {
#ifdef SHAPE_PROF
        shape_search_trial++;
#endif /* SHAPE_PROF */
        if (next_os->n_embedded_slots == n_embedded &&
            next_os->n_extension_slots == n_extension
#if ALLOC_SITE_CACHE
            && next_os->alloc_site == current_os->alloc_site
#endif /* ALLOC_SITE_CACHE */
            ) {
#ifdef SHAPE_PROF
          shape_search_success++;
#endif /* SHAPE_PROF */
          PRINT("    found: %p\n", next_os);
          break;
        } else {
#ifdef ALLOC_SITE_CACHE
          PRINT("    not the one %p: EM/EX %d %d AS %p\n",
                next_os, next_os->n_embedded_slots, next_os->n_extension_slots,
                next_os->alloc_site);
#else /* ALLOC_SITE_CACHE */
          PRINT("    not the one %p: EM/EX %d %d\n",
                next_os, next_os->n_embedded_slots, next_os->n_extension_slots);
#endif /* ALLOC_SITE_CACHE */
        }
        next_os = next_os->next;
      }
    }

    /* 3. If there is not compatible shape, create it. */
    if (next_os == NULL) {
#ifdef ALLOC_SITE_CACHE
      next_os = new_object_shape(ctx, DEBUG_NAME("(extend)"), next_pm,
                                 n_embedded, n_extension,
                                 current_os->alloc_site);
#else /* ALLOC_SITE_CACHE */
      next_os = new_object_shape(ctx, DEBUG_NAME("(extend)"), next_pm,
                                 n_embedded, n_extension);
                                 
#endif /* ALLOC_SITE_CACHE */
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
  if (ic != NULL && ic->pm == NULL) {
    ic->pm = object_get_shape(obj)->pm;
    ic->prop_name = name;
    ic->index = index;
    ic->miss = 0;
#ifdef IC_PROF
    ic->install++;
#endif /* IC_PROF */
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
  if (ic != NULL && ic->pm == NULL) {
    ic->pm = object_get_shape(obj)->pm;
    ic->prop_name = name;
    ic->index = index;
    ic->miss = 0;
#ifdef IC_PROF
    ic->install++;
#endif /* IC_PROF */
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
#ifdef IC_PROF
    if (ic != NULL)
      ic->proto++;
#endif /* IC_PROF */
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
  JSObject *p;
  int i;

  GC_PUSH(os);
  p = (JSObject *) gc_malloc(ctx, size, htag.v);
  p->shape = os;
  for (i = 0; i < n_embedded; i++)
    p->eprop[i] = JS_EMPTY;

#ifdef DEBUG
  p->name = name;
#endif /* DEBUG */

  HC_PROF_ENTER_SHAPE(os);
  GC_POP(os);

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
  for (i = 0; i < size; i++)
    array_data[i] = JS_UNDEFINED;

  set_array_ptr_body(p, array_data);
  set_array_ptr_size(p, size);
  set_array_ptr_length(p, cint_to_number(ctx, (cint) size));
  GC_POP(p);

  assert(Array_num_builtin_props +
         Array_num_double_props + Array_num_gconsts_props == 1);

  return ptr_to_normal_array(p);
}


JSValue new_function_object(Context *ctx, char *name, Shape *os, int ft_index)
{
  JSObject *p;
  JSValue prototype;

  assert(os->pm->n_special_props == FUNCTION_SPECIAL_PROPS);

  GC_PUSH(os);
  prototype = new_simple_object(ctx, DEBUG_NAME("(prototype)"),
                                gshapes.g_shape_Object);
  GC_PUSH(prototype);
  p = allocate_jsobject(ctx, name, os, HTAG_FUNCTION);
  GC_POP2(prototype, os);

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
  hash = hash_create(ctx, n_props);
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
  m->transient = 0;
#endif /* HC_SKIP_INTERNAL */

#ifdef DEBUG
  m->name = name;
  m->n_user_special_props = n_user_special_props;
#endif /* DEBUG */
#if defined(HC_PROF) || defined(HC_SKIP_INTERNAL)
  m->n_enter = 0;
  m->n_leave = 0;
#endif /* HC_PROF || HC_SKIP_INTERNAL */
#ifdef HC_PROF
  if (prev == gpms.g_property_map_root)
    hcprof_add_root_property_map(m);
#ifdef DUMP_HCG
  if (ctx == NULL) {
    m->function_no = -1;
    m->insn_no = -1;
  } else {
    m->function_no = (int) (ctx->spreg.cf - ctx->function_table);
    m->insn_no = ctx->spreg.pc;
  }
  m->is_entry = 0;
  m->is_builtin = 0;
#endif /* DUMP_HCG */
  {
    static int last_id;
    m->id = ++last_id;
  }
#endif /* HC_PROF */

#if defined(HC_SKIP_INTERNAL) || defined(WEAK_SHAPE_LIST)
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
#endif /* HC_SKIP_INTERNAL || WEAK_SHAPE_LIST */
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

#ifdef VERBOSE_HC
  {
    char buf[1000];
    sprint_property_map(buf, m);
    printf("HC-create extend %s\n", buf);
  }
#endif /* VERBOSE_HC */

  return m;
}

#ifdef LOAD_HCG
static void property_map_install___proto__(PropertyMap *pm, JSValue __proto__)
{
  if (pm->__proto__ != JS_EMPTY)
    return;
  pm->__proto__ = __proto__;
  HashTransitionIterator iter = createHashTransitionIterator(pm->map);
  HashTransitionCell *p;
  while (nextHashTransitionCell(pm->map, &iter, &p) != FAIL)
    property_map_install___proto__(hash_transition_cell_pm(p), __proto__);
}
#endif /* LOAD_HCG */

void property_map_add_property_entry(Context *ctx, PropertyMap *pm,
                                     JSValue name, uint32_t index,
                                     Attribute attr)
{
  hash_put_property(ctx, pm->map, name, index, attr);
}

void property_map_add_transition(Context *ctx, PropertyMap *pm,
                                 JSValue name, PropertyMap *dest)
{
#ifdef HC_SKIP_INTERNAL
  {
    uint16_t current_n_trans = pm->n_transitions;
    pm->n_transitions = PM_N_TRANS_UNSURE;  /* protect from GC */
    GC_PUSH(pm);
    hash_put_transition(ctx, pm->map, name, dest);
    GC_POP(pm);
    pm->n_transitions = current_n_trans + 1;
  }
#else /* HC_SKIP_INTERNAL */
  hash_put_transition(ctx, pm->map, name, dest);
#endif /* HC_SKIP_INTERNAL */
}

#ifdef ALLOC_SITE_CACHE
Shape *new_object_shape(Context *ctx, char *name, PropertyMap *pm,
                        int num_embedded, int num_extension,
                        AllocSite *as)
#else /* ALLOC_SITE_CACHE */
Shape *new_object_shape(Context *ctx, char *name, PropertyMap *pm,
                        int num_embedded, int num_extension)
#endif /* ALLOC_SITE_CACHE */
{
  Shape *s;
  Shape **pp;

  assert(num_embedded > 0);

  GC_PUSH(pm);
  s = (Shape *) gc_malloc(ctx, sizeof(Shape), CELLT_SHAPE);
  GC_POP(pm);
  s->pm = pm;
  s->n_embedded_slots  = num_embedded;
  s->n_extension_slots = num_extension;
#ifdef ALLOC_SITE_CACHE
  s->alloc_site = as;
#endif /* ALLOC_SITE_CACHE */
#if defined(HC_PROF) || defined(ALLOC_SITE_CACHE)
  s->n_enter = 0;
  s->n_leave = 0;
#endif /* HC_PROF || ALLOC_SITE_CACHE */
#ifdef AS_PROF
  s->n_alloc = 0;
#endif /* AS_PROF */
#ifdef DUMP_HCG
  s->is_cached = 0;
#endif /* DUMP_HCG */

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
      extension[i] = JS_EMPTY;
    p->eprop[extension_index] = (JSValue) (uintjsv_t) (uintptr_t) extension;
#ifdef AS_PROF
    if (os->alloc_site != NULL)
      os->alloc_site->copy_words += current_size;
#endif /* AS_PROF */
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
#ifdef AS_PROF
  as->n_alloc++;
#endif /* AS_PROF */
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
#ifdef ALLOC_SITE_CACHE
      /* 2. create object shape for this allocation site */
      as->shape = new_object_shape(ctx, DEBUG_NAME("(prealloc)"), as->pm,
                                   n_embedded, 0, as);
#ifdef DUMP_HCG
      as->shape->is_cached = 1;
#endif /* DUMP_HCG */
#else /* ALLOC_SITE_CACHE */
      /* 2. if cached Shape is not available, find shape created for
       *    other alloction site. */
      if (pm->shapes != NULL && pm->shapes->n_embedded_slots == n_embedded)
        as->shape = pm->shapes;
      else
        /* 3. if there is not, create it */
        as->shape = new_object_shape(ctx, DEBUG_NAME("(prealloc)"), as->pm,
                                     n_embedded, 0);
#endif /* ALLOC_SITE_CACHE */
    }
#ifdef AS_PROF
    as->shape->n_alloc++;
#endif /* AS_PROF */
    return as->shape;
  }
  return NULL;
}
#endif /* ALLOC_SITE_CACHE */

/**
 * The most normal way to create an object.
 * Called from ``new'' instruction.
 */
JSValue create_simple_object_with_constructor(Context *ctx, JSValue ctor)
{
  JSValue prototype;
  assert(is_function(ctor));

  prototype = get_prop(ctor, gconsts.g_string_prototype);
  if (!is_jsobject(prototype))
    prototype = gconsts.g_prototype_Object;

  return create_simple_object_with_prototype(ctx, prototype);
}

JSValue create_simple_object_with_prototype(Context *ctx, JSValue prototype)
{
  JSValue obj;
  Shape *os;
#ifdef ALLOC_SITE_CACHE
  AllocSite *as = &ctx->spreg.cf->insns[ctx->spreg.pc].alloc_site;
#endif /* ALLOC_SITE_CACHE */

#ifdef VERBOSE_NEW_OBJECT
#define PRINT(x...) printf(x)
#else /* VERBOSE_NEW_OBJECT */
#define PRINT(x...)
#endif /* VERBOSE_NEW_OBJECT */

  assert(is_jsobject(prototype));


  GC_PUSH(prototype);
#ifdef ALLOC_SITE_CACHE
  os = get_cached_shape(ctx, as, prototype, OBJECT_SPECIAL_PROPS);
  if (os == NULL)
#endif /* ALLOC_SITE_CACHE */
    {
      JSValue retv;
      PropertyMap *pm;

#ifdef ALLOC_SITE_CACHE
  PRINT("new_obj @ %03td:%03d cache miss proto %"PRIJSValue" AS %p\n",
        ctx->spreg.cf - ctx->function_table, ctx->spreg.pc, prototype, as);
#else /* ALLOC_SITE_CACHE */
  PRINT("new_obj @ %03td:%03d cache miss proto %"PRIJSValue"\n",
        ctx->spreg.cf - ctx->function_table, ctx->spreg.pc, prototype);
#endif /* ALLOC_SITE_CACHE */
#ifdef DEBUG
  PRINT("  proto name = %s\n", jsv_to_jsobject(prototype)->name);
#endif /* DEBUG */

#ifdef LOAD_HCG
      /* 0. If compiled hcg is available, use it */
      if (ctx->spreg.cf->insns[ctx->spreg.pc].loaded_pm != NULL) {
        PRINT("preload PM found PM %p\n",
              ctx->spreg.cf->insns[ctx->spreg.pc].loaded_pm);
        pm = ctx->spreg.cf->insns[ctx->spreg.pc].loaded_pm;
        ctx->spreg.cf->insns[ctx->spreg.pc].loaded_pm = NULL;  /* TODO duplicate */
        property_map_install___proto__(pm, prototype);
        GC_PUSH(pm);
        {
          int n_props;
#ifdef DEBUG
          char *debug_name = (char*) malloc(14);
          snprintf(debug_name, 14, "(new@%03d:%03d)",
                   (int) (ctx->spreg.cf - ctx->function_table),
                   ctx->spreg.pc);
#endif /* DEBUG */
          n_props = pm->n_props;
          if (n_props == pm->n_special_props)
            n_props += 1;
#ifdef ALLOC_SITE_CACHE
          pm->shapes = new_object_shape(ctx, DEBUG_NAME(debug_name),
                                        pm, n_props, 0, as);
#else /* ALLOC_SITE_CACHE */
          pm->shapes = new_object_shape(ctx, DEBUG_NAME(debug_name),
                                        pm, n_props, 0);
#endif /* ALLOC_SITE_CACHE */
        }
        /* 3. Create a link from the prototype object to the PM so that
         *    this function can find it in the following calls. */
        set_prop(ctx, prototype, gconsts.g_string___property_map__,
                 (JSValue) (uintjsv_t) (uintptr_t) pm, ATTR_SYSTEM);
        GC_POP(pm);
      } else
#endif /* LOAD_HCG */ 

      /* 1. If `prototype' is valid, find the property map */
      retv = get_system_prop(prototype, gconsts.g_string___property_map__);
      if (retv != JS_EMPTY) {
        pm = jsv_to_property_map(retv);
        PRINT("PM found in proto %p OS[0] %p AS %p\n",
              pm, pm->shapes, pm->shapes->alloc_site);
      } else {
        /* 2. If there is not, create it. */
        int n_props = 0;
        int n_embedded = OBJECT_SPECIAL_PROPS + 1;/* at least 1 normal slot */
        pm = new_property_map(ctx, DEBUG_NAME("(new)"),
                              OBJECT_SPECIAL_PROPS, n_props,
                              OBJECT_USPECIAL_PROPS, prototype,
                              gpms.g_property_map_root);
#ifdef DUMP_HCG
        pm->is_entry = 1;
#endif /* DUMP_HCG */
        GC_PUSH(pm);
#ifdef ALLOC_SITE_CACHE
        new_object_shape(ctx, DEBUG_NAME("(new)"), pm, n_embedded, 0, as);
#else /* ALLOC_SITE_CACHE */
        new_object_shape(ctx, DEBUG_NAME("(new)"), pm, n_embedded, 0);
#endif /* ALLOC_SITE_CACHE */
        assert(Object_num_builtin_props +
               Object_num_double_props + Object_num_gconsts_props == 0);
        PRINT("create PM/OS PM %p OS %p AS %p\n",
              pm, pm->shapes, pm->shapes->alloc_site);

        /* 3. Create a link from the prototype object to the PM so that
         *    this function can find it in the following calls. */
        set_prop(ctx, prototype, gconsts.g_string___property_map__,
                 (JSValue) (uintjsv_t) (uintptr_t) pm, ATTR_SYSTEM);
        GC_POP(pm);
      }
#ifdef ALLOC_SITE_CACHE
      /* 4. serch for the shape whose allocation site is here */
      for (os = pm->shapes; os != NULL; os = os->next) {
        assert(os->n_embedded_slots == OBJECT_SPECIAL_PROPS + 1);
        if (os->alloc_site == as)
          break;
      }
      /* 5. If there is not such a shape, create it */
      if (os == NULL)
        os = new_object_shape(ctx, DEBUG_NAME("(as variant)"),
                              pm, OBJECT_SPECIAL_PROPS + 1, 0, as);
#else /* ALLOC_SITE_CACHE */
      /* 4. Obtain the shape of the PM. There should be a single shape,
       * if any, because the PM is an entrypoint. */
      os = pm->shapes;
#endif /* !ALLOC_SITE_CACHE */

#ifdef ALLOC_SITE_CACHE
      if (as != NULL && as->pm == NULL) {
        as->pm = pm;
        as->shape = os;
#ifdef DUMP_HCG
        as->shape->is_cached = 1;
#endif /* DUMP_HCG */
        as->polymorphic = 0;
      }
#endif /* ALLOC_SITE_CACHE */
    }

#ifdef ALLOC_SITE_CACHE
  GC_PUSH(os);
  obj = new_simple_object(ctx, DEBUG_NAME("inst:new"), os);
  GC_PUSH(obj);
  if (os->pm->__proto__ == JS_EMPTY)
    set_prop(ctx, obj, gconsts.g_string___proto__, prototype, ATTR_NONE);
  GC_POP2(obj, os);
#else /* ALLOC_SITE_CACHE */
  obj = new_simple_object(ctx, DEBUG_NAME("inst:new"), os);
#endif /* ALLOC_SITE_CACHE */
  GC_POP(prototype);
  return obj;

#undef PRINT
}

#ifdef ALLOC_SITE_CACHE
JSValue create_array_object(Context *ctx, char *name, size_t size)
{
  JSValue obj;
  AllocSite *as = &ctx->spreg.cf->insns[ctx->spreg.pc].alloc_site;
  Shape *os = get_cached_shape(ctx, as, gconsts.g_prototype_Array,
                               ARRAY_SPECIAL_PROPS);
  if (os == NULL) {
    PropertyMap *pm = gshapes.g_shape_Array->pm;
    os = new_object_shape(ctx, DEBUG_NAME("(array)"),
                          pm, ARRAY_SPECIAL_PROPS + 1, 0, as);
#ifdef DUMP_HCG
    pm->is_entry = 1;
#endif /* DUMP_HCG */
    if (as->pm == NULL) {
      as->pm = pm;
      as->shape = os;
      as->polymorphic = 0;
    }
  }
  obj = new_array_object(ctx, name, os, size);
  return obj;
}
#endif /* ALLOC_SITE_CACHE */

#ifdef ALLOC_SITE_CACHE
void init_alloc_site(AllocSite *alloc_site)
{
  alloc_site->shape = NULL;
  alloc_site->pm = NULL;
  alloc_site->polymorphic = 0;
#ifdef AS_PROF
  alloc_site->copy_words = 0;
  alloc_site->transition = 0;
  alloc_site->n_alloc = 0;
#endif /* AS_PROF */
}
#endif /* ALLOC_SITE_CACHE */

#ifdef INLINE_CACHE
void init_inline_cache(InlineCache *ic)
{
  ic->pm = NULL;
  ic->prop_name = JS_EMPTY;
  ic->index = 0;
  ic->miss = 0;
#ifdef IC_PROF
  ic->count = 0;
  ic->hit = 0;
  ic->unavailable = 0;
  ic->install = 0;
  ic->proto = 0;
#endif /* IC_PROF */
}
#endif /* INLINE_CACHE */

/**
 * Obtain property of the object. It converts type of ``name'' if necessary.
 *   obj:  any type
 *   name: any type
 */
#ifdef INLINE_CACHE
JSValue get_object_prop(Context *ctx, JSValue obj, JSValue name,
			InlineCache *ic)
#else /* INLINE_CACHE */
  JSValue get_object_prop(Context *ctx, JSValue obj, JSValue name)
#endif /* INLINE_CACHE */
{
  if (!is_string(name)) {
    GC_PUSH(obj);
    name = to_string(ctx, name);
    GC_POP(obj);
  }
#ifdef INLINE_CACHE
  return get_prop_prototype_chain_with_ic(obj, name, ic);
#else /* INLINE_CACHE */
  return get_prop_prototype_chain(obj, name);
#endif /* INLINE_CACHE */
}

/*
 * obtain array element. `index' is an integer.
 * returns JS_EMPTY if `index` is out of range.
 */
JSValue get_array_element(Context *ctx, JSValue array, cint index)
{
  JSValue prop, ret;
  assert(is_array(array));

  if ((ret = get_array_element_no_proto(array, index)) != JS_EMPTY) {
    return ret;
  }

  GC_PUSH(array);
  prop = cint_to_number(ctx, index);
  GC_POP(array);
  prop = number_to_string(prop);
  return get_prop_prototype_chain(array, prop);
}

/*
 *  obtains array's property
 *    a: array
 *    p: property (number / string / other type)
 *  It is not necessary to check the type of `a'.
 */
JSValue get_array_prop(Context *ctx, JSValue a, JSValue p)
{
  if (is_fixnum(p))
    return get_array_element(ctx, a, fixnum_to_cint(p));

  if (!is_string(p)) {
    GC_PUSH(a);
    p = to_string(ctx, p);
    GC_POP(a);
  }
  assert(is_string(p));
  {
    JSValue num;
    GC_PUSH2(a, p);
    num = string_to_number(ctx, p);
    GC_POP2(p, a);
    if (is_fixnum(num))
      return get_array_element(ctx, a, fixnum_to_cint(num));
    else
      return get_prop_prototype_chain(a, p);
  }
}

/*
 * determines whether a[n] exists or not
 * if a[n] is not an element of body (a C array) of a, search properties of a
 *  a: array
 *  n: subscript
 */
int has_array_element(JSValue a, cint n)
{
  if (!is_array(a))
    return FALSE;
  if (n < 0 || get_jsarray_length(a) <= n)
    return FALSE;
  /* in body of 'a' */
  if (n < get_jsarray_size(a))
    return TRUE;
  /* in property of 'a' */
  return get_prop_prototype_chain(a, cint_to_string(n)) != JS_EMPTY;
}

/*
 * sets object's property
 *   o: object (but not an array)
 *   p: property (number / string / other type)
 *   v: value to be set
 * It is not necessary to check the type of `o'.
 */
int set_object_prop(Context *ctx, JSValue o, JSValue p, JSValue v)
{
  if (!is_string(p)) {
    GC_PUSH2(o, v);
    p = to_string(ctx, p);
    GC_POP2(v, o);
  }
  set_prop(ctx, o, p, v, ATTR_NONE);
  return SUCCESS;
}


/*
 * An array element is stored
 *  1. in array storage, or
 *  2. as a property
 * If 0 <= index < array.size, then the element is stored in the array storage.
 * Otherwise, it is stored as a property.
 *
 * Before judging where an element should be stored to, array storage may
 * be expanded.  If array.size <= index < ASIZE_LIMIT, the array storage
 * is expanded to the length of index.
 */

/*
 * Try to set a value into an continuous array of Array.
 * If the index is out of range of limit of continuous container,
 * handle it as a normal property.
 */
void set_array_element(Context *ctx, JSValue array, cint index, JSValue v)
{
  assert(is_array(array));

  /* 1. If array.size <= index < array.size * ASIZE_FACTOR + 1,
   *    expand the storage */
  {
    int32_t size = get_jsarray_size(array);
    if (size <= index && index < size + (size >> LOG_ASIZE_EXPAND_FACTOR) + 1) {
      GC_PUSH2(array, v);
      reallocate_array_data(ctx, array, size + (size >> LOG_ASIZE_EXPAND_FACTOR) + 1);
      GC_POP2(v, array);
    }
  }

  /* 2. If 0 <= index < array.size, store the value to the storage */
  if (0 <= index && index < get_jsarray_size(array)) {
    JSValue *storage = get_jsarray_body(array);
    storage[index] = v;
  } else {
    /* 3. otherwise, store it as a property */
    JSValue prop;
    GC_PUSH2(array, v);
    prop = cint_to_number(ctx, index);
    prop = number_to_string(prop);
    set_prop(ctx, array, prop, v, ATTR_NONE);
    GC_POP2(v, array);
  }

  /* 4. Adjust `length' property. */
  {
    JSValue length_value;
    cint length;
    length_value = get_jsarray_length(array);
    assert(is_fixnum(length_value));
    length = fixnum_to_cint(length_value);
    if (length <= index) {
      GC_PUSH(array);
      JSValue num = cint_to_number(ctx, index + 1);
      GC_POP(array);
      set_jsarray_length(array, num);
    }
  }
}

static void
remove_and_convert_numerical_properties(Context *ctx, JSValue array,
                                        int32_t length)
{
  Shape *os = object_get_shape(array);
  PropertyMap *pm = os->pm;
  HashPropertyIterator iter = createHashPropertyIterator(pm->map);
  JSValue key;
  uint32_t index;
  Attribute attr;
  GC_PUSH2(pm, array);
  while (nextHashPropertyCell(pm->map, &iter, &key, &index, &attr) != FAIL) {
    JSValue number_key;
    double double_key;
    int32_t int32_key;
    assert(is_string(key));
    GC_PUSH(key);
    number_key = string_to_number(ctx, key);
    double_key = number_to_double(number_key);
    int32_key = (int32_t) double_key;
    if (int32_key >= 0 && double_key == (double) int32_key) {
      if (int32_key < length) {
        JSValue v = object_get_prop(array, index);
        JSValue *storage = get_jsarray_body(array);
        storage[index] = v;
      }
      set_prop(ctx, array, key, JS_EMPTY, ATTR_NONE);
    }
    GC_POP(key);
  }
  GC_POP2(array, pm);
}

int set_array_prop(Context *ctx, JSValue array, JSValue prop, JSValue v)
{
  JSValue index_prop;

  /* 1. If prop is fixnum, do element access. */
  if (is_fixnum(prop)) {
    cint index = fixnum_to_cint(prop);
    set_array_element(ctx, array, index, v);
    return SUCCESS;
  }

  /* 2. Convert prop to a string. */
  GC_PUSH2(v, array);
  if (!is_string(prop))
    prop = to_string(ctx, prop);

  /* 3. If prop is fixnum-like, do element access. */
  GC_PUSH(prop);
  index_prop = string_to_number(ctx, prop);
  GC_POP3(prop, array, v);
  if (is_fixnum(index_prop)) {
    cint index = fixnum_to_cint(index_prop);
    set_array_element(ctx, array, index, v);
    return SUCCESS;
  }

  /* 4. If prop is `length', adjust container size. */
  if (prop == gconsts.g_string_length) {
    double double_length;
    int32_t length;
    GC_PUSH2(v, array);
    if (!is_number(v))
      v = to_number(ctx, v);
    double_length = number_to_double(v);
    length = (int32_t) double_length;
    if (double_length != (double) length || length < 0)
      LOG_EXIT("invalid array length");
    {
      int32_t old_size = get_jsarray_size(array);
      JSValue old_len_jsv = get_jsarray_length(array);
      int32_t old_length = (int32_t) number_to_double(old_len_jsv);
      /* 4.1. Adjust container size. */
      reallocate_array_data(ctx, array, length);
      if (old_size < old_length) {
        /* 4.2 Remove and convert numerical properties.
         *       [old_size, length)   -- convert to array element.
         *       [length, old_length) -- remove
         */
        remove_and_convert_numerical_properties(ctx, array, length);
      }
    }
    /* 4.3 Set length property. */
    set_jsarray_length(array, v);
    GC_POP2(array,v);
    return SUCCESS;
  }

  /* 5. Set normal property */
  set_prop(ctx, array, prop, v, ATTR_NONE);
  return SUCCESS;
}

/*
 * delete the hash cell with key and the property of the object
 * NOTE:
 *   The function does not reallocate (shorten) the prop array of the object.
 *   It must be improved.
 * NOTE:
 *   When using hidden class, this function does not delete a property
 *   of an object but merely sets the corresponding property as JS_UNDEFINED,
 */
int delete_object_prop(JSValue obj, HashKey key)
{
  int index;
  Attribute attr;

  if (!is_object(obj))
    return FAIL;

  /* Set corresponding property as JS_UNDEFINED */
  index = prop_index(jsv_to_jsobject(obj), key, &attr, NULL);
  if (index == - 1)
    return FAIL;
  object_set_prop(obj, index, JS_UNDEFINED);

  /* Delete map */
  LOG_EXIT("delete is not implemented");
  return SUCCESS;
}

/*
 * delete a[n]
 * Note that this function does not change a.length
 */
int delete_array_element(JSValue a, cint n)
{
  if (n < get_jsarray_size(a)) {
    JSValue *body = get_jsarray_body(a);
    body[n] = JS_UNDEFINED;
    return SUCCESS;
  }
  return delete_object_prop(a, cint_to_string(n));
}

/*
 * obtains the next property name in an iterator
 * iter:Iterator
 */
int iterator_get_next_propname(JSValue iter, JSValue *name)
{
  int size = get_jsnormal_iterator_size(iter);
  int index = get_jsnormal_iterator_index(iter);
  if(index < size) {
    JSValue *body = get_jsnormal_iterator_body(iter);
    *name = body[index++];
    set_jsnormal_iterator_index(iter, index);
    return SUCCESS;
  }else{
    *name = JS_UNDEFINED;
    return FAIL;
  }
}

#ifdef USE_REGEXP
/*
 * sets a regexp's members and makes an Oniguruma's regexp object
 */
int set_regexp_members(Context *ctx, JSValue re, char *pat, int flag)
{
  OnigOptionType opt;
  OnigErrorInfo err;
  char *e;

  regexp_pattern(re) = strdup(pat);

  opt = ONIG_OPTION_NONE;

  if (flag & F_REGEXP_GLOBAL) {
    regexp_global(re) = true;
    set_obj_cstr_prop(ctx, re, "global", JS_TRUE, ATTR_ALL);
  } else
    set_obj_cstr_prop(ctx, re, "global", JS_FALSE, ATTR_ALL);

  if (flag & F_REGEXP_IGNORE) {
    opt |= ONIG_OPTION_IGNORECASE;
    regexp_ignorecase(re) =  true;
    set_obj_cstr_prop(ctx, re, "ignoreCase", JS_TRUE, ATTR_ALL);
  } else
    set_obj_cstr_prop(ctx, re, "ignoreCase", JS_FALSE, ATTR_ALL);

  if (flag & F_REGEXP_MULTILINE) {
    opt |= ONIG_OPTION_MULTILINE;
    regexp_multiline(re) = true;
    set_obj_cstr_prop(ctx, re, "multiline", JS_TRUE, ATTR_ALL);
  } else
    set_obj_cstr_prop(ctx, re, "multiline", JS_FALSE, ATTR_ALL);

  e = pat + strlen(pat);
  if (onig_new(&(regexp_reg(re)), (OnigUChar *)pat, (OnigUChar *)e, opt,
               ONIG_ENCODING_ASCII, ONIG_SYNTAX_DEFAULT, &err) == ONIG_NORMAL)
    return SUCCESS;
  else
    return FAIL;
}

/*
 * returns a flag value from a ragexp objext
 */
int regexp_flag(JSValue re)
{
  int flag;

  flag = 0;
  if (regexp_global(re)) flag |= F_REGEXP_GLOBAL;
  if (regexp_ignorecase(re)) flag |= F_REGEXP_IGNORE;
  if (regexp_multiline(re)) flag |= F_REGEXP_MULTILINE;
  return flag;
}
#endif

/*
 * makes a simple iterator object
 */
JSValue new_iterator(Context *ctx, JSValue obj) {
  JSValue iter;
  int index = 0;
  int size = 0;
  JSValue tmpobj;

  GC_PUSH(obj);
  iter = ptr_to_normal_iterator(allocate_iterator(ctx));

  /* allocate an itearator */
  tmpobj = obj;
  do {
    PropertyMap *pm = object_get_shape(tmpobj)->pm;
    size += pm->n_props - pm->n_special_props;
    tmpobj = get_prop(tmpobj, gconsts.g_string___proto__);
  } while (tmpobj != JS_NULL);
  GC_PUSH(iter);
  allocate_iterator_data(ctx, iter, size);

  /* fill the iterator with object properties */
  do {
    HashTable *ht;
    HashPropertyIterator hi;
    JSValue key;
    uint32_t prop_index;
    Attribute attr;
    JSValue *body;

    ht = object_get_shape(obj)->pm->map;
    hi = createHashPropertyIterator(ht);

    body = get_jsnormal_iterator_body(iter);
    while (nextHashPropertyCell(ht, &hi, &key, &prop_index, &attr) == SUCCESS) {
      if (attr & ATTR_DE)
        continue;
      body[index++] = key;
    }
    obj = get_prop(obj, gconsts.g_string___proto__);
  } while (obj != JS_NULL);
  GC_POP2(iter, obj);
  return iter;
}

/*  data conversion functions */
char *space_chomp(char *str)
{
  while (isspace(*str)) str++;
  return str;
}

double cstr_to_double(char* cstr)
{
  char* endPtr;
  double ret;
  ret = strtod(cstr, &endPtr);
  while (isspace(*endPtr)) endPtr++;
  if (*endPtr == '\0') return ret;
  else return NAN;
}

#ifdef HC_PROF
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

static void print_shape_line(Shape *os)
{
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

static void print_property_map(char *key, PropertyMap *pm)
{
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
  {
    Shape *os;
    for (os = pm->shapes; os != NULL; os = os->next)
      print_shape_line(os);
  }
  print_hash_table(pm->map);
  printf("======== %s end ========\n", key);
}

static void print_property_map_recursive(char *key, PropertyMap *pm)
{
  HashTransitionIterator iter;
  HashTransitionCell *p;

  print_property_map(key, pm);
  iter = createHashTransitionIterator(pm->map);
  while(nextHashTransitionCell(pm->map, &iter, &p) != FAIL)
    print_property_map_recursive(string_to_cstr(hash_transition_cell_key(p)),
                                 hash_transition_cell_pm(p));
}

void hcprof_print_all_hidden_class(void)
{
  struct root_property_map *e;
  print_property_map_recursive(NULL, gpms.g_property_map_root);
  for (e = root_property_map; e != NULL; e = e->next)
    print_property_map_recursive(NULL, e->pm);
}

#ifdef DUMP_HCG
#ifdef ALLOC_SITE_CACHE
static void alloc_site_loc(Context *ctx,
                           AllocSite *as, int *fun_no, int *insn_no)
{
  int i;
  for (i = 0; i < ctx->nfuncs; i++) {
    FunctionTable *p = ctx->function_table + i;
    int j;
    for (j = 0; j < p->n_insns; j++) {
      if (as == &p->insns[j].alloc_site) {
        *fun_no = i;
        *insn_no = j;
        return;
      }
    }
  }
}
#endif /* ALLOC_SITE_CACHE */

static void dump_property_map_recursive(FILE *fp, Context *ctx,
                                        char *prop_name, PropertyMap *pm)
{
  Shape *os;

  fprintf(fp, "HC");
  fprintf(fp, " %p", pm);
  fprintf(fp, " %p", pm->prev);
  /*  fprintf(fp, " %c", prop_name == NULL ? 'E' : 'N'); */
  fprintf(fp, " %c", pm->is_entry ? 'E' : 'N');
  fprintf(fp, " %c", pm->is_builtin ? 'B' : 'N');
  fprintf(fp, " %d", pm->function_no);
  fprintf(fp, " %d", pm->insn_no);
  fprintf(fp, " %s", prop_name == NULL ? "(null)" : prop_name);
  fprintf(fp, " J");
  fprintf(fp, " %d", pm->n_enter);
  fprintf(fp, " %d", pm->n_leave);
#ifdef DEBUG
  fprintf(fp, " %s", pm->name);
#else /* DEBUG */
  fprintf(fp, " noname");
#endif /* DEBUG */
  fprintf(fp, " %d", pm->n_props);
  fprintf(fp, "\n");
  {
    HashPropertyIterator iter = createHashPropertyIterator(pm->map);
    JSValue key;
    uint32_t index;
    Attribute attr;
    while(nextHashPropertyCell(pm->map, &iter, &key, &index, &attr) != FAIL)
      fprintf(fp, "PROP %p %d %s %d\n", pm, index, string_to_cstr(key), attr);
  }
  for (os = pm->shapes; os != NULL; os = os->next) {
    int fun_no, insn_no;
#ifdef ALLOC_SITE_CACHE
    if (os->alloc_site != NULL) {
      alloc_site_loc(ctx, os->alloc_site, &fun_no, &insn_no);
    } else {
      fun_no = -1;
      insn_no = -1;
    }
#else /* ALLOC_SITE_CACHE */
    fun_no = -1;
    insn_no = -1;
#endif /* ALLOC_SITE_CACHE */
    if (os->is_cached)
      fprintf(fp, "SHAPE %p %d %d %d\n", pm, os->n_embedded_slots, fun_no, insn_no);
  }

  {
    HashTransitionIterator iter = createHashTransitionIterator(pm->map);
    HashTransitionCell *p;
    while(nextHashTransitionCell(pm->map, &iter, &p) != FAIL)
      dump_property_map_recursive(fp, ctx,
                                  string_to_cstr(hash_transition_cell_key(p)),
                                  hash_transition_cell_pm(p));
  }
}

void dump_hidden_classes(char *outfile, Context *ctx)
{
  struct root_property_map *e;
  FILE *fp;
  fp = fopen(outfile, "w");
  if (fp == NULL)
    LOG_EXIT("cannot open HC dump file");
  for (e = root_property_map; e != NULL; e = e->next)
    dump_property_map_recursive(fp, ctx, NULL, e->pm);
  fclose(fp);
}
#endif /* DUMP_HCG */

#ifdef AS_PROF
void print_as_prof(Context *ctx)
{
  int i;
  char buf[2000] = {};

  for (i = 0; i < ctx->nfuncs; i++) {
    FunctionTable *p = ctx->function_table + i;
    int j;
    for (j = 0; j < p->n_insns; j++) {
      AllocSite *as = &p->insns[j].alloc_site;
      if (as->pm != NULL) {
        int nshare = 0;
        PropertyMap *pm = as->pm;
        Shape *os;
        for (os = pm->shapes; os != NULL; os = os->next)
          nshare++;
#ifdef VERBOSE_HC
        buf[0] = ' ';
        sprint_property_map(buf + 1, pm);
#endif /* VERBOSE_HC */
        printf("AS %s %03d:%03d ", as->polymorphic ? "poly" : "mono", i, j);
        printf("alloc %6d hit %6d ", as->n_alloc,
               as->shape == NULL ? 0 : as->shape->n_alloc);
        printf("trans %6d copy %6d ", as->transition, as->copy_words);
        printf("size %d/%d ",
               as->shape == NULL ? pm->n_props : as->shape->n_embedded_slots,
               as->shape == NULL ? 0 : as->shape->n_extension_slots);
        printf("share %d%s\n", nshare, buf);
      }
    }
  }
}
#endif /* AS_PROF */

#endif /* HC_PROF */

#ifdef VERBOSE_HC
#ifndef HC_PROF
#error VERBOSE_HC require HC_PROF
#endif /* HC_PROF */
int sprint_property_map(char *start, PropertyMap *pm)
{
  char *buf = start;
  int i;

  buf += sprintf(buf, "%p(%3d)", pm, pm->id);
  if (pm->prev == NULL)
    buf += sprintf(buf, " prev (NIL)");
  else
    buf += sprintf(buf, " prev (%3d)", pm->prev->id);
  buf += sprintf(buf, " %7d/%7d props %d",
                 pm->n_enter, pm->n_leave, pm->n_props);
#ifdef HC_SKIP_INTERNAL
  buf += sprintf(buf, "trans %d", pm->n_transitions);
#endif /* HC_SKIP_INTERNAL */
  buf += sprintf(buf, " [");
  for (i = 0; i < pm->n_props; i++) {
    HashPropertyIterator iter = createHashPropertyIterator(pm->map);
    JSValue key;
    uint32_t index;
    Attribute attr;
    while (nextHashPropertyCell(pm->map, &iter, &key, &index, &attr) != FAIL) {
      if (index == i) {
        buf += sprintf(buf, "%s ", string_to_cstr(key));
        break;
      }
    }
  }
  buf += sprintf(buf, "]");

  return buf - start;
}
#endif /* VERBOSE_HC */


/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
