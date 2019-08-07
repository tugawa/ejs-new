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
#ifdef HIDDEN_CLASS_PROTO
  if (is_object(p) && p != gconsts.g_object_proto) {
    HiddenClass *hc;
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
    obj_hidden_class(o) = hc;   /* TODO: make new_normal_object_with_class */
  }
#else /* HIDDEN_CLASS_PROTO */
    if (!is_object(p)) p = gconsts.g_object_proto;
    set___proto___all(context, o, p);
#endif /* HIDDEN_CLASS_PROTO */
  } else
    o = JS_UNDEFINED;
  regbase[r0] = o;
}
