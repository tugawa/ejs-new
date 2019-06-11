/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#ifndef TYPES_H_
#define TYPES_H_

#ifdef USE_TYPES_GENERATED
#include "types-generated.h"
#else /* USE_TYPES_GENERATED */
#include "types-handcraft.h"
#endif /* USE_TYPES_GENERATED */

/*
 * First-class data in JavaScript is represented as a JSValue.
 * JSValue has 64 bits, where least sifnificat three bits is its tag.
 *
 *  ---------------------------------------------------
 *  |  pointer / immediate value                  |tag|
 *  ---------------------------------------------------
 *  63                                             210
 */
#define TAGOFFSET (3)
#define TAGMASK   (0x7)  /* 111 */

#define get_tag(p)      (((Tag)(p)) & TAGMASK)
#define put_tag(p,t)    ((JSValue)((uint64_t)(p) + (t)))
#define clear_tag(p)    ((uint64_t)(p) & ~TAGMASK)
#define remove_tag(p,t) (clear_tag(p))
#define equal_tag(p,t)  (get_tag((p)) == (t))

/*
 * Pair of two pointer tags
 * Note that the result of TAG_PAIR is of type Tag
 */
#define TAG_PAIR(t1, t2) ((t1) | ((t2) << TAGOFFSET))

#define TP_OBJOBJ TAG_PAIR(T_GENERIC, T_GENERIC)
#define TP_OBJSTR TAG_PAIR(T_GENERIC, T_STRING)
#define TP_OBJFLO TAG_PAIR(T_GENERIC, T_FLONUM)
#define TP_OBJSPE TAG_PAIR(T_GENERIC, T_SPECIAL)
#define TP_OBJFIX TAG_PAIR(T_GENERIC, T_FIXNUM)
#define TP_STROBJ TAG_PAIR(T_STRING, T_GENERIC)
#define TP_STRSTR TAG_PAIR(T_STRING, T_STRING)
#define TP_STRFLO TAG_PAIR(T_STRING, T_FLONUM)
#define TP_STRSPE TAG_PAIR(T_STRING, T_SPECIAL)
#define TP_STRFIX TAG_PAIR(T_STRING, T_FIXNUM)
#define TP_FLOOBJ TAG_PAIR(T_FLONUM, T_GENERIC)
#define TP_FLOSTR TAG_PAIR(T_FLONUM, T_STRING)
#define TP_FLOFLO TAG_PAIR(T_FLONUM, T_FLONUM)
#define TP_FLOSPE TAG_PAIR(T_FLONUM, T_SPECIAL)
#define TP_FLOFIX TAG_PAIR(T_FLONUM, T_FIXNUM)
#define TP_SPEOBJ TAG_PAIR(T_SPECIAL, T_GENERIC)
#define TP_SPESTR TAG_PAIR(T_SPECIAL, T_STRING)
#define TP_SPEFLO TAG_PAIR(T_SPECIAL, T_FLONUM)
#define TP_SPESPE TAG_PAIR(T_SPECIAL, T_SPECIAL)
#define TP_SPEFIX TAG_PAIR(T_SPECIAL, T_FIXNUM)
#define TP_FIXOBJ TAG_PAIR(T_FIXNUM, T_GENERIC)
#define TP_FIXSTR TAG_PAIR(T_FIXNUM, T_STRING)
#define TP_FIXFLO TAG_PAIR(T_FIXNUM, T_FLONUM)
#define TP_FIXSPE TAG_PAIR(T_FIXNUM, T_SPECIAL)
#define TP_FIXFIX TAG_PAIR(T_FIXNUM, T_FIXNUM)

typedef uint16_t Register;
typedef int16_t  Displacement;
typedef uint16_t Subscript;
typedef uint16_t Tag;

/*
 * Object
 * tag == T_GENERIC
 */
#ifdef HIDDEN_CLASS
/*
 * Hidden Class Transition
 *
 * A hidden class has a hash table, where each key is a property name
 * represented in a JS string.
 * Associated value for a key is either an index in the property array
 * (as a fixnum) or a pointer to the next hidden class.
 * The former is called `index entry' and the latter is called `transition
 * entry'.
 * Member n_entries has the number of entries registered in the map, i.e.,
 * the sum of the number of index entries and that of the transition entries.
 */
