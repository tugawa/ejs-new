/*
   types.h

   SSJS Project at the University of Electro-communications

   Sho Takada, 2012-13
   Akira Tanimura, 2012-13
   Akihiro Urushihara, 2013-14
   Ryota Fujii, 2013-14
   Tomoharu Ugawa, 2013-16
   Hideya Iwasaki, 2013-16
*/

#ifndef TYPES_H_
#define TYPES_H_

/*
   First-class data in JavaScript is represented as a JSValue.
   JSValue has 64 bits, where least sifnificat three bits is its tag.

    ---------------------------------------------------
    |  pointer / immediate value                  |tag|
    ---------------------------------------------------
    63                                             210
 */
#define TAGOFFSET (3)
#define TAGMASK   (0x7)  // 111

#define get_tag(p)      (((Tag)(p)) & TAGMASK)
#define put_tag(p,t)    ((JSValue)((uint64_t)(p) + (t)))
#define remove_tag(p,t) ((uint64_t)(p) - (t))
#define equal_tag(p,t)  (get_tag((p)) == (t))

/*
   Objects
 */
#define T_OBJECT  (0x0)  // 000
#define T_VARIANT (0x1)  // 001  This is used for type inference by Urushihara.
#define T_UNUSED1 (0x2)  // 010
#define T_UNUSED2 (0x3)  // 011

/*
   Constant
 */
#define T_STRING  (0x4)  // 100
#define T_FLONUM  (0x5)  // 101
#define T_SPECIAL (0x6)  // 110
#define T_FIXNUM  (0x7)  // 111

/*
   Pair of two pointer tags
   Note that Tag the result of TAG_PAIR is of type Tag
 */
#define TAG_PAIR(t1, t2) ((t1) | ((t2) << TAGOFFSET))

#define TP_OBJOBJ TAG_PAIR(T_OBJECT, T_OBJECT)
#define TP_OBJSTR TAG_PAIR(T_OBJECT, T_STRING)
#define TP_OBJFLO TAG_PAIR(T_OBJECT, T_FLONUM)
#define TP_OBJSPE TAG_PAIR(T_OBJECT, T_SPECIAL)
#define TP_OBJFIX TAG_PAIR(T_OBJECT, T_FIXNUM)
#define TP_STROBJ TAG_PAIR(T_STRING, T_OBJECT)
#define TP_STRSTR TAG_PAIR(T_STRING, T_STRING)
#define TP_STRFLO TAG_PAIR(T_STRING, T_FLONUM)
#define TP_STRSPE TAG_PAIR(T_STRING, T_SPECIAL)
#define TP_STRFIX TAG_PAIR(T_STRING, T_FIXNUM)
#define TP_FLOOBJ TAG_PAIR(T_FLONUM, T_OBJECT)
#define TP_FLOSTR TAG_PAIR(T_FLONUM, T_STRING)
#define TP_FLOFLO TAG_PAIR(T_FLONUM, T_FLONUM)
#define TP_FLOSPE TAG_PAIR(T_FLONUM, T_SPECIAL)
#define TP_FLOFIX TAG_PAIR(T_FLONUM, T_FIXNUM)
#define TP_SPEOBJ TAG_PAIR(T_SPECIAL, T_OBJECT)
#define TP_SPESTR TAG_PAIR(T_SPECIAL, T_STRING)
#define TP_SPEFLO TAG_PAIR(T_SPECIAL, T_FLONUM)
#define TP_SPESPE TAG_PAIR(T_SPECIAL, T_SPECIAL)
#define TP_SPEFIX TAG_PAIR(T_SPECIAL, T_FIXNUM)
#define TP_FIXOBJ TAG_PAIR(T_FIXNUM, T_OBJECT)
#define TP_FIXSTR TAG_PAIR(T_FIXNUM, T_STRING)
#define TP_FIXFLO TAG_PAIR(T_FIXNUM, T_FLONUM)
#define TP_FIXSPE TAG_PAIR(T_FIXNUM, T_SPECIAL)
#define TP_FIXFIX TAG_PAIR(T_FIXNUM, T_FIXNUM)

typedef uint16_t Register;
typedef int16_t  Displacement;
typedef uint16_t Subscript;
typedef uint16_t Tag;

/*
   Object
   tag == T_OBJECT
 */
