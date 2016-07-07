#include "prefix.h"
#define EXTERN
#include "header.h"

#define PROP_REALLOC_THRESHOLD (0.75)

#define array_subscript_range(n) (0 < (n) && (n) < MINIMUM_ARRAY_SIZE)

#define prop_overflow(o) \
  (obj_n_props(o) > (obj_limit_props(o) * PROP_REALLOC_THRESHOLD))

#define sign(x) ((x) > 0? 1: -1)

// obtains the index of a property of an Object
// If the specified property does not exist, returns -1.
//
int prop_index(JSValue obj, JSValue name)
{
  HashData retv;
  int result;

  if (!is_object(obj)) return -1;
  result = hash_get(obj_map(obj), name, &retv);
  if (result == HASH_GET_FAILED) return -1;
  else return (int)retv;
}

// obtains the property value of the key ``name'', stores it to *ret
// and returns SUCCESS or FAIL
// This function does not follow the prototype chain.
//
int get_prop(JSValue obj, JSValue name, JSValue *ret)
{
  int index;

  index = prop_index(obj, name);
  if (index == -1) return FAIL;

  *ret = obj_prop_index(obj, index);
  return SUCCESS;
}

// obtains the property from an object by following the prototype chain
// (if necessary)
//   o: object
//   p: property, which is a string
//
JSValue get_prop_prototype_chain(JSValue o, JSValue p) {
  JSValue ret;
  extern JSValue prototype_object(JSValue);

  do {
    if (get_prop(o, p, &ret) == SUCCESS) return ret;
  } while (get_prop(o, gconsts.g_string___proto__, &o) == SUCCESS);
  // is it necessary to search in the Object's prototype?
  return JS_UNDEFINED;
}

// obtains object's property
//   o: object (but not an array)
//   p: property (number / string / other type)
// It is not necessary to check the type of `o'.
//

JSValue get_object_prop(Context *context, JSValue o, JSValue p) {
  /*
    if (p is not a string) p = to_string(p);
      returns the value regsitered under the property p
    }
  */
  // printf("get_object_prop, o = %016lx, p = %016lx\n", o, p);
  if (!is_string(p)) p = to_string(context, p);
  return get_prop_prototype_chain(o, p);
}

// obtains array's property
//   a: array
//   p: property (number / string / other type)
// It is not necessary to check the type of `a'.
//
JSValue get_array_prop(Context *context, JSValue a, JSValue p) {
  /*
    if (p a number) {
      if (p is within the range of an subscript of an array)
        returns the p-th element of a
      else {
        p = number_to_string(idx);
        returns the value regsitered under the property p
      }
    } else {
      if (p is not a string) p = to_string(p);
      s = string_to_number(p);
      if (s is within the range of an subscript of an array)
        returns the s-th element of a
      else
        returns the value regsitered under the property p
    }
  */
  ArrayCell *ap;

  switch (get_tag(p)) {
  case T_FIXNUM:
    {
      cint n;
      n = fixnum_to_cint(p);
      if (array_subscript_range(n)) {
        ap = remove_array_tag(a);
        return (n < array_length(ap))? array_body_index(ap, n): JS_UNDEFINED;
      }
      p = fixnum_to_string(p);
      return get_prop_prototype_chain(a, p);
    }
    break;
  default:
   p = to_string(context, p);
   // fall through
  case T_STRING:
    {
      JSValue num;
      cint n;
      num = string_to_number(p);
      if (is_fixnum(num)) {
        n = fixnum_to_cint(num);
        if (array_subscript_range(n)) {
          ap = remove_array_tag(a);
          return (n < array_length(ap))? array_body_index(ap, n): JS_UNDEFINED;
        }
      }
      return get_prop_prototype_chain(a, p);
    }
    break;
  }
}          

// sets an object's property value with its attribute
//
int set_prop_with_attribute(JSValue obj, JSValue name, JSValue v, Attribute attr) {
  uint64_t retv;

  if (hash_get(obj_map(obj), name, (HashData *)(&retv)) == HASH_GET_FAILED) {
    // The specified property is not registered in the hash table.
    if (prop_overflow(obj)) {
      LOG_ERR("proptable overflow\n");
      return FAIL;
    }
    retv = ++(obj_n_props(obj));
    if (hash_put_with_attribute(obj_map(obj), name, retv, attr)
          == HASH_PUT_SUCCESS) {
      obj_prop_index(obj, (int)retv) = v;
      return SUCCESS;
    } else
      return FAIL;
  } else {
    // returned value is HASH_GET_SUCCESS
    // There is already the property `name', overwrites its value.
    obj_prop_index(obj, (int)retv) = v;
    return SUCCESS;
  }
}

