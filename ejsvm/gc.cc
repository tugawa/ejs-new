/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include <stdlib.h>
#include <stdio.h>
#include "prefix.h"
#define EXTERN extern
#include "header.h"
#include "log.h"

/* Objects allocated in the heap
 *                       has   stored as  visible   know
 *                      (ptag) (JSValue) (to user) (size) (type)
 *   CELLT_STRING         yes    yes       yes       fixed StringCell
 *   CELLT_FLONUM         yes    yes       yes       fixed FlonumCell
 *   CELLT_SIMPLE_OBJECT  yes    yes       yes       yes   JSObject
 *   CELLT_ARRAY          yes    yes       yes       yes   JSObject
 *   CELLT_FUNCTION       yes    yes       yes       yes   JSObject
 *   CELLT_BUILTIN        yes    yes       yes       yes   JSObject
 *   CELLT_BOXED_NUMBER   yes    yes       yes       yes   JSObject
 *   CELLT_BOXED_BOOLEAN  yes    yes       yes       yes   JSObject
 *   CELLT_BOXED_STRING   yes    yes       yes       yes   JSObject
 *   CELLT_REGEXP         yes    yes       yes       yes   JSObject
 *   CELLT_ITERATOR       yes    yes       no        yes   Iterator
 *   CELLT_PROP           no     yes       no        no    JSValue*
 *   CELLT_ARRAY_DATA     no     no        no        no    JSValue*
 *   CELLT_BYTE_ARRAY     no     no        no        no    non pointer
 *   CELLT_FUNCTION_FRAME no     no        no        yes   FunctionFrame
 *   CELLT_STR_CONS       no     no        no        fixed StrCons
 *   CELLT_CONTEXT        no     no        no        fixed Context
 *   CELLT_STACK          no     no        no        no    JSValue*
 *   CELLT_HASHTABLE      no     no        no        fixed HashTable
 *   CELLT_HASH_BODY      no     no        no        no    HashCell**
 *   CELLT_HASH_CELL      no     no        no        fixed HashCell
 *   CELLT_PROPERTY_MAP   no     yes       no        fixed PropertyMap
 *   CELLT_SHAPE          no     no        no        fixed Shape
 *   CELLT_UNWIND         no     no        no        fixed UnwindProtect
 *   CELLT_PROPERTY_MAP_LIST no  no        no        fixed PropertyMapList
 *
 * Objects that do not know their size (PROP, ARRAY_DATA, STACK, HASH_BODY)
 * are stored in a dedicated slot and scand together with their owners.
 *
 * CELLT_PROP is stored in the last embedded slot.
 * CELLT_PROPERTY_MAP is stored as the value of property __property_map__
 * of a prototype object.
 *
 * Static data structures
 *   FunctionTable[] function_table (global.h)
 *   StrTable string_table (global.h)
 */

#if 0
#define GCLOG(...) LOG(__VA_ARGS__)
#define GCLOG_TRIGGER(...) LOG(__VA_ARGS__)
#define GCLOG_ALLOC(...) LOG(__VA_ARGS__)
#define GCLOG_SWEEP(...) LOG(__VA_ARGS__)
#else /* 0 */
#define GCLOG(...)
#define GCLOG_TRIGGER(...)
#define GCLOG_ALLOC(...)
#define GCLOG_SWEEP(...)
#endif /* 0 */

/* gc root stack */
#define MAX_ROOTS 1000
JSValue *gc_root_stack[MAX_ROOTS];
int gc_root_stack_ptr = 0;

STATIC int gc_disabled = 1;

int generation = 0;
int gc_sec;
int gc_usec;
#ifdef GC_PROF
uint64_t total_alloc_bytes;
uint64_t total_alloc_count;
uint64_t pertype_alloc_bytes[256];
uint64_t pertype_alloc_count[256];
uint64_t pertype_live_bytes[256];
uint64_t pertype_live_count[256];

const char *cell_type_name[NUM_DEFINED_CELL_TYPES + 1] = {
    /* 00 */ "free",
    /* 01 */ "",
    /* 02 */ "",
    /* 03 */ "",
    /* 04 */ "STRING",
    /* 05 */ "FLONUM",
    /* 06 */ "SIMPLE_OBJECT",
    /* 07 */ "ARRAY",
    /* 08 */ "FUNCTION",
    /* 09 */ "BUILTIN",
    /* 0A */ "ITERATOR",
    /* 0B */ "REGEXP",
    /* 0C */ "BOXED_STRING",
    /* 0D */ "BOXED_NUMBER",
    /* 0E */ "BOXED_BOOLEAN",
    /* 0F */ "",
    /* 10 */ "",
    /* 11 */ "PROP",
    /* 12 */ "ARRAY_DATA",
    /* 13 */ "BYTE_ARRAY",
    /* 14 */ "FUNCTION_FRAME",
    /* 15 */ "STR_CONS",
    /* 16 */ "CONTEXT",
    /* 17 */ "STACK",
    /* 18 */ "" /* was HIDDEN_CLASS */,
    /* 19 */ "HASHTABLE",
    /* 1a */ "HASH_BODY",
    /* 1b */ "HASH_CELL",
    /* 1c */ "PROPERTY_MAP",
    /* 1d */ "SHAPE",
    /* 1e */ "UNWIND",
    /* 1f */ "PROPERTY_MAP_LIST",
};
#endif /* GC_PROF */

/*
 * prototype
 */
