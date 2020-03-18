#ifndef FREELIST_SPACE_H
#define FREELIST_SPACE_H

#ifdef FLONUM_SPACE
#error freelist space does not support FLONUM_SPACE.
#endif /* FLONUM_SPACE */

#ifdef EXCESSIVE_GC
#define GC_THREASHOLD_SHIFT 4
#else  /* EXCESSIVE_GC */
#define GC_THREASHOLD_SHIFT 1
#endif /* EXCESSIVE_GC */

/*
 * Object header layout
 *
 * Heap objects are aligned in `granule' boundary.  Header may consist
 * of multiple granules.  HEADER_GRANULES gives the number of granules
 * in a header.
 *
 * Header fields
 *  - type    Cell type
 *  - markbit Mark bit for GC
 *  - extra   The number of over-allocated space in granule.
 *  - gen     Generation of this object describing the number of GC cycles
 *            have been performed (modulo field size) befor the allocation
 *            of this object.
 *  - magic   Magic number
 *  - size    Size of the object in granule, including the header and extra.
 */

#ifdef BIT_ALIGN32
#define HEADER_GRANULES       2
#define HEADER_TYPE_BITS      8
#define HEADER_MARKBIT_BITS   1
#define HEADER_EXTRA_BITS     3
#define HEADER_GEN_BITS       4
#define HEADER_MAGIC_BITS     16
#define HEADER_SIZE_BITS      32
#define HEADER_MAGIC          0x18
#else /* BIT_ALIGN32 */
#define HEADER_GRANULES       1
#define HEADER_TYPE_BITS      8
#define HEADER_MARKBIT_BITS   1
#define HEADER_EXTRA_BITS     3
#define HEADER_GEN_BITS       4
#define HEADER_MAGIC_BITS     16
#define HEADER_SIZE_BITS      32
#define HEADER_MAGIC          0x18
#endif /* BIT_ALIGN32 */
typedef struct header_t {
  cell_type_t  type:    HEADER_TYPE_BITS;
  unsigned int markbit: HEADER_MARKBIT_BITS;
  unsigned int extra:   HEADER_EXTRA_BITS;
  unsigned int magic:   HEADER_MAGIC_BITS;
  unsigned int gen:     HEADER_GEN_BITS;
  unsigned int size:    HEADER_SIZE_BITS;
} header_t;

static inline header_t compose_header(size_t granules, size_t extra,
                                      cell_type_t type);

/*
 *  Types
 */

#define CELLT_FREE          (0xff)

struct free_chunk {
  header_t header;
  struct free_chunk *next;
};

struct space {
  uintptr_t addr;
  size_t bytes;
  size_t free_bytes;
  struct free_chunk* freelist;
  char *name;
};

extern struct space js_space;

static inline void *header_to_payload(header_t *hdrp);
static inline header_t *payload_to_header(void *ptr);

static inline void mark_cell_header(header_t *hdrp);
static inline void unmark_cell_header(header_t *hdrp);
static inline int is_marked_cell_header(header_t *hdrp);

/* GC private functions */
static inline void mark_cell(void *p);
static inline int is_marked_cell(void *p);
static inline  int test_and_mark_cell(void *p);
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
header_t *get_shadow(void *ptr);
#endif /* GC_DEBUG */

#endif /* FREELIST_SPACE_H */
