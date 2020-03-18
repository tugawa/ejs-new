/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN extern
#include "header.h"

#define ROUNDUP(s,u) (((s) + (u) - 1) & ~((u) - 1))

static int sizeclass_map[GRANULES_IN_PAGE];
struct space space;

#ifdef VERIFY_BIBOP
static void verify_free_page(free_page_header *ph);
#endif /* VERIFY_BIBOP */
void space_print_memory_status();

/* bitmap operation */
STATIC_INLINE void bmp_set(unsigned char* bmp, int index)
{
  bmp[index >> LOG_BITS_IN_BYTE] |= (1 << (index & (BITS_IN_BYTE - 1)));
}

STATIC_INLINE void bmp_clear(unsigned char* bmp, int index)
{
  bmp[index >> LOG_BITS_IN_BYTE] &= ~(1 << (index & (BITS_IN_BYTE - 1)));
}

STATIC_INLINE int bmp_test(unsigned char* bmp, int index)
{
  return bmp[index >> LOG_BITS_IN_BYTE] & (1 << (index & (BITS_IN_BYTE - 1)));
}

STATIC_INLINE int bmp_find_first_zero(unsigned char* bmp, int start, int len)
  __attribute__((unused));
STATIC_INLINE int bmp_find_first_zero(unsigned char* bmp, int start, int len)
{
  int index = start;
  unsigned char *p = bmp + (index >> LOG_BITS_IN_BYTE);
  int offset = index & (BITS_IN_BYTE - 1);
  unsigned char c = *p | ((1 << offset) - 1);
  do {
    if (c != 0xff) {
      if ((c & 0x0f) == 0x0f)  { index += 4; c >>= 4; }
      if ((c & 0x03) == 0x03)  { index += 2; c >>= 2; }
      if ((c & 0x01) == 0x01)  index += 1;
      if (index < len)      
	return index;
      else
	return -1;
    }
    index += 8;
    c = *++p;
  } while (index < len);
  
  return -1;
}

STATIC_INLINE int bmp_count_live(unsigned char *bmp, int len)
{
  const static unsigned char one_bits[] =
    { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4 };
  int count = 0;
  int c;

  while (len >= 8) {
    count += one_bits[*bmp++];
    len -= 8;
  }
  c = *bmp & ((1 << len) - 1);
  count += one_bits[c];
  return count;
}

/* page layout
 *   +-------
 *   | size, type, ..
 *   +- - - -
 *   | next
 *   +------
 *   | mark bitmap
 *   |
 *   +------
 *   | used bitmap
 *   |
 *   +------
 *   | first block
 * (solid line: granule alignment)
 */

STATIC_INLINE void
page_so_init(so_page_header *ph, unsigned int size, cell_type_t type);
STATIC_INLINE size_t page_so_bmp_granules(so_page_header *ph);
STATIC_INLINE unsigned char *page_so_mark_bitmap(so_page_header *ph);
STATIC_INLINE unsigned char *page_so_used_bitmap(so_page_header *ph);
STATIC_INLINE uintptr_t page_so_first_block(so_page_header *ph);
STATIC_INLINE int page_so_block_index(so_page_header *ph, uintptr_t p);
STATIC_INLINE int page_so_blocks(so_page_header *ph);
STATIC_INLINE uintptr_t page_lo_payload(lo_page_header *ph);

#ifdef GC_DEBUG
page_header_t *payload_to_page_header(uintptr_t ptr)
{
  return ((page_header_t *) (ptr & ~(BYTES_IN_PAGE - 1)));
}
#endif /* GC_DEBUG */


STATIC_INLINE void page_so_set_end_used_bitmap(so_page_header *ph)
{
  unsigned char *used_bmp = page_so_used_bitmap(ph);
  int blocks = page_so_blocks(ph);
  int index = blocks >> LOG_BITS_IN_GRANULE;
  int offset = blocks & (BITS_IN_GRANULE - 1);

#if BYTES_IN_GRANULE == 4
  ((uint32_t *) used_bmp)[index] |= ~((1 << offset) - 1);
#elif BYTES_IN_GRANULE == 8
  ((uint64_t *) used_bmp)[index] |= ~((1LL << offset) - 1);
#else
#error not implemented
#endif
}

STATIC_INLINE void
page_so_init(so_page_header *ph, unsigned int size, cell_type_t type)
{
  unsigned int bmp_granules;
  ph->page_type = PAGE_TYPE_SOBJ;
  ph->type = type;
  ph->size = size;
  ph->next = NULL;
#ifdef BIBOP_CACHE_BMP_GRANULES
  {
    unsigned int bmp_entries = GRANULES_IN_PAGE / size;
    unsigned int bmp_granules =
      (bmp_entries + BITS_IN_GRANULE - 1) >> LOG_BITS_IN_GRANULE;
    ph->bmp_granules = bmp_granules;
  }
#endif /* BIBOP_CACHE_BMP_GRANULES */
  bmp_granules = page_so_bmp_granules(ph);
  memset(ph->bitmap, 0, (bmp_granules << LOG_BYTES_IN_GRANULE) * 2);
  page_so_set_end_used_bitmap(ph);
}

