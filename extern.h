/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#define FUNCTION_TABLE_LIMIT  (100)

EXTERN FunctionTable function_table[FUNCTION_TABLE_LIMIT];
EXTERN StrTable string_table;

extern int ftable_flag;
extern int trace_flag;
extern int lastprint_flag;
extern int cputime_flag;
extern int repl_flag;
extern int regstack_limit;

#if defined(USE_OBC) && defined(USE_SBC)
extern int obcsbc;
#endif

extern int run_phase;
extern int generation;
extern int gc_sec;
extern int gc_usec;

extern FILE *log_stream;

#ifdef PROFILE
extern int profile_flag;
extern int coverage_flag;
extern int icount_flag;
extern int forcelog_flag;
extern FILE *prof_stream;

extern int logflag_stack[];
extern int logflag_sp;
#define logflag()   (logflag_sp >= 0 ? logflag_stack[logflag_sp] : 0)
#endif

extern InsnInfo insn_info_table[];
extern int numinsts;

#ifdef HIDDEN_CLASS
extern int n_hc;
extern int n_enter_hc;
extern int n_exit_hc;
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * allocate.c
 */
extern FlonumCell *allocate_flonum(double);
extern StringCell *allocate_string(uint32_t);
extern JSValue allocate_string2(Context *ctx, const char *, const char *);
#ifdef EMBED_PROP
#ifdef ARRAY_EMBED_PROP
#define allocate_simple_object(ctx, nemb)        \
  allocate_jsobject((ctx), (nemb), HTAG_SIMPLE_OBJECT)
extern Object *allocate_jsobject(Context *ctx, size_t n_embedded,
                                 cell_type_t htag);
#else /* ARRAY_EMBED_PROP */
extern Object *allocate_simple_object(Context *ctx, size_t n_embedded);
#endif /* ARRAY_EMBED_PROP */
#else /* EMBED_PROP */
extern Object *allocate_simple_object(Context *ctx);
#endif /* EMBED_PROP */
#ifdef ARRAY_EMBED_PROP
#define allocate_array(ctx)                                     \
  allocate_jsobject((ctx), ARRAY_EMBEDDED_PROPS, HTAG_ARRAY)
#define allocate_function(ctx)                                  \
  allocate_jsobject((ctx), FUNC_EMBEDDED_PROPS, HTAG_FUNCTION)
#define allocate_builtin(ctx)                                           \
  allocate_jsobject((ctx), BUILTIN_EMBEDDED_PROPS, HTAG_BUILTIN)
/* allocate_jsobject is called directly in types.h for boxed types */
#define allocate_regexp(ctx)                                    \
  allocate_jsobject((ctx), REX_EMBEDDED_PROPS, HTAG_REGEXP)
#else /* ARRAY_EMBED_PROP */
extern ArrayCell *allocate_array(Context *ctx);
extern FunctionCell *allocate_function(void);
extern BuiltinCell *allocate_builtin(void);
extern BoxedCell *allocate_boxed(Context *,uint32_t);
#ifdef USE_REGEXP
extern RegexpCell *allocate_regexp(void);
#endif
#endif /* ARRAY_EMBED_PROP */
extern void allocate_array_data(Context *, JSValue, int, int);
extern void reallocate_array_data(Context *, JSValue, int);
extern JSValue *allocate_prop_table(int);
extern JSValue *reallocate_prop_table(Context *, JSValue *, int, int);
extern Iterator *allocate_iterator(void);
extern void allocate_iterator_data(Context *, JSValue, int);

#define allocate_array_data_critical(a,s,l)        \
  allocate_array_data(NULL,(a),(s),(l))
/*
 * builtin.c
 */
extern BUILTIN_FUNCTION(builtin_const_true);
extern BUILTIN_FUNCTION(builtin_const_false);
extern BUILTIN_FUNCTION(builtin_const_undefined);
extern BUILTIN_FUNCTION(builtin_const_null);
extern BUILTIN_FUNCTION(builtin_identity);
extern BUILTIN_FUNCTION(builtin_fixnu_to_string);
extern BUILTIN_FUNCTION(builtin_flonum_to_string);
/* extern BUILTIN_FUNCTION(builtin_string_to_index); */

/*
 * builtin-array.c
 */
extern BUILTIN_FUNCTION(array_constr);

/*
 * builtin-boolean.c
 */
extern BUILTIN_FUNCTION(boolean_constr);

/*
 * builtin-global.c
 */
extern BUILTIN_FUNCTION(builtin_not_a_constructor);

/*
 * builtin-number.c
 */
extern BUILTIN_FUNCTION(number_constr);
extern BUILTIN_FUNCTION(number_constr_nonew);

/*
 * builtin-string.c
 */
extern BUILTIN_FUNCTION(string_constr);
extern BUILTIN_FUNCTION(string_constr_nonew);

