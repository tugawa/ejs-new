#include "prefix.h"
#define EXTERN extern
#include "header.h"

#define get_args()  ((JSValue *)(&(get_stack(context, fp))))

#define builtin_prologue() \
  int fp; JSValue *args; fp = get_fp(context); args = get_args()

// loading a constant value
//

BUILTIN_FUNCTION(builtin_const_true)
{
  set_a(context, gobj.g_string_true);
}

BUILTIN_FUNCTION(builtin_const_false)
{
  set_a(context, gobj.g_string_false);
}

BUILTIN_FUNCTION(builtin_const_undefined)
{
  set_a(context, gobj.g_string_undefined);
}

BUILTIN_FUNCTION(builtin_const_null)
{
  set_a(context, gobj.g_string_null);
}

// identity function
//

BUILTIN_FUNCTION(builtin_identity)
{
  builtin_prologue();
  set_a(context, args[0]);
}

// builtin-functions for debugging

// displays its arguments
//
BUILTIN_FUNCTION(builtin_print)
{
  int i;

  builtin_prologue();
  // printf("builtin_print: na = %d, args = %p\n", na, args);

  for (i = 1; i <= na; ++i) {
     //printf("args[%d] = %016lx\n", i, args[i]);
     print_value_simple(context, args[i]);
    putchar(i < na ? ' ' : '\n');
  }
  set_a(context, JS_UNDEFINED);
}

// displays the status
//
BUILTIN_FUNCTION(builtin_printStatus)
{
  int fp;
  JSValue *regBase;

  fp = get_fp(context);
  regBase = (JSValue*)(&(get_stack(context, fp-1)));
  LOG_ERR("\n-----current spreg-----\ncf = %p\nfp = %d\npc = %d\nlp = %p\n",
          (FunctionCell *)regBase[-CF_POS],
          (int)regBase[-FP_POS],
          (int)regBase[-PC_POS],
          (void *)regBase[-LP_POS]);

  regBase = (JSValue*)(&(get_stack(context, regBase[-FP_POS] - 1)));
  LOG_ERR("\n-----prev spreg-----\ncf = %p\nfp = %d\npc = %d\nlp = %p\n",
          (FunctionCell *)regBase[-CF_POS],
          (int)regBase[-FP_POS],
          (int)regBase[-PC_POS],
          (void *)regBase[-LP_POS]);
}

// displays the address of an object
//
BUILTIN_FUNCTION(builtin_address)
{
  JSValue obj;

  builtin_prologue();
  obj = args[1];
  printf("0x%lx\n", obj);
  set_a(context, JS_UNDEFINED);
}

// prints ``hello, world''
//
BUILTIN_FUNCTION(builtin_hello)
{
  LOG("hello, world\n");
  set_a(context, JS_UNDEFINED);
}

#ifdef USE_PAPI
// obtains the real usec
//
BUILTIN_FUNCTION(builtin_papi_get_real)
{
  long long now = PAPI_get_real_usec();
  set_a(context, int_to_fixnum(now));
}
#endif // USE_PAPI

// constructor for an object
//
BUILTIN_FUNCTION(object_constr)
{
  JSValue rsv;
  uint64_t tag;
  JSValue ret;

  builtin_prologue();
  rsv = args[0];

  // If this is called with `new', which kind of object is allocated
  // depends on the type of the first argument.
  if (na > 0) {
    JSValue arg;
    arg = args[1];
    tag = get_tag(arg);
    switch(tag){
    case T_OBJECT:
      ret = arg;
      break;
    case T_FIXNUM:
    case T_FLONUM:
      ret = new_number(arg);
      set_obj_prop(ret, "__proto__", gobj.g_number_proto, ATTR_ALL);
      break;
    case T_SPECIAL:
      if (is_true(arg) || is_false(arg)) {
        ret = new_boolean(arg);
        set_obj_prop(ret, "__proto__", gobj.g_boolean_proto, ATTR_ALL);
      } else {
        ret = new_object();
        set_obj_prop(ret, "__proto__", gobj.g_object_proto, ATTR_ALL);
      }
      break;
    case T_STRING:
      ret = new_string(arg);
      set_obj_prop(ret, "__proto__", gobj.g_string_proto, ATTR_ALL);
      break;
    }
  } else {
    ret = new_object();
    set_obj_prop(ret, "__proto__", gobj.g_object_proto, ATTR_ALL);
  }
  set_a(context, ret);
}

