/*
   getglobal dst reg
     dst : destination register
     reg : register that has a pointer to a string object
   $dst = property value for the string in the global object
 */
{
  JSValue str = v1;
  JSValue ret;

  if (get_prop(context->global, str, &ret) == FAIL)
    LOG_EXIT("GETGLOBAL: %s not found\n", string_to_cstr(str));
  /*
  printf("getglobal: dst = %d, str = %s, ret = ", dst, string_to_cstr(str));
  print_value_verbose(context, ret); putchar('\n');
  */
  regbase[r0] = ret;
}

