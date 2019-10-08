/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include <stdlib.h>
#include <stdio.h>
#include "prefix.h"
#define EXTERN extern
#include "header.h"
#include "log.h"

/* Objects allocated in the heap
 *                       has   stored as  visible   know
 *                      (ptag) (JSValue) (to user) (size) (type)
 *   HTAG_STRING         yes    yes       yes       fixed StringCell
 *   HTAG_FLONUM         yes    yes       yes       fixed FlonumCell
 *   HTAG_SIMPLE_OBJECT  yes    yes       yes       yes   JSObject
 *   HTAG_ARRAY          yes    yes       yes       yes   JSObject
 *   HTAG_FUNCTION       yes    yes       yes       yes   JSObject
 *   HTAG_BUILTIN        yes    yes       yes       yes   JSObject
 *   HTAG_BOXED_NUMBER   yes    yes       yes       yes   JSObject
 *   HTAG_BOXED_BOOLEAN  yes    yes       yes       yes   JSObject
 *   HTAB_BOXED_STRING   yes    yes       yes       yes   JSObject
 *   HTAG_REGEXP         yes    yes       yes       yes   JSObject
 *   HTAG_ITERATOR       yes    yes       no        yes   Iterator
 *   HTAG_PROP           no     yes       no        no    JSValue*
 *   HTAG_ARRAY_DATA     no     no        no        no    JSValue*
 *   HTAG_FUNCTION_FRAME no     no        no        yes   FunctionFrame
 *   HTAG_STR_CONS       no     no        no        fixed StrCons
 *   HTAG_CONTEXT        no     no        no        fixed Context
 *   HTAG_STACK          no     no        no        no    JSValue*
 *   HTAG_HASHTABLE      no     no        no        fixed HashTable
 *   HTAG_HASH_BODY      no     no        no        no    HashCell**
 *   HTAG_HASH_CELL      no     no        no        fixed HashCell
 *   HTAG_PROPERTY_MAP   no     yes       no        fixed PropertyMap
 *   HTAG_SHAPE          no     no        no        fixed Shape
 *
 * Objects that do not know their size (PROP, ARRAY_DATA, STACK, HASH_BODY)
 * are stored in a dedicated slot and scand together with their owners.
 *
 * HTAG_PROP is stored in the last embedded slot.
 * HTAG_PROPERTY_MAP is stored as the value of property __property_map__
 * of a prototype object.
 *
 * Static data structures
 *   FunctionTable[] function_table (global.h)
 *   StrTable string_table (global.h)
 */

#ifndef NDEBUG
#define GC_DEBUG 1
#define STATIC        /* make symbols global for debugger */
#define STATIC_INLINE /* make symbols global for debugger */
#else
#undef GC_DEBUG
#define STATIC static
#define STATIC_INLINE static inline
#endif

#if 0
#define GCLOG(...) LOG(__VA_ARGS__)
#define GCLOG_TRIGGER(...) LOG(__VA_ARGS__)
#define GCLOG_ALLOC(...) LOG(__VA_ARGS__)
#define GCLOG_SWEEP(...) LOG(__VA_ARGS__)
#else /* 0 */
#define GCLOG(...)
#define GCLOG_TRIGGER(...)
#define GCLOG_ALLOC(...)
#define GCLOG_SWEEP(...)
#endif /* 0 */

/*
 * defined in header.h
 */
/* typedef uint64_t JSValue; */
#define LOG_BYTES_IN_JSVALUE   3
/* #define BYTES_IN_JSVALUE       (1 << LOG_BYTES_IN_JSVALUE) */

#ifdef EXCESSIVE_GC
#define GC_THREASHOLD_SHIFT 4
#else  /* EXCESSIVE_GC */
#define GC_THREASHOLD_SHIFT 1
#endif /* EXCESSIVE_GC */

/*
 * If the remaining room is smaller than a certain size,
 * we do not use the remainder for efficiency.  Rather,
 * we add it below the chunk being allocated.  In this case,
 * the size in the header includes the extra words.
 */
#define MINIMUM_FREE_CHUNK_JSVALUES 4

#include "cell-header.h"

/*
 *  Macro
 */

#define GC_MARK_BIT (1 << HEADER0_GC_OFFSET)

/*
 *  Types
 */

#define HTAG_FREE          (0xff)

struct free_chunk {
  header_t header;
  struct free_chunk *next;
};

struct space {
  uintptr_t addr;
  size_t bytes;
  size_t free_bytes;
  struct free_chunk* freelist;
  char *name;
};

struct property_map_roots {
  PropertyMap *pm;
  struct property_map_roots *next;
};

enum gc_phase {
  PHASE_INACTIVE,
  PHASE_INITIALISE,
  PHASE_MARK,
  PHASE_WEAK,
  PHASE_SWEEP,
  PHASE_FINALISE,
};

/*
 * variables
 */
STATIC struct space js_space;
#ifdef GC_DEBUG
STATIC struct space debug_js_shadow;
#endif /* GC_DEBUG */

enum gc_phase gc_phase = PHASE_INACTIVE;

/* gc root stack */
#define MAX_ROOTS 1000
STATIC JSValue *gc_root_stack[MAX_ROOTS];
STATIC int gc_root_stack_ptr = 0;

STATIC int gc_disabled = 1;

int generation = 0;
int gc_sec;
int gc_usec;
#ifdef GC_PROF
uint64_t total_alloc_bytes;
uint64_t total_alloc_count;
uint64_t pertype_alloc_bytes[256];
uint64_t pertype_alloc_count[256];
uint64_t pertype_live_bytes[256];
uint64_t pertype_live_count[256];

