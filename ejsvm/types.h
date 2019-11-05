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

#include <limits.h>

/*
 * Struct type declaration
 */
/*
 * JS Type
 */
/* types.h */
typedef struct jsobject_cell JSObject;
typedef struct iterator Iterator;
typedef struct string_cell StringCell;
typedef struct flonum_cell FlonumCell;
/*
 * no-JS type
 */
/* types.h */
typedef struct property_map PropertyMap;
typedef struct property_map_list PropertyMapList;
typedef struct shape Shape;
/* context.h */
typedef struct function_frame FunctionFrame;
typedef struct context Context;
typedef struct unwind_protect UnwindProtect;
/* hash.h */
typedef struct hash_table HashTable;
/*
 * no-heap type
 */
/* context.h */
typedef struct function_table FunctionTable;
#ifdef ALLOC_SITE_CACHE
typedef struct alloc_site AllocSite;
#endif /* ALLOC_SITE_CACHE */
#ifdef INLINE_CACHE
typedef struct inline_cache InlineCache;
#endif /* INLINE_CACHE */
/* instruction.h */
typedef struct instruction    Instruction;


#ifdef USE_TYPES_GENERATED
#include "types-generated.h"
#else /* USE_TYPES_GENERATED */
#include "types-handcraft.h"
#endif /* USE_TYPES_GENERATED */


/* Cell types
 *   0        : CELLT_FEEE
 *   1 - 0x10 : HTAG (JSValue)
 *   0x11 -   : others
 */
typedef enum cell_type_t {
  CELLT_STRING        = HTAGV_STRING,
  CELLT_FLONUM        = HTAGV_FLONUM,
  CELLT_SIMPLE_OBJECT = HTAGV_SIMPLE_OBJECT,
  CELLT_ARRAY         = HTAGV_ARRAY,
  CELLT_FUNCTION      = HTAGV_FUNCTION,
  CELLT_BUILTIN       = HTAGV_BUILTIN,
  CELLT_ITERATOR      = HTAGV_ITERATOR,
#ifdef USE_REGEXP
  CELLT_REGEXP        = HTAGV_REGEXP,
#endif
  CELLT_BOXED_STRING  = HTAGV_BOXED_STRING,
  CELLT_BOXED_NUMBER  = HTAGV_BOXED_NUMBER,
  CELLT_BOXED_BOOLEAN = HTAGV_BOXED_BOOLEAN,

  CELLT_PROP          = 0x11, /* Array of JSValues */
  CELLT_ARRAY_DATA    = 0x12, /* Array of JSValues */
  CELLT_FUNCTION_FRAME= 0x13, /* FunctionFrame */
  CELLT_STR_CONS      = 0x14, /* StrCons */
  CELLT_CONTEXT       = 0x15, /* Context */
  CELLT_STACK         = 0x16, /* Array of JSValues */
#ifdef HIDDEN_CLASS
  CELLT_HIDDEN_CLASS  = 0x17, /* HiddenClass */
#endif
  CELLT_HASHTABLE     = 0x18,
  CELLT_HASH_BODY     = 0x19,
  CELLT_HASH_CELL     = 0x1A,
  CELLT_PROPERTY_MAP  = 0x1B,
  CELLT_SHAPE         = 0x1C,
  CELLT_UNWIND        = 0x1D,
  CELLT_PROPERTY_MAP_LIST = 0x1E
} cell_type_t;

/*
 * Heap Data
 *   Compound data allocated in the heap.  Listed only those that
 *   can be stored in a JSValue slot.
 */
#define VMHeapData_LIST                                                 \
VMHeapData(extension_prop, CELLT_PROP,           JSValue)               \
VMHeapData(array_data,     CELLT_ARRAY_DATA,     JSValue)               \
VMHeapData(function_frame, CELLT_FUNCTION_FRAME, FunctionFrame)         \
VMHeapData(property_map,   CELLT_PROPERTY_MAP,   struct property_map)

