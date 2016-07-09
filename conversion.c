#include "prefix.h"
#define EXTERN
#include "header.h"

#define type_error_exception(s)  LOG_EXIT("Type error exception: " s "\n")
#define type_error(s)  LOG_EXIT("Type error: " s "\n")

/*
  Data conversion rule of JavaScript

  source value  destination
                string       number     boolean     Object
  ---------------------------------------------------------------------
  undefined     "undefined"  NaN        false       TypeError exception
  null          "null"       0          false       TypeError exception
  ---------------------------------------------------------------------
  true          "true"       1          ---         new Boolean(true)
  false         "false"      0          ---         new Boolean(false)
  ---------------------------------------------------------------------
  ""            ---          0          false       new String("")
  "1.2"         ---          1.2        true        new String("1.2")
  "one"         ---          NaN        true        new String("one")
  ---------------------------------------------------------------------
  0             "0"          ---        false       new Number(0)
  -0            "0"          ---        false       new Number(-0)
  NaN           "NaN"        ---        false       new Number("NaN")
  Infinity      "Infinity"   ---        true        new Number("Infinity")
  -Infinity     "-Infinity"  ---        true        new Number("-Infinity")
  1             "1"          ---        true        new Number(1)
  ---------------------------------------------------------------------
  Object        *1           *2         true        ---
  []            ""           0          true        ---
  [9]           "9"          9          true        ---
  ['a']         use join()   NaN        true        ---
  Function      *1           NaN        true        ---
  ---------------------------------------------------------------------

  *1: object o -> string

      if (o has toString() method) {
        v = o.toString();
        if (v is a primitive value, i.e., either boolean, number, or string)
          return convert_to_string(v);
      }
      // o does not have toString or toString does not return a basic value
      if (o has valueOf() method) {
        v = o.valueOf():
        if (v is a primitive value, i.e., either boolean, number, or string)
          return convert_to_string(v);
      }
      throws TypeError exception

  *2: object o -> number

      if (o has valueOf() method) {
        v = o.valueOf():
        if (v is a basic value, i.e., either boolean, number, or string)
          return convert_to_number(v);
      }
      // o does not have valueOf or valueOf does not retuen a basic value
      if (o has toString() method) {
        v = o.toString();
        if (v is a basic value, i.e., either boolean, number, or string)
          return convert_to_number(v);
      }
      throws TypeError exception

 */

// JSValue to JSValue conversion functions

// converts a special value to a string
//
JSValue special_to_string(JSValue v) {
  switch (v) {
  case JS_UNDEFINED:
    return gconsts.g_string_undefined;
  case JS_NULL:
    return gconsts.g_string_null;
  case JS_TRUE:
    return gconsts.g_string_true;
  case JS_FALSE:
    return gconsts.g_string_false;
  default:
    type_error("special expected in special_to_string");
    return gconsts.g_string_empty;
  }
}

// convers a special value to a number
//
JSValue special_to_number(JSValue v) {
  switch (v) {
  case JS_UNDEFINED:
    return gconsts.g_flonum_nan;
  case JS_NULL:
  case JS_FALSE:
    return FIXNUM_ZERO;
  case JS_TRUE:
    return FIXNUM_ONE;
  default:
    type_error("special expected in special_to_number");
    return gconsts.g_flonum_nan;
  }
}

// convers a special value to a boolean
//
JSValue special_to_boolean(JSValue v) {
  switch (v) {
  case JS_UNDEFINED:
  case JS_NULL:
    return JS_FALSE;
  case JS_TRUE:
  case JS_FALSE:
    return v;
  default:
    type_error("special expected in special_to_boolean");
    return JS_FALSE;
  }
}

// converts a special value to an object
//
JSValue special_to_object(JSValue v) {
  switch (v) {
  case JS_UNDEFINED:
  case JS_NULL:
    type_error_exception("trying to convert undefined/null to an object");
    return JS_UNDEFINED;
  case JS_TRUE:
  case JS_FALSE:
    return new_boolean(v);
  default:
    type_error("special expected in special_to_object");
    return JS_UNDEFINED;
  }
}

// convers a string to a number
//
JSValue string_to_number(JSValue v) {
  char *p, *q;
  cint n;
  double d;

  if (! is_string(v)) {
    type_error("string expected in strint_to_number");
    return gconsts.g_flonum_nan;
  }
  p = string_value(v);
  if (p[0] == '\0')    // empty string
    return FIXNUM_ZERO;

  // try to read an integer from the string
  n = strtol(p, &q, 10);
  if (p != q) {
    if (*q == '\0') {
      // succeeded to convert to a long integer
      if (is_fixnum_range_cint(n))
        return cint_to_fixnum(n);
      else
        return double_to_flonum((double)n);
    }
  }
  d = strtod(p, &q);
  if (p != q) {
    if (*q == '\0')
      return double_to_flonum(d);
  }
  return gconsts.g_flonum_nan;
}

// converts a string to a boolean


