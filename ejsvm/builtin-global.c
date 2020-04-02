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
 * isNAN
 */
BUILTIN_FUNCTION(builtin_isNaN)
{
  double d;

  builtin_prologue();
  d = to_double(context, args[1]);
  set_a(context, true_false(isnan(d)));
}

/*
 * isFinite
 */
BUILTIN_FUNCTION(builtin_isFinite)
{
  double d;

  builtin_prologue();
  d = to_double(context, args[1]);
  set_a(context, true_false(isinf(d)));
}

/*
 * parseInt str rad
 * converts a string to a number
 */
BUILTIN_FUNCTION(builtin_parseInt)
{
  JSValue str, rad, ret;
  const char *cstr;
  int32_t irad;

  builtin_prologue();
  str = na >= 1 ? args[1] : gconsts.g_string_empty;
  rad = na >= 2 ? args[2] : JS_UNDEFINED;

  GC_PUSH2(str, rad);

  str = to_string(context, str);
  if (!is_string(str))
    goto return_nan;
  cstr = string_to_cstr(str);

  if (rad == JS_UNDEFINED)
    irad = PARSE_INT_RADIX_AUTO;
  else {
    rad = to_number(context, rad);
    if (is_undefined(rad))
      irad = PARSE_INT_RADIX_AUTO;
    else if (is_number(rad)) {
      irad = number_to_cint(rad);
      if (irad == 0)
	irad = PARSE_INT_RADIX_AUTO;
      else if (irad < 2 || irad > 36)
	goto return_nan;
    } else
      irad = 10;
  }

  ret = cstr_parse_int(context, cstr, irad);
  set_a(context, ret);
  GC_POP2(rad, str);
  return;

 return_nan:
  set_a(context, gconsts.g_flonum_nan);
  GC_POP2(rad, str);
  return;
}

BUILTIN_FUNCTION(builtin_parseFloat)
{
  JSValue str, ret;
  const char *cstr;

  builtin_prologue();
  if (na == 0)
    goto return_nan;
  str = to_string(context, args[1]);
  if (!is_string(str))
    goto return_nan;
  cstr = string_to_cstr(str);
  ret = cstr_parse_float(context, cstr);
  set_a(context, ret);
  return;

 return_nan:
  set_a(context, gconsts.g_flonum_nan);
  return;
}

/*
 * throws Error because it is not a constructor
 */
BUILTIN_FUNCTION(builtin_not_a_constructor)
{
  LOG_EXIT("Not a constructor");
}

/*
 * print
 */
BUILTIN_FUNCTION(builtin_print)
{
  int i;

  builtin_prologue();
  /* printf("builtin_print: na = %d, fp = %p, args = %p\n", na, fp, args); */

  for (i = 1; i <= na; ++i) {
    /* printf("args[%d] = %016lx\n", i, args[i]); */
    print_value_simple(context, args[i]);
    putchar(i < na ? ' ' : '\n');
  }
  set_a(context, JS_UNDEFINED);
}

/*
 * printv
 */
BUILTIN_FUNCTION(builtin_printv)
{
  int i;

  builtin_prologue();
  /* printf("builtin_print: na = %d, fp = %p, args = %p\n", na, fp, args); */

  for (i = 1; i <= na; ++i) {
    /* printf("args[%d] = %016lx\n", i, args[i]); */
    print_value_verbose(context, args[i]);
    putchar(i < na ? ' ' : '\n');
  }
  set_a(context, JS_UNDEFINED);
}

/*
 * displays the status
 */
BUILTIN_FUNCTION(builtin_printStatus)
{
  /* int fp; */
  JSValue *regBase;

  /* fp = get_fp(context); */
  regBase = (JSValue*)(&(get_stack(context, fp-1)));
  LOG_ERR("\n-----current spreg-----\ncf = %p\nfp = %d\npc = %d\nlp = %p\n",
          (FunctionTable *) jsv_to_noheap_ptr(regBase[-CF_POS]),
          (int) (intjsv_t) regBase[-FP_POS],
          (int) (intjsv_t) regBase[-PC_POS],
          jsv_to_function_frame(regBase[-LP_POS]));

  regBase = (JSValue*)(&(get_stack(context, regBase[-FP_POS] - 1)));
  LOG_ERR("\n-----prev spreg-----\ncf = %p\nfp = %d\npc = %d\nlp = %p\n",
          (FunctionTable *) jsv_to_noheap_ptr(regBase[-CF_POS]),
          (int) (intjsv_t) regBase[-FP_POS],
          (int) (intjsv_t) regBase[-PC_POS],
          jsv_to_function_frame(regBase[-LP_POS]));
}

