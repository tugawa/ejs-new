/*
   allocate.c

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
#include "gc.h"

/*
   a counter for debugging
 */
#ifdef DEBUG
int count = 0;
#endif // DEBUG

/*
   allocates a flonum
   Note that the return value does not have a pointer tag.
 */
FlonumCell *allocate_flonum(double d)
{
  FlonumCell *n =
    (FlonumCell *) gc_jsalloc_critical(sizeof(FlonumCell), HTAG_FLONUM);
  n->value = d;
  return n;
}

/*
   allocates a string
   It takes only the string length (excluding the last null character).
   Note that the return value does not have a pointer tag.
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
   allocates a string
   This takes a pointer to a C string.
 */
JSValue allocate_string1(const char* str)
{
  uint32_t len;

  len = strlen(str);
  return str_intern(str, len, calc_hash_len(str, len), INTERN_HARD);
}

/*
   allocates a string
   This takes two C strings and concatenates them.
 */
JSValue allocate_string2(const char *str1, const char *str2)
{
  uint32_t len1, len2;

  len1 = strlen(str1);
  len2 = strlen(str2);
  return str_intern2_critical(str1, len1, str2, len2,
			      calc_hash_len2(str1, len1, str2, len2),
			      INTERN_HARD);
}

/*
   allocates an object
   Note that the return value does not have a pointer tag.
 */
Object *allocate_object(Context *ctx)
{
  Object *object = (Object *) gc_jsalloc(ctx, sizeof(Object), HTAG_OBJECT);
  return object;
}

/*
   allocates an array
   Note that the return value does not have a pointer tag.
 */
ArrayCell *allocate_array(Context *ctx) {
  ArrayCell *array =
    (ArrayCell *) gc_jsalloc(ctx, sizeof(ArrayCell), HTAG_ARRAY);
  return array;
}

/*
   allocates an array body
     size : number of elements in the body (size >= len)
     len  : length of the array, i.e., subscripts that are less than len
            are acrutally used
 */
void allocate_array_data(Context *ctx, JSValue a, int size, int len)
{
  JSValue *body;
  int i;

  gc_push_tmp_root(&a);
  body = (JSValue *) gc_malloc(ctx, sizeof(JSValue) * size,
			       HTAG_ARRAY_DATA);
  for (i = 0; i < len; i++) body[i] = JS_UNDEFINED; 
  array_body(a) = body;
  array_size(a) = size;
  array_length(a) = len;
  gc_pop_tmp_root(1);
}

/*
   allocates a function
 */
FunctionCell *allocate_function(void) {
  FunctionCell *function =
    (FunctionCell *) gc_jsalloc_critical(sizeof(FunctionCell), HTAG_FUNCTION);
  return function;
}

/*
   allocates a builtin
 */
BuiltinCell *allocate_builtin(void) {
  BuiltinCell *builtin =
    (BuiltinCell *) gc_jsalloc_critical(sizeof(BuiltinCell), HTAG_BUILTIN);
  return builtin;
}

JSValue *allocate_prop_table(int size) {
  JSValue *table = (JSValue*) gc_malloc_critical(sizeof(JSValue) * size,
						 HTAG_PROP);
  size_t i;
  for (i = 0; i < size; i++)  // initialise for GC
    table[i] = JS_UNDEFINED;
  return table;
}

/*
   allocates an iterator
 */
IteratorCell *allocate_iterator(void) {
  IteratorCell *iter =
    (IteratorCell *) gc_jsalloc_critical(sizeof(IteratorCell), HTAG_ITERATOR);
  return iter;
}

/*
   allocates a regexp
 */
#ifdef USE_REGEXP
RegexpCell *allocate_regexp(void)
{
  RegexpCell *regexp =
    (RegexpCell *) gc_jsalloc_critical(sizeof(RegexpCell), HTAG_REGEXP);
  return regexp;
}
#endif

/*
   allocates a boxed object
 */
BoxedCell *allocate_boxed(uint32_t type)
{
  BoxedCell *box = (BoxedCell *) gc_jsalloc_critical(sizeof(BoxedCell), type);
  return box;
}