const char *htag_name[NUM_DEFINED_HTAG + 1] = {
    /* 00 */ "free",
    /* 01 */ "",
    /* 02 */ "",
    /* 03 */ "",
    /* 04 */ "STRING",
    /* 05 */ "FLONUM",
    /* 06 */ "SIMPLE_OBJECT",
    /* 07 */ "ARRAY",
    /* 08 */ "FUNCTION",
    /* 09 */ "BUILTIN",
    /* 0A */ "ITERATOR",
    /* 0B */ "REGEXP",
    /* 0C */ "BOXED_STRING",
    /* 0D */ "BOXED_NUMBER",
    /* 0E */ "BOXED_BOOLEAN",
    /* 0F */ "",
    /* 10 */ "",
    /* 11 */ "PROP",
    /* 12 */ "ARRAY_DATA",
    /* 13 */ "FUNCTION_FRAME",
    /* 14 */ "STR_CONS",
    /* 15 */ "CONTEXT",
    /* 16 */ "STACK",
    /* 17 */ "HIDDEN_CLASS",
    /* 18 */ "HASHTABLE",
    /* 19 */ "HASH_BODY",
    /* 1a */ "HASH_CELL",
    /* 1b */ "HTAG_PROPERTY_MAP",
    /* 1c */ "HTAG_SHAPE",
};
#endif /* GC_PROF */

#ifdef GC_DEBUG
STATIC void sanity_check();
#endif /* GC_DEBUG */

struct property_map_roots *property_map_roots;

/*
 * prototype
 */
/* space */
STATIC void create_space(struct space *space, size_t bytes, char* name);
STATIC int in_js_space(void *addr_);
#ifdef GC_DEBUG
STATIC header_t *get_shadow(void *ptr);
#endif /* GC_DEBUG */
/* GC */
STATIC int check_gc_request(Context *);
STATIC void garbage_collection(Context *ctx);
STATIC void scan_roots(Context *ctx);
STATIC void weak_clear_StrTable(StrTable *table);
STATIC void weak_clear(void);
STATIC void sweep(void);
#ifdef ALLOC_SITE_CACHE
STATIC void alloc_site_update_info(JSObject *p);
#endif /* ALLOC_SITE_CACHE */
#ifdef GC_DEBUG
STATIC void check_invariant(void);
STATIC void print_memory_status(void);
STATIC void print_heap_stat(void);
#endif /* GC_DEBUG */


/*
 *  Space
 */

STATIC void create_space(struct space *space, size_t bytes, char *name)
{
  struct free_chunk *p;
  p = (struct free_chunk *) malloc(bytes);
  p->header = HEADER0_COMPOSE(bytes >> LOG_BYTES_IN_JSVALUE, 0, HTAG_FREE);
#ifdef GC_DEBUG
  HEADER0_SET_MAGIC(p->header, HEADER0_MAGIC);
#endif /* GC_DEBUG */
  p->next = NULL;
  space->addr = (uintptr_t) p;
  space->bytes = bytes;
  space->free_bytes = bytes;
  space->freelist = p;
  space->name = name;
}

STATIC int in_js_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return (js_space.addr <= addr && addr < js_space.addr + js_space.bytes);
}

#ifdef GC_DEBUG
STATIC header_t *get_shadow(void *ptr)
{
  if (in_js_space(ptr)) {
    uintptr_t a = (uintptr_t) ptr;
    uintptr_t off = a - js_space.addr;
    return (header_t *) (debug_js_shadow.addr + off);
  } else
    return NULL;
}
#endif /* GC_DEBUG */

/*
 * Returns a pointer to the first address of the memory area
 * available to the VM.  The header precedes the area.
 * The header has the size of the chunk including the header,
 * the area available to the VM, and extra bytes if any.
 * Other header bits are zero
 */
STATIC void* space_alloc(struct space *space,
                         size_t request_bytes, cell_type_t type)
{
  size_t  alloc_jsvalues;
  struct free_chunk **p;
  
  alloc_jsvalues =
    (request_bytes + BYTES_IN_JSVALUE - 1) >> LOG_BYTES_IN_JSVALUE;
  alloc_jsvalues += HEADER_JSVALUES;

  /* allocate from freelist */
  for (p = &space->freelist; *p != NULL; p = &(*p)->next) {
    struct free_chunk *chunk = *p;
    size_t chunk_jsvalues = HEADER0_GET_SIZE(chunk->header);
    if (chunk_jsvalues >= alloc_jsvalues) {
      if (chunk_jsvalues >= alloc_jsvalues + MINIMUM_FREE_CHUNK_JSVALUES) {
        /* This chunk is large enough to leave a part unused.  Split it */
        size_t new_chunk_jsvalues = chunk_jsvalues - alloc_jsvalues;
        uintptr_t addr =
          ((uintptr_t) chunk) + (new_chunk_jsvalues << LOG_BYTES_IN_JSVALUE);
        HEADER0_SET_SIZE(chunk->header, new_chunk_jsvalues);
        *(header_t *) addr = HEADER0_COMPOSE(alloc_jsvalues, 0, type);
#ifdef GC_DEBUG
        HEADER0_SET_MAGIC(*(header_t *) addr, HEADER0_MAGIC);
        HEADER0_SET_GEN_MASK(*(header_t *) addr, generation);
#endif /* GC_DEBUG */
        space->free_bytes -= alloc_jsvalues << LOG_BYTES_IN_JSVALUE;
        return (void *) (addr + HEADER_BYTES);
      } else {
        /* This chunk is too small to split. */
        *p = (*p)->next;
        chunk->header =
          HEADER0_COMPOSE(chunk_jsvalues,
                          chunk_jsvalues - alloc_jsvalues, type);
#ifdef GC_DEBUG
        HEADER0_SET_MAGIC(chunk->header, HEADER0_MAGIC);
        HEADER0_SET_GEN_MASK(chunk->header, generation);
#endif /* GC_DEBUG */
        space->free_bytes -= chunk_jsvalues << LOG_BYTES_IN_JSVALUE;
        return (void *) (((uintptr_t) chunk) + HEADER_BYTES);
      }
    }
  }

#ifdef DEBUG
  {
    struct free_chunk *chunk;
    for (chunk = space->freelist; chunk != NULL; chunk = chunk->next) {
      size_t chunk_jsvalues = HEADER0_GET_SIZE(chunk->header);
      LOG(" %lu", chunk_jsvalues * BYTES_IN_JSVALUE);
    }
  }
  LOG("\n");
  LOG("js_space.bytes = %lu\n", js_space.bytes);
  LOG("js_space.free_bytes = %lu\n", js_space.free_bytes);
  LOG("gc_disabled = %d\n", gc_disabled);
  LOG("request = %lu\n", request_bytes);
  LOG("type = 0x%x\n", type);
  LOG("memory exhausted\n");
#endif /* DEBUG */
  abort();
  return NULL;
}