/*
 * displays the address of an object
 */
BUILTIN_FUNCTION(builtin_address)
{
  JSValue obj;

  builtin_prologue();
  obj = args[1];
  printf("0x%"PRIJSValue"\n", obj);
  set_a(context, JS_UNDEFINED);
}

/*
 * prints ``hello, world''
 */
BUILTIN_FUNCTION(builtin_hello)
{
  LOG("hello, world\n");
  set_a(context, JS_UNDEFINED);
}

BUILTIN_FUNCTION(builtin_to_string)
{
  builtin_prologue();
  set_a(context, to_string(context, args[1]));
}

BUILTIN_FUNCTION(builtin_to_number)
{
  builtin_prologue();
  set_a(context, to_number(context, args[1]));
}


#ifdef USE_PAPI
/*
 * obtains the real usec
 */
BUILTIN_FUNCTION(builtin_papi_get_real)
{
  long long now = PAPI_get_real_usec();
  set_a(context, int_to_fixnum(now));
}
#endif /* USE_PAPI */

/*
 * property table
 */
/* instance */
ObjBuiltinProp Global_builtin_props[] = {
  { "isNaN",          builtin_isNaN,              1, ATTR_DDDE },
  { "isFinite",       builtin_isFinite,           1, ATTR_DE   },
  { "parseInt",       builtin_parseInt,          2, ATTR_DE   },
  { "parseFloat",     builtin_parseFloat,        1, ATTR_DE   },
  { "print",          builtin_print,              0, ATTR_ALL  },
  { "printv",         builtin_printv,             0, ATTR_ALL  },
  { "printStatus",    builtin_printStatus,        0, ATTR_ALL  },
  { "address",        builtin_address,            0, ATTR_ALL  },
  { "hello",          builtin_hello,              0, ATTR_ALL  },
  { "to_string",      builtin_to_string,          1, ATTR_ALL  },
  { "to_number",      builtin_to_number,          1, ATTR_ALL  },
#ifdef USE_PAPI
  { "papi_get_real",  builtin_papi_get_real,      0, ATTR_ALL  },
#endif
};
ObjDoubleProp  Global_double_props[] = {};
ObjGconstsProp Global_gconsts_props[] = {
  { "Object",    &gconsts.g_ctor_Object,     ATTR_DE   },
  { "Array",     &gconsts.g_ctor_Array,      ATTR_DE   },
  { "Function",  &gconsts.g_ctor_Function,   ATTR_DE   },
  { "Number",    &gconsts.g_ctor_Number,     ATTR_DE   },
  { "String",    &gconsts.g_ctor_String,     ATTR_DE   },
  { "Boolean",   &gconsts.g_ctor_Boolean,    ATTR_DE   },
#ifdef USE_REGEXP
  { "RegExp",    &gconsts.g_ctor_RegExp,     ATTR_DE   },
#endif /* USE_REGEXP */
  { "NaN",       &gconsts.g_flonum_nan,      ATTR_DDDE },
  { "Infinity",  &gconsts.g_flonum_infinity, ATTR_DDDE },
  { "Math",      &gconsts.g_math,            ATTR_DE   },
  { "performance", &gconsts.g_performance,   ATTR_DE   },
  { "true",      &gconsts.g_boolean_true,    ATTR_DE   },
  { "false",     &gconsts.g_boolean_false,   ATTR_DE   },
  { "null",      &gconsts.g_null,            ATTR_DE   },
  { "undefined", &gconsts.g_undefined,       ATTR_DE   },
};
DEFINE_PROPERTY_TABLE_SIZES_I(Global);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
