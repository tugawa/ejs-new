/*
   equal dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 == $r2
 */
{
  Register dst = r0;
  JSValue ret;
  Tag tag;
  double x1, x2;

  if (v1 == v2)
    ret = false_true(is_nan(v1));
  else {
    ret = JS_FALSE;
    tag = TAG_PAIR(get_tag(v1), get_tag(v2));
    switch (tag) {
    case TP_FIXFIX:
    case TP_FIXFLO:
    case TP_FLOFIX:
    case TP_STRSTR:
    case TP_SPEFLO:
    case TP_FLOSPE:
    case TP_OBJOBJ:
      ret = JS_FALSE;
      break;
    case TP_FLOFLO:
FLOFLO:
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
      ret = true_false(x1 == x2);
      break;
    case TP_SPESPE:
      ret = true_false((is_undefined(v1) && is_null(v2)) ||
                       (is_null(v1) && is_undefined(v2)));
      break;
    case TP_STRFIX:
STRFIX:
      v1 = string_to_number(v1);
      ret = true_false(v1 == v2);
      break;
    case TP_STRFLO:
STRFLO:
      v1 = string_to_number(v1);
      if (is_flonum(v1)) goto FLOFLO;
      ret = JS_FALSE;
      break;
    case TP_FIXSTR:
FIXSTR:
      v2 = string_to_number(v2);
      ret = true_false(v1 == v2);
      break;
    case TP_FLOSTR:
FLOSTR:
      v2 = string_to_number(v2);
      if (is_flonum(v2)) goto FLOFLO;
      ret = JS_FALSE;
      break;
    case TP_SPEFIX:
      if (v1 == JS_UNDEFINED) ret = JS_FALSE;
      else if (v1 == JS_NULL) ret = JS_FALSE;
      else {
        if (v1 == JS_TRUE) v1 = FIXNUM_ONE;
        else if (v1 == JS_FALSE) v1 = FIXNUM_ZERO;
        ret = true_false(v1 == v2);
      }
      break;
    case TP_FIXSPE:
      if (v2 == JS_UNDEFINED) ret = JS_FALSE;
      else if (v2 == JS_NULL) ret = JS_FALSE;
      else {
        if (v2 == JS_TRUE) v2 = FIXNUM_ONE;
        else if (v2 == JS_FALSE) v2 = FIXNUM_ZERO;
        ret = true_false(v1 == v2);
      }
      break;
    case TP_SPESTR:
      if (v1 == JS_UNDEFINED) ret = JS_FALSE;
      else if (v1 == JS_NULL) ret = JS_FALSE;
      else {
SPESTR:
        if (v1 == JS_TRUE) v1 = FIXNUM_ONE;
        else if (v1 == JS_FALSE) v1 = FIXNUM_ZERO;
        goto FIXSTR;
      }
      break;
    case TP_STRSPE:
      if (v2 == JS_UNDEFINED) ret = JS_FALSE;
      else if (v2 == JS_NULL) ret = JS_FALSE;
      else {
STRSPE:
        if (v2 == JS_TRUE) v2 = FIXNUM_ONE;
        else if (v2 == JS_FALSE) v2 = FIXNUM_ZERO;
        goto STRFIX;
      }
      break;
    case TP_OBJFIX:
      v1 = object_to_primitive(context, v1, HINT_NUMBER);
      ret = true_false(v1 == v2);
      break;
    case TP_FIXOBJ:
      v2 = object_to_primitive(context, v2, HINT_NUMBER);
      ret = true_false(v1 == v2);
      break;
    case TP_OBJFLO:
      v1 = object_to_primitive(context, v1, HINT_NUMBER);
      if (is_flonum(v1)) goto FLOFLO;
      ret = JS_FALSE;
      break;
    case TP_FLOOBJ:
      v2 = object_to_primitive(context, v2, HINT_NUMBER);
      if (is_flonum(v2)) goto FLOFLO;
      ret = JS_FALSE;
      break;
    case TP_OBJSTR:
      v1 = object_to_primitive(context, v1, HINT_NUMBER);
      if (is_fixnum(v1)) goto FIXSTR;
      else if (is_flonum(v1)) goto FLOSTR;
      else if (is_string(v1)) ret = true_false(v1 == v2);
      else if (is_boolean(v1)) goto SPESTR;
      break;
    case TP_STROBJ:
      v2 = object_to_primitive(context, v2, HINT_NUMBER);
      if (is_fixnum(v2)) goto STRFIX;
      else if (is_flonum(v2)) goto STRFLO;
      else if (is_string(v2)) ret = true_false(v1 == v2);
      else if (is_boolean(v2)) goto STRSPE;
      break;
    case TP_OBJSPE:
      v1 = object_to_primitive(context, v1, HINT_NUMBER);
      if (is_number(v1)) ret = JS_FALSE;
      else if (is_string(v1)) goto STRSPE;
      else if (is_boolean(v1)) ret = true_false(v1 == v2);
        ret = true_false(v1 == v2);
      break;
    case TP_SPEOBJ:
      v2 = object_to_primitive(context, v2, HINT_NUMBER);
      if (is_number(v2)) ret = JS_FALSE;
      else if (is_string(v2)) goto SPESTR;
      else if (is_boolean(v2)) ret = true_false(v1 == v2);
      break;
    }
  }
  regbase[dst] = ret;
}
