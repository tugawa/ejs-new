/*
   makeiterator obj dst
     dst : destination register
   $dst = iterator object for iterating $obj
 */ 
{
  JSValue obj = regbase[r0];
  Register dst = r1;

  if (!is_object(obj))
    LOG_EXIT("makeiterator: not an object\n");
  regbase[dst] = new_normal_iterator(context, obj);
  // printf("makeiterator: iter = %016lx ", regbase[dst]); simple_print(regbase[dst]); printf("\n");
}
