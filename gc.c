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
#define STAITC_INLINE static inline
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

/*
 * naming convention
 *   name for size: add a surfix representing the unit
 *                    bytes: in bytes
 *                    jsvalues: in the numberof JSValue's
 */

#ifndef JS_SPACE_BYTES
#define JS_SPACE_BYTES     (10 * 1024 * 1024)
#endif
#ifdef EXCESSIVE_GC
#define JS_SPACE_GC_THREASHOLD     (JS_SPACE_BYTES >> 4)
#else  /* EXCESSIVE_GC */
#define JS_SPACE_GC_THREASHOLD     (JS_SPACE_BYTES >> 1)
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

struct property_map_weak_list {
  PropertyMap *pm;
  struct property_map_weak_list *next;
};

/*
 * variables
 */
STATIC struct space js_space;
#ifdef GC_DEBUG
STATIC struct space debug_js_shadow;
#endif /* GC_DEBUG */

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
#endif /* GC_PROF */

#ifdef GC_DEBUG
STATIC void sanity_check();
#endif /* GC_DEBUG */

#ifdef HC_PROF
int traversing_weaks;
#endif /* HC_PROF */

struct property_map_weak_list *property_map_weak_list;

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

void init_memory()
{
  create_space(&js_space, JS_SPACE_BYTES, "js_space");
#ifdef GC_DEBUG
  create_space(&debug_js_shadow, JS_SPACE_BYTES, "debug_js_shadow");
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

cell_type_t gc_obj_header_type(void *p)
{
  header_t *hdrp = ((header_t *) p) - 1;
  return HEADER0_GET_TYPE(*hdrp);
}

STATIC int check_gc_request(Context *ctx)
{
  if (ctx == NULL) {
    if (js_space.free_bytes < JS_SPACE_GC_THREASHOLD)
      GCLOG_TRIGGER("Needed gc for js_space -- cancelled: ctx == NULL\n");
    return 0;
  }
  if (gc_disabled) {
    if (js_space.free_bytes < JS_SPACE_GC_THREASHOLD)
      GCLOG_TRIGGER("Needed gc for js_space -- cancelled: GC disabled\n");
    return 0;
  }
  if (js_space.free_bytes < JS_SPACE_GC_THREASHOLD)
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

  /* printf("Enter gc, generation = %d\n", generation); */
  GCLOG("Before Garbage Collection\n");
  /* print_memory_status(); */
  if (cputime_flag == TRUE) getrusage(RUSAGE_SELF, &ru0);

#ifdef HC_PROF
  traversing_weaks = FALSE;
#endif /* HC_PROF */
  property_map_weak_list = NULL;
  scan_roots(ctx);
#ifdef HC_PROF
  traversing_weaks = TRUE;
#endif /* HC_PROF */
  weak_clear();
#ifdef HC_PROF
  traversing_weaks = FALSE;
#endif /* HC_PROF */
  sweep();

  GCLOG("After Garbage Collection\n");
  /* print_memory_status(); */
  /* print_heap_stat(); */

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
      JSValue *a_body = array_ptr_body(p);
      uint64_t a_length = array_ptr_length(p);
      uint64_t a_size = array_ptr_size(p);
      size_t len = a_length < a_size ? a_length : a_size;
      if (a_body != NULL)
        /* a_body may be NULL during initialization */
        process_edge_JSValue_array(a_body, 0, len);
      break;
    }
  case HTAG_FUNCTION:
    {
      JSObject *p = (JSObject *) ptr;
      FunctionFrame *frame = function_ptr_environment(p);
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
      JSValue value = number_object_ptr_value(p);
      process_edge((uintptr_t) value);
      break;
    }
  case HTAG_BOXED_STRING:
    {
      JSObject *p = (JSObject *) ptr;
      JSValue value = string_object_ptr_value(p);
      process_edge((uintptr_t) value);
      break;
    }
  case HTAG_BOXED_BOOLEAN:
    {
#ifdef DEBUG
      JSObject *p = (JSObject *) ptr;
      JSValue value = number_object_ptr_value(p);
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
        struct property_map_weak_list *e =
          (struct property_map_weak_list *)
          space_alloc(&js_space, sizeof(struct property_map_weak_list),
                      HTAG_FREE);
        e->pm = p;
        e->next = property_map_weak_list;
        property_map_weak_list = e;
      }
      return;
    }
  case HTAG_SHAPE:
    {
      Shape *p = (Shape *) ptr;
      process_edge((uintptr_t) p->pm);
      /* p->next is weak */
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
    for (i = 0; i < ptr->n_insns; i++) {
      Instruction *insn = &ptr->insns[i];
      AllocSite *alloc_site = &insn->alloc_site;
      if (alloc_site->hc != NULL)
        trace_HiddenClass(&alloc_site->hc);
      if (alloc_site->preformed_hc != NULL)
        trace_HiddenClass(&alloc_site->preformed_hc);
    }
  }
