/*
   init.c

   SSJS Project at the University of Electro-communications

   Sho Takada, 2012-13
   Akira Tanimura, 2012-13
   Akihiro Urushihara, 2013-14
   Ryota Fujii, 2013-14
   Tomoharu Ugawa, 2013-16
   Hideya Iwasaki, 2013-16
*/

#include "prefix.h"
#define EXTERN extern
#include "header.h"

#ifdef PARALLEL
static JSValue vmTest;
static JSValue thread;
static JSValue tcp;
#endif

/*
   initilaizes global constants
 */
void init_global_constants(void) {
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

  // Why the following constants are necessary?
  // gconsts.g_const_true = new_builtin(builtin_const_true, 0);
  // gconsts.g_const_false = new_builtin(builtin_const_false, 0);
  // gconsts.g_const_undefined = new_builtin(builtin_const_undefined, 0);
  // gconsts.g_const_null = new_builtin(builtin_const_null, 0);
  // gconsts.g_identity = new_builtin(builtin_identity, 1);
  // gconsts.g_fixnum_to_string = new_builtin(builtin_fixnum_to_string, 0);
  // gconsts.g_flonum_to_string = new_builtin(builtin_flonum_to_string, 0);
  // gconsts.g_string_to_index = new_builtin(builtin_string_to_index, 0);
}

/*
   initializes global objects
 */
JSValue init_global(void) {
  /*
     First, it is necessary to make the object that will be set as
     Object.prototype, because this object is referred to in new_object.
     Its __proto__ property is null.
  */
  gconsts.g_object_proto = new_object_without_prototype(NULL);
  set_prop_de(gconsts.g_object_proto, gconsts.g_string_prototype, JS_NULL);

  gconsts.g_global = new_object(NULL);
  gconsts.g_math = new_object(NULL);
#ifdef USE_REGEXP
  // gconsts.g_regexp
  //   = new_builtin_with_constr(regexp_constr_nonew, regexp_constr, 2);
#endif
#ifdef PARALLEL
  vmTest = new_object();
  thread = new_object();
  tcp = new_object();
#endif

  init_builtin_object();
  init_builtin_array();
  init_builtin_number();
  init_builtin_string();
  init_builtin_boolean();
  init_builtin_math();
#ifdef USE_REGEXP
  init_builtin_regexp();
#endif

#ifdef PARALLEL
  set_obj_cstr_prop(gconsts.vmTest, "run", new_builtin(vmTestRun, 1), ATTR_DE);
  set_obj_cstr_prop(gconsts.thread, "init", new_builtin(threadInit, 1), ATTR_DE);
  set_obj_cstr_prop(gconsts.tcp, "init", new_builtin(tcpInit, 0), ATTR_DE);
#endif

  // call init_buitin_global after gconsts is properly set up
  init_builtin_global();

  srand((unsigned)time(NULL));
  return gconsts.g_global;
}
