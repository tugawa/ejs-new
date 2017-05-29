/*
   call fn nargs
   send fn nargs
   newsend fn nargs
 */
{
  JSValue fn = v0;
  int nargs = i1;
  Opcode op;
  int sendp, newp;

  op = get_opcode(insn);
  sendp = (op != CALL)? TRUE: FALSE;
  newp = (op == NEWSEND)? TRUE: FALSE;
  set_fp(context, fp);
  set_pc(context, pc);
  if (is_function(fn)) {
#ifdef CALC_CALL
    callcount++;
#endif
    // function
    call_function(context, fn, nargs, sendp);
    update_context();
    NEXT_INSN_NOINCPC();
  } else if (is_builtin(fn)) {
    // builtin function
    call_builtin(context, fn, nargs, sendp, newp);
    update_context();  // GC
    NEXT_INSN_INCPC();
#ifdef USE_FFI
    if (isErr(context)) {
      LOG_EXIT("CALL/SEND: exception by builtin");
    }
#endif
  }
#ifdef USE_FFI
  else if (isForeign(fn)) {
    call_foreign(context, fn, nargs, false, false);
    if (!isErr(context)) {
      NEXT_INSTRUCTION_INCPC();
    } else {
      int catchPlace = getCatchFp(context);
      int tempFp;
      while(cfp > border && cfp > catchPlace){
        tempFp = regBase[-FP_POS];
        setFp(context, tempFp);
        setLp(context, (FunctionFrame*)regBase[-LP_POS]);
        setPc(context, (int)regBase[-PC_POS]);
        setCf(context, (FunctionTableCell*)regBase[-CF_POS]);
        setSp(context, cfp);
        updateContext();
      }
       if(catchPlace < border){
        return -1; }
       setPc(context, getCatchPc(context));
      updateContext();
      catchStackPop(context);
      NEXT_INSTRUCTION_NOINCPC();
    }
  }
#endif
  else {
    print_value_simple(context, fn);
    printf(" is called in CALL/SEND/NEWSEND instruction\n");
    if (newp)
      LOG_EXIT("Not a constructor\n");
    else
      LOG_EXIT("CALL/SEND\n");
  }
}
