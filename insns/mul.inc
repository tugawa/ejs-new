/*
   mul dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 * $r2
   If necessary, this instruction does type conversions.
 */
{
  Register dst = r0;
  Tag tag;
  double x1, x2, d;

  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    {
      cint n1, n2, p;
      n1 = fixnum_to_cint(v1);
      n2 = fixnum_to_cint(v2);
      if (half_fixnum_range(n1) && half_fixnum_range(n2)) {
        p = n1 * n2;
        regbase[dst] = cint_to_fixnum(p);
      } else {
        x1 = (double)n1;
        x2 = (double)n2;
        goto MUL_FLOFLO;
      }
    }
    break;
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      goto MUL_FLOFLO;
    }
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto MUL_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
  MUL_FLOFLO:
      d = x1 * x2;
      regbase[dst] = double_to_number(d);
    }
    break;
  default:
    {
      regbase[dst] = slow_mul(context, v1, v2);
    }
    break;
  }
}
