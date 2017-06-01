/*
   getprop dst obj idx
     $dst = $obj[$idx]
 */
{
  Register dst = r0;
  JSValue obj = v1;
  JSValue idx = v2;

  if (is_array(obj)) {
    regbase[dst] = get_array_prop(context, obj, idx);
  } else if (is_object(obj)) {
    regbase[dst] = get_object_prop(context, obj, idx);
  } else {
    if (obj == JS_UNDEFINED) printf("GETPROP: !!!!!\n");
    obj = to_object(context, obj);
    if (!is_object(obj))
      regbase[dst] = JS_UNDEFINED;
    else
      regbase[dst] = get_object_prop(context, obj, idx);
  }
  /*
  printf("getprop: idx = "); print_value_simple(context, idx);
  printf(" ; obj = "); print_value_simple(context, obj);
  printf(" ; result = "); print_value_simple(context, regbase[dst]);
  printf("\n");
  */
}
