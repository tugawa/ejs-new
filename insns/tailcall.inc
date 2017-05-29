/*
   tailcall fn nargs
   tailsend fn nargs
 */
{
  JSValue fn = v0;
  int nargs = i1;
  int sendp;

  sendp = (get_opcode(insn) == TAILSEND)? TRUE: FALSE;
  fn = regbase[get_first_operand_reg(insn)];
  set_fp(context, fp);
  set_pc(context, pc);

  if (is_function(fn)) {
#ifdef CALC_CALL
    callcount++;
#endif
    // function
    tailcall_function(context, fn, nargs, sendp);
    update_context();
    NEXT_INSN_NOINCPC();
  } else if (is_builtin(fn)) {
    tailcall_builtin(context, fn, nargs, sendp, FALSE);
    update_context();        // is this necessary? => yes. moving GC
    NEXT_INSN_INCPC();
  } else
    LOG_EXIT("TAILCALL/TAILSEND: not a function\n");
}