typedef struct hidden_class {
  uint32_t n_entries;
  int htype;              /* HTYPE_TRANSIT or HTYPE_GROW */
  uint32_t n_enter;       /* number of times this class is used */
  uint32_t n_exit;        /* number of times this class is left */
  HashTable *map;         /* map which is explained above */
} HiddenClass;

#define hidden_n_entries(h)    ((h)->n_entries)
#define hidden_htype(h)        ((h)->htype)
#define hidden_n_enter(h)      ((h)->n_enter)
#define hidden_n_exit(h)       ((h)->n_exit)
#define hidden_map(h)          ((h)->map)

#define HTYPE_TRANSIT   0
#define HTYPE_GROW      1

/*
 * #define new_empty_hidden_class(cxt, name, hsize)      \
 *   new_hidden_class(cxt, NULL, name, 0, hsize)
 */
#endif

typedef struct object_cell {
  uint64_t n_props;       /* number of properties */
  uint64_t limit_props;
#ifdef HIDDEN_CLASS
  HiddenClass *class;     /* Hidden class for this object */
#else
  HashTable *map;         /* map from property name to the index within prop */
#endif
  JSValue *prop;          /* array of property values */
} Object;

#define remove_simple_object_tag remove_normal_simple_object_tag
#define put_simple_object_tag    put_normal_simple_object_tag

#define make_simple_object(ctx)                         \
  (put_simple_object_tag(allocate_simple_object(ctx)))

#define remove_object_tag(p)    ((Object *)clear_tag(p))

#define obj_n_props(p)         ((remove_object_tag(p))->n_props)
#define obj_limit_props(p)     ((remove_object_tag(p))->limit_props)
#ifdef HIDDEN_CLASS
#define obj_hidden_class(p)    ((remove_object_tag(p))->class)
#define obj_hidden_class_map(p) (hidden_map(obj_hidden_class(p)))
#else
#define obj_map(p)             ((remove_object_tag(p))->map)
#endif
#define obj_prop(p)            ((remove_object_tag(p))->prop)
#define obj_prop_index(p,i)    ((remove_object_tag(p))->prop[i])

#define obj_header_tag(x)      gc_obj_header_type(remove_object_tag(x))
#define is_obj_header_tag(o,t) (is_object((o)) && (obj_header_tag((o)) == (t)))

#define PSIZE_NORMAL  20  /* default initial size of the property array */
#define PSIZE_BIG    100
#define PSIZE_DELTA   20  /* delta when expanding the property array */
#define PSIZE_LIMIT  500  /* limit size of the property array */
#define HSIZE_NORMAL  30  /* default initial size of the map (hash table) */
#define HSIZE_BIG    100

#define increase_psize(n)     (((n) >= PSIZE_LIMIT)? (n): ((n) + PSIZE_DELTA))

#ifdef HIDDEN_CLASS
#define HHH 0
#else
#define HHH HSIZE_NORMAL
#endif

#define new_normal_object(ctx)  new_simple_object(ctx, HHH, PSIZE_NORMAL)
#define new_normal_predef_object(ctx)                   \
  new_simple_object(ctx, HSIZE_NORMAL, PSIZE_NORMAL)
#define new_big_predef_object(ctx) new_simple_object(ctx, HSIZE_BIG, PSIZE_BIG)
#define new_big_predef_object_without_prototype(ctx)                    \
  new_simple_object_without_prototype(ctx, HSIZE_BIG, PSIZE_BIG)

#define new_normal_function(ctx, s) new_function(ctx, s, HHH, PSIZE_NORMAL)

#define new_normal_builtin(ctx, f, na)          \
  new_builtin(ctx, f, na, HHH, PSIZE_NORMAL)
#define new_normal_builtin_with_constr(ctx, f, cons, na)        \
  new_builtin_with_constr(ctx, f, cons, na, HHH, PSIZE_NORMAL)

#define new_big_builtin(ctx, f, cons, na)       \
  new_builtin(ctx, f, na, HSIZE_BIG, PSIZE_BIG)
#define new_big_builtin_with_constr(ctx, f, cons, na)                   \
  new_builtin_with_constr(ctx, f, cons, na, HSIZE_BIG, PSIZE_BIG)

#define new_normal_array(ctx) new_array(ctx, HHH, PSIZE_NORMAL)
#define new_normal_array_with_size(ctx, n)              \
  new_array_with_size(ctx, n, HHH, PSIZE_NORMAL)
