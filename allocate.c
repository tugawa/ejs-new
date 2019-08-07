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

/*
 * a counter for debugging
 */
#ifdef DEBUG
int count = 0;
#endif /* DEBUG */

/*
 * allocates a flonum
 * Note that the return value does not have a pointer tag.
 */
FlonumCell *allocate_flonum(double d)
{
  FlonumCell *n =
    (FlonumCell *) gc_jsalloc_critical(sizeof(FlonumCell), HTAG_FLONUM);
  n->value = d;
  return n;
}

/*
 * allocates a string
 * It takes only the string length (excluding the last null character).
 * Note that the return value does not have a pointer tag.
 */
StringCell *allocate_string(uint32_t length)
{
  /* plus 1 for the null terminater,
   * minus BYTES_IN_JSVALUE becasue StringCell has payload of
   * BYTES_IN_JSVALUE for placeholder */
  uintptr_t size = sizeof(StringCell) + (length + 1 - BYTES_IN_JSVALUE);
  StringCell *v =
    (StringCell *) gc_jsalloc_critical(size, HTAG_STRING);
#ifdef STROBJ_HAS_HASH
  v->length = length;
#endif
  return v;
}

/*
 * allocates a simple object
 * Note that the return value does not have a pointer tag.
 */
Object *allocate_jsobject(Context *ctx, size_t n_embedded, cell_type_t htag)
{
  size_t size = sizeof(Object) + sizeof(JSValue) * (n_embedded - PSIZE_NORMAL);
  Object *object = (Object *) gc_jsalloc(ctx, size, htag);
  return object;
}

/*
 * allocates an initialised array of JSValue
 */
JSValue *allocate_jsvalue_array(Context *ctx, int size)
{
  int i;
  JSValue* array = (JSValue *) gc_malloc(ctx, sizeof(JSValue) * size,
                                         HTAG_ARRAY_DATA);
  for (i = 0; i < size; i++)
    array[i] = JS_UNDEFINED;
  return array;
}

/*
 * re-allocates an array body
 *   newsize : new size of the array body
 */
void reallocate_array_data(Context *ctx, JSValue a, int newsize)
{
  JSValue *body, *oldbody;

  int i;

  GC_PUSH(a);
  body = (JSValue *) gc_malloc(ctx, sizeof(JSValue) * newsize,
                               HTAG_ARRAY_DATA);
  GC_POP(a);
  oldbody = array_body(a);
  for (i = 0; i < array_length(a); i++) body[i] = oldbody[i];
  array_body(a) = body;
  array_size(a) = newsize;
}

JSValue *allocate_prop_table(int size) {
  JSValue *table = (JSValue*) gc_malloc_critical(sizeof(JSValue) * size,
                                                 HTAG_PROP);
  size_t i;
  for (i = 0; i < size; i++)  /* initialise for GC */
    table[i] = JS_UNDEFINED;
  return table;
}

JSValue *reallocate_prop_table(Context *ctx, JSValue *oldtab, int oldsize, int newsize) {
  JSValue *table;

  gc_push_tmp_root(oldtab);
  table  = (JSValue *) gc_malloc(ctx, sizeof(JSValue) * newsize, HTAG_PROP);
  gc_pop_tmp_root(1);
  size_t i;
  for (i = 0; i < oldsize; i++)
    table[i] = oldtab[i];
  for (; i < newsize; i++)
    table[i] = JS_UNDEFINED;
  return table;
}

/*
 * allocates an iterator
 */
Iterator *allocate_iterator(void) {
  Iterator *iter =
    (Iterator *) gc_jsalloc_critical(sizeof(Iterator), HTAG_ITERATOR);
  iterator_body(iter) = NULL;
  iterator_size(iter) = 0;
  iterator_index(iter) = 0;
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
                               HTAG_ARRAY_DATA);
  GC_POP(a);
  for (i = 0; i < size; i++) body[i] = JS_UNDEFINED; 
  iterator_body(a) = body;
  iterator_size(a) = size;
  iterator_index(a) = 0;
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