// constructor for array
//
BUILTIN_FUNCTION(array_constr)
{
  cint length = 0;
  JSValue rsv;
  ArrayCell *p;

  builtin_prologue();
  rsv = new_array();
  p = (ArrayCell *)(remove_tag(rsv, T_OBJECT));
  set_obj_prop(rsv, "__proto__", gobj.g_array_proto, ATTR_ALL);

  switch (na) {
  case 0:
    {
      allocate_array_data(p, INITIAL_ARRAY_SIZE, 0);
      /*
      array_body(p) = allocate_array_data(INITIAL_ARRAY_SIZE);
      array_size(p) = INITIAL_ARRAY_SIZE;
      array_length(p) = 0;
      */
    }
    break;

  case 1:
    {
      JSValue num = args[1];
      cint size = INITIAL_ARRAY_SIZE;

      if (is_fixnum(num) && FIXNUM_ZERO <= num) {
        length = fixnum_to_cint(num);
        while (size < length) size <<= 1;
        allocate_array_data(p, size, length);
        /*
        array_body(p) = allocate_array_data(size);
        array_size(p) = size;
        array_length(p) = length;
        */
        set_obj_prop_none(rsv, "length", cint_to_fixnum(length));
      } else {
        array_body(p) = NULL;
        array_size(p) = 0;
        array_length(p) = 0;
      }
    }
    break;

  default:
    {
      // not implemented yet
      array_body(p) = NULL;
      array_size(p) = 0;
      array_length(p) = 0;
    }
    break;
  }

  /*
  for (i = 0; i < length; i++)
    array_body_index(p, i) = JS_UNDEFINED);
  */
  set_a(context, rsv);
}

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
    arg = gobj.g_string_blank;
  set_a(context, arg);
}

// constructor of a number 
//
BUILTIN_FUNCTION(number_constr)
{
  JSValue rsv;

  builtin_prologue();  
  rsv = new_number(FIXNUM_ZERO);
  set_obj_prop(rsv, "__proto__", gobj.g_number_proto, ATTR_ALL);
  if (na > 0)
    number_object_value(rsv) = to_number(context, args[1]);
  set_a(context, rsv);
}

// constructor of a number (not OBJECT)
//
BUILTIN_FUNCTION(number_constr_nonew)
{
  JSValue rsv, ret;
  
  builtin_prologue();
  rsv = args[0];
  if (na > 0)
    ret = to_number(context, args[1]);
  else
    ret = FIXNUM_ZERO;
  set_a(context, ret);
}

// constructor of a boolean
//
BUILTIN_FUNCTION(boolean_constr)
{
  JSValue rsv;

  builtin_prologue();  
  rsv = new_boolean(JS_TRUE);
  set_obj_prop(rsv, "__proto__", gobj.g_boolean_proto, ATTR_ALL);
  if (na > 0)
    boolean_object_value(rsv) = to_boolean(args[1]);
  set_a(context, rsv);
}

BUILTIN_FUNCTION(builtin_object_proto_to_string)
{
  set_a(context, gobj.g_string_objtostr);
}

// functions for data conversion

#define TEMPSIZE 1000

BUILTIN_FUNCTION(builtin_fixnum_to_string)
{
  builtin_prologue();  
  set_a(context, fixnum_to_string(args[0]));
}

BUILTIN_FUNCTION(builtin_flonum_to_string)
{
  builtin_prologue();  
  set_a(context, fixnum_to_string(args[0]));
}

BUILTIN_FUNCTION(builtin_string_to_index)
{
  builtin_prologue();
  set_a(context, string_to_index(args[0]));
}

BUILTIN_FUNCTION(builtin_string_valueOf)
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

BUILTIN_FUNCTION(builtin_number_valueOf)
{
  JSValue arg;

  builtin_prologue();  
  arg = args[0];
  if (is_number_object(arg))
    arg = number_object_value(arg);
  set_a(context, arg);
}

BUILTIN_FUNCTION(builtin_boolean_valueOf)
{
  JSValue arg;

  builtin_prologue();  
  arg = args[0];
  if (is_boolean_object(arg))
    arg = boolean_object_value(arg);
  set_a(context, arg);
}

