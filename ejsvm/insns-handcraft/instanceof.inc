/*
   instanceof dst r1 r2
     $dst = $r1 instanceof $r2
 */
ENTER_INSN(__LINE__);
{
  Register dst = r0;
  JSValue p, ret;

  ret = JS_FALSE;
  if (is_jsobject(v1) && (is_function(v2) || is_builtin(v2)) &&
      (p = get_prop(v2, gconsts.g_string_prototype)) != JS_EMPTY) {
    while ((v1 = get_prop(v1, gconsts.g_string___proto__)) != JS_EMPTY)
      if (v1 == p) {
        ret = JS_TRUE;
        break;
      }
  }
  regbase[dst] = ret;
}
