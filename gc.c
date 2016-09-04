#include <stdlib.h>
#include <stdio.h>
#include "prefix.h"
#define EXTERN extern
#include "header.h"

/*
 * defined in header.h
 */
//typedef uint64_t JSValue;
#define LOG_BYTES_IN_JSVALUE   3
//#define BYTES_IN_JSVALUE       (1 << LOG_BYTES_IN_JSVALUE)

/*
 * naming convention
 *   name for size: add a surfix representing the unit
 *                    bytes: in bytes
 *                    jsvalues: in the numberof JSValue's
 */

#define JS_SPACE_BYTES     (1024 * 1024)
#define MALLOC_SPACE_BYTES (1024 * 1024)
#define JS_SPACE_GC_THREASHOLD     (JS_SPACE_BYTES >> 4)
#define MALLOC_SPACE_GC_THREASHOLD (MALLOC_SPACE_BYTES >> 4)

/*
 * If the remaining room is smaller than a certain size,
 * we do not use the remainder for efficiency.  Rather,
 * we add it below the chunk being allocated.  In this case,
 * the size in the header includes the extra words.
 */
#define MINIMUM_FREE_CHUNK_JSVALUES 4

#define MKMASK(l, o, b) \
  ((((uint64_t) -1) << ((l) - (b))) >> ((l) - (o) - (b)))

/*
 * Chunk header layout
 *  HEADER0
 *    bit 0 - 7  :  type (HTAG_xxx)
 *                    4 - 14 : JSValue types
 *                    15     : malloced memory (HTAG_MALLOC)
 *                    16     : free (HTAG_FREE)
 *    bit 8      : mark bit
 *    bit 32 - 63: size (in number of JSValue's)
 */
#define HEADER_JSVALUES       1
#define HEADER_BYTES (HEADER_JSVALUES << LOG_BYTES_IN_JSVALUE)

#define HEADER0_BITS          64
#define HEADER0_TYPE_OFFSET   0
#define HEADER0_TYPE_BITS     8
#define HEADER0_GC_OFFSET     8
#define HEADER0_GC_BITS       1
#define HEADER0_SIZE_OFFSET   32
#define HEADER0_SIZE_BITS     32
#define HEADER0_TYPE_MASK \
  MKMASK(HEADER0_BITS, HEADER0_TYPE_OFFSET, HEADER0_TYPE_BITS)
#define HEADER0_GC_MASK \
  MKMASK(HEADER0_BITS, HEADER0_GC_OFFSET, HEADER0_GC_BITS)
#define HEADER0_SIZE_MASK					\
  MKMASK(HEADER0_BITS, HEADER0_SIZE_OFFSET, HEADER0_SIZE_BITS)
//#define HEADER0_SIZE_MASK 0xffffffff00000000LLU

/* accessor to HEADER0 */
#define HEADER0_SET(hdr, val, off, msk)			\
  ((hdr) = ((((uint64_t) (val)) << (off)) | ((hdr) & ~(msk))))
#define HEADER0_GET(hdr, off, msk) \
  (((uint64_t) ((hdr) & (msk))) >> (off))
#define HEADER0_SET_TYPE(hdr, val) \
  HEADER0_SET(hdr, val, HEADER0_TYPE_OFFSET, HEADER0_TYPE_MASK)
#define HEADER0_GET_TYPE(hdr) \
  HEADER0_GET(hdr, HEADER0_TYPE_OFFSET, HEADER0_TYPE_MASK)
#define HEADER0_SET_GC(hdr, val) \
  HEADER0_SET(hdr, val, HEADER0_GC_OFFSET, HEADER0_GC_MASK)
#define HEADER0_GET_GC(hdr) \
  HEADER0_GET(hdr, HEADER0_GC_OFFSET, HEADER0_GC_MASK)
#define HEADER0_SET_SIZE(hdr, val) \
  HEADER0_SET(hdr, val, HEADER0_SIZE_OFFSET, HEADER0_SIZE_MASK)
#define HEADER0_GET_SIZE(hdr) \
  HEADER0_GET(hdr, HEADER0_SIZE_OFFSET, HEADER0_SIZE_MASK)
#define HEADER0_COMPOSE(size, type) \
  ((((uint64_t)(size)) << HEADER0_SIZE_OFFSET) | \
   ((uint64_t)(type)) << HEADER0_TYPE_OFFSET)

/* 
 * header tag
 */
#define HTAG_MALLOC        (0x0f)
#define HTAG_FREE          (0x10)

struct free_chunk {
  uint64_t header;
  struct free_chunk *next;
};

