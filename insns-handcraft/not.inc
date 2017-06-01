/*
   not dst reg
     dst : destination register
     reg  : source register
   $dst = ! $reg
 */
{
  regbase[r0] =
    true_false(v1 == JS_FALSE || v1 == FIXNUM_ZERO ||
               v1 == gconsts.g_flonum_nan || v1 == gconsts.g_string_empty);
}