STATIC_INLINE size_t page_so_bmp_granules_by_size(size_t size)
  __attribute__((unused));
STATIC_INLINE size_t page_so_bmp_granules_by_size(size_t size)
{
  const size_t bmp_entries = GRANULES_IN_PAGE / size;
  const size_t bmp_granules =
    (bmp_entries + BITS_IN_GRANULE - 1) >> LOG_BITS_IN_GRANULE;
  return bmp_granules;
}

STATIC_INLINE size_t page_so_mark_bitmap_offset()
  __attribute__((unused));
STATIC_INLINE size_t page_so_mark_bitmap_offset()
{
  return (size_t) &((so_page_header *) 0)->bitmap;
}

STATIC_INLINE size_t page_so_used_bitmap_offset_by_size(size_t size)
  __attribute__((unused));
STATIC_INLINE size_t page_so_used_bitmap_offset_by_size(size_t size)
{
  const size_t mark_bmp_offset = page_so_mark_bitmap_offset();
  const size_t bmp_granules = page_so_bmp_granules_by_size(size);
  return mark_bmp_offset + (bmp_granules << LOG_BYTES_IN_GRANULE);
}

STATIC_INLINE size_t page_so_first_block_offset_by_size(size_t size)
  __attribute__((unused));
STATIC_INLINE size_t page_so_first_block_offset_by_size(size_t size)
{
  const size_t mark_bmp_offset = page_so_mark_bitmap_offset();
  const size_t bmp_granules = page_so_bmp_granules_by_size(size);
  return mark_bmp_offset + (bmp_granules << LOG_BYTES_IN_GRANULE) * 2;
}

STATIC_INLINE size_t page_so_blocks_by_size(size_t size)
  __attribute__((unused));
STATIC_INLINE size_t page_so_blocks_by_size(size_t size)
{
  size_t payload_offset = page_so_first_block_offset_by_size(size);
  size_t payload_granules =
    GRANULES_IN_PAGE - (payload_offset  >> LOG_BYTES_IN_GRANULE);
  return payload_granules / size;
}

STATIC_INLINE size_t page_so_bmp_granules(so_page_header *ph)
{
#ifdef BIBOP_CACHE_BMP_GRANULES
  return ph->bmp_granules;
#else /* BIBOP_CACHE_BMP_GRANULES */
  unsigned int size = ph->size;
  return page_so_bmp_granules_by_size(size);
#endif /* BIBOP_CACHE_BMP_GRANULES */
}

STATIC_INLINE unsigned char *page_so_mark_bitmap(so_page_header *ph)
{
  return ph->bitmap;
}

STATIC_INLINE unsigned char *page_so_used_bitmap(so_page_header *ph)
{
  unsigned char *mark_bmp = ph->bitmap;
  unsigned int bmp_granules = page_so_bmp_granules(ph);
  return mark_bmp + (bmp_granules << LOG_BYTES_IN_GRANULE);
}

STATIC_INLINE uintptr_t page_so_first_block(so_page_header *ph)
{
  unsigned char *mark_bmp = ph->bitmap;
  unsigned int bmp_granules = page_so_bmp_granules(ph);
  return (uintptr_t) (mark_bmp + (bmp_granules << LOG_BYTES_IN_GRANULE) * 2);
}

STATIC_INLINE int page_so_block_index(so_page_header *ph, uintptr_t p)
{
  uintptr_t first_block = page_so_first_block(ph);
  unsigned int size_in_byte = ph->size << LOG_BYTES_IN_GRANULE;
  int index = (((uintptr_t) p) - first_block) / size_in_byte;
  assert((((uintptr_t) p) - first_block) % size_in_byte == 0);
  return index;
}

STATIC_INLINE int page_so_blocks(so_page_header *ph)
{
  unsigned int size = ph->size;
  unsigned int payload_offset = page_so_first_block(ph) - ((uintptr_t) ph);
  unsigned int payload_granules =
    GRANULES_IN_PAGE - (payload_offset >> LOG_BYTES_IN_GRANULE);
  return payload_granules / size;
}  

#ifdef GC_DEBUG
STATIC_INLINE int page_so_used_blocks(so_page_header *ph)
{
  static char nbits[] = { 0, 1, 1, 2, 1, 2, 2, 3 };
  unsigned char *used_bmp = page_so_used_bitmap(ph);
  int nblocks = page_so_blocks(ph);
  unsigned char c;
  int count = 0;
  int i;

  for (i = 0; i < (nblocks >> LOG_BITS_IN_BYTE); i++) {
    c = used_bmp[i];
    count += nbits[c & 0x7];
    count += nbits[(c >> 4) & 0x7];
  }
  c = used_bmp[i];
  for (i = 0; i < (nblocks & (BITS_IN_BYTE - 1)); i++) {
    count += c & 1;
    c >>= 1;
  }
  return count;
}
#endif /* GC_DEBUG */

