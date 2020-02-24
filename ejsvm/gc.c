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

#ifndef NDEBUG
#define GC_DEBUG 1
#define STATIC        /* make symbols global for debugger */
#define STATIC_INLINE /* make symbols global for debugger */
#else
#undef GC_DEBUG
#define STATIC static
#define STATIC_INLINE static inline
#endif

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

#ifdef BIBOP
#include "bibop-space.c"
#else /* BIBOP */
#include "freelist-space.c"
#endif /* BIBOP */

/*
 * Constants
 */

enum gc_phase {
  PHASE_INACTIVE,
  PHASE_INITIALISE,
  PHASE_MARK,
  PHASE_WEAK,
  PHASE_SWEEP,
  PHASE_FINALISE,
};

/*
 * Variables
 */
static enum gc_phase gc_phase = PHASE_INACTIVE;
static Context *the_context;

/* gc root stack */
#define MAX_ROOTS 1000
JSValue *gc_root_stack[MAX_ROOTS];
int gc_root_stack_ptr = 0;

STATIC int gc_disabled = 1;

#ifdef MARK_STACK
#define MARK_STACK_SIZE 1000 * 1000
static uintptr_t mark_stack[MARK_STACK_SIZE];
static int mark_stack_ptr;
#endif /* MARK_STACK */

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
    /* 13 */ "FUNCTION_FRAME",
    /* 14 */ "STR_CONS",
    /* 15 */ "CONTEXT",
    /* 16 */ "STACK",
    /* 17 */ "HIDDEN_CLASS",
    /* 18 */ "HASHTABLE",
    /* 19 */ "HASH_BODY",
    /* 1a */ "HASH_CELL",
    /* 1b */ "PROPERTY_MAP",
    /* 1c */ "SHAPE",
    /* 1d */ "UNWIND",
    /* 1e */ "PROPERTY_MAP_LIST",
};
#endif /* GC_PROF */

/*
 * prototype
 */
/* GC */
STATIC_INLINE int check_gc_request(Context *);
STATIC void garbage_collection(Context *ctx);
STATIC void scan_roots(Context *ctx);
STATIC void weak_clear_StrTable(StrTable *table);
STATIC void weak_clear(void);
#ifdef MARK_STACK
STATIC_INLINE void process_node(uintptr_t ptr);
#endif /* MARK_STACK */
#ifdef ALLOC_SITE_CACHE
STATIC void alloc_site_update_info(JSObject *p);
#endif /* ALLOC_SITE_CACHE */

void init_memory(size_t bytes)
{
  space_init(bytes);
  gc_root_stack_ptr = 0;
  gc_disabled = 0;
  generation = 1;
  gc_sec = 0;
  gc_usec = 0;
}

void* gc_malloc(Context *ctx, uintptr_t request_bytes, uint32_t type)
{
  void *addr;

  if (check_gc_request(ctx))
    garbage_collection(ctx);
  addr = space_alloc(request_bytes, type);
  GCLOG_ALLOC("gc_malloc: req %x bytes type %d => %p\n",
              request_bytes, type, addr);
  if (addr == NULL)
    abort();
  return addr;
}

void disable_gc(void)
{
  gc_disabled++;
}

void enable_gc(Context *ctx)
{
  if (--gc_disabled == 0) {
    if (check_gc_request(ctx))
      garbage_collection(ctx);
  }
}

void try_gc(Context *ctx)
{
  if (check_gc_request(ctx))
    garbage_collection(ctx);
}


/*
 * GC
 */


#ifdef MARK_STACK
STATIC_INLINE void mark_stack_push(uintptr_t ptr)
{
  assert(mark_stack_ptr < MARK_STACK_SIZE);
  mark_stack[mark_stack_ptr++] = ptr;
}

STATIC_INLINE uintptr_t mark_stack_pop()
{
  return mark_stack[--mark_stack_ptr];
}

STATIC_INLINE int mark_stack_is_empty()
{
  return mark_stack_ptr == 0;
}

STATIC void process_mark_stack()
{
  while (!mark_stack_is_empty()) {
    uintptr_t ptr = mark_stack_pop();
    process_node(ptr);
  }
}
#endif /* MARK_STACK */

