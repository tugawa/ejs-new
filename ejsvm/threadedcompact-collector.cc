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

#ifdef CXX_TRACER_CBV
#error "Threaded compactor does not except macro; CXX_TRACER_CBV."
#endif /* CXX_TRACER_CBV */
#ifdef CXX_TRACER_RV
#error "Threaded compactor does not except macro; CXX_TRACER_RV."
#endif /* CXX_TRACER_RV */

#ifdef GC_DEBUG
static void fill_mem(void *p1, void *p2, JSValue v);
#endif /* GC_DEBUG */

/*
 * Constants
 */

enum gc_phase {
  PHASE_INACTIVE,
  PHASE_INITIALISE,
  PHASE_MARK,
  PHASE_WEAK,
  PHASE_FWDREF,
  PHASE_BWDREF,
  PHASE_FINALISE,
};

/*
 * Variables
 */
static enum gc_phase gc_phase = PHASE_INACTIVE;

static Context *the_context;
#include "gc-visitor-inl.h"

/*
 * Tracer
 */
#include "mark-tracer.h"


static bool is_reference(void **pptr);
static void thread_reference(void **ref);
static void update_reference(void *ref, void *addr);

static cell_type_t get_threaded_header_type(header_t *hdrp);
static bool get_threaded_header_markbit(header_t *hdrp);
static unsigned int get_threaded_header_size(header_t *hdrp);

class ThreadTracer {
public:
  static constexpr bool is_single_object_scanner = true;
  static constexpr bool is_hcg_mutator = false;
  static void process_edge(JSValue &v) {
    if (is_fixnum(v) || is_special(v))
      return;

    uintjsv_t ptr = clear_ptag(v);
    if ((void *) ptr == NULL)
      return;

    v = (JSValue) ptr;

    assert(in_js_space((void *) v));

    thread_reference((void **) &v);
  }
  static void process_edge(void *&p) {
    if (p == NULL)
      return;

    assert(in_js_space((void *) p));

    thread_reference(&p);
  }
  static void process_edge_function_frame(JSValue &v) {
#if 0
    void *p = jsv_to_function_frame(v);
    thread_reference(&p);
#endif
    if ((void *) v == NULL)
      return;

    assert(in_obj_space((void *) v));

    thread_reference((void **) &v);
  }
  template <typename T>
  static void process_edge_ex_JSValue_array(T &p, size_t n) {
    if ((void *) p == NULL)
      return;

    assert(in_obj_space((void *) p));

    thread_reference((void **) &p);
  }
  template <typename T>
  static void process_edge_ex_ptr_array(T &p, size_t n) {
    if ((void *) p == NULL)
      return;

    assert(in_js_space((void *) p));

    thread_reference((void **) &p);
  }
  static void process_node_JSValue_array(JSValue *p) {
    if (p == NULL)
      return;

    assert(in_js_space((void *) p));
    assert(!in_hc_space((void *) p));

    header_t *hdrp = payload_to_header(p);
    size_t payload_granules = hdrp->size - HEADER_GRANULES;
    size_t slots = payload_granules <<
      (LOG_BYTES_IN_GRANULE - LOG_BYTES_IN_JSVALUE);
    for (size_t i = 0; i < slots; i++)
      process_edge(p[i]);
  }
  static void process_node_ptr_array(void **&p) {
    if (p == NULL)
      return;

    assert(gc_phase == PHASE_FWDREF);
    assert(!in_js_space((void *) &p) || in_hc_space((void *) &p));

    assert(in_js_space((void *) p));
    assert(in_hc_space((void *) p));

    header_t *hdrp = payload_to_header(p);
#ifdef GC_THREADED_BOUNDARY_TAG
    size_t payload_granules = ((footer_t *) hdrp)->size_lo - HEADER_GRANULES;
#else /* GC_THREADED_BOUNDARY_TAG */
    size_t payload_granules = hdrp->size - HEADER_GRANULES;
#endif /* GC_THREADED_BOUNDARY_TAG */
    size_t slots = payload_granules * (BYTES_IN_GRANULE / sizeof(void *));
    for (size_t i = 0; i < slots; i++)
      if (p[i] != NULL)
        thread_reference(&p[i]);
  }
  static void process_weak_edge(JSValue &v) { process_edge(v); }
  static void process_weak_edge(void *&p) { process_edge(p); }

  static void process_mark_stack(void) {}
};


