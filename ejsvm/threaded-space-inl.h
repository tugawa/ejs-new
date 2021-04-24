#ifndef FREELIST_SPACE_INL_H
#define FREELIST_SPACE_INL_H

static inline header_t compose_header(size_t granules, cell_type_t type)
{
  header_t hdr;
  hdr.identifier = 1;
  hdr.type = type;
  hdr.markbit = 0;
#if HEADER_MAGIC_BITS > 0
  hdr.magic = HEADER_MAGIC;
#endif /* HEADER_MAGIC_BITS */
#if HEADER_GEN_BITS > 0
#ifdef GC_DEBUG
  hdr.gen = generation;
#else /* GC_DEBUG */
  hdr.gen = 0;
#endif /* GC_DEBUG */
#endif /* HEADER_GEN_BITS */
  hdr.size  = granules;
  return hdr;
}

static inline header_t
compose_hidden_class_header(size_t granules, cell_type_t type)
{
  header_t hdr;
  hdr.identifier = 1;
  hdr.type = type;
  hdr.markbit = 0;
#if HEADER_MAGIC_BITS > 0
  hdr.magic = HEADER_MAGIC;
#endif /* HEADER_MAGIC_BITS */
#if HEADER_GEN_BITS > 0
#ifdef GC_DEBUG
  hdr.gen = generation;
#else /* GC_DEBUG */
  hdr.gen = 0;
#endif /* GC_DEBUG */
#endif /* HEADER_GEN_BITS */
#ifdef GC_THREADED_BOUNDARY_TAG
  hdr.hc.size_hi = 0;
#endif /* GC_THREADED_BOUNDARY_TAG */
  hdr.hc.size_lo = granules;
  return hdr;
}

#ifdef GC_THREADED_BOUNDARY_TAG
static inline void write_boundary_tag(uintptr_t alloc_end, size_t granules)
{
  header_t *hdrp = (header_t *) alloc_end;
  assert(granules <= BOUNDARY_TAG_MAX_SIZE);
  assert(hdrp->identifier == 1);
  hdrp->hc.size_hi = granules;
}
static inline size_t read_boundary_tag(uintptr_t alloc_end)
{
  header_t *hdrp = (header_t *) alloc_end;
  return hdrp->hc.size_hi;
}
#else /* GC_THREADED_BOUNDARY_TAG */
static inline void write_boundary_tag(uintptr_t alloc_end, size_t granules)
{
  granule_t *tagp = (granule_t *) alloc_end;
  *tagp = granules;
}
static inline size_t read_boundary_tag(uintptr_t alloc_end)
{
  granule_t *tagp = (granule_t *) alloc_end;
  return *tagp;
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
  return (js_space.free_bytes < js_space.threshold_bytes);
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
  hdrp->markbit = 1;
}

static inline void unmark_cell_header(header_t *hdrp)
{
  hdrp->markbit = 0;
}

static inline int is_marked_cell_header(header_t *hdrp)
{
  assert(hdrp->identifier == 1);
  return hdrp->markbit;
}

static inline void check_header(header_t *hdrp)
{
#ifdef GC_DEBUG
  {
    header_t *shadow = get_shadow(hdrp);
    assert(hdrp->identifier == 1);
#if HEADER_MAGIC_BITS > 0
    assert(hdrp->magic == HEADER_MAGIC);
#endif /* HEADER_MAGIC_BITS */
    assert(hdrp->type == shadow->type);
#ifdef GC_THREADED_BOUNDARY_TAG
    if (in_hc_space(hdrp))
      assert(hdrp->hc.size_lo == shadow->hc.size_lo);
    else
      assert(hdrp->size == shadow->size);
#else /* GC_THREADED_BOUNDARY_TAG */
    assert(hdrp->size == shadow->size);
#endif /* GC_THREADED_BOUNDARY_TAG */
#if HEADER_GEN_BITS > 0
    assert(hdrp->gen == shadow->gen);
#endif /* HEADER_GEN_BITS */
  }
#endif /* GC_DEBUG */
}


#endif /* FREELIST_SPACE_INL_H */
