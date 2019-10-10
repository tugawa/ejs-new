#ifndef CONTEXT_INL_H
#define CONTEXT_INL_H

static inline void save_special_registers(Context *ctx, JSValue *stack, int pos)
{
  stack[pos + 0] = noheap_ptr_to_jsv(get_cf(ctx));
  stack[pos + 1] = (JSValue) (intjsv_t) get_pc(ctx);
  stack[pos + 2] = function_frame_to_jsv(get_lp(ctx));
  stack[pos + 3] = (JSValue) (intjsv_t) get_fp(ctx);
}

static inline void restore_special_registers(Context *ctx, JSValue *stack,
                                             int pos)
{
  set_cf(ctx, (FunctionTable *) jsv_to_uintptr(stack[pos]));
  set_pc(ctx, (uintjsv_t) stack[pos + 1]);
  set_lp(ctx, (FunctionFrame *) jsv_to_uintptr(stack[pos + 2]));
  set_fp(ctx, (uintjsv_t) stack[pos + 3]);
}

#endif /* CONTEXT_INL_H */
