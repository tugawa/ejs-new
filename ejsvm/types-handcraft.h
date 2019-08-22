/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

/*
 * Objects
 */
#define T_GENERIC (0x0)  /* 000 */
#define T_UNUSED0 (0x1)  /* 001 */
#define T_UNUSED1 (0x2)  /* 010 */
#define T_UNUSED2 (0x3)  /* 011 */

/*
  Constant
*/
#define T_STRING  (0x4)  /* 100 */
#define T_FLONUM  (0x5)  /* 101 */
#define T_SPECIAL (0x6)  /* 110 */
#define T_FIXNUM  (0x7)  /* 111 */

/*
 * is_object checks whether `p' is one of the object families,
 * i.e., simple object, array, function, builtin, iterator, regexp,
 * boxed string, boxed number, boxed boolean.
 * Note that is_object does not investigate the header tag.
 */
#define is_object(p)             (equal_tag((p), T_GENERIC))
#define is_number(p)             (is_fixnum((p)) || is_flonum((p)))

#define is_simple_object(p)      is_obj_header_tag((p), HTAG_SIMPLE_OBJECT)
#define is_array(p)              is_obj_header_tag((p), HTAG_ARRAY)
#define is_function(p)           is_obj_header_tag((p), HTAG_FUNCTION)
#define is_builtin(p)            is_obj_header_tag((p), HTAG_BUILTIN)
#define is_iterator(p)           is_obj_header_tag((p), HTAG_ITERATOR)
#define is_regexp(r)             is_obj_header_tag((r), HTAG_REGEXP)
#define is_number_object(p)      is_obj_header_tag((p), HTAG_BOXED_NUMBER)
#define is_boolean_object(p)     is_obj_header_tag((p), HTAG_BOXED_BOOLEAN)
#define is_string_object(p)      is_obj_header_tag((p), HTAG_BOXED_STRING)
#define is_flonum(p)             (equal_tag((p), T_FLONUM))
#define is_string(p)             (equal_tag((p), T_STRING))
#define is_fixnum(p)             (equal_tag((p), T_FIXNUM))
#define is_special(p)            (equal_tag((p), T_SPECIAL))

#define remove_normal_flonum_tag(p)             \
  ((FlonumCell *)remove_tag((p), T_FLONUM))
#define remove_normal_simple_object_tag(p)      \
  ((Object *)  remove_tag((p), T_GENERIC))
#define remove_normal_array_tag(p)              \
  ((ArrayCell *)   remove_tag((p), T_GENERIC))
#define remove_normal_function_tag(p)           \
  ((FunctionCell *)remove_tag((p), T_GENERIC))
#define remove_normal_builtin_tag(p)            \
  ((BuiltinCell *) remove_tag((p), T_GENERIC))
#define remove_normal_iterator_tag(p)           \
  ((Iterator *)remove_tag((p), T_GENERIC))
#define remove_normal_string_tag(p)             \
  ((StringCell *)remove_tag((p), T_STRING))
#define remove_normal_regexp_tag(p)             \
  ((RegexpCell *)  remove_tag((p), T_GENERIC))
#define remove_normal_boolean_object_tag(p)     \
  ((BoxedCell *)remove_tag((p), T_GENERIC))
#define remove_normal_number_object_tag(p)      \
  ((BoxedCell *)remove_tag((p), T_GENERIC))
#define remove_normal_string_object_tag(p)      \
  ((BoxedCell *)remove_tag((p), T_GENERIC))

#define put_normal_simple_object_tag(p)  (put_tag((p), T_GENERIC))
#define put_normal_array_tag(p)          (put_tag((p), T_GENERIC))
#define put_normal_function_tag(p)       (put_tag((p), T_GENERIC))
#define put_normal_builtin_tag(p)        (put_tag((p), T_GENERIC))
#define put_normal_iterator_tag(p)       (put_tag((p), T_GENERIC))
#define put_normal_string_tag(p)         (put_tag((p), T_STRING))
#define put_normal_regexp_tag(p)         (put_tag((p), T_GENERIC))
#define put_normal_flonum_tag(p)         (put_tag((p), T_FLONUM))
#define put_normal_normal_string_tag(p)  (put_tag((p), T_STRING))
#define put_normal_boolean_object_tag(p) (put_tag((p), T_GENERIC))
#define put_normal_number_object_tag(p)  (put_tag((p), T_GENERIC))
#define put_normal_string_object_tag(p)  (put_tag((p), T_GENERIC))

#define HTAG_STRING        (0x4)
#define HTAG_FLONUM        (0x5)
#define HTAG_SIMPLE_OBJECT (0x6)
#define HTAG_ARRAY         (0x7)
#define HTAG_FUNCTION      (0x8)
#define HTAG_BUILTIN       (0x9)
#define HTAG_ITERATOR      (0xa)
#ifdef USE_REGEXP
#define HTAG_REGEXP        (0xb)
#endif
#define HTAG_BOXED_STRING  (0xc)
#define HTAG_BOXED_NUMBER  (0xd)
#define HTAG_BOXED_BOOLEAN (0xe)

#define is_pointer(p)     (((p) & 2) == 0)
#define is_leaf_object(p) (((p) & 6) == 4)

#define need_simple_object 1
#define need_string 1
#define need_boolean_object 1
#define need_special 1
#define need_number_object 1
#define need_string_object 1
#define need_regexp 1
#define need_flonum 1
#define need_builtin 1
#define need_fixnum 1
#define need_array 1
#define need_function 1

#define need_normal_number_object 1
#define need_normal_builtin 1
#define need_normal_boolean_object 1
#define need_normal_array 1
#define need_normal_string_object 1
#define need_normal_regexp 1
#define need_normal_function 1
#define need_normal_fixnum 1
#define need_normal_flonum 1
#define need_normal_special 1
#define need_normal_string 1

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
