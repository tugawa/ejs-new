#include <stdlib.h>

/*
 * naming convention
 *   name for size: add a surfix representing the unit
 *                    bytes: in bytes
 *                    jsvalues: in the numberof JSValue's
 */

#define JS_SPACE_BYTES     (1024 * 1024)
#define MALLOC_SPACE_BYTES (1024 * 1024)

#define LOG_BYTES_IN_JSVALUE   3
#define BYTES_IN_JSVALUE       (1 << LOG_BYTES_IN_JSVALUE)

/*
 * If the remaining room is smaller than a certain size,
 * we do not use the remainder for efficiency.  Rather,
 * we add it below the chunk being allocated.  In this case,
 * the size in the header includes the extra words.
 */
#define MINIMUM_FREE_CHUNK_JSVALUES 4

#define MKMASK(l, o, b) \
  ((((uint64_t) -1) << ((l) - (b))) >> ((l) - (o) - (b)))

/*
 * Chunk header layout
 *  HEADER0
 *    bit 0 - 7  :  type (HTAG_xxx)
 *                    4 - 14 : JSValue types
 *                    15     : malloced memory (HTAG_MALLOC)
 *                    16     : free (HTAG_FREE)
 *    bit 8      : mark bit
 *    bit 32 - 63: size (in number of JSValue's)
 */
#define HEADER_JSVALUES       1
#define HEADER_BYTES (HEADER_JSVALUES << LOG_BYTES_IN_JSVALUE)

#define HEADER0_BITS          64
#define HEADER0_TYPE_OFFSET   0
#define HEADER0_TYPE_BITS     8
#define HEADER0_GC_OFFSET     8
#define HEADER0_GC_BITS       1
#define HEADER0_SIZE_OFFSET   32
#define HEADER0_SIZE_BITS     32
#define HEADER0_TYPE_MASK \
  MKMASK(HEADER0_BITS, HEADER0_TYPE_OFFSET, HEADER0_TYPE_BITS)
#define HEADER0_GC_MASK \
  MKMASK(HEADER0_BITS, HEADER0_GC_OFFSET, HEADER0_GC_BITS)
#define HEADER0_SIZE_MASK					\
  MKMASK(HEADER0_BITS, HEADER0_SIZE_OFFSET, HEADER0_SIZE_BITS)
//#define HEADER0_SIZE_MASK 0xffffffff00000000LLU

/* accessor to HEADER0 */
#define HEADER0_SET(hdr, val, off, msk)			\
  ((hdr) = ((((uint64_t) (val)) << (off)) | ((hdr) & ~(msk))))
#define HEADER0_GET(hdr, off, msk) \
  (((uint64_t) ((hdr) & (msk))) >> (off))
#define HEADER0_SET_TYPE(hdr, val) \
  HEADER0_SET(hdr, val, HEADER0_TYPE_OFFSET, HEADER0_TYPE_MASK)
#define HEADER0_GET_TYPE(hdr) \
  HEADER0_GET(hdr, HEADER0_TYPE_OFFSET, HEADER0_TYPE_MASK)
#define HEADER0_SET_GC(hdr, val) \
  HEADER0_SET(hdr, val, HEADER0_GC_OFFSET, HEADER0_GC_MASK)
#define HEADER0_GET_GC(hdr) \
  HEADER0_GET(hdr, HEADER0_GC_OFFSET, HEADER0_GC_MASK)
#define HEADER0_SET_SIZE(hdr, val) \
  HEADER0_SET(hdr, val, HEADER0_SIZE_OFFSET, HEADER0_SIZE_MASK)
#define HEADER0_GET_SIZE(hdr) \
  HEADER0_GET(hdr, HEADER0_SIZE_OFFSET, HEADER0_SIZE_MASK)
#define HEADER0_COMPOSE(size, type) \
  ((((uint64_t)(size)) << HEADER0_SIZE_OFFSET) | \
   ((uint64_t)(type)) << HEADER0_TYPE_OFFSET)

/* 
 * header tag
 */
#define HTAG_MALLOC        (0x0f)
#define HTAG_FREE          (0x10)

