/*
   jumpfalse src disp
   if (!$src) pc = pc + $disp
 */
{
  JSValue v = regbase[r];
  if (v == JS_FALSE || to_boolean(v) == JS_FALSE) {
    Displacement disp = get_second_operand_disp(insn);
    set_pc_relative(disp);
    NEXT_INSN_NOINCPC();
  }
}
