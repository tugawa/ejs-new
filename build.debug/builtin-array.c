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

#define INSERTION_SORT_THRESHOLD (20) /* must >= 1 */
void asort(Context*, JSValue, cint, cint, JSValue);
void quickSort(Context*, JSValue, cint, cint, JSValue);
void insertionSort(Context*, JSValue, cint, cint, JSValue);
void swap(JSValue*, JSValue*);

/*
 * constructor for array
 */
BUILTIN_FUNCTION(array_constr)
{
  JSValue rsv;
  cint size, length;

  builtin_prologue();

  /* compute sizes */
  if (na == 0)
    length = 0;
  else if (na == 1) {
    JSValue n = args[1];
    if (!is_fixnum(n) || (length = fixnum_to_cint(n)) < 0)
      length = 0;
  } else {
    /*
     * na >= 2, e.g., Array(2,4,5,1)
     * This means that the array's length is four whose elements are
     * 2, 4, 5, and 1.
     */
    length = na;
  }
  size = length;

  /* allocate the array */
#ifdef ALLOC_SITE_CACHE
  rsv = create_array_object(context, DEBUG_NAME("array_ctor"), size);
#else /* ALLOC_SITE_CACHE */
  rsv = new_array_object(context, DEBUG_NAME("array_ctor"),
                         gshapes.g_shape_Array, size);
#endif /* ALLOC_SITE_CACHE */
  GC_PUSH(rsv);
  set_jsarray_length(rsv, cint_to_number(context, length));
  GC_POP(rsv);

  /* fill elements if supplied */
  if (na >= 2) {
    int i;
    JSValue *body = get_jsarray_body(rsv);
    for (i = 0; i < length; i++)
      body[i] = args[i + 1];
  }
  /* set as the return value */
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
  a = new_array_object(context, DEBUG_NAME("array_concat"),
                       gshapes.g_shape_Array, 0);
  n = 0;
  GC_PUSH(a);
  for (i = 0; i <= na; i++) {
    e = args[i];
    if (is_array(e)) {
      k = 0;
      len = number_to_cint(get_jsarray_length(e));
      if (n + len > MAX_ARRAY_LENGTH)
        /* This should be improved */
        LOG_EXIT("New array length is more than VM limit (MAX_ARRAY_LENGTH)");
      while (k < len) {
        if (has_array_element(e, k)) {
          GC_PUSH(e);
          subElement = get_array_element(context, a, k);
          set_array_prop(context, a, cint_to_number(context, n), subElement);
          GC_POP(e);
        }
        n++;
        k++;
      }
    } else {
      if (n > MAX_ARRAY_LENGTH)
        /* This should be improved */
        LOG_EXIT("New array length is more than VM limit (MAX_ARRAY_LENGTH)");
      set_array_prop(context, a, cint_to_number(context, n), e);
      n++;
    }
  }
  /* is the two lines below necessary? */
  set_jsarray_length(a, n);
  set_prop_direct(context, a, gconsts.g_string_length,
                  cint_to_number(context, n), ATTR_NONE);
  GC_POP(a);
  set_a(context, a);
  return;
}

BUILTIN_FUNCTION(array_pop)
{
  JSValue a, ret, flen;
  cint len;

  builtin_prologue();
  a = args[0];
  len = number_to_cint(get_jsarray_length(a)) - 1;    /* len >= -1 */
  if (len < 0) {
    set_a(context, JS_UNDEFINED);
    return;
  }

  flen = cint_to_number(context, len);
  if (len < get_jsarray_size(a)) {
    JSValue *body = get_jsarray_body(a);
    ret = body[len];
  } else
    ret = get_prop_prototype_chain(a, fixnum_to_string(flen));
  delete_array_element(a, len);
  set_jsarray_length(a, len);
  GC_PUSH(ret);
  set_prop_direct(context, a, gconsts.g_string_length, flen, ATTR_NONE);
  GC_POP(ret);
  set_a(context, ret);
  return;
}

