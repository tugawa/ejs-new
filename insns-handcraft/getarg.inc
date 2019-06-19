/*
   getarg dst link index
   $dst = value of the index-th argument in the link-th function frame
 */
{
  Register dst = r0;
  int link = i1;
  Subscript index = s2;
  FunctionFrame *fr;
  JSValue arguments;
  int i;

  fr = get_lp(context);
  for (i = 0; i < link; i++) fr = fframe_prev(fr);
  //regbase[dst] = array_body_index(fframe_arguments(fr), index);
  arguments = fframe_arguments(fr);
  // TODO: optimize
  regbase[dst] = get_array_prop(context, arguments, int_to_fixnum(index));
}