STATIC_INLINE uintptr_t page_lo_payload(lo_page_header *ph)
{
  uintptr_t addr = (uintptr_t) ph;
  addr += ROUNDUP(sizeof(lo_page_header), BYTES_IN_GRANULE);
  return addr;
}

STATIC_INLINE size_t page_lo_pages(lo_page_header *ph)
{
  size_t granules =
    ROUNDUP(sizeof(lo_page_header), BYTES_IN_GRANULE) + ph->size;
  size_t pages = (granules + GRANULES_IN_PAGE - 1) >> LOG_GRANULES_IN_PAGE;
  return pages;
}

/*
 * cell mark
 */
void mark_cell(void *p)
{
  page_header_t *xph = payload_to_page_header((uintptr_t) p);
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ
#ifdef BIBOP_MOBJ
      || xph->u.x.page_type == PAGE_TYPE_MOBJ
#endif /* BIBOP_MOBJ */
      ) {
    so_page_header *ph = (so_page_header *) xph;
    unsigned char *mark_bmp = page_so_mark_bitmap(ph);
    int index = page_so_block_index(ph, (uintptr_t) p);
    bmp_set(mark_bmp, index);
  } else {
    assert(xph->u.x.page_type == PAGE_TYPE_LOBJ);
    xph->u.lo.markbit = 1;
  }
}

STATIC_INLINE void unmark_cell (void *p) __attribute__((unused));
STATIC_INLINE void unmark_cell (void *p)
{
  page_header_t *xph = payload_to_page_header((uintptr_t) p);
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ
#ifdef BIBOP_MOBJ
      || xph->u.x.page_type == PAGE_TYPE_MOBJ
#endif /* BIBOP_MOBJ */
      ) {
    so_page_header *ph = (so_page_header *) xph;
    unsigned char *mark_bmp = page_so_mark_bitmap(ph);
    int index = page_so_block_index(ph, (uintptr_t) p);
    bmp_clear(mark_bmp, index);
  } else {
    assert(xph->u.x.page_type == PAGE_TYPE_LOBJ);
    xph->u.lo.markbit = 0;
  }
}

int is_marked_cell(void *p)
{
  page_header_t *xph = payload_to_page_header((uintptr_t) p);
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ
#ifdef BIBOP_MOBJ
      || xph->u.x.page_type == PAGE_TYPE_MOBJ
#endif /* BIBOP_MOBJ */
      ) {
    so_page_header *ph = (so_page_header *) xph;
    unsigned char *mark_bmp = page_so_mark_bitmap(ph);
    int index = page_so_block_index(ph, (uintptr_t) p);
    return bmp_test(mark_bmp, index);
  } else {
    assert(xph->u.x.page_type == PAGE_TYPE_LOBJ);
    return xph->u.lo.markbit;
  }
}

int test_and_mark_cell(void *p)
{
  page_header_t *xph = payload_to_page_header((uintptr_t) p);
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ
#ifdef BIBOP_MOBJ
      || xph->u.x.page_type == PAGE_TYPE_MOBJ
#endif /* BIBOP_MOBJ */
      ) {
    so_page_header *ph = (so_page_header *) xph;
    unsigned char *mark_bmp = page_so_mark_bitmap(ph);
    int index = page_so_block_index(ph, (uintptr_t) p);
    if (bmp_test(mark_bmp, index))
      return 1;
    bmp_set(mark_bmp, index);
  } else {
    assert(xph->u.x.page_type == PAGE_TYPE_LOBJ);
    if (xph->u.lo.markbit == 1)
      return 1;
    xph->u.lo.markbit = 1;
  }
  return 0;
}

/*
 * Initialise
 */

/* TODO: compute offline */
STATIC_INLINE void compute_sizeclass_map()
{
  int i, index;
  for (i = 0, index = 0; i <= GRANULES_IN_PAGE; i++) {
    sizeclass_map[i] = index;
    if (index < (sizeof(sizeclasses) / sizeof(sizeclasses[0])) &&
	i == sizeclasses[index])
      index++;
  }
}

