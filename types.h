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

#define get_tag(p)        (((Tag)(p)) & TAGMASK)
#define put_tag(p,t)      ((JSValue)((uint64_t)(p) + (t)))
#define clear_tag(p)      ((uint64_t)(p) & ~TAGMASK)
#define remove_tag(p,t)   (clear_tag(p))
#define equal_tag(p,t)    (get_tag((p)) == (t))
#define obj_header_tag(x) (gc_obj_header_type((void *) clear_tag(x)))

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
 * header tags for non-JS objects
 */
/* HTAG_FREE is defined in gc.c */
#define HTAG_PROP           (0x11) /* Array of JSValues */
#define HTAG_ARRAY_DATA     (0x12) /* Array of JSValues */
#define HTAG_FUNCTION_FRAME (0x13) /* FunctionFrame */
#define HTAG_STR_CONS       (0x14) /* StrCons */
#define HTAG_CONTEXT        (0x15) /* Context */
#define HTAG_STACK          (0x16) /* Array of JSValues */
#ifdef HIDDEN_CLASS
#define HTAG_HIDDEN_CLASS   (0x17) /* HiddenClass */
#endif
#define HTAG_HASHTABLE      (0x18)
#define HTAG_HASH_BODY      (0x19)
#define HTAG_HASH_CELL      (0x1A)
#define HTAG_PROPERTY_MAP   (0x1B)
#define HTAG_SHAPE          (0x1C)

#ifdef DEBUG
#define DEBUG_NAME(name) name
#else /* DEBUG */
#define DEBUG_NAME(name) ""
#endif /* DEBUG */

/*
 * Hidden Class Transition
 *
 * A hidden class has a hash table, where each key is a property name
 * represented in a JS string.
 * Associated value for a key is either an index in the property array
 * (as a fixnum) or a pointer to the next hidden class.
 * The former is called `index entry' and the latter is called `transition
 * entry'.
 */

typedef struct property_map {
  HashTable *map;            /* [const] property map and transitions */
  struct property_map *prev; /* [weak] pointer to the previous map */
  struct shape *shapes;      /* Weak list of existing shapes arranged from
                              * more specialised to less.
                              * (this pointer is strong) */
  JSValue   __proto__;       /* [const] __proto__ of the object. */
  uint32_t n_props;          /* [const] Number of properties in map.
                              * This number includes special props. */
  uint32_t n_special_props;  /* [const] Number of special props. */
#ifdef DEBUG
  char *name;
#endif /* DEBUG */
} PropertyMap;

typedef struct shape {
  PropertyMap *pm;            /* [const] Pointer to the map. */
  struct shape *next;         /* [weak] Weak list of exisnting shapes
                               * shareing the same map. */
  uint32_t n_embedded_slots;  /* [const] Number of slots for properties
                               * in the object. This number includes 
                               * special props. */
  uint32_t n_extension_slots; /* [const] Size of extension array. */
#ifdef DEBUG
  char *name;
#endif /* DEBUG */
#ifdef HC_PROF
  uint32_t n_enter;
  uint32_t n_exit;
  uint32_t is_dead;
  uint32_t is_printed;
#endif /* HC_PROF */
} Shape;

/*
 * JSObject is used for
 *   - simple_object
 *   - function_object
 *   - builtin_object
 *   - array_object
 *   - string_object
 *   - number_object
 *   - boolean_object
 */

#define JSOBJECT_MIN_EMBEDDED 1
typedef struct jsobject_cell {
  Shape *shape;
#ifdef ALLOC_SITE_CACHE
  AllocSite *alloc_site;
#endif /* ALLOC_SITE_CACHE */
#ifdef DEBUG
  char *name;
#endif /* DEBUG */
  JSValue eprop[JSOBJECT_MIN_EMBEDDED];
} JSObject;

#ifdef USE_REGEXP
#define is_jsobject(p)                                                  \
  (is_simple_object(p) || is_function(p) || is_builtin(p) || is_array(p) || \
   is_string_object(p) || is_number_object(p) || is_boolean_object(p) || \
   is_regexp(p))
#else /* USE_REGEXP */
#define is_jsobject(p)                                                  \
  (is_simple_object(p) || is_function(p) || is_builtin(p) || is_array(p) || \
   is_string_object(p) || is_number_object(p) || is_boolean_object(p))
