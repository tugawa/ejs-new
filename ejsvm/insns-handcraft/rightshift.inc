/*
   rightshift dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 >> $r2
 */
{
  Register dst = r0;
  Tag tag;
  int32_t x1;
  cint x2;

  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
    break;
  case TP_FIXFLO:
    x1 = (int32_t)fixnum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
    break;
  case TP_FLOFIX:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = fixnum_to_cint(v2);
    regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
    break;
  case TP_FLOFLO:
    x1 = (int32_t)flonum_to_cint(v1);
    x2 = flonum_to_cint(v2);
    regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
    break;
  default:
    regbase[dst] = slow_rightshift(context, v1, v2);
    break;
  }
}