/*
 * GC
 */

void init_memory(size_t js_space_bytes)
{
  create_space(&js_space, js_space_bytes, "js_space");
#ifdef GC_DEBUG
  create_space(&debug_js_shadow, js_space_bytes, "debug_js_shadow");
#endif /* GC_DEBUG */
  gc_root_stack_ptr = 0;
  gc_disabled = 0;
  generation = 1;
  gc_sec = 0;
  gc_usec = 0;
}

void gc_push_checked(void *addr)
{
  gc_root_stack[gc_root_stack_ptr++] = (JSValue *) addr;
}

void gc_pop_checked(void *addr)
{
#ifdef GC_DEBUG
  if (gc_root_stack[gc_root_stack_ptr - 1] != (JSValue *) addr) {
    fprintf(stderr, "GC_POP pointer does not match\n");
    abort();
  }
#endif /* GC_DEBUG */
  gc_root_stack[--gc_root_stack_ptr] = NULL;
}

#ifdef DEBUG
cell_type_t gc_obj_header_type(void *p)
{
  header_t *hdrp = ((header_t *) p) - 1;
  return HEADER0_GET_TYPE(*hdrp);
}
#endif /* DEBUG */

STATIC int check_gc_request(Context *ctx)
{
  if (ctx == NULL) {
    if (js_space.free_bytes <
        js_space.bytes - (js_space.bytes >> GC_THREASHOLD_SHIFT))
      GCLOG_TRIGGER("Needed gc for js_space -- cancelled: ctx == NULL\n");
    return 0;
  }
  if (gc_disabled) {
    if (js_space.free_bytes <
        js_space.bytes - (js_space.bytes >> GC_THREASHOLD_SHIFT))
      GCLOG_TRIGGER("Needed gc for js_space -- cancelled: GC disabled\n");
    return 0;
  }
  if (js_space.free_bytes <
      js_space.bytes - (js_space.bytes >> GC_THREASHOLD_SHIFT))
    return 1;
  GCLOG_TRIGGER("no GC needed (%d bytes free)\n", js_space.free_bytes);
  return 0;
}

void* gc_malloc(Context *ctx, uintptr_t request_bytes, uint32_t type)
{
  JSValue *addr;

  if (check_gc_request(ctx))
    garbage_collection(ctx);
  addr = space_alloc(&js_space, request_bytes, type);
  GCLOG_ALLOC("gc_jsalloc: req %x bytes type %d => %p\n",
              request_bytes, type, addr);
#ifdef GC_DEBUG
  {
  header_t *hdrp = (header_t *) (addr - HEADER_JSVALUES);
  header_t *shadow = get_shadow(hdrp);
  *shadow = *hdrp;
  }
#endif /* GC_DEBUG */
#ifdef GC_PROF
  {
    size_t alloc_bytes =
      (request_bytes + BYTES_IN_JSVALUE * (HEADER_JSVALUES + 1) - 1) &
      ~((1 << LOG_BYTES_IN_JSVALUE) - 1);
    total_alloc_bytes += alloc_bytes;
    total_alloc_count++;
    pertype_alloc_bytes[type] += alloc_bytes;
    pertype_alloc_count[type]++;
  }
#endif /* GC_PROF */
  return addr;
}

void disable_gc(void)
{
  gc_disabled++;
}

void enable_gc(Context *ctx)
{
  if (--gc_disabled == 0) {
    if (check_gc_request(ctx))
      garbage_collection(ctx);
  }
}

void try_gc(Context *ctx)
{
  if (check_gc_request(ctx))
    garbage_collection(ctx);
}

STATIC void garbage_collection(Context *ctx)
{
  struct rusage ru0, ru1;

  /* initialise */
  gc_phase = PHASE_INITIALISE;
  GCLOG("Before Garbage Collection\n");
  if (cputime_flag == TRUE)
    getrusage(RUSAGE_SELF, &ru0);
  property_map_roots = NULL;

  /* mark */
  gc_phase = PHASE_MARK;
  scan_roots(ctx);

  /* weak */
  gc_phase = PHASE_WEAK;
  weak_clear();

  /* sweep */
  gc_phase = PHASE_SWEEP;
  sweep();

  /* finalise */
  gc_phase = PHASE_FINALISE;
  GCLOG("After Garbage Collection\n");
  if (cputime_flag == TRUE) {
    time_t sec;
    suseconds_t usec;

    getrusage(RUSAGE_SELF, &ru1);
    sec = ru1.ru_utime.tv_sec - ru0.ru_utime.tv_sec;
    usec = ru1.ru_utime.tv_usec - ru0.ru_utime.tv_usec;
    if (usec < 0) {
      sec--;
      usec += 1000000;
    }
    gc_sec += sec;
    gc_usec += usec;
  }

  generation++;
  /* printf("Exit gc, generation = %d\n", generation); */

  gc_phase = PHASE_INACTIVE;
}

