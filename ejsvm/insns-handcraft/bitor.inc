/*
   bitor dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 | $r2
 */
{
  Register dst = r0;
  int32_t x1, x2;

  switch (TAG_PAIR_VARS(v1, v2)) {
  case TP_FIXFIX:
    regbase[dst] = (JSValue) (((uintjsv_t) v1) | ((uintjsv_t) v2));
    break;
  case TP_FIXFLO:
    x1 = (int32_t) fixnum_to_cint(v1);
    x2 = (int32_t) flonum_to_cint(v2);
    regbase[dst] = int32_to_number(context, x1 | x2);
    break;
  case TP_FLOFIX:
    x1 = (int32_t) flonum_to_cint(v1);
    x2 = (int32_t) fixnum_to_cint(v2);
    regbase[dst] = int32_to_number(context, x1 | x2);
    break;
  case TP_FLOFLO:
    x1 = (int32_t) flonum_to_cint(v1);
    x2 = (int32_t) flonum_to_cint(v2);
   regbase[dst] = int32_to_number(context, x1 | x2);
    break;
  default:
    regbase[dst] = slow_bitor(context, v1, v2);
    break;
  }
}