struct free_chunk {
  uint64_t header;
  struct free_chunk *next;
};

/*
 *  Space
 */

struct space {
  void* addr;
  uintptr_t bytes;
  struct free_chunk* freelist;
};

struct space js_space;
struct space malloc_space;

void create_space(struct space *space, uintptr_t bytes)
{
  struct free_chunk *p;
  p = (struct free_chunk *) malloc(bytes);
  p->header = HEADER0_COMPOSE(bytes >> LOG_BYTES_IN_JSVALUE, HTAG_FREE);
  p->next = NULL;
  space->addr = p;
  space->bytes = bytes;
  space->freelist = p;
}

void memory_init()
{
  create_space(&js_space, JS_SPACE_BYTES);
  create_space(&malloc_space, MALLOC_SPACE_BYTES);
}

/*
 * Returns a pointer to the first address of the memory area
 * available to the VM.  The header precedes the area.
 * The header has the size of the chunk including the header,
 * the area available to the VM, and extra bytes if any.
 * Other header bits are zero
 */
void* gc_malloc(uintptr_t request_bytes)
{
  uint32_t  alloc_jsvalues;
  struct free_chunk **p;
  
  alloc_jsvalues = (request_bytes + BYTES_IN_JSVALUE) >> LOG_BYTES_IN_JSVALUE;
  alloc_jsvalues += HEADER_JSVALUES;

  /* allocate from freelist */
  for (p = &malloc_space.freelist; *p != NULL; p = &(*p)->next) {
    struct free_chunk *chunk = *p;
    uint32_t chunk_jsvalues = HEADER0_GET_SIZE(chunk->header);
    if (chunk_jsvalues >= alloc_jsvalues) {
      if (chunk_jsvalues >= alloc_jsvalues + MINIMUM_FREE_CHUNK_JSVALUES) {
	/* This chunk is large enough to leave a part unused.  Split it */
	uint32_t new_chunk_jsvalues = chunk_jsvalues - alloc_jsvalues;
	uintptr_t addr =
	  ((uintptr_t) chunk) + (new_chunk_jsvalues << LOG_BYTES_IN_JSVALUE);
	HEADER0_SET_SIZE(chunk->header, new_chunk_jsvalues);
	*(uint64_t *) addr = HEADER0_COMPOSE(alloc_jsvalues, 0);
	return (void *) (addr + HEADER_BYTES);
      } else {
	/* This chunk is too small to split. */
	*p = (*p)->next;
	chunk->header = HEADER0_COMPOSE(chunk_jsvalues, 0);
	return (void *) (((uintptr_t) chunk) + HEADER_BYTES);
      }
    }
  }

  return NULL;
}

/*
 * request_jsvalues: the number of JSValue's including the object header.
 */
void* gc_jsalloc(uint32_t request_jsvalues, uint32_t type)
{
  struct free_chunk **p;

  for (p = &js_space.freelist; *p != NULL; p = &(*p)->next) {
    struct free_chunk *chunk = *p;
    uint32_t chunk_jsvalues = HEADER0_GET_SIZE(chunk->header);
    if (chunk_jsvalues >= request_jsvalues) {
      if (chunk_jsvalues >= request_jsvalues + MINIMUM_FREE_CHUNK_JSVALUES) {
	/* This chunk is large enough to leave a part unused.  Split it */
	uint32_t new_chunk_jsvalues = chunk_jsvalues - request_jsvalues;
	uintptr_t addr =
	  ((uintptr_t) chunk) + (new_chunk_jsvalues << LOG_BYTES_IN_JSVALUE);
	HEADER0_SET_SIZE(chunk->header, new_chunk_jsvalues);
	*(uint64_t *) addr = HEADER0_COMPOSE(request_jsvalues, type);
	return (void *) addr;
      } else {
	/* This chunk is too small to split. */
	*p = (*p)->next;
	chunk->header &= HEADER0_COMPOSE(chunk_jsvalues, type);
	return (void *) chunk;
      }
    }
  }

  return NULL;
}

