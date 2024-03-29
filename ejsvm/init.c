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

/*
 * initilaizes global constants
 */
void init_global_constants(void) {
  int i;
  for (i = 0; i < sizeof(gconsts)/sizeof(JSValue); i++)
    ((JSValue *)&gconsts)[i] = JS_UNDEFINED;

  /* string constants */
  gconsts.g_string___property_map__ = cstr_to_string(NULL, "__property_map__");
  gconsts.g_string_prototype = cstr_to_string(NULL, "prototype");
  gconsts.g_string___proto__ = cstr_to_string(NULL, "__proto__");
  gconsts.g_string_tostring  = cstr_to_string(NULL, "toString");
  gconsts.g_string_valueof   = cstr_to_string(NULL, "valueOf");
  gconsts.g_string_boolean   = cstr_to_string(NULL, "boolean");
  gconsts.g_string_number    = cstr_to_string(NULL, "number");
  gconsts.g_string_object    = cstr_to_string(NULL, "object");
  gconsts.g_string_string    = cstr_to_string(NULL, "string");
  gconsts.g_string_true      = cstr_to_string(NULL, "true");
  gconsts.g_string_false     = cstr_to_string(NULL, "false");
  gconsts.g_string_null      = cstr_to_string(NULL, "null");
  gconsts.g_string_undefined = cstr_to_string(NULL, "undefined");
  gconsts.g_string_length    = cstr_to_string(NULL, "length");
  gconsts.g_string_objtostr  = cstr_to_string(NULL, "[object Object]");
  gconsts.g_string_empty     = cstr_to_string(NULL, "");
  gconsts.g_string_comma     = cstr_to_string(NULL, ",");
  gconsts.g_string_blank     = cstr_to_string(NULL, " ");

  /* numbers */
  gconsts.g_flonum_infinity  = double_to_flonum(NULL, INFINITY);
  gconsts.g_flonum_negative_infinity = double_to_flonum(NULL, -INFINITY);
  gconsts.g_flonum_nan       = double_to_flonum(NULL, NAN);

  /* boolean */
  gconsts.g_boolean_true  = JS_TRUE;
  gconsts.g_boolean_false = JS_FALSE;

  /* special */
  gconsts.g_null      = JS_NULL;
  gconsts.g_undefined = JS_UNDEFINED;
}

static Shape *create_map_and_shape(Context *ctx,
                                   char *name,
                                   uint32_t num_special,
                                   JSValue proto,
                                   ObjBuiltinProp builtin_props[],
                                   uint32_t num_builtin_props,
                                   ObjDoubleProp double_props[],
                                   uint32_t num_double_props,
                                   ObjGconstsProp gconsts_props[],
                                   uint32_t num_gconsts_props,
                                   char *user_props[],
                                   uint32_t num_user_props)
{
  PropertyMap *m;
  Shape *s;
  int i;
  uint32_t num_normal_props =
    num_builtin_props + num_double_props + num_gconsts_props + num_user_props;
  uint32_t num_user_special_props;
  uint32_t num_props;
  uint32_t num_embedded;
  uint32_t index = num_special;

  /* remove special props */
  num_user_special_props = 0;
  for (i = 0; i < num_double_props; i++) {
    ObjDoubleProp *p = &double_props[i];
    if (p->index != -1)
      num_user_special_props++;
  }
  num_normal_props -= num_user_special_props;

  num_props = num_normal_props + num_special;
  num_embedded = num_props + (num_normal_props == 0 ? 1 : 0);

  m = new_property_map(ctx, name, num_special, num_props,
                       num_user_special_props, proto,
                       gpms.g_property_map_root);
#ifdef DUMP_HCG
  m->is_entry = 1;
#endif /* DUMP_HCG */
  for (i = 0; i < num_builtin_props; i++) {
    ObjBuiltinProp *p = &builtin_props[i];
    property_map_add_property_entry(ctx, m, cstr_to_string(ctx, p->name),
                                    index++, p->attr);
  }
  for (i = 0; i < num_double_props; i++) {
    ObjDoubleProp *p = &double_props[i];
    if (p->index != -1)
      property_map_add_property_entry(ctx, m, cstr_to_string(ctx, p->name),
                                      p->index, p->attr);
    else
      property_map_add_property_entry(ctx, m, cstr_to_string(ctx, p->name),
                                      index++, p->attr);
  }
  for (i = 0; i < num_gconsts_props; i++) {
    ObjGconstsProp *p = &gconsts_props[i];
    property_map_add_property_entry(ctx, m, cstr_to_string(ctx, p->name),
                                    index++, p->attr);
  }
  for (i = 0; i < num_user_props; i++) {
    char *p = user_props[i];
    property_map_add_property_entry(ctx, m, cstr_to_string(ctx, p),
                                    index++, ATTR_NONE);
  }
#ifdef ALLOC_SITE_CACHE
  s = new_object_shape(ctx, name, m, num_embedded, 0, NULL);
#else /* ALLOC_SITE_CACHE */
  s = new_object_shape(ctx, name, m, num_embedded, 0);
#endif /* ALLOC_SITE_CACHE */
  return s;
}

