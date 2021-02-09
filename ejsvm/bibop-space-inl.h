#ifndef BIBOP_SPACE_INL_H
#define BIBOP_SPACE_INL_H

static inline int space_check_gc_request()
{
  return space.num_free_pages < space.num_threshold_pages;
}

static inline int in_js_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return space.addr <= addr && addr < space.end;
}

#ifndef GC_DEBUG
static inline page_header_t *payload_to_page_header(uintptr_t ptr)
{
  return ((page_header_t *) (ptr & ~(BYTES_IN_PAGE - 1)));
}
#endif /* GC_DEBUG */

#ifdef BIBOP_MOBJ
static inline
cell_status *page_mo_cell_status(so_page_header *ph, uintptr_t ptr)
{
  return (cell_status *) (ptr + ((ph->size - 1) << LOG_BYTES_IN_GRANULE));
}
#endif /* BIBOP_MOBJ */

static inline cell_type_t space_get_cell_type(uintptr_t ptr)
{
  page_header_t *ph = payload_to_page_header(ptr);
  if (ph->u.x.page_type == PAGE_TYPE_SOBJ)
    return ph->u.so.type;
#ifdef BIBOP_MOBJ
  else if (ph->u.x.page_type == PAGE_TYPE_MOBJ)
    return page_mo_cell_status(&ph->u.so, ptr)->type;
#endif /* BIBOP_MOBJ */
  else
    return ph->u.lo.type;
}

#endif /* BIBOP_SPACE_INL_H */
