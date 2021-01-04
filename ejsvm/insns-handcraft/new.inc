/*
   new dst con
     $dst = new object created by $con
 */
{
  JSValue con = v1;

  if (is_function(con)) {
    save_context();
#ifdef ALLOC_SITE_CACHE
    regbase[r0] = create_simple_object_with_constructor(context, con,
                                                        &insns->alloc_site);
#else /* ALLOC_SITE_CACHE */
    regbase[r0] = create_simple_object_with_constructor(context, con);
#endif /* ALLOC_SITE_CACHE */
    update_context();
  } else
    regbase[r0] = JS_UNDEFINED;
}
