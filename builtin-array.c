/*
   builtin-array.c

   SSJS Project at the University of Electro-communications

   Sho Takada, 2012-13
   Akira Tanimura, 2012-13
   Akihiro Urushihara, 2013-14
   Ryota Fujii, 2013-14
   Tomoharu Ugawa, 2013-16
   Hideya Iwasaki, 2013-16
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
  rsv = new_array(context);  // note: new_array sets the `length' property to 0
  gc_push_tmp_root(&rsv);
  if (na == 0) {
    allocate_array_data(context, rsv, ASIZE_INIT, 0);
    set_prop_none(rsv, gconsts.g_string_length, FIXNUM_ZERO);
  } else if (na == 1) {
    JSValue n = args[1];  // GC: n is used in uninterraptible section
    if (is_fixnum(n) && 0 <= (length = fixnum_to_cint(n))) {
      size = compute_asize(length);
      allocate_array_data(context, rsv, size, length);
      // printf("array_constr: length = %ld, size = %ld, rsv = %lx\n", length, size, rsv);
      set_prop_none(rsv, gconsts.g_string_length, cint_to_fixnum(length));
    } else {
      allocate_array_data(context, rsv, ASIZE_INIT, 0);
      set_prop_none(rsv, gconsts.g_string_length, FIXNUM_ZERO);
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
    set_prop_none(rsv, gconsts.g_string_length, cint_to_fixnum(length));
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
  /* toLocaleString() calls other toLocaleString()s of the elements of the array */
  /*
  JSValue array, elem, separator, r, elementObj, func;
  cint len;

  builtin_prologue();
  array = args[0];
  len = array_length(args[0]);

  if (len == 0) {
    set_a(context, gconsts.g_string_empty);
    return;
  }
  separator = gconsts.g_string_comma;
  elem = get_array_prop(context, array, cint_to_fixnum(0));
  if (is_null(elem) || is_undefined(elem)) r = gconsts.g_string_empty;
  else {
    elementObj = to_object(context, elem);
    func = get_prop_prototype_chain(elementObj, cstr_to_string("toLocaleString"));
    if (!is_function(func)) type_error_exception("not callable"); // must use is_callable()?
  }
  set_a(context, array);
  return;
  */
  not_implemented("toLocaleString");
#if 0
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
#endif
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
  JSValue a, e, subElement;
  cint n, k, i, len;

  builtin_prologue();
  a = new_array(context);
  n = 0;
  for (i = 0; i <= na; i++) {
    e = args[i];
    if (is_array(e)) {
      k = 0;
      len = array_length(e);
      if (n + len > MAX_ARRAY_LENGTH) LOG_EXIT("New array length is more than VM limit (MAX_ARRAY_LENGTH)"); // This should be improved
      while (k < len) {
        if (has_array_element(e, k)) {
          subElement = get_array_prop(context, e, cint_to_fixnum(k));
          set_array_prop(context, a, cint_to_fixnum(n), subElement);
        }
        n++;
        k++;
      }
    } else {
      if (n > MAX_ARRAY_LENGTH) LOG_EXIT("New array length is more than VM limit (MAX_ARRAY_LENGTH)"); // This should be improved
      set_array_prop(context, a, cint_to_fixnum(n), e);
      n++;
    }
  }
  // is the two lines below necessary?
  array_length(a) = n;
  set_prop_none(a, gconsts.g_string_length, cint_to_fixnum(n));
  set_a(context, a);
  return;

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
  len = array_length(a) - 1;    // len >= -1
  if (len < 0) {
    set_a(context, JS_UNDEFINED);
    return;
  }

  flen = cint_to_fixnum(len);
  if (len < array_size(a))
    ret = array_body_index(a, len);
  else
    ret = get_prop_prototype_chain(a, fixnum_to_string(flen));
  delete_array_element(a, len);
  array_length(a) = len;
  set_prop_none(a, gconsts.g_string_length, flen);
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
    set_array_prop(context, a, cint_to_fixnum(len++), args[i]);
  ret = (len <= MAX_ARRAY_LENGTH)?
          cint_to_fixnum(len): cint_to_fixnum(MAX_ARRAY_LENGTH);
  set_a(context, ret);
  return;
}

