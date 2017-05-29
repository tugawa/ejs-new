/*
   eq dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 === $r2
 */
{
  Register dst = r0;
  JSValue ret;
  double x1, x2;

  if (v1 == v2)
    ret = false_true(is_nan(v1));
  else if (is_flonum(v1) && is_flonum(v2)) {
    x1 = flonum_to_double(v1);
    x2 = flonum_to_double(v2);
    ret = true_false(x1 == x2);
  } else
    ret = JS_FALSE;
  regbase[dst] = ret;
}

