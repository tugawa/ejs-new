/*
   localcall disp
 */
{
  disp = get_first_operand_disp(insn);
  lcall_stack_push(context, pc);
  set_pc_relative(disp);
}
