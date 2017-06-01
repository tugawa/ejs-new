/*
   fixnum dst imm
     dst : destination register
     imm : immediate value of the fixnum
   $dst = imm
 */

{
  regbase[r0] = cint_to_fixnum((cint)i1);
}
