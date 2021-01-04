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

static Context *the_context;
#include "gc-visitor-inl.h"

/*
 * Constants
 */

enum gc_phase {
  PHASE_INACTIVE,
  PHASE_INITIALISE,
  PHASE_COPY,
  PHASE_WEAK,
  PHASE_FLIP
};

/*
 * Variables
 */
static enum gc_phase gc_phase = PHASE_INACTIVE;

static header_t *copy_object(header_t *hdrp);
static void scavenge(void);

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
static void set_forwarding_pointer(uintptr_t ptr, uintptr_t to) {
  *(uintptr_t *) ptr = to;
}
static uintptr_t forward(uintptr_t ptr) {
  header_t *hdrp = payload_to_header(ptr);
  if (hdrp->forwarded)
    return get_forwarding_pointer(ptr);
  header_t *to_hdrp = copy_object(hdrp);
  uintptr_t to = (uintptr_t) header_to_payload(to_hdrp);
  set_forwarding_pointer(ptr, to);
  hdrp->forwarded = 1;
  return to;
}

class CopyTracer {
  static bool in_to_space(uintptr_t ptr) {
    return space.to <= ptr && ptr < space.to + space.bytes;
  }
 public:
  static void process_edge(JSValue &v) {
    if (is_fixnum(v) || is_special(v))
      return;
    uintptr_t ptr = (uintptr_t) clear_ptag(v);
    assert(!in_to_space(ptr));
    uintptr_t to = forward(ptr);
    PTag tag = get_ptag(v);
    v = put_ptag(to, tag);
  }
  static void process_edge(void *&p) {
    uintptr_t ptr = (uintptr_t) p;
    if (in_to_space(ptr))
      return; /* PropertyMap may be in tospace */
    uintptr_t to = forward(ptr);
    p = (void *) to;
  }
  static void process_edge_function_frame(JSValue &v) {
    void *p = jsv_to_function_frame(v);
    uintptr_t ptr = forward((uintptr_t) p);
    v = (JSValue) (uintjsv_t) (uintptr_t) ptr;
  }
  template <typename T>
  static void process_edge_ex_JSValue_array(T &p, size_t n) {
    uintptr_t ptr = forward((uintptr_t) p);
    p = (T) ptr;
  }
  template <typename T>
  static void process_edge_ex_ptr_array(T &p, size_t n) {
    uintptr_t ptr = forward((uintptr_t) p);
    p = (T) ptr;
  }
#if LOG_BYTES_IN_GRANULE < LOG_BYTES_IN_JSVALUE
#error copy collector does not support sizeof(granule_t) < sizeof(JSValue)
#endif
  static void process_node_JSValue_array(JSValue *p) {
    header_t *hdrp = payload_to_header((uintptr_t) p);
    size_t payload_granules = hdrp->size - HEADER_GRANULES;
    size_t slots = payload_granules <<
      (LOG_BYTES_IN_GRANULE - LOG_BYTES_IN_JSVALUE);
    for (size_t i = 0; i < slots; i++)
      process_edge(p[i]);
  }
  static void process_node_ptr_array(void **p) {
    header_t *hdrp = payload_to_header((uintptr_t) p);
    size_t payload_granules = hdrp->size - HEADER_GRANULES;
    size_t slots = payload_granules * (BYTES_IN_GRANULE / sizeof(void *));
    for (size_t i = 0; i < slots; i++)
      if (p[i] != NULL)
	process_edge(p[i]);
  }
  static void process_weak_edge(JSValue v) {}
  static void process_weak_edge(void *p) {}
  static bool is_marked_cell(void *p) { abort(); }
  static void process_mark_stack(void) {
    scavenge();
  }
};

