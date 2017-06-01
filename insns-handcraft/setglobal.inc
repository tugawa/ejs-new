/*
   setglobal reg src
     reg : register that has a pointer to a string object
     src : property value to be set
   property value for the string in the global object = $src
 */
{
  JSValue str = v0;
  JSValue src = v1;
  if (set_prop_none(context, context->global, str, src) == FAIL)
    LOG_EXIT("SETGLOBAL: setting a value of %s failed\n", string_to_cstr(str));
}