// sets object's property
//   o: object (but not an array)
//   p: property (number / string / other type)
//   v: value to be set
// It is not necessary to check the type of `o'.
//
int set_object_prop(Context *context, JSValue o, JSValue p, JSValue v) {
  if (!is_string(p)) p = to_string(context, p);
  return set_prop_none(o, p, v);
}

// sets array's property
//   a: array
//   p: property (number / string / other type)
//   v: value to be set
// It is not necessary to check the type of `a'.
//
int set_array_prop(Context *context, JSValue a, JSValue p, JSValue v) {
  ArrayCell *ap;

  switch (get_tag(p)) {
  case T_FIXNUM:
    {
      cint n;
      n = fixnum_to_cint(p);
      if (array_subscript_range(n)) {
        ap = remove_array_tag(a);
        if (n < array_length(ap)) {
          array_body_index(ap, n) = v;
          return SUCCESS;
        } else {
          // expand the array --- not implemented yet
          return SUCCESS;
        }
      }
      p = fixnum_to_string(p);
      return set_object_prop(context, a, p, v);
    }
    break;
  default:
   p = to_string(context, p);
   // fall through
  case T_STRING:
    {
      JSValue num;
      cint n;
      num = string_to_number(p);
      if (is_fixnum(num)) {
        n = fixnum_to_cint(num);
        if (array_subscript_range(n)) {
          ap = remove_array_tag(a);
          if (n < array_length(ap)) {
            array_body_index(ap, n) = v;
            return SUCCESS;
          } else {
            // expand the array --- not implemented yet
            return SUCCESS;
          }
        }
      }
      // if the property name is "length", expand / shrink the array
      // -- not implemented
      return set_object_prop(context, a, p, v);
    }
    break;
  }
}

#if 0
/**
 * @brief 配列の要素配列を拡張する
 * @param 対象の配列
 * @param 新たなサイズ
 */
static inline void setArrayBody(JSValue array, int size)
{
  if(size < MINIMUM_ARRAY_SIZE)
    size = MINIMUM_ARRAY_SIZE;
  ((ArrayCell*)array)->body = allocateArrayData(size);
  setArraySize(array, size);
}
#endif

#if 0
// ------------------------------------------------------------------
/**
 * @brief 次のイテレータのキーを取得する
 * @param 対象オブジェクト
 * @param 対応するイテレータ
 * @param 格納する変数
 * @return 成功値
 * @retval 1 成功
 * @retval 0 失敗：これ以上存在しない
 */

int nextPropName(JSValue obj, HashIterator *iter, HashKey *key)
{
  Entry e;
  int r;
  while((r = __hashNext(objectMap(obj), iter, &e)) != NO_MORE_CELL && e.attr & ATTR_DE){
  }
  if(r == NO_MORE_CELL){
    return 0;
  }else{
    *key = e.key;
    return 1;
  }
}

// ------------------------------------------------------------------
/**
 * @brief イテレータに数値を追加
 * @param 対象のイテレータ
 * @param インデックス
 */

void putIndexToIterator(JSValue iter, uint64_t index)
{
  // 文字列に変換して用いる
  char str[1000];
  snprintf(str, 1000 - 1, "%llu", (long long unsigned int)index);
  putCStrToIterator(iter, str);
}


// ------------------------------------------------------------------
/**
 * @brief 次のイテレータの名前を取得する
 * @param 対象イテレータ
 * @param 格納する変数
 */

int getNextName(JSValue iter, JSValue *name)
{
  HashData retv;
  int result;
  result = hashNext(objectMap(iter), &(getIteratorHashIterator(iter)), &retv);
  if(result == NO_MORE_CELL){
    return 0;
  }else{
    *name = objectProp(iter)[(int)retv];
    return 1;
  }
}

// ------------------------------------------------------------------
/**
 * @brief 配列の要素を格納する
 * @param 対象の配列
 * @param インデックス
 * @param 結果を格納する変数
 * @return 成功値
 */

