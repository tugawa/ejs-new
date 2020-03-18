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
  struct free_chunk *p;
  addr = (uintptr_t) malloc(bytes + BYTES_IN_GRANULE - 1);
  p = (struct free_chunk *)
    ((addr + BYTES_IN_GRANULE - 1) & ~(BYTES_IN_GRANULE - 1));
  p->header = compose_header(bytes >> LOG_BYTES_IN_GRANULE, 0, CELLT_FREE);
  p->next = NULL;
  space->addr = (uintptr_t) p;
  space->bytes = bytes;
  space->free_bytes = bytes;
  space->freelist = p;
  space->name = name;
}

#ifdef GC_DEBUG
header_t *get_shadow(void *ptr)
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
STATIC_INLINE void* js_space_alloc(struct space *space,
                                   size_t request_bytes, cell_type_t type)
{
  size_t  alloc_granules;
  struct free_chunk **p;
  
  alloc_granules = BYTE_TO_GRANULE_ROUNDUP(request_bytes);
  alloc_granules += HEADER_GRANULES;

  /* allocate from freelist */
  for (p = &space->freelist; *p != NULL; p = &(*p)->next) {
    struct free_chunk *chunk = *p;
    size_t chunk_granules = chunk->header.size;
    if (chunk_granules >= alloc_granules) {
      if (chunk_granules >= alloc_granules + MINIMUM_FREE_CHUNK_GRANULES) {
        /* This chunk is large enough to leave a part unused.  Split it */
        size_t remaining_granules = chunk_granules - alloc_granules;
        header_t *hdrp = (header_t *)
          (((uintptr_t) chunk) + (remaining_granules << LOG_BYTES_IN_GRANULE));
        *hdrp = compose_header(alloc_granules, 0, type);
        chunk->header.size = remaining_granules;
        space->free_bytes -= alloc_granules << LOG_BYTES_IN_GRANULE;
        return header_to_payload(hdrp);
      } else {
        /* This chunk is too small to split. */
        header_t *hdrp = (header_t *) chunk;
        *p = (*p)->next;
        *hdrp = compose_header(chunk_granules,
                               chunk_granules - alloc_granules, type);
        space->free_bytes -= chunk_granules << LOG_BYTES_IN_JSVALUE;
        return header_to_payload(hdrp);
      }
    }
  }

#ifdef DEBUG
  {
    struct free_chunk *chunk;
    for (chunk = space->freelist; chunk != NULL; chunk = chunk->next)
      LOG(" %u", chunk->header.size * BYTES_IN_GRANULE);
  }
  LOG("\n");
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

/*
 * GC
 */

STATIC void sweep_space(struct space *space)
{
  struct free_chunk **p;
  uintptr_t scan = space->addr;
  uintptr_t free_bytes = 0;

  space->freelist = NULL;
  p = &space->freelist;
  while (scan < space->addr + space->bytes) {
    uintptr_t last_used = 0;
    uintptr_t free_start;
    /* scan used area */
    while (scan < space->addr + space->bytes &&
           is_marked_cell_header((header_t *) scan)) {
      header_t *hdrp = (header_t *) scan;
      assert(hdrp->magic == HEADER_MAGIC);
#ifdef GC_PROF
      {
        cell_type_t type = hdrp->type;
        size_t bytes =
          (hdrp->size - hdrp->extra) << LOG_BYTES_IN_GRANULE;
        pertype_live_bytes[type]+= bytes;
        pertype_live_count[type]++;
      }
#endif /* GC_PROF */
      unmark_cell_header((header_t *) scan);
      last_used = scan;
      scan += hdrp->size << LOG_BYTES_IN_GRANULE;
    }
    free_start = scan;
    while (scan < space->addr + space->bytes &&
           !is_marked_cell_header((header_t *) scan)) {
      header_t *hdrp = (header_t *) scan;
      assert(hdrp->magic == HEADER_MAGIC);
      scan += hdrp->size << LOG_BYTES_IN_GRANULE;
    }
    if (free_start < scan) {
      size_t chunk_granules;
      if (last_used != 0) {
        /* Previous chunk may have extra bytes. Take them back. */
        header_t *last_hdrp = (header_t *) last_used;
        size_t extra = last_hdrp->extra;
        free_start -= extra << LOG_BYTES_IN_GRANULE;
        last_hdrp->size -= extra;
        last_hdrp->extra = 0;
      }
      chunk_granules = (scan - free_start) >> LOG_BYTES_IN_GRANULE;
      if (chunk_granules >= MINIMUM_FREE_CHUNK_GRANULES) {
        struct free_chunk *chunk = (struct free_chunk *) free_start;
        chunk->header = compose_header(chunk_granules, 0, CELLT_FREE);
        *p = chunk;
        p = &chunk->next;
        free_bytes += scan - free_start;
#ifdef GC_DEBUG
        {
          char *p;
          for (p = (char *) (chunk + 1); p < (char *) scan; p++)
            *p = 0xcc;
        }
#endif /* GC_DEBUG */
      } else  {
        /* Too small to make a chunk.
         * Append it at the end of previous chunk, if any */
        if (last_used != 0) {
          header_t *last_hdrp = (header_t *) last_used;
          assert(last_hdrp->extra == 0);
          last_hdrp->size += chunk_granules;
          last_hdrp->extra = chunk_granules;
        } else
          *(header_t *) free_start =
            compose_header(chunk_granules, 0, CELLT_FREE);
      }
    }
  }
  (*p) = NULL;
  space->free_bytes = free_bytes;
}

#ifdef CHECK_MATURED
STATIC void check_matured()
{
  struct space *space = &js_space;
  uintptr_t scan = space->addr;
  while (scan < space->addr + space->bytes) {
    header_t *hdrp = (header_t *) scan;
    if (!is_marked_cell_header(hdrp)) {
      switch(hdrp->type) {
      case CELLT_SIMPLE_OBJECT:
      case CELLT_ARRAY:
      case CELLT_FUNCTION:
      case CELLT_BUILTIN:
      case CELLT_BOXED_NUMBER:
      case CELLT_BOXED_STRING:
      case CELLT_BOXED_BOOLEAN:
#ifdef USE_REGEXP
      case CELLT_REGEXP:
#endif /* USE_REGEXP */
        {
          JSObject *p = (JSObject *) header_to_payload(hdrp);
          Shape *os = p->shape;
          int i;
          for (i = os->pm->n_special_props + 1; i < os->n_embedded_slots; i++) {
            JSValue v;
            if (i == os->n_embedded_slots - 1 && os->n_extension_slots > 0) {
              JSValue *extension = (JSValue *) p->eprop[i];
              v = extension[0];
            } else
              v = p->eprop[i];
            if (v == JS_EMPTY)
              printf("unmatured object (object %p index %d value = EMPTY)\n",
                     p, i);
          }
        }
        break;
      default:
        break;
      }
    }
    scan += hdrp->size << LOG_BYTES_IN_GRANULE;
  }
}
#endif /* CHECK_MATURED */

void sweep(void)
{
#ifdef GC_DEBUG
  check_invariant();
#endif /* GC_DEBUG */
  sweep_space(&js_space);
}

#ifdef GC_DEBUG
#define OFFSET_OF(T, F) (((uintptr_t) &((T *) 0)->F) >> LOG_BYTES_IN_JSVALUE)

STATIC void check_invariant_nobw_space(struct space *space)
{
  uintptr_t scan = space->addr;

  while (scan < space->addr + space->bytes) {
    header_t *hdrp = (header_t *) scan;
    uintptr_t payload = (uintptr_t) header_to_payload(hdrp);
    switch (hdrp->type) {
    case CELLT_STRING:
    case CELLT_FLONUM:
    case CELLT_ARRAY_DATA:
    case CELLT_CONTEXT:
    case CELLT_STACK:
    case CELLT_HIDDEN_CLASS:
    case CELLT_HASHTABLE:
    case CELLT_HASH_CELL:
      break;
    case CELLT_PROPERTY_MAP:
      {
        PropertyMap *pm = (PropertyMap *) payload;
#ifdef NO_SHAPE_CACHE
        if (pm->shapes != NULL)
          assert(payload_to_header(pm->shapes)->type == CELLT_SHAPE);
#else /* NO_SHAPE_CACHE */
        Shape *os;
        for (os = pm->shapes; os != NULL; os = os->next)
          assert(payload_to_header(os)->type == CELLT_SHAPE);
#endif /* NO_SHAPE_CACHE */
        goto DEFAULT;
      }
    default:
    DEFAULT:
      if (is_marked_cell_header(hdrp)) {
        /* this object is black; should not contain a pointer to white */
        size_t payload_bytes =
          get_payload_granules(hdrp) << LOG_BYTES_IN_GRANULE;
        size_t i;
#if BYTES_IN_JSVALUE >= BYTES_IN_GRANULE
#define STEP_BYTES BYTES_IN_GRANULE
#else
#define STEP_BYTES BYTES_IN_JSVALUE
#endif
        for (i = 0; i < payload_bytes; i += STEP_BYTES) {
          JSValue v = *(JSValue *) (payload + i);
          uintjsv_t x = (uintjsv_t) v;
          void * p = (void *) (uintptr_t) clear_ptag(v);
          /* weak pointers */
          /*
          if (HEADER0_GET_TYPE(header) == CELLT_STR_CONS) {
            if (i == OFFSET_OF(StrCons, str))
              continue;
          }
          */
          if (IS_POINTER_LIKE_UINTJSV(x) &&
              (has_htag(x) || get_ptag(x).v == 0) &&
              in_js_space(p))
            assert(is_marked_cell(p));
        }
      }
      break;
    }
    scan += hdrp->size << LOG_BYTES_IN_GRANULE;
  }
}

STATIC void check_invariant(void)
{
  check_invariant_nobw_space(&js_space);
}

STATIC void print_free_list(void)
{
  struct free_chunk *p;
  for (p = js_space.freelist; p; p = p->next)
    printf("%d ", p->header.size * BYTES_IN_GRANULE);
  printf("\n");
}

#endif /* GC_DEBUG */

#ifdef GC_DEBUG
STATIC void space_print_memory_status(void)
{
  printf("  free_bytes = %zu\n", js_space.free_bytes);
}
#endif /* GC_DEBUG */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
