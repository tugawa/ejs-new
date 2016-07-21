#include "prefix.h"
#define EXTERN extern
#include "header.h"

#ifdef USE_REGEXP

int cstr_to_regexp_flag(char *cstr, int *flag) {
  bool global, ignorecase, multiline;
  int c, f;

  global = false;
  ignorecase = false;
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
  if (ignorecase) f |= F_REGEXP_IGNORE;
  if (multiline) f |= F_REGEXP_MULTILINE;
  *flag = f;
  return SUCCESS;
}

int regexp_constructor_sub(char *pat, char *flagstr, JSValue *dst) {
  JSValue re;
  int flag, err;

  if ((err = cstr_to_regexp_flag(flagstr, &flag)) == FAIL)
    return FAIL;
  if ((re = new_regexp(pat, flag)) == JS_UNDEFINED)
    return FAIL;
  set_obj_cstr_prop(re, "source", cstr_to_string(pat), ATTR_ALL);
  regexp_lastindex(re) = 0;
  set_obj_cstr_prop(re, "lastIndex", FIXNUM_ZERO, ATTR_DDDE);
  *dst = re;
  return SUCCESS;
}

void regexp_constr_general(Context *context, int fp, int na, int new) {
  JSValue res, pat, flag;
  char *cstrflag;

  builtin_prologue();
  switch (na) {
  case 0:
    regexp_constructor_sub("", "", &res);
    set_a(context, res);
    return;
  case 1:
LAB0:
    cstrflag = "";
LAB1:
    pat = args[1];
    if (is_regexp(pat)) {
      if (new == TRUE)
        regexp_constructor_sub(regexp_pattern(pat), cstrflag, &res);
      else
        res = pat;
    } else if (pat == JS_UNDEFINED)
      regexp_constructor_sub("", cstrflag, &res);
    else {
      pat = to_string(context, pat);
      if (is_string(pat))
        regexp_constructor_sub(string_value(pat), cstrflag, &res);
      else
        regexp_constructor_sub("", cstrflag, &res);
    }
    break;
  default:
    flag = args[2];
    if (flag == JS_UNDEFINED) goto LAB0;
    flag = to_string(context, flag);
    goto LAB1;
  }
  set_a(context, res);
}

JSValue regexp_exec(Context* context, JSValue rsv, char *cstr) {
  return JS_NULL;
}

BUILTIN_FUNCTION(regexp_constr)
{
  regexp_constr_general(context, fp, na, TRUE);
}

BUILTIN_FUNCTION(regexp_constr_nonew)
{
  regexp_constr_general(context, fp, na, FALSE);
}

BUILTIN_FUNCTION(regexp_toString)
{
  JSValue rsv;
  char *pat, *ret;
  uint64_t len;

  builtin_prologue();
  rsv = args[0];
  if (is_regexp(rsv)) {
    pat = regexp_pattern(rsv);
    len = strlen(pat);
    ret = malloc(sizeof(char) * len + 3);
    ret[0] = '/';
    strcpy(&ret[1], pat);
    ret[len + 1] = '/';
    ret[len + 2] = '\0';
    set_a(context, cstr_to_string(ret));
  } else
    LOG_EXIT("RegExp.prototype.toString: receiver is not a regexp\n");
}

BUILTIN_FUNCTION(builtin_regexp_exec)
{
  JSValue rsv, str;
  char *cstr;

  builtin_prologue();
  rsv = args[0];
  if (is_regexp(rsv)) {
    str = to_string(context, args[1]);
    cstr = string_to_cstr(str);
    set_a(context, regexp_exec(context, rsv, cstr));
  } else
    LOG_EXIT("Regexp.prototype.exec: receiver is not a regexp\n");
}

BUILTIN_FUNCTION(builtin_regexp_test)
{
  JSValue rsv, str, ret;
  char *cstr;

  builtin_prologue();
  rsv = args[0];
  if (is_regexp(rsv)) {
    str = to_string(context, args[1]);
    cstr = string_to_cstr(str);
    print_value_verbose(context, rsv); printf(", cstr = %s\n", cstr);
    ret = regexp_exec(context, rsv, cstr);
    print_value_verbose(context, ret); putchar('\n');
    ret = (ret == JS_NULL)? JS_FALSE: JS_TRUE;
    set_a(context, ret);
  } else
    LOG_EXIT("Regexp.prototype.test: receiver is not a regexp\n");
}


#if 0
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
#endif

ObjBuiltinProp regexp_funcs[] = {
  { "exec",           builtin_regexp_exec,          1, ATTR_DE },
  { "test",           builtin_regexp_test,          1, ATTR_DE },
  { NULL,             NULL,                         0, ATTR_DE }
};

void init_builtin_regexp(void)
{
  JSValue r, proto;

  gconsts.g_regexp = r =
    new_builtin_with_constr(regexp_constr_nonew, regexp_constr, 2);
  gconsts.g_regexp_proto = proto = new_object();
  set_prop_all(r, gconsts.g_string_prototype, proto);
  set_obj_cstr_prop(proto, "constructor", r, ATTR_DE);
  {
    ObjBuiltinProp *p = regexp_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}

#endif // USE_REGEXP
