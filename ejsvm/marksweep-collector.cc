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
#ifdef CXX_TRACER
class MarkTracer {
 public:
  static constexpr bool is_pointer_updating() {
    return false;
  }

  // do implement
  static void process_edge(void *p);
  static void process_edge(JSValue v);
  static void process_edge_function_frame(JSValue v) {
    void *p = jsv_to_function_frame(v);
    process_edge(p);
  }
  static void mark_cell(void *p) {
    ::mark_cell(p);
  }
  static bool test_and_mark_cell(void *p) {
    return ::test_and_mark_cell(p);
  }

  // do not implement
  static void process_edge(void **pp);
  static void process_edge(JSValue *vp);
  static void process_edge_function_frame(JSValue *vp);
  static void mark_cell(void **ptr);
  static bool test_and_mark_cell(void **p);

  static bool is_marked_cell(void *p) { return ::is_marked_cell(p); }

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
#endif /* MARK_STACK */

};

class RefMarkTracer {
 public:
  static constexpr bool is_pointer_updating() {
    return true;
  }

  // do not implement
  static void process_edge(void *p);
  static void process_edge(JSValue v);
  static void process_edge_function_frame(JSValue v);
  static void mark_cell(void *p);
  static bool test_and_mark_cell(void *p);

  // do implement
  static void process_edge(void **pp);
  static void process_edge(JSValue *vp);
  static void process_edge_function_frame(JSValue *vp) {
    FunctionFrame *frame = jsv_to_function_frame(*vp);
    process_edge((void **) &frame);
    *vp = (JSValue) (uintjsv_t) (uintptr_t) frame;
  }
  static void mark_cell(void **p) {
    ::mark_cell(*p);
  }
  static bool test_and_mark_cell(void **p) {
    return ::test_and_mark_cell(*p);
  }

  static bool is_marked_cell(void *p) { return ::is_marked_cell(p); }

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
#endif /* MARK_STACK */
};

#ifdef CXX_TRACER_CBV
typedef MarkTracer DefaultTracer;
uintptr_t MarkTracer::mark_stack[MARK_STACK_SIZE];
int MarkTracer::mark_stack_ptr;
#else /* CXX_TRACER_CBV */
typedef RefMarkTracer DefaultTracer;
uintptr_t RefMarkTracer::mark_stack[MARK_STACK_SIZE];
int RefMarkTracer::mark_stack_ptr;
#endif /* CXX_TRACER_CBV */

#endif /* CXX_TRACER */

/*
 * GC
 */

#ifndef CXX_TRACER
#ifdef MARK_STACK
STATIC_INLINE void mark_stack_push(uintptr_t ptr)
{
  assert(mark_stack_ptr < MARK_STACK_SIZE);
  mark_stack[mark_stack_ptr++] = ptr;
}

STATIC_INLINE uintptr_t mark_stack_pop()
{
  return mark_stack[--mark_stack_ptr];
}

STATIC_INLINE int mark_stack_is_empty()
{
  return mark_stack_ptr == 0;
}

STATIC void process_mark_stack()
{
  while (!mark_stack_is_empty()) {
    uintptr_t ptr = mark_stack_pop();
    process_node(ptr);
  }
}
#endif /* MARK_STACK */
#endif /* CXX_TRACER */

void garbage_collection(Context *ctx)
{
  /* initialise */
  gc_phase = PHASE_INITIALISE;
  the_context = ctx;

  /* mark */
  gc_phase = PHASE_MARK;
#ifdef CXX_TRACER
  scan_roots<DefaultTracer>(ctx);
#else /* CXX_TRACER */
  scan_roots(ctx);
#endif /* CXX_TRACER */

#ifdef MARK_STACK
#ifdef CXX_TRACER
  DefaultTracer::process_mark_stack();
#else /* CXX_TRACER */
  process_mark_stack();
#endif /* CXX_TRACER */
#endif /* MARK_STACK */

  /* profile */
#ifdef CHECK_MATURED
  check_matured();
#endif /* CHECK_MATURED */

  /* weak */
  gc_phase = PHASE_WEAK;
#ifdef CXX_TRACER
  weak_clear<DefaultTracer>();
#else /* CXX_TRACER */
  weak_clear();
#endif /* CXX_TRACER */

  /* sweep */
  gc_phase = PHASE_SWEEP;
  sweep();

  /* finalise */
  gc_phase = PHASE_FINALISE;

  gc_phase = PHASE_INACTIVE;
}

#ifdef CXX_TRACER
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
void RefMarkTracer::process_edge(void **pp)
{
  void *p = *pp;
  if (in_js_space(p) && ::test_and_mark_cell(p))
    return;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<RefMarkTracer>(p);
#endif /* MARK_STACK */
}

void RefMarkTracer::process_edge(JSValue *vp)
{
  JSValue v = *vp;
  if (is_fixnum(v) || is_special(v))
    return;
  void *p = (void *)(uintptr_t) clear_ptag(v);
  if (in_js_space(p) && ::test_and_mark_cell(p))
    return;
#ifdef MARK_STACK
  mark_stack_push((uintptr_t) p);
#else /* MARK_STACK */
  process_node<RefMarkTracer>(p);
#endif /* MARK_STACK */
}
#endif /* CXX_TRACER_CBV */
#else /* CXX_TRACER */
STATIC void process_edge(uintptr_t ptr)
{
  if (is_fixnum(ptr) || is_special(ptr))
    return;

  ptr = ptr & ~TAGMASK;
  if (in_js_space((void *) ptr) && test_and_mark_cell((void *) ptr))
    return;

#ifdef MARK_STACK
  mark_stack_push(ptr);
#else /* MARK_STACK */
  process_node(ptr);
#endif /* MARK_STACK */
}
#endif /* CXX_TRACER */