#define new_normal_number_object(ctx, v)        \
  new_number_object(ctx, v, HHH, PSIZE_NORMAL)
#define new_normal_boolean_object(ctx, v)       \
  new_boolean_object(ctx, v, HHH, PSIZE_NORMAL)
#define new_normal_string_object(ctx, v)        \
  new_string_object(ctx, v, HHH, PSIZE_NORMAL)
#define new_normal_iterator(ctx, o) new_iterator(ctx, o)

#ifdef USE_REGEXP
#define new_normal_regexp(ctx, p, f) new_regexp(ctx, p, f, HHH, PSIZE_NORMAL)
#endif

/*
 * Array
 * tag == T_GENERIC
 */
typedef struct array_cell {
  Object o;
  uint64_t size;        /* size of the C array pointed from `body' field */
  uint64_t length;      /* length of the array, i.e., max subscript - 1 */
  JSValue *body;        /* pointer to a C array */
} ArrayCell;

#define make_array(ctx)       (put_normal_array_tag(allocate_array(ctx)))

#define array_object_p(a)     (&((remove_normal_array_tag(a))->o))
#define array_size(a)         ((remove_normal_array_tag(a))->size)
#define array_length(a)       ((remove_normal_array_tag(a))->length)
#define array_body(a)         ((remove_normal_array_tag(a))->body)
#define array_body_index(a,i) ((remove_normal_array_tag(a))->body[i])

#define ASIZE_INIT   10       /* default initial size of the C array */
#define ASIZE_DELTA  10       /* delta when expanding the C array */
#define ASIZE_LIMIT  100      /* limit size of the C array */
#define MAX_ARRAY_LENGTH  ((uint64_t)(0xffffffff))

#define increase_asize(n)     (((n) >= ASIZE_LIMIT)? (n): ((n) + ASIZE_DELTA))

#define MINIMUM_ARRAY_SIZE  100

/*
 * Function
 * tag == T_GENERIC
 */

typedef struct function_cell {
  Object o;
  FunctionTable *func_table_entry;
  FunctionFrame *environment;
} FunctionCell;

#define make_function()     (put_normal_function_tag(allocate_function()))

#define func_object_p(f)    (&((remove_normal_function_tag(f))->o))
#define func_table_entry(f) ((remove_normal_function_tag(f))->func_table_entry)
#define func_environment(f) ((remove_normal_function_tag(f))->environment)

/*
 * Builtin
 * tag == T_GENERIC
 */

/*
 * [FIXIT]
 * If variable number of arguments is allowed, the following information
 * is necessary.
 *   o number of required arguments
 *   o number of optional arguments
 *   etc.
 */

typedef void (*builtin_function_t)(Context*, int, int);

typedef struct builtin_cell {
  Object o;
  builtin_function_t body;
  builtin_function_t constructor;
  int n_args;
} BuiltinCell;

#define make_builtin()          (put_normal_builtin_tag(allocate_builtin()))

#define builtin_object_p(f)     (&((remove_normal_builtin_tag(f))->o))
#define builtin_body(f)         ((remove_normal_builtin_tag(f))->body)
#define builtin_constructor(f)  ((remove_normal_builtin_tag(f))->constructor)
#define builtin_n_args(f)       ((remove_normal_builtin_tag(f))->n_args)

/*
 * Iterator
 * tag == T_GENERIC
 */
typedef struct iterator {
  uint64_t size;        /* array size */
  uint64_t index;       /* array index */
  JSValue *body;        /* pointer to a C array */
} Iterator;

#define make_iterator()                                        \
  (put_normal_iterator_tag(allocate_iterator()))
#define iterator_size(i)                        \
  ((remove_normal_iterator_tag(i))->size)
#define iterator_index(i)                        \
  ((remove_normal_iterator_tag(i))->index)
#define iterator_body(i)                        \
  ((remove_normal_iterator_tag(i))->body)
#define iterator_body_index(a,i)                \
  ((remove_normal_iterator_tag(a))->body[i])

#ifdef USE_REGEXP
#ifdef need_normal_regexp

#include <oniguruma.h>