BUILTIN_FUNCTION(array_push)
{
  JSValue a, ret;
  cint len;
  cint i;

  builtin_prologue();
  a = args[0];
  len = number_to_cint(get_jsarray_length(a));
  /*
   * The following for-loop is very inefficient.
   * This is for simplicity of implementation.
   */
  GC_PUSH(a);
  for (i = 1; i <= na; i++)
    set_array_prop(context, a, cint_to_number(context, len++), args[i]);
  GC_POP(a);
  ret = (len <= MAX_ARRAY_LENGTH)?
    cint_to_number(context, len): cint_to_number(context, MAX_ARRAY_LENGTH);
  set_a(context, ret);
  return;
}

BUILTIN_FUNCTION(array_reverse)
{
  cint len, mid, lower, upper;
  int lowerExists, upperExists;
  JSValue lowerValue, upperValue;

  builtin_prologue();
  len = number_to_cint(get_jsarray_length(args[0]));
  mid = len / 2;

  lowerValue = JS_NULL;
  GC_PUSH(lowerValue);
  for (lower = 0; lower < mid; lower++) {
    upper = len - lower - 1;
    lowerExists = has_array_element(args[0], lower);
    upperExists = has_array_element(args[0], upper);

    if (lowerExists)
      lowerValue = get_array_element(context, args[0], lower);
    if (upperExists)
      upperValue = get_array_element(context, args[0], upper);

    if (lowerExists && upperExists) {
      set_array_prop(context, args[0], cint_to_number(context, lower),
                     upperValue);
      set_array_prop(context, args[0], cint_to_number(context, upper),
                     lowerValue);
    } else if (!lowerExists && upperExists) {
      set_array_prop(context, args[0], cint_to_number(context, lower),
                     upperValue);
      delete_array_element(args[0], upper);
    } else if (lowerExists && !upperExists) {
      set_array_prop(context, args[0], cint_to_number(context, upper),
                     lowerValue);
      delete_array_element(args[0], lower);
    } else {
      /* No action is required */
    }
  }
  GC_POP(lowerValue);
  set_a(context, args[0]);
  return;
}

BUILTIN_FUNCTION(array_shift)
{
  JSValue first, fromVal;
  cint len, from, to;

  builtin_prologue();
  len = number_to_cint(get_jsarray_length(args[0]));
  if (len <= 0) {
    set_a(context, JS_UNDEFINED);
    return;
  }

  first = get_array_element(context, args[0], 0);
  assert(first != JS_EMPTY);
  GC_PUSH(first);
  for (from = 1; from < len; from++) {
    to = from - 1;
    if (has_array_element(args[0], from)) {
      fromVal = get_array_element(context, args[0], from);
      set_array_prop(context, args[0], cint_to_number(context, to), fromVal);
    } else {
      delete_array_element(args[0], to);
    }
  }
  delete_array_element(args[0], len - 1);
  /* should reallocate (shorten) body array here? */
  set_jsarray_length(args[0], --len);
  set_prop_direct(context, args[0], gconsts.g_string_length,
                  cint_to_number(context, len), ATTR_NONE);
  GC_POP(first);
  set_a(context, first);
  return;
}

