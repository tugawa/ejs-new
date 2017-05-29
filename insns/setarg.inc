/*
   setarg link index src
 */
{
  int link = i0;
  Subscript index = s1;
  FunctionFrame *fr;
  JSValue arguments;
  int i;

  fr = get_lp(context);
  for (i = 0; i < link; i++) fr = fframe_prev(fr);
  // assert(index < array_size(fframe_arguments(fr)));
  // array_body_index(fframe_arguments(fr), index) = v2;
  // TODO: optimize
  arguments = fframe_arguments(fr);
  set_array_prop(context, arguments, int_to_fixnum(index), v2);
}
