/*
   add dst r1 r2
    dst : destination register
    r1, r2 : source registers
   $dst = $r1 + $r2
   If necessary, this instruction does type conversions.
 */
{
  Register dst = r0;
  Tag tag;
  double x1, x2, d;

  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    {
      cint s = fixnum_to_cint(v1) + fixnum_to_cint(v2);
      regbase[dst] = cint_to_number(s);
    }
    break;
  case TP_FIXFLO:
    {
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      goto ADD_FLOFLO;
    }
  case TP_FLOFIX:
    {
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      goto ADD_FLOFLO;
    }
  case TP_FLOFLO:
    {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
  ADD_FLOFLO:
      d = x1 + x2;
      regbase[dst] = double_to_number(d);
    }
    break;
  case TP_STRSTR:
    {
      regbase[dst] = ejs_string_concat(context, v1, v2);
    }
    break;
  default:
    {
      regbase[dst] = slow_add(context, v1, v2);
    }
    break;
  }
}