BUILTIN_FUNCTION(array_reverse)
{
  cint len, mid, lower, upper;
  int lowerExists, upperExists;
  JSValue lowerValue, upperValue;

  builtin_prologue();
  len = array_length(args[0]);
  mid = len / 2;
  for (lower = 0; lower < mid; lower++) {
    upper = len - lower - 1;
    lowerExists = has_array_element(args[0], lower);
    if (lowerExists)
      lowerValue = get_array_prop(context, args[0], cint_to_fixnum(lower));
    upperExists = has_array_element(args[0], upper);
    if (upperExists)
      upperValue = get_array_prop(context, args[0], cint_to_fixnum(upper));

    if (lowerExists && upperExists) {
      set_array_prop(context, args[0], cint_to_fixnum(lower), upperValue);
      set_array_prop(context, args[0], cint_to_fixnum(upper), lowerValue);
    } else if (!lowerExists && upperExists) {
      set_array_prop(context, args[0], cint_to_fixnum(lower), upperValue);
      delete_array_element(args[0], upper);
    } else if (lowerExists && !upperExists) {
      set_array_prop(context, args[0], cint_to_fixnum(upper), lowerValue);
      delete_array_element(args[0], lower);
    } else {
      /* No action is required */
    }
  }
  set_a(context, args[0]);
  return;

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
  JSValue first, fromVal;
  cint len, from, to;

  builtin_prologue();
  len = array_length(args[0]);
  if (len <= 0) {
    set_a(context, JS_UNDEFINED);
    return;
  }

  first = get_array_prop(context, args[0], cint_to_fixnum(0));
  for (from = 1; from < len; from++) {
    to = from - 1;
    if (has_array_element(args[0], from)) {
      fromVal = get_array_prop(context, args[0], cint_to_fixnum(from));
      set_array_prop(context, args[0], cint_to_fixnum(to), fromVal);
    } else {
      delete_array_element(args[0], to);
    }
  }
  delete_array_element(args[0], len - 1);
  /* should reallocate (shorten) body array here? */
  array_length(args[0]) = --len;
  set_prop_none(args[0], gconsts.g_string_length, cint_to_fixnum(len));
  set_a(context, first);
  return;

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
  JSValue o, a;
  cint len, relativeStart, relativeEnd, k, n, final, count;
  JSValue start, end, kValue;

  builtin_prologue();
  o = args[0];
  start = (na >= 1)? args[1]: 0;
  end = (na >= 2)? args[2]: JS_UNDEFINED;

  len = array_length(args[0]);
  relativeStart = toInteger(context, start);

  if (relativeStart < 0) k = max((len + relativeStart), 0);
  else k = min(relativeStart, len);

  if (is_undefined(end)) relativeEnd = len;
  else relativeEnd = toInteger(context, end);

  if (relativeEnd < 0) final = max((len + relativeEnd), 0);
  else final = min(relativeEnd, len);

  count = max(final - k, 0);
  a = new_array_with_size(context, count);
  set_prop_all(a, gconsts.g_string___proto__, gconsts.g_array_proto);

  n = 0;
  while (k < final) {
    if (has_array_element(o,k)) {
      kValue = get_array_prop(context, o, cint_to_fixnum(k));
      set_array_prop(context, a, cint_to_fixnum(n), kValue);
    }
    k++;
    n++;
  }
  set_a(context, a);
  return;

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

/*
   softCompare(context, x, y, comparefn) returns
    x < y: minus
    x = y: 0
    x > y: plus
 */
int sortCompare(Context *context, JSValue x, JSValue y, JSValue comparefn) {
  char *xString, *yString;
  if (is_undefined(x) && is_undefined(y)) return 0;
  else if (is_undefined(x)) return 1;
  else if (is_undefined(y)) return -1;
  else if (!is_undefined(comparefn)) {
    /* TODO
    if (!is_callable(comparefn)) LOG_EXIT("TypeError");
    return call(comparefn(x,y));
    */
    LOG_EXIT("not implemented");
  }
  xString = string_to_cstr(to_string(context, x));
  yString = string_to_cstr(to_string(context, y));
  return strcmp(xString, yString);
}

/* should be refine */
void asort(Context* context, JSValue array, cint l, cint r, JSValue comparefn) {
  JSValue tmp;
  JSValue p;
  cint i, j;
  /*
  for(cint z = 0; z < array_length(array); z++) {
    tmp = get_array_prop(context, array, cint_to_fixnum(z));
    if (l <= z && z <= r) print_value_simple(context, tmp);
    else printf("_");
    if (z < array_length(array)-1) printf(",");
    else printf("\n");
  }
  */

  if (l < r) {
    i = l, j = r;
    p = get_array_prop(context, array, cint_to_fixnum(i)); // kari

    while (1) {
      while (sortCompare(context, p, get_array_prop(context, array, cint_to_fixnum(i)), comparefn) > 0) i++;
      while (sortCompare(context, p, get_array_prop(context, array, cint_to_fixnum(j)), comparefn) < 0) j--;
      if (i >= j) break;
      // exchange a[i] and a[j]
      tmp = get_array_prop(context, array, cint_to_fixnum(i));
      set_array_prop(context, array, cint_to_fixnum(i), get_array_prop(context, array, cint_to_fixnum(j)));
      set_array_prop(context, array, cint_to_fixnum(j), tmp);
      i++;
      j--;
    }
    asort(context, array, l, i - 1, comparefn);
    asort(context, array, j + 1, r, comparefn);
  }
}

BUILTIN_FUNCTION(array_sort)
{
  JSValue obj, comparefn;
  cint len;

  builtin_prologue();
  obj = args[0];
  comparefn = args[1];
  len = array_length(obj);
  /*
     TODO:
       The behavior is implementation defined for each condition 1-4.
   */
  /* 1
      * comparefn is not undefined
      * comparefn is not a consistent comparison function
        for the elements of array
   */
  /* 2
      Let proto be tha value of the prototype of obj.
        * proto is not null
        * there exists an integer j such that all of the conditions below:
          * obj is sparse
          * 0 <= j < len
          * proto has the property ToString(j)
   */
  /* 3
      obj is sparse
      any of the following conditions are true:
        * [[Extensible]] internal property of obj is false
        * any array index property of obj whose name is nonnegative integer
          less than len is a data property whose [[Configurable]] attribute
          is false
   */
  /* 4
      if any array index property of obj whose name is
      a nonnegative integer less than
      len is an accessor property
      or is a data property whose [[Writable]] attribute is false
   */

  /*
  int is_sparse(JSValue a) {
    JSValue e;
    cint i;

    if (!is_array(a)) return FALSE;
    for (i = 0; i < array_length(a); i++) {
      e = get_array_prop(context, a, cint_to_fixnum(i));
      if (is_undefined(e)) return TRUE;
    }
    return FALSE;
  }
  */
  asort(context, obj, 0, len-1, comparefn);
  set_a(context, obj);
  return;

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
  /* BUG?: The method does not print a[i] (i >= max(size, ASIZE_LIMIT)) */
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
  { "toLocaleString", array_toLocaleString, 0, ATTR_DE },
  { "join",           array_join,           1, ATTR_DE },
  { "concat",         array_concat,         0, ATTR_DE },
  { "pop",            array_pop,            0, ATTR_DE },
  { "push",           array_push,           1, ATTR_DE },
  { "reverse",        array_reverse,        0, ATTR_DE },
  { "shift",          array_shift,          1, ATTR_DE },
  { "slice",          array_slice,          2, ATTR_DE },
  { "sort",           array_sort,           1, ATTR_DE },
  { "debugarray",     array_debugarray,     0, ATTR_DE },
  { NULL,             NULL,                 0, ATTR_DE }
};

void init_builtin_array(void)
{
  JSValue proto;

  gconsts.g_array = new_builtin_with_constr(array_constr, array_constr, 0);
  gconsts.g_array_proto = proto = new_object(NULL);
  set_prop_all(gconsts.g_array, gconsts.g_string_prototype, proto);
  {
    ObjBuiltinProp *p = array_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}
