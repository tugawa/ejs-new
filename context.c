#include "prefix.h"
#define EXTERN
#include "header.h"

static Context *allocate_context(size_t);

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
		       HTAG_FUNCTION_FRAME);
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

  c = allocate_context(STACK_LIMIT);
  *context = c;

  init_special_registers(&(c->spreg));
  c->function_table = ftab;
  c->global = glob;

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

static Context *allocate_context(size_t stack_size)
{
  /* GC is not allowed */
  Context *ctx = (Context *) gc_malloc_critical(sizeof(Context), HTAG_CONTEXT);
  ctx->stack = (JSValue *) gc_malloc_critical(sizeof(JSValue) * stack_size,
					       HTAG_STACK);
  ctx->exhandler_stack = new_array(NULL);
  ctx->exhandler_stack_ptr = 0;
  ctx->lcall_stack = new_array(NULL);
  ctx->lcall_stack_ptr = 0;
  return ctx;
}


/*
 * TODO: tidyup debug fuctions
 */
int in_js_space(void *addr_);
int in_malloc_space(void *addr_);

int is_valid_JSValue(JSValue x)
{
  switch(get_tag(x)) {
  case T_OBJECT:
    return in_js_space((void *) x);
  case T_STRING:
    if (!in_js_space((void *) x))
      return 0;
    return ((*(uint64_t *) (x & ~7)) & 0xff) == HTAG_STRING;
  case T_FLONUM:
    if (!in_js_space((void *) x))
      return 0;
    return ((*(uint64_t *) (x & ~7)) & 0xff) == HTAG_FLONUM;
  case T_SPECIAL:
    return (x == JS_TRUE ||
	    x == JS_FALSE ||
	    x == JS_NULL ||
	    x == JS_UNDEFINED);
  case T_FIXNUM:
    return 1;
  default:
    return 0;
  }
}

void check_stack_invariant(Context *ctx)
{
  int sp = get_sp(ctx);
  int fp = get_fp(ctx);
  int pc = get_pc(ctx);
  FunctionTable *cf = get_cf(ctx);
  FunctionFrame *lp = get_lp(ctx);
  int i;

  assert(is_valid_JSValue(get_global(ctx)));
  assert(is_valid_JSValue(get_a(ctx)));
  assert(!is_err(ctx) || is_valid_JSValue(ctx->spreg.err));
  while (1) {
    for (i = sp; i >= fp; i--)
      assert(is_valid_JSValue(get_stack(ctx,i)));
    if (fp == 0)
      break;
    sp = fp - 1;
    fp = get_stack(ctx, sp); sp--;
    lp = (FunctionFrame *) get_stack(ctx, sp); sp--;
    pc = get_stack(ctx, sp); sp--;
    cf = (FunctionTable *) get_stack(ctx, sp); sp--;
  }
}
