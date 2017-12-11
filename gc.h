/*
   gc.h

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-17
     Hideya Iwasaki, 2016-17

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/

typedef uint32_t cell_type_t;

#ifdef USE_NATIVEGC

extern void init_memory(void);
extern void *gc_malloc(Context *, uintptr_t, uint32_t);
extern JSValue *gc_jsalloc(Context *, uintptr_t, uint32_t);
extern void gc_push_tmp_root(JSValue *loc);
extern void gc_push_tmp_root2(JSValue *loc1, JSValue *loc2);
extern void gc_push_tmp_root3(JSValue *loc1, JSValue *loc2, JSValue *loc3);
extern void gc_pop_tmp_root(int n);

extern void enable_gc(Context *ctx);
extern void disable_gc(void);
extern void try_gc(Context *ctx);

extern cell_type_t gc_obj_header_type(void *p);

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
  return mem + 1;
}
#define gc_push_tmp_root(x)
#define gc_push_tmp_root2(x, y)
#define gc_push_tmp_root3(x, y, z)
#define gc_pop_tmp_root(x)
#define enable_gc()
#define disable_gc()
static cell_type_t gc_obj_header_type(void *p)
{
  uint64_t *ptr = (uint64_t *) p;
  return ptr[-1] & HEADER_TYPE_MASK;
}
#endif

#define gc_malloc_critical(s,t) (gc_malloc(NULL,(s),(t)))
#define gc_jsalloc_critical(s,t) (gc_jsalloc(NULL,(s),(t)))

