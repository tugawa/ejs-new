#define LOG_BITS_IN_BYTE  3
#define BITS_IN_BYTE (1 << LOG_BITS_IN_BYTE)
#define ROUNDUP(s,u) (((s) + (u) - 1) & ~((u) - 1))

#if LOG_BYTES_IN_GRANULE == 2
static const int sizeclasses[] = {
  1, 2, 4, 8, 15, 31, 62, 124
};
#else /* LOG_BYTES_IN_GRANULE == 3 */
static const int sizeclasses[] = {
  1, 2, 4, 8, 15, 30, 60
};
#endif /* LOG_BYTES_IN_GRANULE */

#define NUM_SIZECLASSES (sizeof(sizeclasses) / sizeof(int))
#define MAX_SOBJ_GRANULES \
  (sizeclasses[sizeof(sizeclasses) / sizeof(sizeclasses[0]) - 1])

static int sizeclass_map[GRANULES_IN_PAGE];

#define NUM_TYPES 0x20
struct space {
  uintptr_t addr, end;
  int num_pages;
  int num_free_pages;
  struct free_page_header *page_pool;
  struct so_page_header *freelist[NUM_TYPES][NUM_SIZECLASSES];
};

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
page_so_init(so_page_header *ph, cell_type_t type, unsigned int size);
STATIC_INLINE size_t page_so_bmp_granules(so_page_header *ph);
STATIC_INLINE unsigned char *page_so_mark_bitmap(so_page_header *ph);
STATIC_INLINE unsigned char *page_so_used_bitmap(so_page_header *ph);
STATIC_INLINE uintptr_t page_so_first_block(so_page_header *ph);
STATIC_INLINE int page_so_block_index(so_page_header *ph, uintptr_t p);
STATIC_INLINE int page_so_blocks(so_page_header *ph);
STATIC_INLINE uintptr_t page_lo_payload(lo_page_header *ph);

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
  bmp_granules = page_so_bmp_granules(ph);
  memset(ph->bitmap, 0, (bmp_granules << LOG_BYTES_IN_GRANULE) * 2);
  page_so_set_end_used_bitmap(ph);
}

STATIC_INLINE size_t page_so_bmp_granules(so_page_header *ph)
{
  unsigned int size = ph->size;
  unsigned int bmp_entries = GRANULES_IN_PAGE / size;
  unsigned int bmp_granules =
    (bmp_entries + BITS_IN_GRANULE - 1) >> LOG_BITS_IN_GRANULE;
  return bmp_granules;
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
STATIC_INLINE void mark_cell(void *p)
{
  page_header_t *xph = payload_to_page_header((uintptr_t) p);
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
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
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
    so_page_header *ph = (so_page_header *) xph;
    unsigned char *mark_bmp = page_so_mark_bitmap(ph);
    int index = page_so_block_index(ph, (uintptr_t) p);
    bmp_clear(mark_bmp, index);
  } else {
    assert(xph->u.x.page_type == PAGE_TYPE_LOBJ);
    xph->u.lo.markbit = 0;
  }
}

STATIC_INLINE int is_marked_cell(void *p)
{
  page_header_t *xph = payload_to_page_header((uintptr_t) p);
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
    so_page_header *ph = (so_page_header *) xph;
    unsigned char *mark_bmp = page_so_mark_bitmap(ph);
    int index = page_so_block_index(ph, (uintptr_t) p);
    return bmp_test(mark_bmp, index);
  } else {
    assert(xph->u.x.page_type == PAGE_TYPE_LOBJ);
    return xph->u.lo.markbit;
  }
}

STATIC_INLINE int test_and_mark_cell(void *p)
{
  page_header_t *xph = payload_to_page_header((uintptr_t) p);
  if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
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
  space.end = space.addr + bytes;
  p->page_type = PAGE_TYPE_FREE;
  p->num_pages = space.num_pages;
  p->next = NULL;
  space.page_pool = p;
  for (i = 0; i < NUM_TYPES; i++)
    for (j = 0; j < NUM_SIZECLASSES; j++)
      space.freelist[i][j] = NULL;
  compute_sizeclass_map();
}

/*
 * allocation
 */
