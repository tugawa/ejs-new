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
 * If the remaining room is smaller than a certain size,
 * we do not use the remainder for efficiency.  Rather,
 * we add it below the chunk being allocated.  In this case,
 * the size in the header includes the extra words.
 *
 * MINIMUM_FREE_CHECK_GRANULES >= HEADER_GRANULES + roundup(pointer granules)
 * MINIMUM_FREE_CHUNK_GRANULES <= 2^HEADER_EXTRA_BITS
 */
#define MINIMUM_FREE_CHUNK_GRANULES 4

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
STATIC void create_space(struct space *space, size_t bytes, char* name);
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
  return hdr.size - hdr.extra - HEADER_GRANULES;
}

/*
 *  Space
 */
STATIC void create_space(struct space *space, size_t bytes, char *name)
{
  uintptr_t addr;
  addr = (uintptr_t) malloc(bytes);
  space->head = (uintptr_t) addr;
  space->begin = (uintptr_t) addr;
  space->tail = (uintptr_t) addr + bytes;
  space->end = (uintptr_t) addr + bytes;
  space->bytes = bytes;
  space->free_bytes = bytes;
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
  case CELLT_PROPERTY_MAP:
  case CELLT_SHAPE:
  case CELLT_UNWIND: // ???
  case CELLT_PROPERTY_MAP_LIST: // ???
    return true;
  default:
    return false;
  }
}

/*
 * Returns a pointer to the first address of the memory area
 * available to the VM.  The header precedes the area.
 * The header has the size of the chunk including the header,
 * the area available to the VM, and extra bytes if any.
 * Other header bits are zero
 */
STATIC_INLINE void* js_space_alloc(struct space *space,
                                   size_t request_bytes, cell_type_t type)
{
  size_t  alloc_granules;

  alloc_granules = BYTE_TO_GRANULE_ROUNDUP(request_bytes);
  alloc_granules += HEADER_GRANULES;

#if LOG_BYTES_IN_GRANULE != LOG_BYTES_IN_JSVALUE
#error "LOG_BYTES_IN_JSVALUE != LOG_BYTES_IN_GRANULE"
#endif

  if (!is_hidden_class(type)) {
    uintptr_t bytes = (alloc_granules << LOG_BYTES_IN_GRANULE);
    header_t *hdrp = (header_t *) space->begin;
    uintptr_t next = space->begin + bytes;

    if (next < space->end) {
      space->begin = next;
      space->free_bytes -= bytes;
      *hdrp = compose_header(alloc_granules, 0, type);
      return header_to_payload(hdrp);
    }
  } else {
    uintptr_t bytes = ((alloc_granules + HEADER_GRANULES) << LOG_BYTES_IN_GRANULE);
    header_t *hdrp = (header_t *) (space->end - bytes);
    header_t *footer = end_to_footer(space->end);

    if (space->begin < (uintptr_t) hdrp) {
      space->end = (uintptr_t) hdrp;
      space->free_bytes -= bytes;
      *hdrp = compose_header(alloc_granules, 0, type);
      *footer = *hdrp;
      return header_to_payload(hdrp);
    }
  }

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

void space_init(size_t bytes)
{
  create_space(&js_space, bytes, "js_space");
#ifdef GC_DEBUG
  create_space(&debug_js_shadow, bytes, "debug_js_shadow");
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
