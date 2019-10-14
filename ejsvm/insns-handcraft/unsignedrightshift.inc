/*
   unsingedrightshift dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 >>> $r2
 */
{
  Register dst = r0;
  uint32_t x1;
  cint x2;

  switch (TAG_PAIR_VARS(v1, v2)) {
  case TP_FIXFIX:
    x1 = (uint32_t)fixnum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    regbase[dst] = small_cint_to_fixnum((cint)(x1 >> x2));
    break;
  case TP_FIXFLO:
    x1 = (uint32_t)fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    regbase[dst] = small_cint_to_fixnum((cint)(x1 >> x2));
    break;
  case TP_FLOFIX:
    x1 = (uint32_t)flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    regbase[dst] = uint32_to_number(context, x1 >> x2);
    break;
  case TP_FLOFLO:
    x1 = (uint32_t)flonum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    regbase[dst] = uint32_to_number(context, x1 >> x2);
    break;
  default:
    regbase[dst] = slow_unsignedrightshift(context, v1, v2);
    break;
  }
}