void space_init(size_t bytes)
{
  uintptr_t addr;
  struct free_page_header *p;
  int i, j;
  
  addr = (uintptr_t) malloc(bytes + BYTES_IN_PAGE - 1);
  space.num_pages = bytes / BYTES_IN_PAGE;
  space.num_free_pages = space.num_pages;
  p = (struct free_page_header *)
    ((addr + BYTES_IN_PAGE - 1) & ~(BYTES_IN_PAGE - 1));
  space.addr = (uintptr_t) p;
  space.end = space.addr + (bytes & ~(BYTES_IN_PAGE - 1));
  p->page_type = PAGE_TYPE_FREE;
  p->num_pages = space.num_pages;
  p->next = NULL;
  space.page_pool = p;
  for (i = 0; i < NUM_CELL_TYPES; i++)
    for (j = 0; j < NUM_SOBJ_SIZECLASSES; j++)
      space.freelist[i][j] = NULL;
#ifdef BIBOP_MOBJ
  for (i = 0; i < NUM_MOBJ_SIZECLASSES; i++)
    space.mo_freelist[i] = NULL;
#endif /* BIBOP_MOBJ */
#ifdef BIBOP_SEGREGATE_1PAGE
  space.single_page_pool = NULL;
#endif /* BIBOP_SEGREGATE_1PAGE */
#ifdef BIBOP_2WAY_ALLOC
  space.last_free_chunk = space.page_pool;
#endif /* BIBOP_2WAY_ALLOC */
#ifdef FLONUM_SPACE
  space.num_flonum_pages = 0;
#endif /* FLONUM_SPACE */
  compute_sizeclass_map();
}

/*
 * allocation
 */
STATIC_INLINE page_header_t *alloc_page(size_t alloc_pages)
{
  free_page_header **pp;

#ifdef BIBOP_SEGREGATE_1PAGE
  if (alloc_pages == 1)
    if (space.single_page_pool != NULL) {
      free_page_header *found = space.single_page_pool;
      space.single_page_pool = found->next;
      space.num_free_pages -= 1;
      return (page_header_t *) found;
    }
#endif /* BIBOP_SEGREGATE_1PAGE */

#ifdef BIBOP_2WAY_ALLOC
  if (alloc_pages == 1) {
    if (space.last_free_chunk != NULL &&
	space.last_free_chunk->num_pages > 1) {
      free_page_header *p = space.last_free_chunk;
      free_page_header *found = (free_page_header *)
	(((uintptr_t) p) + ((p->num_pages - 1) << LOG_BYTES_IN_PAGE));
      assert(p->next == NULL);
      p->num_pages -= 1;
      space.num_free_pages -= 1;
      return (page_header_t*) found;
    }
  }
#endif /* BIBOP_2WAY_ALLOC */

  for (pp = &space.page_pool; *pp != NULL; pp = &(*pp)->next) {
    free_page_header *p = *pp;
    assert(p->page_type == PAGE_TYPE_FREE);
    if (p->num_pages == alloc_pages) {
#ifdef VERIFY_BIBOP
      if (p->next != NULL)
	verify_free_page(p->next);
#endif /* VERIFY_BIBOP */
      *pp = p->next;
      space.num_free_pages -= alloc_pages;
      return (page_header_t *) p;
    } else if (p->num_pages > alloc_pages) {
      int num_pages = p->num_pages - alloc_pages;
      free_page_header *next = p->next;
      page_header_t *found = (page_header_t *) p;
      p = (free_page_header *)
	(((uintptr_t) p) + (alloc_pages << LOG_BYTES_IN_PAGE));
      p->page_type = PAGE_TYPE_FREE;
      p->num_pages = num_pages;
      p->next = next;
#ifdef VERIFY_BIBOP
      verify_free_page(p);
#endif /* VERIFY_BIBOP */
      *pp = p;
#ifdef BIBOP_2WAY_ALLOC
      if (found == (page_header_t *) space.last_free_chunk)
	space.last_free_chunk = p;
#endif /* BIBOP_2WAY_ALLOC */
      space.num_free_pages -= alloc_pages;
      return found;
    }
  }

  return NULL;
}

STATIC_INLINE uintptr_t
alloc_large_object(size_t request_granules, cell_type_t type)
{
  int alloc_granules =
    request_granules + ROUNDUP(sizeof(lo_page_header), BYTES_IN_GRANULE);
  int alloc_pages =
    (alloc_granules + GRANULES_IN_PAGE - 1) / GRANULES_IN_PAGE;
  page_header_t* found = alloc_page(alloc_pages);

  if (found != NULL) {
    lo_page_header *p = (lo_page_header *) found;
    p->page_type = PAGE_TYPE_LOBJ;
    p->type = type;
    p->markbit = 0;
    p->size = request_granules;
    return page_lo_payload(p);
  }

  return 0;
}

STATIC_INLINE uintptr_t alloc_block_in_page(so_page_header *ph)
{
  int size = ph->size;
  unsigned char *used_bmp = page_so_used_bitmap(ph);
  int bmp_entries = GRANULES_IN_PAGE / size;
  int index = bmp_find_first_zero(used_bmp, 0, bmp_entries);

  if (index == -1)
    return 0;
  else {
    uintptr_t first_block = page_so_first_block(ph);
    uintptr_t block = first_block + ((index * size) << LOG_BYTES_IN_GRANULE);
    bmp_set(used_bmp, index);
    return block;
  }
}

