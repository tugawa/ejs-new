/*
   init.c

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-17
     Hideya Iwasaki, 2016-17

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/

#include "prefix.h"
#define EXTERN extern
#include "header.h"

/*
   initilaizes global constants
 */
void init_global_constants(void) {
  int i;
  for (i = 0; i < sizeof(gconsts)/sizeof(JSValue); i++)
    ((JSValue *)&gconsts)[i] = JS_UNDEFINED;

  // string constants
  gconsts.g_string_prototype = cstr_to_string("prototype");
  gconsts.g_string___proto__ = cstr_to_string("__proto__");
  gconsts.g_string_tostring  = cstr_to_string("toString");
  gconsts.g_string_valueof   = cstr_to_string("valueOf");
  gconsts.g_string_boolean   = cstr_to_string("boolean");
  gconsts.g_string_number    = cstr_to_string("number");
  gconsts.g_string_object    = cstr_to_string("object");
  gconsts.g_string_string    = cstr_to_string("string");
  gconsts.g_string_true      = cstr_to_string("true");
  gconsts.g_string_false     = cstr_to_string("false");
  gconsts.g_string_null      = cstr_to_string("null");
  gconsts.g_string_undefined = cstr_to_string("undefined");
  gconsts.g_string_length    = cstr_to_string("length");
  gconsts.g_string_objtostr  = cstr_to_string("[object Object]");
  gconsts.g_string_empty     = cstr_to_string("");
  gconsts.g_string_comma     = cstr_to_string(",");
  gconsts.g_string_blank     = cstr_to_string(" ");

  // numbers
  gconsts.g_flonum_infinity  = double_to_flonum(INFINITY);
  gconsts.g_flonum_negative_infinity = double_to_flonum(-INFINITY);
  gconsts.g_flonum_nan       = double_to_flonum(NAN);
}

/*
   initilaizes global malloc-ed objects
 */
void init_global_malloc_objects(void) {
#ifdef HIDDEN_CLASS
  gobjects.g_hidden_class_0 =
    new_empty_hidden_class(NULL, HSIZE_NORMAL, HTYPE_TRANSIT);
#endif
}

/*
   initializes global objects
   2017/03/30: moved from init_global
 */
void init_global_objects(void) {
  /*
     It is necessary to make the object that will be set as Object.prototype,
     because this object is referred to in new_simple_object.
     Its `prototype' property is null.
  */
  gconsts.g_object_proto = new_big_predef_object_without_prototype(NULL);
  set_prototype_de(NULL, gconsts.g_object_proto, JS_NULL);
  gconsts.g_global = new_big_predef_object(NULL);
  gconsts.g_math = new_big_predef_object(NULL);

#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_object_proto", obj_hidden_class(gconsts.g_object_proto));
  print_hidden_class("g_global", obj_hidden_class(gconsts.g_global));
  print_hidden_class("g_math", obj_hidden_class(gconsts.g_math));
#endif
#endif

}

/*
   initializes builtin
 */
void init_builtin(Context *ctx) {
  init_builtin_object(ctx);
  init_builtin_array(ctx);
  init_builtin_number(ctx);
  init_builtin_string(ctx);
  init_builtin_boolean(ctx);
  init_builtin_math(ctx);
#ifdef USE_REGEXP
  init_builtin_regexp(ctx);
#endif

  // call init_buitin_global after gconsts is properly set up
  init_builtin_global(ctx);
}
