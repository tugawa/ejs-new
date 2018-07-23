/*
   makesimpleiterator obj dst
     dst : destination register
   $dst = iterator object for iterating $obj
 */ 
{
  JSValue obj = v0;
  Register dst = r1;
  
  if (!is_object(obj))
    LOG_EXIT("makesimpleiterator: not an object\n");
  regbase[dst] = new_normal_simple_iterator(context, obj);
   //printf("makesimpleiterator: iter = %016lx ", regbase[dst]); simple_print(regbase[dst]); printf("\n");
}