STATIC_INLINE page_header_t *alloc_page(size_t alloc_pages)
{
  free_page_header **pp;

  for (pp = &space.page_pool; *pp != NULL; pp = &(*pp)->next) {
    free_page_header *p = *pp;
    assert(p->page_type == PAGE_TYPE_FREE);
    if (p->num_pages == alloc_pages) {
#ifdef VERIFY_BIBOP
      verify_free_page(p->next);
      assert(p->next != NULL ||
	     ((uintptr_t) p) + (p->num_pages << LOG_BYTES_IN_PAGE)== space.end);
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
      assert(p->next != NULL ||
	     ((uintptr_t) p) + (p->num_pages << LOG_BYTES_IN_PAGE)== space.end);
#endif /* VERIFY_BIBOP */
      *pp = p;
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

static uintptr_t
alloc_small_object(uintptr_t request_granules, cell_type_t type)
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

void *space_alloc(uintptr_t request_bytes, cell_type_t type)
{
  int request_granules =
    (request_bytes + BYTES_IN_GRANULE - 1) >> LOG_BYTES_IN_GRANULE;
  if (request_granules == 0)
    request_granules = 1;

  if (request_granules > MAX_SOBJ_GRANULES)
    return (void*) alloc_large_object(request_granules, type);
  else
    return (void*) alloc_small_object(request_granules, type);
}

/*
 * sweep
 */


/**
 * 1. Copy mark bmp to used bmp.
 * 2. Clear mark bmp.
 * 3. Link this page to the free list (unlesss full or empty).
 * return 0 if empty
 **/
int sweep_so_page(so_page_header *ph)
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
  if (is_free == ZERO)
    return 0;
  page_so_set_end_used_bitmap(ph);
  is_full &= used_bmp[i];
  memset(mark_bmp, 0, bmp_granules << LOG_BYTES_IN_GRANULE);
  if (is_full != ~ZERO) {
    int sizeclass_index = sizeclass_map[ph->size];
    ph->next = space.freelist[ph->type][sizeclass_index];
    space.freelist[ph->type][sizeclass_index] = ph;
  }
  return 1;

#undef granule_t
#undef ZERO
}

void sweep()
{
  uintptr_t page_addr;
  int i, j;
  free_page_header *last_free = NULL;
  uintptr_t free_end = 0;
  free_page_header **free_pp = &space.page_pool;
#ifdef VERIFY_BIBOP
  uintptr_t prev_free_end = 0;
#endif /* VERIFY_BIBOP */

  for (i = 0; i < NUM_TYPES; i++)
    for (j = 0; j < NUM_SIZECLASSES; j++)
      space.freelist[i][j] = NULL;
  space.num_free_pages = 0;

  page_addr = space.addr;
  while (page_addr < space.end) {
    while (page_addr < space.end) {
      page_header_t *xph = (page_header_t*) page_addr;
      if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
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
      if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
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
      *free_pp = last_free;
      free_pp = &last_free->next;
      last_free = NULL;
      space.num_free_pages += pages;
    }
  }
#ifdef VERIFY_BIBOP
  {
    free_page_header *ph;
    for (ph = space.page_pool; ph != NULL; ph = ph->next)
      assert(ph->next != NULL ||
	     ((uintptr_t) ph) + (ph->num_pages << LOG_BYTES_IN_PAGE)
	     == space.end);
  }
#endif /* VERIFY_BIBOP */
  return;
}

int space_check_gc_request()
{
  return space.num_free_pages < (space.num_pages >> 3);
;
}

int in_js_space(void *addr_)
{
  uintptr_t addr = (uintptr_t) addr_;
  return space.addr <= addr && addr < space.end;
}

void space_print_memory_status()
{
  uintptr_t page_addr;

  printf("space.num_free_pages = %d\n", space.num_free_pages);
  for (page_addr = space.addr; page_addr < space.end; ) {
    page_header_t *xph = (page_header_t *) page_addr;
    if (xph->u.x.page_type == PAGE_TYPE_SOBJ) {
      printf("%p - %p (1) SOBJ size = %d type = %d %d/%d, %d\n",
	     (void*) page_addr,
	     (void*) (page_addr + BYTES_IN_PAGE),
	     xph->u.so.size, xph->u.so.type,
	     page_so_used_blocks(&xph->u.so),
	     page_so_blocks(&xph->u.so),
	     page_so_used_blocks(&xph->u.so) * 100 / page_so_blocks(&xph->u.so));
      page_addr += BYTES_IN_PAGE;
    } else if (xph->u.x.page_type == PAGE_TYPE_LOBJ) {
      printf("%p - %p (%d) LOBJ size = %d type = %d\n",
	     (void*) page_addr,
	     (void*) (page_addr + (page_lo_pages(&xph->u.lo) << LOG_BYTES_IN_PAGE)),
	     (int) page_lo_pages(&xph->u.lo),
	     xph->u.lo.size,
	     xph->u.lo.type);
      page_addr += page_lo_pages(&xph->u.lo) << LOG_BYTES_IN_PAGE;
    } else {
      printf("%p - %p (%d) FREE\n",
	     (void *) page_addr,
	     (void *) (page_addr + (xph->u.free.num_pages << LOG_BYTES_IN_PAGE)),
	     xph->u.free.num_pages);
      page_addr += xph->u.free.num_pages << LOG_BYTES_IN_PAGE;
    }
  }
}

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
