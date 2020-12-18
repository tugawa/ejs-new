#ifndef FREELIST_SPACE_INL_H
#define FREELIST_SPACE_INL_H

static inline header_t compose_header(size_t granules, cell_type_t type)
{
  header_t hdr;
  hdr.identifier = 1;
  hdr.type = type;
  hdr.markbit = 0;
  hdr.magic = HEADER_MAGIC;
#ifdef GC_DEBUG
  hdr.gen = generation;
#else /* GC_DEBUG */
  hdr.gen = 0;
#endif /* GC_DEBUG */
  hdr.size  = granules;
  return hdr;
}

#ifdef GC_THREADED_BOUNDARY_TAG
static inline footer_t compose_footer(size_t granules, size_t extra,
                                      cell_type_t type)
{
  footer_t footer;
  footer.as_header.identifier = 1;
  footer.as_header.type = type;
  footer.as_header.markbit = 0;
  footer.as_header.magic = HEADER_MAGIC;
#ifdef GC_DEBUG
  footer.as_header.gen = generation;
#else /* GC_DEBUG */
  footer.as_header.gen = 0;
#endif /* GC_DEBUG */
  footer.size_hi = 0;
  footer.size_lo = granules;
  return footer;
}
#endif /* GC_THREADED_BOUNDARY_TAG */

static inline void *header_to_payload(header_t *hdrp)
{
  return (void *) (hdrp + 1);
}

static inline header_t *payload_to_header(void *ptr)
{
  return ((header_t *) ptr) - 1;
}

#ifdef GC_THREADED_BOUNDARY_TAG
static inline footer_t* header_to_footer(header_t *hdrp)
{
  assert(hdrp->identifier == 1);
  unsigned int size = hdrp->size;
  return (footer_t *) ((uintptr_t) hdrp + (size << LOG_BYTES_IN_GRANULE));
}
#else /* GC_THREADED_BOUNDARY_TAG */
static inline header_t* header_to_footer(header_t *hdrp)
{
  assert(hdrp->identifier == 1);
  unsigned int size = hdrp->size;
  return (header_t *) ((uintptr_t) hdrp + (size << LOG_BYTES_IN_GRANULE));
}
#endif /* GC_THREADED_BOUNDARY_TAG */

#ifdef GC_THREADED_BOUNDARY_TAG
static inline header_t* footer_to_header(footer_t *footer)
{
  assert(footer->as_header.identifier == 1);
  unsigned int size = footer->size_hi;
  return (header_t *) ((uintptr_t) footer - (size << LOG_BYTES_IN_GRANULE));
}
#else /* GC_THREADED_BOUNDARY_TAG */
static inline header_t* footer_to_header(header_t *footer)
{
  assert(footer->identifier == 1);
  unsigned int size = footer->size;
  return (header_t *) ((uintptr_t) footer - (size << LOG_BYTES_IN_GRANULE));
}
#endif /* GC_THREADED_BOUNDARY_TAG */

#ifdef GC_THREADED_BOUNDARY_TAG
static inline footer_t* end_to_footer(uintptr_t end)
{
  return (footer_t *) (end - (HEADER_GRANULES << LOG_BYTES_IN_GRANULE));
}
#else /* GC_THREADED_BOUNDARY_TAG */
static inline header_t* end_to_footer(uintptr_t end)
{
  return (header_t *) (end - (HEADER_GRANULES << LOG_BYTES_IN_GRANULE));
}
#endif /* GC_THREADED_BOUNDARY_TAG */

#ifdef GC_THREADED_BOUNDARY_TAG
static inline uintptr_t footer_to_end(footer_t* footer)
{
  assert(footer->as_header.identifier == 1);
  return ((uintptr_t) footer) + (HEADER_GRANULES << LOG_BYTES_IN_GRANULE);
}
#else /* GC_THREADED_BOUNDARY_TAG */
#if 0
/* no longer used */
static inline uintptr_t footer_to_end(header_t* footer)
{
  assert(footer->identifier == 1);
  return ((uintptr_t) footer) + (HEADER_GRANULES << LOG_BYTES_IN_GRANULE);
}
#endif /* 0 */
#endif /* GC_THREADED_BOUNDARY_TAG */

