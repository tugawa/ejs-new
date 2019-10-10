/*
   leftshift dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 << $r2
 */
{
  Register dst = r0;
  int32_t x1;
  cint x2;

  switch (TAG_PAIR_VARS(v1, v2)) {
  case TP_FIXFIX:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = fixnum_to_cint(v2) & 31;
    regbase[dst] = cint_to_number(context, (cint)(x1 << x2));
    break;
  case TP_FIXFLO:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2) & 31;
    regbase[dst] = cint_to_number(context, (cint)(x1 << x2));
    break;
  case TP_FLOFIX:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2) & 31;
    regbase[dst] = cint_to_number(context, (cint)(x1 << x2));
    break;
  case TP_FLOFLO:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = flonum_to_cint(v2) & 31;
    regbase[dst] = cint_to_number(context, (cint)(x1 << x2));
    break;
  default:
    regbase[dst] = slow_leftshift(context, v1, v2);
    break;
  }
}