STATIC_INLINE uintptr_t
alloc_small_object(unsigned int request_granules, cell_type_t type)
{
  uintptr_t found = 0;
  int sizeclass_index = sizeclass_map[request_granules];
  so_page_header **pp;

  /* find a half-used page */
  for (pp = &space.freelist[type][sizeclass_index]; *pp != NULL; ) {
    struct so_page_header *p = *pp;
    assert(p->page_type == PAGE_TYPE_SOBJ);
    assert(p->type == type);
    found = alloc_block_in_page(p);
    if (found != 0) {
      assert((found & (BYTES_IN_GRANULE - 1)) == 0);
      return found;
    }
    /* This page was full and failed to allocate in this page. */
    *pp = p->next;
  }

  /* allocate a new page */
  {
    page_header_t *xph = alloc_page(1);
    if (xph != NULL) {
      so_page_header *ph = &xph->u.so;
      uintptr_t found;
      page_so_init(ph, sizeclasses[sizeclass_index], type);
      ph->next = space.freelist[type][sizeclass_index];
      space.freelist[type][sizeclass_index] = ph;
      found = alloc_block_in_page(ph);
      assert((found & (BYTES_IN_GRANULE - 1)) == 0);
      return found;
    }
  }

  return 0;
}

#ifdef BIBOP_MOBJ
STATIC_INLINE uintptr_t
alloc_middle_object(unsigned int request_granules, cell_type_t type)
{
  uintptr_t found = 0;
  unsigned int total_granules = request_granules + 1;
  int sizeclass_index =
    sizeclass_map[total_granules] - NUM_SOBJ_SIZECLASSES;
  so_page_header **pp;

  /* find a half-used page */
  for (pp = &space.mo_freelist[sizeclass_index]; *pp != NULL; ) {
    struct so_page_header *p = *pp;
    assert(p->page_type == PAGE_TYPE_MOBJ);
    assert(p->type == 0);
    found = alloc_block_in_page(p);
    if (found != 0) {
      cell_status *status = page_mo_cell_status(p, found);
      assert((found & (BYTES_IN_GRANULE - 1)) == 0);
      status->type = type;
      status->extra = p->size - request_granules;
      return found;
    }
    /* This page was full and failed to allocate in this page. */
    *pp = p->next;
  }

  /* allocate a new page */
  {
    page_header_t *xph = alloc_page(1);
    if (xph != NULL) {
      so_page_header *ph = &xph->u.so;
      uintptr_t found;
      cell_status *status;
      page_so_init(ph, sizeclasses[sizeclass_index + NUM_SOBJ_SIZECLASSES], 0);
      ph->page_type = PAGE_TYPE_MOBJ;
      ph->next = space.mo_freelist[sizeclass_index];
      space.mo_freelist[sizeclass_index] = ph;
      found = alloc_block_in_page(ph);
      assert((found & (BYTES_IN_GRANULE - 1)) == 0);
      status = page_mo_cell_status(ph, found);
      status->type = type;
      status->extra = ph->size - request_granules;
      return found;
    }
  }

  return 0;
}
#endif /* BIBOP_MOBJ */

void *space_alloc(uintptr_t request_bytes, cell_type_t type)
{
  int request_granules =
    (request_bytes + BYTES_IN_GRANULE - 1) >> LOG_BYTES_IN_GRANULE;
  if (request_granules == 0)
    request_granules = 1;

  if (request_granules <= MAX_SOBJ_GRANULES)
    return (void*) alloc_small_object(request_granules, type);
#ifdef BIBOP_MOBJ
  else if (request_granules <= MAX_MOBJ_GRANULES)
    return (void*) alloc_middle_object(request_granules, type);
#endif /* BIBOP_MOBJ */
  else
    return (void*) alloc_large_object(request_granules, type);
}

#ifdef FLONUM_SPACE
static inline unsigned int flonum_space_hash(double x)
{
  unsigned long long key = *(unsigned long long*)&x;
  key ^= key >> 12;
  key ^= key >> 32;
  key ^= key >> (52 - LOG_GRANULES_IN_PAGE);
  return (unsigned int) key;
}

