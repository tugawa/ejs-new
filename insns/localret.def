/*
   localret
 */
{
  Displacement disp;
  int newpc;
  lcall_stack_pop(context, &newpc);
  disp = (Displacement) (newpc - pc);
  set_pc_relative(disp);
  // NEXT_INSN_INCPC();   /* need INCPC; local_call_stack has the address of local call */
}
