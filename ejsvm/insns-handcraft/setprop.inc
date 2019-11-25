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
  else if (is_object(obj)) {
#ifdef INLINE_CACHE
    if (is_string(prop)) {
      InlineCache *ic = &insns->inl_cache;
      assert(ic->shape == NULL || ic->shape->n_extension_slots == 0);
      if (ic->shape == object_get_shape(obj) && ic->prop_name == prop)
	jsv_to_jsobject(obj)->eprop[ic->index] = val;
      else
	set_prop_with_ic(context, obj, prop, val, ATTR_NONE, ic);
    } else
      set_object_prop(context, obj, prop, val);
#else /* INLINE_CACHE */
    set_object_prop(context, obj, prop, val);
#endif /* INLINE_CACHE */
  } else
    LOG_EXIT("setprop: first operand is not an object\n");
}