FlonumCell *space_try_alloc_flonum(double x)
{
  const size_t flonum_cell_granules =
    (sizeof(FlonumCell) + BYTES_IN_GRANULE - 1) >> LOG_BYTES_IN_GRANULE;
  const size_t nblocks = page_so_blocks_by_size(flonum_cell_granules);
  unsigned int key;
  int page_idx;
  so_page_header *ph;
  unsigned char *used_bmp;
  FlonumCell *payload;

  if (space.num_flonum_pages == 0) {
    so_page_header *flonum_pages[MAX_FLONUM_PAGES];
    int i;
    for (i = 0; i < MAX_FLONUM_PAGES; i++) {
      page_header_t *xph = alloc_page(1);
      so_page_header *ph;
      if (xph == NULL)
	return NULL;
      ph = &xph->u.so;
      page_so_init(ph, flonum_cell_granules, CELLT_FLONUM);
      ph->page_type = PAGE_TYPE_SOBJ;
      ph->next = NULL;
      flonum_pages[i] = ph;
    }
    for (i = 0; i < MAX_FLONUM_PAGES; i++) {
      flonum_pages[i]->next = FLONUM_PAGE_MARKER;
      space.flonum_pages[i] = flonum_pages[i];
    }
    space.num_flonum_pages = MAX_FLONUM_PAGES;
  }
  key = flonum_space_hash(x);
  page_idx = key & (MAX_FLONUM_PAGES - 1);
  key >>= LOG_MAX_FLONUM_PAGES;
  key %= nblocks;
  ph = space.flonum_pages[page_idx];
  used_bmp =
    ((unsigned char *) ph) +
    page_so_used_bitmap_offset_by_size(flonum_cell_granules);
  payload = (FlonumCell *)
    (((unsigned char *) ph) +
     page_so_first_block_offset_by_size(flonum_cell_granules));
  if (bmp_test(used_bmp, key) != 0) {
    if (payload[key].value == x)
      return &payload[key];
    return NULL;
  }
  payload[key].value = x;
  bmp_set(used_bmp, key);
  return &payload[key];
}
#endif /* FLONUM_SPACE */

/*
 * sweep
 */


/**
 * 1. Copy mark bmp to used bmp.
 * 2. Clear mark bmp.
 * 3. Link this page to the free list (unlesss full or empty).
 * return 0 if empty
 **/
static int sweep_so_page(so_page_header *ph)
{
#if BYTES_IN_GRANULE == 4
#define granule_t uint32_t
#define ZELO 0
#elif BYTES_IN_GRANULE == 8
#define granule_t uint64_t
#define ZERO 0LL
#else
#error not implemented
#endif

  unsigned int bmp_granules = page_so_bmp_granules(ph);
  granule_t *mark_bmp = (granule_t *) page_so_mark_bitmap(ph);
  granule_t *used_bmp = (granule_t *) page_so_used_bitmap(ph);
  granule_t is_free = ZERO;
  granule_t is_full = ~ZERO;
  int i;

  for (i = 0; i < bmp_granules - 1; i++) {
    granule_t x = mark_bmp[i];
    is_free |= x;
    is_full &= x;
    used_bmp[i] = x;
  }
  {
    granule_t x = mark_bmp[i];
    is_free |= x;
    used_bmp[i] = x;
  }
#ifdef FLONUM_SPACE
  if (ph->next != FLONUM_PAGE_MARKER && is_free == ZERO)
    return 0;
#else /* FLONUM_SPACE */
  if (is_free == ZERO)
    return 0;
#endif /* FLONUM_SPACE */
  page_so_set_end_used_bitmap(ph);
  is_full &= used_bmp[i];
  memset(mark_bmp, 0, bmp_granules << LOG_BYTES_IN_GRANULE);
#ifdef FLONUM_SPACE
  if (ph->next == FLONUM_PAGE_MARKER)
    return 1;
#endif /* FLONUM_SPACE */
  if (is_full != ~ZERO) {
#ifdef BIBOP_MOBJ
    if (ph->size <= MAX_SOBJ_GRANULES) {
      int sizeclass_index = sizeclass_map[ph->size];
      ph->next = space.freelist[ph->type][sizeclass_index];
      space.freelist[ph->type][sizeclass_index] = ph;
    } else {
      int sizeclass_index = sizeclass_map[ph->size] - NUM_SOBJ_SIZECLASSES;
      ph->next = space.mo_freelist[sizeclass_index];
      space.mo_freelist[sizeclass_index] = ph;
    }
#else /* BIBOP_MOBJ */
    int sizeclass_index = sizeclass_map[ph->size];
    ph->next = space.freelist[ph->type][sizeclass_index];
    space.freelist[ph->type][sizeclass_index] = ph;
#endif /* BIBOP_MOBJ */
  }
  return 1;

#undef granule_t
#undef ZERO
}

#ifdef GC_PROF
void profile_live_objects()
{
  uintptr_t page_addr;
  for (page_addr = space.addr; page_addr < space.end; ) {
    page_header_t *xph = (page_header_t *) page_addr;
    if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
      so_page_header *ph = &xph->u.so;
      unsigned char *used_bmp = page_so_used_bitmap(ph);
      int nblocks = page_so_blocks(ph);
      int nlive = bmp_count_live(used_bmp, nblocks);
      pertype_live_bytes[ph->type] +=
	(ph->size << LOG_BYTES_IN_GRANULE) * nlive;
      pertype_live_count[ph->type] += nlive;
      page_addr += BYTES_IN_PAGE;
#ifdef BIBOP_MOBJ
    } else if (xph->u.x.page_type == PAGE_TYPE_MOBJ) {
#error not implemented
#endif /* BIBOP_MOBJ */
    } else if (xph->u.x.page_type == PAGE_TYPE_LOBJ) {
      lo_page_header *ph = &xph->u.lo;
      pertype_live_bytes[ph->type] += ph->size;
      pertype_live_count[ph->type] += 1;
      page_addr += page_lo_pages(ph) << LOG_BYTES_IN_PAGE;
    } else {
      free_page_header *ph = &xph->u.free;
      page_addr += ph->num_pages << LOG_BYTES_IN_PAGE;
    }
  }
}
#endif /* GC_PROF */

