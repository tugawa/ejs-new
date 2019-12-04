/*
   getprop dst obj idx
     $dst = $obj[$idx]
 */
{
  Register dst = r0;
  JSValue obj = v1;
  JSValue idx = v2;

  if (is_array(obj)) {
    if (is_fixnum(idx))
      regbase[dst] = get_array_element(context, obj, fixnum_to_cint(idx));
    else
      regbase[dst] = get_array_prop(context, obj, idx);
  } else if (is_object(obj)) {
#ifdef INLINE_CACHE
    if (is_string(idx)) {
      InlineCache *ic = &insns->inl_cache;
      assert(ic->shape == NULL || ic->shape->n_extension_slots == 0);
      if (ic->shape == object_get_shape(obj) && ic->prop_name == idx)
	regbase[dst] = jsv_to_jsobject(obj)->eprop[ic->index];
      else
	regbase[dst] = get_object_prop(context, obj, idx, ic);
    } else
      regbase[dst] = get_object_prop(context, obj, idx, NULL);
#else /* INLINE_CACHE */
    regbase[dst] = get_object_prop(context, obj, idx);
#endif /* INLINE_CACHE */
  } else {
    if (obj == JS_UNDEFINED)
      LOG_EXIT("attempt to get property from UNDEFINED\n");
    obj = to_object(context, obj);
    if (!is_object(obj))
      regbase[dst] = JS_UNDEFINED;
    else
#ifdef INLINE_CACHE
      regbase[dst] = get_object_prop(context, obj, idx, NULL);
#else /* INLINE_CACHE */
      regbase[dst] = get_object_prop(context, obj, idx);
#endif /* INLINE_CACHE */
  }
}