#endif /* ALLOC_SITE_CACHE */
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


#ifdef ALLOC_SITE_CACHE
STATIC HiddenClass *find_lub(HiddenClass *a, HiddenClass *b)
{
  HiddenClass *p;
  int alen = 0;
  int blen = 0;

  for (p = a; p != NULL; p = p->prev, alen++)
    if (p == b)
      return p;
  for (p = b; p != NULL; p = p->prev, blen++)
    if (p == a)
      return p;
  while (alen > blen) {
    a = a->prev;
    alen--;
  }
  while (alen < blen) {
    b = b->prev;
    blen--;
  }
  while (a != b) {
    a = a->prev;
    b = b->prev;
  }
  return a;
}

STATIC void alloc_site_update_hc(AllocSite *alloc_site, HiddenClass *hc)
{
  alloc_site->hc = hc;
  alloc_site->preformed_hc = NULL;
}

STATIC void alloc_site_cache(Object *obj)
{
  /* feedback hidden class statistics to allocation sites. */
  if (obj->alloc_site != NULL) {
    HiddenClass *obj_hc = obj->klass;
    if (hidden_base(obj_hc) != NULL)
      obj_hc = hidden_base(obj_hc);
    if (obj->alloc_site->polymorphic) {
      if (obj->alloc_site->hc != NULL)
        alloc_site_update_hc(obj->alloc_site,
                             find_lub(obj_hc, obj->alloc_site->hc));
    } else {
      if (obj->alloc_site->hc == NULL)
        alloc_site_update_hc(obj->alloc_site, obj_hc);
      else {
        HiddenClass *p;
        /* check monomorphism */
        for (p = obj->alloc_site->hc; p != NULL; p = p->prev)
          if (p == obj_hc)
            break;
        if (p == NULL) {
          for (p = obj_hc; p != NULL; p = p->prev)
            if (p == obj->alloc_site->hc) {
              alloc_site_update_hc(obj->alloc_site, obj_hc);
              break;
            }
        }
        if (p == NULL) {
          obj->alloc_site->polymorphic = 1;
          alloc_site_update_hc(obj->alloc_site,
                               find_lub(obj_hc, obj->alloc_site->hc));
        }
      }
    }
  }
}
#endif /* ALLOC_SITE_CACHE */

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

void weak_clear_shape_recursive(PropertyMap *pm)
{
  Shape **p;
  HashIterator iter;
  HashCell *cell;

  printf("weak_clear_shape %p\n", pm);

  for (p = &pm->shapes; *p != NULL; ) {
    Shape *os = *p;
    if (is_marked_cell(os))
      p = &(*p)->next;
    else {
      Shape *skip = *p;
      *p = skip->next;
#ifdef DEBUG
      printf("skip %p emp: %d ext: %d\n", skip, skip->n_embedded_slots, skip->n_extension_slots);
      skip->next = NULL;  /* avoid Black->While check failer */
#endif /* DEBUG */
    }
  }

  iter = createHashIterator(pm->map);
  while (nextHashCell(pm->map, &iter, &cell) != FAIL)
    if (is_transition(cell->entry.attr))
      weak_clear_shape_recursive((PropertyMap *) cell->entry.data);
}

void weak_clear_shapes()
{
  PropertyMap *pm = gconsts.g_property_map_root;
  struct property_map_weak_list *e;
  weak_clear_shape_recursive(pm);
  for (e = property_map_weak_list; e != NULL; e = e->next)
    weak_clear_shape_recursive(e->pm);
  property_map_weak_list = NULL;
}

#ifdef HC_PROF
extern HiddenClass *hcprof_entrypoints[];
extern int hcprof_n_entrypoints;
STATIC void weak_clear_hcprof_entrypoints()
{
  int i;
  for (i = 0; i < hcprof_n_entrypoints; i++) {
    HiddenClass *hc = hcprof_entrypoints[i];
    if (!is_marked_cell(hc))
      trace_HiddenClass(&hcprof_entrypoints[i]);
  }
}
#endif /* HC_PROF */

STATIC void weak_clear(void)
{
  weak_clear_StrTable(&string_table);
  weak_clear_shapes();
#ifdef HC_PROF
  weak_clear_hcprof_entrypoints();
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