void sweep()
{
  uintptr_t page_addr;
  int i, j;
  free_page_header *last_free = NULL;
  uintptr_t free_end = 0;
  free_page_header **free_pp = &space.page_pool;
#ifdef BIBOP_SEGREGATE_1PAGE
  free_page_header **free_single_pp = &space.single_page_pool;
#endif /* BIBOP_SEGREGATE_1PAGE */
#ifdef VERIFY_BIBOP
  uintptr_t prev_free_end = 0;
#endif /* VERIFY_BIBOP */

  for (i = 0; i < NUM_CELL_TYPES; i++)
    for (j = 0; j < NUM_SOBJ_SIZECLASSES; j++)
      space.freelist[i][j] = NULL;
#ifdef BIBOP_MOBJ
  for (i = 0; i < NUM_MOBJ_SIZECLASSES; i++)
      space.mo_freelist[i] = NULL;
#endif /* BIBOP_MOBJ */
  space.num_free_pages = 0;
  space.page_pool = NULL;
#ifdef BIBOP_SEGREGATE_1PAGE
  space.single_page_pool = NULL;
#endif /* BIBOP_SEGREGATE_1PAGE */
#ifdef BIBOP_2WAY_ALLOC
  space.last_free_chunk = NULL;
#endif /* BIBOP_2WAY_ALLOC */

  page_addr = space.addr;
  while (page_addr < space.end) {
    while (page_addr < space.end) {
      page_header_t *xph = (page_header_t*) page_addr;
      if (xph->u.x.page_type == PAGE_TYPE_SOBJ
#ifdef BIBOP_MOBJ
	  || xph->u.x.page_type == PAGE_TYPE_MOBJ
#endif /* BIBOP_MOBJ */
	  ) {
	so_page_header *ph = &xph->u.so;
	page_addr += BYTES_IN_PAGE;
	if (sweep_so_page(ph) == 0) {
	  last_free = &xph->u.free;
#ifdef VERIFY_BIBOP
	  assert(prev_free_end < (uintptr_t) last_free);
#endif /* VERIFY_BIBOP */
	  break;
	}
      } else if (xph->u.x.page_type == PAGE_TYPE_LOBJ) {
	lo_page_header *ph = &xph->u.lo;
	page_addr += page_lo_pages(ph) << LOG_BYTES_IN_PAGE;
	if (ph->markbit == 0) {
	  last_free = &xph->u.free;
#ifdef VERIFY_BIBIOP
	  assert(prev_free_end < (uintptr_t) last_free);
#endif /* VERIFY_BIBOP */
	  break;
	}
	ph->markbit = 0;
      } else {
	assert(xph->u.x.page_type == PAGE_TYPE_FREE);
	page_addr += xph->u.free.num_pages << LOG_BYTES_IN_PAGE;
	last_free = &xph->u.free;
#ifdef VERIFY_BIBOP
	assert(prev_free_end < (uintptr_t) last_free);
#endif /* VERIFY_BIBIOP */
	break;
      }
    }
    free_end = page_addr;
    while (page_addr < space.end) {
      page_header_t *xph = (page_header_t *) page_addr;
      if (xph->u.x.page_type == PAGE_TYPE_SOBJ
#ifdef BIBOP_MOBJ
	  || xph->u.x.page_type == PAGE_TYPE_MOBJ
#endif /* BIBOP_MOBJ */
	  ) {
	so_page_header *ph = &xph->u.so;
	page_addr  += BYTES_IN_PAGE;
	if (sweep_so_page(ph) != 0)
	  break;
      } else if (xph->u.x.page_type == PAGE_TYPE_LOBJ) {
	lo_page_header *ph = &xph->u.lo;
	page_addr += page_lo_pages(ph) << LOG_BYTES_IN_PAGE;
	if (ph->markbit != 0) {
	  ph->markbit = 0;
	  break;
	}
      } else {
	assert(xph->u.x.page_type == PAGE_TYPE_FREE);
	page_addr += xph->u.free.num_pages << LOG_BYTES_IN_PAGE;
      }
      free_end = page_addr;
    }
    if (last_free != NULL) {
      unsigned int bytes = free_end - ((uintptr_t) last_free);
      unsigned int pages = bytes >> LOG_BYTES_IN_PAGE;

      last_free->page_type = PAGE_TYPE_FREE;
      last_free->num_pages = pages;
      last_free->next = NULL;
#ifdef VERIFY_BIBOP
      prev_free_end = free_end;
      verify_free_page(last_free);
      memset(last_free + 1, 0xef,
	     (last_free->num_pages << LOG_BYTES_IN_PAGE) - sizeof(*last_free));
#endif /* VERIFY_BIBOP */
#ifdef BIBOP_SEGREGATE_1PAGE
      if (pages == 1) {
	*free_single_pp = last_free;
	free_single_pp = &last_free->next;
      } else {
	*free_pp = last_free;
	free_pp = &last_free->next;
#ifdef BIBOP_2WAY_ALLOC
	space.last_free_chunk = last_free;
#endif /* BIBOP_2WAY_ALLOC */
      }
#else /* BIBOP_SEGREGATE_1PAGE */
      *free_pp = last_free;
      free_pp = &last_free->next;
#endif /* BIBOP_SEGREGATE_1PAGE */
      last_free = NULL;
      space.num_free_pages += pages;
    }
  }
#ifdef VERIFY_BIBOP
  {
    free_page_header *ph;
    for (ph = space.page_pool; ph != NULL; ph = ph->next)
      verify_free_page(ph);
#ifdef BIBOP_SEGREGATE_1PAGE
    for (ph = space.single_page_pool; ph != NULL; ph = ph->next)  {
      assert(ph->num_pages == 1);
      verify_free_page(ph);
    }
#endif /* BIBOP_SEGREGATE_1PAGE */
  }
#endif /* VERIFY_BIBOP */

#ifdef GC_PROF
  profile_live_objects();
#endif /* GC_PROF */
  return;
}

