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
    get_next_propname_simple_iterator(itr, &res);
  regbase[dst] = res;
  //printf("nextpropnameidx: itr = %016lx ", itr); simple_print(itr); printf("\n");
}
