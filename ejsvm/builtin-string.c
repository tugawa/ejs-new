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
#include <limits.h>

#define not_implemented(s)                                              \
  LOG_EXIT("%s is not implemented yet\n", (s)); set_a(context, JS_UNDEFINED)

#define type_error_exception(s)  LOG_EXIT("%s\n", s)

#define mallocstr(n) ((char *)malloc(sizeof(char) * ((n) + 1)))

/*
 * constrcutor of a string
 */
BUILTIN_FUNCTION(string_constr)
{
  JSValue rsv;

  builtin_prologue();
  /* printf("In string_constr\n"); */
  rsv =
    new_string_object(context, DEBUG_NAME("string_constr"),
                      gshapes.g_shape_String,
                      na > 0? args[1]: gconsts.g_string_empty);
  set_a(context, rsv);
}

/*
 * constrcutor of a string (not Object)
 */
BUILTIN_FUNCTION(string_constr_nonew)
{
  JSValue arg;
  
  builtin_prologue();
  if (na > 0)
    arg = to_string(context, args[1]);
  else
    arg = gconsts.g_string_empty;
  set_a(context, arg);
}

BUILTIN_FUNCTION(string_valueOf)
{
  JSValue arg;

  builtin_prologue();  
  arg = args[0];
  if (is_string_object(arg))
    arg = get_jsstring_object_value(arg);
  else if (!is_string(arg))
    arg = JS_UNDEFINED;
  set_a(context, arg);
}

/*
 * 15.5.4.6 String.prototype.concat
 */

BUILTIN_FUNCTION(string_concat)
{
  JSValue retval;
  int i;

  builtin_prologue();

  /* 1. check coercible */
  if (args[0] == JS_NULL || args[0] == JS_UNDEFINED)
    type_error_exception("string_concat");

  /* 2. */
  retval = to_string(context, args[0]);

  /* 5. */
  for (i = 1; i <= na; i++) {
    JSValue append_str = to_string(context, args[i]);
    retval = ejs_string_concat(context, retval, append_str);
  }

  /* 6. */
  set_a(context, retval);
}

/*
 *  Step 1. - 3. of
 *   15.5.4.16 String.prototype.toLowerCase        (upper = FALSE)
 *   15.5.4.17 String.prototype.toLocaleLowerCase  (upper = FALSE)
 *   15.5.4.18 String.prototype.toUpperCase        (upper = TRUE)
 *   15.5.4.19 String.prototype.toLocaleUpperCase  (upper = TRUE)
 */
static JSValue to_upper_lower(Context *ctx, JSValue str, int upper)
{
  /* 1. check coercible */
  if (str == JS_NULL || str == JS_UNDEFINED)
    type_error_exception("to_upper_lower");

  /* 2. */
  str = to_string(ctx, str);

  /* 3. */
  return string_to_upper_lower_case(ctx, str, upper);
}

/*
 *  15.5.4.16 String.prototype.toLowerCase
 */
BUILTIN_FUNCTION(string_toLowerCase)
{
  JSValue ret;

  builtin_prologue();
  /* 1. - 3. */
  ret = to_upper_lower(context, args[0], FALSE);
  /* 4. */
  set_a(context, ret);
}

/*
 *  15.5.4.18 String.prototype.toUpperCase
 */
BUILTIN_FUNCTION(string_toUpperCase)
{
  JSValue ret;

  builtin_prologue();
  /* 1. - 3. */
  ret = to_upper_lower(context, args[0], TRUE);
  /* 4. */
  set_a(context, ret);
}

/*
 *  15.5.4.15 String.prototype.substring
 */
BUILTIN_FUNCTION(string_substring)
{
  JSValue str, ret;
  cint len, intStart, intEnd;
  cint finalStart, finalEnd, from, to;

  builtin_prologue();

  /* 1. check coercible */
  if (args[0] == JS_NULL || args[0] == JS_UNDEFINED)
    type_error_exception("string_substring");

  /* 2. */
  str = to_string(context, args[0]);
  GC_PUSH(str);

  /* 3. */
  len = string_length(str);

  /* 4. */
  intStart = na >= 1 ? toInteger(context, args[1]) : 0;

  /* 5. */
  intEnd =
    (na >= 2 && args[2] != JS_UNDEFINED) ? toInteger(context, args[2]) : len;

  /* 6. */
  finalStart = min(max(intStart, 0), len);

  /* 7. */
  finalEnd = min(max(intEnd, 0), len);

  /* 8. */
  from = min(finalStart, finalEnd);

  /* 9. */
  to = max(finalStart, finalEnd);

  /* 10. */
  ret = string_make_substring(context, str, from, to - from);
  set_a(context, ret);
  GC_POP(str);
}

