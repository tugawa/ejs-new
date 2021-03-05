#ifndef FREELIST_SPACE_H
#define FREELIST_SPACE_H

#ifdef __cplusplus
extern "C" {
#endif

#ifdef FLONUM_SPACE
#error freelist space does not support FLONUM_SPACE.
#endif /* FLONUM_SPACE */

#ifdef EXCESSIVE_GC
#define DEFAULT_GC_THRESHOLD(heap_limit) ((heap_limit) - ((heap_limit) >> 4))
#else  /* EXCESSIVE_GC */
#define DEFAULT_GC_THRESHOLD(heap_limit) ((heap_limit) >> 4)
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
#define HEADER_GRANULES        2
#define HEADER_IDENTIFIER_BITS 1
#define HEADER_TYPE_BITS       LOG_MAX_NUM_CELL_TYPES
#define HEADER_MARKBIT_BITS    1
#define HEADER_GEN_BITS        7
#define HEADER_MAGIC_BITS      15
#define HEADER_SIZE_BITS       32
#define HEADER_SIZE_BITS_LO    16
#define HEADER_SIZE_BITS_HI    16
#define HEADER_MAGIC           0x18
#else /* BIT_ALIGN32 */
#define HEADER_GRANULES        1
#define HEADER_IDENTIFIER_BITS 1
#define HEADER_TYPE_BITS       LOG_MAX_NUM_CELL_TYPES
#define HEADER_MARKBIT_BITS    1
#define HEADER_GEN_BITS        7
#define HEADER_MAGIC_BITS      15
#define HEADER_SIZE_BITS       32
#define HEADER_SIZE_BITS_LO    16
#define HEADER_SIZE_BITS_HI    16
#define HEADER_MAGIC           0x18
#endif /* BIT_ALIGN32 */
typedef struct header_t {
  union {
    uintptr_t threaded;
    struct {
      unsigned int identifier: HEADER_IDENTIFIER_BITS;
      cell_type_t  type:       HEADER_TYPE_BITS;
      unsigned int markbit:    HEADER_MARKBIT_BITS;
      unsigned int magic:      HEADER_MAGIC_BITS;
      unsigned int gen:        HEADER_GEN_BITS;
#ifdef GC_THREADED_BOUNDARY_TAG
      union {
	unsigned int size:     HEADER_SIZE_BITS;
	struct {
#define BOUNDARY_TAG_MAX_SIZE ((1 << HEADER_SIZE_BITS_LO) - 1)
	  unsigned int size_lo: HEADER_SIZE_BITS_LO;
	  unsigned int size_hi: HEADER_SIZE_BITS_HI;
	};
      };
#else /* GC_THREADED_BOUNDARY_TAG */
      unsigned int size:       HEADER_SIZE_BITS;
#endif /* GC_THREADED_BOUNDARY_TAG */
    };
  };
} header_t;

static inline header_t compose_header(size_t granules, cell_type_t type);
#ifdef GC_THREADED_BOUNDARY_TAG
static inline header_t
compose_hidden_class_header(size_t granules, cell_type_t type);
#endif /* GC_THREADED_BOUNDARY_TAG */

#ifdef GC_THREADED_NO_HCGC
#define BOUNDARY_TAG_GRANULES 0
#elif defined(GC_THREADED_BOUNDARY_TAG)
#define BOUNDARY_TAG_GRANULES 0
#else /*  GC_THREADED_BOUNDARY_TAG */
#define BOUNDARY_TAG_GRANULES 1
#endif /* GC_THREADED_BOUNDARY_TAG */

/*
 *  Types
 */

/*
 * A) without GC_THREADED_SPEARATE_HC_AREA
 * |----------->       <-------------|
 * head      begin    end          tail
 *
 * B) with GC_THREADED_SPEARATE_HC_AREA
 * |----------->    :    |   <-------------|
 * head      begin  :    |  end          tail
 *                  :  ordinary_limit
 *                  :      <--------------->
 *                  :   GC_THREADED_HC_AREA_BYTES
 *             threshold
 */

struct space {
  uintptr_t head;
  uintptr_t tail;
  uintptr_t begin;
  uintptr_t end;
#ifdef GC_THREADED_SEPARATE_HC_AREA
#define GC_THREADED_HC_AREA_BYTES (1024*1024)
  uintptr_t ordinary_limit;
#endif /* GC_THREADED_SEPARATE_HC_AREA */
  size_t bytes;
  size_t free_bytes;
#ifdef GC_THREADED_SEPARATE_HC_AREA
  size_t threshold;
#else /* GC_THREADED_SEPARATE_HC_AREA */
  size_t threshold_bytes;
#endif /* GC_THREADED_SEPARATE_HC_AREA */
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
extern void space_init(size_t bytes, size_t threshold_bytes);
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

#ifdef __cplusplus
}
#endif

#endif /* FREELIST_SPACE_H */
