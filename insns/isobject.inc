/*
   isobject dst reg
     $dst = $reg is an Object or not
 */
{
  regbase[r0] = true_false(is_object(v1));
}