/*
 * Mark the header
 */
STATIC_INLINE void mark_cell_header(header_t *hdrp)
{
#ifdef GC_DEBUG
  {
    header_t header  = *hdrp;
    header_t *shadow = get_shadow(hdrp);
    header_t sheader = *shadow;
    assert(HEADER0_GET_MAGIC(header) == HEADER0_MAGIC);
    assert(HEADER0_GET_TYPE(header) == HEADER0_GET_TYPE(sheader));
    assert(HEADER0_GET_SIZE(header) - HEADER0_GET_EXTRA(header) ==
           HEADER0_GET_SIZE(sheader) - HEADER0_GET_EXTRA(sheader));
    assert(HEADER0_GET_GEN(header) == HEADER0_GET_GEN(sheader));
  }
#endif /* GC_DEBUG */
  *hdrp |= GC_MARK_BIT;
}

STATIC_INLINE void mark_cell(void *ref)
{
  header_t *hdrp = (header_t *)(((uintptr_t) ref) - HEADER_BYTES);
  mark_cell_header(hdrp);
}

STATIC_INLINE void unmark_cell_header(header_t *hdrp)
{
  *hdrp &= ~GC_MARK_BIT;
}

STATIC_INLINE int is_marked_cell_header(header_t *hdrp)
{
#if HEADER0_GC_OFFSET <= 4 * 8  /* BITS_IN_INT */
  return *hdrp & GC_MARK_BIT;
#else
  return !!(*hdrp & GC_MARK_BIT);
#endif
}

STATIC_INLINE int is_marked_cell(void *ref)
{
  header_t *hdrp = (header_t *)(((uintptr_t) ref) - HEADER_BYTES);
  return is_marked_cell_header(hdrp);
}

STATIC_INLINE int test_and_mark_cell(void *ref)
{
  if (in_js_space(ref)) {
    header_t *hdrp = (header_t *)(((uintptr_t) ref) - HEADER_BYTES);
    if (is_marked_cell_header(hdrp))
      return 1;
    mark_cell_header(hdrp);
  }
  return 0;
}

/*
 * Tracer
 *
 *  process_edge, process_edge_XXX
 *    If the destination node is not marked, mark it and process the
 *    destination node. XXX is specialised version for type XXX.
 *  scan_XXX
 *    Scan static structure XXX.
 *  process_node_XXX
 *    Scan object of type XXX in the heap.  Move it if nencessary.
 */

STATIC void process_edge_JSValue_array(JSValue *p, size_t start, size_t length);
STATIC void process_edge_HashBody(HashCell **p, size_t length);
STATIC void process_node_FunctionFrame(FunctionFrame *p);
STATIC void process_node_Context(Context *p);
STATIC void scan_function_table_entry(FunctionTable *p);
STATIC void scan_stack(JSValue* stack, int sp, int fp);

