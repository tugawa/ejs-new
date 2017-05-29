/*
   setprop obj prop val
     obj : object into which (prop,val) pair is set
     prop : property name
     val : value
   $obj[$prop] = $val
 */
{
  JSValue obj = regbase[r0];
  JSValue prop = regbase[r1];
  JSValue val = regbase[r2];

  if (is_array(obj))
    set_array_prop(context, obj, prop, val);
  else if (is_object(obj))
    set_object_prop(context, obj, prop, val);
  else
    LOG_EXIT("setprop: first operand is not an object\n");
}

