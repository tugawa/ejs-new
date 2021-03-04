/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN extern
#include "header.h"

/*
 * Variables
 */
struct space js_space;
#ifdef GC_DEBUG
STATIC struct space debug_js_shadow;
#endif /* GC_DEBUG */

/*
 * prototype
 */
/* space */
STATIC void create_space(struct space *space, size_t bytes, size_t threshold_bytes, char* name);
#ifdef GC_DEBUG
STATIC header_t *get_shadow(void *ptr);
#endif /* GC_DEBUG */
/* GC */
#ifdef CHECK_MATURED
STATIC void check_matured(void);
#endif /* CHECK_MATURED */
#ifdef GC_DEBUG
STATIC void check_invariant(void);
STATIC void print_memory_status(void);
#endif /* GC_DEBUG */


/*
 * Header operation
 */

STATIC_INLINE size_t get_payload_granules(header_t *hdrp) __attribute((unused));
STATIC_INLINE size_t get_payload_granules(header_t *hdrp)
{
  header_t hdr = *hdrp;
  return hdr.size - HEADER_GRANULES;
}

/*
 *  Space
 */
STATIC void create_space(struct space *space, size_t bytes, size_t threshold_bytes, char *name)
{
  uintptr_t addr;
  addr = (uintptr_t) malloc(bytes);
  space->head = (uintptr_t) addr;
  space->begin = space->head;
  space->tail = (uintptr_t) addr + bytes;
  space->end = space->tail;
#ifdef GC_THREADED_BOUNDARY_TAG
  space->end -= HEADER_GRANULES << LOG_BYTES_IN_GRANULE;
  *((header_t *) space->end) = compose_hidden_class_header(0, CELLT_FREE);
  write_boundary_tag(space->end, 0);
#endif /* GC_THREADED_BOUNDARY_TAG */
  space->bytes = bytes;
  space->free_bytes = space->end - space->begin;
  space->threshold_bytes = threshold_bytes;
  space->name = name;
}

#ifdef GC_DEBUG
header_t *get_shadow(void *ptr)
{
  if (in_js_space(ptr)) {
    uintptr_t a = (uintptr_t) ptr;
    uintptr_t off = a - js_space.head;
    return (header_t *) (debug_js_shadow.head + off);
  } else {
    printf("Warn : get_shadow return NULL;");
    printf(" ptr = %p, js_space.head = %p, js_space.end = %p\n", ptr, (void *) js_space.head, (void *) js_space.end);
	fflush(stdout);
    return NULL;
  }
}
#endif /* GC_DEBUG */

STATIC_INLINE bool is_hidden_class(cell_type_t type)
{
  switch(type) {
  case CELLT_HASHTABLE:
  case CELLT_HASH_BODY:
  case CELLT_HASH_CELL:
  case CELLT_TRANSITIONS:
  case CELLT_PROPERTY_MAP:
  case CELLT_SHAPE:
  case CELLT_PROPERTY_MAP_LIST: // ???
    return true;
  default:
    return false;
  }
}

/*
 * Returns a pointer to the first address of the memory area
 * available to the VM. The header precedes the area.
 */
STATIC_INLINE void* js_space_alloc(struct space *space,
                                   size_t request_bytes, cell_type_t type)
{
#if LOG_BYTES_IN_GRANULE != LOG_BYTES_IN_JSVALUE
#error "LOG_BYTES_IN_JSVALUE != LOG_BYTES_IN_GRANULE"
#endif

  size_t alloc_granules =
    BYTE_TO_GRANULE_ROUNDUP(request_bytes) + HEADER_GRANULES;
  size_t bytes = (alloc_granules << LOG_BYTES_IN_GRANULE);
  header_t *hdrp;

  if (!is_hidden_class(type)) {
    uintptr_t next = space->begin + bytes;
    if (next >= space->end)
      goto js_space_alloc_out_of_memory;
    hdrp = (header_t *) space->begin;
    space->begin = next;
    *hdrp = compose_header(alloc_granules, type);
  } else {
    uintptr_t alloc_end =
      space->end - (BOUNDARY_TAG_GRANULES << LOG_BYTES_IN_GRANULE);
    hdrp = (header_t *) (alloc_end - bytes);
    if (space->begin > (uintptr_t) hdrp)
      goto js_space_alloc_out_of_memory;
    space->end = (uintptr_t) hdrp;
    *hdrp = compose_hidden_class_header(alloc_granules, type);
    write_boundary_tag(alloc_end, alloc_granules);
  }

  space->free_bytes -= bytes;
  return header_to_payload(hdrp);

js_space_alloc_out_of_memory:
#ifdef DEBUG
  LOG("js_space.head  = %zu\n", js_space.head);
  LOG("js_space.begin = %zu\n", js_space.begin);
  LOG("js_space.end   = %zu\n", js_space.end);
  LOG("js_space.tail  = %zu\n", js_space.tail);
  LOG("js_space.bytes = %zu\n", js_space.bytes);
  LOG("js_space.free_bytes = %zu\n", js_space.free_bytes);
  LOG("request = %zu\n", request_bytes);
  LOG("type = 0x%x\n", type);
  LOG("memory exhausted\n");
#endif /* DEBUG */
  return NULL;
}


/*
 * GC interface
 */

void space_init(size_t bytes, size_t threshold_bytes)
{
  create_space(&js_space, bytes, threshold_bytes, "js_space");
#ifdef GC_DEBUG
  create_space(&debug_js_shadow, bytes, threshold_bytes, "debug_js_shadow");
#endif /* GC_DEBUG */
}

void* space_alloc(uintptr_t request_bytes, uint32_t type)
{
  void* addr = js_space_alloc(&js_space, request_bytes, type);
#ifdef GC_DEBUG
  if (addr != NULL) {
    header_t *hdrp = payload_to_header(addr);
    header_t *shadow = get_shadow(hdrp);
    *shadow = *hdrp;
  }
#endif /* GC_DEBUG */
  return addr;
}

#ifdef GC_DEBUG
STATIC void space_print_memory_status(void)
{
  printf(" free_bytes = %zu\n", js_space.free_bytes);
}
#endif /* GC_DEBUG */

/* Local Variables: */
/* mode: c */
/* c-basic-offset: 2 */
/* indent-tabs-mode: nil */
/* End: */
