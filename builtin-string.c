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

#define not_implemented(s)                                              \
  LOG_EXIT("%s is not implemented yet\n", (s)); set_a(context, JS_UNDEFINED)

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
    new_normal_string_object(context, na > 0? args[1]: gconsts.g_string_empty);
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
    arg = string_object_value(arg);
  else if (!is_string(arg))
    arg = JS_UNDEFINED;
  set_a(context, arg);
}

#define MAXSTRS 100

BUILTIN_FUNCTION(string_concat)
{
  JSValue v, ret;
  char *strs[MAXSTRS];
  char *s, *p;
  int i, len;

  builtin_prologue();
  /*
   * printf("------\n");
   * printf("In string_concat: na = %d\n", na);
   * for (i = 0; i <= na; i++) {
   *   v = args[i];
   *   printf("string_concat: before to_string: i = %d: ", i);
   *   print_value_simple(context, v); printf("\n");
   * }
   * printf("-----\n");
   */
  for (i = 0, len = 0; i <= na; i++) {
    v = args[i];
    /*
     * printf("string_concat: i = %d: ", i);
     * print_value_simple(context, v); printf("\n");
     */
    if (!is_string(v)) v = to_string(context, v);
    len += string_length(v);
    strs[i] = string_to_cstr(v);
    /* printf("strs[%d] = %s\n", i, strs[i]); */
  }
  s = mallocstr(len);
  for (i = 0, p = s, len = 0; i <= na; i++)
    p = stpcpy(p, strs[i]);
  /* printf("s = %s\n", s); */
  ret = cstr_to_string(context, s);
  free(s);
  set_a(context, ret);
}

JSValue to_upper_lower(Context *context, JSValue str, int upper) {
  JSValue ret;
  int len, i;
  char *s, *r;

  if (!is_string(str)) str = to_string(context, str);
  s = string_to_cstr(str);
  len = string_length(str);
  r = mallocstr(len);
  if (upper == TRUE) {
    for (i = 0; i < len; i++)
      r[i] = toupper(s[i]);
  } else {
    for (i = 0; i < len; i++)
      r[i] = tolower(s[i]);
  }
  r[len] = '\0';
  ret = cstr_to_string(context, r);
  free(r);
  return ret;
}

BUILTIN_FUNCTION(string_toLowerCase)
{
  JSValue ret;

  builtin_prologue();
  ret = to_upper_lower(context, args[0], FALSE);
  set_a(context, ret);
}

BUILTIN_FUNCTION(string_toUpperCase)
{
  JSValue ret;

  builtin_prologue();
  ret = to_upper_lower(context, args[0], TRUE);
  set_a(context, ret);
}

BUILTIN_FUNCTION(string_substring)
{
  JSValue ret;
  cint len, intStart, intEnd;
  cint finalStart, finalEnd, from, to;
  char *s, *r;

  builtin_prologue();

  ret = is_string(args[0]) ? args[0] : to_string(context, args[0]);
  s = string_to_cstr(ret);
  len = string_length(ret);

  intStart = is_undefined(args[1]) ? 0 : toInteger(context, args[1]);
  intEnd = is_undefined(args[2]) ? len : toInteger(context, args[2]);
  finalStart = min(max(intStart, 0), len);
  finalEnd = min(max(intEnd, 0), len);
  from = min(finalStart, finalEnd);
  to = max(finalStart, finalEnd);
  len = to - from;

  r = mallocstr(len);
  strncpy(r, s+from, len);
  r[len] = '\0';
  ret = cstr_to_string(context, r);
  free(r);

  set_a(context, ret);

#if 0
  JSValue endv;
  JSValue* args;
  int start, end;
  int fp, resultLen;
  char *rsvcs, *result;
  double dstart, dend;
  uint64_t len;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsvcs = stringToCStr(JSValueToString(args[0], context));

  len = strlen(rsvcs);
  dstart = JSValueToIntegralDouble(args[1], context);

  endv = args[2];
  if(isUndefined(endv)){
    dend = (double)len;
  }else{
    dend = JSValueToIntegralDouble(endv, context);
  }

  start = (int)min(max(dstart, 0), (double)len);
  end = (int)min(max(dend, 0), (double)len);

  if(start == end){
    setA(context, gStringBlank);
    return;

  }else if(start > end){
    resultLen = start - end;
    result = (char*)malloc(sizeof(char) * (resultLen+1));
    strncpy(result, rsvcs + end, resultLen);
    result[resultLen] = '\0';
    setA(context, cStrToString(result));
    return;

  }else{
    resultLen = end - start;
    result = (char*)malloc(sizeof(char) * (resultLen+1));
    strncpy(result, rsvcs + start, resultLen);
    result[resultLen] = '\0';
    setA(context, cStrToString(result));
    return;
  }
#endif
}

