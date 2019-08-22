/*
   isundef dst reg
     $dst = $reg == undefined
 */
{
  regbase[r0] = true_false(is_undefined(v1));
}
