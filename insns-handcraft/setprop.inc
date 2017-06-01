/*
   setprop obj prop val
     obj : object into which (prop,val) pair is set
     prop : property name
     val : value
   $obj[$prop] = $val
 */
{
  JSValue obj = v0;
  JSValue prop = v1;
  JSValue val = v2;

  if (is_array(obj))
    set_array_prop(context, obj, prop, val);
  else if (is_object(obj))
    set_object_prop(context, obj, prop, val);
  else
    LOG_EXIT("setprop: first operand is not an object\n");
}

