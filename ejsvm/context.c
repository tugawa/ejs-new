/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN
#include "header.h"

static Context *allocate_context(size_t);

/*
 * creates a new function frame
 */
FunctionFrame *new_frame(Context *ctx, FunctionTable *ft,
                         FunctionFrame *env, int nl) {
  FunctionFrame *frame;
  JSValue *locals;
  int i;

#ifdef DEBUG
  nl++;
#endif /* DEBUG */
  GC_PUSH(env);
  frame = (FunctionFrame *)
    gc_malloc(ctx, sizeof(FunctionFrame) + BYTES_IN_JSVALUE * nl,
              CELLT_FUNCTION_FRAME);
  GC_POP(env);
  frame->prev_frame = env;
  frame->arguments = JS_UNDEFINED;
  frame->nlocals = nl;
  locals = frame->locals;
  for (i = 0; i < nl; i++)
    locals[i] = JS_UNDEFINED;
  return frame;
}

/*
 * initializes special registers in a context
 */
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

void reset_context(Context *ctx, FunctionTable *ftab) {
  init_special_registers(&(ctx->spreg));
  ctx->function_table = function_table;
  set_cf(ctx, ftab);
  set_lp(ctx, new_frame(NULL, ftab, NULL, 0));

  ctx->global = gconsts.g_global;
  ctx->exhandler_stack_top = NULL;
  ctx->exhandler_pool = NULL;
  ctx->lcall_stack = new_array_object(NULL, DEBUG_NAME("allocate_context"),
                                      gshapes.g_shape_Array, 0);
  ctx->lcall_stack_ptr = 0;
}

/*
 * Create context with minimum initialisation to create objects.
 * Note that global objects are not created because their creation needs
 * context.  Bottom half of initialisation is done in reset_context.
 */
void init_context(size_t stack_limit, Context **context)
{
  Context *c;

  c = allocate_context(stack_limit);
  *context = c;
#if defined(HC_SKIP_INTERNAL) || defined(WEAK_SHAPE_LIST)
  c->property_map_roots = NULL;
#endif /* HC_SKIP_INTERNAL || WEAK_SHAPE_LIST */
}

static Context *allocate_context(size_t stack_size)
{
  Context *ctx = (Context *) malloc(sizeof(Context));
  ctx->stack = (JSValue *) malloc(sizeof(JSValue) * stack_size);
  return ctx;
}

static void print_single_frame(Context *ctx, int index)
{
  fprintf(log_stream,
          "  #%d %p LP:%p, PC: %d, SP:%d, FP:%d (#i = %d, #c = %d)\n",
          index,
          ctx->spreg.cf,
          ctx->spreg.lp,
          ctx->spreg.pc,
          ctx->spreg.sp,
          ctx->spreg.fp,
          ctx->spreg.cf->n_insns,
          ctx->spreg.cf->n_constants);
}

void print_backtrace(Context *ctx)
{
  JSValue *stack = &get_stack(ctx, 0);
  int i = 0;
  fprintf(log_stream, "backtrace:\n");
  print_single_frame(ctx, i++);
  while (ctx->spreg.fp != 0) {
    ctx->spreg.sp = ctx->spreg.fp - 5;
    restore_special_registers(ctx, stack, ctx->spreg.fp - 4);
    print_single_frame(ctx, i++);
  };
}



/*
 * TODO: tidyup debug fuctions
 */
int in_js_space(void *addr_);
int in_malloc_space(void *addr_);

/*
 * Need to generate automatically because this validator
 * depends on type representation.
 */
int is_valid_JSValue(JSValue x)
{
  return 1;
}

void check_stack_invariant(Context *ctx)
{
  int sp = get_sp(ctx);
  int fp = get_fp(ctx);
  int pc __attribute__((unused)) = get_pc(ctx);
  FunctionTable *cf __attribute__((unused)) = get_cf(ctx);
  FunctionFrame *lp __attribute__((unused)) = get_lp(ctx);
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
    fp = (int) (intjsv_t) get_stack(ctx, sp); sp--;
    lp = jsv_to_function_frame(get_stack(ctx, sp)); sp--;
    pc = (int) (intjsv_t) get_stack(ctx, sp); sp--;
    cf = (FunctionTable *) jsv_to_noheap_ptr(get_stack(ctx, sp)); sp--;
  }
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
