#define LOG_BYTES_IN_PAGE 9
#define LOG_GRANULES_IN_PAGE (LOG_BYTES_IN_PAGE - LOG_BYTES_IN_GRANULE)
#define BYTES_IN_PAGE     (1 << LOG_BYTES_IN_PAGE)
#define GRANULES_IN_PAGE  (1 << LOG_GRANULES_IN_PAGE)

#define PAGE_HEADER_TYPE_BITS    9
#define PAGE_HEADER_SO_SIZE_BITS (LOG_BYTES_IN_PAGE - LOG_BYTES_IN_GRANULE)
#define PAGE_HEADER_LO_SIZE_BITS 32

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
      page_type_t page_type: 2;
    } x;
    struct free_page_header {
      page_type_t  page_type: 2;
      unsigned int num_pages;
      struct free_page_header *next;
    } free;
    struct so_page_header {
      page_type_t page_type: 2;
      cell_type_t type:      PAGE_HEADER_TYPE_BITS;
      unsigned int size:     PAGE_HEADER_SO_SIZE_BITS;  /* size of block (in granule) */
      struct so_page_header *next __attribute__((aligned(BYTES_IN_GRANULE)));
      unsigned char bitmap[];
    } so;
    struct lo_page_header {
      page_type_t  page_type: 2;
      cell_type_t  type:      PAGE_HEADER_TYPE_BITS;
      unsigned int markbit:   1;
      unsigned int size:      PAGE_HEADER_LO_SIZE_BITS;  /* size of payload */
    } lo;
  } u;
} page_header_t;

static inline page_header_t *payload_to_page_header(uintptr_t ptr)
{
  return ((page_header_t *) (ptr & ~(BYTES_IN_PAGE - 1)));
}


/* space interface */
static inline cell_type_t space_get_cell_type(uintptr_t ptr)
{
  page_header_t *ph = payload_to_page_header(ptr);
  if (ph->u.x.page_type == PAGE_TYPE_SOBJ)
    return ph->u.so.type;
  else
    return ph->u.lo.type;
}

/* GC interface */
static inline cell_type_t gc_obj_header_type(void *p)
{
  return space_get_cell_type((uintptr_t) p);
}