typedef struct object_cell {
  uint64_t header;        // header
  uint64_t n_props;       // number of properties
  uint64_t limit_props;   //
  HashTable *map;         // map from property name to the index within prop
  JSValue *prop;          // array of property values
#ifdef PARALLEL
  pthread_mutex_t mutex;
#endif
} Object;

#define is_object(p)           (equal_tag((p), T_OBJECT))
#define put_object_tag(p)      (put_tag(p, T_OBJECT))
#define remove_object_tag(p)   ((Object *)(remove_tag((p), T_OBJECT)))
#define make_object()          (put_object_tag(allocate_object()))

#define obj_header(p)          ((remove_object_tag(p))->header)
#define obj_n_props(p)         ((remove_object_tag(p))->n_props)
#define obj_limit_props(p)     ((remove_object_tag(p))->limit_props)
#define obj_map(p)             ((remove_object_tag(p))->map)
#define obj_prop(p)            ((remove_object_tag(p))->prop)
#define obj_prop_index(p,i)    ((remove_object_tag(p))->prop[i])

#define is_callable(p)     (is_object(p) && (isFunction(p) || isBuiltin(p) || isForeign(p)))
#define obj_header_tag(x)      ((Tag)(obj_header(x) & OBJECT_HEADER_MASK))
#define is_obj_header_tag(o,t) (is_object((o)) && (obj_header_tag((o)) == (t)))
#define obj_size(x)            (obj_header(x) >> OBJECT_SIZE_OFFSET)



/*
   Array
   tag == T_OBJECT
 */
typedef struct array_cell {
  Object o;
  uint64_t size;        // 2^n
  uint64_t length;      // user-spefified size of the array
  JSValue* body;
} ArrayCell;

#define is_array(p)           is_obj_header_tag((p), HTAG_ARRAY)
#define put_array_tag(p)      (put_tag(p, T_OBJECT))
#define remove_array_tag(p)   ((ArrayCell *)(remove_tag((p), T_OBJECT)))
#define make_array()          (put_array_tag(allocate_array()))

#define array_body(a)         ((remove_array_tag(a))->body)
#define array_body_index(a,i) ((remove_array_tag(a))->body[i])
#define array_size(a)         ((remove_array_tag(a))->size)
#define array_length(a)       ((remove_array_tag(a))->length)


#define MINIMUM_ARRAY_SIZE  100

/*
   Function
   tag == T_OBJECT
 */

typedef struct function_cell {
  Object o;
  FunctionTable *func_table_entry;
  FunctionFrame *environment;
} FunctionCell;

#define is_function(p)         is_obj_header_tag((p), HTAG_FUNCTION)
#define put_function_tag(p)    (put_tag(p, T_OBJECT))
#define remove_function_tag(p) ((FunctionCell *)(remove_tag((p), T_OBJECT)))
#define make_function()        (put_function_tag(allocate_function()))

#define func_table_entry(f)    ((remove_function_tag(f))->func_table_entry)
#define func_environment(f)    ((remove_function_tag(f))->environment)

/*
   Builtin
   tag == T_OBJECT
 */

/*
   [FIXIT]
   If variable number of arguments is allowed, the following information
   is necessary.
     o number of required arguments
     o number of optional arguments
     etc.
 */

typedef void (*builtin_function_t)(Context*, int, int);

typedef struct builtin_cell {
  Object o;
  builtin_function_t body;
  builtin_function_t constructor;
  int n_args;
} BuiltinCell;

#define is_builtin(p)           is_obj_header_tag((p), HTAG_BUILTIN)
#define put_builtin_tag(p)      (put_tag(p, T_OBJECT))
#define remove_builtin_tag(p)   ((BuiltinCell *)remove_tag((p), T_OBJECT))
#define make_builtin()          (put_builtin_tag(allocate_builtin()))

#define builtin_body(f)         ((remove_builtin_tag(f))->body)
#define builtin_constructor(f)  ((remove_builtin_tag(f))->constructor)
#define builtin_n_args(f)       ((remove_builtin_tag(f))->n_args)

