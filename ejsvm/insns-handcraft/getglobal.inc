/*
   getglobal dst reg
     dst : destination register
     reg : register that has a pointer to a string object
   $dst = property value for the string in the global object
 */
{
  JSValue str = v1;
  JSValue ret;
  if (!is_string(str))
    LOG_EXIT("GETGLOBAL: name is no a string\n");
  else {
    ret = get_prop(context->global, str);
    if (ret == JS_EMPTY)
      LOG_EXIT("GETGLOBAL: %s not found\n", string_to_cstr(str));
    regbase[r0] = ret;
  }
}

