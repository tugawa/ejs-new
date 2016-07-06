#include "prefix.h"
#define EXTERN extern
#include "header.h"

// a counter for debugging
//
#ifdef DEBUG
int count = 0;
#endif // DEBUG

// initializes the string table
//
void init_string_table(unsigned int size) {
  StrCons **a;

  a = (StrCons **)malloc(sizeof(StrCons*) * size);
  memset(a, 0, sizeof(StrCons*) * size);
  string_table.obvector = a;
  string_table.size = size;
  string_table.count = 0;
}

// allocates a flonum
// Note that the return value does not have a pointer tag.
//
FlonumCell *allocate_flonum(double d)
{
  FlonumCell *n;
  n = (FlonumCell *)malloc(sizeof(FlonumCell));
  n->value = d;
  n->header = HEADER_FLONUM;
  //LOG("count %d\n", ++count);
  return n;
}

// allocates a string
// It takes only the string length (except the last null character).
// Note that the return value does not have a pointer tag.
//
StringCell *allocate_string(int length)
{
  int size = (int)((length + BYTES_IN_JSVALUE) / BYTES_IN_JSVALUE
                   + sizeof(StringCell) - 1);
  StringCell *v = (StringCell *)malloc(size * BYTES_IN_JSVALUE);
  v->header = make_header(size, HTAG_STRING);
#ifdef STROBJ_HAS_HASH
  v->length = length;
#endif
  return v;
}

// allocates a string
// This takes a pointer to a C string.
//
JSValue allocate_string1(const char* str)
{
  uint32_t len;

  len = strlen(str);
  return str_intern(str, len, calc_hash_len(str, len), INTERN_HARD);
}

// allocates a string
// This takes two C strings and concatenates them.
//
JSValue allocate_string2(const char *str1, const char *str2)
{
  uint32_t len1, len2;

  len1 = strlen(str1);
  len2 = strlen(str2);
  return str_intern2(str1, len1, str2, len2,
                     calc_hash_len2(str1, len1, str2, len2), INTERN_HARD);
}

// allocates an object
// Note that the return value does not have a pointer tag.
//
Object *allocate_object(void) {
  Object *object = (Object *)malloc(sizeof(Object));
  object->header = HEADER_OBJECT;
  // LOG("count %d\n", ++count);
  return object;
}

// allocates an array
// Note that the return value does not have a pointer tag.
//
ArrayCell *allocate_array(void) {
  ArrayCell *array = (ArrayCell *)malloc(sizeof(ArrayCell));
  array->o.header = HEADER_ARRAY;
  return array;
}

// allocates an array body
//   size : number of elements in the body (size >= len)
//   len  : length of the array, i.e., subscripts under len are acrutally used
void allocate_array_data(ArrayCell *p, int size, int len)
{
  JSValue *a;
  int i;
  a = (JSValue *)malloc(sizeof(JSValue) * size);
  for (i = 0; i < len; i++) a[i] = JS_UNDEFINED; 
  array_body(p) = a;
  array_size(p) = size;
  array_length(p) = len;
}

// allocates a function
//
FunctionCell *allocate_function(void) {
  FunctionCell *function = (FunctionCell *)malloc(sizeof(FunctionCell));
  function->o.header = HEADER_FUNCTION;
  // LOG("count %d\n", ++count);
  return function;
}

// allocates a builtin
//
BuiltinCell *allocate_builtin(void) {
  BuiltinCell *builtin = (BuiltinCell *)malloc(sizeof(BuiltinCell));
  builtin->o.header = HEADER_BUILTIN;
  // LOG("count %d\n", ++count);
  return builtin;
}

JSValue *allocate_prop_table(int size) {
  // LOG("count %d\n", ++count);
  return (JSValue*)malloc(sizeof(JSValue) * size);
}

// allocates an iterator
//
IteratorCell *allocate_iterator(void) {
  IteratorCell *iter = (IteratorCell *)malloc(sizeof(IteratorCell));
  iter->o.header = HEADER_ITERATOR;
  return iter;
}

// allocates a regexp
//
#ifdef USE_REGEXP
RegexpCell *allocate_regexp(void)
{
  RegExpCell *regexp = (RegExpCell *)malloc(sizeof(RegExpCell));
  regexp->o.header = HEADER_REGEXP;
  regexp->pattern = NULL;
  regexp->reg = NULL;
  regexp->global = false;
  regexp->ignoreCase = false;
  regexp->multiline = false;
  return (JSValue)regexp;
}
#endif

// allocates a boxed object
//
BoxedCell *allocate_boxed(uint64_t b)
{
  BoxedCell *box = (BoxedCell *)malloc(sizeof(BoxedCell));
  switch(b){
  case HEADER_BOXED_STRING:
  case HEADER_BOXED_FLONUM:
  case HEADER_BOXED_BOOLEAN:
    box->o.header = b;
    break;
  default:
    assert(false);
  }
  return box;
}