#define CREATE_MAP_AND_SHAPE(ctx, name, num_special, proto, KEY)  \
  create_map_and_shape(ctx, name, num_special, proto,             \
                       KEY ## _builtin_props,                     \
                       KEY ## _num_builtin_props,                 \
                       KEY ## _double_props,                      \
                       KEY ## _num_double_props,                  \
                       KEY ## _gconsts_props,                     \
                       KEY ## _num_gconsts_props,                 \
                       NULL,                                      \
                       0)

static void fill_builtin_properties(Context *ctx,
                                    JSValue object,
                                    ObjBuiltinProp builtin_props[],
                                    uint32_t num_builtin_props,
                                    ObjDoubleProp double_props[],
                                    uint32_t num_double_props,
                                    ObjGconstsProp gconsts_props[],
                                    uint32_t num_gconsts_props)
{
  int i;
  for (i = 0; i < num_builtin_props; i++) {
    ObjBuiltinProp *p = &builtin_props[i];
    JSValue value =
      new_builtin_object(ctx, p->name, gshapes.g_shape_Builtin,
                         p->fn, builtin_not_a_constructor, p->na);
    set_prop_direct(ctx, object, cstr_to_string(ctx, p->name),
                    value, p->attr);
  }
  for (i = 0; i < num_double_props; i++) {
    ObjDoubleProp *p = &double_props[i];
    set_prop_direct(ctx, object, cstr_to_string(ctx, p->name),
                    double_to_number(ctx, p->value), p->attr);
  }
  for (i = 0; i < num_gconsts_props; i++) {
    ObjGconstsProp *p = &gconsts_props[i];
    set_prop_direct(ctx, object, cstr_to_string(ctx, p->name),
                    *(p->addr), p->attr);
  }
}
#define FILL_BUILTIN_PROPERTIES(ctx, object, KEY)      \
  fill_builtin_properties(ctx, object,                 \
                          KEY ## _builtin_props,       \
                          KEY ## _num_builtin_props,   \
                          KEY ## _double_props,        \
                          KEY ## _num_double_props,    \
                          KEY ## _gconsts_props,       \
                          KEY ## _num_gconsts_props)

/*
 * initialisation of prototypes objects, constructor objects, and their
 * property maps and shapes.
 */
void init_meta_objects(Context *ctx)
{
  /*
   * Step 0
   *  - Create root property map
   */
  gpms.g_property_map_root =
    new_property_map(ctx, DEBUG_NAME("root"), 0, 0, 0, JS_NULL, NULL);

  /*
   * Step 1
   *  - Create property maps and object shapes for prototype objects.
   *  - Create prototype objects.
   *  - Create property maps and object shapes for instances.
   */

  /* STEP1
   *   T        Type name.
   *   pproto   __proto__ of prototype object.
   *   psp      # of special fields of prototype object.
   *   isp      # of special fields of instances
   *   ctor     C-constructor function to create prototype object.
   *   ctorargs [VA] Custom arguments to be passed to the constructor.
   */
#define STEP1(T, pproto, psp, isp, ctor, ctorargs...)                   \
  do {                                                                  \
    Shape *os =                                                         \
      CREATE_MAP_AND_SHAPE(ctx, DEBUG_NAME(#T "Prototype"), psp, pproto, \
                           T ## Prototype);                             \
    JSValue iproto = ctor(ctx, DEBUG_NAME(#T "Prototype"), os, ##ctorargs); \
    gshapes.g_shape_ ## T =                                             \
      CREATE_MAP_AND_SHAPE(ctx, DEBUG_NAME(#T "0"), isp, iproto, T);    \
    gconsts.g_prototype_ ## T = iproto;                                 \
  } while(0)

#define OBJPROTO gconsts.g_prototype_Object

  STEP1(Object,   JS_NULL,  OBJECT_SPECIAL_PROPS,  OBJECT_SPECIAL_PROPS,
        new_simple_object);
  STEP1(Function, OBJPROTO, BUILTIN_SPECIAL_PROPS, FUNCTION_SPECIAL_PROPS,
        new_builtin_object, function_prototype_fun,
        builtin_not_a_constructor, 0);
  STEP1(Array,    OBJPROTO, ARRAY_SPECIAL_PROPS,   ARRAY_SPECIAL_PROPS,
        new_array_object, 0);
  STEP1(String,   OBJPROTO, STRING_SPECIAL_PROPS,  STRING_SPECIAL_PROPS,
        new_string_object, gconsts.g_string_empty);
  STEP1(Boolean,  OBJPROTO, BOOLEAN_SPECIAL_PROPS, BOOLEAN_SPECIAL_PROPS,
        new_boolean_object, JS_FALSE);
  STEP1(Number,   OBJPROTO, NUMBER_SPECIAL_PROPS,  NUMBER_SPECIAL_PROPS,
        new_number_object, FIXNUM_ZERO);

  gpms.g_property_map_Object = gshapes.g_shape_Object->pm;

#undef OBJPROTO
#undef STEP1
  
  /*
   * Step 2
   *   - Fill built-in properties of prototype objects.
   *   - Create constructors.
   *   - Fill built-in properties of constructors.
   *   TODO: create dedicated object shapes for the constructors.
   */

  gshapes.g_shape_Builtin =
    CREATE_MAP_AND_SHAPE(ctx, DEBUG_NAME("Builtin0"), BUILTIN_SPECIAL_PROPS,
                         gconsts.g_prototype_Function, Builtin);

#define STEP2(T, cfun, cctor, na)                               \
  do {                                                          \
    JSValue ctor;                                               \
    FILL_BUILTIN_PROPERTIES(ctx, gconsts.g_prototype_ ## T,     \
                            T ## Prototype);                    \
    ctor = new_builtin_object(ctx, DEBUG_NAME(#T),              \
                              gshapes.g_shape_Builtin,          \
                              cfun, cctor, na);                 \
    FILL_BUILTIN_PROPERTIES(ctx, ctor, T ## Constructor);       \
    gconsts.g_ctor_ ## T = ctor;                                \
  } while (0)

  STEP2(Object,   object_constr,        object_constr,   0);
  STEP2(Function, function_constr,      function_constr, 0);
  STEP2(Array,    array_constr,         array_constr,    0);
  STEP2(String,   string_constr_nonew,  string_constr,   1);
  STEP2(Number,   number_constr_nonew,  number_constr,   1);
  STEP2(Boolean,  boolean_constr_nonew, boolean_constr,  1);
#undef STEP2

  /*
   * Step 3
   *   - Create builtin constructors whose prototypes are normal objects.
   *   TODO: create dedicated object shapes for the constructors.
   */
#define STEP3(T, cfun, cctor, na)                                       \
  do {                                                                  \
    JSValue prototype, ctor;                                            \
    prototype = new_simple_object(ctx, DEBUG_NAME(#T "Prototype"),      \
                                  gshapes.g_shape_Object);              \
    ctor = new_builtin_with_constr(ctx, DEBUG_NAME(#T),                 \
                                   gshapes.g_shape_Builtin,             \
                                   cfun, ccotr, na);                    \
    FILL_BUILTIN_PROPERTIES(ctx, ctor, T ## Constructor);               \
    gconsts.g_ctor_ ## T = ctor;                                        \
  } while (0)

#ifdef USE_REGEXP
  STEP3(RegExp,   regexp_constr_nonew, regexp_constr,   2);
#endif /* USE_REGEXP */

#undef STEP3
}

#define CREATE_GLOBAL_OBJECT(ctx, name, KEY, key, uprops, num_uprops)   \
do {                                                                    \
  JSValue proto = new_simple_object(ctx, DEBUG_NAME(name ".__proto__"), \
                                    gshapes.g_shape_Object);            \
  Shape *os = create_map_and_shape(ctx, DEBUG_NAME(name), 0, proto,     \
                                   KEY ## _builtin_props,               \
                                   KEY ## _num_builtin_props,           \
                                   KEY ## _double_props,                \
                                   KEY ## _num_double_props,            \
                                   KEY ## _gconsts_props,               \
                                   KEY ## _num_gconsts_props,           \
                                   uprops, num_uprops);                 \
  JSValue obj = new_simple_object(ctx, DEBUG_NAME(name), os);           \
  gconsts.key = obj;                                                    \
} while(0)

/*
 * initializes global objects
 */
void init_global_objects(Context *ctx)
{
  /* Step 1
   *   - create shape
   *   - create object with empty slots
   */
  CREATE_GLOBAL_OBJECT(ctx, "global", Global, g_global, NULL, 0);
  CREATE_GLOBAL_OBJECT(ctx, "math", Math, g_math, NULL, 0);
  CREATE_GLOBAL_OBJECT(ctx, "performance", Performance, g_performance, NULL, 0);

  /* Step 2
   *   - fill propertyes
   */
  FILL_BUILTIN_PROPERTIES(ctx, gconsts.g_global, Global);
  FILL_BUILTIN_PROPERTIES(ctx, gconsts.g_math, Math);
  FILL_BUILTIN_PROPERTIES(ctx, gconsts.g_performance, Performance);
}

#ifdef LOAD_HCG
/*
 * load compiled hidden class
 */
void load_hcg(Context *ctx, char *filename)
{
  FILE *fp;
  char buf[1000];
  struct transition {
    int from;
    int to;
    JSValue name;
  } transitions[1000];
  int ntotal_trans = 0;
  int previds[1000];
  int id;
  int line;
  PropertyMap *pms[1000];
  int i;

  if ((fp = fopen(filename, "r")) == NULL)
    LOG_ERR("load_pre_hc");

  line = 0;
  fgets(buf, sizeof buf, fp);
  line++;
  for (id = 0; ; id++) {
    int nprops, ntrans, previd;
    int funno, insnno;
    PropertyMap *pm;
    int ret;
#ifdef DEBUG
    char *debug_name = (char*) malloc(10);
#endif /* DEBUG */

    ret = sscanf(buf, "HC %d %d %d", &nprops, &ntrans, &previd);
    if (ret == 0)
      break;

#ifdef DEBUG
    sprintf(debug_name, "(pre%d)", id);
#endif /* DEBUG */
    previds[id] = previd;
    pm = new_property_map(ctx, DEBUG_NAME(debug_name),
                          OBJECT_SPECIAL_PROPS, nprops,
                          OBJECT_USPECIAL_PROPS, JS_EMPTY,
                          gpms.g_property_map_root);
    pms[id] = pm;

    fgets(buf, sizeof buf, fp);
    line++;

    ret = sscanf(buf, "ENTRY %d %d", &funno, &insnno);
    if (ret == 2) {
      struct function_table *f = &ctx->function_table[funno];
      f->insns[insnno].loaded_pm = pm;
      fgets(buf, sizeof buf, fp);
      line++;
    }

    for (i = 0; i < nprops + ntrans; i++) {
      char name[100];
      int index, destid;

      ret = sscanf(buf, "PROP %d %s", &index, name);
      if (ret == 2) {
        JSValue name_jsv = cstr_to_string(ctx, name);
        property_map_add_property_entry(ctx, pm, name_jsv, index, ATTR_NONE);
        fgets(buf, sizeof buf, fp);
        line++;
        continue;
      }
      ret = sscanf(buf, "TRANSITION %d %s", &destid, name);
      if (ret == 2) {
        struct transition *t= &transitions[ntotal_trans++];
        t->from = id;
        t->to = destid;
        t->name = cstr_to_string(ctx, name);
        fgets(buf, sizeof buf, fp);
        line++;
        continue;
      }
      fprintf(stderr, "format error in line %d: %s\n", line, buf);
      exit(1);
    }
  }

  fclose(fp);

  /* install prev pointer */
  for (i = 0; i < id; i++) {
    PropertyMap *pm, *prev;
    pm = pms[i];
    if (previds[i] == -1)
      prev = gpms.g_property_map_root;
    else
      prev = pms[previds[i]];
    pm->prev = prev;
  }

  /* install transitions */
  for (i = 0; i < ntotal_trans; i++) {
    struct transition *t = &transitions[i];
    PropertyMap *pm = pms[t->from];
    PropertyMap *dest = pms[t->to];
    property_map_add_transition(ctx, pm, t->name, dest);
  }

  fflush(stdout);
}
#endif /* LOAD_HCG */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
