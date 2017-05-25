/*
   setglobal reg src
     reg : register that has a pointer to a string object
     src : property value to be set
   property value for the string in the global object = $src
 */
{
  JSValue str = regbase[r0];
  JSValue src = regbase[r1];
  if (set_prop_none(context, context->global, str, src) == FAIL)
    LOG_EXIT("SETGLOBAL: setting a value of %s failed\n", string_to_cstr(str));
}

