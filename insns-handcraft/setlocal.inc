/*
   setlocal link index src
 */
{
  int link = i0;
  Subscript index = s1;
  FunctionFrame *fr;
  int i;

  fr = get_lp(context);
  for (i = 0; i < link; i++) fr = fframe_prev(fr);
  fframe_locals_idx(fr, index) = v2;
}