STATIC void process_edge(uintptr_t ptr)
{
  cell_type_t type;

  if (is_fixnum(ptr) || is_special(ptr))
    return;

  ptr = ptr & ~TAGMASK;

  if (test_and_mark_cell((void *)ptr))
    return;

  type = HEADER0_GET_TYPE(((header_t *) ptr)[-1]);

  /* part of code for processing the node is inlined */
  switch (type) {
  case HTAG_STRING:
  case HTAG_FLONUM:
    return;
  case HTAG_SIMPLE_OBJECT:
    break;
  case HTAG_ARRAY:
    {
      JSObject *p = (JSObject *) ptr;
      JSValue *a_body = get_array_ptr_body(p);
      uint64_t a_length = get_array_ptr_length(p);
      uint64_t a_size = get_array_ptr_size(p);
      size_t len = a_length < a_size ? a_length : a_size;
      if (a_body != NULL)
        /* a_body may be NULL during initialization */
        process_edge_JSValue_array(a_body, 0, len);
      break;
    }
  case HTAG_FUNCTION:
    {
      JSObject *p = (JSObject *) ptr;
      FunctionFrame *frame = get_function_ptr_environment(p);
      /* FunctionTable *ftentry = function_ptr_table_entry(p);
       * scan_function_table_entry(ftentry);
       *    All function table entries are scanned through Context
       */
      process_edge((uintptr_t) frame);
      break;
    }
  case HTAG_BUILTIN:
    break;
  case HTAG_BOXED_NUMBER:
    {
      JSObject *p = (JSObject *) ptr;
      JSValue value = get_number_object_ptr_value(p);
      process_edge((uintptr_t) value);
      break;
    }
  case HTAG_BOXED_STRING:
    {
      JSObject *p = (JSObject *) ptr;
      JSValue value = get_string_object_ptr_value(p);
      process_edge((uintptr_t) value);
      break;
    }
  case HTAG_BOXED_BOOLEAN:
    {
#ifdef DEBUG
      JSObject *p = (JSObject *) ptr;
      JSValue value = get_number_object_ptr_value(p);
      assert(is_boolean(value));
#endif /* DEBUG */
      break;
    }
#ifdef USE_REGEXP
  case HTAG_REGEXP:
    break;
#endif /* USE_REGEXP */
  case HTAG_ITERATOR:
    {
      Iterator *p = (Iterator *) ptr;
      if (p->size > 0)
        process_edge_JSValue_array(p->body, 0, p->size);
      return;
    }
  case HTAG_PROP:
  case HTAG_ARRAY_DATA:
    abort();
  case HTAG_FUNCTION_FRAME:
    process_node_FunctionFrame((FunctionFrame *) ptr);
    return;
  case HTAG_STR_CONS:
    {
      StrCons *p = (StrCons *) ptr;
      /* WEAK: p->str */
      if (p->next != NULL)
        process_edge((uintptr_t) p->next); /* StrCons */
      return;
    }
  case HTAG_CONTEXT:
    process_node_Context((Context *) ptr);
    return;
  case HTAG_STACK:
    abort();
  case HTAG_HASHTABLE:
    {
      HashTable *p = (HashTable *) ptr;
      if (p->body != NULL)
        process_edge_HashBody(p->body, p->size);
      return;
    }
  case HTAG_HASH_BODY:
    abort();
  case HTAG_HASH_CELL:
    {
      HashCell *p = (HashCell *) ptr;
      process_edge(p->entry.key);
#ifndef HC_SKIP_INTERNAL
      /* transition link is weak if HC_SKIP_INTERNAL */
      if (is_transition(p->entry.attr))
        process_edge((uintptr_t) p->entry.data);  /* PropertyMap */
#endif /* HC_SKIP_INTERNAL */
      if (p->next != NULL)
        process_edge((uintptr_t) p->next);  /* HashCell */
      return;
    }
  case HTAG_PROPERTY_MAP:
    {
      PropertyMap *p = (PropertyMap *) ptr;
      process_edge((uintptr_t) p->map); /* HashTable */
#ifndef HC_SKIP_INTERNAL
      if (p->prev != NULL)
        /* weak if HC_SKIP_INTERNAL */
        process_edge((uintptr_t) p->prev); /* PropertyMap */
#endif /* HC_SKIP_INTERNAL */
      if (p->shapes != NULL)
        process_edge((uintptr_t) p->shapes); /* Shape */
      process_edge((uintptr_t) p->__proto__);
      /* collect children of roots for entriy points of weak lists */
      if (p->prev == gconsts.g_property_map_root) {
        struct property_map_roots *e =
          (struct property_map_roots *)
          space_alloc(&js_space, sizeof(struct property_map_roots),
                      HTAG_FREE);
        e->pm = p;
        e->next = property_map_roots;
        property_map_roots = e;
      }
      return;
    }
  case HTAG_SHAPE:
    {
      Shape *p = (Shape *) ptr;
      process_edge((uintptr_t) p->pm);
#ifdef WEAK_SHAPE_LIST
      /* p->next is weak */
#else /* WEAK_SHAPE_LIST */
      if (p->next != NULL)
        process_edge((uintptr_t) p->next);
#endif /* WEAK_SHAPE_LIST */
      return;
    }
  default:
    abort();
  }

  /* common fields and payload of JSObject */
  {
    JSObject *p = (JSObject *) ptr;
    Shape *os = p->shape;
    int n_extension = os->n_extension_slots;
    size_t actual_embedded = os->n_embedded_slots - (n_extension == 0 ? 0 : 1);
    int i;
    /* 1. shape */
    process_edge((uintptr_t) os);
    /* 2. embedded propertyes */
    for (i = os->pm->n_special_props; i < actual_embedded; i++)
      process_edge(p->eprop[i]);
    if (n_extension != 0) {
      /* 3. extension */
      process_edge_JSValue_array((JSValue *) p->eprop[actual_embedded], 0,
                                 os->pm->n_props - actual_embedded);
    }
#ifdef ALLOC_SITE_CACHE
    /* 4. allocation site cache */
    if (p->alloc_site != NULL)
      alloc_site_update_info(p);
#endif /* ALLOC_SITE_CACHE */
  }
}

STATIC void process_edge_JSValue_array(JSValue *p, size_t start, size_t length)
{
  size_t i;
  assert(in_js_space(p));
  if (test_and_mark_cell(p))
    return;
  for (i = start; i < length; i++)
    process_edge((uintptr_t) p[i]);
}

STATIC void process_edge_HashBody(HashCell **p, size_t length)
{
  size_t i;
  assert(in_js_space(p));
  if (test_and_mark_cell(p))
    return;
  for (i = 0; i < length; i++)
    if (p[i] != NULL)
      process_edge((uintptr_t) p[i]);  /* HashCell */
}

STATIC void process_node_FunctionFrame(FunctionFrame *p)
{
  header_t header;
  size_t length;
  size_t i;

  if (p->prev_frame != NULL)
    process_edge((uintptr_t) p->prev_frame); /* FunctionFrame */
  process_edge(p->arguments);
  /* local variables
   *   TODO: no idea about the number of local variables.
   */
  header = ((header_t *) p)[-1];
  length = HEADER0_GET_SIZE(header);
  length -= HEADER_JSVALUES;
  length -= sizeof(FunctionFrame) >> LOG_BYTES_IN_JSVALUE;
  length -= HEADER0_GET_EXTRA(header);
  for (i = 0; i < length; i++)
    process_edge(p->locals[i]);

  assert(p->locals[length - 1] == JS_UNDEFINED);  /* GC_DEBUG (cacary) */
}

STATIC void process_node_Context(Context *context)
{
  int i;

  process_edge((uintptr_t) context->global);
  /* function table is a static data structure
   *   Note: spreg.cf points to internal address of the function table.
   */
  for (i = 0; i < FUNCTION_TABLE_LIMIT; i++)
    scan_function_table_entry(&context->function_table[i]);
  process_edge((uintptr_t) context->spreg.lp);  /* FunctionFrame */
  process_edge((uintptr_t) context->spreg.a);
  process_edge((uintptr_t) context->spreg.err);
  process_edge((uintptr_t) context->exhandler_stack);
  process_edge((uintptr_t) context->lcall_stack);

  /* process stack */
  assert(!is_marked_cell(context->stack));
  mark_cell(context->stack);
  scan_stack(context->stack, context->spreg.sp, context->spreg.fp);
}