// isNAN
//
BUILTIN_FUNCTION(builtin_is_nan)
{
  JSValue v;

  builtin_prologue();  
  v = args[1];
  if (is_object(v))
    v = objectToPrimitiveHintNumber(v, context);
  set_a(context, int_to_boolean(isnan(primitive_to_double(v))));
}

// isFinite
//
BUILTIN_FUNCTION(builtin_is_finite)
{
  JSValue v;
  double x;

  builtin_prologue();  
  v = args[1];
  if (is_object(v))
    v = objectToPrimitiveHintNumber(v, context);
  x = primitive_to_double(v);
  set_a(context, int_to_boolean(!(isnan(x) || isinf(x))));
}

// parseInt str rad
// converts a string to a number

/**
 * @brief 文字列を数値に書き換える
 * rad は何処まで丸めるか？
 */
BUILTIN_FUNCTION(builtin_parse_int)
{
  JSValue str, rad;
  char *cstr;
  char *endPtr;
  int32_t irad;
  long ret;

  builtin_prologue();  
  str = args[1];
  rad = args[2];

  if (is_object(str))
    str = objectToPrimitiveHintString(str, context);
  if (is_object(rad))
    rad = objectToPrimitiveHintNumber(rad, context);

  str = primitive_to_string(str);
  cstr = string_to_cstr(str);

  if (!is_undefined(rad)) {
    irad = (int32_t)primitive_to_double(rad);
    if (irad < 2 || irad > 36) {
      set_a(context, gobj.g_flonum_nan);
      return;
    }
  } else
    irad = 10;

  cstr = space_chomp(cstr);
  ret = strtol(cstr, &endPtr, irad);
  if (cstr == endPtr)
    set_a(context, gobj.g_flonum_nan);
  else
    set_a(context, int_to_fixnum(ret));
}

// -------------------------------------------------------------------------------------
// parseFloat str
/**
 * @brief 文字列を数値に書き換える
 */

BUILTIN_FUNCTION(builtin_parse_float)
{
  JSValue str;
  char *cstr;
  double x;

  builtin_prologue();  
  str = args[1];

  if(is_object(str)){
    str = objectToPrimitiveHintString(str, context); }

  str = primitive_to_string(str);
  cstr = string_to_cstr(str);
  cstr = space_chomp(cstr);

  x = strtod(cstr, NULL);
  if (is_fixnum_range_double(x))
    set_a(context, double_to_fixnum(x));
  else
    set_a(context, double_to_flonum(x));
}

