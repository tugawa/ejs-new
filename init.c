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
             new_builtin(builtin_object_proto_to_string, 0), ATTR_DE);

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

typedef struct obj_builtin_prop {
  char *name;
  builtin_function_t fn;
  int na;
  Attribute attr;
} ObjBuiltinProp;

ObjBuiltinProp array_proto_props[] = {
  { "toString",       array_proto_toString,       0, ATTR_DE },
  { "toLocateString", array_proto_toLocaleString, 0, ATTR_DE },
//  { "join",           array_proto_join,           1, ATTR_DE },
//  { "concat",         array_proto_concat,         1, ATTR_DE },
//  { "pop",            array_proto_pop,            0, ATTR_DE },
//  { "push",           array_proto_push,           1, ATTR_DE },
//  { "reverse",        array_proto_reverse,        0, ATTR_DE },
//  { "shift",          array_proto_shift,          1, ATTR_DE },
//  { "slice",          array_proto_slice,          2, ATTR_DE },
//  { "sort",           array_proto_sort,           1, ATTR_DE },
  { NULL,             NULL,                       0, ATTR_DE }
};

ObjBuiltinProp number_proto_props[] = {
  { "valueOf",        number_proto_valueOf,       0, ATTR_DE },
  { "toString",       number_proto_toString,      0, ATTR_DE },
  { NULL,             NULL,                       0, ATTR_DE }
};

ObjBuiltinProp string_proto_props[] = {
  { "valueOf",        builtin_string_valueOf,     0, ATTR_DE },
//  { "toString",       builtin_string_toString     0, ATTR_DE },
//  { "concat",         string_proto_concat,        1, ATTR_DE },
//  { "toLowerCase",    string_proto_toLowerCase,   0, ATTR_DE },
//  { "toUpperCase",    string_proto_toUpperCase,   0, ATTR_DE },
//  { "substring",      string_proto_substring,     2, ATTR_DE },
//  { "slice",          string_proto_slice,         2, ATTR_DE },
//  { "charAt",         string_proto_charAt,        0, ATTR_DE },
//  { "charCodeAt",     string_proto_charCodeAt,    0, ATTR_DE },
//  { "indexOf",        string_proto_indexOf,       1, ATTR_DE },
//  { "lastIndexOf",    string_proto_lastIndexOf,   1, ATTR_DE },
//  { "localeCompare",  string_proto_localeCompare, 0, ATTR_DE },
  { NULL,             NULL,                       0, ATTR_DE }
};

ObjBuiltinProp boolean_proto_props[] = {
  { "valueOf",        builtin_boolean_valueOf,    0, ATTR_DE },
  { NULL,             NULL,                       0, ATTR_DE }
};

#ifdef USE_REGEXP
ObjBuiltinProp regex_proto_props[] = {
  { "exec",           regexp_proto_exec,          1, ATTR_DE },
  { "test",           regexp_proto_test,          1, ATTR_DE }
  { NULL,             NULL,                       0, ATTR_DE }
};
#endif // USE_REGEXP

ObjBuiltinProp global_builtin_props[] = {
  { "isNaN",          builtin_is_nan,             1, ATTR_DDDE },
  { "isFinite",       builtin_is_finite,          1, ATTR_DE   },
//  { "parseInt",       builtin_parseInt,           2, ATTR_DE   },
//  { "parseFloat",     builtin_parseFloat,         1, ATTR_DE   },
  { "print",          builtin_print,              0, ATTR_ALL  },
// for debugging
  { "printStatus",    builtin_printStatus,        0, ATTR_ALL  },
  { "address",        builtin_address,            0, ATTR_ALL  },
  { "hello",          builtin_hello,              0, ATTR_ALL  },
#ifdef USE_PAPI
  { "papi_get_real",  builtin_papi_get_real,      0, ATTR_ALL  },
#endif
  { NULL,             NULL,                       0, ATTR_DE   }
};

ObjBuiltinProp math_builtin_props[] = {
  { "abs",    math_abs,    1, ATTR_DE },
//  { "acos",   math_acos,   1, ATTR_DE },
//  { "asin",   math_asin,   1, ATTR_DE },
//  { "atan",   math_atan,   1, ATTR_DE },
//  { "atan2",  math_atan2,  2, ATTR_DE },
//  { "ceil",   math_ceil,   1, ATTR_DE },
//  { "cos",    math_cos,    1, ATTR_DE },
//  { "exp",    math_exp,    1, ATTR_DE },
//  { "floor",  math_floor,  1, ATTR_DE },
//  { "log",    math_log,    1, ATTR_DE },
//  { "max",    math_max,    0, ATTR_DE },
//  { "min",    math_mn,     0, ATTR_DE },
//  { "pow",    math_pow,    2, ATTR_DE },
//  { "random", math_random, 0, ATTR_DE },
//  { "round",  math_round,  1, ATTR_DE },
//  { "sin",    math_sin,    1, ATTR_DE },
//  { "sqrt",   math_sqrt,   1, ATTR_DE },
//  { "tan",    math_tan,    1, ATTR_DE },
  { NULL,     NULL,        0, ATTR_DE }
};

