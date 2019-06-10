/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN extern
#include "header.h"

#define not_implemented(s)                                              \
  LOG_EXIT("%s is not implemented yet\n", (s)); set_a(context, JS_UNDEFINED)

#define INSERTION_SORT_THRESHOLD (20) /* must >= 1 */
void asort(Context*, JSValue, cint, cint, JSValue);
void quickSort(Context*, JSValue, cint, cint, JSValue);
void insertionSort(Context*, JSValue, cint, cint, JSValue);
void swap(JSValue*, JSValue*);

/*
 * computes the asize for a given n
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
 * constructor for array
 */
BUILTIN_FUNCTION(array_constr)
{
  JSValue rsv;
  cint size, length;

  builtin_prologue();
  rsv = new_normal_array(context); /* this sets the `length' property to 0 */
  GC_PUSH(rsv);
  if (na == 0) {
    allocate_array_data(context, rsv, ASIZE_INIT, 0);
    set_prop_none(context, rsv, gconsts.g_string_length, FIXNUM_ZERO);
  } else if (na == 1) {
    JSValue n = args[1];  /* GC: n is used in uninterraptible section */
    if (is_fixnum(n) && 0 <= (length = fixnum_to_cint(n))) {
      size = compute_asize(length);
      allocate_array_data(context, rsv, size, length);
      /*
       * printf("array_constr: length = %ld, size = %ld,
       *        rsv = %lx\n", length, size, rsv);
       */
      set_prop_none(context, rsv, gconsts.g_string_length,
                    cint_to_fixnum(length));
    } else {
      allocate_array_data(context, rsv, ASIZE_INIT, 0);
      set_prop_none(context, rsv, gconsts.g_string_length, FIXNUM_ZERO);
    }
  } else {
    /*
     * na >= 2, e.g., Array(2,4,5,1)
     * This means that the array's length is four whose elements are
     * 2, 4, 5, and 1.
     */
    int i;
    length = na;
    size = compute_asize(length);
    allocate_array_data(context, rsv, size, length);
    set_prop_none(context, rsv, gconsts.g_string_length, cint_to_fixnum(length));
    for (i = 0; i < length; i++)
      array_body_index(rsv, i) = args[i + 1];
  }
  GC_POP(rsv);
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

BUILTIN_FUNCTION(array_toLocaleString){
  not_implemented("toLocaleString");
}

/*
 * joins the elements of an array by using a specified separator,
 * where default separator is ','
 */
BUILTIN_FUNCTION(array_join)
{
  JSValue sep, ret;

  builtin_prologue();
  if (is_undefined(args[1])) {
    sep = gconsts.g_string_comma;
  } else {
    sep = to_string(context, args[1]);
    if (!is_string(sep))
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
  a = new_normal_array(context);
  n = 0;
  GC_PUSH(a);
  for (i = 0; i <= na; i++) {
    e = args[i];
    if (is_array(e)) {
      k = 0;
      len = array_length(e);
      if (n + len > MAX_ARRAY_LENGTH)
        /* This should be improved */
        LOG_EXIT("New array length is more than VM limit (MAX_ARRAY_LENGTH)");
      while (k < len) {
        if (has_array_element(e, k)) {
          GC_PUSH(e);
          subElement = get_array_prop(context, e, cint_to_fixnum(k));
          set_array_prop(context, a, cint_to_fixnum(n), subElement);
          GC_POP(e);
        }
        n++;
        k++;
      }
    } else {
      if (n > MAX_ARRAY_LENGTH)
        /* This should be improved */
        LOG_EXIT("New array length is more than VM limit (MAX_ARRAY_LENGTH)");
      set_array_prop(context, a, cint_to_fixnum(n), e);
      n++;
    }
  }
  /* is the two lines below necessary? */
  array_length(a) = n;
  set_prop_none(context, a, gconsts.g_string_length, cint_to_fixnum(n));
  GC_POP(a);
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
  len = array_length(a) - 1;    /* len >= -1 */
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
  GC_PUSH(ret);
  set_prop_none(context, a, gconsts.g_string_length, flen);
  GC_POP(ret);
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
   * The following for-loop is very inefficient.
   * This is for simplicity of implementation.
   */
  GC_PUSH(a);
  for (i = 1; i <= na; i++)
    set_array_prop(context, a, cint_to_fixnum(len++), args[i]);
  GC_POP(a);
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
  /* All right : MissingAdd, MissingInit RemoveToAlloc */
  for (lower = 0; lower < mid; lower++) {
    upper = len - lower - 1;
    lowerExists = has_array_element(args[0], lower);

    if (lowerExists) {
      lowerValue = get_array_prop(context, args[0], cint_to_fixnum(lower));
      upperExists = has_array_element(args[0], upper);
      if (upperExists) {
        GC_PUSH(lowerValue);
        upperValue = get_array_prop(context, args[0], cint_to_fixnum(upper));
        GC_POP(lowerValue);
      }
    } else {
      upperExists = has_array_element(args[0], upper);
      if (upperExists)
        upperValue = get_array_prop(context, args[0], cint_to_fixnum(upper));
    }

    if (lowerExists && upperExists) {
      GC_PUSH(lowerValue);
      set_array_prop(context, args[0], cint_to_fixnum(lower), upperValue);
      GC_POP(lowerValue);
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
  GC_PUSH(first);
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
  set_prop_none(context, args[0], gconsts.g_string_length, cint_to_fixnum(len));
  GC_POP(first);
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


/*
 * ProtoUnShift
 * http://www.tohoho-web.com/js/array.htm#unshift
 *
 * ProtoSplice
 * http://www.tohoho-web.com/js/array.htm#splice
 */


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
  GC_PUSH2(o, end);
  relativeStart = toInteger(context, start);
  GC_POP(end);

  if (relativeStart < 0) k = max((len + relativeStart), 0);
  else k = min(relativeStart, len);

  if (is_undefined(end)) relativeEnd = len;
  else relativeEnd = toInteger(context, end);

  if (relativeEnd < 0) final = max((len + relativeEnd), 0);
  else final = min(relativeEnd, len);

  count = max(final - k, 0);
  a = new_normal_array_with_size(context, count);
  GC_PUSH(a);
  set_prop_all(context, a, gconsts.g_string___proto__, gconsts.g_array_proto);

  n = 0;
  while (k < final) {
    if (has_array_element(o,k)) {
      kValue = get_array_prop(context, o, cint_to_fixnum(k));
      set_array_prop(context, a, cint_to_fixnum(n), kValue);
    }
    k++;
    n++;
  }
  GC_POP2(a, o);
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
 * sortCompare(context, x, y, comparefn) returns
 *   x < y: minus
 *   x = y: 0
 *   x > y: plus
 */
cint sortCompare(Context *context, JSValue x, JSValue y, JSValue comparefn) {
  char *xString, *yString;
  JSValue *stack, ret;
  int oldsp, oldfp;
  
  GC_PUSH(y);
  if (is_undefined(x) && is_undefined(y)) {
    GC_POP(y);
    return 0;
  } else if (is_undefined(x)) {
    GC_POP(y);
    return 1;
  } else if (is_undefined(y)) {
    GC_POP(y);
    return -1;
  } else if (is_function(comparefn) ||
             (is_builtin(comparefn) && builtin_n_args(comparefn) >= 2)) {
    /*
     * printf(">> sortCompare(%d,%d)\n",fixnum_to_cint(x),
     *        fixnum_to_cint(y));
     */
    stack = &get_stack(context, 0);
    oldsp = get_sp(context);
    oldfp = get_fp(context);
    stack[oldsp] = y;
    stack[oldsp-1] = x;
    stack[oldsp-2] = context->global; /* is receiver always global object? */
    GC_PUSH(x);
    if (is_function(comparefn)) {
      call_function(context, comparefn, 2, TRUE);
      vmrun_threaded(context, get_fp(context));
    }
    else if (is_builtin(comparefn)) {
      save_special_registers(context, stack, oldsp - 6);
      set_fp(context, oldsp-2); /* for GC */
      /*
       * set_lp(context, NULL);
       * set_pc(context, -1);
       * set_cf(context, NULL);
       * set_ac(context, 2);
       */
      call_builtin(context, comparefn, 2, TRUE, FALSE);
    }
    restore_special_registers(context, stack, oldsp - 6);
    set_fp(context, oldfp);
    set_sp(context, oldsp);
    /* should refine lines below? */
    ret = get_a(context);
    if(is_nan(ret)) {
      GC_POP2(x, y);
      return FIXNUM_ZERO;
    }
    ret = to_number(context, ret);
    GC_POP(x);
    if(is_fixnum(ret)){
      GC_POP(y);
      return fixnum_to_cint(ret);
    } else if(is_flonum(ret)) {
      double dret = flonum_value(ret);
      if (dret > 0) {
        GC_POP(y);
        return fixnum_to_cint(1);
      } else if (dret < 0) {
        GC_POP(y);
        return fixnum_to_cint(-1);
      }
      GC_POP(y);
      return FIXNUM_ZERO;
    }
    /* LOG_EXIT("to_number(ret) is not a number"); */
  }
  {
    JSValue vx, vy;
    vx = to_string(context, x);
    GC_POP(y);
    GC_PUSH(vx);
    vy = to_string(context, y);
    GC_POP(vx);
    xString = string_to_cstr(vx);
    yString = string_to_cstr(vy);
    return strcmp(xString, yString);
  }
}

void swap(JSValue *a, JSValue *b) {
  JSValue tmp = *a;

  *a = *b;
  *b = tmp;
}

void insertionSort(Context* context, JSValue array, cint l, cint r, JSValue comparefn) {
  JSValue aj, tmp;
  cint i, j;
  GC_PUSH2(array, comparefn);
  for (i = l; i <= r; i++) {
    tmp = get_array_prop(context, array, cint_to_fixnum(i)); /* tmp = a[i] */
    GC_PUSH(tmp);
    for (j = i - 1; l <= j; j--) {
      aj = get_array_prop(context, array, cint_to_fixnum(j));
      GC_PUSH(aj);
      if (sortCompare(context, aj, tmp, comparefn) > 0)
        set_array_prop(context, array, cint_to_fixnum(j + 1), aj);
      /* a[j+1] = a[j] */
      else {
        GC_POP(aj);
        break;
      }
      GC_POP(aj);
    }
    GC_POP(tmp);
    set_array_prop(context, array, cint_to_fixnum(j + 1), tmp);
    /* a[j+1] = tmp; */
  }
  GC_POP2(comparefn, array);
}

void quickSort(Context* context, JSValue array, cint l, cint r, JSValue comparefn) {
  JSValue p, tmp;
  cint i, j;
  /* Find pivot (2nd biggest value in a[l], a[r] and a[l+((r-l)/2)]) */
  JSValue v0, v1, v2;
  cint m = l + ((r - l) / 2);
  GC_PUSH2(array, comparefn);
  v0 = get_array_prop(context, array, cint_to_fixnum(l));
  GC_PUSH(v0);
  v1 = get_array_prop(context, array, cint_to_fixnum(m));
  GC_PUSH(v1);
  v2 = get_array_prop(context, array, cint_to_fixnum(r));
  GC_PUSH(v2);
  /* Sort v0 v1 v2 */
  if (sortCompare(context, v0, v1, comparefn) > 0)  /* v0 < v1 */
    swap(&v0, &v1);
  if (sortCompare(context, v1, v2, comparefn) > 0) { /* v0 < v1 and v2 < v1 */
    swap(&v1, &v2);
    if (sortCompare(context, v0, v1, comparefn) > 0) /* v2 < v0 < v1 */
      swap(&v0, &v1);
  }
  /*
   * Update array with
   * [v0, v1(=p), a[2], a[3],..., a[m-1], a[1], a[m+1],..., a[r-1], v2]
   *   l             i                                           j   r
   */
  p = v1;
  GC_PUSH(p);
  set_array_prop(context, array, cint_to_fixnum(l), v0); /* a[l] = v0 */
  set_array_prop(context, array, cint_to_fixnum(r), v2); /* a[r] = v2 */
  set_array_prop(context, array, cint_to_fixnum(m),
                 get_array_prop(context, array, cint_to_fixnum(l+1)));
  /* a[m] = a[l+1] */
  set_array_prop(context, array, cint_to_fixnum(l+1), v1); /* a[l+1] = v1(=p) */
  i = l+2;
  j = r-1;
  /* Sorting (from i to j) */
  while (1) {
    while (i < r && sortCompare(context, p, get_array_prop(context, array, cint_to_fixnum(i)), comparefn) > 0) i++;
    while (l < j && sortCompare(context, p, get_array_prop(context, array, cint_to_fixnum(j)), comparefn) < 0) j--;
    if (i >= j) break;
    /* Exchange a[i] and a[j] */
    tmp = get_array_prop(context, array, cint_to_fixnum(i));
    GC_PUSH(tmp);
    set_array_prop(context, array, cint_to_fixnum(i),
                   get_array_prop(context, array, cint_to_fixnum(j)));
    GC_POP(tmp);
    set_array_prop(context, array, cint_to_fixnum(j), tmp);
    i++;
    j--;
  }
  GC_POP4(p, v2, v1, v0);
  asort(context, array, j + 1, r, comparefn);
  GC_POP2(comparefn, array);
  asort(context, array, l, i - 1, comparefn);
}

void asort(Context* context, JSValue array, cint l, cint r, JSValue comparefn) {
  /* DEBUG: print array
   *  for(cint z = 0; z < array_length(array); z++) {
   *    tmp = get_array_prop(context, array, cint_to_fixnum(z));
   *    if (l <= z && z <= r) print_value_simple(context, tmp);
   *    else printf("_");
   *    if (z < array_length(array)-1) printf(",");
   *    else printf("\n");
   *  }
   */
  if (l >= r) return;
  if(r - l <= INSERTION_SORT_THRESHOLD)
    insertionSort(context, array, l, r, comparefn);
  else quickSort(context, array, l, r, comparefn);
}

BUILTIN_FUNCTION(array_sort)
{
  JSValue obj, comparefn;
  cint len;

  builtin_prologue();
  obj = args[0];
  comparefn = args[1];
  len = array_length(obj);
  GC_PUSH(obj);
  asort(context, obj, 0, len - 1, comparefn);
  GC_POP(obj);
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
  printf("debugarray: size = %"PRId64", length = %"PRId64", to = %"PRId64"\n",
         size, length, to);
  GC_PUSH(a);
  for (i = 0; i < to; i++) {
    printf("i = %d: ", i);
    print_value_simple(context, array_body_index(a, i));
    printf("\n");
  }
  GC_POP(a);
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

void init_builtin_array(Context *ctx)
{
  JSValue proto;

  gconsts.g_array =
    new_normal_builtin_with_constr(ctx, array_constr, array_constr, 0);
  proto = new_big_predef_object(ctx);
  GC_PUSH(proto);
  gconsts.g_array_proto = proto;
  set_prototype_all(ctx, gconsts.g_array, proto);
  {
    ObjBuiltinProp *p = array_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name,
                        new_normal_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
  }
  GC_POP(proto);
}