/*
 * Regexp
 * tag == T_GENERIC
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

#define make_regexp()          (put_normal_regexp_tag(allocate_regexp()))

#define regexp_object_p(r)     (&((remove_normal_regexp_tag(r))->o))
#define regexp_pattern(r)      ((remove_normal_regexp_tag(r))->pattern)
#define regexp_reg(r)          ((remove_normal_regexp_tag(r))->reg)
#define regexp_global(r)       ((remove_normal_regexp_tag(r))->global)
#define regexp_ignorecase(r)   ((remove_normal_regexp_tag(r))->ignorecase)
#define regexp_multiline(r)    ((remove_normal_regexp_tag(r))->multiline)
#define regexp_lastindex(r)    ((remove_normal_regexp_tag(r))->lastindex)
#endif /* need_normal_regexp */
#endif /* USE_REGEXP */

/*
 * Boxed Object
 * tag == T_GENERIC
 */
typedef struct boxed_cell {
  Object o;
  JSValue value;   /* boxed value; it is number, boolean, or string */
} BoxedCell;

#define make_number_object(ctx)                                         \
  (put_normal_number_object_tag(allocate_boxed((ctx), HTAG_BOXED_NUMBER)))
#define number_object_value(n)     (remove_normal_number_object_tag(n)->value)
#define number_object_object_ptr(n)             \
  (&((remove_normal_number_object_tag(n))->o))

#define make_boolean_object(ctx)                                        \
  (put_normal_number_object_tag(allocate_boxed((ctx), HTAG_BOXED_BOOLEAN)))
#define boolean_object_value(b)    (remove_normal_boolean_object_tag(b)->value)
#define boolean_object_object_ptr(b)            \
  (&((remove_normal_boolean_object_tag(b))->o))

#define make_string_object(ctx)                                         \
  (put_normal_number_object_tag(allocate_boxed((ctx), HTAG_BOXED_STRING)))
#define string_object_value(s)      (remove_normal_string_object_tag(s)->value)
#define string_object_object_ptr(s)             \
  (&((remove_normal_string_object_tag(s))->o))

/*
 * Flonum
 */
#if !defined(need_flonum)

#define flonum_value(p)      JS_UNDEFINED
#define double_to_flonum(n)  JS_UNDEFINED
#define int_to_flonum(i)     JS_UNDEFINED
#define cint_to_flonum(i)    JS_UNDEFINED
#define flonum_to_double(p)  0
#define flonum_to_cint(p)    0
#define flonum_to_int(p)     0
#define is_nan(p) JS_FALSE

#elif !defined(customised_flonum)

#define flonum_value(p)      (normal_flonum_value(p))
#define double_to_flonum(n)  (double_to_normal_flonum(n))
#define int_to_flonum(i)     (int_to_normal_flonum(i))
#define cint_to_flonum(i)    (cint_to_normal_flonum(i))
#define flonum_to_double(p)  (normal_flonum_to_double(p))
#define flonum_to_cint(p)    (normal_flonum_to_cint(p))
#define flonum_to_int(p)     (normal_flonum_to_int(p))
#define is_nan(p)            (normal_flonum_is_nan(p))

#endif

#ifdef need_normal_flonum

/*
 * FlonumCell
 * tag == T_FLONUM
 */
typedef struct flonum_cell {
  double value;
} FlonumCell;

#define normal_flonum_value(p)      ((remove_normal_flonum_tag(p))->value)
#define double_to_normal_flonum(n)  (put_normal_flonum_tag(allocate_flonum(n)))
#define int_to_normal_flonum(i)     cint_to_flonum(i)
#define cint_to_normal_flonum(i)    double_to_flonum((double)(i))
#define normal_flonum_to_double(p)  flonum_value(p)
#define normal_flonum_to_cint(p)    ((cint)(flonum_value(p)))
#define normal_flonum_to_int(p)     ((int)(flonum_value(p)))
#define normal_flonum_is_nan(p)                         \
  (is_flonum((p))? isnan(flonum_to_double((p))): 0)

#endif /* need_flonum */


/*
 * String
 */
#ifndef customised_string
#define string_value(p)   (normal_string_value(p))
#define string_hash(p)    (normal_string_hash(p))
#define string_length(p)  (normal_string_length(p))
#define cstr_to_string(ctx,str) (cstr_to_normal_string((ctx),(str)))
#define ejs_string_concat(ctx,str1,str2)                \
  (ejs_normal_string_concat((ctx),(str1),(str2)))
#endif /* customised_string */

#ifdef need_normal_string

/*
 * StringCell
 * tag == T_STRING
 */
typedef struct string_cell {
#ifdef STROBJ_HAS_HASH
  uint32_t hash;           /* hash value before computing mod */
  uint32_t length;         /* length of the string */
#endif
  char value[BYTES_IN_JSVALUE];
} StringCell;

