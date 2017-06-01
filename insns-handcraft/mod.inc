/*
   mod dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 % $r2
   If necessary, this instruction does type conversions.
 */
{
  Register dst = r0;
  Tag tag;
  double x1, x2, d;

  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    {
      if (v2 == FIXNUM_ZERO)
        regbase[dst] = gconsts.g_flonum_nan;
      else {
        cint s = fixnum_to_cint(v1) % fixnum_to_cint(v2);
        regbase[dst] = cint_to_fixnum(s);
      }
    }
    break;
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
    }
    goto MOD_FLOFLO;
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto MOD_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
  MOD_FLOFLO:
      if (isinf(x1) || x2 == 0.0f)
        regbase[dst] = gconsts.g_flonum_nan;
      else {
        d = x1 / x2;
        d = d >= 0 ? floor(d) : ceil(d);
        d = x1 - (d * x2);
        regbase[dst] = double_to_number(d);
      }
    }
    break;
  default:
    {
      regbase[dst] = slow_mod(context, v1, v2);
    }
    break;
  }
}