#define is_function_or_builtin(p) \
  (is_object((p)) && \
   ((obj_header_tag((p)) == HTAG_FUNCTION) || \
    (obj_header_tag((p)) == HTAG_BUILTIN))

/*
   Iterator
   tag == T_OBJECT
 */
typedef struct iterator_cell {
  Object o;
  HashIterator iter;
} IteratorCell;

#define is_iterator(p)           is_obj_header_tag((p), HTAG_ITERATOR)
#define remove_iterator_tag(p)   ((IteratorCell *)remove_tag((p), T_OBJECT))
#define put_iterator_tag(p)      (put_tag(p, T_OBJECT))
#define make_iterator()          (put_iterator_tag(allocate_iterator()))

#define iterator_iter(i)         ((remove_iterator_tag(i))->iter)

#ifdef USE_REGEXP
/*
   Regexp
   tag == T_OBJECT
 */
typedef struct regexp_cell {
  Object o;
  char *pattern;
  regex_t *reg;
  bool global;
  bool ignorecase;
  bool multiline;
  int lastindex;
} RegexpCell;

#define F_REGEXP_NONE      (0x0)
#define F_REGEXP_GLOBAL    (0x1)
#define F_REGEXP_IGNORE    (0x2)
#define F_REGEXP_MULTILINE (0x4)

#define is_regexp(r)           is_obj_header_tag((r), HTAG_REGEXP)
#define put_regexp_tag(p)      (put_tag(p, T_OBJECT))
#define remove_regexp_tag(p)   ((RegexpCell *)remove_tag((p), T_OBJECT))
#define make_regexp()          (put_regexp_tag(allocate_regexp()))

#define regexp_pattern(r)      ((remove_regexp_tag(r))->pattern)
#define regexp_reg(r)          ((remove_regexp_tag(r))->reg)
#define regexp_global(r)       ((remove_regexp_tag(r))->global)
#define regexp_ignorecase(r)   ((remove_regexp_tag(r))->ignorecase)
#define regexp_multiline(r)    ((remove_regexp_tag(r))->multiline)
#define regexp_lastindex(r)    ((remove_regexp_tag(r))->lastindex)

#endif

/*
   Boxed Object
   tag == T_OBJECT
 */
typedef struct boxed_cell {
  Object o;
  JSValue value;   // boxed value; it is number, boolean, or string
} BoxedCell;

#define remove_boxed_tag(p)      ((BoxedCell *)(remove_tag((p), T_OBJECT)))
#define put_boxed_tag(p)         (put_tag(p, T_OBJECT))
#define make_boxed(t)            (put_boxed_tag(allocate_boxed((t))))
#define boxed_value(b)           ((remove_boxed_tag(b))->value)

#define is_number_object(p)      is_obj_header_tag((p), HTAG_BOXED_NUMBER)
#define make_number_object()     make_boxed(HEADER_BOXED_NUMBER)
#define number_object_value(n)   boxed_value((n))

#define is_boolean_object(p)     is_obj_header_tag((p), HTAG_BOXED_BOOLEAN)
#define make_boolean_object()    make_boxed(HEADER_BOXED_BOOLEAN)
#define boolean_object_value(b)  boxed_value((b))

#define is_string_object(p)      is_obj_header_tag((p), HTAG_BOXED_STRING)
#define make_string_object()     make_boxed(HEADER_BOXED_STRING)
#define string_object_value(s)   boxed_value((s))

/*
   FlonumCell
   tag == T_FLONUM
 */
typedef struct flonum_cell {
  uint64_t header;
  double value;
} FlonumCell;

#define is_flonum(p)         (equal_tag((p), T_FLONUM))
#define put_flonum_tag(p)    (put_tag(p, T_FLONUM))
#define remove_flonum_tag(p) ((FlonumCell *)remove_tag((p), T_FLONUM))

#define flonum_value(p)      ((remove_flonum_tag(p))->value)

#define double_to_flonum(n)  (put_flonum_tag(allocate_flonum(n)))
#define int_to_flonum(i)     cint_to_flonum(i)
#define cint_to_flonum(i)    double_to_flonum((double)(i))

#define flonum_to_double(p)  flonum_value(p)
#define flonum_to_cint(p)    ((cint)(flonum_value(p)))

#define is_nan(p) (is_flonum((p))? isnan(flonum_to_double((p))): 0)

/*
   StringCell
   tag == T_STRING
 */
typedef struct string_cell {
  uint64_t header;
#ifdef STROBJ_HAS_HASH
  uint32_t hash;           // hash value before computing mod
  uint32_t length;         // length of the string
#endif // STROBJ_HAS_HASH
  char value[BYTES_IN_JSVALUE];
} StringCell;

#define is_string(p)         (equal_tag((p), T_STRING))
#define put_string_tag(p)    (put_tag(p, T_STRING))
#define remove_string_tag(p) ((StringCell *)remove_tag((p), T_STRING))

#define string_value(p)      ((remove_string_tag(p))->value)
#define string_to_cstr(p)    string_value(p)

#ifdef STROBJ_HAS_HASH
#define string_hash(p)       ((remove_string_tag(p))->hash)
#define string_length(p)     ((remove_string_tag(p))->length)
#else
#define string_hash(p)       (calc_hash(string_value(p)))
#define string_length(p)     (strlen(string_value(p)))
#endif // STROBJ_HAS_HASH

#define cstr_to_string(str) \
  (str_intern(str, strlen(str), calc_hash(str), INTERN_HARD))

#define cstr_to_string2(str1, str2) (allocate_string2(str1, str2))

/*
   Object header
  
    ---------------------------------------------------
    |  object size in bytes  |    header tag          |
    ---------------------------------------------------
    63                     32 31                     0
 */

#define OBJECT_SIZE_OFFSET   (32)
#define OBJECT_HEADER_MASK   ((uint64_t)0x3fffffff)
#define OBJECT_SHARED_MASK   ((uint64_t)0x80000000)
#define FUNCTION_ATOMIC_MASK ((uint64_t)0x40000000)

#define make_header(s, t) (((uint64_t)(s) << OBJECT_SIZE_OFFSET) | (t))

/*
   header tags
 */
#define HTAG_STRING        (0x4)
#define HTAG_FLONUM        (0x5)
#define HTAG_OBJECT        (0x6)
#define HTAG_ARRAY         (0x7)
#define HTAG_FUNCTION      (0x8)
#define HTAG_BUILTIN       (0x9)
#define HTAG_ITERATOR      (0xa)
#define HTAG_REGEXP        (0xb)
#define HTAG_BOXED_STRING  (0xc)
#define HTAG_BOXED_NUMBER  (0xd)
#define HTAG_BOXED_BOOLEAN (0xe)

#define HEADER_COMMON(cell, htag) \
   (make_header((sizeof(cell) / BYTES_IN_JSVALUE), htag))
#define HEADER_FLONUM         HEADER_COMMON(FlonumCell, HTAG_FLONUM)
#define HEADER_OBJECT         HEADER_COMMON(Object, HTAG_OBJECT)
#define HEADER_ARRAY          HEADER_COMMON(ArrayCell, HTAG_ARRAY)
#define HEADER_FUNCTION       HEADER_COMMON(FunctionCell, HTAG_FUNCTION)
#define HEADER_BUILTIN        HEADER_COMMON(BuiltinCell, HTAG_BUILTIN)
#define HEADER_ITERATOR       HEADER_COMMON(IteratorCell, HTAG_ITERATOR)
#define HEADER_REGEXP         HEADER_COMMON(RegexpCell, HTAG_REGEXP)
#define HEADER_BOXED_STRING   HEADER_COMMON(BoxedCell, HTAG_BOXED_STRING)
#define HEADER_BOXED_NUMBER   HEADER_COMMON(BoxedCell, HTAG_BOXED_NUMBER)
#define HEADER_BOXED_BOOLEAN  HEADER_COMMON(BoxedCell, HTAG_BOXED_BOOLEAN)

/*
   Fixnum
   tag == T_FIXNUM

   In 64-bits environment, C's `int' is a 32-bits integer.
   A fixnum value (61-bits signed integer) cannot be represented in an int. 
   So we use `cint' to represent a fixnum value.
*/

typedef int64_t cint;
typedef uint64_t cuint;

#define is_fixnum(p) (equal_tag((p), T_FIXNUM))

#define fixnum_to_int(p) (((int64_t)(p)) >> TAGOFFSET)
#define fixnum_to_cint(p) (((cint)(p)) >> TAGOFFSET)
#define fixnum_to_double(p) ((double)(fixnum_to_cint(p)))

#define int_to_fixnum(f) ((JSValue)(put_tag((((uint64_t)(f)) << TAGOFFSET), T_FIXNUM)))
#define cint_to_fixnum(f)   put_tag(((f) << TAGOFFSET), T_FIXNUM)

#define double_to_fixnum(f) int_to_fixnum((int64_t)(f))

#define is_fixnum_range_cint(n) \
  ((MIN_FIXNUM_CINT <= (n)) && ((n) <= MAX_FIXNUM_CINT))

#define is_integer_value_double(d) ((d) == (double)((cint)(d)))

#define is_fixnum_range_double(d) \
  (is_integer_value_double(d) && is_fixnum_range_cint((cint)(d)))

#define in_fixnum_range(dval) \
  ((((double)(dval)) == ((double)((int64_t)(dval)))) \
  && ((((int64_t)(dval)) <= MAX_FIXNUM_INT) \
  && (((int64_t)(dval)) >= MIN_FIXNUM_INT)))

#define in_flonum_range(ival) \
  ((ival ^ (ival << 1)) \
  & ((int64_t)1 << (BITS_IN_JSVALUE - TAGOFFSET)))

#define half_fixnum_range(ival) \
  (((MIN_FIXNUM_CINT / 2) <= (ival)) && ((ival) <= (MAX_FIXNUM_CINT / 2)))

#define FIXNUM_ZERO (cint_to_fixnum((cint)0))
#define FIXNUM_ONE  (cint_to_fixnum((cint)1))
#define FIXNUM_TEN  (cint_to_fixnum((cint)10))

#define MAX_FIXNUM_CINT (((cint)(1) << (BITS_IN_JSVALUE - TAGOFFSET - 1)) - 1)
#define MIN_FIXNUM_CINT ((cint)(-1) << (BITS_IN_JSVALUE - TAGOFFSET - 1))

#define is_number(p) (is_fixnum((p)) || is_flonum((p)))

#define cint_to_number(n) \
  (is_fixnum_range_cint((n))? cint_to_fixnum((n)): cint_to_flonum((n)))

#define number_to_double(p) \
  ((is_fixnum(p)? fixnum_to_double(p): flonum_to_double(p)))
#define double_to_number(d) \
  ((is_fixnum_range_double(d))? double_to_fixnum(d): double_to_flonum(d))

/*
   Special
   tag == T_SPECIAL
 */
#define SPECIALOFFSET           (TAGOFFSET + 1)
#define SPECIALMASK             ((uint64_t)(1 << SPECIALOFFSET) - 1)

#define is_special(p)           (equal_tag((p), T_SPECIAL))
#define remove_special_tag(p)   (remove_tag((p), T_SPECIAL))
#define make_special(spe,t)     ((JSValue)((spe) << SPECIALOFFSET | (t)))
#define special_tag(p)          ((uint64_t)(p) & SPECIALMASK)
#define special_equal_tag(p,t)  (special_tag((p)) == (t))

/*
   Special - Boolean
 */
#define T_BOOLEAN         ((0x1 << TAGOFFSET) | T_SPECIAL)
#define JS_TRUE           make_special(1, T_BOOLEAN)
#define JS_FALSE          make_special(0, T_BOOLEAN)

#define is_boolean(p)     (special_tag((p)) == T_BOOLEAN)
#define is_true(p)        ((p) == JS_TRUE)
#define is_false(p)       ((p) == JS_FALSE)
#define int_to_boolean(e) ((e) ? JS_TRUE : JS_FALSE)

/*
   Special - Others
 */
#define T_OTHER           ((0x0 << TAGOFFSET) | T_SPECIAL)
#define JS_NULL           make_special(0, T_OTHER)
#define JS_UNDEFINED      make_special(1, T_OTHER)

#define is_null_or_undefined(p)  (special_tag((p)) == T_OTHER)
#define is_null(p)               ((p) == JS_NULL)
#define is_undefined(p)          ((p) == JS_UNDEFINED)

/*
   Primitive is either number, boolean, or string.
 */
#define is_primitive(p) (get_tag(p) != T_OBJECT && (!is_null_or_undefined(p)))

/*
   Set a specified property to an object where property name is given
   by a string object or a C string.
 */

#define set_prop_none(o, s, v) set_prop_with_attribute(o, s, v, ATTR_NONE)
#define set_prop_all(o, s, v) set_prop_with_attribute(o, s, v, ATTR_ALL)
#define set_prop_de(o, s, v) set_prop_with_attribute(o, s, v, ATTR_DE)

#define set_obj_cstr_prop(o, s, v, attr) \
  set_prop_with_attribute(o, cstr_to_string(s), v, attr)
#define set_obj_cstr_prop_none(o, s, v) set_obj_cstr_prop(o, s, v, ATTR_NONE)

#endif