void setArrayValue(JSValue obj, int index, JSValue src)
{
  int length, size, i;
  JSValue *oldbody;

  size = (int)getArraySize(obj);
  length = (int)getArrayLength(obj);

  // 現在の長さを超えたインデックスに書き込む場合
  if (index + 1 > length) {

    // 配列のサイズが足りないので拡張する
    if (index + 1 > size) {
      oldbody = getArrayBody(obj);
      setArrayBody(obj, size * 2);
      memcpy(getArrayBody(obj), oldbody, sizeof(JSValue) * size);
    }

    setArrayLength(obj, index + 1);

    // プロパティに格納するので、Fixnumの範囲かを確認
    // Fixnum の範囲なら Fixnum に変換して
    if (isInFixnumRange(index + 1)) {
      setObjPropNone(obj, "length", intToFixnum(index + 1));
    } else {
      setObjPropNone(obj, "length", doubleToFlonum((double)index + 1));
    }

    // Undefined で埋める
    for (i = length; i < index + 1; i++) {
      getArrayBody(obj)[i] = JS_UNDEFINED;
    }
  }
  getArrayBody(obj)[index] = src;
}

// makes an Oniguruma's regexp object of type regex_t
//
#ifdef USE_REGEXP
int make_onig_regexp(JSValue r, OnigOptionType option)
{
  OnigErrorInfo err;

  char *ps = regexp_pattern(r);
  char *pe = p + strlen(p);
  if (onig_new(&(regexp_reg(r)), (OnigUChar*)ps, (OnigUChar*)pe, option,
               ONIG_ENCODING_ASCII, ONIG_SYNTAX_DEFAULT, &err) == ONIG_NORMAL)
    return MAKE_REGEX_OBJECT_SUCCESS;
  else
    return MAKE_REGEX_OBJECT_FAILED;
}
#endif

#endif

void set_object_members(Object *p) {
  Map *a;

  a = malloc_hashtable();
  hash_create(a, INITIAL_HASH_SIZE);
  obj_map(p) = a;
  obj_prop(p) = allocate_prop_table(INITIAL_PROPTABLE_SIZE);
  obj_n_props(p) = 0;
  obj_limit_props(p) = INITIAL_PROPTABLE_SIZE;
}

// makes a new object
//
JSValue new_object(void)
{
  JSValue ret;
  Object *p;
#ifdef PARALLEL
  pthread_mutexattr_t attr;
#endif

  ret = make_object();
  p = remove_object_tag(ret);
  set_object_members(p);
#ifdef PARALLEL
  pthread_mutexattr_init(&attr);
  pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
  pthread_mutex_init(&(p->mutex), &attr);
  pthread_mutexattr_destroy(&attr);
#endif
  return ret;
}

// makes a new array
//
JSValue new_array(void) {
  JSValue ret;
  ArrayCell *p;

  ret = make_array();
  p = remove_array_tag(ret);
  set_object_members(&(p->o));
  allocate_array_data(p, 0, 0);
  set_prop_none(ret, gconsts.g_string_length, FIXNUM_ZERO);
  return ret;
}

// makes a new array with size
//
JSValue new_array_with_size(int size)
{
  JSValue ret;
  ArrayCell *p;

  ret = make_array();
  p = remove_array_tag(ret);
  set_object_members(&(p->o));
  allocate_array_data(p, size, size);
  set_prop_none(ret, gconsts.g_string_length, int_to_fixnum(size));
  return ret;
}

// makes a function object
// Is it ok to delete this def?
//
#if 0
JSValue new_function(void)
{
  FunctionCell *ret, *p;

  ret = (FunctionCell *)make_function();
  p = (FunctionCell *)(remove_tag(ret, T_OBJECT));
  set_object_members(&(p->o));
  return ret;
}
#endif

// makes a function
// The name of this function was formerly new_closure.
//
JSValue new_function(Context *context, Subscript subscr)
{
  JSValue ret;
  FunctionCell *p;

  ret = make_function();
  p = remove_function_tag(ret);
  set_object_members(&(p->o));
  func_table_entry(p) = &(context->function_table[subscr]);
  func_environment(p) = get_lp(context);
  set_prop_none(ret, gconsts.g_string_prototype, new_object());
  set_prop_none(ret, gconsts.g_string___proto__, gconsts.g_function_proto);
  return ret;
}

// makes a new built-in function object with constructor
//
JSValue new_builtin_with_constr(builtin_function_t f, builtin_function_t cons, int na) {
  JSValue ret;
  BuiltinCell *p;

  ret = make_builtin();
  p = remove_builtin_tag(ret);
  set_object_members(&(p->o));
  builtin_body(p) = f;
  builtin_constructor(p) = cons;
  builtin_n_args(p) = na;
  set_prop_none(ret, gconsts.g_string_prototype, new_object());
  return ret;
}

