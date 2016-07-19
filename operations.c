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

JSValue slow_sub(Context *context, JSValue v1, JSValue v2) {
  return FIXNUM_ZERO;
}

JSValue slow_mul(Context *context, JSValue v1, JSValue v2) {
  return FIXNUM_ZERO;
}

JSValue slow_mod(Context *context, JSValue v1, JSValue v2) {
  return FIXNUM_ZERO;
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