/*
 * builtin-object.c
 */
extern BUILTIN_FUNCTION(object_constr);
extern BUILTIN_FUNCTION(object_toString);

/*
 * builtin-function.c
 */
extern BUILTIN_FUNCTION(function_apply);

/*
 * call.c
 */
extern void call_function(Context *, JSValue, int, int);
extern void call_builtin(Context *, JSValue, int, int, int);
extern void tailcall_function(Context *, JSValue, int, int);
extern void tailcall_builtin(Context *, JSValue, int, int, int);
extern JSValue invoke_function(Context *, JSValue, JSValue, int, JSValue, int);
extern JSValue invoke_builtin(Context *, JSValue, JSValue, int, JSValue, int);

/*
 * codeloader.c
 */
extern char *insn_nemonic(int);
extern void init_code_loader(FILE *);
extern void end_code_loader(void);
extern int code_loader(Context *, FunctionTable *, int);
extern JSValue specstr_to_jsvalue(const char *);

/*
 * context.c
 */
void reset_context(Context *, FunctionTable *);
extern FunctionFrame *new_frame(Context *, FunctionTable *, FunctionFrame *, int);
extern void pop_special_registers(Context *, int, JSValue *);
extern void init_context(FunctionTable *, JSValue, Context **);

/*
 * conversion.c
 */
extern JSValue special_to_string(JSValue);
extern JSValue special_to_number(JSValue);
extern JSValue special_to_boolean(JSValue);
/* JSValue special_to_object(JSValue v); */
extern JSValue string_to_number(JSValue);
/* JSValue string_to_boolean(JSValue v); */
/* JSValue string_to_object(JSValue v); */
extern JSValue fixnum_to_string(JSValue);
extern JSValue flonum_to_string(JSValue);
extern JSValue number_to_string(JSValue);
/* JSValue fixnum_to_boolean(JSValue v); */
/* JSValue flonum_to_boolean(JSValue v); */
/* JSValue fixnum_to_object(JSValue v); */
/* JSValue flonum_to_object(JSValue v); */
extern double primitive_to_double(JSValue);
extern JSValue primitive_to_string(JSValue);
extern JSValue object_to_string(Context *, JSValue);
extern JSValue object_to_number(Context *, JSValue);
extern JSValue object_to_primitive(Context *, JSValue, int);
extern JSValue array_to_string(Context *, JSValue, JSValue);
extern JSValue to_object(Context *, JSValue);
extern JSValue to_string(Context *, JSValue);
extern JSValue to_boolean(JSValue v);
extern JSValue to_number(Context *, JSValue);
extern double to_double(Context *, JSValue);
extern JSValue number_to_cint(JSValue n);
extern cint toInteger(Context *context, JSValue a);
extern char *type_name(JSValue);
extern JSValue cint_to_string(cint);

/*
 * hash.c
 */
extern HashTable *malloc_hashtable(void);
extern int hash_create(HashTable *, unsigned int);
extern int hash_get_with_attribute(HashTable *, HashKey, HashData *, Attribute *attr);
extern int hash_get(HashTable *, HashKey, HashData *);
extern int hash_put_with_attribute(HashTable *, HashKey, HashData, Attribute);
#ifdef HIDDEN_CLASS
extern int hash_copy(Context *, HashTable *, HashTable *);
#endif
extern int hash_delete(HashTable *table, HashKey key);
extern int init_hash_iterator(HashTable *, HashIterator *);
extern void print_hash_table(HashTable *);

extern HashIterator createHashIterator(HashTable *);
extern int hash_next(HashTable *, HashIterator *, HashData *);
extern int nextHashEntry(HashTable *table, HashIterator *Iter, HashEntry *ep);
extern int nextHashCell(HashTable *table, HashIterator *iter, HashCell** p);
extern int rehash(HashTable *table);
extern HashCell **__hashMalloc(int size);
extern HashCell *__hashCellMalloc();
extern void hashBodyFree(HashCell **body);
extern void hashCellFree(HashCell *cell);

/*
 * string.c
 */
extern void init_string_table(unsigned int);
extern JSValue cstr_to_string_ool(Context *context, const char *s);
extern JSValue string_concat_ool(Context *context, JSValue v1, JSValue v2);

/*
 * init.c
 */
extern void init_global_constants(void);
extern void init_global_malloc_objects(void);
extern void init_global_objects(void);
extern void init_builtin(Context *);

/*
 * main.c
 */
extern void print_value_simple(Context *, JSValue);
extern void print_value_verbose(Context *, JSValue);
extern void print_value(Context *, JSValue, int);
extern void simple_print(JSValue);
extern void debug_print(Context *, int);

/*
 * object.c
 */