STATIC_INLINE int check_gc_request(Context *ctx)
{
  if (space_check_gc_request()) {
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

STATIC void garbage_collection(Context *ctx)
{
  struct rusage ru0, ru1;

  /* initialise */
  gc_phase = PHASE_INITIALISE;
  GCLOG("Before Garbage Collection\n");
  if (cputime_flag == TRUE)
    getrusage(RUSAGE_SELF, &ru0);
  the_context = ctx;

  /* mark */
  gc_phase = PHASE_MARK;
  scan_roots(ctx);

#ifdef MARK_STACK
  process_mark_stack();
#endif /* MARK_STACK */

  /* profile */
#ifdef CHECK_MATURED
  check_matured();
#endif /* CHECK_MATURED */

  /* weak */
  gc_phase = PHASE_WEAK;
  weak_clear();

  /* sweep */
  gc_phase = PHASE_SWEEP;
  sweep();

  /* finalise */
  gc_phase = PHASE_FINALISE;
  GCLOG("After Garbage Collection\n");
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
  /* printf("Exit gc, generation = %d\n", generation); */

  gc_phase = PHASE_INACTIVE;
}

/*
 * Tracer
 *
 *  process_edge, process_edge_XXX
 *    If the destination node is not marked, mark it and process the
 *    destination node. XXX is specialised version for type XXX.
 *  scan_XXX
 *    Scan static structure XXX.
 *  process_node_XXX
 *    Scan object of type XXX in the heap.  Move it if nencessary.
 */

STATIC void process_edge(uintptr_t ptr);
STATIC void process_edge_JSValue_array(JSValue *p, size_t start, size_t length);
STATIC void process_edge_HashBody(HashCell **p, size_t length);
STATIC void process_node_FunctionFrame(FunctionFrame *p);
STATIC void process_node_Context(Context *p);
STATIC void scan_function_table_entry(FunctionTable *p);
STATIC void scan_stack(JSValue* stack, int sp, int fp);

STATIC_INLINE void process_node(uintptr_t ptr)
{
  /* part of code for processing the node is inlined */
  switch (space_get_cell_type(ptr)) {
  case CELLT_STRING:
  case CELLT_FLONUM:
    return;
  case CELLT_SIMPLE_OBJECT:
    break;
  case CELLT_ARRAY:
    {
      JSObject *p = (JSObject *) ptr;
      JSValue *a_body = get_array_ptr_body(p);
      if (a_body != NULL) {
        /* a_body may be NULL during initialization */
        uint64_t a_size = get_array_ptr_size(p);
        uint64_t a_length =
          (uint64_t) number_to_double(get_array_ptr_length(p));
        size_t len = a_length < a_size ? a_length : a_size;
        process_edge(get_array_ptr_length(p));
        process_edge_JSValue_array(a_body, 0, len);
      }
      break;
    }
  case CELLT_FUNCTION:
    {
      JSObject *p = (JSObject *) ptr;
      FunctionFrame *frame = get_function_ptr_environment(p);
      /* FunctionTable *ftentry = function_ptr_table_entry(p);
       * scan_function_table_entry(ftentry);
       *    All function table entries are scanned through Context
       */
      process_edge((uintptr_t) frame);
      break;
    }
  case CELLT_BUILTIN:
    break;
  case CELLT_BOXED_NUMBER:
    {
      JSObject *p = (JSObject *) ptr;
      JSValue value = get_number_object_ptr_value(p);
      process_edge((uintptr_t) value);
      break;
    }
  case CELLT_BOXED_STRING:
    {
      JSObject *p = (JSObject *) ptr;
      JSValue value = get_string_object_ptr_value(p);
      process_edge((uintptr_t) value);
      break;
    }
  case CELLT_BOXED_BOOLEAN:
    {
#ifdef DEBUG
      JSObject *p = (JSObject *) ptr;
      JSValue value = get_number_object_ptr_value(p);
      assert(is_boolean(value));
#endif /* DEBUG */
      break;
    }
#ifdef USE_REGEXP
  case CELLT_REGEXP:
    break;
#endif /* USE_REGEXP */
  case CELLT_ITERATOR:
    {
      Iterator *p = (Iterator *) ptr;
      if (p->size > 0)
        process_edge_JSValue_array(p->body, 0, p->size);
      return;
    }
  case CELLT_PROP:
  case CELLT_ARRAY_DATA:
    abort();
  case CELLT_FUNCTION_FRAME:
    process_node_FunctionFrame((FunctionFrame *) ptr);
    return;
  case CELLT_STR_CONS:
    {
      StrCons *p = (StrCons *) ptr;
      /* WEAK: p->str */
      if (p->next != NULL)
        process_edge((uintptr_t) p->next); /* StrCons */
      return;
    }
  case CELLT_CONTEXT:
    process_node_Context((Context *) ptr);
    return;
  case CELLT_STACK:
    abort();
  case CELLT_HASHTABLE:
    {
      HashTable *p = (HashTable *) ptr;
      if (p->body != NULL)
        process_edge_HashBody(p->body, p->size);
      return;
    }
  case CELLT_HASH_BODY:
    abort();
  case CELLT_HASH_CELL:
    {
      HashCell *p = (HashCell *) ptr;
      process_edge(p->entry.key);
#ifndef HC_SKIP_INTERNAL
      /* transition link is weak if HC_SKIP_INTERNAL */
      if (is_transition(p->entry.attr))
        process_edge((uintptr_t) p->entry.data.u.pm);  /* PropertyMap */
#endif /* HC_SKIP_INTERNAL */
      if (p->next != NULL)
        process_edge((uintptr_t) p->next);  /* HashCell */
      return;
    }
  case CELLT_PROPERTY_MAP:
    {
      PropertyMap *p = (PropertyMap *) ptr;
      process_edge((uintptr_t) p->map); /* HashTable */
#ifndef HC_SKIP_INTERNAL
      if (p->prev != NULL)
        /* weak if HC_SKIP_INTERNAL */
        process_edge((uintptr_t) p->prev); /* PropertyMap */
#endif /* HC_SKIP_INTERNAL */
      if (p->shapes != NULL)
        process_edge((uintptr_t) p->shapes); /* Shape
                                              * (always keep the largest one) */
      process_edge((uintptr_t) p->__proto__);
      return;
    }
  case CELLT_SHAPE:
    {
      Shape *p = (Shape *) ptr;
      process_edge((uintptr_t) p->pm);
#ifndef NO_SHAPE_CACHE
#ifdef WEAK_SHAPE_LIST
      /* p->next is weak */
#else /* WEAK_SHAPE_LIST */
      if (p->next != NULL)
        process_edge((uintptr_t) p->next);
#endif /* WEAK_SHAPE_LIST */
#endif /* NO_SHAPE_CACHE */
      return;
    }
  case CELLT_UNWIND:
    {
      UnwindProtect *p = (UnwindProtect *) ptr;
      process_edge((uintptr_t) p->prev);
      process_edge((uintptr_t) p->lp);
    }
#if defined(HC_SKIP_INTERNAL) || defined(WEAK_SHAPE_LIST)
  case CELLT_PROPERTY_MAP_LIST:
    abort();
    break;
#endif /* HC_SKIP_INTERNAL || WEAK_SHAPE_LIST */
  default:
    abort();
  }

  /* common fields and payload of JSObject */
  {
    JSObject *p = (JSObject *) ptr;
    Shape *os = p->shape;
    int n_extension = os->n_extension_slots;
    size_t actual_embedded = os->n_embedded_slots - (n_extension == 0 ? 0 : 1);
    int i;
    /* 1. shape */
    process_edge((uintptr_t) os);
    /* 2. embedded propertyes */
    for (i = os->pm->n_special_props; i < actual_embedded; i++)
      process_edge(p->eprop[i]);
    if (n_extension != 0) {
      /* 3. extension */
      JSValue *extension = jsv_to_extension_prop(p->eprop[actual_embedded]);
      process_edge_JSValue_array(extension, 0,
                                 os->pm->n_props - actual_embedded);
    }
#ifdef ALLOC_SITE_CACHE
    /* 4. allocation site cache */
    if (p->alloc_site != NULL)
      alloc_site_update_info(p);
#endif /* ALLOC_SITE_CACHE */
  }
}

STATIC void process_edge(uintptr_t ptr)
{
  if (is_fixnum(ptr) || is_special(ptr))
    return;

  ptr = ptr & ~TAGMASK;
  if (in_js_space((void *) ptr) && test_and_mark_cell((void *) ptr))
    return;

#ifdef MARK_STACK
  mark_stack_push(ptr);
#else /* MARK_STACK */
  process_node(ptr);
#endif /* MARK_STACK */
}

STATIC void process_edge_JSValue_array(JSValue *p, size_t start, size_t length)
{
  size_t i;
  assert(in_js_space(p));
  if (test_and_mark_cell(p))
    return;
  for (i = start; i < length; i++)
    process_edge((uintptr_t) p[i]);
}

STATIC void process_edge_HashBody(HashCell **p, size_t length)
{
  size_t i;
  assert(in_js_space(p));
  if (test_and_mark_cell(p))
    return;
  for (i = 0; i < length; i++)
    if (p[i] != NULL)
      process_edge((uintptr_t) p[i]);  /* HashCell */
}

STATIC void process_node_FunctionFrame(FunctionFrame *p)
{
  size_t i;

  if (p->prev_frame != NULL)
    process_edge((uintptr_t) p->prev_frame); /* FunctionFrame */
  process_edge(p->arguments);
  for (i = 0; i < p->nlocals; i++)
    process_edge(p->locals[i]);
#ifdef DEBUG
  assert(p->locals[p->nlocals - 1] == JS_UNDEFINED);
#endif /* DEBUG */
}

STATIC void process_node_Context(Context *context)
{
  int i;

  process_edge((uintptr_t) context->global);
  /* function table is a static data structure
   *   Note: spreg.cf points to internal address of the function table.
   */
  for (i = 0; i < FUNCTION_TABLE_LIMIT; i++)
    scan_function_table_entry(&context->function_table[i]);
  process_edge((uintptr_t) context->spreg.lp);  /* FunctionFrame */
  process_edge((uintptr_t) context->spreg.a);
  process_edge((uintptr_t) context->spreg.err);
  if (context->exhandler_stack_top != NULL)
    process_edge((uintptr_t) context->exhandler_stack_top);
  process_edge((uintptr_t) context->lcall_stack);

  /* process stack */
  assert(!is_marked_cell(context->stack));
  mark_cell(context->stack);
  scan_stack(context->stack, context->spreg.sp, context->spreg.fp);
}

STATIC void scan_function_table_entry(FunctionTable *p)
{
  /* trace constant pool */
  {
    JSValue *constant_pool = (JSValue *) &p->insns[p->n_insns];
    size_t n_constants = p->n_constants;
    size_t i;
    for (i = 0; i < n_constants; i++)
      process_edge(constant_pool[i]);
  }

#ifdef ALLOC_SITE_CACHE
  /* scan Allocation Sites */
  {
    size_t i;
    for (i = 0; i < p->n_insns; i++) {
      Instruction *insn = &p->insns[i];
      AllocSite *alloc_site = &insn->alloc_site;
      if (alloc_site->shape != NULL)
        process_edge((uintptr_t) alloc_site->shape);
      /* TODO: too eary PM sacnning. scan after updating alloc site info */
      if (alloc_site->pm != NULL)
        process_edge((uintptr_t) alloc_site->pm);
    }
  }
#endif /* ALLOC_SITE_CACHE */

#ifdef INLINE_CACHE
  /* scan Inline Cache */
  {
    size_t i;
    for (i = 0; i < p->n_insns; i++) {
      Instruction *insn = &p->insns[i];
      InlineCache *ic = &insn->inl_cache;
      if (ic->shape != NULL) {
        process_edge((uintptr_t) ic->shape);
        process_edge((uintptr_t) ic->prop_name);
      }
    }
  }
#endif /* INLINE_CACHE */
}

STATIC void scan_stack(JSValue* stack, int sp, int fp)
{
  while (1) {
    while (sp >= fp) {
      process_edge((uintptr_t) stack[sp]);
      sp--;
    }
    if (sp < 0)
      return;
    fp = stack[sp--]; /* FP */
    process_edge((uintptr_t) jsv_to_function_frame(stack[sp--])); /* LP */
    sp--; /* PC */
    sp--; /* CF (function table entries are scanned as a part of context) */
  }
}

STATIC void scan_string_table(StrTable *p)
{
  StrCons **vec = p->obvector;
  size_t length = p->size;
  size_t i;

  for (i = 0; i < length; i++)
    if (vec[i] != NULL)
      process_edge((uintptr_t) vec[i]); /* StrCons */
}

STATIC void scan_roots(Context *ctx)
{
  int i;

  /*
   * global variables
   */
  {
    struct global_constant_objects *gconstsp = &gconsts;
    JSValue *p;
    for (p = (JSValue *) gconstsp; p < (JSValue *) (gconstsp + 1); p++)
      process_edge((uintptr_t) *p);
  }
  {
    struct global_property_maps *gpmsp = &gpms;
    PropertyMap **p;
    for (p = (PropertyMap **) gpmsp; p < (PropertyMap **) (gpmsp + 1); p++)
      process_edge((uintptr_t) *p);
  }
  {
    struct global_object_shapes *gshapesp = &gshapes;
    Shape** p;
    for (p = (Shape **) gshapesp; p < (Shape **) (gshapesp + 1); p++)
      process_edge((uintptr_t) *p);
  }

  /* function table: do not trace.
   *                 Used slots should be traced through Function objects
   */

  /* string table */
  scan_string_table(&string_table);

  /*
   * Context
   */
  process_edge((uintptr_t) ctx); /* Context */

  /*
   * GC_PUSH'ed
   */
  for (i = 0; i < gc_root_stack_ptr; i++)
    process_edge(*(uintptr_t*) gc_root_stack[i]);
}

/*
 * Clear pointer field to StringCell whose mark bit is not set.
 * Unlink the StrCons from the string table.  These StrCons's
 * are collected in the next collection cycle.
 */
STATIC void weak_clear_StrTable(StrTable *table)
{
  size_t i;
  for (i = 0; i < table->size; i++) {
    StrCons ** p = table->obvector + i;
    while (*p != NULL) {
      StringCell *cell = jsv_to_normal_string((*p)->str);
      if (!is_marked_cell(cell)) {
        (*p)->str = JS_UNDEFINED;
        *p = (*p)->next;
      } else
        p = &(*p)->next;
    }
  }
}

#ifdef WEAK_SHAPE_LIST
void weak_clear_shape_recursive(PropertyMap *pm)
{
  HashIterator iter;
  HashCell *cell;

#ifdef VERBOSE_GC_SHAPE
#defien PRINT(x...) printf(x)
#else /* VERBOSE_GC_SHAPE */
#define PRINT(x...)
#endif /* VERBOSE_GC_SHAPE */

#ifndef NO_SHAPE_CACHE
  {
    Shape **p;
    for (p = &pm->shapes; *p != NULL; ) {
      Shape *os = *p;
      if (is_marked_cell(os))
        p = &(*p)->next;
      else {
        Shape *skip = *p;
        *p = skip->next;
#ifdef DEBUG
        PRINT("skip %p emp: %d ext: %d\n",
              skip, skip->n_embedded_slots, skip->n_extension_slots);
        skip->next = NULL;  /* avoid Black->While check failer */
#endif /* DEBUG */
      }
    }
  }
#endif /* NO_SHAPE_CACHE */

  iter = createHashIterator(pm->map);
  while (nextHashCell(pm->map, &iter, &cell) != FAIL)
    if (is_transition(cell->entry.attr))
      weak_clear_shape_recursive(cell->entry.data.u.pm);

#undef PRINT /* VERBOSE_GC_SHAPE */
}

STATIC void weak_clear_shapes()
{
  PropertyMapList **pp;
  for (pp = &the_context->property_map_roots; *pp != NULL;) {
    PropertyMapList *e = *pp;
    if (is_marked_cell(e->pm)) {
      mark_cell(e);
      weak_clear_shape_recursive(e->pm);
      pp = &(*pp)->next;
    } else
      *pp = (*pp)->next;
  }
}
#endif /* WEAK_SHAPE_LIST */

#ifdef HC_SKIP_INTERNAL
/*
 * Get the only transision from internal node.
 */
static PropertyMap* get_transition_dest(PropertyMap *pm)
{
  HashIterator iter;
  HashCell *p;

  iter = createHashIterator(pm->map);
  while(nextHashCell(pm->map, &iter, &p) != FAIL)
    if (is_transition(p->entry.attr)) {
      PropertyMap *ret = p->entry.data.u.pm;
#ifdef GC_DEBUG
      while(nextHashCell(pm->map, &iter, &p) != FAIL)
        assert(!is_transition(p->entry.attr));
#endif /* GC_DEBUG */
      return ret;
    }
  abort();
  return NULL;
}

static void weak_clear_property_map_recursive(PropertyMap *pm)
{
  HashIterator iter;
  HashCell *p;
  int n_transitions = 0;

  assert(is_marked_cell(pm));

  iter = createHashIterator(pm->map);
  while(nextHashCell(pm->map, &iter, &p) != FAIL)
    if (is_transition(p->entry.attr)) {
      PropertyMap *next = p->entry.data.u.pm;
      /*
       * If the next node is both
       *   1. not pointed to through strong pointers and
       *   2. outgoing edge is exactly 1,
       * then, the node is an internal node to be eliminated.
       */
      while (!is_marked_cell(next) && next->n_transitions == 1) {
#ifdef VERBOSE_WEAK
        printf("skip PropertyMap %p\n", next);
#endif /* VERBOSE_WEAK */
        next = get_transition_dest(next);
      }
      if (!is_marked_cell(next) && next->n_transitions == 0) {
        p->deleted = 1;             /* TODO: use hash_delete */
        p->entry.data.u.pm = NULL;  /* make invariant check success */
        continue;
      }
      n_transitions++;
#ifdef VERBOSE_WEAK
      if (is_marked_cell(next))
        printf("preserve PropertyMap %p because it has been marked\n", next);
      else
        printf("preserve PropertyMap %p because it is a branch (P=%d T=%d)\n",
               next, next->n_props, next->n_transitions);
#endif /* VERBOSE_WEAK */
      /* Resurrect if it is branching node or terminal node */
      if (!is_marked_cell(next)) {
        process_edge((uintptr_t) next);
#ifdef MARK_STACK
        process_mark_stack();
#endif /* MARK_STACK */
      }
      p->entry.data.u.pm = next;
      next->prev = pm;
      weak_clear_property_map_recursive(next);
    }
  pm->n_transitions = n_transitions;
}

STATIC void weak_clear_property_maps()
{
  PropertyMapList **pp;
  for (pp = &the_context->property_map_roots; *pp != NULL; ) {
    PropertyMap *pm = (*pp)->pm;
    while(!is_marked_cell(pm) && pm->n_transitions == 1)
      pm = get_transition_dest(pm);
    if (!is_marked_cell(pm) && pm->n_transitions == 0)
      *pp = (*pp)->next;
    else {
      pm = (*pp)->pm;
      mark_cell(*pp);
      if (!is_marked_cell(pm)) {
        process_edge((uintptr_t) pm);
#ifdef MARK_STACK
        process_mark_stack();
#endif /* MARK_STACK */
      }
      weak_clear_property_map_recursive(pm);
      pp = &(*pp)->next;
    }
  }
}
#endif /* HC_SKIP_INTERNAL */

#ifdef HC_PROF

#endif /* HC_PROF */

STATIC void weak_clear(void)
{
#ifdef HC_SKIP_INTERNAL
  /* !!! Do weak_clear_property_map first. This may resurrect some objects. */
  weak_clear_property_maps();
#endif /* HC_SKIP_INTERNAL */
#ifdef WEAK_SHAPE_LIST
  weak_clear_shapes();
#endif /* WEAK_SHAPE_LIST */
  weak_clear_StrTable(&string_table);

  /* clear cache in the context */
  the_context->exhandler_pool = NULL;
}

#ifdef ALLOC_SITE_CACHE
STATIC PropertyMap *find_lub(PropertyMap *a, PropertyMap *b)
{
  while(a != b && a->prev != NULL) {
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

STATIC void alloc_site_update_info(JSObject *p)
{
  AllocSite *as = p->alloc_site;
  PropertyMap *pm = p->shape->pm;

  assert(as != NULL);

  /* likely case */
  if (as->pm == pm)
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
    if (lub == as->pm) {
      if (as->polymorphic)
        /* keep current as->pm */ ;
      else {
        as->pm = pm;
        as->shape = NULL;
      }
    } else if (lub == pm)
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