/* GC */
STATIC_INLINE int check_gc_request(Context *, int);
extern void garbage_collection(Context *ctx);
void start_garbage_collection(Context *ctx);

void init_memory(size_t bytes)
{
  space_init(bytes);
  gc_root_stack_ptr = 0;
  gc_disabled = 1;
  generation = 1;
  gc_sec = 0;
  gc_usec = 0;
}

void* gc_malloc(Context *ctx, uintptr_t request_bytes, cell_type_t type)
{
  void *addr;
#ifdef DEBUG
  static int count;
  count++;
#endif /* DEBUG */
  
  if (check_gc_request(ctx, 0))
    start_garbage_collection(ctx);
  addr = space_alloc(request_bytes, type);
  GCLOG_ALLOC("gc_malloc: req %x bytes type %d => %p\n",
              request_bytes, type, addr);
  if (addr == NULL) {
    if (check_gc_request(ctx, 1)) {
#ifdef GC_DEBUG
      printf("emergency GC\n");
#endif /* GC_DEBUG */
      start_garbage_collection(ctx);
      addr = space_alloc(request_bytes, type);
    }
    if (addr == NULL) {
      printf("Out of memory\n");
#ifdef GC_DEBUG
      printf("#GC = %d\n", generation);
      space_print_memory_status();
#endif /* GC_DEBUG */
      abort();
    }
  }
#ifdef GC_PROF
  if (addr != NULL) {
    size_t bytes = request_bytes;
    total_alloc_bytes += bytes;
    total_alloc_count++;
    pertype_alloc_bytes[type] += bytes;
    pertype_alloc_count[type]++;
  }
#endif /* GC_PROF */
  return addr;
}

#ifdef FLONUM_SPACE
FlonumCell *gc_try_alloc_flonum(double x)
{
  return space_try_alloc_flonum(x);
}
#endif /* FLONUM_SPACE */

void start_garbage_collection(Context *ctx)
{
  struct rusage ru0, ru1;

  if (cputime_flag == TRUE)
    getrusage(RUSAGE_SELF, &ru0);

  garbage_collection(ctx);

  if (cputime_flag == TRUE) {
    time_t sec;
    suseconds_t usec;

    getrusage(RUSAGE_SELF, &ru1);
    sec = ru1.ru_utime.tv_sec - ru0.ru_utime.tv_sec;
    usec = ru1.ru_utime.tv_usec - ru0.ru_utime.tv_usec;
    if (usec < 0) {
      sec--;
      usec += 1000000;
    }
    gc_sec += sec;
    gc_usec += usec;
  }

  generation++;
  /*  printf("Exit gc, generation = %d\n", generation); */
}


void disable_gc(void)
{
  gc_disabled++;
}

void enable_gc(Context *ctx)
{
  if (--gc_disabled == 0) {
    if (check_gc_request(ctx, 0))
      start_garbage_collection(ctx);
  }
}

void try_gc(Context *ctx)
{
  if (check_gc_request(ctx, 0))
    start_garbage_collection(ctx);
}

STATIC_INLINE int check_gc_request(Context *ctx, int force)
{
  if (force || space_check_gc_request()) {
    if (ctx == NULL) {
      GCLOG_TRIGGER("Needed gc for js_space -- cancelled: ctx == NULL\n");
      return 0;
    }
    if (gc_disabled) {
      GCLOG_TRIGGER("Needed gc for js_space -- cancelled: GC disabled\n");
      return 0;
    }
    return 1;
  }
  GCLOG_TRIGGER("no GC needed (%d bytes free)\n", js_space.free_bytes);
  return 0;
}

#ifdef ALLOC_SITE_CACHE
STATIC PropertyMap *find_lub(PropertyMap *a, PropertyMap *b)
{
  while(!GC_PM_EQ(a, b) && a->prev != NULL) {
    /* If both a->n_props and b->n_props are 0, rewind `a', so that we can
     * do NULL check only for `a'.
     */
    if (a->n_props < b->n_props)
      b = b->prev;
    else
      a = a->prev;
  }
  return a;
}

void alloc_site_update_info(JSObject *p)
{
  AllocSite *as = p->alloc_site;
  PropertyMap *pm = p->shape->pm;

  assert(as != NULL);

  /* likely case */
  if (GC_PM_EQ(pm, as->pm))
    return;

  if (as->pm == NULL) {
    /* 1. If the site is empty, cache this. */
    as->pm = pm;
    assert(as->shape == NULL);
  } else {
    /* 2. Otherwise, compute LUB.
     *
     *   LUB       monomorphic   polymorphic
     *   pm        mono:as->pm   poly:as->pm
     *   as->pm    mono:pm       poly:as->pm
     *   less      poly:LUB      poly:LUB
     */
    PropertyMap *lub = find_lub(pm, as->pm);
    if (GC_PM_EQ(lub, as->pm)) {
      if (as->polymorphic)
        /* keep current as->pm */ ;
      else {
        as->pm = pm;
        as->shape = NULL;
      }
    } else if (GC_PM_EQ(lub, pm))
      /* keep current as->pm */  ;
    else {
      as->polymorphic = 1;
      as->pm = lub;
      as->shape = NULL;
    }
  }
}
#endif /* ALLOC_SITE_CACHE */

#ifdef GC_DEBUG
STATIC void print_memory_status(void)
{
  GCLOG("gc_disabled = %d\n", gc_disabled);
  space_print_memory_status();
}
#endif /* GC_DEBUG */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