/*
 *  Space
 */

struct space {
  uintptr_t addr;
  uintptr_t bytes;
  uintptr_t free_bytes;
  struct free_chunk* freelist;
};

/*
 * GC
 */
#define GC_MARK_BIT (1 << HEADER0_GC_OFFSET)


/*
 * prototype
 */
static void create_space(struct space *space, uintptr_t bytes);
static void* do_malloc(uintptr_t request_bytes);
static JSValue* do_jsalloc(uintptr_t request_bytes, uint32_t type);
static int check_gc_request(void);

static void garbage_collect(void);
static void trace_JSValue_array(JSValue **, uint32_t);
static void trace_slot(JSValue* ptr);
static void scan_roots(void);

static void print_memory_status(void);

/*
 * variables
 */

static struct space js_space;
static struct space debug_js_shadow;
static struct space malloc_space;
static struct space debug_malloc_shadow;
static int gc_disabled = 1;

static void create_space(struct space *space, uintptr_t bytes)
{
  struct free_chunk *p;
  p = (struct free_chunk *) malloc(bytes);
  p->header = HEADER0_COMPOSE(bytes >> LOG_BYTES_IN_JSVALUE, HTAG_FREE);
  p->next = NULL;
  space->addr = (uintptr_t) p;
  space->bytes = bytes;
  space->free_bytes = bytes;
  space->freelist = p;
}

void init_memory()
{
  create_space(&js_space, JS_SPACE_BYTES);
  create_space(&malloc_space, MALLOC_SPACE_BYTES);
  create_space(&debug_js_shadow, JS_SPACE_BYTES);
  create_space(&debug_malloc_shadow, MALLOC_SPACE_BYTES);
  gc_disabled = 0;
}

/*
 * Returns a pointer to the first address of the memory area
 * available to the VM.  The header precedes the area.
 * The header has the size of the chunk including the header,
 * the area available to the VM, and extra bytes if any.
 * Other header bits are zero
 */
static void* do_malloc(uintptr_t request_bytes)
{
  uint32_t  alloc_jsvalues;
  struct free_chunk **p;
  
  alloc_jsvalues =
    (request_bytes + BYTES_IN_JSVALUE - 1) >> LOG_BYTES_IN_JSVALUE;
  alloc_jsvalues += HEADER_JSVALUES;

  /* allocate from freelist */
  for (p = &malloc_space.freelist; *p != NULL; p = &(*p)->next) {
    struct free_chunk *chunk = *p;
    uint32_t chunk_jsvalues = HEADER0_GET_SIZE(chunk->header);
    if (chunk_jsvalues >= alloc_jsvalues) {
      if (chunk_jsvalues >= alloc_jsvalues + MINIMUM_FREE_CHUNK_JSVALUES) {
	/* This chunk is large enough to leave a part unused.  Split it */
	uint32_t new_chunk_jsvalues = chunk_jsvalues - alloc_jsvalues;
	uintptr_t addr =
	  ((uintptr_t) chunk) + (new_chunk_jsvalues << LOG_BYTES_IN_JSVALUE);
	HEADER0_SET_SIZE(chunk->header, new_chunk_jsvalues);
	*(uint64_t *) addr = HEADER0_COMPOSE(alloc_jsvalues, 0);
	malloc_space.free_bytes -= alloc_jsvalues << LOG_BYTES_IN_JSVALUE;
	return (void *) (addr + HEADER_BYTES);
      } else {
	/* This chunk is too small to split. */
	*p = (*p)->next;
	chunk->header = HEADER0_COMPOSE(chunk_jsvalues, 0);
	malloc_space.free_bytes -= chunk_jsvalues << LOG_BYTES_IN_JSVALUE;
	return (void *) (((uintptr_t) chunk) + HEADER_BYTES);
      }
    }
  }

  return NULL;
}

/*
 * request_jsvalues: the number of JSValue's including the object header.
 */
