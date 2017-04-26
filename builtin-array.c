/*
   builtin-array.c

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-17
     Hideya Iwasaki, 2016-17

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/

#include "prefix.h"
#define EXTERN extern
#include "header.h"

#define not_implemented(s) \
  LOG_EXIT("%s is not implemented yet\n", (s)); set_a(context, JS_UNDEFINED)

/*
   computes the asize for a given n
 */
cint compute_asize(cint n) {
  cint s, news;

  s = ASIZE_INIT;
  while (s < n) {
    news = increase_asize(s);
    if (s == news) return n;
    s = news;
  }
  return s;
}

/*
   constructor for array
 */
BUILTIN_FUNCTION(array_constr)
{
  JSValue rsv;
  cint size, length;

  builtin_prologue();
  rsv = new_normal_array(context); // this sets the `length' property to 0
  gc_push_tmp_root(&rsv);
  if (na == 0) {
    allocate_array_data(context, rsv, ASIZE_INIT, 0);
    set_prop_none(context, rsv, gconsts.g_string_length, FIXNUM_ZERO);
  } else if (na == 1) {
    JSValue n = args[1];  // GC: n is used in uninterraptible section
    if (is_fixnum(n) && 0 <= (length = fixnum_to_cint(n))) {
      size = compute_asize(length);
      allocate_array_data(context, rsv, size, length);
      // printf("array_constr: length = %ld, size = %ld, rsv = %lx\n", length, size, rsv);
      set_prop_none(context, rsv, gconsts.g_string_length, cint_to_fixnum(length));
    } else {
      allocate_array_data(context, rsv, ASIZE_INIT, 0);
      set_prop_none(context, rsv, gconsts.g_string_length, FIXNUM_ZERO);
    }
  } else {
    /*
       na >= 2, e.g., Array(2,4,5,1)
       This means that the array's length is four whose elements are
       2, 4, 5, and 1.
     */
    int i;
    length = na;
    size = compute_asize(length);
    allocate_array_data(context, rsv, size, length);
    set_prop_none(context, rsv, gconsts.g_string_length, cint_to_fixnum(length));
    for (i = 0; i < length; i++)
      array_body_index(rsv, i) = args[i];
  }
  set_a(context, rsv);
  gc_pop_tmp_root(1);
}

BUILTIN_FUNCTION(array_toString)
{
  JSValue ret;

  builtin_prologue();  
  ret = array_to_string(context, args[0], gconsts.g_string_comma);
  set_a(context, ret);
  return;
}

BUILTIN_FUNCTION(array_toLocaleString){
  int i;
  uint64_t length, sumLength;
  JSValue array, item, prim;
  char **strs;
  char *retCStr, *addr;
  ArrayCell *ap;

  builtin_prologue();  
  array = args[0];
  length = array_length(array);

  if (length > 0) {
    strs = (char **)malloc(sizeof(char*)*length);
    sumLength = 0;
    ap = remove_array_tag(array);

    for (i = 0; i < length; i++) {
      item = array_body_index(array, i);
      if (is_object(item)) {
        // invokeToLocaleString(item, context, &prim);
        prim = item;   // kore wa tekitou
      }
      strs[i] = string_to_cstr(to_string(context, prim));
      sumLength += strlen(strs[i]);
    }

    // 全文字列のデータを格納する文字列を確保
    retCStr = (char *)malloc(sizeof(char)*(sumLength+length));
    addr = retCStr;
    strcpy(addr, strs[0]);

    // 文字列を全てカンマで繋げる
    addr += strlen(strs[0]);
    for(i = 1; i < length; i++){
      *(addr++) = ',';
      strcpy(addr, strs[i]);
      addr += strlen(strs[i]); }
    *addr = '\0';

    set_a(context, cstr_to_string(retCStr));

  }else{
    set_a(context, gconsts.g_string_empty);
    return;
  }
}

/*
  joins the elements of an array by using a specified separator,
  where default separator is ','
 */
BUILTIN_FUNCTION(array_join)
{
  JSValue sep, ret;

  builtin_prologue();
  if (na == 0)
    sep = gconsts.g_string_comma;
  else {
    sep = to_string(context, args[1]);
    if (!is_string(sep))
      /* Could it happen? */
      sep = gconsts.g_string_comma;
  }
  ret = array_to_string(context, args[0], sep);
  set_a(context, ret);
  return;
}

