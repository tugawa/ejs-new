#include "prefix.h"
#define EXTERN extern
#include "header.h"

static JSValue global;
static JSValue math;
#ifdef PARALLEL
static JSValue vmTest;
static JSValue thread;
static JSValue tcp;
#endif

void init_global_constants(void) {
  gobj.g_const_true = new_builtin(builtin_const_true, 0);
  gobj.g_const_false = new_builtin(builtin_const_false, 0);
  gobj.g_const_undefined = new_builtin(builtin_const_undefined, 0);
  gobj.g_const_null = new_builtin(builtin_const_null, 0);
  gobj.g_identity = new_builtin(builtin_identity, 1);
  gobj.g_object = new_builtin(object_constr, 0);
  gobj.g_array = new_builtin(array_constr, 0);

  gobj.g_object_proto = new_object();
  set_obj_prop(gobj.g_object_proto, "prototype", gobj.g_object_proto, ATTR_ALL);
  set_obj_prop(gobj.g_object_proto, "toString",
             new_builtin(object_toString, 0), ATTR_DE);

  // not implemented yet
  // set_obj_prop(gObjectProto, "hasOwnPropaty",
  //            new_builtin(objectProtoHasOwnPropaty, 0), ATTR_DE);

  gobj.g_function_proto = new_object();
#ifdef PARALLEL
  set_obj_prop(gobj.g_function_proto, "setAtomic", new_builtin(functionProtoSetAtomic, 0), ATTR_DE);
#endif

  gobj.g_fixnum_to_string = new_builtin(builtin_fixnum_to_string, 0);
  gobj.g_flonum_to_string = new_builtin(builtin_flonum_to_string, 0);
  gobj.g_string_to_index = new_builtin(builtin_string_to_index, 0);

  // strings
  gobj.g_string_valueof   = cstr_to_string("valueOf");
  gobj.g_string_prototype = cstr_to_string("prototype");
  gobj.g_string___proto__ = cstr_to_string("__proto__");
  gobj.g_string_tostring  = cstr_to_string("toString");
  gobj.g_string_boolean   = cstr_to_string("boolean");
  gobj.g_string_false     = cstr_to_string("false");
  gobj.g_string_null      = cstr_to_string("null");
  gobj.g_string_number    = cstr_to_string("number");
  gobj.g_string_object    = cstr_to_string("object");
  gobj.g_string_string    = cstr_to_string("string");
  gobj.g_string_true      = cstr_to_string("true");
  gobj.g_string_undefined = cstr_to_string("undefined");
  gobj.g_string_objtostr  = cstr_to_string("[object Object]");
  gobj.g_string_blank     = cstr_to_string("");
  gobj.g_string_comma     = cstr_to_string(",");

  // numbers
  gobj.g_flonum_infinity  = double_to_flonum(INFINITY);
  gobj.g_flonum_negative_infinity = double_to_flonum(-INFINITY);
  gobj.g_flonum_nan       = double_to_flonum(NAN);
}

#ifdef USE_REGEXP
#endif // USE_REGEXP

ObjGobjProp global_gobj_props[] = {
  { "Object",    &gobj.g_object,          ATTR_DE   },
  { "Array",     &gobj.g_array,           ATTR_DE   },
  { "NaN",       &gobj.g_flonum_nan,      ATTR_DDDE },
  { "Infinity",  &gobj.g_flonum_infinity, ATTR_DDDE },
  { "Number",    &gobj.g_number,          ATTR_DE   },
  { "String",    &gobj.g_string,          ATTR_DE   },
  { "Boolean",   &gobj.g_boolean,         ATTR_DE   },
  { "undefined", &gobj.g_const_undefined, ATTR_DDDE },
  { "Math",      &math,                   ATTR_DE   },
#ifdef PARALLEL
  { "VMTest",    &vmTest,                 ATTR_DE   },
  { "Thread",    &thread,                 ATTR_DE   },
  { "Tcp",       &tcp,                    ATTR_DE   },
#endif
  { NULL,        NULL,                    ATTR_DE   }
};

// initializes global objects
//
JSValue init_global(void) {
  global = new_object();
  math = new_object();
  gobj.g_number
    = new_builtin_with_constr(number_constr_nonew, number_constr, 1);
  gobj.g_string
    = new_builtin_with_constr(string_constr_nonew, string_constr, 1);
  gobj.g_boolean = new_builtin(boolean_constr, 1);
#ifdef USE_REGEXP
  gobj.g_regexp
    = new_builtin_with_constr(regexp_constr_nonew, regexp_constr, 2);
#endif
#ifdef PARALLEL
  vmTest = new_object();
  thread = new_object();
  tcp = new_object();
#endif

  init_builtin_global(global);
  {
    ObjGobjProp *p = global_gobj_props;
    while (p->name != NULL) {
      set_obj_prop(global, p->name, *(p->addr), p->attr);
      p++;
    }
  }

  init_builtin_array();
  init_builtin_number();
  init_builtin_string();
  init_builtin_boolean();
  init_builtin_math(math);
#ifdef USE_REGEXP
  init_builtin_regexp();
#endif

#ifdef PARALLEL
  set_obj_prop(g_object_proto, "setShared", new_builtin(objectProtoSetShared, 0), ATTR_DE);
  set_obj_prop(vmTest, "run", new_builtin(vmTestRun, 1), ATTR_DE);
  set_obj_prop(thread, "init", new_builtin(threadInit, 1), ATTR_DE);
  set_obj_prop(tcp, "init", new_builtin(tcpInit, 0), ATTR_DE);
#endif

  srand((unsigned)time(NULL));
  return global;
}
