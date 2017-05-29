/*
   throw
 */
{
  Displacement disp;
  int newpc;
  int handler_fp;
  exhandler_stack_pop(context, &newpc, &handler_fp);
  while (handler_fp != fp) {
    JSValue *stack;
    stack = &get_stack(context, 0);
    restore_special_registers(context, stack, fp - 4);
    set_sp(context, fp - 5);
    update_context();      /* TODO: optimise */
  }
  disp = (Displacement) (newpc - pc);
  set_pc_relative(disp);
}
