#ifndef BIBOP_SPACE_H
#define BIBOP_SPACE_H

#define LOG_BYTES_IN_PAGE 9
#define LOG_GRANULES_IN_PAGE (LOG_BYTES_IN_PAGE - LOG_BYTES_IN_GRANULE)
#define BYTES_IN_PAGE     (1 << LOG_BYTES_IN_PAGE)
#define GRANULES_IN_PAGE  (1 << LOG_GRANULES_IN_PAGE)
#define LOG_BITS_IN_BYTE  3
#define BITS_IN_BYTE (1 << LOG_BITS_IN_BYTE)

#define PAGE_HEADER_TYPE_BITS    9
#define PAGE_HEADER_SO_SIZE_BITS (LOG_BYTES_IN_PAGE - LOG_BYTES_IN_GRANULE)
#define PAGE_HEADER_LO_SIZE_BITS 32

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

struct space {
  uintptr_t addr, end;
  int num_pages;
  int num_free_pages;
  struct free_page_header *page_pool;
  struct so_page_header *freelist[NUM_CELL_TYPES][NUM_SIZECLASSES];
};

typedef enum page_type_t {
  PAGE_TYPE_FREE = 1,
  PAGE_TYPE_SOBJ = 2,
  PAGE_TYPE_LOBJ = 3
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
      unsigned int bmp_granules: 3;
#endif /* BIBOP_CACHE_BMP_GRANULES */
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

#ifdef GC_DEBUG
extern page_header_t *payload_to_page_header(uintptr_t ptr);
#else /* GC_DEBUG */
/* bibop-space private functions */
static inline page_header_t *payload_to_page_header(uintptr_t ptr);
#endif /* GC_DEBUG */

extern struct space space;

#endif /* BIBOP_SPACE_H */
