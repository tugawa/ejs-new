/*
   geta dst
     dst : destination register
   $dst = a
 */
{
  regbase[r0] = get_a(context);
}
