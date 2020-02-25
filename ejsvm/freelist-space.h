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
                                      cell_type_t type)
{
  header_t hdr;
  hdr.type = type;
  hdr.markbit = 0;
  hdr.extra = extra;
  hdr.magic = HEADER_MAGIC;
#ifdef GC_DEBUG
  hdr.gen = generation;
#else /* GC_DEBUG */
  hdr.gen = 0;
#endif /* GC_DEBUG */
  hdr.size  = granules;
  return hdr;
}

static inline void *header_to_payload(header_t *hdrp)
{
  return (void *) (hdrp + 1);
}

static inline header_t *payload_to_header(void *ptr)
{
  return ((header_t *) ptr) - 1;
}

/* space interface */
static inline cell_type_t space_get_cell_type(uintptr_t ptr)
{
  return payload_to_header((void *) ptr)->type;
}

/* GC interface */
static inline cell_type_t gc_obj_header_type(void *p)
{
  return space_get_cell_type((uintptr_t) p);
}