STATIC void scan_function_table_entry(FunctionTable *p)
{
  /* trace constant pool */
  {
    JSValue *constant_pool = (JSValue *) &p->insns[p->n_insns];
    size_t n_constants = p->n_constants;
    size_t i;
    for (i = 0; i < n_constants; i++)
      process_edge(constant_pool[i]);
  }

#ifdef ALLOC_SITE_CACHE
  /* scan Allocation Sites */
  {
    size_t i;
    for (i = 0; i < p->n_insns; i++) {
      Instruction *insn = &p->insns[i];
      AllocSite *alloc_site = &insn->alloc_site;
      if (alloc_site->shape != NULL)
        process_edge((uintptr_t) alloc_site->shape);
      /* TODO: too eary PM sacnning. scan after updating alloc site info */
      if (alloc_site->pm != NULL)
        process_edge((uintptr_t) alloc_site->pm);
    }
  }
#endif /* ALLOC_SITE_CACHE */

#ifdef INLINE_CACHE
  /* scan Inline Cache */
  {
    size_t i;
    for (i = 0; i < p->n_insns; i++) {
      Instruction *insn = &p->insns[i];
      InlineCache *ic = &insn->inl_cache;
      if (ic->shape != NULL) {
        process_edge((uintptr_t) ic->shape);
        process_edge((uintptr_t) ic->prop_name);
      }
    }
  }
#endif /* INLINE_CACHE */
}

STATIC void scan_stack(JSValue* stack, int sp, int fp)
{
  while (1) {
    while (sp >= fp) {
      process_edge((uintptr_t) stack[sp]);
      sp--;
    }
    if (sp < 0)
      return;
    fp = stack[sp--];                                     /* FP */
    process_edge((uintptr_t) stack[sp]);/*FunctionFrame*/ /* LP */
    sp--;
    sp--;                                                 /* PC */
    /* scan_function_table_entry((FunctionTable *) stack[sp--]); <== CF
     * All function table entries are scanned through Context
     */
    sp--;                                                 /* CF */
  }
}

STATIC void scan_string_table(StrTable *p)
{
  StrCons **vec = p->obvector;
  size_t length = p->size;
  size_t i;

  for (i = 0; i < length; i++)
    if (vec[i] != NULL)
      process_edge((uintptr_t) vec[i]); /* StrCons */
}

STATIC void scan_roots(Context *ctx)
{
  struct global_constant_objects *gconstsp = &gconsts;
  JSValue* p;
  int i;

  /*
   * global variables
   */
  for (p = (JSValue *) gconstsp; p < (JSValue *) (gconstsp + 1); p++)
    process_edge((uintptr_t) *p);

  /* function table: do not trace.
   *                 Used slots should be traced through Function objects
   */

  /* string table */
  scan_string_table(&string_table);

  /*
   * Context
   */
  process_edge((uintptr_t) ctx); /* Context */

  /*
   * GC_PUSH'ed
   */
  for (i = 0; i < gc_root_stack_ptr; i++)
    process_edge(*(uintptr_t*) gc_root_stack[i]);
}

/*
 * Clear pointer field to StringCell whose mark bit is not set.
 * Unlink the StrCons from the string table.  These StrCons's
 * are collected in the next collection cycle.
 */
STATIC void weak_clear_StrTable(StrTable *table)
{
  size_t i;
  for (i = 0; i < table->size; i++) {
    StrCons ** p = table->obvector + i;
    while (*p != NULL) {
      StringCell *cell = remove_normal_string_tag((*p)->str);
      if (!is_marked_cell(cell)) {
        (*p)->str = JS_UNDEFINED;
        *p = (*p)->next;
      } else
        p = &(*p)->next;
    }
  }
}

#ifdef WEAK_SHAPE_LIST
void weak_clear_shape_recursive(PropertyMap *pm)
{
  Shape **p;
  HashIterator iter;
  HashCell *cell;

#ifdef VERBOSE_GC_SHAPE
#defien PRINT(x...) printf(x)
#else /* VERBOSE_GC_SHAPE */
#define PRINT(x...)
#endif /* VERBOSE_GC_SHAPE */
  
  for (p = &pm->shapes; *p != NULL; ) {
    Shape *os = *p;
    if (is_marked_cell(os))
      p = &(*p)->next;
    else {
      Shape *skip = *p;
      *p = skip->next;
#ifdef DEBUG
      PRINT("skip %p emp: %d ext: %d\n",
            skip, skip->n_embedded_slots, skip->n_extension_slots);
      skip->next = NULL;  /* avoid Black->While check failer */
#endif /* DEBUG */
    }
  }

  iter = createHashIterator(pm->map);
  while (nextHashCell(pm->map, &iter, &cell) != FAIL)
    if (is_transition(cell->entry.attr))
      weak_clear_shape_recursive((PropertyMap *) cell->entry.data);

#undef PRINT /* VERBOSE_GC_SHAPE */
}

STATIC void weak_clear_shapes()
{
  struct property_map_roots *e;
  for (e = property_map_roots; e != NULL; e = e->next)
    weak_clear_shape_recursive(e->pm);
}
#endif /* WEAK_SHAPE_LIST */

#ifdef HC_SKIP_INTERNAL
/*
 * Get the only transision from internal node.
 */