BUILTIN_FUNCTION(string_slice)
{
  JSValue ret;
  cint len, intStart, intEnd;
  cint from, to;
  char *s, *r;

  builtin_prologue();

  ret = is_string(args[0]) ? args[0] : to_string(context, args[0]);
  s = string_to_cstr(ret);
  len = string_length(ret);

  intStart = is_undefined(args[1]) ? 0 : toInteger(context, args[1]);
  intEnd = is_undefined(args[2]) ? len : toInteger(context, args[2]);
  from = intStart < 0 ? max(len + intStart, 0) : min(intStart, len);
  to = intEnd < 0 ? max(len + intEnd, 0) : min(intEnd, len);
  len = max(to - from, 0);

  r = mallocstr(len);
  strncpy(r, s+from, len);
  r[len] = '\0';
  ret = cstr_to_string(context, r);
  free(r);

  set_a(context, ret);

#if 0
  JSValue rsv, startv, endv;
  JSValue* args;
  int fp;
  char *rsvcs, *result;
  int start, end, resultLen;
  double dstart, dend;
  uint64_t len;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  if(is_object(rsv)){
    rsv = objectToPrimitiveHintString(rsv, context); }
  rsv = PrimitiveToString(rsv);
  rsvcs = stringToCStr(rsv);
  len = strlen(rsvcs);

  startv = args[1];
  endv = args[2];

  if(is_object(startv)){
    startv = objectToPrimitiveHintNumber(startv, context); }
  dstart = PrimitiveToIntegralDouble(startv);

  if(dstart < 0){
    start = (int)max(((double)len) + dstart, 0);
  }else{
    start = (int)min((double)len, dstart);
  }

  if(is_object(endv)){
    endv = objectToPrimitiveHintNumber(endv, context); }
  dend = PrimitiveToIntegralDouble(endv);

  if(dend < 0){
    end = (int)max(((double)len) + dend, 0);
  }else{
    end = (int)min((double)len, dend);
  }

  resultLen = end - start;

  if(resultLen > 0){
    result = (char*)malloc(sizeof(char) * (resultLen+1));
    strncpy(result, rsvcs + start, resultLen);
    result[resultLen] = '\0';
    setA(context, cStrToString(result));
  }else{
    setA(context, gStringBlank);
  }
#endif
}

int char_code_at(Context *context, JSValue str, JSValue a) {
  char *s;
  int n;

  GC_PUSH2(a,str);
  if (!is_string(str)) str = to_string(context, str);
  if (is_fixnum(a)) n = fixnum_to_int(a);
  else if (is_flonum(a)) n = flonum_to_int(a);
  else {
    a = to_number(context, a);
    if (is_fixnum(a)) n = fixnum_to_int(a);
    else if (is_flonum(a)) n = flonum_to_int(a);
    else {
      printf("cannot convert to a number\n");
      n = 0;
    }
  }
  s = string_to_cstr(str);
  GC_POP2(str,a);
  return (0 <= n && n < string_length(str))? s[n]: -1;
}

BUILTIN_FUNCTION(string_charAt)
{
  JSValue ret;
  int r;
  char s[2];

  builtin_prologue();
  r = char_code_at(context, args[0], args[1]);
  if (r >= 0) {
    s[0] = r;
    s[1] = '\0';
    ret = cstr_to_string(context, s);
  } else
    ret = gconsts.g_string_blank;
  set_a(context, ret);
}

BUILTIN_FUNCTION(string_charCodeAt)
{
  int r;

  builtin_prologue();
  r = char_code_at(context, args[0], args[1]);
  set_a(context, r >= 0? cint_to_fixnum((cint)r): gconsts.g_flonum_nan);
}

/*
 * void strReverse(char* str) {
 *   char *l, *r;
 *   char c;
 *   if (str[0] == '\0') return;
 *   l = str;
 *   r = str + strlen(str) - 1;
 *   while (r-l > 0) {
 *     c = *l;
 *     *l = *r;
 *     *r = c;
 *     r--;
 *     l++;
 *   }
 * }
 */

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
  else if (isLastIndexOf) pos = INFINITY;
  else pos = 0;
  start = min(max(pos, 0), len);
  if (searchLen == 0)
    return cint_to_fixnum(start); /* When searchStr is an empty string */

  if (isLastIndexOf) delta = -1;
  else delta = 1;
  k = min(start, len - searchLen);
  while (0 <= k && k <= len - searchLen) {
    for (j = 0; s[k+j] == searchStr[j]; j++)
      if (j == (searchLen - 1)) return cint_to_fixnum(k);
    k += delta;
  }

  return cint_to_fixnum(-1);
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
  int c, i;

  builtin_prologue();
  s = (char *)malloc(sizeof(char) * (na + 1));
  for (i = 1; i <= na; i++) {
    a = args[i];
    if (is_fixnum(a)) c = fixnum_to_int(a);
    else if (is_flonum(a)) c = flonum_to_int(a);
    else {
      a = to_number(context, a);
      if (is_fixnum(a)) c = fixnum_to_int(a);
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

  /* implemantation-defined */
  r = strcmp(cs0, cs1);
  if (r > 0) r = TRUE;
  else if (r < 0) r = FALSE;
  else r = 0;

  ret = cint_to_fixnum(r);
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

ObjBuiltinProp string_funcs[] = {
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
  /*
   * { "fromCharCode",   string_fromCharCode,  0, ATTR_DE },
   */
  { "localeCompare",  string_localeCompare, 0, ATTR_DE },
  { NULL,             NULL,                 0, ATTR_DE }
};

void init_builtin_string(Context *ctx)
{
  JSValue str, proto;

  str = gconsts.g_string =
    new_normal_builtin_with_constr(ctx, string_constr_nonew, string_constr, 1);
  GC_PUSH(str);
  proto = gconsts.g_string_proto =
    new_string_object(ctx, gconsts.g_string_empty, HSIZE_NORMAL, PSIZE_NORMAL);
  GC_PUSH(proto);
  set___proto___all(ctx, proto, gconsts.g_object_proto);
  set_prototype_de(ctx, str, proto);
  set_prop_de(ctx, str, cstr_to_string(NULL, "fromCharCode"),
              new_normal_builtin(ctx, string_fromCharCode, 0));
  {
    ObjBuiltinProp *p = string_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name,
                        new_normal_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
    GC_POP2(proto,str);
  }
}
