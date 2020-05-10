typedef struct header_t {
  size_t size: 22;
  cell_type_t type: 8;
  int forwarded: 1;
} header_t;

#define HEADER_GRANULES 1

struct copy_space {
  uintptr_t ss0, ss1;
  size_t bytes;
  size_t total_bytes;
  uintptr_t from, end, to, free, scan;
};

extern struct copy_space space;

extern void space_init(size_t bytes);
extern void *space_alloc(uintptr_t request_bytes, cell_type_t type);
static inline header_t *payload_to_header(uintptr_t ptr) {
  return ((header_t *) ptr) - 1;
}
static inline void *header_to_payload(header_t *hdrp) {
  return hdrp + 1;
}
static inline int space_check_gc_request() {
  return space.end - space.free < 20 * 1024;
}
static inline cell_type_t space_get_cell_type(uintptr_t ptr) {
  header_t *hdrp = payload_to_header(ptr);
  return hdrp->type;
}

#ifdef GC_DEBUG
extern void space_print_memory_status(void);
#endif /* GC_DEBUG */