static PropertyMap* get_transition_dest(PropertyMap *pm)
{
  HashIterator iter;
  HashCell *p;

  iter = createHashIterator(pm->map);
  while(nextHashCell(pm->map, &iter, &p) != FAIL)
    if (is_transition(p->entry.attr)) {
      PropertyMap *ret = (PropertyMap *) p->entry.data;
#ifdef GC_DEBUG
      while(nextHashCell(pm->map, &iter, &p) != FAIL)
        assert(!is_transition(p->entry.attr));
#endif /* GC_DEBUG */
      return ret;
    }
  abort();
  return NULL;
}

static void weak_clear_property_map_recursive(PropertyMap *pm)
{
  HashIterator iter;
  HashCell *p;

  assert(is_marked_cell(pm));

  iter = createHashIterator(pm->map);
  while(nextHashCell(pm->map, &iter, &p) != FAIL)
    if (is_transition(p->entry.attr)) {
      PropertyMap *next = (PropertyMap *) p->entry.data;
      /*
       * If the next node is both
       *   1. not pointed to through strong pointers and
       *   2. outgoing edge is exactly 1,
       * then, the node is an internal node.
       */
      while (!is_marked_cell(next) && next->n_transitions == 1) {
#ifdef VERBOSE_WEAK
        printf("skip PropertyMap %p\n", next);
#endif /* VERBOSE_WEAK */
        next = (PropertyMap *) get_transition_dest(next);
      }
#ifdef VERBOSE_WEAK
      if (is_marked_cell(next))
        printf("preserve PropertyMap %p because it has been marked\n", next);
      else
        printf("preserve PropertyMap %p because it is a branch (P=%d T=%d)\n",
               next, next->n_props, next->n_transitions);
#endif /* VERBOSE_WEAK */
      /* Resurrect if it is branching node or terminal node */
      if (!is_marked_cell(next))
        process_edge((uintptr_t) next);
      p->entry.data = (HashData) next;
      next->prev = pm;
      weak_clear_property_map_recursive(next);
    }
}

STATIC void weak_clear_property_maps()
{
  struct property_map_roots *e;
  for (e = property_map_roots; e != NULL; e = e->next)
    weak_clear_property_map_recursive(e->pm);
}
#endif /* HC_SKIP_INTERNAL */

#ifdef HC_PROF

#endif /* HC_PROF */

STATIC void weak_clear(void)
{
#ifdef HC_SKIP_INTERNAL
  /* !!! Do weak_clear_property_map first. This may resurrect some objects. */
  weak_clear_property_maps();
#endif /* HC_SKIP_INTERNAL */
#ifdef WEAK_SHAPE_LIST
  weak_clear_shapes();
#endif /* WEAK_SHAPE_LIST */
  weak_clear_StrTable(&string_table);
  property_map_roots = NULL;
#ifdef HC_PROF
  /*  weak_clear_hcprof_entrypoints(); */
#endif /* HC_PROF */
}

STATIC void sweep_space(struct space *space)
{
  struct free_chunk **p;
  uintptr_t scan = space->addr;
  uintptr_t free_bytes = 0;

  GCLOG_SWEEP("sweep %s\n", space->name);

  space->freelist = NULL;
  p = &space->freelist;
  while (scan < space->addr + space->bytes) {
    uintptr_t last_used = 0;
    uintptr_t free_start;
    /* scan used area */
    while (scan < space->addr + space->bytes &&
           is_marked_cell_header((void *) scan)) {
      header_t header = *(header_t *) scan;
      size_t size = HEADER0_GET_SIZE(header);
#ifdef GC_DEBUG
      assert(HEADER0_GET_MAGIC(header) == HEADER0_MAGIC);
#endif /* GC_DEBUG */
#ifdef GC_PROF
      {
        cell_type_t type = HEADER0_GET_TYPE(header);
        size_t net_size =
          (size - HEADER0_GET_EXTRA(header)) << LOG_BYTES_IN_JSVALUE;
        pertype_live_bytes[type]+= net_size;
        pertype_live_count[type]++;
      }
#endif /* GC_PROF */
      unmark_cell_header((void *) scan);
      last_used = scan;
      scan += size << LOG_BYTES_IN_JSVALUE;
    }
    free_start = scan;
    while (scan < space->addr + space->bytes &&
           !is_marked_cell_header((void *) scan)) {
      uint64_t header = *(uint64_t *) scan;
      uint32_t size = HEADER0_GET_SIZE(header);
#ifdef GC_DEBUG
      assert(HEADER0_GET_MAGIC(header) == HEADER0_MAGIC);
#endif /* GC_DEBUG */
      scan += size << LOG_BYTES_IN_JSVALUE;
    }
    if (free_start < scan) {
      if (last_used != 0) {
        uint64_t last_header = *(uint64_t *) last_used;
        uint32_t extra = HEADER0_GET_EXTRA(last_header);
        uint32_t size = HEADER0_GET_SIZE(last_header);
        free_start -= extra << LOG_BYTES_IN_JSVALUE;
        size -= extra;
        HEADER0_SET_SIZE(*(uint64_t *) last_used, size);
        HEADER0_SET_EXTRA(*(uint64_t *) last_used, 0);
      }
      if (scan - free_start >=
          MINIMUM_FREE_CHUNK_JSVALUES << LOG_BYTES_IN_JSVALUE) {
        struct free_chunk *chunk = (struct free_chunk *) free_start;
        GCLOG_SWEEP("add_cunk %x - %x (%d)\n",
                    free_start - space->addr, scan - space->addr,
                    scan - free_start);
#ifdef GC_DEBUG
        memset(chunk, 0xcc, scan - free_start);
#endif /* GC_DEBUG */
        chunk->header =
          HEADER0_COMPOSE((scan - free_start) >> LOG_BYTES_IN_JSVALUE,
                          0, HTAG_FREE);
#ifdef GC_DEBUG
        HEADER0_SET_MAGIC(chunk->header, HEADER0_MAGIC);
#endif /* GC_DEBUG */
        *p = chunk;
        p = &chunk->next;
        free_bytes += scan - free_start;
      } else  {
        *(header_t *) free_start =
          HEADER0_COMPOSE((scan - free_start) >> LOG_BYTES_IN_JSVALUE,
                          0, HTAG_FREE);
#ifdef GC_DEBUG
        HEADER0_SET_MAGIC(*(header_t *) free_start, HEADER0_MAGIC);
#endif /* GC_DEBUG */
      }
    }
  }
  (*p) = NULL;
  space->free_bytes = free_bytes;
}