extern int get_prop(JSValue, JSValue, JSValue *);
extern JSValue get_prop_prototype_chain(JSValue, JSValue);
extern JSValue get_object_prop(Context *, JSValue, JSValue);
extern int has_prop_prototype_chain(JSValue o, JSValue p);
extern int has_array_element(JSValue a, cint n);
extern JSValue get_array_prop(Context *, JSValue, JSValue);
extern int set_prop_with_attribute(Context *, JSValue, JSValue, JSValue, Attribute);
extern int set_object_prop(Context *, JSValue, JSValue, JSValue);
extern int set_array_index_value(Context *, JSValue, cint, JSValue, int);
extern int set_array_prop(Context *, JSValue, JSValue, JSValue);
extern void remove_array_props(JSValue, cint, cint);
extern int delete_object_prop(JSValue obj, HashKey key);
extern int delete_array_element(JSValue a, cint n);
extern int iterator_get_next_propname(JSValue, JSValue *);
#ifdef USE_REGEXP
#ifdef need_regexp
extern int regexp_flag(JSValue);
#endif /* need_regexp */
#endif
#ifdef HIDDEN_CLASS_PROTO
extern JSValue new_object_proto_object(Context *, int, int);
#else /* HIDDEN_CLASS_PROTO */
extern JSValue new_simple_object_without___proto__(Context *, int, int);
#endif /* HIDDEN_CLASS_PROTO */
extern JSValue new_simple_object(Context *, int, int);
extern JSValue new_array_with_size(Context *, int, int, int);
extern JSValue new_function(Context *, Subscript, int, int);
extern JSValue new_builtin_with_constr(Context *, builtin_function_t, builtin_function_t, int, int, int);
extern JSValue new_builtin(Context *, builtin_function_t, int, int, int);
extern JSValue new_iterator(Context *, JSValue);
#ifdef USE_REGEXP
#ifdef need_regexp
extern JSValue new_regexp(Context *, char *, int, int, int);
#endif /* need_regexp */
#endif // USE_REGEXP
extern JSValue new_number_object(Context *, JSValue, int, int);
extern JSValue new_boolean_object(Context *, JSValue, int, int);
extern JSValue new_string_object(Context *, JSValue, int, int);
extern char *space_chomp(char *);
#ifdef HIDDEN_CLASS
#ifdef RICH_HIDDEN_CLASS
#ifdef ARRAY_EMBED_PROP
extern HiddenClass *new_empty_hidden_class(Context *, int, int, int, int, int);
#else /* ARRAY_EMBED_PROP */
extern HiddenClass *new_empty_hidden_class(Context *, int, int, int);
#endif /* ARRAY_EMBED_PROP */
#else /* RICH_HIDDEN_CLASS */
extern HiddenClass *new_empty_hidden_class(Context *, int, int);
#endif /* RICH_HIDDEN_CLASS */
extern HiddenClass *new_hidden_class(Context *, HiddenClass *);
extern void print_hidden_class(char *, HiddenClass *);
extern void print_all_hidden_class(void);
#endif

/*
 * operations.c
 */
extern JSValue slow_add(Context *, JSValue, JSValue);
extern JSValue slow_sub(Context *, JSValue, JSValue);
extern JSValue slow_mul(Context *, JSValue, JSValue);
extern JSValue slow_div(Context *, JSValue, JSValue);
extern JSValue slow_mod(Context *, JSValue, JSValue);
extern JSValue slow_bitand(Context *context, JSValue v1, JSValue v2);
extern JSValue slow_bitor(Context *context, JSValue v1, JSValue v2);
extern JSValue slow_leftshift(Context *context, JSValue v1, JSValue v2);
extern JSValue slow_rightshift(Context *context, JSValue v1, JSValue v2);
extern JSValue slow_unsignedrightshift(Context *context, JSValue v1, JSValue v2);
extern JSValue slow_lessthan(Context *context, JSValue v1, JSValue v2);
extern JSValue slow_lessthanequal(Context *context, JSValue v1, JSValue v2);

/*
 * vmloop.c
 */
extern int vmrun_threaded(Context *, int);

extern void init_builtin_global(Context *);
extern void init_builtin_object(Context *);
extern void init_builtin_array(Context *);
extern void init_builtin_function(Context *);
extern void init_builtin_number(Context *);
extern void init_builtin_string(Context *);
extern void init_builtin_boolean(Context *);
extern void init_builtin_math(Context *);
#ifdef USE_REGEXP
#ifdef need_regexp
extern void init_builtin_regexp(Context *);
#endif /* need_regexp */
#endif

/*
 * gc.c
 */
#ifdef GC_PROF
extern uint64_t total_alloc_bytes;
extern uint64_t total_alloc_count;
extern uint64_t pertype_alloc_bytes[];
extern uint64_t pertype_alloc_count[];
#endif /* GC_PROF */

#ifdef __cplusplus
}
#endif

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
