/*
   newframe
 */
{
  int frame_len = i0;
  FunctionFrame *fr;

  /*
     allocates a new function frame into which arguments array is stored
   */
  // However, is it correct?
  // fr = new_frame(get_cf(context), fframe_prev(get_lp(context))); ???
  fr = new_frame(context, get_cf(context), get_lp(context), frame_len);
  set_lp(context, fr);
}
