/*
   getglobalobj dst
   $dst = global object
 */
{
  // if (context->global == JS_UNDEFINED) printf("GETGLOBALOBJ: !!!!\n");
  regbase[r0] = context->global;
}