#endif /* USE_REGEXP */

static inline JSObject *remove_jsobject_tag(JSValue obj)
{
  assert(is_jsobject(obj));
  return (JSObject *) clear_tag(obj);
}

static inline JSValue *object_get_prop_address(JSValue obj, int index)
{
  JSObject *p;
  int n_embedded;

  p = remove_jsobject_tag(obj);
  n_embedded = p->shape->n_embedded_slots;
  if (index < n_embedded - 1 || p->shape->n_extension_slots == 0)
    return &p->eprop[index];
  else {
    JSValue *extension = (JSValue *) p->eprop[n_embedded - 1];
    return &extension[index - (n_embedded - 1)];
  }
}

#define object_get_prop(obj, index) *object_get_prop_address(obj, index)
#define object_set_prop(obj, index, v) \
  *(object_get_prop_address(obj, index)) = v

#define object_get_shape(obj) (remove_jsobject_tag(obj)->shape)



/** SPECIAL FIELDS OF JSObjects **/

#define jsobject_xprop(p,t,i) (*(t*)&(p)->eprop[i])

/* Simple */

#define put_simple_object_tag(p) put_normal_simple_object_tag(p)
#define OBJECT_SPECIAL_PROPS 0

/* Array */

#define put_array_tag(p) put_normal_array_tag(p)
#define ARRAY_SPECIAL_PROPS 3
#define array_ptr_size(p)          jsobject_xprop(p, uint64_t,  0)
#define array_ptr_length(p)        jsobject_xprop(p, uint64_t,  1)
#define array_ptr_body(p)          jsobject_xprop(p, JSValue *, 2)
#define array_size(o)   array_ptr_size(remove_jsobject_tag(o))
#define array_length(o) array_ptr_length(remove_jsobject_tag(o))
#define array_body(o)   array_ptr_body(remove_jsobject_tag(o))


#define ASIZE_INIT   10       /* default initial size of the C array */
#define ASIZE_DELTA  10       /* delta when expanding the C array */
#define ASIZE_LIMIT  100      /* limit size of the C array */
#define MAX_ARRAY_LENGTH  ((uint64_t)(0xffffffff))
#define increase_asize(n)     (((n) >= ASIZE_LIMIT)? (n): ((n) + ASIZE_DELTA))
#define MINIMUM_ARRAY_SIZE  100

/* Function */

#define put_function_tag(p) put_normal_function_tag(p)
#define FUNCTION_SPECIAL_PROPS 2
#define function_ptr_table_entry(p) jsobject_xprop(p, FunctionTable *, 0)
#define function_ptr_environment(p) jsobject_xprop(p, FunctionFrame *, 1)
#define function_table_entry(o) function_ptr_table_entry(remove_jsobject_tag(o))
#define function_environment(o) function_ptr_environment(remove_jsobject_tag(o))

/* Builtin */

typedef void (*builtin_function_t)(Context*, int, int);

#define put_builtin_tag(p) put_normal_builtin_tag(p)
#define BUILTIN_SPECIAL_PROPS 3
#define builtin_ptr_body(p)        jsobject_xprop(p, builtin_function_t, 0)
#define builtin_ptr_constructor(p) jsobject_xprop(p, builtin_function_t, 1)
#define builtin_ptr_n_args(p)      jsobject_xprop(p, uint64_t,           2)
#define builtin_body(o)        builtin_ptr_body(remove_jsobject_tag(o))
#define builtin_constructor(o) builtin_ptr_constructor(remove_jsobject_tag(o))
#define builtin_n_args(o)      builtin_ptr_n_args(remove_jsobject_tag(o))

#ifdef USE_REGEXP
/* Regexp */

#include <oniguruma.h>