static inline int in_js_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return (js_space.head <= addr && addr < js_space.tail);
}

static inline int in_obj_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return (js_space.head <= addr && addr <= js_space.begin);
}

static inline int in_hc_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return (js_space.end <= addr && addr < js_space.tail);
}

static inline cell_type_t space_get_cell_type(uintptr_t ptr)
{
  return payload_to_header((void *) ptr)->type;
}

static inline int space_check_gc_request()
{
  if (js_space.free_bytes <
      js_space.bytes - (js_space.bytes >> GC_THREASHOLD_SHIFT))
    return 1;
  return 0;
}

static inline void mark_cell(void *p)
{
  header_t *hdrp = payload_to_header(p);
  mark_cell_header(hdrp);
}

static inline void unmark_cell (void *p) __attribute__((unused));
static inline void unmark_cell (void *p)
{
  header_t *hdrp = payload_to_header(p);
  unmark_cell_header(hdrp);
}

static inline int is_marked_cell(void *p)
{
  header_t *hdrp = payload_to_header(p);
  return is_marked_cell_header(hdrp);
}

static inline int test_and_mark_cell(void *p)
{
  header_t *hdrp;
  assert(in_js_space(p));
  hdrp = payload_to_header(p);
  if (is_marked_cell_header(hdrp))
    return 1;
  mark_cell_header(hdrp);
  return 0;
}

static inline void mark_cell_header(header_t *hdrp)
{
#ifdef GC_DEBUG
  {
    header_t *shadow = get_shadow(hdrp);
    assert(hdrp->identifier == 1);
    assert(hdrp->magic == HEADER_MAGIC);
    assert(hdrp->type == shadow->type);
#ifdef GC_THREADED_BOUNDARY_TAG
    if (in_hc_space(hdrp)) {
      footer_t *footer = (footer_t *) hdrp;
      footer_t *shadow_footer = (footer_t *) shadow;
      assert(footer->size_lo == shadow_footer->size_lo);
    }
    else {
      assert(hdrp->size - hdrp->extra ==
            shadow->size - shadow->extra);
    }
#else /* GC_THREADED_BOUNDARY_TAG */
    assert(hdrp->size - hdrp->extra ==
           shadow->size - shadow->extra);
#endif /* GC_THREADED_BOUNDARY_TAG */
    assert(hdrp->gen == shadow->gen);
  }
#endif /* GC_DEBUG */
  hdrp->markbit = 1;
}

static inline void unmark_cell_header(header_t *hdrp)
{
#ifdef GC_DEBUG
  {
    header_t *shadow = get_shadow(hdrp);
    assert(hdrp->identifier == 1);
    assert(hdrp->magic == HEADER_MAGIC);
    assert(hdrp->type == shadow->type);
#ifdef GC_THREADED_BOUNDARY_TAG
    if (in_hc_space(hdrp)) {
      footer_t *footer = (footer_t *) hdrp;
      footer_t *shadow_footer = (footer_t *) shadow;
      assert(footer->size_lo == shadow_footer->size_lo);
    }
    else {
      assert(hdrp->size - hdrp->extra ==
            shadow->size - shadow->extra);
    }
#else /* GC_THREADED_BOUNDARY_TAG */
    assert(hdrp->size - hdrp->extra ==
           shadow->size - shadow->extra);
#endif /* GC_THREADED_BOUNDARY_TAG */
    assert(hdrp->gen == shadow->gen);
  }
#endif /* GC_DEBUG */
  hdrp->markbit = 0;
}

static inline int is_marked_cell_header(header_t *hdrp)
{
  assert(hdrp->identifier == 1);
  return hdrp->markbit;
}

#endif /* FREELIST_SPACE_INL_H */
