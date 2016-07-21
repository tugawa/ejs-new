/*
   operations.c

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

/*
   adds two values slowly
   For details, see sect. 4.8.1.
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
    return cstr_to_string2(string_to_cstr(v1), string_to_cstr(v2));
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
   subtracts two values slowly
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
  return FIXNUM_ZERO;
}

JSValue slow_bitor(Context *context, JSValue v1, JSValue v2) {
  return FIXNUM_ZERO;
}

JSValue slow_lessthan(Context *context, JSValue v1, JSValue v2) {
  return FIXNUM_ZERO;
}

JSValue slow_lessthanequal(Context *context, JSValue v1, JSValue v2) {
  return FIXNUM_ZERO;
}