BUILTIN_FUNCTION(array_concat)
{
  not_implemented("concat");
#if 0
  int fp, i, j;
  int putPoint;
  JSValue *args;
  JSValue item, ret, v;
  uint64_t length;

  putPoint = 0;
  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  ret = newArray();
  set_prop_all(ret, gconsts.g_string___proto__, gArrayProto);


  for(i=0; i<=nArgs; i++){
    item = args[i];
    if(is_object(item) && isArray(item)){

      length = getArrayLength(item);
      for(j=0; j<length; j++){
        getArrayValue(item, j, &v);
        setArrayValue(ret, putPoint++, v); }

    }else{

      setArrayValue(ret, putPoint++, item);
    }
  }

  setA(context, ret);
  return;
#endif
}

BUILTIN_FUNCTION(array_pop)
{
  JSValue a, ret, flen;
  cint len;

  builtin_prologue();
  a = args[0];
  len = array_length(a) - 1;    // len >= 0
  flen = cint_to_fixnum(len);
  if (len < array_size(a))
    ret = array_body_index(a, len);
  else
    ret = get_prop_prototype_chain(a, flen);
  array_length(a) = len;
  set_prop_none(context, a, gconsts.g_string_length, flen);
  set_a(context, ret);
  return;
}

BUILTIN_FUNCTION(array_push)
{
  JSValue a, ret;
  cint len;
  int i;

  builtin_prologue();
  a = args[0];
  len = array_length(a);
  /*
     The following for-loop is very inefficient.
     This is for simplicity of implementation.
   */
  for (i = 1; i <= na; i++)
    set_array_index_value(context, a, len++, args[i], FALSE);
  ret = (len <= MAX_ARRAY_LENGTH)?
          cint_to_fixnum(len): cint_to_fixnum(MAX_ARRAY_LENGTH);
  set_a(context, ret);
  return;
}

BUILTIN_FUNCTION(array_reverse)
{
  not_implemented("reverse");
#if 0
  int fp, i;
  uint64_t length;
  JSValue* args;
  JSValue rsv, temp1, temp2;
  fp = getFp(context);

  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  length = getArrayLength(rsv);
  for(i=0; i<(length-1)/2; i++){
    getArrayValue(rsv, i, &temp1);
    getArrayValue(rsv, (int)(length-i-1), &temp2);
    setArrayValue(rsv, i, temp2);
    setArrayValue(rsv, (int)(length-i-1), temp1);
  }
  setA(context, rsv);
  return;
#endif
}

BUILTIN_FUNCTION(array_shift)
{
  not_implemented("shift");
#if 0
  int fp, i;
  uint64_t length;
  JSValue* args;
  JSValue rsv, ret, temp;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  length = getArrayLength(rsv);
  if(length > 0){
    getArrayValue(rsv, 0, &ret);
    for(i=1; i<length; i++){
      getArrayValue(rsv, i, &temp);
      setArrayValue(rsv, i-1, temp); }

    set_prop_none(rsv, gconsts.g_string_length, intToFixnum(length-1));
    setArrayLength(rsv, length-1);
    setA(context, ret);
    return;
  }else{
    setA(context, JS_UNDEFINED);
    return;
  }
#endif
}


// -------------------------------------------------------------------------------------
// ProtoUnShift
// http://www.tohoho-web.com/js/array.htm#unshift

// -------------------------------------------------------------------------------------
// ProtoSplice
// http://www.tohoho-web.com/js/array.htm#splice


