#include "prefix.h"
#define EXTERN
#include "header.h"
#include "gc.h"

#define allocate_context()  ((Context *)malloc(sizeof(Context)))

// creates a new function frame
//
FunctionFrame *new_frame(FunctionTable *ft, FunctionFrame *env) {
  FunctionFrame *frame;
  JSValue *locals;
  int nl, i;
#ifdef PARALLEL
  pthread_mutexattr_t attr;
#endif

  nl = ftab_n_locals(ft);
  nl++;   /* GC_DEBUG (canary; search GC_DEBUG in gc.c) */
  frame = (FunctionFrame *)
    gc_malloc_critical(sizeof(FunctionFrame) + BYTES_IN_JSVALUE * nl,
		       MATYPE_FUNCTION_FRAME);
  frame->prev_frame = env;
  frame->arguments = JS_UNDEFINED;
  locals = frame->locals;
  for (i = 0; i < nl; i++)
    locals[i] = JS_UNDEFINED;
#ifdef PARALLEL
  // initilizes the mutex
  // Is it possible to reuse attr???
  pthread_mutexattr_init(&attr);
  pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);
  pthread_mutex_init(&(frame->mutex), &attr);
  pthread_mutexattr_destroy(&attr);
#endif
  return frame;
}

// initializes special registers in a context
//
void init_special_registers(SpecialRegisters *spreg){
  spreg->fp = 0;
  spreg->cf = NULL;
  spreg->lp = NULL;
  spreg->sp = 0;
  spreg->pc = 0;
  spreg->a = JS_UNDEFINED;
  spreg->err = JS_UNDEFINED;
  spreg->iserr = false;
}

#if 0
void pop_special_registers(Context *context, int fp, JSValue *regbase) {
  set_fp(context, (int)regbase[-FP_POS]);
  set_lp(context, (FunctionFrame *)regbase[-LP_POS]);
  set_pc(context, (int)regbase[-PC_POS]);
  set_cf(context, (FunctionTable *)regbase[-CF_POS]);
  printf("pop_special_registers, fp: %p, lp: %p, pc: %p, cf: %p\n",
    &regbase[-FP_POS], &regbase[-LP_POS], &regbase[-PC_POS], &regbase[-CF_POS]);
  set_sp(context, fp);
}
#endif

void init_context(FunctionTable *ftab, JSValue glob, Context **context) {
  Context *c;

  c = allocate_context();
  *context = c;

  init_special_registers(&(c->spreg));
  c->function_table = ftab;
  c->global = glob;
  c->catch_stacktop = -1;

  set_cf(c, ftab);
  set_lp(c, new_frame(ftab, NULL));

#ifdef USE_FFI
  initForeignFunctionInterface(c);
#endif

#ifdef PARALLEL
  c->inParallel = false;
  c->threadId = 0;
  c->eventQueue = newEventQueue();
#endif
}