static JSValue* do_jsalloc(uintptr_t request_bytes, uint32_t type)
{
  struct free_chunk **p;
  uint32_t alloc_jsvalues;

  alloc_jsvalues =
    (request_bytes + BYTES_IN_JSVALUE - 1) >> LOG_BYTES_IN_JSVALUE;

  for (p = &js_space.freelist; *p != NULL; p = &(*p)->next) {
    struct free_chunk *chunk = *p;
    uint32_t chunk_jsvalues = HEADER0_GET_SIZE(chunk->header);
    if (chunk_jsvalues >= alloc_jsvalues) {
      if (chunk_jsvalues >= alloc_jsvalues + MINIMUM_FREE_CHUNK_JSVALUES) {
	/* This chunk is large enough to leave a part unused.  Split it */
	uint32_t new_chunk_jsvalues = chunk_jsvalues - alloc_jsvalues;
	uintptr_t addr =
	  ((uintptr_t) chunk) + (new_chunk_jsvalues << LOG_BYTES_IN_JSVALUE);
	HEADER0_SET_SIZE(chunk->header, new_chunk_jsvalues);
	*(uint64_t *) addr = HEADER0_COMPOSE(alloc_jsvalues, type);
	js_space.free_bytes -= alloc_jsvalues << LOG_BYTES_IN_JSVALUE;
	return (JSValue *) addr;
      } else {
	/* This chunk is too small to split. */
	*p = (*p)->next;
	chunk->header &= HEADER0_COMPOSE(chunk_jsvalues, type);
	js_space.free_bytes -= chunk_jsvalues << LOG_BYTES_IN_JSVALUE;
	return (JSValue *) chunk;
      }
    }
  }

  LOG_EXIT("run out of memory for JS object");
  return NULL;
}

static int check_gc_request(void)
{
  if (gc_disabled)
    return 0;
  if (js_space.free_bytes < JS_SPACE_GC_THREASHOLD)
    return 1;
  if (malloc_space.free_bytes < MALLOC_SPACE_GC_THREASHOLD)
    return 1;
  return 0;
}

void* gc_malloc(uintptr_t request_bytes)
{
  void * addr;
  if (check_gc_request())
    garbage_collect();
  addr = do_malloc(request_bytes);
  {
    uintptr_t a = (uintptr_t) addr;
    uintptr_t off = a - malloc_space.addr - HEADER_BYTES;
    uint64_t *shadow = (uint64_t *) (debug_malloc_shadow.addr + off);
    *shadow = 0x1;
  }
  printf("gc_malloc: req %x bytes => %p\n", request_bytes, addr);
  return addr;
}

JSValue* gc_jsalloc(uintptr_t request_bytes, uint32_t type)
{
  JSValue *addr;
  if (check_gc_request())
    garbage_collect();
  addr = do_jsalloc(request_bytes, type);
  {
    uintptr_t a = (uintptr_t) addr;
    uintptr_t off = a - js_space.addr;
    uint64_t *shadow = (uint64_t *) (debug_js_shadow.addr + off);
    *shadow = 0x1;
  }
  printf("gc_jsalloc: req %x bytes type %d => %p\n", request_bytes, type, addr);
  return addr;
}

void disable_gc(void)
{
  gc_disabled = 1;
}

void enable_gc(void)
{
  gc_disabled = 0;
}

static int in_js_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return (js_space.addr <= addr && addr <= js_space.addr + js_space.bytes);
}

static int in_malloc_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return (malloc_space.addr <= addr &&
	  addr <= malloc_space.addr + malloc_space.bytes);
}

static void garbage_collect(void)
{
  printf("GC: Not Implemented\n");
  print_memory_status();

  scan_roots();
}

static uint64_t *get_shadow(void *ptr)
{
  if (in_js_space(ptr)) {
    uintptr_t a = (uintptr_t) ptr;
    uintptr_t off = a - js_space.addr;
    return (uint64_t *) (debug_js_shadow.addr + off);
  } else if (in_malloc_space(ptr)) {
    uintptr_t a = (uintptr_t) ptr;
    uintptr_t off = a - malloc_space.addr;
    return (uint64_t *) (debug_malloc_shadow.addr + off);
  } else
    return NULL;
}

static void mark_object(Object *obj)
{
  {
    if (in_js_space(obj)) {
      uintptr_t a = (uintptr_t) obj;
      uintptr_t off = a - js_space.addr;
      uint64_t *shadow = (uint64_t *) (debug_js_shadow.addr + off);
      assert((*shadow & 1) == 1);
    } else if (in_malloc_space(obj)) {
      uintptr_t a = (uintptr_t) obj;
      uintptr_t off = a - malloc_space.addr;
      uint64_t *shadow = (uint64_t *) (debug_malloc_shadow.addr + off);
      assert((*shadow & 1) == 1);
    }
  }
  obj->header |= GC_MARK_BIT;
}

static void unmark_object(Object *obj)
{
  obj->header &= ~GC_MARK_BIT;
}

static int is_marked_object(Object *obj)
{
#if HEADER0_GC_OFFSET <= 4 * 8  /* BITS_IN_INT */
  return obj->header & GC_MARK_BIT;
#else
  return !!(obj->header & GC_MARK_BIT);
#endif
}