class CopyWeakTracer {
  static bool in_to_space(uintptr_t ptr) {
    return space.to <= ptr && ptr < space.to + space.bytes;
  }
public:
  static void process_edge(JSValue &v) {
    if (is_fixnum(v) || is_special(v))
      return;
    uintptr_t ptr = (uintptr_t) clear_ptag(v);
    if (in_to_space(ptr))
      return;
    uintptr_t to = forward(ptr);
    PTag tag = get_ptag(v);
    v = put_ptag(to, tag);
  }
  static void process_edge(void *&p) {
    uintptr_t ptr = (uintptr_t) p;
    if (in_to_space(ptr))
      return;
    uintptr_t to = forward(ptr);
    p = (void *) to;
  }
  static bool is_marked_cell(void *p) {
    uintptr_t ptr = (uintptr_t) p;
    if (in_to_space(ptr))
      return true;
    header_t *hdrp = payload_to_header(ptr);
    return hdrp->forwarded;
  }
  static void process_mark_stack(void) {
    scavenge();
  }
};

/*
 * GC
 */

static void scavenge(void)
{
  uintptr_t scan = space.scan;
  while (scan < space.free) {
    header_t *hdrp = (header_t *) scan;
    void *payload = header_to_payload(hdrp);
    process_node<CopyTracer>((uintptr_t) payload);
    scan += hdrp->size << LOG_BYTES_IN_GRANULE;
  }
  space.scan = scan;
}

void garbage_collection(Context *ctx)
{
  /* initialise */
  gc_phase = PHASE_INITIALISE;
  the_context = ctx;

  /* copy */
  gc_phase = PHASE_COPY;
  space.free = space.to;
  space.scan = space.to;
  scan_roots<CopyTracer>(ctx);
  scavenge();

  /* weak */
  gc_phase = PHASE_WEAK;
  weak_clear<CopyWeakTracer>();

  /* flip */
  gc_phase = PHASE_FLIP;
  uintptr_t tmp = space.from;
  space.from = space.to;
  space.to = tmp;
  space.end = space.from + space.bytes;

  gc_phase = PHASE_INACTIVE;
}


/*
 * space
 */
struct copy_space space;

void space_init(size_t bytes)
{
  uintptr_t addr = (uintptr_t) malloc(bytes);
  size_t ss_bytes = (bytes >> 1) & ~(BYTES_IN_GRANULE - 1);

  space.ss0 = addr;
  space.ss1 = addr + ss_bytes;
  space.bytes = ss_bytes;
  space.total_bytes = bytes;

  space.from = space.ss0;
  space.end = space.ss0 + ss_bytes;
  space.to = space.ss1;
  space.free = space.from;
}

void *space_alloc(size_t request_bytes, cell_type_t type)
{
  if (request_bytes == 0) {
    assert(type == CELLT_ARRAY_DATA);
    JSValue *array = (JSValue *) space_alloc(sizeof(JSValue), type);
    array[0] = JS_UNDEFINED;
    return array;
  }
  size_t alloc_granules = HEADER_GRANULES +
    ((request_bytes + BYTES_IN_GRANULE - 1) >> LOG_BYTES_IN_GRANULE);
  size_t alloc_bytes = alloc_granules << LOG_BYTES_IN_GRANULE;
  
  header_t *hdrp = (header_t *) space.free;
  space.free += alloc_bytes;
  if (space.free > space.end) {
    space.free -= alloc_bytes;
    return NULL;
  }

  hdrp->size = alloc_granules;
  hdrp->type = type;
  hdrp->forwarded = 0;

  return header_to_payload(hdrp);
}

static header_t *copy_object(header_t *hdrp)
{
  assert(!hdrp->forwarded);
  size_t size = hdrp->size;
  granule_t *from = (granule_t *) hdrp;
  granule_t *to = (granule_t *) space.free;
  for (size_t i = 0; i < hdrp->size; i++)
    to[i] = from[i];
  space.free += size << LOG_BYTES_IN_GRANULE;
  return (header_t *) to;
}

#ifdef GC_DEBUG
void space_print_memory_status(void)
{
}
#endif /* GC_DEBUG */