BUILTIN_FUNCTION(array_slice)
{
  JSValue o, a;
  cint len, relativeStart, relativeEnd, k, n, final, count;
  JSValue start, end, kValue;

  builtin_prologue();
  o = args[0];
  start = (na >= 1)? args[1]: 0;
  end = (na >= 2)? args[2]: JS_UNDEFINED;

  len = number_to_cint(get_jsarray_length(args[0]));
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
  a = new_array_object(context, DEBUG_NAME("array_slice"),
                       gshapes.g_shape_Array, count);
  GC_PUSH(a);
  n = 0;
  while (k < final) {
    if (has_array_element(o,k)) {
      kValue = get_array_element(context, o, k);
      set_array_prop(context, a, cint_to_number(context, n), kValue);
    }
    k++;
    n++;
  }
  GC_POP2(a, o);
  set_a(context, a);
  return;
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
             (is_builtin(comparefn) && get_jsbuiltin_nargs(comparefn) >= 2)) {
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
    tmp = get_array_element(context, array, i); /* tmp = a[i] */
    GC_PUSH(tmp);
    for (j = i - 1; l <= j; j--) {
      aj = get_array_element(context, array, j);
      GC_PUSH(aj);
      if (sortCompare(context, aj, tmp, comparefn) > 0)
        set_array_prop(context, array, cint_to_number(context, j + 1), aj);
      /* a[j+1] = a[j] */
      else {
        GC_POP(aj);
        break;
      }
      GC_POP(aj);
    }
    GC_POP(tmp);
    set_array_prop(context, array, cint_to_number(context, j + 1), tmp);
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
  v0 = get_array_element(context, array, l);
  v1 = get_array_element(context, array, m);
  v2 = get_array_element(context, array, r);
  GC_PUSH3(v0, v1, v2);
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
  /* a[l] = v0 */
  set_array_prop(context, array, cint_to_number(context, l), v0);
 /* a[r] = v2 */
  set_array_prop(context, array, cint_to_number(context, r), v2);
  /* a[m] = a[l+1] */
  set_array_prop(context, array, cint_to_number(context, m),
                 get_array_element(context, array, l+1));
 /* a[l+1] = v1(=p) */
  set_array_prop(context, array, cint_to_number(context, l+1), v1);
  i = l+2;
  j = r-1;
  /* Sorting (from i to j) */
  while (1) {
    while (i < r && sortCompare(context, p,
                                get_array_element(context, array, i),
                                comparefn) > 0)
      i++;
    while (l < j && sortCompare(context, p,
                                get_array_element(context, array, j),
                                comparefn) < 0)
      j--;
    if (i >= j)
      break;
    /* Exchange a[i] and a[j] */
    tmp = get_array_element(context, array, i);
    assert(tmp != JS_EMPTY);
    GC_PUSH(tmp);
    set_array_prop(context, array, cint_to_number(context, i),
                   get_array_element(context, array, j));
    GC_POP(tmp);
    set_array_prop(context, array, cint_to_number(context, j), tmp);
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
  len = number_to_cint(get_jsarray_length(obj));
  GC_PUSH(obj);
  asort(context, obj, 0, len - 1, comparefn);
  GC_POP(obj);
  set_a(context, obj);
  return;
}

BUILTIN_FUNCTION(array_debugarray)
{
  /* BUG?: The method does not print a[i] (i >= max(size, ASIZE_LIMIT)) */
  JSValue a;
  cint size, length, to;
  int i;

  builtin_prologue();
  a = args[0];
  size = get_jsarray_size(a);
  length = number_to_cint(get_jsarray_length(a));
  to = length < size? length: size;
  printf("debugarray: size = %lld length = %lld, to = %lld\n",
         (long long) size, (long long) length, (long long) to);
  GC_PUSH(a);
  for (i = 0; i < to; i++) {
    JSValue *body = get_jsarray_body(a);
    printf("i = %d: ", i);
    print_value_simple(context, body[i]);
    printf("\n");
  }
  GC_POP(a);
  set_a(context, JS_UNDEFINED);
  return;
}

/*
 * property table
 */

/* prototype */
ObjBuiltinProp ArrayPrototype_builtin_props[] = {
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
};
ObjDoubleProp  ArrayPrototype_double_props[] = {
  { "length",   0, ATTR_DDDE, 2},
};
ObjGconstsProp ArrayPrototype_gconsts_props[] = {};
/* constructor */
ObjBuiltinProp ArrayConstructor_builtin_props[] = {};
ObjDoubleProp  ArrayConstructor_double_props[] = {};
ObjGconstsProp ArrayConstructor_gconsts_props[] = {
  { "prototype", &gconsts.g_prototype_Array,  ATTR_ALL },
};
/* instance */
ObjBuiltinProp Array_builtin_props[] = {};
ObjDoubleProp  Array_double_props[] = {
  { "length",    0, ATTR_DDDE, 2 },  /* placeholder */
};
ObjGconstsProp Array_gconsts_props[] = {};
DEFINE_PROPERTY_TABLE_SIZES_PCI(Array);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
