#define TV_STRING 4
#define T_STRING ((PTag) {TV_STRING})
#define TV_FLONUM 5
#define T_FLONUM ((PTag) {TV_FLONUM})
#define TV_SPECIAL 6
#define T_SPECIAL ((PTag) {TV_SPECIAL})
#define TV_FIXNUM 7
#define T_FIXNUM ((PTag) {TV_FIXNUM})
#define TV_GENERIC 0
#define T_GENERIC ((PTag) {TV_GENERIC})

#define HTAGV_STRING 4
#define HTAG_STRING ((HTag) {CELLT_STRING})
#define HTAGV_FLONUM 5
#define HTAG_FLONUM ((HTag) {CELLT_FLONUM})
#define HTAGV_SIMPLE_OBJECT 6
#define HTAG_SIMPLE_OBJECT ((HTag) {CELLT_SIMPLE_OBJECT})
#define HTAGV_ARRAY 7
#define HTAG_ARRAY ((HTag) {CELLT_ARRAY})
#define HTAGV_FUNCTION 8
#define HTAG_FUNCTION ((HTag) {CELLT_FUNCTION})
#define HTAGV_BUILTIN 9
#define HTAG_BUILTIN ((HTag) {CELLT_BUILTIN})
#define HTAGV_ITERATOR 10
#define HTAG_ITERATOR ((HTag) {CELLT_ITERATOR})
#define HTAGV_REGEXP 11
#define HTAG_REGEXP ((HTag) {CELLT_REGEXP})
#define HTAGV_BOXED_STRING 12
#define HTAG_BOXED_STRING ((HTag) {CELLT_BOXED_STRING})
#define HTAGV_BOXED_NUMBER 13
#define HTAG_BOXED_NUMBER ((HTag) {CELLT_BOXED_NUMBER})
#define HTAGV_BOXED_BOOLEAN 14
#define HTAG_BOXED_BOOLEAN ((HTag) {CELLT_BOXED_BOOLEAN})

/* VM-DataTypes */
#define is_string(x) (((0) && (0)) || is_ptag((x), T_STRING))
#define is_fixnum(x) (((0) && (0)) || is_ptag((x), T_FIXNUM))
#define is_flonum(x) (((0) && (0)) || is_ptag((x), T_FLONUM))
#define is_special(x) (((0) && (0)) || is_ptag((x), T_SPECIAL))
#define is_simple_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_SIMPLE_OBJECT))))
#define is_array(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_ARRAY))))
#define is_function(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_FUNCTION))))
#define is_builtin(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BUILTIN))))
#define is_iterator(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_ITERATOR))))
#define is_regexp(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_REGEXP))))
#define is_string_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BOXED_STRING))))
#define is_number_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BOXED_NUMBER))))
#define is_boolean_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BOXED_BOOLEAN))))
/* VM-RepTypes */
#define is_normal_string(x) (((0) && (0)) || is_ptag((x), T_STRING))
#define is_normal_fixnum(x) (((0) && (0)) || is_ptag((x), T_FIXNUM))
#define is_normal_flonum(x) (((0) && (0)) || is_ptag((x), T_FLONUM))
#define is_normal_special(x) (((0) && (0)) || is_ptag((x), T_SPECIAL))
#define is_normal_simple_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_SIMPLE_OBJECT))))
#define is_normal_array(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_ARRAY))))
#define is_normal_function(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_FUNCTION))))
#define is_normal_builtin(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BUILTIN))))
#define is_normal_iterator(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_ITERATOR))))
#define is_normal_regexp(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_REGEXP))))
#define is_normal_string_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BOXED_STRING))))
#define is_normal_number_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BOXED_NUMBER))))
#define is_normal_boolean_object(x) (((0 || is_ptag((x), T_GENERIC)) && (0 || is_htag((x), HTAG_BOXED_BOOLEAN))))

#define is_object(x) (((0) && (0)) || is_ptag((x), T_GENERIC))
#define is_number(x) (((0) && (0)) || is_ptag((x), T_FIXNUM) || is_ptag((x), T_FLONUM))
#define has_htag(x) (((0) && (0)) || is_ptag((x), T_FLONUM) || is_ptag((x), T_STRING) || is_ptag((x), T_GENERIC))

/* case label(s) for get_ptag_value_by_cell_type */
#define CASE_LABELS_FOR_get_ptag_value_by_cell_type \
case CELLT_STRING: return TV_STRING;\
case CELLT_FLONUM: return TV_FLONUM;

#define VMRepType_LIST \
VMRepType(normal_simple_object, T_GENERIC, JSObject) \
VMRepType(normal_string, T_STRING, StringCell) \
VMRepType(normal_flonum, T_FLONUM, FlonumCell) \
VMRepType(normal_array, T_GENERIC, JSObject) \
VMRepType(normal_function, T_GENERIC, JSObject) \
VMRepType(normal_builtin, T_GENERIC, JSObject) \
VMRepType(normal_iterator, T_GENERIC, Iterator) \
VMRepType(normal_regexp, T_GENERIC, JSObject) \
VMRepType(normal_string_object, T_GENERIC, JSObject) \
VMRepType(normal_number_object, T_GENERIC, JSObject) \
VMRepType(normal_boolean_object, T_GENERIC, JSObject) \


/* for GC */
#define is_pointer(p)     (((p) & 2) == 0)
#define is_leaf_object(p) (((p) & 6) == 4)


