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

#ifdef CXX_TRACER_CBV
#error "Threaded compactor does not except macro; CXX_TRACER_CBV."
#endif /* CXX_TRACER_CBV */
#ifdef CXX_TRACER_RV
#error "Threaded compactor does not except macro; CXX_TRACER_RV."
#endif /* CXX_TRACER_RV */

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
class RefMarkTracer {
 public:
  static void process_edge(JSValue &v);
  static void process_edge(void *&p);
  static void process_edge_function_frame(JSValue &v) {
    FunctionFrame *frame = jsv_to_function_frame(v);
    process_edge(reinterpret_cast<void *&>(frame));
    v = (JSValue) (uintjsv_t) (uintptr_t) frame;
  }
  template<typename T>
  static void process_edge_ex_JSValue_array(T &p_, size_t n) {
    JSValue *&p = (JSValue *&) p_;

    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (size_t i = 0; i < n; i++)
      process_edge(p[i]);
  }
  template<typename T>
  static void process_edge_ex_ptr_array(T &p_, size_t n) {
    void **&p = (void **&) p_;
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (size_t i = 0; i < n; i++)
      if (p[i] != NULL)
        process_edge(p[i]);
  }
  template <typename T>
  static void process_node_JSValue_array(T &p) { abort(); }
  template <typename T>
  static void process_node_ptr_array(T &p) { abort(); }

  static void process_weak_edge(JSValue &v) {}
  static void process_weak_edge(void *&p) {}
  static bool is_marked_cell(void *p) {
    return ::is_marked_cell(p);
  }

#ifdef MARK_STACK
#define MARK_STACK_SIZE 1000 * 1000
  static uintptr_t mark_stack[MARK_STACK_SIZE];
  static int mark_stack_ptr;

  static void mark_stack_push(uintptr_t ptr){
    assert(mark_stack_ptr < MARK_STACK_SIZE);
    mark_stack[mark_stack_ptr++] = ptr;
  }
  static uintptr_t mark_stack_pop(void) {
    return mark_stack[--mark_stack_ptr];
  }
  static bool mark_stack_is_empty(void) {
    return mark_stack_ptr == 0;
  }
  static void process_mark_stack(void) {
    while (!mark_stack_is_empty()) {
      uintptr_t ptr = mark_stack_pop();
      process_node<RefMarkTracer>(ptr);
    }
  }
#else /* MARK_STACK */
  static void process_mark_stack(void) {}
#endif /* MARK_STACK */
};
typedef RefMarkTracer DefaultTracer;

#ifdef MARK_STACK
uintptr_t RefMarkTracer::mark_stack[MARK_STACK_SIZE];
int RefMarkTracer::mark_stack_ptr;
#endif /* MARK_STACK */


static bool is_reference(void **pptr);
static void thread_reference(void **ref);
static void update_reference(void *ref, void *addr);

static cell_type_t get_threaded_header_type(header_t *hdrp);
static bool get_threaded_header_markbit(header_t *hdrp);
static unsigned int get_threaded_header_size(header_t *hdrp);

class RootTracer {
public:
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

    assert(in_js_space((void *) p));
    assert(!in_hc_space((void *) p));

