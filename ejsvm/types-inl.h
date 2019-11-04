#ifndef TYPES_INL_H
#define TYPES_INL_H

/*
 * Tag operation
 */

static inline PTag get_ptag(JSValue v)
{
  return (PTag) {((uintjsv_t) v) & TAGMASK};
}

static inline int is_ptag(JSValue v, PTag t)
{
  return (((uintjsv_t) v) & TAGMASK) == t.v;
}

static inline JSValue put_ptag(uintjsv_t x, PTag t)
{
  assert((x & TAGMASK) == 0);
  /* REMARK: When you modify this function, you need to modify
   *         PUT_PTAG_CONSTNAT in types.h accordingly.
   *         As a remainder, we use PUT_PTAG_CONSTANT although
   *         `x' is not a constant here. */
  return PUT_PTAG_CONSTANT(x, t.v);
}

static inline uintjsv_t clear_ptag(JSValue v)
{
  return ((uintjsv_t) v) & ~TAGMASK;
}

static inline HTag get_htag(JSValue v)
{
  void *p;
  assert(has_htag(v));
  p = (void *) (uintptr_t) (uintjsv_t) v;
  return (HTag) {gc_obj_header_type(p)};
}

static inline int is_htag(JSValue v, HTag t)
{
  return get_htag(v).v == t.v;
}

/*
 * Type conversion from/to JSValue
 */

#define IS_POINTER_LIKE_UINTJSV(x)		\
  (((x) & ~((uintjsv_t) (uintptr_t) -1)) == 0)

/*
 * jsv_to_jsobject converts JSValue to JSObject pointer.
 *  - Check PTag and HTag (using is_jsobject)
 *  - Clear PTag
 *  - Check if higher bits are zero if sizeof(void*) < sizeof(JSValue)
 */
static inline JSObject *jsv_to_jsobject(JSValue v)
{
  assert(IS_POINTER_LIKE_UINTJSV((uintjsv_t) v));
  assert(is_jsobject(v));
  return (JSObject *) (uintptr_t) clear_ptag(v);
}

/*
 * jsv_to_RT (RT: VMRepType including those that are implemeted with JSObject)
 *  - Check PTag and HTag (using is_xxx)
 *  - Clear PTag
 *  - Check if higher bits are zero if sizeof(void*) < sizeof(JSValue)
 */
#define VMRepType(RT, ptag, S)				\
static inline S *jsv_to_##RT(JSValue v)			\
{							\
  assert(IS_POINTER_LIKE_UINTJSV((uintjsv_t) v));	\
  assert(is_##RT(v));					\
  return (S *) (uintptr_t) clear_ptag(v);		\
}
VMRepType_LIST
#undef VMRepType

/*
 * jsv_to_T (T: type name)
 *  - Check if PTag field is zeor, and has a proper HTag
 *  - Check if higher bits are zero if sizeof(void*) < sizeof(JSValue)
 */
#define VMHeapData(name, CELLT, T)			\
static inline T *jsv_to_##name(JSValue v)		\
{							\
  T *p;							\
  assert(IS_POINTER_LIKE_UINTJSV((uintjsv_t) v));	\
  assert(get_ptag(v).v == 0);				\
  p = (T *) (uintptr_t) (uintjsv_t) v;			\
  assert(p == NULL || gc_obj_header_type(p) == CELLT);	\
  return p;						\
}
VMHeapData_LIST
#undef VMHeapData

/*
 * jsv_to_noheap_ptr
 *  - Check if higher bits are zore if sizeof(void*) < sizeof(JSValue)
 */
static inline void *jsv_to_noheap_ptr(JSValue v)
{
  assert(IS_POINTER_LIKE_UINTJSV((uintjsv_t) v));
  return (void *) (uintptr_t) (uintjsv_t) v;
}

/*
 * ptr_to_RT (RT: VMRepType)
 *  - Check HTag
 *  - Put PTag
 */
#define VMRepType(RT, ptag, S)					\
static inline JSValue ptr_to_##RT(S *p)				\
{								\
  JSValue v = put_ptag((uintptr_t) p, ptag);			\
  assert(is_##RT(v));  /* check HTag */				\
  return v;							\
}
VMRepType_LIST
#undef VMRepType_LIST


/*
 * Number
 */

static inline JSValue small_cint_to_fixnum(cint n)
{
  assert(is_fixnum_range_cint(n));
  return put_ptag(((uintjsv_t) n) << TAGOFFSET, T_FIXNUM);
}

static inline JSValue cint_to_number(Context *ctx, cint n)
{
  if (is_fixnum_range_cint(n))
    return small_cint_to_fixnum(n);
  else
    return cint_to_flonum(ctx, n);
}

static inline double number_to_double(JSValue v)
{
  if (is_fixnum(v))
    return fixnum_to_double(v);
  else
    return flonum_to_double(v);
}

static inline JSValue double_to_number(Context *ctx, double n)
{
  if (isnan(n))
    return gconsts.g_flonum_nan;
  else if (is_fixnum_range_double(n))
    return small_cint_to_fixnum((cint) n);
  else
    return double_to_flonum(ctx, n);
}

#endif /* TYPES_INL_H */
