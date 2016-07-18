#include "prefix.h"
#define EXTERN extern
#include "header.h"

#ifdef USE_REGEXP

#if 0
BUILTIN_FUNCTION(regexp_constr)
{
  JSValue res, pat, flag;

  builtin_prologue();
  switch (na) {
  case 0:
    regexpConstructorSub("", "", &result);
    setA(context, result);
    return;
  case 1:
    pattern = args[1];
    if(isRegExp(pattern)){
        regexpConstructorSub(getRegExpPattern(pattern), "", &result);
        setA(context, result); return; }

      else{
        if(isUndefined(pattern)){
          regexpConstructorSub("", "", &result);
          setA(context, result); return; }

        else{
          pattern = JSValueToString(pattern, context);
          if(isString(pattern)){
            regexpConstructorSub(stringToCStr(pattern), "", &result);
            setA(context, result); return; } } }

    case 2:
      pattern = args[1];
      flag = args[2];
      if(isRegExp(pattern)){
        if(isUndefined(flag)){
          regexpConstructorSub(getRegExpPattern(pattern), "", &result);
          setA(context, result); return; } }

      else{
        pattern = JSValueToString(pattern, context);
        flag = JSValueToString(flag, context);
        if(isString(pattern) && isString(flag)){
          regexpConstructorSub(stringToCStr(pattern), stringToCStr(flag), &result);
          setA(context, result); return; } }

    default:
      break;
  }

  LOG_EXIT("pattern can't convert to String");
}

BUILTIN_FUNCTION(regexp_constr_nonew)
{
  JSValue* args;
  JSValue result, pattern, flag;
  int fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  switch(nArgs){
    case 0:
      regexpConstructorSub("", "", &result);
      setA(context, result); return;

    case 1:
      pattern = args[1];
      if(isRegExp(pattern)){
        setA(context, pattern); return; }

      else{
        if(isUndefined(pattern)){
          regexpConstructorSub("", "", &result);
          setA(context, result); return; }

        else{
          pattern = JSValueToString(pattern, context);
          if(isString(pattern)){
            regexpConstructorSub(stringToCStr(pattern), "", &result);
            setA(context, result); return; } } }
    case 2:
      pattern = args[1];
      flag = args[2];
      if(isRegExp(pattern)){
        if(isUndefined(flag)){
          setA(context, pattern ); return; } }

      else{
        pattern = JSValueToString(pattern, context);
        flag = JSValueToString(flag, context);
        if(isString(pattern) && isString(flag)){
          regexpConstructorSub(stringToCStr(pattern), stringToCStr(flag), &result);
          setA(context, result); return; } }

    default:
      ;
  }

  LOG_EXIT("regexpConstructor:pattern can't convert to String");
}

BUILTIN_FUNCTION(regexp_toString)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];

  if(isRegExp(rsv)){
    char *pattern = getRegExpPattern(rsv);
    uint64_t length = strlen(pattern);
    char *retStr = malloc((sizeof(char)*length) +3);

    // "/.../" のフォーマットに変換
    *retStr = '/';
    strcpy(retStr+1, pattern);
    *(retStr + length + 1) = '/';
    *(retStr + length + 2) = '\0';
    setA(context, cStrToString(retStr));
    return;

  }else{
    LOG_EXIT("RegExp.prototype.toString received not RegExpObject");
  }
}

BUILTIN_FUNCTION(regexp_exec)
{
  int fp;
  char *cstr;
  JSValue* args;
  JSValue rsv, str;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  if(isRegExp(rsv)){
    str = JSValueToString(args[1], context);
    cstr = stringToCStr(str);
    setA(context, regExpExec(context, rsv, cstr));
    return;

  }else{
    LOG_EXIT("RegExp.prototype.exec received not RegExp Object\n");
  }
}

BUILTIN_FUNCTION(regexp_test)
{
  int fp;
  char *cstr;
  JSValue* args;
  JSValue rsv, str;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  if(isRegExp(rsv)){
    str = JSValueToString(args[1], context);
    cstr = stringToCStr(str);
    setA(context, isNull(regExpExec(context, rsv, cstr))? JS_FALSE:JS_TRUE);
    return;

  }else{
    LOG_EXIT("RegExp.prototype.exec received not RegExp Object\n");
  }
}


bool regExpProtoExecSub(regex_t* regex, const char* str, int startIndex, OnigRegion* region)
{
  int res = onig_search
  (regex, (OnigUChar*)str,
   (OnigUChar*)str + strlen(str),
   (OnigUChar*)str + startIndex,
   (OnigUChar*)str + strlen(str),
   region, ONIG_OPTION_NONE);

  return res == ONIG_MISMATCH ? false : true;
}

