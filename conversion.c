#include "prefix.h"
#define EXTERN
#include "header.h"

#define type_error()  LOG_EXIT("Type error")

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
      // o does not have toString or toString does not retuen a basic value
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
    LOG_ERR("Special expected in special_to_string");
    return JS_UNDEFINED;
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
    LOG_ERR("Special expected in special_to_number");
    return JS_UNDEFINED;
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
    LOG_ERR("Special expected in special_to_boolean");
    return JS_UNDEFINED;
  }
}

// converts a special value to an object
//
JSValue special_to_object(JSValue v) {
  switch (v) {
  case JS_UNDEFINED:
  case JS_NULL:
    type_error();
    return JS_UNDEFINED;
  case JS_TRUE:
  case JS_FALSE:
    return new_boolean(v);
  default:
    LOG_ERR("Special expected in special_to_object");
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
    LOG_ERR("Not a string in strint_to_number");
    return JS_UNDEFINED;
  }
  p = string_value(v);
  if (p[0] == '\0')    // empty string
    return FIXNUM_ZERO;

  // try to read an integer from the string
  n = strtol(p, &q, 10);
  if (p != q) {
    if (*q == '\0') {
      // succeeded to convert to a long integer
      if (MIN_FIXNUM_CINT <= n && n <= MAX_FIXNUM_CINT)
        return cint_to_fixnum(n);
      else {
        d = (double)n;
        return double_to_fixnum(d);
      }
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
    LOG_ERR("Not a string in string_to_boolean");
    return JS_UNDEFINED;
  }
  p = string_value(v);
  return (p[0] == '\0')? JS_FALSE: JS_TRUE;
}

// converts a string to an Object
//
JSValue string_to_object(JSValue v) {
  if (! is_string(v)) {
    LOG_ERR("Not a string in string_to_object");
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
    LOG_ERR("Not a fixnum in fixnum_to_string");
    return JS_UNDEFINED;
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
    LOG_ERR("Not a flonum in flonum_to_string");
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
    LOG_ERR("Not a fixnum in fixnum_to_boolean");
    return JS_UNDEFINED;
  }
  if (v == FIXNUM_ZERO) return JS_FALSE;
  else return JS_TRUE;
}

// converts a flonum to a boolean
//
JSValue flonum_to_boolean(JSValue v) {
  double d;

  if (!is_flonum(v)) {
    LOG_ERR("Not a flonum in flonum_to_boolean");
    return JS_UNDEFINED;
  }
  d = flonum_to_double(v);
  return isnan(d)? JS_FALSE: JS_TRUE;
}

// converts a fixnum to an object
//
JSValue fixnum_to_object(JSValue v) {
  if (!is_fixnum(v)) {
    LOG_ERR("Not a fixnum in fixnum_to_object");
    return JS_UNDEFINED;
  }
  return new_number(v);
}

// converts a flonum to an object
//
JSValue flonum_to_object(JSValue v) {
  if (!is_flonum(v)) {
    LOG_ERR("Not a flonum in flonum_to_object");
    return JS_UNDEFINED;
  }
  return new_number(v);
}

// converts an object to a string
//
JSValue object_to_string(Context *context, JSValue v) {
  JSValue f;

  if (!is_object(v))
    return JS_UNDEFINED;
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
  // type_error();
  return gconsts.g_string_undefined;
}

// converts an object to a number
//
JSValue object_to_number(Context *context, JSValue v) {
  JSValue f;

  if (!is_object(v))
    return JS_UNDEFINED;
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
  type_error();
  return FIXNUM_ZERO;

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
  return JS_TRUE;
}

// general functions

// converts to an object
JSValue to_object(Context *context, JSValue v) {
  if (is_string(v)) return string_to_object(v);
  if (is_fixnum(v)) return fixnum_to_object(v);
  if (is_flonum(v)) return flonum_to_object(v);
  if (is_special(v)) return special_to_object(v);
  if (is_object(v)) return v;
  LOG_ERR("This cannot happen in to_string");
  return JS_UNDEFINED;
}

// converts to a string
//
JSValue to_string(Context *context, JSValue v) {
  if (is_string(v)) return v;
  if (is_fixnum(v)) return fixnum_to_string(v);
  if (is_flonum(v)) return flonum_to_string(v);
  if (is_special(v)) return special_to_string(v);
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
