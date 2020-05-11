/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#ifdef __cplusplus
extern "C" {
#endif

#ifndef USE_NATIVEGC
#error Boehm GC is no longer supported
#endif  /* USE_NATIVEGC */

/*
 * Alignment of objects in the heap
 */

#ifdef BIT_ALIGN32
#define LOG_BYTES_IN_GRANULE  2
typedef uint32_t granule_t;
#else /* BIT_ALIGN32 */
#define LOG_BYTES_IN_GRANULE  3
typedef uint64_t granule_t;
#endif /* BIT_ALIGN32 */

#define LOG_BITS_IN_GRANULE  (LOG_BYTES_IN_GRANULE + 3)
#define BYTES_IN_GRANULE     (1 << LOG_BYTES_IN_GRANULE)
#define BITS_IN_GRANULE      (BYTES_IN_GRANULE * 8)
#define BYTE_TO_GRANULE_ROUNDUP(x)              \
  (((x) + BYTES_IN_GRANULE - 1) >> LOG_BYTES_IN_GRANULE)

/*
 * GC profiling stuff
 */

#ifdef GC_PROF
#define NUM_DEFINED_CELL_TYPES 0x1F
extern const char *cell_type_name[NUM_DEFINED_CELL_TYPES + 1];
#define CELLT_NAME(t) ((t) <= NUM_DEFINED_CELL_TYPES ? cell_type_name[t] : "")
#else /* GC_PROF */
#define CELLT_NAME(t) abort();  /* HTAG_NAME is only for GC profiling */
#endif /* GC_PROF */

/*
 * GC interface
 */

extern void init_memory(size_t);
extern void *gc_malloc(Context *, uintptr_t, cell_type_t);
#ifdef FLONUM_SPACE
extern FlonumCell *gc_try_alloc_flonum(double x);
#endif /* FLONUM_SPACE */

extern void enable_gc(Context *ctx);
extern void disable_gc(void);
extern void try_gc(Context *ctx);

static inline void gc_restore_root_stack(int sp);
static inline int gc_save_root_stack();
static inline void gc_pop_checked(void *addr);
static inline void gc_push_checked(void *addr);
static inline cell_type_t gc_obj_header_type(void *p);
  
#define GC_ROOT(_type, _var) _type _var

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

#define GC_POP(a)                gc_pop_checked(&a)
#define GC_POP2(a,b)             do {GC_POP(a); GC_POP(b);} while(0)
#define GC_POP3(a,b,c)           do {GC_POP(a); GC_POP2(b,c);} while(0)
#define GC_POP4(a,b,c,d)         do {GC_POP(a); GC_POP3(b,c,d);} while(0)
#define GC_POP5(a,b,c,d,e)       do {GC_POP(a); GC_POP4(b,c,d,e);} while(0)
#define GC_POP6(a,b,c,d,e,f)     do {GC_POP(a); GC_POP5(b,c,d,e,f);} while(0)
#define GC_POP7(a,b,c,d,e,f,g)   do {GC_POP(a); GC_POP6(b,c,d,e,f,g);} while(0)
#define GC_POP8(a,b,c,d,e,f,g,h)                        \
  do {GC_POP(a); GC_POP7(b,c,d,e,f,g,h);} while(0)

#ifdef __cplusplus
}
#endif

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
