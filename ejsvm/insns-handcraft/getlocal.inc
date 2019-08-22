/*
   getlocal dst link index
   $dst = value of the index-th local variable in the link-th function frame
 */
{
  Register dst = r0;
  int link = i1;
  Subscript index = s2;
  FunctionFrame *fr;
  int i;

  fr = get_lp(context);
  for (i = 0; i < link; i++) fr = fframe_prev(fr);
  regbase[dst] = fframe_locals_idx(fr, index);
}
