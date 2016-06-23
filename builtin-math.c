#include "prefix.h"
#define EXTERN extern
#include "header.h"

#define set_a_number(x) \
  (isnan((x))? gconsts.g_flonum_nan: \
   (is_fixnum_range_double((x))? set_a(context, double_to_fixnum((x))): \
                                 set_a(context, double_to_flonum((x)))))

void math_func(Context *context, double (*fn)(double)) {
  JSValue v;
  double x;

  builtin_prologue();
  v = args[1];
  if (!is_number(v)) v = to_number(context, v);
  if (is_nan(v)) {
    set_a(context, v);
    return;
  }
  // v is either fixnum or flonum
  x = is_fixnum(v)? fixnum_to_double(v): flonum_to_double(v);
  x = (*fn)(x);
  set_a_number(x);
}

void math_func2(Context *context, double (*fn)(double, double)) {
  JSValue v1, v2;
  double x1, x2;

  builtin_prologue();

  v1 = args[1];
  if (!is_number(v1)) v1 = to_number(context, v1);
  if (is_nan(v1)) {
    set_a(context, v1);
    return;
  }
  x1 = is_fixnum(v1)? fixnum_to_double(v1): flonum_to_double(v1);

  v2 = args[2];
  if (!is_number(v2)) v2 = to_number(context, v2);
  if (is_nan(v2)) {
    set_a(context, v2);
    return;
  }
  x2 = is_fixnum(v2)? fixnum_to_double(v2): flonum_to_double(v2);

  x1 = (*fn)(x1, x2);
  set_a_number(x1);
}

BUILTIN_FUNCTION(math_abs)
{
  math_func(context, &fabs);
}

BUILTIN_FUNCTION(math_sqrt)
{
  math_func(context, &sqrt);
}

BUILTIN_FUNCTION(math_sin)
{
  math_func(context, &sin);
}

BUILTIN_FUNCTION(math_cos)
{
  math_func(context, &cos);
}

BUILTIN_FUNCTION(math_tan)
{
  math_func(context, &tan);
}

BUILTIN_FUNCTION(math_asin)
{
  math_func(context, &asin);
}

BUILTIN_FUNCTION(math_acos)
{
  math_func(context, &asin);
}

BUILTIN_FUNCTION(math_atan)
{
  math_func(context, &atan);
}

// mathAtan2

BUILTIN_FUNCTION(math_atan2)
{
  math_func2(context, &atan2);
}

BUILTIN_FUNCTION(math_exp)
{
  math_func(context, &exp);
}

BUILTIN_FUNCTION(math_log)
{
  math_func(context, &log);
}

BUILTIN_FUNCTION(math_ceil)
{
  math_func(context, &ceil);
}

BUILTIN_FUNCTION(math_floor)
{
  math_func(context, &floor);
}

BUILTIN_FUNCTION(math_round)
{
  math_func(context, &round);
}

BUILTIN_FUNCTION(math_max)
{
  JSValue v;
  double x, r;
  int i;

  builtin_prologue();
  r = -INFINITY;
  for (i = 1; i <= na; i++) {
    v = args[i];
    if (!is_number(v)) v = to_number(context, v);
    if (is_nan(v)) r = NAN;
    // v is either fixnum or flonum
    x = is_fixnum(v)? fixnum_to_double(v): flonum_to_double(v);
    if (r < x) r = x;
  }
  set_a_number(r);
}

BUILTIN_FUNCTION(math_min)
{
  JSValue v;
  double x, r;
  int i;

  builtin_prologue();
  r = INFINITY;
  for (i = 1; i <= na; i++) {
    v = args[i];
    if (!is_number(v)) v = to_number(context, v);
    if (is_nan(v)) r = NAN;
    // v is either fixnum or flonum
    x = is_fixnum(v)? fixnum_to_double(v): flonum_to_double(v);
    if (x < r) r = x;
  }
  set_a_number(r);
}

BUILTIN_FUNCTION(math_pow)
{
  math_func2(context, pow);
}

BUILTIN_FUNCTION(math_random)
{
  double x;

  x = ((double)rand()) / (((double)RAND_MAX) + 1);
  set_a_number(x);
}

ObjBuiltinProp math_funcs[] = {
  { "abs",    math_abs,    1, ATTR_DE },
  { "sqrt",   math_sqrt,   1, ATTR_DE },
  { "sin",    math_sin,    1, ATTR_DE },
  { "cos",    math_cos,    1, ATTR_DE },
  { "tan",    math_tan,    1, ATTR_DE },
  { "asin",   math_asin,   1, ATTR_DE },
  { "acos",   math_acos,   1, ATTR_DE },
  { "atan",   math_atan,   1, ATTR_DE },
  { "atan2",  math_atan2,  2, ATTR_DE },
  { "exp",    math_exp,    1, ATTR_DE },
  { "log",    math_log,    1, ATTR_DE },
  { "ceil",   math_ceil,   1, ATTR_DE },
  { "floor",  math_floor,  1, ATTR_DE },
  { "round",  math_round,  1, ATTR_DE },
  { "max",    math_max,    0, ATTR_DE },
  { "min",    math_min,    0, ATTR_DE },
  { "pow",    math_pow,    2, ATTR_DE },
  { "random", math_random, 0, ATTR_DE },
  { NULL,     NULL,        0, ATTR_DE }
};

ObjDoubleProp math_values[] = {
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

void init_builtin_math(void)
{
  {
    ObjDoubleProp *p = math_values;
    while (p->name != NULL) {
      set_obj_prop(gconsts.g_math, p->name, double_to_flonum(p->value), p->attr);
      p++;
    }
  }
  {
    ObjBuiltinProp *p = math_funcs;
    while (p->name != NULL) {
      set_obj_prop(gconsts.g_math, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }

}
