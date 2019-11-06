/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#ifndef USE_NATIVEGC
#error Boehm GC is no longer supported
#endif  /* USE_NATIVEGC */

/*
 * Alignment of objects in the heap
 */

#ifdef BIT_ALIGN32
#define LOG_BYTES_IN_GRANULE  2
#else /* BIT_ALIGN32 */
#define LOG_BYTES_IN_GRANULE  3
#endif /* BIT_ALIGN32 */

#define LOG_BITS_IN_GRANULE  (LOG_BYTES_IN_GRANULE + 3)
#define BYTES_IN_GRANULE     (1 << LOG_BYTES_IN_GRANULE)
#define BITS_IN_GRANULE      (BYTES_IN_GRANULE * 8)
#define BYTE_TO_GRANULE_ROUNDUP(x)              \
  (((x) + BYTES_IN_GRANULE - 1) >> LOG_BYTES_IN_GRANULE)

/*
 * Object header layout
 *
 * Heap objects are aligned in `granule' boundary.  Header may consist
 * of multiple granules.  HEADER_GRANULES gives the number of granules
 * in a header.
 *
 * Header fields
 *  - type    Cell type
 *  - markbit Mark bit for GC
 *  - extra   The number of over-allocated space in granule.
 *  - gen     Generation of this object describing the number of GC cycles
 *            have been performed (modulo field size) befor the allocation
 *            of this object.
 *  - magic   Magic number
 *  - size    Size of the object in granule, including the header and extra.
 */

#ifdef BIT_ALIGN32
#define HEADER_GRANULES       2
#define HEADER_TYPE_BITS      8
#define HEADER_MARKBIT_BITS   1
#define HEADER_EXTRA_BITS     3
#define HEADER_GEN_BITS       4
#define HEADER_MAGIC_BITS     16
#define HEADER_SIZE_BITS      32
#define HEADER_MAGIC          0x18
#else /* BIT_ALIGN32 */
#define HEADER_GRANULES       1
#define HEADER_TYPE_BITS      8
#define HEADER_MARKBIT_BITS   1
#define HEADER_EXTRA_BITS     3
#define HEADER_GEN_BITS       4
#define HEADER_MAGIC_BITS     16
#define HEADER_SIZE_BITS      32
#define HEADER_MAGIC          0x18
#endif /* BIT_ALIGN32 */

typedef struct header_t {
  cell_type_t  type:    HEADER_TYPE_BITS;
  unsigned int markbit: HEADER_MARKBIT_BITS;
  unsigned int extra:   HEADER_EXTRA_BITS;
  unsigned int magic:   HEADER_MAGIC_BITS;
  unsigned int gen:     HEADER_GEN_BITS;
  unsigned int size:    HEADER_SIZE_BITS;
} header_t;

static inline header_t compose_header(size_t granules, size_t extra,
                                      cell_type_t type)
{
  header_t hdr;
  hdr.type = type;
  hdr.markbit = 0;
  hdr.extra = extra;
  hdr.magic = HEADER_MAGIC;
#ifdef GC_DEBUG
  hdr.gen = generation;
#else /* GC_DEBUG */
  hdr.gen = 0;
#endif /* GC_DEBUG */
  hdr.size  = granules;
  return hdr;
}

static inline void *header_to_payload(header_t *hdrp)
{
  return (void *) (hdrp + 1);
}

static inline header_t *payload_to_header(void *ptr)
{
  return ((header_t *) ptr) - 1;
}

/*
 * GC profiling stuff
 */

#ifdef GC_PROF
#define NUM_DEFINED_CELL_TYPES 0x1E
extern const char *cell_type_name[NUM_DEFINED_CELL_TYPES + 1];
#define CELLT_NAME(t) ((t) <= NUM_DEFINED_CELL_TYPES ? cell_type_name[t] : "")
#else /* GC_PROF */
#define CELLT_NAME(t) abort();  /* HTAG_NAME is only for GC profiling */
#endif /* GC_PROF */

/*
 * GC interface
 */

extern void init_memory(size_t);
extern void *gc_malloc(Context *, uintptr_t, uint32_t);

extern void enable_gc(Context *ctx);
extern void disable_gc(void);
extern void try_gc(Context *ctx);

static inline cell_type_t gc_obj_header_type(void *p)
{
  header_t *hdrp = payload_to_header(p);
  return hdrp->type;
}

static inline void gc_push_checked(void *addr)
{
  extern JSValue *gc_root_stack[];
  extern int gc_root_stack_ptr;
  gc_root_stack[gc_root_stack_ptr++] = (JSValue *) addr;
}

static inline void gc_pop_checked(void *addr)
{
  extern JSValue *gc_root_stack[];
  extern int gc_root_stack_ptr;
  assert(gc_root_stack[gc_root_stack_ptr - 1] == (JSValue *) addr);
  --gc_root_stack_ptr;
}

static inline int gc_save_root_stack()
{
  extern int gc_root_stack_ptr;
  return gc_root_stack_ptr;
}

static inline void gc_restore_root_stack(int sp)
{
  extern int gc_root_stack_ptr;
  gc_root_stack_ptr = sp;
}

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

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
