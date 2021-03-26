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

#ifdef GC_PROF
static void count_dead_object(header_t hdr, size_t size)
{
  cell_type_t type = hdr.type;
  size_t bytes = (size + BOUNDARY_TAG_GRANULES) << LOG_BYTES_IN_GRANULE;
  pertype_collect_bytes[type]+= bytes;
  pertype_collect_count[type]++;
}

static void count_live_object(header_t hdr, size_t size)
{
  cell_type_t type = hdr.type;
  size_t bytes = (size + BOUNDARY_TAG_GRANULES) << LOG_BYTES_IN_GRANULE;
  pertype_live_bytes[type]+= bytes;
  pertype_live_count[type]++;
}

#define COUNT_DEAD_OBJECT(hdrp, size) count_dead_object(hdrp, size)
#define COUNT_LIVE_OBJECT(hdrp, size) count_live_object(hdrp, size)
#else /* GC_PROF */
#define COUNT_DEAD_OBJECT(hdrp, size)
#define COUNT_LIVE_OBJECT(hdrp, size)
#endif /* GC_PROF */

static bool is_reference(void **pptr);
static void thread_reference(void **ref);

static header_t get_threaded_header(header_t *hdrp);

class ThreadTracer {
public:
  static constexpr bool is_single_object_scanner = true;
  static constexpr bool is_hcg_mutator = false;
  static void process_edge(JSValue &v) {
    if (is_fixnum(v) || is_special(v))
      return;

    uintjsv_t ptr = clear_ptag(v);
#ifdef GC_THREADED_NO_HCGC
    if (!in_obj_space((void *) ptr))
      return;
#else /* GC_THREADED_NO_HCGC */
    if ((void *) ptr == NULL)
      return;
#endif /* GC_THREADED_NO_HCGC */

    v = (JSValue) ptr;

    assert(in_js_space((void *) v));

    thread_reference((void **) &v);
  }
  static void process_edge(void *&p) {
#ifdef GC_THREADED_NO_HCGC
    if (!in_obj_space(p))
      return;
#else /* GC_THREADED_NO_HCGC */
    if (p == NULL)
      return;
#endif /* GC_THREADED_NO_HCGC */

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
#ifdef GC_THREADED_NO_HCGC
    if (!in_obj_space((void *) p))
      return;
#else /* GC_THREADED_NO_HCGC */
    if ((void *) p == NULL)
      return;
#endif /* GC_THREADED_NO_HCGC */

    assert(in_obj_space((void *) p));

    thread_reference((void **) &p);
  }
  template <typename T>
  static void process_edge_ex_ptr_array(T &p, size_t n) {
#ifdef GC_THREADED_NO_HCGC
    if (!in_obj_space(p))
      return;
#else /* GC_THREADED_NO_HCGC */
    if ((void *) p == NULL)
      return;
#endif /* GC_THREADED_NO_HCGC */
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
    size_t payload_granules = hdrp->hc.size_lo - HEADER_GRANULES;
    size_t slots = payload_granules * (BYTES_IN_GRANULE / sizeof(void *));
    for (size_t i = 0; i < slots; i++) {
#ifdef GC_THREADED_NO_HCGC
      if (in_obj_space(p[i]))
        thread_reference(&p[i]);
#else /* GC_THREADED_NO_HCGC */
      if (p[i] != NULL)
        thread_reference(&p[i]);
#endif /* GC_THREADED_NO_HCGC */
    }
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

  assert(get_threaded_header(payload_to_header(*ref)).markbit);

  if (*ref != NULL) {
#ifdef GC_DEBUG
    if (!is_reference((void **) *ref)) {
      printf("*ref value : 0x%016" PRIxPTR " (at %p) is not reference\n", (uintptr_t) *ref, ref);
      fflush(stdout);
      abort();
    }
    if (!in_js_space(*ref)) {
      printf("*ref value : 0x%016" PRIxPTR " (at %p) is not in js_space\n", (uintptr_t) *ref, ref);
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

static header_t get_threaded_header(header_t *hdrp)
{
  header_t hdr = *hdrp;
  while(!hdr.identifier)
    hdr.threaded = *(uintptr_t *) hdr.threaded;

  return hdr;
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

static void update_reference(uintjsv_t tag, void *ref_, void *addr) {
  header_t *hdrp = payload_to_header(ref_);
  uintjsv_t value = put_ptag((uintjsv_t) addr, ((PTag) {tag}));

  void **ref = (void **) hdrp->threaded;
  while(is_reference(ref)) {
    void **next = (void **) *ref;
    *ref = (void *) value;
    ref = next;
  }
  hdrp->threaded = (uintptr_t) ref;
}

/*
 * GC
 */

static void update_forward_reference(Context *ctx);
static void update_backward_reference();
static void copy_object(void *from, void *to, unsigned int size);
#ifndef GC_THREADED_NO_HCGC
static void copy_object_reverse(uintptr_t from,
				uintptr_t from_end, uintptr_t to_end);
#endif /* GC_THREADED_NO_HCGC */

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

static inline uintptr_t make_free_cell(uintptr_t end, size_t granules)
{
  uintptr_t p = end - (granules << LOG_BYTES_IN_GRANULE);
  header_t *hdrp = (header_t *) p;
  *hdrp = compose_hidden_class_header(granules, CELLT_FREE);
#ifdef GC_DEBUG
  {
    header_t *shadow = get_shadow(hdrp);
    *shadow = *hdrp;
  }
#endif /* GC_DEBUG */
  write_boundary_tag(end, granules);
  return p;
}

#ifndef GC_THREADED_NO_HCGC
static inline void
merge_free_space_in_hidden_class_area(uintptr_t start, uintptr_t end,
				      uintptr_t first_free)
{
  /*
   *  do merge
   *  start                  first   end
   *   | free  | free  | free | free |
   *
   *  do not merge (1): single free cell
   *  first == start          end
   *   |         free          |
   *
   *  do not merge (2): no free cell
   *  first                   end == start
   *   |         live          |
   */
  if (first_free <= start)
    return;
  size_t bytes = end - start;
  size_t granules = bytes >> LOG_BYTES_IN_GRANULE;
#ifdef GC_THREADED_BOUNDARY_TAG
  if (granules > BOUNDARY_TAG_MAX_SIZE) {
    while (end - start >
	   ((BOUNDARY_TAG_MAX_SIZE + HEADER_GRANULES) <<
	    LOG_BYTES_IN_GRANULE))
      end = make_free_cell(end, BOUNDARY_TAG_MAX_SIZE);
    if (end - start > BOUNDARY_TAG_MAX_SIZE)
      end = make_free_cell(end, HEADER_GRANULES);
    granules = (end - start) >> LOG_BYTES_IN_GRANULE;
  }
#endif /* BOUNDARY_TAG_MAX_SIZE */
  header_t *hdrp = (header_t *) start;
  hdrp->hc.size_lo = granules;
  write_boundary_tag(end, granules);
}
#endif /* GC_THREADED_NO_HCGC */

static inline void
merge_free_space_in_ordinary_area(uintptr_t start, uintptr_t end)
{
  if (start == end)
    return;
  size_t bytes = end - start;
  size_t granules = bytes >> LOG_BYTES_IN_GRANULE;
  header_t *hdrp = (header_t *) start;
  if (granules > ((static_cast<uintjsv_t>(1) << HEADER_SIZE_BITS) - 1)) {
    hdrp->size = 0;
    *(granule_t *) (start + (HEADER_GRANULES << BYTES_IN_GRANULE)) = granules;
  } else
    hdrp->size = granules;
}

static void update_forward_reference(Context *ctx)
{
  scan_roots<ThreadTracer>(ctx);

  uintptr_t scan, end, free;

#ifdef GC_THREADED_NO_HCGC
  scan = js_space.end;
  end = js_space.tail;
  while (scan < end) {
    header_t *hdrp = (header_t *) scan;

    /* skip free/garbage */
    while (!is_marked_cell_header(hdrp)) {
      size_t size = hdrp->size;
      scan += size << LOG_BYTES_IN_GRANULE;
      assert(scan <= end);
      if (scan == end)
	goto HIDDEN_CLASS_AREA_DONE;
      hdrp = (header_t *) scan;
    }

    /* process live object */
    void *p = header_to_payload(hdrp);
    process_node<ThreadTracer>((uintptr_t) p);
    unmark_cell_header(hdrp);
    scan += hdrp->size << LOG_BYTES_IN_GRANULE;
  }
 HIDDEN_CLASS_AREA_DONE:
#endif /* GC_THREADED_NO_HCGC */

  scan = js_space.head;
  end = js_space.begin;
  free = scan;
  while (scan < end) {
    header_t *hdrp = (header_t *) scan;

    /* skip free/garbage */
    uintptr_t first_free = scan;
    while (!get_threaded_header_markbit(hdrp)) {
      size_t size = hdrp->size;
      scan += size << LOG_BYTES_IN_GRANULE;
      assert(scan <= end);
      COUNT_DEAD_OBJECT(*hdrp, hdrp->size);
      if (scan == end) {
	merge_free_space_in_ordinary_area(first_free, scan);
	goto ORDINARY_AREA_DONE;
      }
      hdrp = (header_t *) scan;
    }
    merge_free_space_in_ordinary_area(first_free, scan);

    /* process live object */
    assert((uintptr_t) hdrp < end);
    assert(get_threaded_header_markbit(hdrp));
    void *from = header_to_payload(hdrp);
    header_t *to_hdrp = (header_t *) free;
    void *to = header_to_payload(to_hdrp);

#ifdef GC_DEBUG
    if (get_threaded_header(hdrp).size > 1) {
      header_t **shadow = (header_t **) get_shadow(hdrp);
      shadow[1] = to_hdrp;
    }
#endif /* GC_DEBUG */

    header_t hdr = get_threaded_header(hdrp);
    uintjsv_t tag = get_ptag_value_by_cell_type(hdr.type);
    update_reference(tag, from, to);
    size_t size = hdrp->size;
    COUNT_LIVE_OBJECT(hdr, size);
    process_node<ThreadTracer>((uintptr_t) from);

    free += size << LOG_BYTES_IN_GRANULE;
    scan += size << LOG_BYTES_IN_GRANULE;
  }
 ORDINARY_AREA_DONE:

#ifndef GC_THREADED_NO_HCGC
  /* hidden class area */
  scan = js_space.tail;
#ifdef GC_THREADED_BOUNDARY_TAG
  /* There is an object header at the end of the heap, holding the
   * size of the last object */
  scan -= HEADER_GRANULES << LOG_BYTES_IN_GRANULE;
#endif /* GC_THREADED_BOUNDARY_TAG */
  end = js_space.end;
  free = scan;
  while (scan > end) {
    /*
     * In this loop, scan, size, and hdrp are updated nimultaneously
     * so that they hold:
     *   hdrp             scan
     *    V                V
     *   |hd|   payload   |bt|
     *    <---  size  --->
     */

    scan -= BOUNDARY_TAG_GRANULES << LOG_BYTES_IN_GRANULE;
    size_t size = read_boundary_tag(scan);
    header_t *hdrp = (header_t *) (scan - (size << LOG_BYTES_IN_GRANULE));

    /* skip free/garbage */
    uintptr_t free_end = scan;
    uintptr_t first_free = (uintptr_t) hdrp;
    uintptr_t last_free = scan;
    while (!is_reference(*(void ***) hdrp) && !is_marked_cell_header(hdrp)) {
      COUNT_DEAD_OBJECT(*hdrp, size);
      scan = (uintptr_t) hdrp;
      last_free = scan;
      assert(scan >= end);
      if (scan == end) {
	merge_free_space_in_hidden_class_area(last_free, free_end, first_free);
	goto HIDDEN_CLASS_AREA_DONE;
      }
      scan -= BOUNDARY_TAG_GRANULES << LOG_BYTES_IN_GRANULE;
      size = read_boundary_tag(scan);
      hdrp = (header_t *) (scan - (size << LOG_BYTES_IN_GRANULE));
    }
    merge_free_space_in_hidden_class_area(last_free, free_end, first_free);

    /* process live object */
    assert(((uintptr_t) hdrp) >= end);
    assert(get_threaded_header_markbit(hdrp));
    COUNT_LIVE_OBJECT(get_threaded_header(hdrp), size);
    free -= (size + BOUNDARY_TAG_GRANULES) << LOG_BYTES_IN_GRANULE;
    void *from = header_to_payload(hdrp);
    header_t *to_hdrp = (header_t *) free;
    void *to = header_to_payload(to_hdrp);
#ifdef GC_DEBUG
    if (size > 1) {
      header_t **shadow = (header_t **) get_shadow(hdrp);
      shadow[1] = to_hdrp;
    }
#endif /* GC_DEBUG */
    update_reference(0, from, to);
    process_node<ThreadTracer>((uintptr_t) from);
    scan = (uintptr_t) hdrp;
  }
 HIDDEN_CLASS_AREA_DONE:
#ifdef GC_THREADED_BOUNDARY_TAG
  assert(read_boundary_tag(scan) == 0);
#endif /* GC_THREADED_BOUNDARY_TAG */
#endif /* GC_THREADED_NO_HCGC*/
  return;
}

static void update_backward_reference()
{
  uintptr_t scan = js_space.head;
  uintptr_t end = js_space.begin;
  uintptr_t free = scan;
  while (scan < end) {
    header_t *hdrp = (header_t *) scan;
    size_t size;

    if (get_threaded_header_markbit(hdrp)) {
      void *from = header_to_payload(hdrp);
      header_t *to_hdrp = (header_t *) free;
      void *to = header_to_payload(to_hdrp);

#ifdef GC_DEBUG
      if (get_threaded_header(hdrp).size > 1) {
	header_t **shadow = (header_t **) get_shadow(hdrp);
	assert(shadow[1] == to_hdrp);
      }
#endif /* GC_DEBUG */
      header_t hdr = get_threaded_header(hdrp);
      uintjsv_t tag = get_ptag_value_by_cell_type(hdr.type);
      update_reference(tag, from, to);
      size = hdrp->size;
      unmark_cell_header(hdrp);
      copy_object(hdrp, to_hdrp, size);
#ifdef GC_DEBUG
      {
        header_t *shadow = get_shadow(to_hdrp);
        *shadow = *to_hdrp;
	if (hdrp->size > 1)
	  ((uintptr_t *)shadow)[1] = (uintptr_t) from;
      }
#endif
      free += size << LOG_BYTES_IN_GRANULE;
    } else {
      size = hdrp->size;
      if (size == 0)
	size = *(granule_t *)(scan + (HEADER_GRANULES << BYTES_IN_GRANULE));
    }

    scan += size << LOG_BYTES_IN_GRANULE;
  }
  js_space.begin = free;

#ifndef GC_THREADED_NO_HCGC
  scan = js_space.tail;
#ifdef GC_THREADED_BOUNDARY_TAG
  /* There is an object header at the end of the heap, holding the
   * size of the last object */
  scan -= HEADER_GRANULES << LOG_BYTES_IN_GRANULE;
#endif /* GC_THREADED_BOUNDARY_TAG */
  end = js_space.end;
  free = scan;
  while (scan > end) {
    /* skip boundary tag */
    scan -= BOUNDARY_TAG_GRANULES << LOG_BYTES_IN_GRANULE;
    /* scan points to the next address of the last byte of the object */
    size_t size = read_boundary_tag(scan);
    header_t *hdrp = (header_t *) (scan - (size << LOG_BYTES_IN_GRANULE));

    if (get_threaded_header_markbit(hdrp)) {
      free -= BOUNDARY_TAG_GRANULES << LOG_BYTES_IN_GRANULE;
      header_t *to_hdrp = (header_t *) (free - (size << LOG_BYTES_IN_GRANULE));
      void *from = header_to_payload(hdrp);
      void *to = header_to_payload(to_hdrp);
#ifdef GC_DEBUG
      if (size > 1) {
	header_t **shadow = (header_t **) get_shadow(hdrp);
	assert(shadow[1] == to_hdrp);
      }
#endif /* GC_DEBUG */
      update_reference(0, from, to);
      copy_object_reverse((uintptr_t) hdrp, scan, free);
      unmark_cell_header(to_hdrp);
      write_boundary_tag(free, size);
#ifdef GC_DEBUG
      {
        header_t *shadow = get_shadow(to_hdrp);
        *shadow = *to_hdrp;
	if (hdrp->size > 1)
	  ((uintptr_t *)shadow)[1] = (uintptr_t) from;
      }
#endif
      free = (uintptr_t) to_hdrp;
    }
    scan = (uintptr_t) hdrp;
  }

#ifdef GC_THREADED_BOUNDARY_TAG
  assert(read_boundary_tag(scan) == 0);
  write_boundary_tag(free, 0);
#endif /* GC_THREADED_BOUNDARY_TAG */
  js_space.end = free;
#endif /* GC_THREADED_NO_HCGC */

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

#ifndef GC_THREADED_NO_HCGC
static void copy_object_reverse(uintptr_t from,
				uintptr_t from_end, uintptr_t to_end)
{
  if (from_end == to_end)
    return;
  granule_t *p = (granule_t *) from_end;
  granule_t *q = (granule_t *) to_end;
  granule_t *end = (granule_t *) from;
  while(end < p)
    *--q = *--p;
}
#endif /* GC_THREADED_NO_HCGC */

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

