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
class MarkTracer {
 public:
  static void process_edge(JSValue v);
  static void process_edge(void *p);
  static void process_edge_function_frame(JSValue v) {
    void *p = jsv_to_function_frame(v);
    process_edge(p);
  }
  static void process_edge_ex_JSValue_array(JSValue *p, size_t n) {
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (int i = 0; i < n; i++)
      process_edge(p[i]);
  }
  static void process_edge_ex_ptr_array(void **p, size_t n) {
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (int i = 0; i < n; i++)
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
#endif /* MARK_STACK */
};

class RefMarkTracer {
 public:
  static void process_edge(JSValue &v);
  static void process_edge(void *&p);
  static void process_edge_function_frame(JSValue &v) {
    FunctionFrame *frame = jsv_to_function_frame(v);
    process_edge(reinterpret_cast<void *&>(frame));
    v = (JSValue) (uintjsv_t) (uintptr_t) frame;
  }
  static void process_edge_ex_JSValue_array(JSValue *&p, size_t n) {
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (int i = 0; i < n; i++)
      process_edge(p[i]);
  }
  static void process_edge_ex_ptr_array(void **&p, size_t n) {
    if (in_js_space(p) && ::test_and_mark_cell(p))
      return;
    for (int i = 0; i < n; i++)
      if (p[i] != NULL)
	process_edge(p[i]);
  }
  static void process_node_JSValue_array(JSValue *&p) { abort(); }
  static void process_node_ptr_array(void **&p) { abort(); }

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
#endif /* MARK_STACK */
};

#ifdef CXX_TRACER_CBV
typedef MarkTracer DefaultTracer;
#ifdef MARK_STACK
uintptr_t MarkTracer::mark_stack[MARK_STACK_SIZE];
int MarkTracer::mark_stack_ptr;
#endif /* MARK_STACK */
#else /* CXX_TRACER_CBV */
typedef RefMarkTracer DefaultTracer;
#ifdef MARK_STACK
uintptr_t RefMarkTracer::mark_stack[MARK_STACK_SIZE];
int RefMarkTracer::mark_stack_ptr;
#endif /* MARK_STACK */
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

#ifdef MARK_STACK
  DefaultTracer::process_mark_stack();
#endif /* MARK_STACK */

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
  process_edge_ptr((void *) ptr);
}
#else /* CXX_TRACER_CBV */
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
#endif /* CXX_TRACER_CBV */


