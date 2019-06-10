/*
   number dst disp
   string dst disp
   regexp dst disp
     dst : destination register
     disp : displacement of the constant position from the pc
   $dst = ((JSValur *)insns)[disp]
 */
{
    // regbase[r0] = insns[d1].code;
    regbase[r0] = get_literal(insns, d1);
}

