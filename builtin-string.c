#include "prefix.h"
#define EXTERN extern
#include "header.h"

// constrcutor of a string
//
BUILTIN_FUNCTION(string_constr)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];
  // Why it is not necessary to allocate a string object here?
  if (na > 0)
    string_object_value(rsv) = to_string(context, args[1]);
  set_a(context, rsv);
}

// constrcutor of a string (not Object)
//
BUILTIN_FUNCTION(string_constr_nonew)
{
  JSValue arg;
  
  builtin_prologue();
  if (na > 0)
    arg = to_string(context, args[1]);
  else
    arg = gconsts.g_string_blank;
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

#if 0
BUILTIN_FUNCTION(stringFromCharCode)
{
  int i, fp;
  char *str;
  JSValue ret;
  JSValue* args;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  str = (char*)malloc(sizeof(char) * (nArgs+1));

  for(i=1; i<=nArgs; i++){
    JSValue ch;
    uint16_t code;
    ch = args[i];
    if(is_object(ch)){
      ch = objectToPrimitiveHintNumber(ch, context); }
    code = doubleToUInt16(PrimitiveToDouble(ch));
    str[i-1] = (char)code; }

  str[i] = '\0';
  ret = cStrToString(str);
  setA(context, ret);
  return;
}
#endif

// stringProtoCharAt
// 引数で与えられた番号の文字を取得する

#if 0
BUILTIN_FUNCTION(stringProtoCharAt)
{
  JSValue rsv, arg;
  JSValue* args;
  char *rsvstr;
  char rets[2];
  int fp, len;
  double x;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];
  arg = args[1];

  if(is_object(rsv)){
    rsv = objectToPrimitiveHintString(rsv, context); }
  rsvstr = stringToCStr(PrimitiveToString(rsv));

  if(is_object(arg)){
    arg = objectToPrimitiveHintNumber(arg, context); }
  x = PrimitiveToIntegralDouble(arg);

  len = (int)strlen(rsvstr);
  if((len < 0) || (len < x)){
    setA(context, gStringBlank);
    return;
  }else{
    rets[1] = '\0';
    rets[0] = rsvstr[(int64_t) x];
    setA(context, cStrToString(rets));
    return;
  }
}
#endif

// stringProtoCharCodeAt

#if 0
BUILTIN_FUNCTION(stringProtoCharCodeAt)
{
  JSValue rsv, arg;
  JSValue* args;
  char *rsvstr;
  int fp, len;
  double x;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];
  arg = args[1];

  if(is_object(rsv)){
    rsv = objectToPrimitiveHintString(rsv, context); }
  rsvstr = stringToCStr(PrimitiveToString(rsv));

  if(is_object(arg)){
    arg = objectToPrimitiveHintNumber(arg, context); }
  x = PrimitiveToIntegralDouble(arg);

  len = (int)strlen(rsvstr);
  if((len < 0) || (len < x)){
    setA(context, gFlonum_NaN);
    return;
  }else{
    setA(context, intToFixnum(rsvstr[(int64_t)x]));
    return;
  }
}
#endif

// stringProtoConcat
#if 0
BUILTIN_FUNCTION(stringProtoConcat)
{
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
}
#endif

// stringProtoIndexOf

// レシーバの文字列から、検索文字列を検索する
// 検索結果のインデックスを返却する

#if 0
BUILTIN_FUNCTION(stringProtoIndexOf)
{
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
}
#endif

// stringProtoIndexOf

// レシーバの文字列から、検索文字列を検索する
// 検索結果のインデックスを返却する
// この関数は最後の場所を見付ける

#if 0
BUILTIN_FUNCTION(stringProtoLastIndexOf)
{
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
}
#endif

// stringProtoLocalCompare

#if 0
BUILTIN_FUNCTION(stringProtoLocaleCompare)
{
  JSValue rsv, that;
  JSValue* args;
  int fp;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  rsv = JSValueToString(args[0], context);
  that = JSValueToString(args[1], context);
  setA(context, intToFixnum(strcmp(stringToCStr(rsv), stringToCStr(that))));
  return;
}
#endif

// -------------------------------------------------------------------------------------
// stringProtoSlice

#if 0
BUILTIN_FUNCTION(stringProtoSlice)
{
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
}
#endif

// -------------------------------------------------------------------------------------
// stringProtoSubstring

#if 0
BUILTIN_FUNCTION(stringProtoSubstring)
{
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
}
#endif

// -------------------------------------------------------------------------------------
// stringProtoLowerCase
#if 0
BUILTIN_FUNCTION(stringProtoToLowerCase)
{
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
}
#endif

// -------------------------------------------------------------------------------------
// stringProtoUpperCase

#if 0
BUILTIN_FUNCTION(stringProtoToUpperCase)
{
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
}

#endif
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
  { "valueOf",        string_valueOf,     0, ATTR_DE },
//  { "toString",     string_toString     0, ATTR_DE },
//  { "concat",         string_concat,        1, ATTR_DE },
//  { "toLowerCase",    string_toLowerCase,   0, ATTR_DE },
//  { "toUpperCase",    string_toUpperCase,   0, ATTR_DE },
//  { "substring",      string_substring,     2, ATTR_DE },
//  { "slice",          string_slice,         2, ATTR_DE },
//  { "charAt",         string_charAt,        0, ATTR_DE },
//  { "charCodeAt",     string_charCodeAt,    0, ATTR_DE },
//  { "indexOf",        string_indexOf,       1, ATTR_DE },
//  { "lastIndexOf",    string_lastIndexOf,   1, ATTR_DE },
//  { "localeCompare",  string_localeCompare, 0, ATTR_DE },
  { NULL,             NULL,                       0, ATTR_DE }
};

void init_builtin_string(void)
{
  gconsts.g_string
    = new_builtin_with_constr(string_constr_nonew, string_constr, 1);
  gconsts.g_string_proto = new_string(gconsts.g_string_blank);
  set_prop_all(gconsts.g_string, gconsts.g_string_prototype, gconsts.g_string_proto);
  // set_obj_cstr_prop(gconsts.g_string, "fromCharCode", new_builtin(stringFromCharCode, 0), ATTR_DE);
  {
    ObjBuiltinProp *p = string_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(gconsts.g_string_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}
