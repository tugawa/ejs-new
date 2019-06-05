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
  gconsts.g_flonum_infinity  = double_to_flonum(INFINITY);
  gconsts.g_flonum_negative_infinity = double_to_flonum(-INFINITY);
  gconsts.g_flonum_nan       = double_to_flonum(NAN);
}

/*
 * initilaizes global malloc-ed objects
 */
void init_global_malloc_objects(void) {
#ifdef HIDDEN_CLASS
  gobjects.g_hidden_class_0 =
    new_empty_hidden_class(NULL, HSIZE_NORMAL, HTYPE_TRANSIT);
#endif
}

/*
 * initializes global objects
 * 2017/03/30: moved from init_global
 */
void init_global_objects(void) {
  /*
   * It is necessary to make the object that will be set as Object.prototype,
   * because this object is referred to in new_simple_object.
   * Its `prototype' property is null.
   */
  gconsts.g_object_proto = new_big_predef_object_without_prototype(NULL);
  set_prototype_de(NULL, gconsts.g_object_proto, JS_NULL);
  gconsts.g_global = new_big_predef_object(NULL);
  gconsts.g_math = new_big_predef_object(NULL);

#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_object_proto",
                     obj_hidden_class(gconsts.g_object_proto));
  print_hidden_class("g_global", obj_hidden_class(gconsts.g_global));
  print_hidden_class("g_math", obj_hidden_class(gconsts.g_math));
#endif
#endif

}

/*
 * initializes builtin
 */
void init_builtin(Context *ctx) {
  init_builtin_object(ctx);
  init_builtin_array(ctx);
  init_builtin_function(ctx);
  init_builtin_number(ctx);
  init_builtin_string(ctx);
  init_builtin_boolean(ctx);
#ifdef need_flonum
  init_builtin_math(ctx);
#endif /* need_flonum */
#ifdef USE_REGEXP
#ifdef need_regexp
  init_builtin_regexp(ctx);
#endif /* need_regexp */
#endif

  /* calls init_buitin_global after gconsts is properly set up */
  init_builtin_global(ctx);
}