JSValue regExpMatchToString(const char* str, int start, int end)
{
  int length = end - start;
  char *ret = malloc((sizeof(char)*length) +1);
  memcpy(ret, str + start, length);
  ret[length] = '\0';
  return cStrToString(ret);
}

inline JSValue regExpExec(Context* context, JSValue rsv, char *cstr)
{
  int start;
  OnigRegion *region;
  region = onig_region_new();
  if(getRegExpGlobal(rsv)){
    start = getRegExpLastIndex(rsv);
  }else{
    start = 0;
  }
  if(strlen(cstr) >= start){
    if(regExpProtoExecSub(getRegExpRegexObject(rsv), cstr, start, region)){
      int length = region->num_regs;
      int i, index;
      JSValue arr = newArrayWithSize(length);
      index = region->beg[0];
      for(i = 0;i < length; i++){
        setArrayValue(arr, i, regExpMatchToString(cstr, region->beg[i], region->end[i]));
      }
      set_obj_cstr_prop_none(arr, "index", intToFixnum(index));
      if(getRegExpGlobal(rsv)){
        setRegExpLastIndex(rsv, region->end[i-1]);
        set_obj_cstr_prop(rsv, "lastIndex", intToFixnum(region->end[i-1]), ATTR_DDDE);
      }
      return arr;
    }else{
      setRegExpLastIndex(rsv, 0);
      set_obj_cstr_prop(rsv, "lastIndex", FIXNUM_ZERO, ATTR_DDDE);
      return JS_NULL;
    }
  }else{
    setRegExpLastIndex(rsv, 0);
    set_obj_cstr_prop(rsv, "lastIndex", FIXNUM_ZERO, ATTR_DDDE);
    return JS_NULL;
  }
}

int cstr_to_regexpflag(char *cstr, int *flag) {
{
  bool global, ignorecase, multiline;
  int c, f;

  global = false;
  ignoreCase = false;
  multiline = false;
  f = 0;
  while ((c = *cstr++) != '\0') {
    switch (c) {
    case 'g':
      // if (global) return FAIL;
      global = true;
      break;
    case 'i':
      // if (ignorecase) return FAIL;
      ignorecase = true;
      break;
    case 'm':
      // if (multiline) return FAIL;
      multiline = true;
      break;
    default:
      return FAIL;
    }
  }
  if (global) f |= F_REGEXP_GLOBAL;
  if (ignoreCase) f |= F_REGEXP_IGNORE;
  if (multiline) f |= F_REGEXP_MULTILINE;
  *flag = f;
  return SUCCESS;
}

int regexp_constructor_sub(char *pat, char *cstr, JSValue *dst) {
  int flag, err;
  OnigOptionType opt;
  JSValue ret;
  RegexpCell *p;

  if ((err = cStrToRegExpFlag(cStrFlag, &flag)) == FAIL)
    return FAIL;
  }
  ret = new_regexp();
  p = remove_regexp_tag(ret);
  regexp_pattern(p) = strdup(pat);   // The original code used ststrdup. Why?
  opt = set_regexp_flag(p, flag);

  err = makeRegExObject(*dst, option);
  if(!err == MAKE_REGEX_OBJECT_SUCCESS){
    return ERROR_REGEX_CONST;
  }
  set_obj_cstr_prop(*dst, "source", cStrToString(pattern), ATTR_ALL);
  setRegExpLastIndex(*dst, 0);
  set_obj_cstr_prop(*dst, "lastIndex", FIXNUM_ZERO, ATTR_DDDE);
  return SUCCESS_REGEX_CONST;
}
#endif

ObjBuiltinProp regexp_funcs[] = {
  // { "exec",           regexp_exec,          1, ATTR_DE },
  // { "test",           regexp_test,          1, ATTR_DE },
  { NULL,             NULL,                 0, ATTR_DE }
};

void init_builtin_regexp(void)
{
  /*
  JSValue r, proto;

  gconsts.g_regexp = r =
    new_builtin_with_constr(regexp_constr_nonew, regexp_constr, 2);
  gconsts.g_regexp_proto = proto = new_object();
  set_prop_all(r, gconsts.g_string_prototype, proto);
  set_obj_cstr_prop(proto, "constructor", g_regexp, ATTR_DE);
  {
    ObjBuiltinProp *p = regexp_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
  */
}

#endif // USE_REGEXP
