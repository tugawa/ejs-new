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

#ifndef USE_TYPES_GENERATED

/*
 * adds two values slowly
 * For details, see sect. 4.8.1.
 */
JSValue slow_add(Context *context, JSValue v1, JSValue v2) {
  Tag tag;

  if (is_object(v1))
    v1 = object_to_string(context, v1);
  if (is_object(v2))
    v2 = object_to_string(context, v2);
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_STRFLO:
    v2 = flonum_to_string(v2);
    goto STRSTR;
  case TP_FLOSTR:
    v1 = flonum_to_string(v1);
    goto STRSTR;
  case TP_STRSPE:
    v2 = special_to_string(v2);
    goto STRSTR;
  case TP_SPESTR:
    v1 = special_to_string(v1);
    goto STRSTR;
  case TP_STRFIX:
    v2 = fixnum_to_string(v2);
    goto STRSTR;
  case TP_FIXSTR:
    v1 = fixnum_to_string(v1);
  case TP_STRSTR:
  STRSTR:
    return ejs_string_concat(context, v1, v2);
  case TP_FIXFIX:
    {
      cint sum = fixnum_to_cint(v1) + fixnum_to_cint(v2);
      return cint_to_number(sum);
    }
  default:
    {
      double x1, x2, sum;
      x1 = to_double(context, v1);
      x2 = to_double(context, v2);
      sum = x1 + x2;
      return double_to_number(sum);
    }
  }
}

/*
 * subtracts two values slowly
 */
JSValue slow_sub(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  double x1, x2, d;

  if (!is_number(v1)) v1 = to_number(context, v1);
  if (!is_number(v2)) v2 = to_number(context, v2);
  tag = TAG_PAIR(get_tag(v1), get_tag(v2));
  switch (tag) {
  case TP_FIXFIX:
    {
      cint s = fixnum_to_cint(v1) - fixnum_to_cint(v2);
      return cint_to_number(s);
    }
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      goto SUB_FLOFLO;
    }
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto SUB_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
    SUB_FLOFLO:
      d = x1 - x2;
      return double_to_number(d);
    }
    break;
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_mul(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  double x1, x2, d;

  if (!is_number(v1)) v1 = to_number(context, v1);
  if (!is_number(v2)) v2 = to_number(context, v2);
  tag = TAG_PAIR(get_tag(v1), get_tag(v2));
  switch (tag) {
  case TP_FIXFIX:
    {
      cint n1, n2, p;
      n1 = fixnum_to_cint(v1);
      n2 = fixnum_to_cint(v2);
      if (half_fixnum_range(n1) && half_fixnum_range(n2)) {
        p = n1 * n2;
        return cint_to_fixnum(p);
      } else {
        x1 = (double)n1;
        x2 = (double)n2;
        goto MUL_FLOFLO;
      }
    }
    break;
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      goto MUL_FLOFLO;
    }
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto MUL_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
    MUL_FLOFLO:
      d = x1 * x2;
      return double_to_number(d);
    }
    break;
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_div(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  double x1, x2, d;

  if (!is_number(v1)) v1 = to_number(context, v1);
  if (!is_number(v2)) v2 = to_number(context, v2);
  tag = TAG_PAIR(get_tag(v1), get_tag(v2));
  switch (tag) {
  case TP_FIXFIX:
    {
      int n1, n2, s;
      n1 = fixnum_to_cint(v1);
      if (v2 == FIXNUM_ZERO) {
        if (n1 > 0) return gconsts.g_flonum_infinity;
        else if (n1 == 0) return gconsts.g_flonum_nan;
        else return gconsts.g_flonum_negative_infinity;
      } else {
        n2 = fixnum_to_cint(v2);
        s = n1 / n2;
        return (n1 == n2 * s)? cint_to_fixnum(s):
          double_to_flonum((double)n1 / (double)n2);
      }
    }
    break;
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      goto DIV_FLOFLO;
    }
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto DIV_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
    DIV_FLOFLO:
      d = x1 / x2;
      if (isinf(d)) return d > 0? gconsts.g_flonum_infinity:
                      gconsts.g_flonum_negative_infinity;
      else if (isnan(d)) return gconsts.g_flonum_nan;
      else return double_to_number(d);
    }
    break;
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_mod(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  double x1, x2, d;

  if (!is_number(v1)) v1 = to_number(context, v1);
  if (!is_number(v2)) v2 = to_number(context, v2);
  tag = TAG_PAIR(get_tag(v1), get_tag(v2));
  switch (tag) {
  case TP_FIXFIX:
    return (v2 == FIXNUM_ZERO)? gconsts.g_flonum_nan:
    cint_to_number(fixnum_to_cint(v1) % fixnum_to_cint(v2));
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      goto MOD_FLOFLO;
    }
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto MOD_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
    MOD_FLOFLO:
      if (isinf(x1) || x2 == 0.0f) return gconsts.g_flonum_nan;
      else {
        d = x1 / x2;
        d = d >= 0 ? floor(d) : ceil(d);
        d = x1 - d * x2;
        return double_to_number(d);
      }
    }
    break;
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_bitand(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  cint x1, x2;

  if (!is_number(v1)) v1 = to_number(context, v1);
  if (!is_number(v2)) v2 = to_number(context, v2);
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    return v1 & v2;
  case TP_FIXFLO:
    x1 = fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum(x1 & x2);
  case TP_FLOFIX:
    x1 = flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum(x1 & x2);
  case TP_FLOFLO:
    x1 = flonum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum(x1 & x2);
    break;
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_bitor(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  cint x1, x2;

  if (!is_number(v1)) v1 = to_number(context, v1);
  if (!is_number(v2)) v2 = to_number(context, v2);
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    return v1 | v2;
  case TP_FIXFLO:
    x1 = fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum(x1 | x2);
  case TP_FLOFIX:
    x1 = flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum(x1 | x2);
  case TP_FLOFLO:
    x1 = flonum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum(x1 | x2);
    break;
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_leftshift(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  int32_t x1;
  cint x2;

  if (!is_number(v1)) {
    v1 = to_number(context, v1);
    if (v1 == gconsts.g_flonum_infinity ||
        v1 == gconsts.g_flonum_negative_infinity ||
        v1 == gconsts.g_flonum_nan) {
      v1 = FIXNUM_ZERO;
    }
  }
  if (!is_number(v2)) v2 = to_number(context, v2);
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 << x2));
  case TP_FIXFLO:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 << x2));
  case TP_FLOFIX:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 << x2));
  case TP_FLOFLO:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 << x2));
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_rightshift(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  int32_t x1;
  cint x2;

  if (!is_number(v1)) {
    v1 = to_number(context, v1);
    if (v1 == gconsts.g_flonum_infinity ||
        v1 == gconsts.g_flonum_negative_infinity ||
        v1 == gconsts.g_flonum_nan) {
      v1 = FIXNUM_ZERO;
    }
  }
  if (!is_number(v2)) v2 = to_number(context, v2);
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  case TP_FIXFLO:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  case TP_FLOFIX:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  case TP_FLOFLO:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_unsignedrightshift(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  uint32_t x1;
  cint x2;

  if (!is_number(v1)) v1 = to_number(context, v1);
  if (!is_number(v2)) v2 = to_number(context, v2);
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    x1 = (uint32_t)fixnum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  case TP_FIXFLO:
    x1 = (uint32_t)fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  case TP_FLOFIX:
    x1 = (uint32_t)flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  case TP_FLOFLO:
    x1 = (uint32_t)flonum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    return cint_to_fixnum((cint)(x1 >> x2));
  default:
    return gconsts.g_flonum_nan;
  }
}