/*
 *  15.5.4.13 String.prototype.slice
 */
BUILTIN_FUNCTION(string_slice)
{
  JSValue str, ret;
  cint len, intStart, intEnd;
  cint from, to, span;

  builtin_prologue();

  /* 1. check coercible */
  if (args[0] == JS_NULL || args[0] == JS_UNDEFINED)
    type_error_exception("string_slice");

  /* 2. */
  str = to_string(context, args[0]);
  GC_PUSH(str);

  /* 3. */
  len = string_length(str);

  /* 4. */
  intStart = na >= 1 ? toInteger(context, args[1]) : 0;

  /* 5. */
  intEnd =
    (na >= 2 && args[2] != JS_UNDEFINED) ? toInteger(context, args[2]) : len;

  /* 6. */
  from = intStart < 0 ? max(len + intStart, 0) : min(intStart, len);

  /* 7. */
  to = intEnd < 0 ? max(len + intEnd, 0) : min(intEnd, len);

  /* 8. */
  span = max(to - from, 0);

  /* 9. */
  ret = string_make_substring(context, str, from, span);
  set_a(context, ret);
  GC_POP(str);
}

/*
 *  15.5.4.4 String.prototype.charAt
 */
BUILTIN_FUNCTION(string_charAt)
{
  JSValue str, ret;
  cint pos, c;
  builtin_prologue();

  /* 1. check coercible */
  if (args[0] == JS_NULL || args[0] == JS_UNDEFINED)
    type_error_exception("string_charAt");

  /* 2. */
  str = to_string(context, args[0]);
  GC_PUSH(str);

  /* 3. */
  pos = na >= 1 ? toInteger(context, args[1]) : 0;

  /* 4. 5. */
  if (pos < 0 || string_length(str) <= pos)
    ret = gconsts.g_string_empty;
  else {
    /* 6. */
    c = string_char_code_at(str, pos);
    if (c < 0)
      ret = gconsts.g_string_blank;
    else {
      char s[2] = {c, '\0'};
      ret = cstr_to_string(context, s);
    }
  }

  set_a(context, ret);
  GC_POP(str);
}

/*
 *  15.5.4.5 String.prototype.charCodeAt
 */
BUILTIN_FUNCTION(string_charCodeAt)
{
  JSValue str, ret;
  cint pos, c;
  builtin_prologue();

  /* 1. check coercible */
  if (args[0] == JS_NULL || args[0] == JS_UNDEFINED)
    type_error_exception("string_charAt");

  /* 2. */
  str = to_string(context, args[0]);
  GC_PUSH(str);

  /* 3. */
  pos = na >= 1 ? toInteger(context, args[1]) : 0;

  /* 4. 5. */
  if (pos < 0 || string_length(str) <= pos)
    ret = gconsts.g_flonum_nan;
  else {
    /* 6. */
    c = string_char_code_at(str, pos);
    if (c < 0)
      ret = gconsts.g_flonum_nan;
    else
      ret = cint_to_number(context, c);
  }

  set_a(context, ret);
  GC_POP(str);
}

/*
 * Note that "abcdefg".lastIndexOf("efg", 4) must return not -1 but 4
 * and "abcdefg".indexOf("",4);
 */
JSValue string_indexOf_(Context *context, JSValue *args, int na,
                        int isLastIndexOf) {
  JSValue s0, s1;
  char *s, *searchStr;
  cint pos, len, start, searchLen, k, j;
  int delta;

  s0 = is_string(args[0]) ? args[0] : to_string(context, args[0]);
  GC_PUSH(s0);
  s1 = is_string(args[1]) ? args[1] : to_string(context, args[1]);
  s = string_to_cstr(s0);
  searchStr = string_to_cstr(s1);
  len = string_length(s0);
  GC_POP(s0);
  searchLen = string_length(s1);

  if (na >= 2 && !is_undefined(args[2])) pos = toInteger(context, args[2]);
  else if (isLastIndexOf) pos = INT_MAX;
  else pos = 0;
  start = min(max(pos, 0), len);
  if (searchLen == 0)
    return cint_to_number(context, start);

  if (isLastIndexOf) delta = -1;
  else delta = 1;
  k = min(start, len - searchLen);
  while (0 <= k && k <= len - searchLen) {
    for (j = 0; s[k+j] == searchStr[j]; j++)
      if (j == (searchLen - 1)) return cint_to_number(context, k);
    k += delta;
  }

  return FIXNUM_MINUS_ONE;
}