//
JSValue string_to_boolean(JSValue v) {
  char *p;

  if (! is_string(v)) {
    type_error("string expected in string_to_boolean");
    return JS_FALSE;
  }
  p = string_value(v);
  return (p[0] == '\0')? JS_FALSE: JS_TRUE;
}

// converts a string to an Object
//
JSValue string_to_object(JSValue v) {
  if (! is_string(v)) {
    type_error("string expected in string_to_object");
    return JS_UNDEFINED;
  }
  return new_string(v);
}

#define BUFSIZE 1000

// converts a fixnum to a string
//
JSValue fixnum_to_string(JSValue v) {
  char buf[BUFSIZE];

  if (!is_fixnum(v)) {
    type_error("fixnum expected in fixnum_to_string");
    return gconsts.g_string_empty;
  }
  snprintf(buf, BUFSIZE, "%"PRId64, fixnum_to_cint(v));
  return cstr_to_string(buf);
}

// convers a flonum to a string
//
JSValue flonum_to_string(JSValue v) {
  double d;
  char buf[BUFSIZE];

  if (!is_flonum(v)) {
    type_error("flonum expected in flonum_to_string");
    return JS_UNDEFINED;
  }
  d = flonum_to_double(v);
  if (d == 0.0)
    return cstr_to_string("0");
  if (isnan(d))
    return cstr_to_string("NaN");
  if (isinf(d)) {
    if (d > 0) return cstr_to_string("Infinity");
    else return cstr_to_string("-Infinity");
  }
  snprintf(buf, BUFSIZE, "%.15g", d);
  return cstr_to_string(buf);
}

// converts a fixnum to a boolean
//
JSValue fixnum_to_boolean(JSValue v) {
  if (!is_fixnum(v)) {
    type_error("fixnum expected in fixnum_to_boolean");
    return JS_FALSE;
  }
  return (v == FIXNUM_ZERO)? JS_FALSE: JS_TRUE;
}

// converts a flonum to a boolean
//
JSValue flonum_to_boolean(JSValue v) {
  double d;

  if (!is_flonum(v)) {
    type_error("flonum expected in flonum_to_boolean");
    return JS_FALSE;
  }
  d = flonum_to_double(v);
  return isnan(d)? JS_FALSE: JS_TRUE;
}

// converts a fixnum to an object
//
JSValue fixnum_to_object(JSValue v) {
  if (!is_fixnum(v)) {
    type_error("fixnum expected in fixnum_to_object");
    return JS_UNDEFINED;
  }
  return new_number(v);
}

// converts a flonum to an object
//
JSValue flonum_to_object(JSValue v) {
  if (!is_flonum(v)) {
    type_error("flonum expected in flonum_to_object");
    return JS_UNDEFINED;
  }
  return new_number(v);
}

#if 0
double primitive_to_double(JSValue p) {
  uint64_t tag;
  double x;

  tag = get_tag(p);
  switch (tag) {
  case T_FIXNUM:
    return (double)fixnum_to_cint(p);
  case T_SPECIAL:
    x = special_to_double(p);
    goto TO_INT_FLO;
  case T_STRING:
    x = cstr_to_double(string_to_cstr(p));
    goto TO_INT_FLO;
  case T_FLONUM:
    x = flonum_to_double(p);
TO_INT_FLO:
    if (isnan(x))
      return (double)0.0;
    else
      return sign(x) * floor(fabs(x));
  default:
    LOG_EXIT("Argument is not a primitive.");
  }
}
#endif

JSValue primitive_to_string(JSValue p) {
  uint64_t tag;

  tag = get_tag(p);
  switch (tag) {
  case T_FIXNUM:
    return fixnum_to_string(p);
  case T_FLONUM:
    return flonum_to_string(p);
  case T_SPECIAL:
    return special_to_string(p);
  case T_STRING:
    return p;
  default:
    LOG_EXIT("cannot convert to string.");
  }
}

// converts an object to a string
//
JSValue object_to_string(Context *context, JSValue v) {
  JSValue f;

  if (!is_object(v)) {
    type_error("object expected in object_to_string");
    return gconsts.g_string_empty;
  }
  if (get_prop(v, gconsts.g_string_tostring, &f) == SUCCESS && is_function(f)) {
    f = call_method(v, f);
    if (is_string(f)) return f;
    if (is_fixnum(f)) return fixnum_to_string(f);
    if (is_flonum(f)) return flonum_to_string(f);
    if (is_boolean(f)) return special_to_string(f);
  }
  if (get_prop(v, gconsts.g_string_valueof, &f) == SUCCESS && is_function(f)) {
    f = call_method(v, f);
    if (is_string(f)) return f;
    if (is_fixnum(f)) return fixnum_to_string(f);
    if (is_flonum(f)) return flonum_to_string(f);
    if (is_boolean(f)) return special_to_string(f);
  }
  type_error_exception("neither toString nor valueOf returned a string in object_to_string");
  return gconsts.g_string_undefined;     // not reached
}