static bool is_reference(void **pptr) {
  header_t hdr;
  hdr.threaded = (uintptr_t) pptr;
  return hdr.identifier == 0;
}
static void thread_reference(void **ref) {
#ifdef DEBUG
  {
    if (!is_reference(ref)) {
      fprintf(stderr, "refernece %p is unthreadable address.\n", ref); fflush(stdout);
      abort();
    }
  }
#endif /* DEBUG */

  assert(get_threaded_header_markbit(payload_to_header(*ref)));

  if (*ref != NULL) {
#ifdef GC_DEBUG
    if (!is_reference((void **) *ref)) {
      printf("*ref value : 0x%016" PRIx64 " (at %p) is not reference\n", (uintptr_t) *ref, ref);
      fflush(stdout);
      abort();
    }
    if (!in_js_space(*ref)) {
      printf("*ref value : 0x%016" PRIx64 " (at %p) is not in js_space\n", (uintptr_t) *ref, ref);
      fflush(stdout);
      abort();
    }
#endif

    void *payload = *ref;
    header_t *hdrp = payload_to_header(payload);
    assert((!is_reference((void **) hdrp))? (hdrp->markbit == 1) : 1);

    uintptr_t val = hdrp->threaded;
    hdrp->threaded = (uintptr_t) ref;
    *ref = (void *) val;
  }
}
static void update_reference(void *ref_, void *addr) {
  header_t *hdrp = payload_to_header(ref_);
  cell_type_t type = get_threaded_header_type(hdrp);
  uintjsv_t tag = get_ptag_value_by_cell_type(type);

  void **ref = (void **) hdrp->threaded;
  while(is_reference(ref)) {
    void **next = (void **) *ref;
    *ref = (void *) put_ptag((uintjsv_t) addr, ((PTag) {tag}));
    ref = next;
  }
  hdrp->threaded = (uintptr_t) ref;
}
static cell_type_t get_threaded_header_type(header_t *hdrp) {
  while(is_reference((void **) hdrp->threaded)) {
    hdrp = (header_t *) hdrp->threaded;
  }

  return hdrp->type;
}
static bool get_threaded_header_markbit(header_t *hdrp) {
  /* If header is threaded, it must be marked */
  if (is_reference((void **) hdrp->threaded)) {
#ifdef GC_DEBUG
  while(is_reference((void **) hdrp->threaded)) {
    hdrp = (header_t *) hdrp->threaded;
  }
  assert(hdrp->markbit != 0);
#endif /* GC_DEBUG */
    return true;
  }

  return hdrp->markbit != 0;
}
static unsigned int get_threaded_header_size(header_t *hdrp) {
#if (HEADER_GRANULES == 1)
  assert(sizeof(void *) == sizeof(header_t));

  while(is_reference((void **) hdrp->threaded)) {
    hdrp = (header_t *) hdrp->threaded;
  }
#endif /* (HEADER_GRANULES == 1) */

  return hdrp->size;
}

/*
 * GC
 */

static void update_forward_reference(Context *ctx);
static void update_backward_reference();
static void copy_object(void *from, void *to, unsigned int size);
static void copy_object_reverse(void *from_, void *to_, unsigned int size);
#if GC_DEBUG
static void fill_mem(void *p1, void *p2, JSValue v);
#endif /* GC_DEBUG */

void garbage_collection(Context *ctx)
{
  /* initialise */
  gc_phase = PHASE_INITIALISE;
  the_context = ctx;

  /* mark */
#ifdef TU_DEBUG
  printf("mark\n");
#endif /* TU_DEBUG */
  gc_phase = PHASE_MARK;
  scan_roots<DefaultTracer>(ctx);
  DefaultTracer::process_mark_stack();

  /* profile */
#ifdef CHECK_MATURED
  check_matured();
#endif /* CHECK_MATURED */

  /* weak */
#ifdef TU_DEBUG
  printf("weak\n");
#endif /* TU_DEBUG */
  gc_phase = PHASE_WEAK;
  weak_clear<DefaultTracer>(ctx);

  /* forwarding reference */
#ifdef TU_DEBUG
  printf("froward\n");
#endif /* TU_DEBUG */
  gc_phase = PHASE_FWDREF;
  update_forward_reference(ctx);

  /* backwarding reference */
#ifdef TU_DEBUG
  printf("backward\n");
#endif /* TU_DEBUG */
  gc_phase = PHASE_BWDREF;
  update_backward_reference();

  /* finalise */
  gc_phase = PHASE_FINALISE;

#ifdef GC_DEBUG
  fill_mem((void *) js_space.begin, (void *) js_space.end, (JSValue) 0xcccccccccccccccc);
#endif

  gc_phase = PHASE_INACTIVE;
}

