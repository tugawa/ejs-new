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
#ifdef INLINE_CACHE
    InlineCache *ic = &insns->inl_cache;
    assert(ic->shape == NULL || ic->shape->n_extension_slots == 0);
    if (ic->shape == object_get_shape(obj) && ic->prop_name == idx)
      regbase[dst] = remove_jsobject_tag(obj)->eprop[ic->index];
    else
      regbase[dst] = get_object_prop(context, obj, idx, ic);
#else /* INLINE_CACHE */
    regbase[dst] = get_object_prop(context, obj, idx);
#endif /* INLINE_CACHE */
  } else {
    if (obj == JS_UNDEFINED) printf("GETPROP: !!!!!\n");
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
