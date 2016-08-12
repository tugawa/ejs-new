#include "prefix.h"
#define EXTERN extern
#include "header.h"

/*
  constrcutor of a string
 */
BUILTIN_FUNCTION(string_constr)
{
  JSValue rsv;

  builtin_prologue();
  // printf("In string_constr\n");
  rsv = new_string(na > 0? args[1]: gconsts.g_string_empty);
  set_a(context, rsv);
}

/*
   constrcutor of a string (not Object)
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

BUILTIN_FUNCTION(string_concat)
{
  printf("string_concat is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

#if 0
  JSValue ret, s;
  JSValue *args;
  int fp, i;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  ret = args[0];

  if(is_object(ret)){
    ret = objectToPrimitiveHintString(ret, context); }
  ret = PrimitiveToString(ret);

  for(i=1; i<=nArgs; i++){
    s = args[i];
    if(is_object(s)){
      s = objectToPrimitiveHintString(s, context); }
    ret = cStrToString2(stringToCStr(ret), stringToCStr(s));
  }

  setA(context, ret);
  return;
#endif
}

BUILTIN_FUNCTION(string_toLowerCase)
{
  printf("string_toLowerCase is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

#if 0
  int i, fp;
  uint64_t len;
  JSValue *args;
  char *rsvcs, *result;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsvcs = stringToCStr(JSValueToString(args[0], context));
  len = strlen(rsvcs);

  result = (char*)malloc(sizeof(char) * (len+1));
  for(i=0; i<=len; i++){
    result[i] = tolower(rsvcs[i]); }
  setA(context, cStrToString(result));
  return;
#endif
}

BUILTIN_FUNCTION(string_toUpperCase)
{
  printf("string_toUpperCase is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

#if 0
  int i, fp;
  uint64_t len;
  JSValue *args;
  char *rsvcs, *result;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsvcs = stringToCStr(JSValueToString(args[0], context));
  len = strlen(rsvcs);

  result = (char*)malloc(sizeof(char) * (len + 1));
  for(i=0; i<=len; i++){
    result[i] = toupper(rsvcs[i]); }
  setA(context, cStrToString(result));
  return;
#endif
}

BUILTIN_FUNCTION(string_substring)
{
  printf("string_substring is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

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
  printf("string_slice is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

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
    ret = cstr_to_string(s);
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

BUILTIN_FUNCTION(string_indexOf)
{
  printf("string_indexOf is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

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

  // 検索する文字が無い場合
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
  printf("string_lastIndexOf is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

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

    // 頑張って最後のを見付ける
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
  ret = cstr_to_string(s);
  set_a(context, ret);
}


BUILTIN_FUNCTION(string_localeCompare)
{
  printf("string_localeCompare is not implemented yet\n");
  set_a(context, gconsts.g_string_empty);

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

// -------------------------------------------------------------------------------------
// stringProtoMatch

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
  { "concat",         string_concat,        1, ATTR_DE },
  { "toLowerCase",    string_toLowerCase,   0, ATTR_DE },
  { "toUpperCase",    string_toUpperCase,   0, ATTR_DE },
  { "substring",      string_substring,     2, ATTR_DE },
  { "slice",          string_slice,         2, ATTR_DE },
  { "charAt",         string_charAt,        0, ATTR_DE },
  { "charCodeAt",     string_charCodeAt,    0, ATTR_DE },
  { "indexOf",        string_indexOf,       1, ATTR_DE },
  { "lastIndexOf",    string_lastIndexOf,   1, ATTR_DE },
  // { "fromCharCode",   string_fromCharCode,  0, ATTR_DE },
  { "localeCompare",  string_localeCompare, 0, ATTR_DE },
  { NULL,             NULL,                 0, ATTR_DE }
};

void init_builtin_string(void)
{
  JSValue str, proto;

  gconsts.g_string = str =
    new_builtin_with_constr(string_constr_nonew, string_constr, 1);
  gconsts.g_string_proto = proto = new_string(gconsts.g_string_empty);
  set_prop_de(str, gconsts.g_string_prototype, proto);
  set_prop_de(str, cstr_to_string("fromCharCode"), new_builtin(string_fromCharCode, 0));
  set_prop_all(proto, gconsts.g_string___proto__, gconsts.g_object_proto);
  {
    ObjBuiltinProp *p = string_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}