static void update_forward_reference(Context *ctx) {
  scan_roots<ThreadTracer>(ctx);

  uintptr_t scan = js_space.head;
  uintptr_t end = js_space.begin;
  uintptr_t free = scan;
#ifdef GC_THREADED_MERGE_FREE_SPACE
  uintptr_t last_free_space = scan;
  bool is_last_free = false;
#endif /* GC_THREADED_MERGE_FREE_SPACE */

  while (scan < end) {
    header_t *hdrp = (header_t *) scan;
    unsigned int size = get_threaded_header_size(hdrp);

    if (get_threaded_header_markbit(hdrp))
    {
      void *from = header_to_payload(hdrp);
      header_t *to_hdrp = (header_t *) free;
      void *to = header_to_payload(to_hdrp);

#ifdef GC_DEBUG
#ifdef TU_DEBUG
      printf("%p -> %p\n", hdrp, to_hdrp);
#endif /* TU_DEBUG */
      if (size > 1) {
	header_t **shadow = (header_t **) get_shadow(hdrp);
	shadow[1] = to_hdrp;
      }
#endif /* GC_DEBUG */

      update_reference(from, to);
      process_node<ThreadTracer>((uintptr_t) from);

#ifdef GC_THREADED_MERGE_FREE_SPACE
      is_last_free = false;
#endif /* GC_THREADED_MERGE_FREE_SPACE */

      free += size << LOG_BYTES_IN_JSVALUE;
    }
#ifdef GC_THREADED_MERGE_FREE_SPACE
    else {
      if (!is_last_free)
        last_free_space = scan;
      else if (scan != last_free_space) {
        header_t *last_free_hdrp = (header_t *) last_free_space;
        last_free_hdrp->size += size;
      }

      is_last_free = true;
    }
#endif /* GC_THREADED_MERGE_FREE_SPACE */

    scan += size << LOG_BYTES_IN_JSVALUE;
  }

#ifdef GC_THREADED_BOUNDARY_TAG
  scan = (uintptr_t) end_to_footer(js_space.tail);
#else /* GC_THREADED_BOUNDARY_TAG */
  scan = js_space.tail;
#endif /* GC_THREADED_BOUNDARY_TAG */
  end = js_space.end;
  free = scan;
#ifdef GC_THREADED_MERGE_FREE_SPACE
  last_free_space = scan;
  is_last_free = false;
#endif /* GC_THREADED_MERGE_FREE_SPACE */

  while (scan > end) {
#ifdef GC_THREADED_BOUNDARY_TAG
    footer_t *footer = (footer_t *) scan;
    header_t *hdrp = footer_to_header(footer);
    unsigned int size = footer->size_hi;
#else /* GC_THREADED_BOUNDARY_TAG */
    header_t *footer = end_to_footer(scan);
    header_t *hdrp = footer_to_header(footer);
    unsigned int size = footer->size;
#endif /* GC_THREADED_BOUNDARY_TAG */

    const bool markbit = get_threaded_header_markbit(hdrp);
    if (markbit) {
#ifdef GC_THREADED_BOUNDARY_TAG
      free -= size << LOG_BYTES_IN_JSVALUE;
#else /* GC_THREADED_BOUNDARY_TAG */
      free -= (size + HEADER_GRANULES) << LOG_BYTES_IN_JSVALUE;
      footer->markbit = 1;
    }
    else {
      footer->markbit = 0;
#endif /* GC_THREADED_BOUNDARY_TAG */
    }

#ifdef GC_THREADED_MERGE_FREE_SPACE
    if (markbit)
      is_last_free = false;
    else {
      if (!is_last_free)
        last_free_space = scan;
      else if (scan != last_free_space) {
#ifdef GC_THREADED_BOUNDARY_TAG
        footer_t *last_free_footer = (footer_t *) last_free_space;
        unsigned int nextsize = last_free_footer->size_hi + size;
#ifndef GC_THREADED_BOUNDARY_TAG_SKIP_SIZE_CHECK
        if (nextsize > BOUNDARY_TAG_MAX_SIZE)
          last_free_space = scan;
        else
#endif /* GC_THREADED_BOUNDARY_TAG_NO_SIZE_CHECK */
        {
          last_free_footer->size_hi = nextsize;
          header_t *free_header = hdrp;
          while (is_reference((void **) free_header->threaded))
            free_header = (header_t *) free_header->threaded;
          ((footer_t *) free_header)->size_lo = nextsize;
        }
#else /* GC_THREADED_BOUNDARY_TAG */
        header_t *last_free_footer = end_to_footer(last_free_space);
        last_free_footer->size += size + HEADER_GRANULES;
        hdrp->size = last_free_footer->size;
#endif /* GC_THREADED_BOUNDARY_TAG */
      }
      is_last_free = true;
    }
#endif /* GC_THREADED_MERGE_FREE_SPACE */

    if (markbit)
    {
      void *from = header_to_payload(hdrp);
      header_t *to_hdrp = (header_t *) free;
      void *to = header_to_payload(to_hdrp);

#ifdef GC_DEBUG
#ifdef TU_DEBUG
      printf("%p -> %p\n", hdrp, to_hdrp);
#endif /* TU_DEBUG */
      if (size > 1) {
	header_t **shadow = (header_t **) get_shadow(hdrp);
	shadow[1] = to_hdrp;
      }
#endif /* GC_DEBUG */

      update_reference(from, to);
      process_node<ThreadTracer>((uintptr_t) from);
    }

#ifdef GC_THREADED_BOUNDARY_TAG
    scan -= size << LOG_BYTES_IN_JSVALUE;
#else /* GC_THREADED_BOUNDARY_TAG */
    scan -= (size + HEADER_GRANULES) << LOG_BYTES_IN_JSVALUE;
#endif /* GC_THREADED_BOUNDARY_TAG */
  }

#ifdef GC_THREADED_BOUNDARY_TAG
  assert(((footer_t *) scan)->size_hi == 0);
#endif /* GC_THREADED_BOUNDARY_TAG */
}

