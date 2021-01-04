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


#ifdef FLONUM_PROF

#define LOG_DOUBLE_HASH_SIZE 11
#define DOUBLE_HASH_SIZE (1 << LOG_DOUBLE_HASH_SIZE)
#define DOUBLE_HASH_FLUSH_THREASHOLD  10

struct double_hash_record {
  double v;
  int c;
};

static struct double_hash_record double_hash[DOUBLE_HASH_SIZE];
static int double_hash_used;

void double_hash_flush()
{
  int i;
  printf("used: %d\n", double_hash_used);
  double_hash_used = 0;
  for (i = 0; i < DOUBLE_HASH_SIZE; i++) {
    if (double_hash[i].c != 0) {
      printf("%lf %d\n", double_hash[i].v, double_hash[i].c);
      double_hash[i].c = 0;
    }
  }
}

static inline int double_hash_key(double v)
{
  unsigned long long key = *(unsigned long long*)&v;
  key ^= key >> 12;
  key ^= key >> 32;
  key ^= key >> (52 - LOG_DOUBLE_HASH_SIZE);
  key &= (DOUBLE_HASH_SIZE - 1);
  return (int) key;
}

static void double_hash_put(double v)
{
  int key = double_hash_key(v);
  int i;
  for (i = 0; i < DOUBLE_HASH_FLUSH_THREASHOLD; i++) {
    if (double_hash[key].c == 0) {
      double_hash[key].v = v;
      double_hash[key].c = 1;
      double_hash_used++;
      return;
    } else if (double_hash[key].v == v) {
      double_hash[key].c++;
      return;
    }
    key = (key + DOUBLE_HASH_FLUSH_THREASHOLD + 1) & (DOUBLE_HASH_SIZE - 1);
  }
  double_hash_flush();
  double_hash_put(v);
}
#endif /* FLONUM_PROF */

/*
 * allocates a flonum
 */
FlonumCell *allocate_flonum(Context *ctx, double d)
{
  FlonumCell *p;
#ifdef FLONUM_PROF
  double_hash_put(d);
#endif /* FLONUM_PROF */
#ifdef FLONUM_SPACE
  p = gc_try_alloc_flonum(d);
  if (p != NULL)
    return p;
#endif /* FLONUM_SPACE */
  p = (FlonumCell *) gc_malloc(ctx, sizeof(FlonumCell), HTAG_FLONUM.v);
  set_normal_flonum_ptr_value(p, d);
  return p;
}

/*
 * allocates a string
 * It takes only the string length (excluding the last null character).
 */
StringCell *allocate_string(Context *ctx, uint32_t length)
{
  /* plus 1 for the null terminater,
   * minus BYTES_IN_JSVALUE becasue StringCell has payload of
   * BYTES_IN_JSVALUE for placeholder */
  uint32_t size = sizeof(StringCell) + length + 1 - BYTES_IN_JSVALUE;
  StringCell *p = (StringCell *) gc_malloc(ctx, size, HTAG_STRING.v);
#ifdef STROBJ_HAS_HASH
  set_normal_string_ptr_length(p, length);
#endif
  return p;
}

/*
 * re-allocates an array body
 *   newsize : new size of the array body
 */
void reallocate_array_data(Context *ctx, JSValue a, int newsize)
{
  JSValue *body, *oldbody;
  JSValue length_value = get_jsarray_length(a);
  int32_t size, length;
  int i;

#ifndef NEW_ASIZE_STRATEGY
  assert(newsize <= ASIZE_LIMIT);
#endif /* NEW_ASIZE_STRATEGY */

  length = (int32_t) number_to_double(length_value);
  size = get_jsarray_size(a);
  if (size < length)
    length = size;
  if (length == newsize)
    return;
  GC_PUSH(a);
  body = (JSValue *) gc_malloc(ctx, sizeof(JSValue) * newsize,
                               CELLT_ARRAY_DATA);
  GC_POP(a);
  oldbody = get_jsarray_body(a);
  for (i = 0; i < length; i++)
    body[i] = oldbody[i];
  for (; i < newsize; i++)
    body[i] = JS_UNDEFINED;
  set_jsarray_body(a, body);
  set_jsarray_size(a, newsize);
}

/*
 * allocates a ByteArray
 */
ByteArray allocate_byte_array(Context *ctx, size_t size)
{
  return (ByteArray) gc_malloc(ctx, size, CELLT_BYTE_ARRAY);
}

/*
 * allocates an iterator
 */
Iterator *allocate_iterator(Context *ctx) {
  Iterator *iter =
    (Iterator *) gc_malloc(ctx, sizeof(Iterator), HTAG_ITERATOR.v);
  set_normal_iterator_ptr_body(iter, NULL);
  set_normal_iterator_ptr_size(iter, 0);
  set_normal_iterator_ptr_index(iter, 0);
  return iter;
}

/*
 * allocates an iterator body
 *   size  : number of elements in the body (size >= len)
 *   index : reference number in the body
 */
void allocate_iterator_data(Context *ctx, JSValue a, int size)
{
  JSValue *body;
  int i;
  GC_PUSH(a);
  body = (JSValue *) gc_malloc(ctx, sizeof(JSValue) * size,
                               CELLT_ARRAY_DATA);
  GC_POP(a);
  for (i = 0; i < size; i++) body[i] = JS_UNDEFINED; 
  set_jsnormal_iterator_body(a, body);
  set_jsnormal_iterator_size(a, size);
  set_jsnormal_iterator_index(a, 0);
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
