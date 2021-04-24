static inline int GC_PM_EQ(PropertyMap *p, PropertyMap *q) {
  return p == q;
}

#ifdef GC_DEBUG
extern void space_print_memory_status(void);
#endif /* GC_DEBUG */
