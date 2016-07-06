#include "prefix.h"
#define EXTERN extern
#include "header.h"

// constructor for array
//
BUILTIN_FUNCTION(array_constr)
{
  JSValue rsv;
  ArrayCell *p;

  builtin_prologue();
  rsv = new_array();
  p = remove_array_tag(rsv);
  set_prop_all(rsv, gconsts.g_string___proto__, gconsts.g_array_proto);

  switch (na) {
  case 0:
    allocate_array_data(p, INITIAL_ARRAY_SIZE, 0);
    break;
  case 1:
    {
      JSValue num;
      cint size, length;

      num = args[1];
      size =INITIAL_ARRAY_SIZE;

      if (is_fixnum(num) && 0 <= (length = fixnum_to_cint(num))) {
        while (size < length) size <<= 1;
        allocate_array_data(p, size, length);
        set_prop_none(rsv, gconsts.g_string_length, cint_to_fixnum(length));
      } else
        allocate_array_data(p, INITIAL_ARRAY_SIZE, 0);
    }
    break;
  default:
    {
      // not implemented yet
      allocate_array_data(p, INITIAL_ARRAY_SIZE, 0);
    }
    break;
  }
  set_a(context, rsv);
}

BUILTIN_FUNCTION(array_toString)
{
  JSValue ret;

  builtin_prologue();  
  ret = array_to_string(context, args[0], gconsts.g_string_comma);
  set_a(context, ret);
  return;
}

JSValue array_to_string(Context* context, JSValue array, JSValue separator)
{
  int i;
  uint64_t length, sumLength, separatorLength;
  JSValue ret, item;
  char **strs;
  char *separatorCStr;
  char *retCStr, *addr;
  ArrayCell *ap;

  ret = gconsts.g_string_blank;
  length = array_length(array);

  if (length > 0) {
    strs = (char**)malloc(sizeof(char*) * length);
    separatorCStr = string_to_cstr(separator);
    separatorLength = strlen(separatorCStr);
    sumLength = 0;
    ap = remove_array_tag(array);

    for (i = 0; i < length; i++) {
      item = array_body_index(ap, i);
      if (is_object(item)) {
        item = objectToPrimitiveHintString(item, context); }
      strs[i] = string_to_cstr(primitive_to_string(item));
      sumLength += strlen(strs[i]);
    }

    retCStr = (char*)malloc(sizeof(char) * (sumLength + (length - 1) * separatorLength + 1));
    addr = retCStr;
    strcpy(addr, strs[0]);
    addr += strlen(strs[0]);

    for(i=1; i<length; i++){
      strcpy(addr, separatorCStr);
      addr += separatorLength;
      strcpy(addr, strs[i]);
      addr += strlen(strs[i]);
    }

    *addr = '\0';
    return cstr_to_string(retCStr);
  }

  return gconsts.g_string_blank;
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
      item = array_body_index(ap, i);
      if (is_object(item)) {
        // invokeToLocaleString(item, context, &prim);
        prim = item;   // kore wa tekitou
      }
      strs[i] = string_to_cstr(primitive_to_string(prim));
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
    set_a(context, gconsts.g_string_blank);
    return;
  }
}

#if 0
// ProtoJoin

// セパレータを指定して JOIN する。
// 指定されてなかった場合は、コンマで JOIN する。

BUILTIN_FUNCTION(arrayProtoJoin)
{
  int fp;
  JSValue* args;
  JSValue ret, separator;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  if(nArgs > 0){
    separator = args[1];
    if(is_object(separator)){
      separator = objectToPrimitiveHintString(separator, context); }
    ret = arrayToString(context, args[0], PrimitiveToString(separator));
  }else{
    ret = arrayToString(context, args[0], gStringComma);
  }
  setA(context, ret);
  return;
}

// -------------------------------------------------------------------------------------
// ProtoConcat
// 引数で与えられたものを配列に確保していく

BUILTIN_FUNCTION(arrayProtoConcat)
{
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

      // 配列だった場合は配列の中身を全て格納
      length = getArrayLength(item);
      for(j=0; j<length; j++){
        getArrayValue(item, j, &v);
        setArrayValue(ret, putPoint++, v); }

    }else{

      // 配列以外だったらそのまま格納
      setArrayValue(ret, putPoint++, item);
    }
  }

  setA(context, ret);
  return;
}

// -------------------------------------------------------------------------------------
// ProtoPop
// 配列から一番後ろの要素から取る