typedef struct obj_double_prop {
  char *name;
  double value;
  Attribute attr;
} ObjDoubleProp;

ObjDoubleProp number_value_props[] = {
  { "MAX_VALUE", DBL_MAX,               ATTR_ALL },
  { "MIN_VALUE", DBL_MIN,               ATTR_ALL },
  { NULL,        0.0,                   ATTR_ALL }
};

ObjDoubleProp math_value_props[] = {
  { "E",         2.7182818284590452354, ATTR_ALL },
  { "LN10",      2.302585092994046,     ATTR_ALL },
  { "LN2",       0.6931471805599453,    ATTR_ALL },
  { "LOG2E",     1.4426950408889634,    ATTR_ALL },
  { "LOG10E",    0.4342944819032518,    ATTR_ALL },
  { "PI",        3.1415926535897932,    ATTR_ALL },
  { "SQRT1_2",   0.7071067811865476,    ATTR_ALL },
  { "SQRT2",     1.4142135623730951,    ATTR_ALL },
  { NULL,        0.0,                   ATTR_ALL }
};

typedef struct obj_gobj_prop {
  char *name;
  JSValue *addr;
  Attribute attr;
} ObjGobjProp;

ObjGobjProp global_gobj_props[] = {
  { "Object",    &(gobj.g_object),          ATTR_DE   },
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

  // sets the global object's properties
  //
  {
    ObjBuiltinProp *p = global_builtin_props;
    while (p->name != NULL) {
      set_obj_prop(global, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
  {
    ObjGobjProp *p = global_gobj_props;
    while (p->name != NULL) {
      set_obj_prop(global, p->name, *(p->addr), p->attr);
      p++;
    }
  }

  // array
  gobj.g_array_proto = new_object();
  set_obj_prop(gobj.g_array, "prototype", gobj.g_array_proto, ATTR_ALL);
  {
    ObjBuiltinProp *p = array_proto_props;
    while (p->name != NULL) {
      set_obj_prop(gobj.g_array_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }

  // number
  //
  gobj.g_number_proto = new_number(FIXNUM_ZERO);
  set_obj_prop(gobj.g_number, "prototype", gobj.g_number_proto, ATTR_DE);
  set_obj_prop(gobj.g_number, "INFINITY", gobj.g_flonum_infinity, ATTR_ALL);
  set_obj_prop(gobj.g_number, "NEGATIVE_INFINITY",
             gobj.g_flonum_negative_infinity, ATTR_ALL);
  set_obj_prop(gobj.g_number, "NaN", gobj.g_flonum_nan, ATTR_ALL);
  set_obj_prop(gobj.g_number_proto, "__proto__", gobj.g_object_proto, ATTR_ALL);
  {
    ObjBuiltinProp *p = number_proto_props;
    while (p->name != NULL) {
      set_obj_prop(gobj.g_number_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
  {
    ObjDoubleProp *p = number_value_props;
    while (p->name != NULL) {
      set_obj_prop(gobj.g_number, p->name, double_to_flonum(p->value), p->attr);
      p++;
    }
  }

  // string
  //
  gobj.g_string_proto = new_string(gobj.g_string_blank);
  set_obj_prop(gobj.g_string, "prototype", gobj.g_string_proto, ATTR_ALL);
  // set_obj_prop(gobj.g_string, "fromCharCode", new_builtin(stringFromCharCode, 0), ATTR_DE);
  {
    ObjBuiltinProp *p = string_proto_props;
    while (p->name != NULL) {
      set_obj_prop(gobj.g_string_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }

  // boolean
  //
  gobj.g_boolean_proto = new_object();
  set_obj_prop(gobj.g_boolean, "prototype", gobj.g_boolean_proto, ATTR_ALL);
  {
    ObjBuiltinProp *p = boolean_proto_props;
    while (p->name != NULL) {
      set_obj_prop(gobj.g_boolean_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }

  // math
  {
    ObjDoubleProp *p = math_value_props;
    while (p->name != NULL) {
      set_obj_prop(math, p->name, double_to_flonum(p->value), p->attr);
      p++;
    }
  }
  {
    ObjBuiltinProp *p = math_builtin_props;
    while (p->name != NULL) {
      set_obj_prop(math, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }

  // RegExp
#ifdef USE_REGEXP
  gobj.g_regexp_proto = new_object();
  set_obj_prop(gobj.g_regexp, "prototype", gobj.g_regexp_proto, ATTR_ALL);
  set_obj_prop(gRegExpProto, "constructor", gRegExp, ATTR_DE);
  {
    ObjBuiltinProp *p = boolean_proto_props;
    while (p->name != NULL) {
      set_obj_prop(gobj.g_regexp_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
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
