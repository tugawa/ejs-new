/*
 * gc.h
 * 
 *   eJS Project
 *
 *   Hideya Iwaski, 2016
 *   Tomoharu Ugawa, 2016
 */

#ifdef USE_NATIVEGC

extern void init_memory(void);
extern void *gc_malloc(Context *, uintptr_t, uint32_t);
extern JSValue *gc_jsalloc(Context *, uintptr_t, uint32_t);
extern void gc_push_tmp_root(void *loc);
extern void gc_pop_tmp_root(int n);

extern void enable_gc(Context *ctx);
extern void disable_gc(void);

#else

#define init_memory()
#define gc_malloc(c,s,t)  (malloc(s))
static JSValue *gc_jsalloc(Context *c, uintptr_t request_bytes, uint32_t type)
{
  size_t alloc_bytes;
  JSValue *mem;

  alloc_bytes =
    (request_bytes + BYTES_IN_JSVALUE - 1) & ~(BYTES_IN_JSVALUE - 1);
  mem = (JSValue *) malloc(alloc_bytes);
  *mem = make_header(alloc_bytes / BYTES_IN_JSVALUE, type);
  return mem;
}
#define gc_push_tmp_root(x)
#define gc_pop_tmp_root(x)
#define enable_gc()
#define disable_gc()

#endif

#define gc_malloc_critical(s,t) (gc_malloc(NULL,(s),(t)))
#define gc_jsalloc_critical(s,t) (gc_jsalloc(NULL,(s),(t)))

#define MATYPE_PROP           1
#define MATYPE_ARRAY_DATA     2
#define MATYPE_FUNCTION_FRAME 3
#define MATYPE_HASH_BODY      4
#define MATYPE_STR_CONS       5
