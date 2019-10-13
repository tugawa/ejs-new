/*
   setglobal reg src
     reg : register that has a pointer to a string object
     src : property value to be set
   property value for the string in the global object = $src
 */
{
  JSValue str = v0;
  JSValue src = v1;
  if (!is_string(str))
    LOG_EXIT("SETGLOBAL %"PRIJSValue" : name is not a string\n", str);
  else
    set_prop(context, context->global, str, src, ATTR_NONE);
}