BUILTIN_FUNCTION(arrayProtoPop)
{
  int fp;
  JSValue* args;
  JSValue rsv, ret;
  uint64_t length;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  length = getArrayLength(rsv);
  getArrayValue(rsv, (int)(length-1), &ret);
  set_prop_none(rsv, gconsts.g_string_length, intToFixnum(length-1));
  setArrayLength(rsv, length-1);
  setA(context, ret);
  return;
}

// -------------------------------------------------------------------------------------
// ProtoPush
// 配列にデータを格納する

BUILTIN_FUNCTION(arrayProtoPush)
{
  JSValue* args;
  JSValue rsv;
  int i, fp;
  uint64_t length;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  rsv = args[0];

  length = getArrayLength(rsv);
  for(i=1; i<=nArgs; i++){
    setArrayValue(rsv, (int)(length++), args[i]); }

  setArrayLength(rsv, length);
  setA(context, intToFixnum(i));
  return;
}

// -------------------------------------------------------------------------------------
// ProtoReverse
// 配列のデータをひっくり返す

BUILTIN_FUNCTION(arrayProtoReverse)
{
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
}

// -------------------------------------------------------------------------------------
// ProtoShift

// 配列の先頭の要素を削除する
// 返り値として先頭の要素を与える
// 配列の長さが間違っていたのを修正

BUILTIN_FUNCTION(arrayProtoShift)
{
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
}


// -------------------------------------------------------------------------------------
// ProtoUnShift
// http://www.tohoho-web.com/js/array.htm#unshift
// 未実装

// -------------------------------------------------------------------------------------
// ProtoSplice
// http://www.tohoho-web.com/js/array.htm#splice
// 未実装

// -------------------------------------------------------------------------------------
// ProtoSlice

BUILTIN_FUNCTION(arrayProtoSlice)
{
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

  // スタートの位置を測定
  if(is_object(startv)){
    startv = objectToPrimitive(startv, context); }
  dstart = PrimitiveToIntegralDouble(PrimitiveToDouble(startv));

  if(dstart < 0){
    start = (int)max(dstart + ((double)length), 0);
  }else{ start = (int)min(dstart, length); }

  // エンドの位置を測定
  // Undef の場合は最後まで
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

  // 新しい配列を作成する
  newLength = end - start;
  array = newArrayWithSize(newLength);
  set_prop_all(array, gconsts.g_string___proto__, gArrayProto);

  for(i = 0; i < newLength; i++){
    getArrayValue(rsv, i + start, &src);
    setArrayValue(array, i, src); }
  setA(context, array);
  return;

}

// -------------------------------------------------------------------------------------
// ProtoSort

BUILTIN_FUNCTION(arrayProtoSort)
{
  JSValue* args;
  JSValue rsv, comp, lenv, iv, jv, temp;
  int i, j, index, fp, length;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  rsv = args[0];
  comp = args[1];
  getProp(rsv, cStrToString("length"), &lenv);
  length = (int)fixnumToInt(PrimitiveToInteger(lenv));

  // 選択ソートでソートを行う
  // ソートアルゴリズムだれか改良して
  if(isArray(rsv)){

    // 比較関数を受け取っていない場合
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

    // 比較関数を受け取った場合
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

  // レシーバが配列じゃない場合は強制終了
  LOG_EXIT("receiver is not array\n");
}

#endif

ObjBuiltinProp array_funcs[] = {
  { "toString",       array_toString,       0, ATTR_DE },
  { "toLocateString", array_toLocaleString, 0, ATTR_DE },
//  { "join",           array_join,           1, ATTR_DE },
//  { "concat",         array_concat,         1, ATTR_DE },
//  { "pop",            array_pop,            0, ATTR_DE },
//  { "push",           array_push,           1, ATTR_DE },
//  { "reverse",        array_reverse,        0, ATTR_DE },
//  { "shift",          array_shift,          1, ATTR_DE },
//  { "slice",          array_slice,          2, ATTR_DE },
//  { "sort",           array_sort,           1, ATTR_DE },
  { NULL,             NULL,                  0, ATTR_DE }
};

void init_builtin_array(void)
{
  gconsts.g_array = new_builtin(array_constr, 0);
  gconsts.g_array_proto = new_object();
  set_prop_all(gconsts.g_array, gconsts.g_string_prototype, gconsts.g_array_proto);
  {
    ObjBuiltinProp *p = array_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(gconsts.g_array_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}
