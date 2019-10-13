/*
   bitor dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 | $r2
 */
{
  Register dst = r0;
  uint32_t x1, x2;

  switch (TAG_PAIR_VARS(v1, v2)) {
  case TP_FIXFIX:
    regbase[dst] = (JSValue) (uintjsv_t) (uint32_t) (((uintjsv_t) v1) | ((uintjsv_t) v2));
    break;
  case TP_FIXFLO:
    x1 = (uint32_t) fixnum_to_cint(v1);
    x2 = (uint32_t) flonum_to_cint(v2);
    regbase[dst] = cint_to_number(context, (cint) (x1 | x2));
    break;
  case TP_FLOFIX:
    x1 = (uint32_t) flonum_to_cint(v1);
    x2 = (uint32_t) fixnum_to_cint(v2);
    regbase[dst] = cint_to_number(context, (cint) (x1 | x2));
    break;
  case TP_FLOFLO:
    x1 = (uint32_t) flonum_to_cint(v1);
    x2 = (uint32_t) flonum_to_cint(v2);
   regbase[dst] = cint_to_number(context, (cint) (x1 | x2));
    break;
  default:
    regbase[dst] = slow_bitor(context, v1, v2);
    break;
  }
}
