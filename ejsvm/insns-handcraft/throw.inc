/*
   throw
 */
{
  save_context();
  set_a(context, v0);
  exhandler_throw(context);
  /* exhandler_throw never returns */
}
