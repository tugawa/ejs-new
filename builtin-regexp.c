#include "prefix.h"
#define EXTERN extern
#include "header.h"

#ifdef USE_REGEXP

BUILTIN_FUNCTION(regexp_constr)
{
  JSValue* args;
  JSValue result, pattern, flag;
  int fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  switch(nArgs){
    case 0:

      // 初期化パターン無し、オプション無し
      regexpConstructorSub("", "", &result);
      setA(context, result);
      return;

    case 1:

      //
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
      set_obj_prop_none(arr, "index", intToFixnum(index));
      if(getRegExpGlobal(rsv)){
        setRegExpLastIndex(rsv, region->end[i-1]);
        set_obj_prop(rsv, "lastIndex", intToFixnum(region->end[i-1]), ATTR_DDDE);
      }
      return arr;
    }else{
      setRegExpLastIndex(rsv, 0);
      set_obj_prop(rsv, "lastIndex", FIXNUM_ZERO, ATTR_DDDE);
      return JS_NULL;
    }
  }else{
    setRegExpLastIndex(rsv, 0);
    set_obj_prop(rsv, "lastIndex", FIXNUM_ZERO, ATTR_DDDE);
    return JS_NULL;
  }
}

int cStrToRegExpFlag(const char* cStrFlag, int* flag)
{
  bool global, ignoreCase, multiline;
  global = false;
  ignoreCase = false;
  multiline = false;
  while(cStrFlag != '\0'){
    switch(*cStrFlag){
      case 'g':
        if(global){
          return ERROR_CSTR_TO_REGEXP_FLAG;
        }else{
          global = true;
          break;
        }
      case 'i':
        if(ignoreCase){
          return ERROR_CSTR_TO_REGEXP_FLAG;
        }else{
          ignoreCase = true;
          break;
        }
      case 'm':
        if(multiline){
          return ERROR_CSTR_TO_REGEXP_FLAG;
        }else{
          multiline = true;
          break;
        }
      default:
        return ERROR_CSTR_TO_REGEXP_FLAG;
    }
    cStrFlag++;
  }
  if(global){
    *flag |= F_REGEXP_GLOBAL;
  }
  if(ignoreCase){
    *flag |= F_REGEXP_IGNORE;
  }
  if(multiline){
    *flag |= F_REGEXP_MULTILINE;
  }
  return SUCCESS_CSTR_TO_REGEXP_FLAG;
}

int regexpConstructorSub(const char* pattern, const char* cStrFlag, JSValue* dst)
{
  int flag = 0, err;
  OnigOptionType option;
  err = cStrToRegExpFlag(cStrFlag, &flag);
  if(!err == SUCCESS_CSTR_TO_REGEXP_FLAG){
    return ERROR_REGEX_CONST;
  }
  *dst = newRegExp();
  setRegExpPattern(*dst, ststrdup(pattern));
  option = setRegExpFlag(flag, *dst);
  err = makeRegExObject(*dst, option);
  if(!err == MAKE_REGEX_OBJECT_SUCCESS){
    return ERROR_REGEX_CONST;
  }
  set_obj_prop(*dst, "source", cStrToString(pattern), ATTR_ALL);
  setRegExpLastIndex(*dst, 0);
  set_obj_prop(*dst, "lastIndex", FIXNUM_ZERO, ATTR_DDDE);
  return SUCCESS_REGEX_CONST;
}

ObjBuiltinProp regexp_funcs[] = {
  { "exec",           regexp_exec,          1, ATTR_DE },
  { "test",           regexp_test,          1, ATTR_DE },
  { NULL,             NULL,                 0, ATTR_DE }
};

void init_builtin_regexp(void)
{
  gconsts.g_regexp
    = new_builtin_with_constr(regexp_constr_nonew, regexp_constr, 2);
  gconsts.g_regexp_proto = new_object();
  set_obj_prop(gconsts.g_regexp, "prototype", gconsts.g_regexp_proto, ATTR_ALL);
  set_obj_prop(gRegExpProto, "constructor", gRegExp, ATTR_DE);
  {
    ObjBuiltinProp *p = regexp_funcs;
    while (p->name != NULL) {
      set_obj_prop(gconsts.g_regexp_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}

#endif // USE_REGEXP
