/*
   localcall disp
 */
{
  lcall_stack_push(context, pc);
  set_pc_relative(d0);
}