static void update_backward_reference() {
  uintptr_t scan = js_space.head;
  uintptr_t end = js_space.begin;
  uintptr_t free = scan;

  while (scan < end) {
    header_t *hdrp = (header_t *) scan;
    unsigned int size = get_threaded_header_size(hdrp);

    if (get_threaded_header_markbit(hdrp))
    {
      void *from = header_to_payload(hdrp);
      header_t *to_hdrp = (header_t *) free;
      void *to = header_to_payload(to_hdrp);

#ifdef GC_DEBUG
      if (size > 1) {
	header_t **shadow = (header_t **) get_shadow(hdrp);
	assert(shadow[1] == to_hdrp);
      }
#endif /* GC_DEBUG */

      update_reference(from, to);
      unmark_cell_header(hdrp);
      copy_object(hdrp, to_hdrp, size);

#ifdef GC_PROF
      {
        cell_type_t type = to_hdrp->type;
        size_t bytes = size << LOG_BYTES_IN_GRANULE;
        pertype_live_bytes[type]+= bytes;
        pertype_live_count[type]++;
      }
#endif /* GC_PROF */

#ifdef GC_DEBUG
      {
        header_t *shadow = get_shadow(to_hdrp);
        *shadow = *to_hdrp;
	if (hdrp->size > 1)
	  ((uintptr_t *)shadow)[1] = (uintptr_t) from;
      }
#endif

      free += size << LOG_BYTES_IN_JSVALUE;
    }

    scan += size << LOG_BYTES_IN_JSVALUE;
  }

  js_space.begin = free;

#ifdef GC_THREADED_BOUNDARY_TAG
  scan = (uintptr_t) end_to_footer(js_space.tail);
#else /* GC_THREADED_BOUNDARY_TAG */
  scan = js_space.tail;
#endif /* GC_THREADED_BOUNDARY_TAG */
  end = js_space.end;
  free = scan;

  while (scan > end) {
#ifdef GC_THREADED_BOUNDARY_TAG
    footer_t *footer = (footer_t *) scan;
    header_t *hdrp = footer_to_header(footer);
    unsigned int size = footer->size_hi;
#else /* GC_THREADED_BOUNDARY_TAG */
    header_t *footer = end_to_footer(scan);
    header_t *hdrp = footer_to_header(footer);
    unsigned int size = footer->size;
#endif /* GC_THREADED_BOUNDARY_TAG */

#ifdef GC_THREADED_BOUNDARY_TAG
    const bool markbit = get_threaded_header_markbit(hdrp);
#else /* GC_THREADED_BOUNDARY_TAG */
    const bool markbit = footer->markbit == 1;
#endif /* GC_THREADED_BOUNDARY_TAG */

    if (markbit)
#ifdef GC_THREADED_BOUNDARY_TAG
      free -= size << LOG_BYTES_IN_JSVALUE;
#else /* GC_THREADED_BOUNDARY_TAG */
      free -= (size + HEADER_GRANULES) << LOG_BYTES_IN_JSVALUE;
#endif /* GC_THREADED_BOUNDARY_TAG */

    if (markbit)
    {
      void *from = header_to_payload(hdrp);
      header_t *to_hdrp = (header_t *) free;
      void *to = header_to_payload(to_hdrp);

#ifdef GC_DEBUG
#ifdef TU_DEBUG
      printf("%p -> %p\n", hdrp, to_hdrp);
#endif /* TU_DEBUG */
      if (size > 1) {
	header_t **shadow = (header_t **) get_shadow(hdrp);
	assert(shadow[1] == to_hdrp);
      }
#endif /* GC_DEBUG */

      update_reference(from, to);
      unmark_cell_header(hdrp);
#ifdef GC_THREADED_BOUNDARY_TAG
      copy_object_reverse(hdrp, to_hdrp, size);
#else /* GC_THREADED_BOUNDARY_TAG */
      footer->markbit = 0;
      copy_object_reverse(hdrp, to_hdrp, size + HEADER_GRANULES);
#endif /* GC_THREADED_BOUNDARY_TAG */

#ifdef GC_PROF
      {
        cell_type_t type = to_hdrp->type;
#ifdef GC_THREADED_BOUNDARY_TAG
        size_t bytes = size << LOG_BYTES_IN_GRANULE;
#else /* GC_THREADED_BOUNDARY_TAG */
        size_t bytes = (size + HEADER_GRANULES) << LOG_BYTES_IN_GRANULE;
#endif /* GC_THREADED_BOUNDARY_TAG */
        pertype_live_bytes[type]+= bytes;
        pertype_live_count[type]++;
      }
#endif /* GC_PROF */

#ifdef GC_DEBUG
      {
        header_t *shadow = get_shadow(to_hdrp);
        *shadow = *to_hdrp;
	if (hdrp->size > 1)
	  ((uintptr_t *)shadow)[1] = (uintptr_t) from;
      }
#endif
    }

#ifdef GC_THREADED_BOUNDARY_TAG
    scan -= size << LOG_BYTES_IN_JSVALUE;
#else /* GC_THREADED_BOUNDARY_TAG */
    scan -= (size + HEADER_GRANULES) << LOG_BYTES_IN_JSVALUE;
#endif /* GC_THREADED_BOUNDARY_TAG */
  }

#ifdef GC_THREADED_BOUNDARY_TAG
  assert(((footer_t *) scan)->size_hi == 0);

  ((footer_t *) free)->size_hi = 0;
#endif /* GC_THREADED_BOUNDARY_TAG */

  js_space.end = free;
  js_space.free_bytes = js_space.end - js_space.begin;
}

