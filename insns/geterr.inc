/*
   geterr dst
     dst : destination register
   $dst = err
   I don't know in which situation this instruction is necessary.
 */
{
  regbase[r] = get_err(context);
}

