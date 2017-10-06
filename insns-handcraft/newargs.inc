/*
   newargs
 */
{
  int na;
  JSValue args;
  int i;

  na = get_ac(context);

  save_context(); // GC
  args = new_normal_array_with_size(context, na);
  update_context(); // GC
  /*
     Note that the i-th arg is regbase[i + 2].
     (regbase[1] is the receiver)
   */
  for (i = 0; i < na; i++)
    array_body_index(args, i) = regbase[i + 2];
  set_a(context, args);
}
