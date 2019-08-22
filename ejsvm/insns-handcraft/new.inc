/*
   new dst con
     $dst = new object created by $con
 */
{
  JSValue con = v1;
  JSValue o, p;

  if (is_function(con)) {
    save_context(); // GC
    o = new_normal_object(context);
    update_context(); // GC
    // printf("NEW: is_function, o = %lx\n", o);
    get_prop(con, gconsts.g_string_prototype, &p);
    if (!is_object(p)) p = gconsts.g_object_proto;
    set___proto___all(context, o, p);
  } else
    o = JS_UNDEFINED;
  regbase[r0] = o;
}