BUILTIN_FUNCTION(string_indexOf)
{
  JSValue ret;
  builtin_prologue();
  ret = string_indexOf_(context, args, na, FALSE);
  set_a(context, ret);
  return;


#if 0
  JSValue sch, position, rsv;
  JSValue *args;
  char *rsvcs;
  char *schcs;
  char *adr;
  int fp, pos;
  uint64_t len;
  double dposition;
  dposition = INFINITY;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  rsv = args[0];
  if(is_object(rsv)){
    rsv = objectToPrimitiveHintString(rsv, context); }
  rsv = PrimitiveToString(rsv);
  rsvcs = stringToCStr(rsv);
  len = strlen(rsvcs);

  sch = args[1];
  if(is_object(sch)){
    sch = objectToPrimitiveHintString(sch, context); }
  sch = PrimitiveToString(sch);
  schcs = stringToCStr(sch);

  if(*schcs == '\0'){
    setA(context, FIXNUM_ZERO);
    return; }

  if(nArgs > 1){
    position = args[2];
    if(is_object(position)){
      position = objectToPrimitiveHintNumber(position, context); }
    dposition = PrimitiveToDouble(position);

    if(!isnan(dposition)){
      dposition = sign(dposition) * floor(fabs(dposition));
    }
  }

  pos = min(max(dposition, 0), (int)len);
  adr = strstr(rsvcs + pos, schcs);

  if(adr == NULL){
    setA(context, intToFixnum(-1));
    return;
  }else{
    setA(context, intToFixnum(adr - rsvcs));
    return;
  }
#endif
}

BUILTIN_FUNCTION(string_lastIndexOf)
{
  JSValue ret;
  builtin_prologue();
  ret = string_indexOf_(context, args, na, TRUE);
  set_a(context, ret);
  return;

#if 0
  JSValue sch, position, rsv;
  JSValue *args;
  char *rsvcs, *schcs;
  char *adr, *ret;
  int fp, pos;
  uint64_t len;
  double dposition;
  dposition = INFINITY;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  if(is_object(rsv)){
    rsv = objectToPrimitiveHintString(rsv, context); }
  rsv = PrimitiveToString(rsv);
  rsvcs = stringToCStr(rsv);
  len = strlen(rsvcs);

  sch = args[1];
  if(is_object(sch)){
    sch = objectToPrimitiveHintString(sch, context); }
  sch = PrimitiveToString(sch);
  schcs = stringToCStr(sch);

  if(*schcs == '\0'){
    setA(context, FIXNUM_ZERO);
    return; }

  if(nArgs > 1){
    position = args[2];
    if(is_object(position)){
      position = objectToPrimitiveHintNumber(position, context); }
    dposition = PrimitiveToDouble(position);
    if(!isnan(dposition)){
      dposition = sign(dposition) * floor(fabs(dposition));
    }
  }

  pos = min(max(dposition, 0), (int)len);
  adr = strstr(rsvcs + pos, schcs);

  if(adr == NULL){
    setA(context, intToFixnum(-1));
    return;
  }else{
    while(adr != NULL){
      ret = adr;
      adr = strstr(ret+1, schcs); }
    setA(context, intToFixnum(ret - rsvcs));
    return;
  }
#endif
}

BUILTIN_FUNCTION(string_fromCharCode)
{
  JSValue a, ret;
  char *s;
  cint c;
  int i;

  builtin_prologue();
  s = (char *)malloc(sizeof(char) * (na + 1));
  for (i = 1; i <= na; i++) {
    a = args[i];
    if (is_fixnum(a)) c = fixnum_to_cint(a);
    else if (is_flonum(a)) c = flonum_to_int(a);
    else {
      a = to_number(context, a);
      if (is_fixnum(a)) c = fixnum_to_cint(a);
      else if (is_flonum(a)) c = flonum_to_int(a);
      else {
        printf("fromCharCode: cannot convert to a number\n");
        c = ' ';
      }
    }
    s[i - 1] = c;
  }
  s[na] = '\0';
  ret = cstr_to_string(context, s);
  free(s);
  set_a(context, ret);
}