/* JSValue
 *
 * First-class data in JavaScript is represented as a JSValue.
 *
 * JSValue is either a pointer with a pointer tag (ptag) or an
 * immediate value with a ptag.
 *
 *  ---------------------------------------------------
 *  |  pointer / immediate value                 |ptag|
 *  ---------------------------------------------------
 *
 * The size of JSValue and bits for ptag depend on the configuration.
 * JSValue would be 64 or 32 bits depending on the architechture,
 * and ptag is 2 or 3 bits depending on object alignment.
 *
 * We have some related types.
 *   - uintjsv_t, intjsv_t:
 *     Integer types that have the same size as JSValue.  Arithmetic
 *     operations to deal with JSValue, such as tag operations, should
 *     be done on this type.
 *   - cuint, cint:
 *     Integers compatible to fixnums.  Any arithmetic operations
 *     are allowed on this type.  To support 32-bit bitwise operaions
 *     of JavaScript, these types should have at least 32 bits.
 *
 * There are some invariants:
 *   - sizeof(JSValue) >= sizeof(void*)
 *   - sizeof(cint) >= sizeof(JSValue)
 *   - sizeof(cint) >= 32
 *
 *
 *
 * Enable compile-time and runtime type check.
 *
 *   Tag operations and conversion between JSValue and otehr types
 *   are implemented in the way that strict type check works.
 *   We use inline functions to enable compile time type check, and
 *   `assert' statements to check the datatypes of JSValues.
 */

#ifdef BIT_ALIGN32
#define TAGOFFSET 2
#else /* BIT_ALIGN32 */
#define TAGOFFSET 3
#endif /* BIT_ALIGN32 */

#define TAGMASK   ((((uintjsv_t) 1) << TAGOFFSET) - 1)

#ifdef BIT_JSVALUE32
#define LOG_BYTES_IN_JSVALUE 2
typedef uint32_t JSValue;
typedef uint32_t uintjsv_t;
typedef int32_t intjsv_t;
typedef int32_t cint;
typedef uint32_t cuint;
#define PRIJSValue "08"PRIx32
#else /* BIT_JSVALUE32 */
#define LOG_BYTES_IN_JSVALUE 3
typedef uint64_t JSValue;
typedef uint64_t uintjsv_t;
typedef int64_t intjsv_t;
typedef int64_t cint;
typedef uint64_t cuint;
#define PRIJSValue "016"PRIx64
#endif /* BIT_JSVALUE32 */

#define LOG_BITS_IN_JSVALUE  (LOG_BYTES_IN_JSVALUE + 3)
#define BYTES_IN_JSVALUE     (1 << LOG_BYTES_IN_JSVALUE)
#define BITS_IN_JSVALUE      (1 << LOG_BITS_IN_JSVALUE)

/*
 * Tag operations
 */

typedef struct {
  uintjsv_t v: TAGOFFSET;
} PTag;

static inline PTag get_ptag(JSValue v);
static inline int  is_ptag(JSValue v, PTag t);
static inline JSValue put_ptag(uintjsv_t x, PTag t);
/* This macro is used to create a constant.  `x' should be a constant. */
#define PUT_PTAG_CONSTANT(x, tv)  ((JSValue) ((x) | (tv)))
static inline uintjsv_t clear_ptag(JSValue v);

typedef struct {
  cell_type_t v;
} HTag;

static inline HTag get_htag(JSValue v);
static inline int  is_htag(JSValue v, HTag t);

/* Type conversion from/to JSValue
 *   JSValue -> JSObject             jsv_to_jsobject -- check and clear tag
 *   JSValue -> JavaScript object    jsv_to_RT       -- check and clear tag
 *   JSValue -> no-JS heap ptr       jsv_to_T        -- check HTag
 *   JSValue -> no-heap ptr          (T) jsv_to_noheap_ptr
 *   JSValue -> uintjsv_t/intjsv_t   explicit cast
 *   JSValue -> other no-JS value    explicit cast through uintjsv_t/intjsv_t
 *   Fixnum  -> cint/cuint           defined separately
 *
 *   JSValue <- JSObject             ptr_to_RT -- put PTag
 *   JSValue <- JavaScript object    ptr_to_RT -- put PTag
 *   JSValue <- no-JS heap ptr       explicit cast
 *   JSValue <- no-heap ptr          explicit cast
 *   JSValue <- uintjsv_t/intjsv_t   explicit cast
 *   JSValue <- other no-JS value    explicit cast
 *   Fixnum  <- cint/cuint           defined separately
 */

