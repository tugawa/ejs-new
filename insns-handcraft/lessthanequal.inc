/*
   lessthanequal dst r1 r2
     dst : destination register
     r1, r2 : source registers
   $dst = $r1 <= $r2
 */
{
  Register dst = r0;
  Tag tag;
  double x1, x2;

  switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
  case TP_FIXFIX:
    regbase[dst] = true_false((int64_t)v1 <= (int64_t)v2);
    break;
  case TP_FIXFLO:
    x1 = fixnum_to_double(v1);
    x2 = flonum_to_double(v2);
    regbase[dst] = true_false(x1 <= x2);
    break;
  case TP_FLOFIX:
    x1 = flonum_to_double(v1);
    x2 = fixnum_to_double(v2);
    regbase[dst] = true_false(x1 <= x2);
    break;
  case TP_FLOFLO:
    x1 = flonum_to_double(v1);
    x2 = flonum_to_double(v2);
    regbase[dst] = true_false(x1 <= x2);
    break;
  case TP_STRSTR:
    regbase[dst] =
      true_false(strcmp(string_to_cstr(v1), string_to_cstr(v2)) <= 0);
    break;
  default:
    regbase[dst] = slow_lessthanequal(context, v1, v2);
    break;
  }
}
