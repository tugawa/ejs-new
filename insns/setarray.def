/*
   setarray dst subscript src
     dst : register that holds an array
     subscript : array's subscript
     src : register that has the assigned value
   $dst[$reg] = $src
 */
{
  JSValue a = regbase[dst];
  JSValue v = regbase[src];
  
  // It is unnecessary to typecheck the values.
  array_body_index(a, s) = v;
}
