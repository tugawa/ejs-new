#define FUNCTION_TABLE_LIMIT  (100)

EXTERN FunctionTable function_table[FUNCTION_TABLE_LIMIT];
EXTERN StrTable string_table;

extern int ftable_flag;
extern int trace_flag;
extern int lastprint_flag;

#ifdef __cplusplus
extern "C" {
#endif

/*
 * allocate.c
 */
extern FlonumCell *allocate_flonum(double);
extern StringCell *allocate_string(uint32_t);
extern JSValue allocate_string2(const char *, const char *);
extern Object *allocate_object(Context *ctx);
extern ArrayCell *allocate_array(void);
extern void allocate_array_data(JSValue, int, int);
extern FunctionCell *allocate_function(void);
extern BuiltinCell *allocate_builtin(void);
extern JSValue *allocate_prop_table(int size);
extern IteratorCell *allocate_iterator(void);
#ifdef USE_REGEXP
extern RegexpCell *allocate_regexp(void);
#endif
extern BoxedCell *allocate_boxed(uint32_t);

/*
 * builtin.c
 */
extern BUILTIN_FUNCTION(builtin_const_true);
extern BUILTIN_FUNCTION(builtin_const_false);
extern BUILTIN_FUNCTION(builtin_const_undefined);
extern BUILTIN_FUNCTION(builtin_const_null);
extern BUILTIN_FUNCTION(builtin_identity);
extern BUILTIN_FUNCTION(builtin_fixnum_to_string);
extern BUILTIN_FUNCTION(builtin_flonum_to_string);
// extern BUILTIN_FUNCTION(builtin_string_to_index);

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
 * call.c
 */
extern void call_function(Context *, JSValue, int, int);
extern void call_builtin(Context *, JSValue, int, int, int);
extern void tailcall_function(Context *, JSValue, int, int);
extern void tailcall_builtin(Context *, JSValue, int, int, int);
extern JSValue invoke_function0(Context *, JSValue, JSValue, int);
JSValue call_builtin0(Context *, JSValue, JSValue, int);


/*
 * codeloader.c
 */
extern char *insn_nemonic(int);
extern void init_code_loader(FILE *);
extern void end_code_loader(void);
extern int code_loader(FunctionTable *);
extern void init_constant_cell(ConstantCell *);
extern void end_constant_cell(ConstantCell *);
extern int insn_load(ConstantCell *, Bytecode *, int);
extern int update_function_table(FunctionTable *, int, ConstantCell *,
                                 Bytecode *, int, int, int, int);

extern JSValue specstr_to_jsvalue(const char *);

extern int add_constant_number(ConstantCell *, double);
extern int add_constant_string(ConstantCell *, char *);
#ifdef USE_REGEXP
extern int add_constant_regexp(ConstantCell *, char *, int);
#endif

/*
 * context.c
 */
extern FunctionFrame *new_frame(FunctionTable *, FunctionFrame *);
extern void pop_special_registers(Context *, int, JSValue *);
extern void init_context(FunctionTable *, JSValue, Context **);

/*
 * conversion.c
 */
extern JSValue special_to_string(JSValue);
extern JSValue special_to_number(JSValue);
extern JSValue special_to_boolean(JSValue);
// JSValue special_to_object(JSValue v);
extern JSValue string_to_number(JSValue);
// JSValue string_to_boolean(JSValue v);
// JSValue string_to_object(JSValue v);
extern JSValue fixnum_to_string(JSValue);
extern JSValue flonum_to_string(JSValue);
extern JSValue number_to_string(JSValue);
// JSValue fixnum_to_boolean(JSValue v);
// JSValue flonum_to_boolean(JSValue v);
// JSValue fixnum_to_object(JSValue v);
// JSValue flonum_to_object(JSValue v);
extern double primitive_to_double(JSValue);
extern JSValue primitive_to_string(JSValue);
extern JSValue object_to_string(Context *, JSValue);
extern JSValue object_to_number(Context *, JSValue);
extern JSValue object_to_primitive(Context *, JSValue, int);
extern JSValue array_to_string(Context *, JSValue, JSValue);
extern JSValue to_object(Context *, JSValue v);
extern JSValue to_string(Context *, JSValue v);
extern JSValue to_boolean(JSValue v);
extern JSValue to_number(Context *, JSValue v);
extern double to_double(Context *, JSValue v);
extern char *type_name(JSValue v);

/*
 * hash.c
 */

extern HashTable *malloc_hashtable(void);
extern int hash_create(HashTable *, unsigned int);
extern int hash_get(HashTable *, HashKey, HashData *);
extern int hash_put_with_attribute(HashTable *, HashKey, HashData, Attribute);
extern int init_hash_iterator(HashTable *, HashIterator *);
extern void print_hash_table(HashTable *);
extern void print_object_properties(JSValue);

// DELETE
// int hashDelete(HashTable *table, HashKey key);

extern  HashIterator createHashIterator(HashTable *);
extern int hash_next(HashTable *, HashIterator *, HashData *);
// int hashNextKey(HashTable *table, HashIterator *Iter, HashKey *key);
extern int __hashNext(HashTable *table, HashIterator *Iter, HashEntry *ep);
extern int ___hashNext(HashTable *table, HashIterator *iter, HashCell** p);

extern int rehash(HashTable *table);

extern uint32_t calc_hash_len(const char *, uint32_t);
extern uint32_t calc_hash(const char *);
extern uint32_t calc_hash_len2(const char *, uint32_t, const char *, uint32_t);
uint32_t calc_hash2(const char *, const char *);

extern HashCell **__hashMalloc(int size);
extern HashCell *__hashCellMalloc();

extern void hashBodyFree(HashCell **body);
extern void hashCellFree(HashCell *cell);
// char* ststrdup(const char*);

extern void init_string_table(unsigned int);
extern JSValue str_intern(const char* s, int len, uint32_t hash, int soft);
extern JSValue str_intern2(const char* s1, int len1, const char* s2, int len2,
                           uint32_t hash, int soft);

/*
 * init.c
 */
extern void init_global_constants(void);
extern JSValue init_global(void);

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
extern JSValue get_array_prop(Context *, JSValue, JSValue);
extern int set_prop_with_attribute(JSValue, JSValue, JSValue, Attribute);
extern int set_object_prop(Context *, JSValue, JSValue, JSValue);
extern int set_array_prop(Context *, JSValue, JSValue, JSValue);
extern int get_next_propname(JSValue, JSValue *);
#ifdef USE_REGEXP
extern int regexp_flag(JSValue);
#endif
extern JSValue new_object_without_prototype(Context *);
extern JSValue new_object(Context *);
extern JSValue new_array(void);
extern JSValue new_array_with_size(int);
extern JSValue new_function(Context *, Subscript);
extern JSValue new_builtin_with_constr(builtin_function_t, builtin_function_t, int);
extern JSValue new_builtin(builtin_function_t, int);
extern JSValue new_iterator(JSValue);
#ifdef USE_REGEXP
extern JSValue new_regexp(char *, int);
#endif // USE_REGEXP
extern JSValue new_number(JSValue);
extern JSValue new_boolean(JSValue);
extern JSValue new_string(JSValue);

extern char *space_chomp(char *);
// extern double cstr_to_double(char *);

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

extern void init_builtin_global(void);
extern void init_builtin_object(void);
extern void init_builtin_array(void);
extern void init_builtin_number(void);
extern void init_builtin_string(void);
extern void init_builtin_boolean(void);
extern void init_builtin_math(void);
#ifdef USE_REGEXP
extern void init_builtin_regexp(void);
#endif

#ifdef __cplusplus
}
#endif
