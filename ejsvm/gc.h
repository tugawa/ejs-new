/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "cell-header.h"

typedef uint32_t cell_type_t;

#ifdef USE_NATIVEGC

extern void init_memory(size_t);
extern void *gc_malloc(Context *, uintptr_t, uint32_t);

extern void enable_gc(Context *ctx);
extern void disable_gc(void);
extern void try_gc(Context *ctx);

#ifdef DEBUG
extern cell_type_t gc_obj_header_type(void *p);
#else /* DEBUG */
#define gc_obj_header_type(p) HEADER0_GET_TYPE(((header_t *) (p))[-1])
#endif /* DEBUG */

/* #define GC_ROOT(_type, _var) _type _var = ((_type) 0) */
#define GC_ROOT(_type, _var) _type _var

extern void gc_push_checked(void *addr);
#define GC_PUSH(a)                gc_push_checked(&a)
#define GC_PUSH2(a,b)             do {GC_PUSH(a); GC_PUSH(b);} while(0)
#define GC_PUSH3(a,b,c)           do {GC_PUSH(a); GC_PUSH2(b,c);} while(0)
#define GC_PUSH4(a,b,c,d)         do {GC_PUSH(a); GC_PUSH3(b,c,d);} while(0)
#define GC_PUSH5(a,b,c,d,e)       do {GC_PUSH(a); GC_PUSH4(b,c,d,e);} while(0)
#define GC_PUSH6(a,b,c,d,e,f)                           \
  do {GC_PUSH(a); GC_PUSH5(b,c,d,e,f);} while(0)
#define GC_PUSH7(a,b,c,d,e,f,g)                         \
  do {GC_PUSH(a); GC_PUSH6(b,c,d,e,f,g);} while(0)
#define GC_PUSH8(a,b,c,d,e,f,g,h)                       \
  do {GC_PUSH(a); GC_PUSH7(b,c,d,e,f,g,h);} while(0)

extern void gc_pop_checked(void* addr);
#define GC_POP(a)                gc_pop_checked(&a)
#define GC_POP2(a,b)             do {GC_POP(a); GC_POP(b);} while(0)
#define GC_POP3(a,b,c)           do {GC_POP(a); GC_POP2(b,c);} while(0)
#define GC_POP4(a,b,c,d)         do {GC_POP(a); GC_POP3(b,c,d);} while(0)
#define GC_POP5(a,b,c,d,e)       do {GC_POP(a); GC_POP4(b,c,d,e);} while(0)
#define GC_POP6(a,b,c,d,e,f)     do {GC_POP(a); GC_POP5(b,c,d,e,f);} while(0)
#define GC_POP7(a,b,c,d,e,f,g)   do {GC_POP(a); GC_POP6(b,c,d,e,f,g);} while(0)
#define GC_POP8(a,b,c,d,e,f,g,h)                        \
  do {GC_POP(a); GC_POP7(b,c,d,e,f,g,h);} while(0)

#else

#error Boehm GC is no longer supported

#define init_memory()
static JSValue *gc_malloc(Context *c, uintptr_t request_bytes, uint32_t type)
{
  size_t alloc_bytes;
  JSValue *mem;

  alloc_bytes =
    (request_bytes + BYTES_IN_JSVALUE - 1) & ~(BYTES_IN_JSVALUE - 1);
  mem = (JSValue *) malloc(alloc_bytes);
  *mem = make_header(alloc_bytes / BYTES_IN_JSVALUE, type);
  return mem + 1;
}
#define enable_gc(ctx)
#define disable_gc(ctx)

static cell_type_t gc_obj_header_type(void *p)
{
  uint64_t *ptr = (uint64_t *) p;
  return ptr[-1] & HEADER_TYPE_MASK;
}

#define GC_PUSH(a)
#define GC_PUSH2(a,b)
#define GC_PUSH3(a,b,c)
#define GC_PUSH4(a,b,c,d)
#define GC_PUSH5(a,b,c,d,e)
#define GC_PUSH6(a,b,c,d,e,f)
#define GC_PUSH7(a,b,c,d,e,f,g)
#define GC_PUSH8(a,b,c,d,e,f,g,h)

#define GC_POP(a)
#define GC_POP2(a,b)
#define GC_POP3(a,b,c)
#define GC_POP4(a,b,c,d)
#define GC_POP5(a,b,c,d,e)
#define GC_POP6(a,b,c,d,e,f)
#define GC_POP7(a,b,c,d,e,f,g)
#define GC_POP8(a,b,c,d,e,f,g,h)

#endif  /* USE_NATIVEGC */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