static void trace_leaf_object_pointer(uintptr_t *ptrp)
{
  uintptr_t ptr = *ptrp;
  /* TODO: make a type for leaf object. */
  if (in_js_space((void *) ptr))
    mark_object((Object *) ptr);
  else if (in_malloc_space((void *) ptr))
    mark_object((Object *) (ptr - HEADER_BYTES));
}

static void trace_HashTable(HashTable **ptrp)
{
  printf("Not Implemented: trace_HashTable\n");
}

static void trace_HashCell(HashCell **ptrp)
{
  printf("Not Implemented: trace_HashCell\n");
}

static void trace_FunctionTable(FunctionTable **ptrp)
{
  printf("Not Implemented: trace_FunctionTable\n");
}

static void trace_FunctionFrame(FunctionFrame **ptrp)
{
  printf("Not Implemented: trace_FunctionFrame\n");
}

static void trace_object_pointer(uintptr_t *ptrp)
{
  uintptr_t ptr = *ptrp;
  Object *obj;
  /* TODO: specialise.  If obj is pointed at through JSValue, we do not
   *       need to check it's space.  It should be in js_space */
  if (ptr == 0)
    return;
  if (in_malloc_space((void *) ptr))
    obj = (Object *) (ptr - HEADER_BYTES);
  else if (in_js_space((void *) ptr))
    obj = (Object *) ptr;
  else {
    /* default: JS object */
    obj = (Object *) ptr;
    goto SCAN;
  }

  if (is_marked_object(obj))
    return;
  mark_object(obj);

 SCAN:
  /* common header */
  trace_HashTable(&obj->map);
  trace_JSValue_array(&obj->prop, obj->n_props);

  switch (HEADER0_GET_TYPE(obj->header)) {
  case HTAG_OBJECT:
    break;
  case HTAG_ARRAY:
    trace_JSValue_array(&((ArrayCell *) obj)->body,
			((ArrayCell *) obj)->length);
    break;
  case HTAG_FUNCTION:
    trace_FunctionTable(&((FunctionCell *) obj)->func_table_entry);
    trace_FunctionFrame(&((FunctionCell *) obj)->environment);
    break;
  case HTAG_BUILTIN:
    break;
  case HTAG_ITERATOR:
    /* TODO: call scanHashIterator */
    trace_HashCell(&((IteratorCell *) obj)->iter.p);
    break;
#ifdef USE_REGEXP
  case HTAG_REGEXP:
    trace_leaf_object_pointer((uintptr_t *)&((RegexpCell *)obj)->pattern);
    break;
#endif /* USE_REGEXP */
  case HTAG_BOXED_STRING:
  case HTAG_BOXED_NUMBER:
  case HTAG_BOXED_BOOLEAN:
    trace_slot(&((BoxedCell *) obj)->value);
    break;
  }
}

static void trace_JSValue_array(JSValue **ptrp, uint32_t length)
{
  JSValue *ptr = *ptrp;
  int i;

  /* never be JS object */
  if (in_js_space(ptr))
    LOG_EXIT("GC: found an array of JSValue in js_space\n");

  if (in_malloc_space(ptr)) {
    Object *header = (Object *) (ptr - HEADER_JSVALUES);
    if (is_marked_object(header))
      return;
    mark_object(header);
  }

  /* SCAN */
  for (i = 0; i < length; i++, ptr++)
    trace_slot(ptr);
}

static void trace_slot(JSValue* ptr)
{
  JSValue jsv = *ptr;
  /* TODO: use macro */
  if ((jsv & 0x2) != 0) /* not a pointer */
    return;
  if ((jsv & 0x4) != 0) {
    uint8_t tag = jsv & TAGMASK;
    jsv &= ~TAGMASK;
    trace_leaf_object_pointer((uintptr_t *) &jsv);
    *ptr = jsv | tag;
  }
  else
    trace_object_pointer((uintptr_t *) ptr);
}

static void scan_roots(void)
{
  struct global_constant_objects *gconstsp = &gconsts;
  JSValue* p;
  /* global variables */
  for (p = (JSValue *) gconstsp; p < (JSValue *) (gconstsp + 1); p++) {
    trace_slot(p);
  }
}

static void print_memory_status(void)
{
  printf("  gc_disabled = %d\n", gc_disabled);
  printf("  js_space.free_bytes = %d\n", js_space.free_bytes);
  printf("  malloc_space.free_bytes = %d\n", malloc_space.free_bytes);
}