#ifdef GC_DEBUG
void space_print_memory_status()
{
  uintptr_t page_addr;

  printf("space.num_free_pages = %d\n", space.num_free_pages);
  for (page_addr = space.addr; page_addr < space.end; ) {
    page_header_t *xph = (page_header_t *) page_addr;
    if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
      printf("%p - %p ( 1) %5d SOBJ size = %d type = %d %d/%d, %d\n",
	     (void*) page_addr,
	     (void*) (page_addr + BYTES_IN_PAGE),
	     BYTES_IN_PAGE - (xph->u.so.size << LOG_BYTES_IN_GRANULE) * page_so_used_blocks(&xph->u.so),
	     xph->u.so.size, xph->u.so.type,
	     page_so_used_blocks(&xph->u.so),
	     page_so_blocks(&xph->u.so),
	     page_so_used_blocks(&xph->u.so) * 100 / page_so_blocks(&xph->u.so));
      page_addr += BYTES_IN_PAGE;
#ifdef BIBOP_MOBJ
    } else if (xph->u.x.page_type == PAGE_TYPE_MOBJ) {
      printf("%p - %p ( 1) %5d MOBJ size = %d type = %d %d/%d, %d\n",
	     (void*) page_addr,
	     (void*) (page_addr + BYTES_IN_PAGE),
	     BYTES_IN_PAGE - (xph->u.so.size << LOG_BYTES_IN_GRANULE) * page_so_used_blocks(&xph->u.so),
	     xph->u.so.size, xph->u.so.type,
	     page_so_used_blocks(&xph->u.so),
	     page_so_blocks(&xph->u.so),
	     page_so_used_blocks(&xph->u.so) * 100 / page_so_blocks(&xph->u.so));
      page_addr += BYTES_IN_PAGE;
#endif /* BIBOP_MOBJ */
    } else if (xph->u.x.page_type == PAGE_TYPE_LOBJ) {
      printf("%p - %p (%2d) %5lu LOBJ size = %d type = %d\n",
	     (void*) page_addr,
	     (void*) (page_addr + (page_lo_pages(&xph->u.lo) << LOG_BYTES_IN_PAGE)),
	     (int) page_lo_pages(&xph->u.lo),
	     (page_lo_pages(&xph->u.lo) << LOG_BYTES_IN_PAGE) - (xph->u.lo.size << LOG_BYTES_IN_GRANULE),
	     xph->u.lo.size,
	     xph->u.lo.type);
      page_addr += page_lo_pages(&xph->u.lo) << LOG_BYTES_IN_PAGE;
    } else {
      printf("%p - %p (%2d) %5d FREE\n",
	     (void *) page_addr,
	     (void *) (page_addr + (xph->u.free.num_pages << LOG_BYTES_IN_PAGE)),
	     xph->u.free.num_pages,
	     xph->u.free.num_pages << LOG_BYTES_IN_PAGE);
      page_addr += xph->u.free.num_pages << LOG_BYTES_IN_PAGE;
    }
  }
}
#endif /* GC_DEBUG */

#ifdef VERIFY_BIBOP
static void verify_free_page(free_page_header *ph)
{
  assert(space.addr <= (uintptr_t) ph);
  assert((uintptr_t) ph < space.end);
  assert(((uintptr_t) ph) + (ph->num_pages << LOG_BYTES_IN_PAGE) <= space.end);
  assert(ph->next == NULL || space.addr <= (uintptr_t) ph->next);
  assert(ph->next == NULL || (uintptr_t) ph->next < space.end);
}
#endif /* VERIFY_BIBOP */