BUILTIN_FUNCTION(array_slice)
{
  not_implemented("slice");
#if 0
  JSValue* args;
  JSValue startv, endv, src, array, rsv;
  int fp, start, end, i, newLength;
  double dstart, dend;
  uint64_t length;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  rsv = args[0];
  length = getArrayLength(rsv);

  startv = args[1];
  endv = args[2];

  if(is_object(startv)){
    startv = objectToPrimitive(startv, context); }
  dstart = PrimitiveToIntegralDouble(PrimitiveToDouble(startv));

  if(dstart < 0){
    start = (int)max(dstart + ((double)length), 0);
  }else{ start = (int)min(dstart, length); }

  if(isUndefined(endv)){
    end = (int)length; }

  else{
    if(is_object(endv)){
      endv = objectToPrimitive(endv, context); }

    dend = PrimitiveToIntegralDouble(PrimitiveToDouble(endv));
    if(dend < 0){
      end = (int)max(dend + ((double)length), 0);
    }else{ end = (int)min(dend, length); }
  }

  newLength = end - start;
  array = newArrayWithSize(newLength);
  set_prop_all(array, gconsts.g_string___proto__, gArrayProto);

  for(i = 0; i < newLength; i++){
    getArrayValue(rsv, i + start, &src);
    setArrayValue(array, i, src); }
  setA(context, array);
  return;
#endif
}

BUILTIN_FUNCTION(array_sort)
{
  not_implemented("sort");
#if 0
  JSValue* args;
  JSValue rsv, comp, lenv, iv, jv, temp;
  int i, j, index, fp, length;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  rsv = args[0];
  comp = args[1];
  getProp(rsv, cStrToString("length"), &lenv);
  length = (int)fixnumToInt(PrimitiveToInteger(lenv));

  if(isArray(rsv)){

    if(isUndefined(comp)){
      for(i=0; i<length; i++){
        index = i;
        getArrayValue(rsv, index, &iv);
        for(j=i+1; j<length; j++){
          getArrayValue(rsv, j, &jv);
          if(slowLessthan(jv, iv, context) == JS_TRUE){
            index = j; iv = jv; }}

        getArrayValue(rsv, i, &temp);
        setArrayValue(rsv, i, iv);
        setArrayValue(rsv, index, temp);
      }
      setA(context, rsv);
      return;
    }

    else if(isCallable(comp)){
      JSValue compret;
      for(i=0; i<length; i++){
        index = i;
        getArrayValue(rsv, index, &iv);
        for(j=i+1; j<length; j++){
          getArrayValue(rsv, j, &jv);
          compret = invoke(Global(context), context, comp, 2, jv, iv);
          if(slowLessthan(compret, FIXNUM_ZERO, context) == JS_TRUE){
            index = j; iv = jv; }}

        getArrayValue(rsv, i, &temp);
        setArrayValue(rsv, i, iv);
        setArrayValue(rsv, index, temp);
      }
      setA(context, rsv);
      return;
    }
  }

  LOG_EXIT("receiver is not array\n");
#endif
}

BUILTIN_FUNCTION(array_debugarray)
{
  JSValue a;
  cint size, length, to;
  int i;

  builtin_prologue();
  a = args[0];
  size = array_size(a);
  length = array_length(a);
  to = length < size? length: size;
  printf("debugarray: size = %ld, length = %ld, to = %ld\n", size, length, to);
  for (i = 0; i < to; i++) {
    printf("i = %d: ", i);
    print_value_simple(context, array_body_index(a, i));
    printf("\n");
  }
  set_a(context, JS_UNDEFINED);
  return;
}

ObjBuiltinProp array_funcs[] = {
  { "toString",       array_toString,       0, ATTR_DE },
  { "toLocateString", array_toLocaleString, 0, ATTR_DE },
  { "join",           array_join,           1, ATTR_DE },
  { "concat",         array_concat,         1, ATTR_DE },
  { "pop",            array_pop,            0, ATTR_DE },
  { "push",           array_push,           1, ATTR_DE },
  { "reverse",        array_reverse,        0, ATTR_DE },
  { "shift",          array_shift,          1, ATTR_DE },
  { "slice",          array_slice,          2, ATTR_DE },
  { "sort",           array_sort,           1, ATTR_DE },
  { "debugarray",     array_debugarray,     0, ATTR_DE },
  { NULL,             NULL,                 0, ATTR_DE }
};

void init_builtin_array(Context *ctx)
{
  JSValue proto;

  gconsts.g_array =
    new_normal_builtin_with_constr(ctx, array_constr, array_constr, 0);
  gconsts.g_array_proto = proto = new_big_predef_object(ctx);
  set_prototype_all(ctx, gconsts.g_array, proto);
  {
    ObjBuiltinProp *p = array_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name,
                        new_normal_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
  }
}
