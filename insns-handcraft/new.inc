/*
   new dst con
     $dst = new object created by $con
 */
{
  JSValue con = v1;
  JSValue o, p;
  HiddenClass *hc;

  if (is_function(con)) {
    // printf("NEW: is_function, o = %lx\n", o);
    get_prop(con, gconsts.g_string_prototype, &p);
    if (is_object(p) && p != gconsts.g_object_proto) {
      if (get_prop(p, gconsts.g_string___hidden_class__,
                  (JSValue *)&hc) == FAIL) {
        save_context();
        hc = new_hidden_class(context, gobjects.g_hidden_class_0);
        hidden_n_props(hc) = 0;   /* TODO: do this in new_hidden_class() */
        set_prop_with_attribute(context, p, gconsts.g_string___hidden_class__,
                                (JSValue) hc, ATTR_ALL);
        update_context();
        hidden_proto(hc) = p;
      }
    } else
      hc = gobjects.g_hidden_class_0;
    save_context(); // GC
    o = new_normal_object_with_class(context, hc);
    update_context(); // GC
  } else
    o = JS_UNDEFINED;
  regbase[r0] = o;
}
