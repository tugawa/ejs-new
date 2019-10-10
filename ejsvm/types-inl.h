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

static inline uintjsv_t clear_ptag(JSValue v)
{
  return ((uintjsv_t) v) & ~TAGMASK;
}

static inline JSValue put_ptag(uintjsv_t x, PTag t)
{
  /* REMARK: When you modify this function, you need to modify
   *         PUT_PTAG_CONSTNAT in types.h accordingly. */
  assert((x & TAGMASK) == 0);
  return (JSValue) (x | t.v);
}

static inline HTag get_htag(JSValue v)
{
  assert(is_object(v));
  return (HTag) {gc_obj_header_type((void *)jsv_to_uintptr(v))};
}

static inline int is_htag(JSValue v, HTag t)
{
  return get_htag(v).v == t.v;
}

/* Type convertion from/to JSValue
 *   JSValue -> JSObject             jsv_to_jsobject
 *   JSValue -> JavaScript object    (T) jsv_to_uintptr
 *   JSValue -> no-JS heap object    (T) jsv_to_uintptr
 *   JSValue -> no-heap object       (T) jsv_to_uintptr
 *   JSValue -> uintjsv_t            explicit cast
 *   JSValue -> intjsv_t             explicit cast
 *   JSValue -> other primitive      explicit cast through uintjsv_t/intjsv_t
 *   JSObject -> JSValue             ptr_to_T
 *   JavaScript object -> JSValue    ptr_to_T
 *   no-JS heap object -> JSValue    T_to_jsv
 *   no-heap object -> JSValue       noheap_ptr_to_jsv
 *   uintjsv_t -> JSValue            explicit cast
 *   intjsv_t -> JSValue             explicit cast
 */

static inline uintptr_t jsv_to_uintptr(JSValue v)
{
  const uintjsv_t JSV_POINTER_BITS_MASK = ((uintjsv_t) (uintptr_t) -1);
  uintjsv_t x = clear_ptag(v);
  assert((x & ~JSV_POINTER_BITS_MASK) == 0);
  return (uintptr_t) x;
}

/*
 * noheap_ptr_to_jsv converts a pointer to outside heap to JSValue.
 */
static inline JSValue noheap_ptr_to_jsv(void *p)
{
  return (JSValue) (uintptr_t) p;
}

#define DEFINE_CONVERSION(name, T, CELLT)               \
static inline JSValue name##_to_jsv(T p) {              \
  assert(gc_obj_header_type(p) == CELLT);               \
  return (JSValue) (uintptr_t) p;                       \
}

struct function_frame;
struct property_map;
DEFINE_CONVERSION(function_frame, struct function_frame*, CELLT_FUNCTION_FRAME)
DEFINE_CONVERSION(extension_prop, JSValue*,               CELLT_PROP)
DEFINE_CONVERSION(property_map,   struct property_map*,   CELLT_PROPERTY_MAP)

#undef DEFINE_CONVERSION



#endif /* TYPES_INL_H */
