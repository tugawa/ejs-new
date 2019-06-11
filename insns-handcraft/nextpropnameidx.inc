/*
   nextpropnameidx itr dst
     itr : iterator for enumerating properties in obj
     dst : destination
   $dst = the next property name of $obj in $itr
 */
{
  JSValue itr = v0;
  Register dst = r1;
  JSValue res = JS_UNDEFINED;
  iterator_get_next_propname(itr, &res);
  regbase[dst] = res;
}