#define string_to_cstr(p) (string_value(p))

#define normal_string_value(p)   ((remove_normal_string_tag(p))->value)

#ifdef STROBJ_HAS_HASH
#define normal_string_hash(p)       ((remove_normal_string_tag(p))->hash)
#define normal_string_length(p)     ((remove_normal_string_tag(p))->length)
#else
#define normal_string_hash(p)       (calc_hash(string_value(p)))
#define normal_string_length(p)     (strlen(string_value(p)))
#endif /* STROBJ_HAS_HASH */

#define cstr_to_normal_string(ctx, str)  (cstr_to_string_ool((ctx), (str)))
/* TODO: give a nice name to ejs_string_concat
 *       (string_concat is used for builtin function) */
#define ejs_normal_string_concat(ctx, str1, str2)       \
  (string_concat_ool((ctx), (str1), (str2)))

#endif /* need_normal_string */

/*
 * Object header
 *
 *  ---------------------------------------------------
 *  |  object size in bytes  |    header tag          |
 *  ---------------------------------------------------
 *  63                     32 31                     0
 */

/* change name: OBJECT_xxx -> HEADER_xxx (ugawa) */
#define HEADER_SIZE_OFFSET   (32)
#define HEADER_TYPE_MASK     ((uint64_t)0x000000ff)
#define HEADER_SHARED_MASK   ((uint64_t)0x80000000)
#define FUNCTION_ATOMIC_MASK ((uint64_t)0x40000000)

#define make_header(s, t) (((uint64_t)(s) << HEADER_SIZE_OFFSET) | (t))

/*
 * header tags for non-JS objects
 */
/* HTAG_FREE is defined in gc.c */
#define HTAG_PROP           (0x11)
#define HTAG_ARRAY_DATA     (0x12)
#define HTAG_FUNCTION_FRAME (0x13)
#define HTAG_HASH_BODY      (0x14)
#define HTAG_STR_CONS       (0x15)
#define HTAG_CONTEXT        (0x16)
#define HTAG_STACK          (0x17)
#ifdef HIDDEN_CLASS
#define HTAG_HIDDEN_CLASS   (0x18)
#endif

/*
 * Fixnum
 * tag == T_FIXNUM
 *
 * In 64-bits environment, C's `int' is a 32-bits integer.
 * A fixnum value (61-bits signed integer) cannot be represented in an int. 
 * So we use `cint' to represent a fixnum value.
 */

typedef int64_t cint;
typedef uint64_t cuint;


/* #define fixnum_to_int(p) (((int64_t)(p)) >> TAGOFFSET) */
#define fixnum_to_cint(p) (((cint)(p)) >> TAGOFFSET)
#define fixnum_to_int(p)  ((int)fixnum_to_cint(p))
#define fixnum_to_double(p) ((double)(fixnum_to_cint(p)))

/*
 * #define int_to_fixnum(f) \
 * ((JSValue)(put_tag((((uint64_t)(f)) << TAGOFFSET), T_FIXNUM)))
 */
#define int_to_fixnum(f)    cint_to_fixnum(((cint)(f)))
#define cint_to_fixnum(f)   put_tag(((uint64_t)(f) << TAGOFFSET), T_FIXNUM)

/* #define double_to_fixnum(f) int_to_fixnum((int64_t)(f)) */
#define double_to_fixnum(f) cint_to_fixnum((cint)(f))

#define is_fixnum_range_cint(n)                                 \
  ((MIN_FIXNUM_CINT <= (n)) && ((n) <= MAX_FIXNUM_CINT))

#define is_integer_value_double(d) ((d) == (double)((cint)(d)))

#define is_fixnum_range_double(d)                                       \
  (is_integer_value_double(d) && is_fixnum_range_cint((cint)(d)))

#define in_fixnum_range(dval)                           \
  ((((double)(dval)) == ((double)((int64_t)(dval))))    \
   && ((((int64_t)(dval)) <= MAX_FIXNUM_INT)            \
       && (((int64_t)(dval)) >= MIN_FIXNUM_INT)))

#define in_flonum_range(ival)                           \
  ((ival ^ (ival << 1))                                 \
   & ((int64_t)1 << (BITS_IN_JSVALUE - TAGOFFSET)))

