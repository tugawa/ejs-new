/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */


#ifdef BIT_ALIGN32
#error handcrafted code does not support BIT_ALIGN32
#endif /* BIT_ALIGN32 */

/*
 * Objects
 */
#define TV_GENERIC (0x0)  /* 000 */
#define TV_UNUSED0 (0x1)  /* 001 */
#define TV_UNUSED1 (0x2)  /* 010 */
#define TV_UNUSED2 (0x3)  /* 011 */

/*
 *  Constant
 */
#define TV_STRING  (0x4)  /* 100 */
#define TV_FLONUM  (0x5)  /* 101 */
#define TV_SPECIAL (0x6)  /* 110 */
#define TV_FIXNUM  (0x7)  /* 111 */

/*
 * pointer tag wrap
 */
#define T_GENERIC ((PTag) {TV_GENERIC})
#define T_STRING  ((PTag) {TV_STRING})
#define T_FLONUM  ((PTag) {TV_FLONUM})
#define T_SPECIAL ((PTag) {TV_SPECIAL})
#define T_FIXNUM  ((PTag) {TV_FIXNUM})

/*
 * is_object checks whether `v' is one of the object families,
 * i.e., simple object, array, function, builtin, iterator, regexp,
 * boxed string, boxed number, boxed boolean.
 * Note that is_object does not investigate the header tag.
 */
#define is_object(v)             (is_ptag((v), T_GENERIC))
#define is_number(v)             (is_fixnum((v)) || is_flonum((v)))
#define has_htag(x)              ((get_ptag(x).v & 0x2) == 0)

#define is_obj_header_tag(v,t)   (is_object((v)) && is_htag((v), (t)))

#define is_simple_object(v)      is_obj_header_tag((v), HTAG_SIMPLE_OBJECT)
#define is_array(v)              is_obj_header_tag((v), HTAG_ARRAY)
#define is_function(v)           is_obj_header_tag((v), HTAG_FUNCTION)
#define is_builtin(v)            is_obj_header_tag((v), HTAG_BUILTIN)
#define is_iterator(v)           is_obj_header_tag((v), HTAG_ITERATOR)
#define is_regexp(v)             is_obj_header_tag((v), HTAG_REGEXP)
#define is_number_object(v)      is_obj_header_tag((v), HTAG_BOXED_NUMBER)
#define is_boolean_object(v)     is_obj_header_tag((v), HTAG_BOXED_BOOLEAN)
#define is_string_object(v)      is_obj_header_tag((v), HTAG_BOXED_STRING)
#define is_flonum(v)             (is_ptag((v), T_FLONUM))
#define is_string(v)             (is_ptag((v), T_STRING))
#define is_fixnum(v)             (is_ptag((v), T_FIXNUM))
#define is_special(v)            (is_ptag((v), T_SPECIAL))

#define VMRepType_LIST                                                  \
VMRepType(normal_simple_object,  T_GENERIC, struct jsobject_cell)       \
VMRepType(normal_array,          T_GENERIC, struct jsobject_cell)       \
VMRepType(normal_function,       T_GENERIC, struct jsobject_cell)       \
VMRepType(normal_builtin,        T_GENERIC, struct jsobject_cell)       \
VMRepType(normal_boolean_object, T_GENERIC, struct jsobject_cell)       \
VMRepType(normal_number_object,  T_GENERIC, struct jsobject_cell)       \
VMRepType(normal_string_object,  T_GENERIC, struct jsobject_cell)       \
VMRepType(normal_iterator,       T_GENERIC, struct iterator)            \
VMRepType(normal_string,         T_STRING,  struct string_cell)         \
VMRepType(normal_flonum,         T_FLONUM,  struct flonum_cell)         \
VMRepTypeREGEXP

#ifdef USE_REGEXP
#define VMRepTypeREGEXP                                                  \
VMRepType(normal_regexp,         T_GENERIC, struct jsobject_cell)
#else /* USE_REGEXP */
#define VMRepTypeREGEXP
#endif /* USE_REGEXP */

#define HTAGV_STRING        (0x4)
#define HTAGV_FLONUM        (0x5)
#define HTAGV_SIMPLE_OBJECT (0x6)
#define HTAGV_ARRAY         (0x7)
#define HTAGV_FUNCTION      (0x8)
#define HTAGV_BUILTIN       (0x9)
#define HTAGV_ITERATOR      (0xa)
#ifdef USE_REGEXP
#define HTAGV_REGEXP        (0xb)
#endif
#define HTAGV_BOXED_STRING  (0xc)
#define HTAGV_BOXED_NUMBER  (0xd)
#define HTAGV_BOXED_BOOLEAN (0xe)

