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
#ifdef STROBJ_HAS_HASH
      char *s1, *s2;
      uint32_t len1, len2;

      s1 = string_value(v1); len1 = string_length(v1);
      s2 = string_value(v2); len2 = string_length(v2);
      regbase[dst] = str_intern2(context, s1, len1, s2, len2,
                                 calc_hash2(s1, s2), INTERN_HARD);
#else
      regbase[dst] = cstr_to_string2(context,
		       string_to_cstr(v1), string_to_cStr(v2));
#endif
    }
    break;
  default:
    {
      regbase[dst] = slow_add(context, v1, v2);
    }
    break;
  }
}