struct jsobject_cell;
static inline struct jsobject_cell *jsv_to_jsobject(JSValue v);

/* jsv_to_RT for JavaScript objects */
#define VMRepType(RT, ptag, S)                  \
static inline S *jsv_to_##RT(JSValue v);
VMRepType_LIST /* defined in types-generated/handcraft.h */
#undef VMRepType

/* jsv_to_T for no-JS heap data */
#define VMHeapData(name, CELLT, T)              \
static inline T *jsv_to_##name(JSValue v);
VMHeapData_LIST
#undef VMHeapData

static inline void *jsv_to_noheap_ptr(JSValue v);

/* ptr_to_RT for JavaScript objects */
#define VMRepType(RT, ptag, S)                  \
static inline JSValue ptr_to_##RT(S *p);
VMRepType_LIST
#undef VMRepType

/*
 * JavaScript Object Definition
 */

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

struct property_map {
  HashTable *map;            /* [const] property map and transitions */
  PropertyMap *prev;         /* [weak] pointer to the previous map */
  Shape *shapes;             /* Weak list of existing shapes arranged from
                              * more specialised to less.
                              * (this pointer is strong) */
  uint16_t n_props;          /* [const] Number of properties in map.
                              * This number includes special props. */
  uint8_t n_special_props;   /* [const] Number of special props. */
#ifdef HC_SKIP_INTERNAL
  uint8_t n_transitions;     /* [const] Number of transitions. Used by GC.
                              * 2 bits (0, 1, more, and UNSURE) would
                              * suffice. */
#define PM_N_TRANS_UNSURE   (1 << 7)
#endif /* HC_SKIP_INTERNAL */
  JSValue   __proto__  __attribute__((aligned(BYTES_IN_JSVALUE)));
                             /* [const] __proto__ of the object. */
#ifdef DEBUG
  char *name;
#endif /* DEBUG */
#ifdef HC_PROF
  uint32_t n_enter;
  uint32_t n_leave;
#endif /* HC_PROF */
};

#ifdef HC_SKIP_INTERNAL
struct property_map_list {
  PropertyMap* pm;
  PropertyMapList *next;
};
#endif /* HC_SKIP_INTERNAL */

struct shape {
  PropertyMap *pm;            /* [const] Pointer to the map. */
  Shape *next;                /* [weak] Weak list of exisnting shapes
                               * shareing the same map. */
  uint16_t n_embedded_slots;  /* [const] Number of slots for properties
                               * in the object. This number includes 
                               * special props. */
  uint16_t n_extension_slots; /* [const] Size of extension array. */
#ifdef DEBUG
  char *name;
#endif /* DEBUG */
#ifdef HC_PROF
  uint32_t n_enter;
  uint32_t n_exit;
  uint32_t is_dead;
  uint32_t is_printed;
#endif /* HC_PROF */
};

struct jsobject_cell {
  Shape *shape;
#ifdef ALLOC_SITE_CACHE
  AllocSite *alloc_site;
#endif /* ALLOC_SITE_CACHE */
#ifdef DEBUG
  char *name;
#endif /* DEBUG */
  JSValue eprop[] __attribute__((aligned(BYTES_IN_JSVALUE)));
};

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

static inline JSValue *object_get_prop_address(JSValue obj, int index)
{
  JSObject *p;
  int n_embedded;

  p = jsv_to_jsobject(obj);
  n_embedded = p->shape->n_embedded_slots;
  if (index < n_embedded - 1 || p->shape->n_extension_slots == 0)
    return &p->eprop[index];
  else {
    JSValue *extension = jsv_to_extension_prop(p->eprop[n_embedded - 1]);
    return &extension[index - (n_embedded - 1)];
  }
}

#define object_get_prop(obj, index) *object_get_prop_address(obj, index)
#define object_set_prop(obj, index, v) \
  *(object_get_prop_address(obj, index)) = v

