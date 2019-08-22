/*
   div dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 / $r2
   If necessary, this instruction does type conversions.
 */
{
  Register dst = r0;
  Tag tag;
  double x1, x2, d;

  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    {
      int n1, n2, s;
      n1 = fixnum_to_cint(v1);
      if (v2 == FIXNUM_ZERO) {
        if (n1 > 0) regbase[dst] = gconsts.g_flonum_infinity;
        else if (n1 == 0) regbase[dst] = gconsts.g_flonum_nan;
        else regbase[dst] = gconsts.g_flonum_negative_infinity;
      } else {
        n2 = fixnum_to_cint(v2);
        s = n1 / n2;
        regbase[dst] =
          (n1 == n2 * s)? cint_to_fixnum(s):
                          double_to_flonum((double)n1 / (double)n2);
      }
    }
    break;
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      goto DIV_FLOFLO;
    }
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto DIV_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
  DIV_FLOFLO:
      d = x1 / x2;
      if (isinf(d)) regbase[dst] = d > 0? gconsts.g_flonum_infinity:
                                          gconsts.g_flonum_negative_infinity;
      else if (isnan(d)) regbase[dst] = gconsts.g_flonum_nan;
      else regbase[dst] = double_to_number(d);
    }
    break;
  default:
    {
      regbase[dst] = slow_div(context, v1, v2);
    }
    break;
  }
}