#define half_fixnum_range(ival)                                         \
  (((MIN_FIXNUM_CINT / 2) <= (ival)) && ((ival) <= (MAX_FIXNUM_CINT / 2)))

#define FIXNUM_ZERO (cint_to_fixnum((cint)0))
#define FIXNUM_ONE  (cint_to_fixnum((cint)1))
#define FIXNUM_TEN  (cint_to_fixnum((cint)10))

#define MAX_FIXNUM_CINT (((cint)(1) << (BITS_IN_JSVALUE - TAGOFFSET - 1)) - 1)
#define MIN_FIXNUM_CINT (-MAX_FIXNUM_CINT-1)


#define cint_to_number(n)                                               \
  (is_fixnum_range_cint((n))? cint_to_fixnum((n)): cint_to_flonum((n)))

#define number_to_double(p)                                     \
  ((is_fixnum(p)? fixnum_to_double(p): flonum_to_double(p)))
#define double_to_number(d)                                             \
  ((is_fixnum_range_double(d))? double_to_fixnum(d): double_to_flonum(d))

/*
 * Special
 * tag == T_SPECIAL
 */
#define SPECIALOFFSET           (TAGOFFSET + 1)
#define SPECIALMASK             ((uint64_t)(1 << SPECIALOFFSET) - 1)

#define make_special(spe,t)     ((JSValue)((spe) << SPECIALOFFSET | (t)))
#define special_tag(p)          ((uint64_t)(p) & SPECIALMASK)
#define special_equal_tag(p,t)  (special_tag((p)) == (t))

/*
 * Special - Boolean
 */
#define T_BOOLEAN         ((0x1 << TAGOFFSET) | T_SPECIAL)
#define JS_TRUE           make_special(1, T_BOOLEAN)
#define JS_FALSE          make_special(0, T_BOOLEAN)

#define is_boolean(p)     (special_tag((p)) == T_BOOLEAN)
#define is_true(p)        ((p) == JS_TRUE)
#define is_false(p)       ((p) == JS_FALSE)
#define int_to_boolean(e) ((e) ? JS_TRUE : JS_FALSE)

#define true_false(e)     ((e) ? JS_TRUE : JS_FALSE)
#define false_true(e)     ((e) ? JS_FALSE : JS_TRUE)

/*
 * Special - Others
 */
#define T_OTHER           ((0x0 << TAGOFFSET) | T_SPECIAL)
#define JS_NULL           make_special(0, T_OTHER)
#define JS_UNDEFINED      make_special(1, T_OTHER)

#define is_null_or_undefined(p)  (special_tag((p)) == T_OTHER)
#define is_null(p)               ((p) == JS_NULL)
#define is_undefined(p)          ((p) == JS_UNDEFINED)

/*
 * Primitive is either number, boolean, or string.
 */
#define is_primitive(p) (!is_object(p) && !is_null_or_undefined(p))

/*
 * Set a specified property to an object where property name is given
 * by a string object or a C string.
 */

#define set_prop_none(c, o, s, v)                       \
  set_prop_with_attribute(c, o, s, v, ATTR_NONE)
#define set_prop_all(c, o, s, v) set_prop_with_attribute(c, o, s, v, ATTR_ALL)
#define set_prop_de(c, o, s, v) set_prop_with_attribute(c, o, s, v, ATTR_DE)

#define set___proto___none(c, o, v)                     \
  set_prop_none(c, o, gconsts.g_string___proto__, v)
#define set___proto___all(c, o, v)                      \
  set_prop_all(c, o, gconsts.g_string___proto__, v)
#define set___proto___de(c, o, v)                       \
  set_prop_de(c, o, gconsts.g_string___proto__, v)
#define set_prototype_none(c, o, v)                     \
  set_prop_none(c, o, gconsts.g_string_prototype, v)
#define set_prototype_all(c, o, v)                      \
  set_prop_all(c, o, gconsts.g_string_prototype, v)
#define set_prototype_de(c, o, v)                       \
  set_prop_de(c, o, gconsts.g_string_prototype, v)

#define set_obj_cstr_prop(c, o, s, v, attr)                             \
  set_prop_with_attribute(c, o, cstr_to_string((c),(s)), v, attr)
#define set_obj_cstr_prop_none(c, o, s, v)      \
  set_obj_cstr_prop(c, o, s, v, ATTR_NONE)

#define get___proto__(o, r) get_prop(o, gconsts.g_string___proto__, r)
#endif