#define object_get_shape(obj) (jsv_to_jsobject(obj)->shape)
#ifdef ALLOC_SITE_CACHE
#define object_set_alloc_site(obj, as)          \
  (jsv_to_jsobject(obj)->alloc_site = (as))
#endif /* ALLOC_SITE_CACHE */

/** SPECIAL FIELDS OF JSObjects
 *  get/set_jsxxx_yyy:
 *     Accessors that take JSValue of datatype xxx. Normally, these
 *     accessors should be used.
 *  get/set_xxx_ptr_yyy:
 *     Accessors that take JSObject. These accessors are intended
 *     to be used to pointers that have not gotten equipped with PTags,
 *     typically during object initialisation.
 */

#define DEFINE_COMMON_ACCESSORS(OT, index, FT, field)           \
static inline FT get_js##OT##_##field(JSValue v)                \
{                                                               \
  JSObject *p;                                                  \
  assert(is_##OT(v));                                           \
  p = jsv_to_jsobject(v);                                       \
  return get_##OT##_ptr_##field(p);                             \
}                                                               \
static inline void set_js##OT##_##field(JSValue v, FT val)      \
{                                                               \
  JSObject *p;                                                  \
  assert(is_##OT(v));                                           \
  p = jsv_to_jsobject(v);                                       \
  set_##OT##_ptr_##field(p, val);                               \
}

/* for JSValues */
#define DEFINE_ACCESSORS_J(OT, index, field)    \
  DEFINE_ACCESSORS_I(OT, index, JSValue, field)

/* for pointers (references) to (no-JS) heap object  */
#define DEFINE_ACCESSORS_R(OT, index, FT, field, Tname)         \
static inline FT get_##OT##_ptr_##field(JSObject *p)            \
{                                                               \
  return (FT) jsv_to_##Tname(p->eprop[index]);                  \
}                                                               \
static inline void set_##OT##_ptr_##field(JSObject *p, FT val)  \
{                                                               \
  p->eprop[index] = (JSValue) (uintjsv_t) (uintptr_t) val;      \
}                                                               \
DEFINE_COMMON_ACCESSORS(OT, index, FT, field)

/* for no-heap pointers */
#define DEFINE_ACCESSORS_P(OT, index, FT, field)                \
static inline FT get_##OT##_ptr_##field(JSObject *p)            \
{                                                               \
 return (FT) jsv_to_noheap_ptr(p->eprop[index]);                \
}                                                               \
static inline void set_##OT##_ptr_##field(JSObject *p, FT val)  \
{                                                               \
  p->eprop[index] = (JSValue) (uintjsv_t) (uintptr_t) val;      \
}                                                               \
DEFINE_COMMON_ACCESSORS(OT, index, FT, field)

/* for integers */
#define DEFINE_ACCESSORS_I(OT, index, FT, field)                \
static inline FT get_##OT##_ptr_##field(JSObject *p)            \
{                                                               \
  return (FT) (uintjsv_t) p->eprop[index];                      \
}                                                               \
static inline void set_##OT##_ptr_##field(JSObject *p, FT val)  \
{                                                               \
  p->eprop[index] = (JSValue) (uintjsv_t) val;                  \
}                                                               \
DEFINE_COMMON_ACCESSORS(OT, index, FT, field)

/* Simple */
#define OBJECT_SPECIAL_PROPS 0

/* Array
 *   The length of array ranges up to 2^32-1 by specification.
 *   If sizeof(uintjsv_t) < 32, size and length fields may overflow.
 */
#define ARRAY_SPECIAL_PROPS 3
DEFINE_ACCESSORS_I(array, 0, uintjsv_t, size)
DEFINE_ACCESSORS_I(array, 1, uintjsv_t, length)
DEFINE_ACCESSORS_R(array, 2, JSValue *, body, array_data)

#define ASIZE_INIT   10       /* default initial size of the C array */
#define ASIZE_DELTA  10       /* delta when expanding the C array */
#define ASIZE_LIMIT  100      /* limit size of the C array */
#define MAX_ARRAY_LENGTH  ((uintjsv_t)(0xffffffff))
#define increase_asize(n)     (((n) >= ASIZE_LIMIT)? (n): ((n) + ASIZE_DELTA))
#define MINIMUM_ARRAY_SIZE  100

/* Function */
#define FUNCTION_SPECIAL_PROPS 2
DEFINE_ACCESSORS_P(function, 0, FunctionTable*, table_entry)
DEFINE_ACCESSORS_R(function, 1, FunctionFrame*, environment, function_frame)

/* Builtin */
typedef void (*builtin_function_t)(struct context*, int, int);
#define BUILTIN_SPECIAL_PROPS 3
DEFINE_ACCESSORS_P(builtin, 0, builtin_function_t, body)
DEFINE_ACCESSORS_P(builtin, 1, builtin_function_t, constructor)
DEFINE_ACCESSORS_I(builtin, 2, uintjsv_t, nargs)

#ifdef USE_REGEXP
/* Regexp */

#include <oniguruma.h>

#define REX_SPECIAL_PROPS 6
DEFINE_ACCESSORS_P(regexp, 0, char*, pattern)
DEFINE_ACCESSORS_I(regexp, 1, intjsv_t, reg)
DEFINE_ACCESSORS_I(regexp, 2, intjsv_t, global)
DEFINE_ACCESSORS_I(regexp, 3, intjsv_t, ignorecase)
DEFINE_ACCESSORS_I(regexp, 4, intjsv_t, multiline)
DEFINE_ACCESSORS_I(regexp, 5, intjsv_t, lastindex)

#define F_REGEXP_NONE      (0x0)
#define F_REGEXP_GLOBAL    (0x1)
#define F_REGEXP_IGNORE    (0x2)
#define F_REGEXP_MULTILINE (0x4)
#endif /* USE_REGEXP */

/* String */
#define STRING_SPECIAL_PROPS 1
DEFINE_ACCESSORS_J(string_object, 0, value)

/* Number */
#define NUMBER_SPECIAL_PROPS 1
DEFINE_ACCESSORS_J(number_object, 0, value)

/* Boolean */
#define BOOLEAN_SPECIAL_PROPS 1
DEFINE_ACCESSORS_J(boolean_object, 0, value)

#undef DEFINE_ACCESSORS

/** Primitive Heap-allocated Object ************************************/

#define DEFINE_GETTER(RT, S, FT, field)                         \
static inline FT get_##RT##_ptr_##field(S *p)                   \
{                                                               \
  return p->field;                                              \
}                                                               \
static inline FT get_js##RT##_##field(JSValue v)                \
{                                                               \
  S *p = (S *) jsv_to_##RT(v);                                  \
  return get_##RT##_ptr_##field(p);                             \
}                                                               \

#define DEFINE_SETTER(RT, S, FT, field)                         \
static inline void set_##RT##_ptr_##field(S *p, FT val)         \
{                                                               \
  p->field = val;                                               \
}                                                               \
static inline void set_js##RT##_##field(JSValue v, FT val)      \
{                                                               \
  S *p = (S *) jsv_to_##RT(v);                                  \
  set_##RT##_ptr_##field(p, val);                               \
}

#define DEFINE_ACCESSORS(RT, S, FT, field)                      \
  DEFINE_GETTER(RT, S, FT, field)                               \
  DEFINE_SETTER(RT, S, FT, field)

/* Iterator VMDataType interface
 *   No inteface is defined as Iterator always has a single VMRepType.
 */

/* Iterator */

struct iterator {
  uint16_t size;        /* array size */
  uint16_t index;       /* array index */
  JSValue *body;        /* pointer to a C array */
};

#define make_iterator(ctx) (put_iterator_tag(allocate_iterator(ctx)))

DEFINE_ACCESSORS(normal_iterator, Iterator, uint16_t, size)
DEFINE_ACCESSORS(normal_iterator, Iterator, uint16_t, index)
DEFINE_ACCESSORS(normal_iterator, Iterator, JSValue*, body)

/* 
 * Flonum VMDataType interface
 */

#define flonum_value(p)           (get_jsnormal_flonum_value(p))
#define double_to_flonum(ctx, n)  (double_to_normal_flonum(ctx, n))
#define int_to_flonum(i)          (int_to_normal_flonum(i))
#define cint_to_flonum(ctx, i)    (cint_to_normal_flonum(ctx, i))
#define flonum_to_double(p)       (normal_flonum_to_double(p))
#define flonum_to_cint(p)         (normal_flonum_to_cint(p))
#define flonum_to_int(p)          (normal_flonum_to_int(p))
#define is_nan(p)                 (normal_flonum_is_nan(p))

/* Normal Flonum */
struct flonum_cell {
  double value;        /* immutable */
};

DEFINE_ACCESSORS(normal_flonum, FlonumCell, double, value)

#define double_to_normal_flonum(ctx, n)                 \
  (ptr_to_normal_flonum(allocate_flonum(ctx, n)))
#define int_to_normal_flonum(ctx, i)     cint_to_flonum(ctx, i)
#define cint_to_normal_flonum(ctx, i)    double_to_flonum(ctx, (double)(i))
#define normal_flonum_to_double(p)  flonum_value(p)
#define normal_flonum_to_cint(p)    ((cint)(flonum_value(p)))
#define normal_flonum_to_int(p)     ((int)(flonum_value(p)))
static inline int normal_flonum_is_nan(JSValue v)
{
  if (is_flonum(v))
    return isnan(flonum_to_double(v));
  else
    return 0;
}

/*
 * String VMDataType Interface
 */
#define string_value(p)   (get_jsnormal_string_value(p))
#define string_to_cstr(p) (string_value(p))
#define string_hash(p)    (normal_string_hash(p))
#define string_length(p)  (normal_string_length(p))
#define cstr_to_string(ctx,str) (cstr_to_normal_string((ctx),(str)))
#define ejs_string_concat(ctx,str1,str2)                \
  (ejs_normal_string_concat((ctx),(str1),(str2)))

/* Normal String */
struct string_cell {
#ifdef STROBJ_HAS_HASH
  uint32_t hash;           /* hash value before computing mod */
  uint32_t length;         /* length of the string */
#endif /* STROBJ_HAS_HASH */
  char value[BYTES_IN_JSVALUE];
};

#ifdef STROBJ_HAS_HASH
DEFINE_ACCESSORS(normal_string, StringCell, uint32_t, hash)
DEFINE_ACCESSORS(normal_string, StringCell, uint32_t, length)
#endif /* STROBJ_HAS_HASH */
DEFINE_GETTER(normal_string, StringCell, char*, value)

#ifdef STROBJ_HAS_HASH
#define normal_string_hash(p)     (get_jsnormal_string_hash(p))
#define normal_string_length(p)   (get_jsnormal_string_length(p))
#else
#define normal_string_hash(p)     (calc_hash(get_jsnormal_string_value(p)))
#define normal_string_length(p)   ((uint32_t) strlen(string_value(p)))
#endif /* STROBJ_HAS_HASH */

#define cstr_to_normal_string(ctx, str)  (cstr_to_string_ool((ctx), (str)))
/* TODO: give a nice name to ejs_string_concat
 *       (string_concat is used for builtin function) */
#define ejs_normal_string_concat(ctx, str1, str2)       \
  (string_concat_ool((ctx), (str1), (str2)))


#undef DEFINE_GETTER
#undef DEFINE_SETTER
#undef DEFINE_ACCESSORS

/** UNBOXED TYPE *****************************************************/

/*
 * Fixnum
 */
#define BITS_IN_FIXNUM  (BITS_IN_JSVALUE - TAGOFFSET)
#define MAX_FIXNUM_CINT ((((cint) 1) << (BITS_IN_FIXNUM - 1)) - 1)
#define MIN_FIXNUM_CINT (-MAX_FIXNUM_CINT-1)
#define is_fixnum_range_cint(n)                                 \
  ((MIN_FIXNUM_CINT <= (n)) && ((n) <= MAX_FIXNUM_CINT))

static inline cint fixnum_to_cint(JSValue v)
{
  assert(is_fixnum(v));
  return (cint) (((intjsv_t) v) >> TAGOFFSET);
}

#define fixnum_to_double(v) ((double) fixnum_to_cint((v)))


/* Convert a cint value to Fixnum.  The value sould be guaranteed
 * that it is small enough to fit fixnum.
 * 
 * In general, cint may contain an intenger that is out of range of
 * fixnum. cint_to_fixnum_nocheck should be used in limited caess
 * where the value is guaranteed to be small.
 */
static inline JSValue small_cint_to_fixnum(cint n);

#define is_integer_value_double(d) ((d) == (double)((cint)(d)))

#define is_fixnum_range_double(d)                                       \
  (is_integer_value_double(d) && is_fixnum_range_cint((cint)(d)))

#define HALF_BITS_IN_FIXNUM (BITS_IN_FIXNUM >> 1)
#define half_fixnum_range(x)                            \
  ((-(1 << (HALF_BITS_IN_FIXNUM - 1)) <= (x)) &&        \
   ((x) < (1 << (HALF_BITS_IN_FIXNUM - 1))))

#define FIXNUM_MINUS_ONE (small_cint_to_fixnum((cint)-1))
#define FIXNUM_ZERO      (small_cint_to_fixnum((cint)0))
#define FIXNUM_ONE       (small_cint_to_fixnum((cint)1))
#define FIXNUM_TEN       (small_cint_to_fixnum((cint)10))


static inline JSValue cint_to_number(Context *ctx, cint n);

#if BITS_IN_FIXNUM >= 32
#define int32_to_number(ctx, n) (small_cint_to_fixnum((cint) (n)))
#define uint32_to_number(ctx, n) (small_cint_to_fixnum((cint) (n)))
#else /* FIXNUM SIZE */
#define int32_to_number(ctx, n) (cint_to_number((ctx), (cint) (n)))
#define uint32_to_number(ctx, n) (cint_to_number((ctx), (cint) (n)))
#endif /* FIXNUM SIZE */

static inline double number_to_double(JSValue v);
static inline JSValue double_to_number(Context *ctx, double n);

/*
 * Special
 */
#define SPECIAL_TAG_BITS  1
/* We use TV_SPECIAL because T_SPECIAL.v is not regarded as a constant. */
#define MAKE_SPECIAL(val, special_tag)                          \
  PUT_PTAG_CONSTANT((val << (TAGOFFSET + SPECIAL_TAG_BITS)) |   \
                    (special_tag << TAGOFFSET),                 \
                    TV_SPECIAL)

/* Boolean */
#define SPECIAL_TAG_BOOLEAN  1
#define JS_TRUE           MAKE_SPECIAL(1, SPECIAL_TAG_BOOLEAN)
#define JS_FALSE          MAKE_SPECIAL(0, SPECIAL_TAG_BOOLEAN)

static inline int is_boolean(JSValue v)
{
  const uintjsv_t TAGS_MASK =
    (((uintjsv_t) 1) << (TAGOFFSET + SPECIAL_TAG_BITS)) - 1;
  uintjsv_t vtag = ((uintjsv_t) v) & TAGS_MASK;
  return vtag == ((SPECIAL_TAG_BOOLEAN << TAGOFFSET) | T_SPECIAL.v);
}
static inline int is_true(JSValue v)
{
  return v == JS_TRUE;
}
static inline int is_false(JSValue v)
{
  return v == JS_FALSE;
}
#define int_to_boolean(e) ((e) ? JS_TRUE : JS_FALSE)
#define true_false(e)     ((e) ? JS_TRUE : JS_FALSE)
#define false_true(e)     ((e) ? JS_FALSE : JS_TRUE)

/* Other */
#define SPECIAL_TAG_OTHER    0
#define JS_NULL           MAKE_SPECIAL(0, SPECIAL_TAG_OTHER)
#define JS_UNDEFINED      MAKE_SPECIAL(1, SPECIAL_TAG_OTHER)
#define JS_EMPTY          MAKE_SPECIAL(2, SPECIAL_TAG_OTHER)

#define is_null(p)               ((p) == JS_NULL)
#define is_undefined(p)          ((p) == JS_UNDEFINED)

/*
 * Primitive is either number, boolean, or string.
 */
#define is_primitive(p) (!is_object(p) && !is_null(p) && !is_undefined(p))

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
