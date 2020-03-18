#ifndef BIBOP_SPACE_H
#define BIBOP_SPACE_H

#define LOG_BYTES_IN_PAGE 12
#define LOG_GRANULES_IN_PAGE (LOG_BYTES_IN_PAGE - LOG_BYTES_IN_GRANULE)
#define BYTES_IN_PAGE     (1 << LOG_BYTES_IN_PAGE)
#define GRANULES_IN_PAGE  (1 << LOG_GRANULES_IN_PAGE)
#define LOG_BITS_IN_BYTE  3
#define BITS_IN_BYTE (1 << LOG_BITS_IN_BYTE)

#define PAGE_HEADER_TYPE_BITS    9
#define PAGE_HEADER_SO_SIZE_BITS (LOG_BYTES_IN_PAGE - LOG_BYTES_IN_GRANULE)
#define PAGE_HEADER_LO_SIZE_BITS 32
#define PAGE_HEADER_SO_BMP_GRANULES_BITS \
  (LOG_GRANULES_IN_PAGE - LOG_BITS_IN_GRANULE + 1)

static const int sizeclasses[] = {
#if LOG_BYTES_IN_GRANULE == 2
  1, 2, 4, 8, 15, 31, 62, 124
#else /* LOG_BYTES_IN_GRANULE == 3 */
#if LOG_BYTES_IN_PAGE == 12
  1, 2, 4, 8, 15, 21, 31, 63, 127, 254
#elif LOG_BYTES_IN_PAGE == 11
  1, 2, 4, 8, 15, 21, 31, 63, 127
#elif LOG_BYTES_IN_PAGE == 10
  1, 2, 4, 8, 15, 30, 60
#elif LOG_BYTES_IN_PAGE == 9
  1, 2, 4, 8, 15, 30
#else /* LOG_BYTES_IN_PAGE */
#error not implemented
#endif /* LOG_BYTES_IN_PAGE */
#endif /* LOG_BYTES_IN_GRANULE */
};
#define NUM_SIZECLASSES (sizeof(sizeclasses) / sizeof(int))

#ifdef BIBOP_MOBJ
#define MAX_SOBJ_GRANULES    2
#define NUM_SOBJ_SIZECLASSES 2
#define NUM_MOBJ_SIZECLASSES (NUM_SIZECLASSES - NUM_SOBJ_SIZECLASSES)
#define MAX_MOBJ_GRANULES						\
  (sizeclasses[sizeof(sizeclasses) / sizeof(sizeclasses[0]) - 1] - 1)
#else /* BIBOP_MOBJ */
#define MAX_SOBJ_GRANULES						\
  (sizeclasses[sizeof(sizeclasses) / sizeof(sizeclasses[0]) - 1])
#define NUM_SOBJ_SIZECLASSES NUM_SIZECLASSES
#endif /* BIBOP_MOBJ */

struct space {
  uintptr_t addr, end;
  int num_pages;
  int num_free_pages;
  struct free_page_header *page_pool;
  struct so_page_header *freelist[NUM_CELL_TYPES][NUM_SOBJ_SIZECLASSES];
#ifdef BIBOP_MOBJ
  struct so_page_header *mo_freelist[NUM_MOBJ_SIZECLASSES];
#endif /* BIBOP_MOBJ */
#ifdef BIBOP_SEGREGATE_1PAGE
  struct free_page_header *single_page_pool;
#endif /* BIBOP_SEGREGATE_1PAGE */
#ifdef BIBOP_2WAY_ALLOC
  struct free_page_header *last_free_chunk;
#endif /* BIBOP_2WAY_ALLOC */
#ifdef FLONUM_SPACE
#define LOG_MAX_FLONUM_PAGES 4
#define MAX_FLONUM_PAGES (1 << LOG_MAX_FLONUM_PAGES)
  struct so_page_header *flonum_pages[MAX_FLONUM_PAGES];
  int num_flonum_pages;
#endif /* FLONUM_SPACE */
};

typedef enum page_type_t {
  PAGE_TYPE_FREE = 1,
  PAGE_TYPE_SOBJ = 2,
#ifdef BIBOP_MOBJ
  PAGE_TYPE_MOBJ = 3,
#endif /* BIBOP_MOBJ */
  PAGE_TYPE_LOBJ = 4
} page_type_t;

typedef struct free_page_header free_page_header;
typedef struct so_page_header so_page_header;
typedef struct lo_page_header lo_page_header;

typedef struct page_header_t {
  union {
    struct {
      page_type_t page_type: 3;
    } x;
    struct free_page_header {
      page_type_t  page_type: 3;
      unsigned int num_pages;
      struct free_page_header *next;
    } free;
    struct so_page_header {
      page_type_t page_type: 3;
      cell_type_t type:      PAGE_HEADER_TYPE_BITS;
      unsigned int size:     PAGE_HEADER_SO_SIZE_BITS;  /* size of block (in granule) */
#ifdef BIBOP_CACHE_BMP_GRANULES
      unsigned int bmp_granules: PAGE_HEADER_SO_BMP_GRANULES_BITS;
#endif /* BIBOP_CACHE_BMP_GRANULES */
#ifdef FLONUM_SPACE
#define FLONUM_PAGE_MARKER ((void*)-1)
#endif /* FLONUM_SPACE */
      struct so_page_header *next __attribute__((aligned(BYTES_IN_GRANULE)));
      unsigned char bitmap[];
    } so;
    struct lo_page_header {
      page_type_t  page_type: 3;
      cell_type_t  type:      PAGE_HEADER_TYPE_BITS;
      unsigned int markbit:   1;
      unsigned int size:      PAGE_HEADER_LO_SIZE_BITS;  /* size of payload */
    } lo;
  } u;
} page_header_t;

#ifdef BIBOP_MOBJ
typedef struct cell_status cell_status;
struct cell_status {
  cell_type_t  type:  PAGE_HEADER_TYPE_BITS;
  unsigned int extra: LOG_BYTES_IN_PAGE;
};
#endif /* BIBOP_MOBJ */

/* GC private functions */
extern void mark_cell(void *p);
extern int is_marked_cell(void *p);
extern int test_and_mark_cell(void *p);
extern void space_init(size_t bytes);
extern void *space_alloc(uintptr_t request_bytes, cell_type_t type);
extern void sweep(void);
static inline int space_check_gc_request();
static inline int in_js_space(void *addr_);
static inline cell_type_t space_get_cell_type(uintptr_t ptr);
#ifdef GC_DEBUG
extern void space_print_memory_status(void);
#endif /* GC_DEBUG */
#ifdef FLONUM_SPACE
extern FlonumCell *space_try_alloc_flonum(double x);
#endif /* FLONUM_SPACE */

#ifdef GC_DEBUG
extern page_header_t *payload_to_page_header(uintptr_t ptr);
#else /* GC_DEBUG */
/* bibop-space private functions */
static inline page_header_t *payload_to_page_header(uintptr_t ptr);
#endif /* GC_DEBUG */

#ifdef BIBOP_MOBJ
static inline
cell_status *page_mo_cell_status(so_page_header *ph, uintptr_t ptr);
#endif /* BIBOP_MOBJ */

extern struct space space;

#endif /* BIBOP_SPACE_H */
