/*
   instanceof dst r1 r2
     $dst = $r1 instanceof $r2
 */
ENTER_INSN(__LINE__);
{
  Register dst = r0;
  JSValue p, ret;

  ret = JS_FALSE;
  if (is_object(v1) && is_object(v2) &&
      get_prop(v2, gconsts.g_string_prototype, &p) == SUCCESS) {
    while (get___proto__(v1, &v1) == SUCCESS)
      if (v1 == p) {
        ret = JS_TRUE;
        break;
      }
  }
  regbase[dst] = ret;
}
