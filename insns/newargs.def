/*
   newargs
 */
{
  int na;
  FunctionFrame *fr;
  JSValue args;
  int i;

  na = get_ac(context);

  /*
     allocates a new function frame into which arguments array is stored
   */
  // However, is it correct?
  // fr = new_frame(get_cf(context), fframe_prev(get_lp(context))); ???
  fr = new_frame(get_cf(context), get_lp(context));
  set_lp(context, fr);
  save_context(); // GC
  args = new_normal_array_with_size(context, na);
  update_context(); // GC
  /*
     Note that the i-th arg is regbase[i + 2].
     (regbase[1] is the receiver)
   */
  for (i = 0; i < na; i++)
    array_body_index(args, i) = regbase[i + 2];
  fframe_arguments(fr) = args;
}