#define put_regexp_tag(p) put_normal_regexp_tag(p)
#define REX_SPECIAL_PROPS 6
#define regexp_ptr_pattern(p)    jsobject_xprop(p, char*,    0)
#define regexp_ptr_reg(p)        jsobject_xprop(p, regex_t*, 1)
#define regexp_ptr_global(p)     jsobject_xprop(p, int,      2)
#define regexp_ptr_ignorecase(p) jsobject_xprop(p, int,      3)
#define regexp_ptr_multiline(p)  jsobject_xprop(p, int,      4)
#define regexp_ptr_lastindex(p)  jsobject_xprop(p, int,      5)
#define regexp_pattern(o)    regexp_ptr_pattern(remove_jsobject_tag(o))
#define regexp_reg(o)        regexp_ptr_reg(remove_jsobject_tag(o))
#define regexp_global(o)     regexp_ptr_global(remove_jsobject_tag(o))
#define regexp_ignorecase(o) regexp_ptr_ignorecase(remove_jsobject_tag(o))
#define regexp_multiline(o)  regexp_ptr_multiline(remove_jsobject_tag(o))
#define regexp_lastindex(o)  regexp_ptr_lastindex(remove_jsobject_tag(o))

#define F_REGEXP_NONE      (0x0)
#define F_REGEXP_GLOBAL    (0x1)
#define F_REGEXP_IGNORE    (0x2)
#define F_REGEXP_MULTILINE (0x4)
#endif /* USE_REGEXP */

/* String */

#define put_string_object_tag(p) put_normal_string_object_tag(p)
#define STRING_SPECIAL_PROPS 1
#define string_object_ptr_value(p) jsobject_xprop(p, JSValue, 0)
#define string_object_value(o) string_object_ptr_value(remove_jsobject_tag(o))

/* Number */

#define put_number_object_tag(p) put_normal_number_object_tag(p)
#define NUMBER_SPECIAL_PROPS 1
#define number_object_ptr_value(p) jsobject_xprop(p, JSValue, 0)
#define number_object_value(o) number_object_ptr_value(remove_jsobject_tag(o))


/* Boolean */

#define put_boolean_object_tag(p) put_normal_boolean_object_tag(p)
#define BOOLEAN_SPECIAL_PROPS 1
#define boolean_object_ptr_value(p) jsobject_xprop(p, JSValue, 0)
#define boolean_object_value(o) boolean_object_ptr_value(remove_jsobject_tag(o))

/** Internal Object ******************************************/

/*
 * Iterator
 * tag == T_GENERIC
 */
typedef struct iterator {
  uint64_t size;        /* array size */
  uint64_t index;       /* array index */
  JSValue *body;        /* pointer to a C array */
} Iterator;

#define make_iterator()                                 \
  (put_normal_iterator_tag(allocate_iterator()))
#define iterator_size(i)                        \
  ((remove_normal_iterator_tag(i))->size)
#define iterator_index(i)                       \
  ((remove_normal_iterator_tag(i))->index)
#define iterator_body(i)                        \
  ((remove_normal_iterator_tag(i))->body)
#define iterator_body_index(a,i)                \
  ((remove_normal_iterator_tag(a))->body[i])


/*
 * Flonum
 */
#define flonum_value(p)      (normal_flonum_value(p))
#define double_to_flonum(n)  (double_to_normal_flonum(n))
#define int_to_flonum(i)     (int_to_normal_flonum(i))
#define cint_to_flonum(i)    (cint_to_normal_flonum(i))
#define flonum_to_double(p)  (normal_flonum_to_double(p))
#define flonum_to_cint(p)    (normal_flonum_to_cint(p))
#define flonum_to_int(p)     (normal_flonum_to_int(p))
#define is_nan(p)            (normal_flonum_is_nan(p))

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

/*
 * String
 */
#define string_value(p)   (normal_string_value(p))
#define string_hash(p)    (normal_string_hash(p))
#define string_length(p)  (normal_string_length(p))
#define cstr_to_string(ctx,str) (cstr_to_normal_string((ctx),(str)))
#define ejs_string_concat(ctx,str1,str2)                \
  (ejs_normal_string_concat((ctx),(str1),(str2)))

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

/** UNBOXED TYPE *****************************************************/

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
#define JS_EMPTY          make_special(2, T_OTHER)

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

#define set_obj_cstr_prop(c, o, s, v, attr)                             \
  set_prop_with_attribute(c, o, cstr_to_string((c),(s)), v, attr)
#define set_obj_cstr_prop_none(c, o, s, v)      \
  set_obj_cstr_prop(c, o, s, v, ATTR_NONE)

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */

#endif /* TYPES_H_ */
