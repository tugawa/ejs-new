#ifndef GC_INL_H
#define GC_INL_H

static inline cell_type_t gc_obj_header_type(void *p)
{
  return space_get_cell_type((uintptr_t) p);
}

static inline void gc_push_checked(void *addr)
{
  extern JSValue *gc_root_stack[];
  extern int gc_root_stack_ptr;
  gc_root_stack[gc_root_stack_ptr++] = (JSValue *) addr;
}

static inline void gc_pop_checked(void *addr)
{
#ifndef NDEBUG
  extern JSValue *gc_root_stack[];
#endif /* NDEBUG */
  extern int gc_root_stack_ptr;
  assert(gc_root_stack[gc_root_stack_ptr - 1] == (JSValue *) addr);
  --gc_root_stack_ptr;
}

static inline int gc_save_root_stack()
{
  extern int gc_root_stack_ptr;
  return gc_root_stack_ptr;
}

static inline void gc_restore_root_stack(int sp)
{
  extern int gc_root_stack_ptr;
  gc_root_stack_ptr = sp;
}

#endif /* GC_INL_H */
