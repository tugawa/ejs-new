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
  FlonumCell *n = (FlonumCell *) gc_jsalloc(sizeof(FlonumCell), HTAG_FLONUM);
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
  StringCell *v = (StringCell *) gc_jsalloc(size, HTAG_STRING);
  v->header = make_header(size, HTAG_STRING);
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
  return str_intern2(str1, len1, str2, len2,
                     calc_hash_len2(str1, len1, str2, len2), INTERN_HARD);
}

/*
   allocates an object
   Note that the return value does not have a pointer tag.
 */
Object *allocate_object(void) {
  Object *object = (Object *) gc_jsalloc(sizeof(Object), HTAG_OBJECT);
  return object;
}

/*
   allocates an array
   Note that the return value does not have a pointer tag.
 */
ArrayCell *allocate_array(void) {
  ArrayCell *array = (ArrayCell *) gc_jsalloc(sizeof(ArrayCell), HTAG_ARRAY);
  return array;
}

/*
   allocates an array body
     size : number of elements in the body (size >= len)
     len  : length of the array, i.e., subscripts that are less than len
            are acrutally used
 */
void allocate_array_data(JSValue a, int size, int len)
{
  JSValue *body;
  int i;

  body = (JSValue *) gc_malloc(sizeof(JSValue) * size);
  for (i = 0; i < len; i++) body[i] = JS_UNDEFINED; 
  array_body(a) = body;
  array_size(a) = size;
  array_length(a) = len;
}

/*
   allocates a function
 */
FunctionCell *allocate_function(void) {
  FunctionCell *function =
    (FunctionCell *) gc_jsalloc(sizeof(FunctionCell), HTAG_FUNCTION);
  return function;
}

/*
   allocates a builtin
 */
BuiltinCell *allocate_builtin(void) {
  BuiltinCell *builtin =
    (BuiltinCell *) gc_jsalloc(sizeof(BuiltinCell), HTAG_BUILTIN);
  return builtin;
}

JSValue *allocate_prop_table(int size) {
  return (JSValue*) gc_malloc(sizeof(JSValue) * size);
}

/*
   allocates an iterator
 */
IteratorCell *allocate_iterator(void) {
  IteratorCell *iter =
    (IteratorCell *) gc_jsalloc(sizeof(IteratorCell), HTAG_ITERATOR);
  return iter;
}

/*
   allocates a regexp
 */
#ifdef USE_REGEXP
RegexpCell *allocate_regexp(void)
{
  RegexpCell *regexp =
    (RegexpCell *) gc_jsalloc(sizeof(RegexpCell), HTAG_REGEXP);
  return regexp;
}
#endif

/*
   allocates a boxed object
 */
BoxedCell *allocate_boxed(uint32_t type)
{
  BoxedCell *box = (BoxedCell *) gc_jsalloc(sizeof(BoxedCell), type);
  return box;
}