JSValue slow_lessthan(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  double x1, x2;

  if (is_object(v1)) v1 = object_to_primitive(context, v1, HINT_NUMBER);
  if (is_special(v1)) v1 = special_to_number(v1);
  if (is_object(v2)) v2 = object_to_primitive(context, v2, HINT_NUMBER);
  if (is_special(v2)) v2 = special_to_number(v2);
 LTAGAIN:
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    return true_false((int64_t)v1 < (int64_t)v2);
  case TP_FIXFLO:
    x1 = fixnum_to_double(v1);
    x2 = flonum_to_double(v2);
    return true_false(x1 < x2);
  case TP_FLOFIX:
    x1 = flonum_to_double(v1);
    x2 = fixnum_to_double(v2);
    return true_false(x1 < x2);
  case TP_FLOFLO:
    x1 = flonum_to_double(v1);
    x2 = flonum_to_double(v2);
    return true_false(x1 < x2);
  case TP_STRSTR:
    return true_false(strcmp(string_to_cstr(v1), string_to_cstr(v2)) < 0);
  default:
    if (is_string(v1)) v1 = string_to_number(v1);
    if (is_string(v2)) v2 = string_to_number(v2);
    goto LTAGAIN;
  }
}

JSValue slow_lessthanequal(Context *context, JSValue v1, JSValue v2) {
  Tag tag;
  double x1, x2;

  if (is_object(v1)) v1 = object_to_primitive(context, v1, HINT_NUMBER);
  if (is_special(v1)) v1 = special_to_number(v1);
  if (is_object(v2)) v2 = object_to_primitive(context, v2, HINT_NUMBER);
  if (is_special(v2)) v2 = special_to_number(v2);
 LEAGAIN:
  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    return true_false((int64_t)v1 <= (int64_t)v2);
  case TP_FIXFLO:
    x1 = fixnum_to_double(v1);
    x2 = flonum_to_double(v2);
    return true_false(x1 <= x2);
  case TP_FLOFIX:
    x1 = flonum_to_double(v1);
    x2 = fixnum_to_double(v2);
    return true_false(x1 <= x2);
  case TP_FLOFLO:
    x1 = flonum_to_double(v1);
    x2 = flonum_to_double(v2);
    return true_false(x1 <= x2);
  case TP_STRSTR:
    return true_false(strcmp(string_to_cstr(v1), string_to_cstr(v2)) <= 0);
  default:
    if (is_string(v1)) v1 = string_to_number(v1);
    if (is_string(v2)) v2 = string_to_number(v2);
    goto LEAGAIN;
  }
}

#endif /* USE_TYPES_GENERATED */