// converts an object to a number
//
JSValue object_to_number(Context *context, JSValue v) {
  JSValue f;

  if (!is_object(v)) {
    type_error("object expected in object_to_number");
    return gconsts.g_string_empty;
  }
  if (get_prop(v, gconsts.g_string_valueof, &f) == SUCCESS && is_function(f)) {
    f = call_method(v, f);
    if (is_number(f)) return f;
    if (is_string(f)) return string_to_number(f);
    if (is_boolean(f)) return special_to_number(f);
  }
  if (get_prop(v, gconsts.g_string_tostring, &f) == SUCCESS && is_function(f)) {
    f = call_method(v, f);
    if (is_number(f)) return f;
    if (is_string(f)) return string_to_number(f);
    if (is_boolean(f)) return special_to_number(f);
  }
  type_error_exception("neither valueOf nor toString returned a number in object_to_number");
  return FIXNUM_ZERO;       // not reached

  // not completed yet
  /*
  if (is_array(v)) {
    ArrayCell *p = remove_array_tag(v);
    if (array_size(p) == 0)    // empty array
      return FIXNUM_ZERO;
    if (array_size(p) == 1) {
      v = array_body_index(p, 0);
      if (is_number(v)) return v;
    }
  }
  return gconsts.g_flonum_nan;
  */
}

// converts an object to a boolean
//
JSValue object_to_boolean(JSValue v) {
  if (!is_object(v)) {
    type_error("object expected in object_to_boolean");
    return JS_FALSE;
  }
  return JS_TRUE;
}

// converts an array to a string
//
JSValue array_to_string(Context *context, JSValue array, JSValue separator)
{
  uint64_t length, seplen, sumlen;
  JSValue ret, item;
  char **strs;
  char *sep, *cstr, *p;
  ArrayCell *ap;
  int i;

  if (!is_array(array)) {
    type_error("array expected in array_to_string");
    return gconsts.g_string_empty;
  }
  ret = gconsts.g_string_empty;
  length = array_length(array);
  if (length <= 0)
    return ret;

  // length > 0

  strs = (char **)malloc(sizeof(char *) * length);
  sep = string_to_cstr(separator);
  seplen = strlen(sep);
  sumlen = 0;
  ap = remove_array_tag(array);

  for (i = 0; i < length; i++) {
    item = array_body_index(ap, i);
    strs[i] = string_to_cstr(to_string(context, item));
    sumlen += strlen(strs[i]);
  }

  cstr = (char *)malloc(sizeof(char) * (sumlen + (length - 1) * seplen + 1));

  for (i = 0, p = cstr; i < length; i++) {
    strcpy(p, strs[i]);
    p += strlen(strs[i]);
    if (i != length - 1) {
      strcpy(p, sep);
      p += seplen;
    }
  }
  *p = '\0';
  return cstr_to_string(cstr);
}

// general functions

// converts to a string
//
JSValue to_string(Context *context, JSValue v) {
  if (is_string(v)) return v;
  if (is_fixnum(v)) return fixnum_to_string(v);
  if (is_flonum(v)) return flonum_to_string(v);
  if (is_special(v)) return special_to_string(v);
  if (is_array(v)) return array_to_string(context, v, gconsts.g_string_comma);
  if (is_object(v)) return object_to_string(context, v);
  LOG_ERR("This cannot happen in to_string");
  return gconsts.g_string_undefined;
}

// converts to a boolean
//
JSValue to_boolean(JSValue v) {
  if (is_string(v)) return string_to_boolean(v);
  if (is_fixnum(v)) return fixnum_to_string(v);
  if (is_flonum(v)) return flonum_to_string(v);
  if (is_special(v)) return special_to_boolean(v);
  if (is_object(v)) return object_to_boolean(v);
  LOG_ERR("This cannot happen in to_boolean");
  return JS_FALSE;
}

// converts to a number
//
JSValue to_number(Context *context, JSValue v) {
  if (is_number(v)) return v;
  if (is_string(v)) return string_to_number(v);
  if (is_special(v)) return special_to_number(v);
  if (is_object(v)) return object_to_number(context, v);
  LOG_ERR("This cannot happen in to_number");
  return gconsts.g_flonum_nan;
}

// converts to an object
JSValue to_object(Context *context, JSValue v) {
  if (is_string(v)) return string_to_object(v);
  if (is_fixnum(v)) return fixnum_to_object(v);
  if (is_flonum(v)) return flonum_to_object(v);
  if (is_special(v)) return special_to_object(v);
  if (is_object(v)) return v;
  LOG_ERR("This cannot happen in to_object");
  return JS_UNDEFINED;
}

// converts to a C's double
// FIXIT: This implementation is not efficient.
//
double to_double(Context *context, JSValue v) {
  JSValue num;

  num = to_number(context, v);
  if (is_fixnum(num)) return fixnum_to_double(num);
  else if (is_flonum(num)) return flonum_to_double(num);
  else {
    LOG_ERR("This cannot happen in to_double");
    return gconsts.g_flonum_nan;
  }
}