BUILTIN_FUNCTION(string_localeCompare)
{
  JSValue s0, s1, ret;
  char *cs0, *cs1;
  int r;

  builtin_prologue();
  s0 = to_string(context, args[0]);
  GC_PUSH(s0);
  if (na >= 1) s1 = to_string(context, args[1]);
  else s1 = to_string(context, JS_UNDEFINED);
  cs0 = string_to_cstr(s0);
  GC_POP(s0);
  cs1 = string_to_cstr(s1);

  r = strcmp(cs0, cs1); /* implemantation-defined */
  if (r > 0) ret = FIXNUM_ONE;
  else if (r < 0) ret = FIXNUM_MINUS_ONE;
  else ret = FIXNUM_ZERO;

  set_a(context, ret);
  return;

#if 0
  JSValue rsv, that;
  JSValue* args;
  int fp;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  rsv = JSValueToString(args[0], context);
  that = JSValueToString(args[1], context);
  setA(context, intToFixnum(strcmp(stringToCStr(rsv), stringToCStr(that))));
  return;
#endif
}

#if 0
BUILTIN_FUNCTION(stringProtoMatch)
{
  int fp;
  JSValue* args;
  JSValue rsv, reg, str, pat;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];
  reg = args[1];

#ifdef USE_REGEXP
  JSValue regex;
  if(isRegExp(reg)){
    str = JSValueToString(rsv, context);
    if(getRegExpGlobal(rsv)){
      setRegExpLastIndex(reg, 0);
      set_obj_cstr_prop(reg, "lastIndex", FIXNUM_ZERO, ATTR_DDDE); }
    setA(context, regExpExec(context, reg, stringToCStr(str)));
    return;
  }else
#endif

    {
      pat = JSValueToString(reg, context);
      str = JSValueToString(rsv, context);

#ifdef USE_REGEXP
      if(regexpConstructorSub(stringToCStr(pat), "", &regex) == SUCCESS_REGEX_CONST){
        setA(context, regExpExec(context, regex, stringToCStr(str)));
        return;
      }else
#endif

        {
          LOG_EXIT("string.match is not implimented.\n");
        }
    }
}
#endif

/*
 * property table
 */

/* prototype */
ObjBuiltinProp StringPrototype_builtin_props[] = {
  { "valueOf",        string_valueOf,       0, ATTR_DE },
  { "toString",       string_valueOf,       0, ATTR_DE },
  { "concat",         string_concat,        0, ATTR_DE },
  { "toLowerCase",    string_toLowerCase,   0, ATTR_DE },
  { "toUpperCase",    string_toUpperCase,   0, ATTR_DE },
  { "substring",      string_substring,     2, ATTR_DE },
  { "slice",          string_slice,         2, ATTR_DE },
  { "charAt",         string_charAt,        0, ATTR_DE },
  { "charCodeAt",     string_charCodeAt,    0, ATTR_DE },
  { "indexOf",        string_indexOf,       1, ATTR_DE },
  { "lastIndexOf",    string_lastIndexOf,   1, ATTR_DE },
  { "localeCompare",  string_localeCompare, 0, ATTR_DE },
};
ObjDoubleProp  StringPrototype_double_props[] = {
  { "length", 0, ATTR_ALL },
};
ObjGconstsProp StringPrototype_gconsts_props[] = {};
/* constructor */
ObjBuiltinProp StringConstructor_builtin_props[] = {
 { "fromCharCode",   string_fromCharCode,  0, ATTR_DE },
};
ObjDoubleProp  StringConstructor_double_props[] = {};
ObjGconstsProp StringConstructor_gconsts_props[] = {
  { "prototype", &gconsts.g_prototype_String, ATTR_ALL },
};
/* instance */
ObjBuiltinProp String_builtin_props[] = {};
ObjDoubleProp  String_double_props[] = {
  { "length", 0, ATTR_ALL },  /* placeholder */
};
ObjGconstsProp String_gconsts_props[] = {};
DEFINE_PROPERTY_TABLE_SIZES_PCI(String);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