STATIC void sweep(void)
{
#ifdef GC_DEBUG
  sanity_check();
  check_invariant();
#endif /* GC_DEBUG */
  sweep_space(&js_space);
}

#ifdef ALLOC_SITE_CACHE
STATIC PropertyMap *find_lub(PropertyMap *a, PropertyMap *b)
{
  while(a != b) {
    if (a->n_props < b->n_props)
      b = b->prev;
    else
      a = a->prev;
  }
  return a;
}

STATIC void alloc_site_update_info(JSObject *p)
{
  AllocSite *as = p->alloc_site;
  PropertyMap *pm = p->shape->pm;

  assert(as != NULL);

  /* likely case */
  if (as->pm == pm)
    return;

  if (as->pm == NULL) {
    /* 1. If the site is empty, cache this. */
    as->pm = pm;
    assert(as->shape == NULL);
  } else {
    /* 2. Otherwise, compute LUB.
     *
     *   LUB       monomorphic   polymorphic
     *   pm        mono:as->pm   poly:as->pm
     *   as->pm    mono:pm       poly:as->pm
     *   less      poly:LUB      poly:LUB
     */
    PropertyMap *lub = find_lub(pm, as->pm);
    if (lub == as->pm) {
      if (as->polymorphic)
        /* keep current as->pm */ ;
      else {
        as->pm = pm;
        as->shape = NULL;
      }
    } else if (lub == pm)
      /* keep current as->pm */  ;
    else {
      as->polymorphic = 1;
      as->pm = lub;
      as->shape = NULL;
    }
  }
}
#endif /* ALLOC_SITE_CACHE */

#ifdef GC_DEBUG
#define OFFSET_OF(T, F) (((uintptr_t) &((T *) 0)->F) >> LOG_BYTES_IN_JSVALUES)

STATIC void check_invariant_nobw_space(struct space *space)
{
  uintptr_t scan = space->addr;

  while (scan < space->addr + space->bytes) {
    header_t *hdrp = (header_t *) scan;
    header_t header = *hdrp;
    JSValue *payload = (JSValue *)(((header_t *) scan) + 1);
    switch (HEADER0_GET_TYPE(header)) {
    case HTAG_STRING:
    case HTAG_FLONUM:
    case HTAG_ARRAY_DATA:
    case HTAG_CONTEXT:
    case HTAG_STACK:
    case HTAG_HIDDEN_CLASS:
    case HTAG_HASHTABLE:
    case HTAG_HASH_CELL:
      break;
    case HTAG_PROPERTY_MAP:
      {
        PropertyMap *pm = (PropertyMap *) payload;
        Shape *os;
        for (os = pm->shapes; os != NULL; os = os->next)
          assert(HEADER0_GET_TYPE(((JSValue *) os)[-1]) == HTAG_SHAPE);
        goto DEFAULT;
      }
    default:
    DEFAULT:
      if (is_marked_cell_header(hdrp)) {
        /* this object is black; should not contain a pointer to white */
        size_t payload_jsvalues =
          HEADER0_GET_SIZE(header)
          - HEADER_JSVALUES
          - HEADER0_GET_EXTRA(header);
        size_t i;
        for (i = 0; i < payload_jsvalues; i++) {
          JSValue x = payload[i];
          /* weak pointers */
          /*
          if (HEADER0_GET_TYPE(header) == HTAG_STR_CONS) {
            if (i == OFFSET_OF(StrCons, str))
              continue;
          }
          */
          if (in_js_space((void *)(x & ~7))) {
            assert(is_marked_cell((void *) (x & ~7)));
          }
        }
      }
      break;
    }
    scan += HEADER0_GET_SIZE(header) << LOG_BYTES_IN_JSVALUE;
  }
}

STATIC void check_invariant(void)
{
  check_invariant_nobw_space(&js_space);
}


STATIC void print_memory_status(void)
{
  GCLOG("  gc_disabled = %d\n", gc_disabled);
  GCLOG("  js_space.free_bytes = %d\n", js_space.free_bytes);
}

STATIC void print_heap_stat(void)
{
  size_t jsvalues[17] = {0, };
  size_t number[17] = {0, };
  uintptr_t scan = js_space.addr;
  size_t i;

  while (scan < js_space.addr + js_space.bytes) {
    header_t header = *(header_t *) scan;
    cell_type_t type = HEADER0_GET_TYPE(header);
    size_t size = HEADER0_GET_SIZE(header);
    if (type != HTAG_FREE) {
      jsvalues[type] += size;
      number[type] ++;
    }
    scan += (size << LOG_BYTES_IN_JSVALUE);
  }

  for (i = 0; i < 17; i++) {
    printf("type %02zu: num = %08zu volume = %08zu\n", i, number[i], jsvalues[i]);
  }
}

extern void** stack_start;
STATIC void sanity_check()
{
}
#endif /* GC_DEBUG */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
