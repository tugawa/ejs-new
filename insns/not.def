/*
   not dst reg
     dst : destination register
     reg  : source register
   $dst = ! $reg
 */
{
  JSValue v = regbase[r1];
  regbase[r0] =
    true_false(v == JS_FALSE || v == FIXNUM_ZERO ||
               v == gconsts.g_flonum_nan || v == gconsts.g_string_empty);
}