// makes a new built-in function object
//
JSValue new_builtin(builtin_function_t f, int na) {
  return new_builtin_with_constr(f, f, na);
}

// makes an iterator object
//
JSValue new_iterator(void) {
  JSValue ret;
  IteratorCell *p;

  ret = make_iterator();
  p = remove_iterator_tag(ret);
  set_object_members(&(p->o));
  return ret;
}

// makes a new regexp
//
#ifdef USE_REGEXP
JSValue new_regexp(void) {
  JSValue ret;
  RegexpCell *p;

  ret = make_regexp();
  p = remove_regexp_tag(ret);
  set_object_members(&(p->o));
  return ret;
}
#endif // USE_REGEXP

// makes a new boxed number
//
JSValue new_number(JSValue v) {
  JSValue ret;
  BoxedCell *p;

  ret = make_number_object();
  p = remove_boxed_tag(ret);
  set_object_members(&(p->o));
  set_number_object_value(p, v);
  return ret;
}

// makes a new boxed boolean
//
JSValue new_boolean(JSValue v) {
  JSValue ret;
  BoxedCell *p;

  ret = make_boolean_object();
  p = remove_boxed_tag(ret);
  set_object_members(&(p->o));
  set_boolean_object_value(p, v);
  return (JSValue)ret;
}

// makes a new boxed string
//
JSValue new_string(JSValue v) {
  JSValue ret;
  BoxedCell *p;

  ret = make_string_object();
  p = remove_boxed_tag(ret);
  set_object_members(&(p->o));
  set_string_object_value(p, v);

  // A boxed string has a property ``length'' whose associated value
  // is the length of the string.
  set_prop_all(ret, gconsts.g_string_length, int_to_fixnum(strlen(string_to_cstr(v))));
  return ret;
}

// data conversion functions
//
char *space_chomp(char *str) {
  while (isspace(*str)) str++;
  return str;
}

double cstr_to_double(char* cstr) {
  char* endPtr;
  double ret;
  ret = strtod(cstr, &endPtr);
  while( isspace(*endPtr)) endPtr++;
  if (*endPtr == '\0') return ret;
  else return NAN;
}

inline JSValue objectToPrimitiveHintNumber(JSValue obj, Context* context)
{
  JSValue prim;
//  if(!invokeValueOf(obj, context, &prim) || isObject(prim)){
//    if(!invokeToString(obj, context, &prim) || isObject(prim)){
//      LOG_EXIT("can't convert primitive\n"); } }
  prim = JS_UNDEFINED;
  return prim;
}

inline JSValue objectToPrimitiveHintString(JSValue obj, Context* context)
{
  JSValue prim;
//  if(!invokeToString(obj, context, &prim) || isObject(prim)){
//    if(!invokeValueOf(obj, context, &prim) || isObject(prim)){
//      LOG_EXIT("can't convert primitive\n"); } }
  prim = JS_UNDEFINED;
  return prim;
}

#if 0
JSValue string_to_index(JSValue str)
{
  long index;
  if (is_string(str)) {
    char* cstr = string_to_cstr(str);
    char* endPtr;
    if(cstr[0] == '0'){
      if(cstr[1] == '\0'){
        return FIXNUM_ZERO; }
    }
    else if(isdigit(cstr[0])){
      index = strtol(cstr, &endPtr, 10);
      if(endPtr[0] == '\0' && index < ARRAY_INDEX_MAX){
        return int_to_fixnum(index); }
    }
  }
  return str;
}
#endif

/*
double cStrToDouble(char* cstr)
{
  char* endPtr;
  double ret;
  ret = strtod(cstr, &endPtr);
  while(isspace(*endPtr)){
    endPtr++; }

  // 終端文字から判別
  if(*endPtr == '\0'){
    return ret;
  }else{
    return NAN;
  }
}
*/

double special_to_double(JSValue x) {
  switch (x) {
  case JS_TRUE:
    return 1.0;
  case JS_FALSE:
  case JS_NULL:
    return 0.0;
  case JS_UNDEFINED:
  default:
    return NAN;
  }
}

JSValue call_method(JSValue receiver, JSValue method) {
  return JS_UNDEFINED;
}
