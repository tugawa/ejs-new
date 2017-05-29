/*
   isundef dst reg
     $dst = $reg == undefined
 */
{
  JSValue v = regbase[r1];
  regbase[r0] = true_false(is_undefined(v));
}