    header_t *hdrp = payload_to_header(p);
    size_t payload_granules = hdrp->size - HEADER_GRANULES;
    size_t slots = payload_granules * (BYTES_IN_GRANULE / sizeof(void *));
    for (size_t i = 0; i < slots; i++)
      if (p[i] != NULL)
        thread_reference(&p[i]);
  }
  static void process_weak_edge(JSValue &v) { process_edge(v); }
  static void process_weak_edge(void *&p) { process_edge(p); }
};
class ObjTracer {
public:
  static void process_edge(JSValue &v) {
    if (is_fixnum(v) || is_special(v))
      return;

    assert(!(in_hc_space(&v) && in_hc_space((void *) clear_ptag(v))));

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

    assert(!(in_hc_space(&p) && in_hc_space(p)));

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

    assert(!(in_hc_space(&v) && in_hc_space((void *) clear_ptag(v))));

    assert(in_obj_space((void *) v));

    thread_reference((void **) &v);
  }
  template <typename T>
  static void process_edge_ex_JSValue_array(T &p, size_t n) {
    if ((void *) p == NULL)
      return;

    assert(!(in_hc_space(&p) && in_hc_space((void *) p)));

    assert(in_obj_space((void *) p));

    thread_reference((void **) &p);
  }
  template <typename T>
  static void process_edge_ex_ptr_array(T &p, size_t n) {
    if ((void *) p == NULL)
      return;

    assert(!(in_hc_space(&p) && in_hc_space(p)));

    assert(in_js_space((void *) p));

    thread_reference((void **) &p);
  }
  static void process_node_JSValue_array(JSValue *p) {
    if (p == NULL)
      return;

    assert(!(in_hc_space(&p) && in_hc_space(p)));

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

    assert(!(in_hc_space(&p) && in_hc_space(p)));

    assert(in_js_space((void *) p));
    assert(!in_hc_space((void *) p));

    header_t *hdrp = payload_to_header(p);
    size_t payload_granules = hdrp->size - HEADER_GRANULES;
    size_t slots = payload_granules * (BYTES_IN_GRANULE / sizeof(void *));
    for (size_t i = 0; i < slots; i++)
      if (p[i] != NULL)
        thread_reference(&p[i]);
  }
  static void process_weak_edge(JSValue &v) { process_edge(v); }
  static void process_weak_edge(void *&p) { process_edge(p); }
};
class HCTracer {
public:
  static void process_edge(JSValue &v) {
    if (is_fixnum(v) || is_special(v))
      return;

    uintjsv_t ptr = clear_ptag(v);
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

    assert(in_js_space((void *) p));

    header_t *hdrp = payload_to_header(p);
#ifdef GC_THREADED_BOUNDARY_TAG
    size_t payload_granules;
    if (in_hc_space((void *) p))
      payload_granules = ((footer_t *) hdrp)->size_lo - HEADER_GRANULES;
    else
      payload_granules = hdrp->size - HEADER_GRANULES;
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
  uintjsv_t tag = 0;

  switch(type) {
    case CELLT_FREE:
      LOG_EXIT("unreachable code.");
      abort();
      break;

    /* User defined types */
    case CELLT_STRING:
      tag = GC_GET_PTAG_FOR_HTAG_STRING().v;
      break;
    case CELLT_FLONUM:
      tag = GC_GET_PTAG_FOR_HTAG_FLONUM().v;
      break;
    case CELLT_SIMPLE_OBJECT:
      tag = GC_GET_PTAG_FOR_HTAG_SIMPLE_OBJECT().v;
      break;
    case CELLT_ARRAY:
      tag = GC_GET_PTAG_FOR_HTAG_ARRAY().v;
      break;
    case CELLT_FUNCTION:
      tag = GC_GET_PTAG_FOR_HTAG_FUNCTION().v;
      break;
    case CELLT_BUILTIN:
      tag = GC_GET_PTAG_FOR_HTAG_BUILTIN().v;
      break;
    case CELLT_ITERATOR:
      tag = GC_GET_PTAG_FOR_HTAG_ITERATOR().v;
      break;
#ifdef use_regexp
    case CELLT_REGEXP:
      tag = GC_GET_PTAG_FOR_HTAG_REGEXP().v;
      LOG_EXIT("Not Implemented");
      break;
#endif
    case CELLT_BOXED_STRING:
      tag = GC_GET_PTAG_FOR_HTAG_BOXED_STRING().v;
      break;
    case CELLT_BOXED_NUMBER:
      tag = GC_GET_PTAG_FOR_HTAG_BOXED_NUMBER().v;
      break;
    case CELLT_BOXED_BOOLEAN:
      tag = GC_GET_PTAG_FOR_HTAG_BOXED_BOOLEAN().v;
      break;

    /* VM inner defined types */
    case CELLT_PROP:
    case CELLT_ARRAY_DATA:
    case CELLT_BYTE_ARRAY:
    case CELLT_FUNCTION_FRAME:
    case CELLT_STR_CONS:
    // case CELLT_CONTEXT: /* no longer used */
    // case CELLT_STACK: /* no longer used */
    case CELLT_HASHTABLE:
    case CELLT_HASH_BODY:
    case CELLT_HASH_CELL:
    case CELLT_PROPERTY_MAP:
    case CELLT_SHAPE:
    case CELLT_UNWIND:
    case CELLT_PROPERTY_MAP_LIST:
      tag = (uintjsv_t) -1;
      break;

    default:
      LOG_EXIT("Unreachable code.");
      abort();
      break;
  }

  void **ref = (void **) hdrp->threaded;
  while(is_reference(ref)) {
    void **next = (void **) *ref;
    if (tag != (uintjsv_t) -1)
      *ref = (void *) put_ptag((uintjsv_t) addr, ((PTag) {tag}));
    else
      *ref = addr;

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
  while(is_reference((void **) hdrp->threaded)) {
    hdrp = (header_t *) hdrp->threaded;
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
  gc_phase = PHASE_MARK;
  scan_roots<DefaultTracer>(ctx);
  DefaultTracer::process_mark_stack();

  /* profile */
#ifdef CHECK_MATURED
  check_matured();
#endif /* CHECK_MATURED */

  /* weak */
  gc_phase = PHASE_WEAK;
  weak_clear<DefaultTracer>();

  /* forwarding reference */
  gc_phase = PHASE_FWDREF;
  update_forward_reference(ctx);

  /* backwarding reference */
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
  scan_roots<RootTracer>(ctx);

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

      update_reference(from, to);
      process_node<ObjTracer>((uintptr_t) from);

      free += size << LOG_BYTES_IN_JSVALUE;
    }

    scan += size << LOG_BYTES_IN_JSVALUE;
  }

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

    if (markbit)
    {
      void *from = header_to_payload(hdrp);
      header_t *to_hdrp = (header_t *) free;
      void *to = header_to_payload(to_hdrp);

      update_reference(from, to);
      process_node<HCTracer>((uintptr_t) from);
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

      update_reference(from, to);
      unmark_cell_header(hdrp);
      copy_object(hdrp, to_hdrp, size);

#ifdef GC_DEBUG
      {
        header_t *shadow = get_shadow(to_hdrp);
        *shadow = *to_hdrp;
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

      update_reference(from, to);
      unmark_cell_header(hdrp);
#ifdef GC_THREADED_BOUNDARY_TAG
      copy_object_reverse(hdrp, to_hdrp, size);
#else /* GC_THREADED_BOUNDARY_TAG */
      footer->markbit = 0;
      copy_object_reverse(hdrp, to_hdrp, size + HEADER_GRANULES);
#endif /* GC_THREADED_BOUNDARY_TAG */

#ifdef GC_DEBUG
      {
        header_t *shadow = get_shadow(to_hdrp);
        *shadow = *to_hdrp;
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


void RefMarkTracer::process_edge(void *&p)
{
#ifdef GC_DEBUG
  if (!is_reference((void **) p)) {
    printf("threaded pointer!!; at %p is 0x%016" PRIx64 "\n", &p, (uintptr_t) p);
    fflush(stdout);
    abort();
  }
#endif

  if (p == NULL)
    return;

  if (in_js_space(p) && ::test_and_mark_cell(p))
    return;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<RefMarkTracer>((uintptr_t) p);
#endif /* MARK_STACK */
}

void RefMarkTracer::process_edge(JSValue &v)
{
  if (is_fixnum(v) || is_special(v))
    return;

  void *p = (void *)(uintptr_t) clear_ptag(v);
  if (p == NULL)
    return;

  if (in_js_space(p) && ::test_and_mark_cell(p))
    return;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<RefMarkTracer>((uintptr_t) p);
#endif /* MARK_STACK */
}

