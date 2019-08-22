/*
   setfl newfl
     newfl : number of elements between fp and sp (after growing sp)
   sp = fp + newfl - 1
 */
ENTER_INSN(__LINE__);
{
  int newfl = i0;
  int oldfl;

  oldfl = get_sp(context) - fp + 1;
  // printf("fp = %d, newfl = %d, fp + newfl = %d\n", fp, newfl, fp + newfl);
  if (fp + newfl > STACK_LIMIT)
    LOG_EXIT("register stack overflow\n");
  set_sp(context, fp + newfl - 1);
  while (++oldfl <= newfl)
    regbase[oldfl] = JS_UNDEFINED;
}
