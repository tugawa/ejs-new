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
  PHASE_MARK,
  PHASE_WEAK,
  PHASE_SWEEP,
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
#ifdef CXX_TRACER_CBV
class MarkTracer {
 public:
  static void process_edge(JSValue v);
  static void process_edge(void *p);
  static void process_edge_function_frame(JSValue v) {
    void *p = jsv_to_function_frame(v);
    process_edge(p);
  }
  template <typename T>
  static void process_edge_ex_JSValue_array(T p_, size_t n) {
    JSValue *p = (JSValue *) p_;
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (size_t i = 0; i < n; i++)
      process_edge(p[i]);
  }
  template <typename T>
  static void process_edge_ex_ptr_array(T p_, size_t n) {
    void **p = (void **) p_;
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (size_t i = 0; i < n; i++)
      if (p[i] != NULL)
	process_edge(p[i]);
  }
  static void process_node_JSValue_array(JSValue *p) { abort(); }
  static void process_node_ptr_array(void **p) { abort(); }

  static void process_weak_edge(JSValue v) {}
  static void process_weak_edge(void *p) {}
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
      process_node<MarkTracer>(ptr);
    }
  }
#else /* MARK_STACK */
  static void process_mark_stack(void) {}
#endif /* MARK_STACK */
};
#else /* CXX_TRACER_CBV */
#ifdef CXX_TRACER_RV
class RVTracer {
public:
  static JSValue process_edge(JSValue v);
  static void *process_edge(void *p);
  static JSValue process_edge_function_frame(JSValue v) {
    void *p = jsv_to_function_frame(v);
    return (JSValue) (uintjsv_t) (uintptr_t) process_edge(p);
  }
  template <typename T>
  static T process_edge_ex_JSValue_array(T p_, size_t n) {
    JSValue *p = (JSValue *) p_;
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return (T) p;
    for (size_t i = 0; i < n; i++)
      p[i] = process_edge(p[i]);
    return (T) p;
  }
  template <typename T>
  static T process_edge_ex_ptr_array(T p_, size_t n) {
    void **p = (void **) p_;
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return (T) p;
    for (size_t i = 0; i < n; i++)
      if (p[i] != NULL)
	p[i] = process_edge(p[i]);
    return (T) p;
  }
  static JSValue *process_node_JSValue_array(JSValue *p) { abort(); }
  static void **process_node_ptr_array(void **p) { abort(); }

  static JSValue process_weak_edge(JSValue v) { return v; }
  static void *process_weak_edge(void *p) { return p; }
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
      process_node<RVTracer>(ptr);
    }
  }
#else /* MARK_STACK */
  static void process_mark_stack(void) {}
#endif /* MARK_STACK */
};
#else /* CXX_TRACER_RV */
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
#endif /* CXX_TRACER_RV */
#endif /* CXX_TRACER_CBV */

#ifdef CXX_TRACER_CBV
typedef MarkTracer DefaultTracer;
#ifdef MARK_STACK
uintptr_t MarkTracer::mark_stack[MARK_STACK_SIZE];
int MarkTracer::mark_stack_ptr;
#endif /* MARK_STACK */
#else /* CXX_TRACER_CBV */
#ifdef CXX_TRACER_RV
typedef RVTracer DefaultTracer;
#ifdef MARK_STACK
uintptr_t RVTracer::mark_stack[MARK_STACK_SIZE];
int RVTracer::mark_stack_ptr;
#endif /* MARK_STACK */
#else /* CXX_TRACER_RV */
typedef RefMarkTracer DefaultTracer;
#ifdef MARK_STACK
uintptr_t RefMarkTracer::mark_stack[MARK_STACK_SIZE];
int RefMarkTracer::mark_stack_ptr;
#endif /* MARK_STACK */
#endif /* CXX_TRACER_RV */
#endif /* CXX_TRACER_CBV */

/*
 * GC
 */

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

  /* sweep */
  gc_phase = PHASE_SWEEP;
  sweep();

  /* finalise */
  gc_phase = PHASE_FINALISE;

  gc_phase = PHASE_INACTIVE;
}

#ifdef CXX_TRACER_CBV
void MarkTracer::process_edge(void *p)
{
  if (in_js_space(p) && ::test_and_mark_cell(p))
    return;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<MarkTracer>(p);
#endif /* MARK_STACK */
}

void MarkTracer::process_edge(JSValue v)
{
  if (is_fixnum(v) || is_special(v))
    return;
  uintptr_t ptr = (uintptr_t) clear_ptag(v);
  process_edge((void *) ptr);
}
#else /* CXX_TRACER_CBV */
#ifdef CXX_TRACER_RV
void *RVTracer::process_edge(void *p)
{
  if (in_js_space(p) && ::test_and_mark_cell(p))
    return p;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<RVTracer>((uintptr_t) p);
#endif /* MARK_STACK */
  return p;
}

JSValue RVTracer::process_edge(JSValue v)
{
  if (is_fixnum(v) || is_special(v))
    return v;
  void *p = (void *)(uintptr_t) clear_ptag(v);
  if (in_js_space(p) && ::test_and_mark_cell(p))
    return v;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<RVTracer>((uintptr_t) p);
#endif /* MARK_STACK */
  return v;
}
#else /* CXX_TRACER_RV */
void RefMarkTracer::process_edge(void *&p)
{
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
  if (in_js_space(p) && ::test_and_mark_cell(p))
    return;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<RefMarkTracer>((uintptr_t) p);
#endif /* MARK_STACK */
}
#endif /* CXX_TRACER_RV */
#endif /* CXX_TRACER_CBV */


