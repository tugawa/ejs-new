/*
   jumpfalse src disp
   if (!$src) pc = pc + $disp
 */
{
  if (v0 == JS_FALSE || to_boolean(v0) == JS_FALSE) {
    set_pc_relative(d1);
    NEXT_INSN_NOINCPC();
  }
}
