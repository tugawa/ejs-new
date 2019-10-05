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
BUILTIN_FUNCTION(builtin_parse_int)
{
  JSValue str, rad;
  char *cstr;
  char *endPtr;
  int32_t irad;
  cint ret;

  builtin_prologue();
  str = to_string(context, args[1]);
  GC_PUSH(str);
  rad = to_number(context, args[2]);
  GC_POP(str);
  cstr = string_to_cstr(str);

  if (!is_undefined(rad)) {
    if (is_fixnum(rad)) irad = fixnum_to_cint(rad);
    else if (is_flonum(rad)) irad = flonum_to_cint(rad);
    else irad = 10;
    if (irad < 2 || irad > 36) {
      set_a(context, gconsts.g_flonum_nan);
      return;
    }
  } else
    irad = 10;

  cstr = space_chomp(cstr);
  ret = strtol(cstr, &endPtr, irad);
  if (cstr == endPtr)
    set_a(context, gconsts.g_flonum_nan);
  else
    set_a(context, cint_to_number(context, ret));
}

#ifdef need_float
BUILTIN_FUNCTION(builtin_parse_float)
{
  JSValue str;
  char *cstr;
  double x;

  builtin_prologue();
  str = to_string(context, args[1]);
  cstr = string_to_cstr(str);
  cstr = space_chomp(cstr);

  x = strtod(cstr, NULL);
  if (is_fixnum_range_double(x))
    set_a(context, double_to_fixnum(x));
  else
    set_a(context, double_to_flonum(x));
}
#endif /* need_float */

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
          (FunctionTable *)regBase[-CF_POS],
          (int)regBase[-FP_POS],
          (int)regBase[-PC_POS],
          (void *)regBase[-LP_POS]);

  regBase = (JSValue*)(&(get_stack(context, regBase[-FP_POS] - 1)));
  LOG_ERR("\n-----prev spreg-----\ncf = %p\nfp = %d\npc = %d\nlp = %p\n",
          (FunctionTable *)regBase[-CF_POS],
          (int)regBase[-FP_POS],
          (int)regBase[-PC_POS],
          (void *)regBase[-LP_POS]);
}

/*
 * displays the address of an object
 */
BUILTIN_FUNCTION(builtin_address)
{
  JSValue obj;

  builtin_prologue();
  obj = args[1];
  printf("0x%"PRIx64"\n", obj);
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
  /*
  { "parseInt",       builtin_parseInt,           2, ATTR_DE   },
  { "parseFloat",     builtin_parseFloat,         1, ATTR_DE   },
  */
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
  { "Number",    &gconsts.g_ctor_Number,     ATTR_DE   },
  { "String",    &gconsts.g_ctor_String,     ATTR_DE   },
  { "Boolean",   &gconsts.g_ctor_Boolean,    ATTR_DE   },
#ifdef USE_REGEXP
  { "RegExp",    &gconsts.g_ctor_RegExp,     ATTR_DE   },
#endif /* USE_REGEXP */
  { "NaN",       &gconsts.g_flonum_nan,      ATTR_DDDE },
  { "Infinity",  &gconsts.g_flonum_infinity, ATTR_DDDE },
  { "Math",      &gconsts.g_math,            ATTR_DE   },
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
