/*
   jumptrue src disp
   if ($src) pc = pc + $disp
 */
{
  if (v0 == JS_TRUE || to_boolean(v0) == JS_TRUE) {
    set_pc_relative(d1);
    NEXT_INSN_NOINCPC();
  }
}
