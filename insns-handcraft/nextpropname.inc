/*
   nextpropname obj itr dst
     obj : object
     itr : iterator for enumerating properties in obj
     dst : destination
   $dst = the next property name of $obj in $itr
 */
{
  JSValue obj = v0;
  JSValue itr = v1;
  Register dst = r2;
  JSValue res = JS_UNDEFINED;
  JSValue val;
  // printf("nextpropname: itr = %016lx ", itr); simple_print(itr); printf("\n");
  while (1) {
    int r;
    r = get_next_propname(itr, &res);
    if (r != SUCCESS) break;
    if ((val = get_prop_prototype_chain(obj, res)) != JS_UNDEFINED) break;
  }
  regbase[dst] = res;
}