// ProtoToString
BUILTIN_FUNCTION(array_proto_toString)
{
  JSValue ret;

  builtin_prologue();  
  ret = array_to_string(context, args[0], gobj.g_string_comma);
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

  ret = gobj.g_string_blank;
  length = array_length(array);

  if(length > 0){
    strs = (char**)malloc(sizeof(char*) * length);
    separatorCStr = string_to_cstr(separator);
    separatorLength = strlen(separatorCStr);
    sumLength = 0;

    for(i=0; i<length; i++){
      array_value(array, i, &item);
      if(is_object(item)){
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

  return gobj.g_string_blank;
}

// ProtoToLocalString

// 配列で渡されたものを全てひっくるめて
// カンマで implode された文字列を返す

BUILTIN_FUNCTION(array_proto_toLocaleString){
  int i;
  uint64_t length, sumLength;
  JSValue array, item, prim;
  char **strs;
  char *retCStr, *addr;

  builtin_prologue();  
  array = args[0];
  length = array_length(array);

  if(length > 0){
    strs = (char **)malloc(sizeof(char*)*length);
    sumLength = 0;

    // 配列の中身の文字列を取得
    for(i=0; i<length; i++){
      array_value(array, i, &item);
      if(is_object(item)){
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
    set_a(context, gobj.g_string_blank);
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
  set_obj_prop(ret, "__proto__", gArrayProto, ATTR_ALL);


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
  set_obj_prop_none(rsv, "length", intToFixnum(length-1));
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

    set_obj_prop_none(rsv, "length", intToFixnum(length-1));
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
  set_obj_prop(array, "__proto__", gArrayProto, ATTR_ALL);

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

// 日時関係関数群

#if 0
BUILTIN_FUNCTION(dateProtoToString){}
BUILTIN_FUNCTION(dateProtoToDateString){}
BUILTIN_FUNCTION(dateProtoToTimeString){}
BUILTIN_FUNCTION(dateProtoToLocaleString){}
BUILTIN_FUNCTION(dateProtoToLocaleDateString){}
BUILTIN_FUNCTION(dateProtoToLocaleTimeString){}
BUILTIN_FUNCTION(dateProtoValueOf){}
BUILTIN_FUNCTION(dateProtoGetTime){}
BUILTIN_FUNCTION(dateProtoGetFullYear){}
BUILTIN_FUNCTION(dateProtoGetUTCFullYear){}
BUILTIN_FUNCTION(dateProtoGetMonth){}
BUILTIN_FUNCTION(dateProtoGetUTCMonth){}
BUILTIN_FUNCTION(dateProtoGetDate){}
BUILTIN_FUNCTION(dateProtoGetUTCDate){}
BUILTIN_FUNCTION(dateProtoGetDay){}
BUILTIN_FUNCTION(dateProtoGetUTCDay){}
BUILTIN_FUNCTION(dateProtoGetHours){}
BUILTIN_FUNCTION(dateProtoGetUTCHours){}
BUILTIN_FUNCTION(dateProtoGetMinutes){}
BUILTIN_FUNCTION(dateProtoGetUTCMinutes){}
BUILTIN_FUNCTION(dateProtoGetSeconds){}
BUILTIN_FUNCTION(dateProtoGetUTCSeconds){}
BUILTIN_FUNCTION(dateProtoGetMilliseconds){}
BUILTIN_FUNCTION(dateProtoGetUTCMilliseconds){}
BUILTIN_FUNCTION(dateProtoGetTimezoneOffset){}
BUILTIN_FUNCTION(dateProtoSetTime){}
BUILTIN_FUNCTION(dateProtoSetMillisecnods){}
BUILTIN_FUNCTION(dateProtoSetUTCMillisecnods){}
BUILTIN_FUNCTION(dateProtoSetSeconds){}
BUILTIN_FUNCTION(dateProtoSetUTCSeconds){}
BUILTIN_FUNCTION(dateProtoSetMinutes){}
BUILTIN_FUNCTION(dateProtoSetUTCMinutes){}
BUILTIN_FUNCTION(dateProtoSetHours){}
BUILTIN_FUNCTION(dateProtoSetUTCHours){}
BUILTIN_FUNCTION(dateProtoSetDate){}
BUILTIN_FUNCTION(dateProtoSetUTCDate){}
BUILTIN_FUNCTION(dateProtoSetMonth){}
BUILTIN_FUNCTION(dateProtoSetUTCMonth){}
BUILTIN_FUNCTION(dateProtoSetFullYear){}
BUILTIN_FUNCTION(dateProtoSetUTCFullYear){}
BUILTIN_FUNCTION(dateProtoToUTCString){}
#endif

void math_func(Context *context, int na, double (*fn)()) {
  JSValue v;
  double x;

  builtin_prologue();
  v = args[1];
  if (is_object(v)) 
    v = objectToPrimitiveHintNumber(v, context);
  x = (*fn)(primitive_to_double(v));
  if (is_fixnum_range_double(x))
    set_a(context, double_to_fixnum(x));
  else
    set_a(context, double_to_flonum(x));
}

BUILTIN_FUNCTION(math_abs)
{
  math_func(context, na, &fabs);
}

/*
BUILTIN_FUNCTION(math_abs)
{
  JSValue v;
  double x;

  builtin_prologue();
  v = args[1];
  if (is_object(v)) 
    v = objectToPrimitiveHintNumber(v, context);
  x = fabs(primitive_to_double(v));

  if (is_fixnum_range_double(x))
    set_a(context, double_to_fixnum(x));
  else
    set_a(context, double_to_flonum(x));
}
*/

// -------------------------------------------------------------------------------------
// mathAcos

#if 0
BUILTIN_FUNCTION(mathAcos)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  v = args[1];
  if(isObject(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  if((x<-1.0) || (x>1.0)){
    setA(context, gFlonum_NaN);
  }else{
    ret = acos(x);
    if(isInFixnumRange(ret)){
      setA(context, doubleToFixnum(ret));
    }else{
      setA(context, doubleToFlonum(ret));
    }
  }
}
#endif

// mathAsin

#if 0
BUILTIN_FUNCTION(mathAsin)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));

  v = args[1];
  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  if((x<-1.0) || (x>1.0)){
    setA(context, gFlonum_NaN);
  }else{
    ret = asin(x);
    if(isInFixnumRange(ret)){
      setA(context, doubleToFixnum(ret));
    }else{
      setA(context, doubleToFlonum(ret));
    }
  }
}
#endif

// mathAtan

#if 0
BUILTIN_FUNCTION(mathAtan)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];
  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context);
  }
  x = PrimitiveToDouble(v);
  if(x < -1.0 || x > 1.0){
    setA(context, gFlonum_NaN);
  }else{
    ret = atan(x);
    if(isInFixnumRange(ret)){
      setA(context, doubleToFixnum(ret));
    }else{
      setA(context, doubleToFlonum(ret));
    }
  }
}
#endif

// mathAtan2

#if 0
BUILTIN_FUNCTION(mathAtan2)
{
  JSValue* args;
  JSValue v1, v2;
  int fp;
  double x, y;
  double ret;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v1 = args[1];
  v2 = args[2];

  // 型変換
  if(is_object(v1)){
    v1 = objectToPrimitiveHintNumber(v1, context); }
  if(is_object(v2)){
    v2 = objectToPrimitiveHintNumber(v2, context); }
  y = PrimitiveToDouble(v1);
  x = PrimitiveToDouble(v2);

  ret = atan2(y, x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}

#endif

// mathCeil

#if 0
BUILTIN_FUNCTION(mathCeil)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);
  ret = ceil(x);

  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathCos

#if 0
BUILTIN_FUNCTION(mathCos)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = cos(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathExp

#if 0
BUILTIN_FUNCTION(mathExp)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = exp(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathFloor

#if 0
BUILTIN_FUNCTION(mathFloor)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;
  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = floor(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathLog

#if 0
BUILTIN_FUNCTION(mathLog)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = log(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}

#endif

// mathMax

#if 0
BUILTIN_FUNCTION(mathMax)
{
  int i, fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  ret = -INFINITY;

  for(i=1; i<=nArgs; i++){
    v = args[i];
    if(is_object(v)){
      v = objectToPrimitiveHintNumber(v, context); }
    x = PrimitiveToDouble(v);

    if(isnan(x)){
      setA(context, gFlonum_NaN);
      return; }

    // 最大値を記録する
    if(x > ret){
      ret = x; }}

  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathMin

#if 0
BUILTIN_FUNCTION(mathMin)
{
  int i, fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  ret = INFINITY;

  for(i=1; i<=nArgs; i++){
    v = args[i];
    if(is_object(v)){
      v = objectToPrimitiveHintNumber(v, context); }
    x = PrimitiveToDouble(v);

    if(isnan(x)){
      setA(context, gFlonum_NaN);
      return; }

    // 最小値を記録する
    if(x < ret){
      ret = x;}}

  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathPow

#if 0
BUILTIN_FUNCTION(mathPow)
{
  int fp;
  double x, y, ret;
  JSValue* args;
  JSValue v1, v2;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v1 = args[1];
  v2 = args[2];

  if(is_object(v1)){
    v1 = objectToPrimitiveHintNumber(v1, context); }
  if(is_object(v2)){
    v2 = objectToPrimitiveHintNumber(v2, context); }
  x = PrimitiveToDouble(v1);
  y = PrimitiveToDouble(v2);

  ret = pow(x, y);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathRandom

#if 0
BUILTIN_FUNCTION(mathRandom)
{
  double ret;
  ret = ((double)rand()) / (((double)RAND_MAX) + 1);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathRound

#if 0
BUILTIN_FUNCTION(mathRound)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = round(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathSin

#if 0
BUILTIN_FUNCTION(mathSin)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = sin(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif
// mathSqrt

#if 0
BUILTIN_FUNCTION(mathSqrt)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = sqrt(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

// mathTan

#if 0
BUILTIN_FUNCTION(mathTan)
{
  int fp;
  double x, ret;
  JSValue* args;
  JSValue v;

  fp = getFp(context);
  args = (JSValue*)(&Stack(context, fp));
  v = args[1];

  if(is_object(v)){
    v = objectToPrimitiveHintNumber(v, context); }
  x = PrimitiveToDouble(v);

  ret = tan(x);
  if(isInFixnumRange(ret)){
    setA(context, doubleToFixnum(ret));
  }else{
    setA(context, doubleToFlonum(ret));
  }
}
#endif

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
      set_obj_prop(reg, "lastIndex", FIXNUM_ZERO, ATTR_DDDE); }
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

BUILTIN_FUNCTION(number_proto_valueOf)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];
  if (is_number_object(rsv))
    set_a(context, number_object_value(rsv));
  else
    LOG_EXIT("Receiver of valueOf is not a Number instance\n");
}

BUILTIN_FUNCTION(number_proto_toString)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];
  if (is_number_object(rsv)) {
    if (na == 0)
      set_a(context, primitive_to_string(number_object_value(rsv)));

    // 引数を取り、１０進数指定か、または正しくない場合の処理
    else if (args[1] == JS_UNDEFINED || args[1] == FIXNUM_TEN)
      set_a(context, primitive_to_string(number_object_value(rsv)));

    // n進数文字列変換
    else {

      if(!get_tag(args[1]) == T_FIXNUM){
        LOG_ERR("args[1] is not a fixnum.");
        set_a(context, JS_UNDEFINED); }

      int n = (int)fixnum_to_int(args[1]);
      JSValue v = number_object_value(rsv);
      char map[36] = "0123456789abcdefghijklmnopqrstuvwxyz";

      int i, ff, acc;
      uint32_t numeric;
      double decimal;
      int nlen, dlen;
      char str[100];
      nlen = dlen = 0;


      // 小数点部と分離させる
      if(get_tag(v) == T_FIXNUM){
        numeric = (int)fixnum_to_int(v);
        decimal = 0.0;
      }else{
        numeric = (int)(flonum_to_double(v));
        decimal = flonum_to_double(v) - numeric; }

      // 整数部の展開
      while(numeric >= n){
        str[nlen++] = map[numeric%n];
        numeric /= n; }
      str[nlen++] = map[numeric];

      // 整数部をひっくり返す
      for(i=0; i<nlen/2; i++){
        ff = str[nlen-1-i];
        str[nlen-1-i] = str[i];
        str[i] = ff; }
      str[nlen++] = '.';

      // 小数部の展開
      // 小数点制度は以下の式により決定する (実装依存個所)
      acc = (int)(48/((int)(log(n)/log(2))));
      while((decimal != 0.0) && (dlen < acc)){
        str[nlen+dlen++] = map[(int)(decimal*n)];
        decimal = decimal*n - (int)(decimal*n); }
      str[nlen+dlen++] = '\0';

      set_a(context, cstr_to_string(strdup(str)));
    }

  }else if(is_number(rsv)){
    set_a(context, primitive_to_string(rsv));

  }else{

    // Type Error 例外処理をする [FIXME]
    LOG_EXIT("Number Instance's valueOf received not Number Instance\n");
  }
}

// 正規表現関連

// -------------------------------------------------------------------------------------
// regExpProtoToString

#ifdef USE_REGEXP
BUILTIN_FUNCTION(regExpProtoToString)
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
#endif


// -------------------------------------------------------------------------------------
// regExpProtoExec
// 実行

#ifdef USE_REGEXP
BUILTIN_FUNCTION(regExpProtoExec)
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
#endif

// -------------------------------------------------------------------------------------
// regExpProtoTest
// 実行

#ifdef USE_REGEXP
BUILTIN_FUNCTION(regExpProtoTest)
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
#endif


// -------------------------------------------------------------------------------------
// regexpConstructor

#ifdef USE_REGEXP
BUILTIN_FUNCTION(regexpConstructor)
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
#endif



#ifdef USE_REGEXP
BUILTIN_FUNCTION(regexpConstructorNoNew)
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
#endif

// -------------------------------------------------------------------------------------
// regExpProtoExecSub

#ifdef USE_REGEXP
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
#endif

// -------------------------------------------------------------------------------------
// regExpProtoExecSub

#ifdef USE_REGEXP
JSValue regExpMatchToString(const char* str, int start, int end)
{
  int length = end - start;
  char *ret = malloc((sizeof(char)*length) +1);
  memcpy(ret, str + start, length);
  ret[length] = '\0';
  return cStrToString(ret);
}
#endif

#ifdef USE_REGEXP
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
#endif



#ifdef USE_REGEXP
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
#endif

#ifdef USE_REGEXP
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
#endif
