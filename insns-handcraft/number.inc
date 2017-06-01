/*
   number dst disp
   string dst disp
   regexp dst disp
     dst : destination register
     disp : displacement of the constant position from the pc
   $dst = insns[pc + disp]
 */
{
    regbase[r0] = insns[d1].code;
}