#define HTAG_STRING        ((HTag) {HTAGV_STRING})
#define HTAG_FLONUM        ((HTag) {HTAGV_FLONUM})
#define HTAG_SIMPLE_OBJECT ((HTag) {HTAGV_SIMPLE_OBJECT})
#define HTAG_ARRAY         ((HTag) {HTAGV_ARRAY})
#define HTAG_FUNCTION      ((HTag) {HTAGV_FUNCTION})
#define HTAG_BUILTIN       ((HTag) {HTAGV_BUILTIN})
#define HTAG_ITERATOR      ((HTag) {HTAGV_ITERATOR})
#ifdef USE_REGEXP
#define HTAG_REGEXP        ((HTag) {HTAGV_REGEXP})
#endif
#define HTAG_BOXED_STRING  ((HTag) {HTAGV_BOXED_STRING})
#define HTAG_BOXED_NUMBER  ((HTag) {HTAGV_BOXED_NUMBER})
#define HTAG_BOXED_BOOLEAN ((HTag) {HTAGV_BOXED_BOOLEAN})

#define is_normal_simple_object(v)  is_simple_object(v)
#define is_normal_array(v)          is_array(v)
#define is_normal_function(v)       is_function(v)
#define is_normal_builtin(v)        is_builtin(v)
#define is_normal_iterator(v)       is_iterator(v)
#define is_normal_regexp(v)         is_regexp(v)
#define is_normal_number_object(v)  is_number_object(v)
#define is_normal_boolean_object(v) is_boolean_object(v)
#define is_normal_string_object(v)  is_string_object(v)
#define is_normal_flonum(v)         is_flonum(v)
#define is_normal_string(v)         is_string(v)
#define is_normal_fixnum(v)         is_fixnum(v)
#define is_normal_special(v)        is_special(v)

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

/*
 * Pair of two pointer tags
 * Note that the result of TAG_PAIR is of type Tag
 */
#define TAG_PAIR(t1, t2) ((t1) | ((t2) << TAGOFFSET))
#define TAG_PAIR_VARS(v1, v2) (TAG_PAIR(get_ptag(v1).v, get_ptag(v2).v))

#define TP_OBJOBJ TAG_PAIR(TV_GENERIC, TV_GENERIC)
#define TP_OBJSTR TAG_PAIR(TV_GENERIC, TV_STRING)
#define TP_OBJFLO TAG_PAIR(TV_GENERIC, TV_FLONUM)
#define TP_OBJSPE TAG_PAIR(TV_GENERIC, TV_SPECIAL)
#define TP_OBJFIX TAG_PAIR(TV_GENERIC, TV_FIXNUM)
#define TP_STROBJ TAG_PAIR(TV_STRING, TV_GENERIC)
#define TP_STRSTR TAG_PAIR(TV_STRING, TV_STRING)
#define TP_STRFLO TAG_PAIR(TV_STRING, TV_FLONUM)
#define TP_STRSPE TAG_PAIR(TV_STRING, TV_SPECIAL)
#define TP_STRFIX TAG_PAIR(TV_STRING, TV_FIXNUM)
#define TP_FLOOBJ TAG_PAIR(TV_FLONUM, TV_GENERIC)
#define TP_FLOSTR TAG_PAIR(TV_FLONUM, TV_STRING)
#define TP_FLOFLO TAG_PAIR(TV_FLONUM, TV_FLONUM)
#define TP_FLOSPE TAG_PAIR(TV_FLONUM, TV_SPECIAL)
#define TP_FLOFIX TAG_PAIR(TV_FLONUM, TV_FIXNUM)
#define TP_SPEOBJ TAG_PAIR(TV_SPECIAL, TV_GENERIC)
#define TP_SPESTR TAG_PAIR(TV_SPECIAL, TV_STRING)
#define TP_SPEFLO TAG_PAIR(TV_SPECIAL, TV_FLONUM)
#define TP_SPESPE TAG_PAIR(TV_SPECIAL, TV_SPECIAL)
#define TP_SPEFIX TAG_PAIR(TV_SPECIAL, TV_FIXNUM)
#define TP_FIXOBJ TAG_PAIR(TV_FIXNUM, TV_GENERIC)
#define TP_FIXSTR TAG_PAIR(TV_FIXNUM, TV_STRING)
#define TP_FIXFLO TAG_PAIR(TV_FIXNUM, TV_FLONUM)
#define TP_FIXSPE TAG_PAIR(TV_FIXNUM, TV_SPECIAL)
#define TP_FIXFIX TAG_PAIR(TV_FIXNUM, TV_FIXNUM)

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