static void copy_object(void *from_, void *to_, unsigned int size)
{
  if (from_ == to_)
    return;

  JSValue *from = (JSValue *) from_;
  JSValue *to = (JSValue *) to_;
  JSValue *end = from + size;

  while(from < end) {
    *to = *from;
    ++from;
    ++to;
  }
}
static void copy_object_reverse(void *from_, void *to_, unsigned int size)
{
  if (from_ == to_)
    return;

#ifdef GC_THREADED_BOUNDARY_TAG
  JSValue *from = (JSValue *) from_ + size;
  JSValue *to = (JSValue *) to_ + size;
#else /* GC_THREADED_BOUNDARY_TAG */
  JSValue *from = (JSValue *) from_ + (size - 1);
  JSValue *to = (JSValue *) to_ + (size - 1);
#endif /* GC_THREADED_BOUNDARY_TAG */
  JSValue *end = (JSValue *) from_;

#ifdef GC_THREADED_BOUNDARY_TAG
  ((footer_t *) to)->size_hi = ((footer_t *) from)->size_hi;

  --from;
  --to;
#endif /* GC_THREADED_BOUNDARY_TAG */

  while(from >= end) {
    *to = *from;
    --from;
    --to;
  }
}

#ifdef GC_DEBUG
static void fill_mem(void *p1, void *p2, JSValue v)
{
  JSValue *p = (JSValue *) p1;
  JSValue *end = (JSValue *) p2;
  while (p < end) {
    *p = v;
    ++p;
  }
}
#endif /* GC_DEBUG */

