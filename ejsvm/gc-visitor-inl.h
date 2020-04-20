#ifdef CXX_TRACER
#define ACCEPTOR template<typename Tracer>
template<typename T>
static inline void **to_voidpp(T** p) { return (void**) p; }
static inline JSValue *to_voidpp(JSValue *p) { return p; }
#define PROCESS_EDGE(x) do {                                    \
    if (Tracer::is_pointer_updating())				\
      Tracer::process_edge(to_voidpp(&(x)));                    \
    else                                                        \
      Tracer::process_edge((x));                                \
  } while(0)

#define PROCESS_EDGE_FUNCTION_FRAME(x) do {                     \
    if (Tracer::is_pointer_updating())				\
      Tracer::process_edge_function_frame(&(x));                \
    else                                                        \
      Tracer::process_edge_function_frame((x));                 \
  } while(0)

#define MARK_CELL(x) do {                                       \
    if (Tracer::is_pointer_updating())				\
      Tracer::mark_cell((void**) &(x));                         \
    else                                                        \
      Tracer::mark_cell((x));                                   \
  } while(0)

#define TEST_AND_MARK_CELL(x) ({                                \
  bool retval;                                                  \
  if (Tracer::is_pointer_updating())				\
    retval = Tracer::test_and_mark_cell((void**) &(x));         \
  else                                                          \
    retval = Tracer::test_and_mark_cell((x));                   \
  retval;                                                       \
  })
#else /* CXX_TRACER */
#define ACCEPTOR
STATIC void process_edge(uintptr_t ptr);
#endif /* CXX_TRACER */

ACCEPTOR STATIC_INLINE void process_node(uintptr_t ptr);
ACCEPTOR STATIC void process_edge_JSValue_array(JSValue *p, size_t start, size_t length);
ACCEPTOR STATIC void process_edge_HashBody(HashCell **p, size_t length);
ACCEPTOR STATIC void process_node_FunctionFrame(FunctionFrame *p);
ACCEPTOR STATIC void scan_Context(Context *context);
ACCEPTOR STATIC void scan_function_table_entry(FunctionTable *p);
ACCEPTOR STATIC void scan_stack(JSValue* stack, int sp, int fp);
ACCEPTOR STATIC void scan_string_table(StrTable *p);
ACCEPTOR STATIC void scan_roots(Context *ctx);
ACCEPTOR STATIC void weak_clear_StrTable(StrTable *table);
#ifdef WEAK_SHAPE_LIST
ACCEPTOR STATIC void weak_clear_shape_recursive(PropertyMap *pm);
ACCEPTOR STATIC void weak_clear_shapes();
#endif /* WEAK_SHAPE_LIST */
#ifdef HC_SKIP_INTERNAL
ACCEPTOR STATIC void weak_clear_property_map_recursive(PropertyMap *pm);
ACCEPTOR STATIC void weak_clear_property_maps();
#endif /* HC_SKIP_INTERNAL */
ACCEPTOR STATIC void weak_clear(void);

extern JSValue *gc_root_stack[];
extern int gc_root_stack_ptr;
#ifdef ALLOC_SITE_CACHE
extern void alloc_site_update_info(JSObject *p);
#endif /* ALLOC_SITE_CACHE */

ACCEPTOR STATIC_INLINE void process_node(uintptr_t ptr)
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
#ifdef CXX_TRACER
        PROCESS_EDGE(p->eprop[array_length_index]);
#else /* CXX_TRACER */
        process_edge((uintptr_t) get_array_ptr_length(p));
#endif /* CXX_TRACER */
#ifdef CXX_TRACER
        process_edge_JSValue_array<Tracer>(a_body, 0, len);
#else /* CXX_TRACER */
        process_edge_JSValue_array(a_body, 0, len);
#endif /* CXX_TRACER */
      }
      break;
    }
  case CELLT_FUNCTION:
    {
      JSObject *p = (JSObject *) ptr;
#ifndef CXX_TRACER
      FunctionFrame *frame = get_function_ptr_environment(p);
#endif /* CXX_TRACER */
      /* FunctionTable *ftentry = function_ptr_table_entry(p);
       * scan_function_table_entry(ftentry);
       *    All function table entries are scanned through Context
       */
#ifdef CXX_TRACER
      PROCESS_EDGE(p->eprop[function_environment_index]);
#else /* CXX_TRACER */
      process_edge((uintptr_t) frame);
#endif /* CXX_TRACER */
      break;
    }
  case CELLT_BUILTIN:
    break;
  case CELLT_BOXED_NUMBER:
    {
      JSObject *p = (JSObject *) ptr;
#ifdef CXX_TRACER
      PROCESS_EDGE(p->eprop[number_object_value_index]);
#else /* CXX_TRACER */
      JSValue value = get_number_object_ptr_value(p);
      process_edge((uintptr_t) value);
#endif /* CXX_TRACER */
      break;
    }
  case CELLT_BOXED_STRING:
    {
      JSObject *p = (JSObject *) ptr;
#ifdef CXX_TRACER
      PROCESS_EDGE(p->eprop[string_object_value_index]);
#else /* CXX_TRACER */
      JSValue value = get_string_object_ptr_value(p);
      process_edge((uintptr_t) value);
#endif /* CXX_TRACER */
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
      if (p->size > 0) {
#ifdef CXX_TRACER
        process_edge_JSValue_array<Tracer>(p->body, 0, p->size);
#else /* CXX_TRACER */
        process_edge_JSValue_array(p->body, 0, p->size);
#endif /* CXX_TRACER */
      }
      return;
    }
  case CELLT_PROP:
  case CELLT_ARRAY_DATA:
    abort();
  case CELLT_BYTE_ARRAY:
    return;
  case CELLT_FUNCTION_FRAME:
#ifdef CXX_TRACER
    process_node_FunctionFrame<Tracer>((FunctionFrame *) ptr);
#else /* CXX_TRACER */
    process_node_FunctionFrame((FunctionFrame *) ptr);
#endif /* CXX_TRACER */
    return;
  case CELLT_STR_CONS:
    {
      StrCons *p = (StrCons *) ptr;
      /* WEAK: p->str */
      if (p->next != NULL) {
#ifdef CXX_TRACER
        PROCESS_EDGE(p->next);
#else /* CXX_TRACER */
        process_edge((uintptr_t) p->next); /* StrCons */
#endif /* CXX_TRACER */
      }
      return;
    }
  case CELLT_HASHTABLE:
    {
      HashTable *p = (HashTable *) ptr;
      if (p->body != NULL) {
#ifdef CXX_TRACER
        process_edge_HashBody<Tracer>(p->body, p->size);
#else /* CXX_TRACER */
        process_edge_HashBody(p->body, p->size);
#endif /* CXX_TRACER */
      }
      return;
    }
  case CELLT_HASH_BODY:
    abort();
  case CELLT_HASH_CELL:
    {
      HashCell *p = (HashCell *) ptr;
#ifdef CXX_TRACER
      PROCESS_EDGE(p->entry.key);
#else /* CXX_TRACER */
      process_edge((uintptr_t) p->entry.key);
#endif /* CXX_TRACER */
#ifndef HC_SKIP_INTERNAL
      /* transition link is weak if HC_SKIP_INTERNAL */
      if (is_transition(p->entry.attr)) {
#ifdef CXX_TRACER
        PROCESS_EDGE(p->entry.data.u.pm);
#else /* CXX_TRACER */
        process_edge((uintptr_t) p->entry.data.u.pm);  /* PropertyMap */
#endif /* CXX_TRACER */
      }
#endif /* HC_SKIP_INTERNAL */
      if (p->next != NULL) {
#ifdef CXX_TRACER
        PROCESS_EDGE(p->next);
#else /* CXX_TRACER */
        process_edge((uintptr_t) p->next);  /* HashCell */
#endif /* CXX_TRACER */
      }
      return;
    }
  case CELLT_PROPERTY_MAP:
    {
      PropertyMap *p = (PropertyMap *) ptr;
#ifdef CXX_TRACER
      PROCESS_EDGE(p->map);
#else /* CXX_TRACER */
      process_edge((uintptr_t) p->map); /* HashTable */
#endif /* CXX_TRACER */
#ifndef HC_SKIP_INTERNAL
      if (p->prev != NULL) {
        /* weak if HC_SKIP_INTERNAL */
#ifdef CXX_TRACER
        PROCESS_EDGE(p->prev);
#else /* CXX_TRACER */
        process_edge((uintptr_t) p->prev); /* PropertyMap */
#endif /* CXX_TRACER */
      }
#endif /* HC_SKIP_INTERNAL */
      if (p->shapes != NULL) {
#ifdef CXX_TRACER
        PROCESS_EDGE(p->shapes);
#else /* CXX_TRACER */
        process_edge((uintptr_t) p->shapes); /* Shape
                                              * (always keep the largest one) */
#endif /* CXX_TRACER */
      }
#ifdef CXX_TRACER
      PROCESS_EDGE(p->__proto__);
#else /* CXX_TRACER */
      process_edge((uintptr_t) p->__proto__);
#endif /* CXX_TRACER */
      return;
    }
  case CELLT_SHAPE:
    {
      Shape *p = (Shape *) ptr;
#ifdef CXX_TRACER
      PROCESS_EDGE(p->pm);
#else /* CXX_TRACER */
      process_edge((uintptr_t) p->pm);
#endif /* CXX_TRACER */
#ifndef NO_SHAPE_CACHE
#ifdef WEAK_SHAPE_LIST
      /* p->next is weak */
#else /* WEAK_SHAPE_LIST */
      if (p->next != NULL)
#ifdef CXX_TRACER
        PROCESS_EDGE(p->next);
#else /* CXX_TRACER */
        process_edge((uintptr_t) p->next);
#endif /* CXX_TRACER */
#endif /* WEAK_SHAPE_LIST */
#endif /* NO_SHAPE_CACHE */
      return;
    }
  case CELLT_UNWIND:
    {
      UnwindProtect *p = (UnwindProtect *) ptr;
#ifdef CXX_TRACER
      PROCESS_EDGE(p->prev);
      PROCESS_EDGE(p->lp);
#else /* CXX_TRACER */
      process_edge((uintptr_t) p->prev);
      process_edge((uintptr_t) p->lp);
#endif /* CXX_TRACER */
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
    /* 1. shape */
#ifdef CXX_TRACER
    PROCESS_EDGE(p->shape);
#else /* CXX_TRACER */
    process_edge((uintptr_t) p->shape);
#endif /* CXX_TRACER */

    /* 2. embedded propertyes */
    Shape *os = p->shape;
    int n_extension = os->n_extension_slots;
    size_t actual_embedded = os->n_embedded_slots - (n_extension == 0 ? 0 : 1);
    size_t i;
    for (i = os->pm->n_special_props; i < actual_embedded; i++)
#ifdef CXX_TRACER
      PROCESS_EDGE(p->eprop[i]);
#else /* CXX_TRACER */
      process_edge((uintptr_t) p->eprop[i]);
#endif /* CXX_TRACER */
    if (n_extension != 0) {
      /* 3. extension */
      JSValue *extension = jsv_to_extension_prop(p->eprop[actual_embedded]);
#ifdef CXX_TRACER
      process_edge_JSValue_array<Tracer>(extension, 0,
                                 os->pm->n_props - actual_embedded);
#else /* CXX_TRACER */
      process_edge_JSValue_array(extension, 0,
                                 os->pm->n_props - actual_embedded);
#endif /* CXX_TRACER */
    }
#ifdef ALLOC_SITE_CACHE
    /* 4. allocation site cache */
    if (p->alloc_site != NULL)
      alloc_site_update_info(p);
#endif /* ALLOC_SITE_CACHE */
  }
}

ACCEPTOR STATIC void process_edge_JSValue_array(JSValue *p, size_t start, size_t length)
{
  size_t i;
  assert(in_js_space(p));
#ifdef CXX_TRACER
  if (TEST_AND_MARK_CELL(p))
#else /* CXX_TRACER */
  if (test_and_mark_cell(p))
#endif /* CXX_TRACER */
    return;
  for (i = start; i < length; i++) {
#ifdef CXX_TRACER
    PROCESS_EDGE(p[i]);
#else /* CXX_TRACER */
    process_edge((uintptr_t) p[i]);
#endif /* CXX_TRACER */
  }
}

ACCEPTOR STATIC void process_edge_HashBody(HashCell **p, size_t length)
{
  size_t i;
  assert(in_js_space(p));
#ifdef CXX_TRACER
  if (TEST_AND_MARK_CELL(p))
#else /* CXX_TRACER */
  if (test_and_mark_cell(p))
#endif /* CXX_TRACER */
    return;
  for (i = 0; i < length; i++)
    if (p[i] != NULL) {
#ifdef CXX_TRACER
      PROCESS_EDGE(p[i]);
#else /* CXX_TRACER */
      process_edge((uintptr_t) p[i]);  /* HashCell */
#endif /* CXX_TRACER */
    }
}

ACCEPTOR STATIC void process_node_FunctionFrame(FunctionFrame *p)
{
  int i;

  if (p->prev_frame != NULL) {
#ifdef CXX_TRACER
    PROCESS_EDGE(p->prev_frame);
#else /* CXX_TRACER */
    process_edge((uintptr_t) p->prev_frame); /* FunctionFrame */
#endif /* CXX_TRACER */
  }
#ifdef CXX_TRACER
  PROCESS_EDGE(p->arguments);
#else /* CXX_TRACER */
  process_edge((uintptr_t) p->arguments);
#endif /* CXX_TRACER */
  for (i = 0; i < p->nlocals; i++) {
#ifdef CXX_TRACER
    PROCESS_EDGE(p->locals[i]);
#else /* CXX_TRACER */
    process_edge((uintptr_t) p->locals[i]);
#endif /* CXX_TRACER */
  }
#ifdef DEBUG
  assert(p->locals[p->nlocals - 1] == JS_UNDEFINED);
#endif /* DEBUG */
}

ACCEPTOR STATIC void scan_Context(Context *context)
{
  int i;

#ifdef CXX_TRACER
  PROCESS_EDGE(context->global);
#else /* CXX_TRACER */
  process_edge((uintptr_t) context->global);
#endif /* CXX_TRACER */
  /* function table is a static data structure
   *   Note: spreg.cf points to internal address of the function table.
   */
  for (i = 0; i < FUNCTION_TABLE_LIMIT; i++) {
#ifdef CXX_TRACER
    scan_function_table_entry<Tracer>(&context->function_table[i]);
#else /* CXX_TRACER */
    scan_function_table_entry(&context->function_table[i]);
#endif /* CXX_TRACER */
  }
#ifdef CXX_TRACER
  PROCESS_EDGE(context->spreg.lp);
  PROCESS_EDGE(context->spreg.a);
  PROCESS_EDGE(context->spreg.err);
#else /* CXX_TRACER */
  process_edge((uintptr_t) context->spreg.lp);  /* FunctionFrame */
  process_edge((uintptr_t) context->spreg.a);
  process_edge((uintptr_t) context->spreg.err);
#endif /* CXX_TRACER */
  if (context->exhandler_stack_top != NULL) {
#ifdef CXX_TRACER
    PROCESS_EDGE(context->exhandler_stack_top);
#else /* CXX_TRACER */
    process_edge((uintptr_t) context->exhandler_stack_top);
#endif /* CXX_TRACER */
  }
#ifdef CXX_TRACER
  PROCESS_EDGE(context->lcall_stack);
#else /* CXX_TRACER */
  process_edge((uintptr_t) context->lcall_stack);
#endif /* CXX_TRACER */

  /* process stack */
#ifdef CXX_TRACER
  scan_stack<Tracer>(context->stack, context->spreg.sp, context->spreg.fp);
#else /* CXX_TRACER */
  scan_stack(context->stack, context->spreg.sp, context->spreg.fp);
#endif /* CXX_TRACER */
}

ACCEPTOR STATIC void scan_function_table_entry(FunctionTable *p)
{
  /* trace constant pool */
  {
    JSValue *constant_pool = (JSValue *) &p->insns[p->n_insns];
    size_t n_constants = p->n_constants;
    size_t i;
    for (i = 0; i < n_constants; i++) {
#ifdef CXX_TRACER
      PROCESS_EDGE(constant_pool[i]);
#else /* CXX_TRACER */
      process_edge((uintptr_t) constant_pool[i]);
#endif /* CXX_TRACER */
    }
  }

#ifdef ALLOC_SITE_CACHE
  /* scan Allocation Sites */
  {
    int i;
    for (i = 0; i < p->n_insns; i++) {
      Instruction *insn = &p->insns[i];
      AllocSite *alloc_site = &insn->alloc_site;
      if (alloc_site->shape != NULL) {
#ifdef CXX_TRACER
        PROCESS_EDGE(alloc_site->shape);
#else /* CXX_TRACER */
        process_edge((uintptr_t) alloc_site->shape);
#endif /* CXX_TRACER */
      }
      /* TODO: too eary PM sacnning. scan after updating alloc site info */
      if (alloc_site->pm != NULL) {
#ifdef CXX_TRACER
        PROCESS_EDGE(alloc_site->pm);
#else /* CXX_TRACER */
        process_edge((uintptr_t) alloc_site->pm);
#endif /* CXX_TRACER */
      }
    }
  }
#endif /* ALLOC_SITE_CACHE */

#ifdef INLINE_CACHE
  /* scan Inline Cache */
  {
    int i;
    for (i = 0; i < p->n_insns; i++) {
      Instruction *insn = &p->insns[i];
      InlineCache *ic = &insn->inl_cache;
      if (ic->shape != NULL) {
#ifdef CXX_TRACER
        PROCESS_EDGE(ic->shape);
        PROCESS_EDGE(ic->prop_name);
#else /* CXX_TRACER */
        process_edge((uintptr_t) ic->shape);
        process_edge((uintptr_t) ic->prop_name);
#endif /* CXX_TRACER */
      }
    }
  }
#endif /* INLINE_CACHE */
}

ACCEPTOR STATIC void scan_stack(JSValue* stack, int sp, int fp)
{
  while (1) {
    while (sp >= fp) {
#ifdef CXX_TRACER
      PROCESS_EDGE(stack[sp]);
#else /* CXX_TRACER */
      process_edge((uintptr_t) stack[sp]);
#endif /* CXX_TRACER */
      sp--;
    }
    if (sp < 0)
      return;
    fp = stack[sp--]; /* FP */
#ifdef CXX_TRACER
    PROCESS_EDGE_FUNCTION_FRAME(stack[sp--]); /* LP */
#else /* CXX_TRACER */
    process_edge((uintptr_t) jsv_to_function_frame(stack[sp--])); /* LP */
#endif /* CXX_TRACER */
    sp--; /* PC */
    sp--; /* CF (function table entries are scanned as a part of context) */
  }
}

ACCEPTOR STATIC void scan_string_table(StrTable *p)
{
  StrCons **vec = p->obvector;
  size_t length = p->size;
  size_t i;

  for (i = 0; i < length; i++)
    if (vec[i] != NULL) {
#ifdef CXX_TRACER
      PROCESS_EDGE(vec[i]);
#else /* CXX_TRACER */
      process_edge((uintptr_t) vec[i]); /* StrCons */
#endif /* CXX_TRACER */
    }
}

ACCEPTOR STATIC void scan_roots(Context *ctx)
{
  int i;

  /*
   * global variables
   */
  {
    struct global_constant_objects *gconstsp = &gconsts;
    JSValue *p;
    for (p = (JSValue *) gconstsp; p < (JSValue *) (gconstsp + 1); p++) {
#ifdef CXX_TRACER
      PROCESS_EDGE(*p);
#else /* CXX_TRACER */
      process_edge((uintptr_t) *p);
#endif /* CXX_TRACER */
    }
  }
  {
    struct global_property_maps *gpmsp = &gpms;
    PropertyMap **p;
    for (p = (PropertyMap **) gpmsp; p < (PropertyMap **) (gpmsp + 1); p++) {
#ifdef CXX_TRACER
      PROCESS_EDGE(*p);
#else /* CXX_TRACER */
      process_edge((uintptr_t) *p);
#endif /* CXX_TRACER */
    }
  }
  {
    struct global_object_shapes *gshapesp = &gshapes;
    Shape** p;
    for (p = (Shape **) gshapesp; p < (Shape **) (gshapesp + 1); p++) {
#ifdef CXX_TRACER
      PROCESS_EDGE(*p);
#else /* CXX_TRACER */
      process_edge((uintptr_t) *p);
#endif /* CXX_TRACER */
    }
  }

  /* function table: do not trace.
   *                 Used slots should be traced through Function objects
   */

  /* string table */
#ifdef CXX_TRACER
  scan_string_table<Tracer>(&string_table);
#else /* CXX_TRACER */
  scan_string_table(&string_table);
#endif /* CXX_TRACER */

  /*
   * Context
   */
  scan_Context<Tracer>(ctx);

  /*
   * GC_PUSH'ed
   */
  for (i = 0; i < gc_root_stack_ptr; i++) {
#ifdef CXX_TRACER
    PROCESS_EDGE(*(gc_root_stack[i]));
#else /* CXX_TRACER */
    process_edge(*(uintptr_t*) gc_root_stack[i]);
#endif /* CXX_TRACER */
  }
}

/*
 * Clear pointer field to StringCell whose mark bit is not set.
 * Unlink the StrCons from the string table.  These StrCons's
 * are collected in the next collection cycle.
 */
ACCEPTOR STATIC void weak_clear_StrTable(StrTable *table)
{
  size_t i;
  for (i = 0; i < table->size; i++) {
    StrCons ** p = table->obvector + i;
    while (*p != NULL) {
      StringCell *cell = jsv_to_normal_string((*p)->str);
#ifdef CXX_TRACER
      if (!Tracer::is_marked_cell(cell)) {
        (*p)->str = JS_UNDEFINED;
        *p = (*p)->next;
      } else
#else /* CXX_TRACER */
      if (!is_marked_cell(cell)) {
        (*p)->str = JS_UNDEFINED;
        *p = (*p)->next;
      } else
#endif /* CXX_TRACER */
        p = &(*p)->next;
    }
  }
}

#ifdef WEAK_SHAPE_LIST
ACCEPTOR STATIC void weak_clear_shape_recursive(PropertyMap *pm)
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
#ifdef CXX_TRACER
      if (Tracer::is_marked_cell(os))
#else /* CXX_TRACER */
      if (is_marked_cell(os))
#endif /* CXX_TRACER */
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
#ifdef CXX_TRACER
      weak_clear_shape_recursive<Tracer>(cell->entry.data.u.pm);
#else /* CXX_TRACER */
      weak_clear_shape_recursive(cell->entry.data.u.pm);
#endif /* CXX_TRACER */

#undef PRINT /* VERBOSE_GC_SHAPE */
}

ACCEPTOR STATIC void weak_clear_shapes()
{
  PropertyMapList **pp;
  for (pp = &the_context->property_map_roots; *pp != NULL;) {
    PropertyMapList *e = *pp;
#ifdef CXX_TRACER
    if (Tracer::is_marked_cell(e->pm)) {
      MARK_CELL(e);
      weak_clear_shape_recursive<Tracer>(e->pm);
      pp = &(*pp)->next;
    } else
#else /* CXX_TRACER */
    if (is_marked_cell(e->pm)) {
      mark_cell(e);
      weak_clear_shape_recursive(e->pm);
      pp = &(*pp)->next;
    } else
#endif /* CXX_TRACER */
      *pp = (*pp)->next;
  }
}
#endif /* WEAK_SHAPE_LIST */

#ifdef HC_SKIP_INTERNAL
/*
 * Get the only transision from internal node.
 */
STATIC PropertyMap* get_transition_dest(PropertyMap *pm)
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

ACCEPTOR STATIC void weak_clear_property_map_recursive(PropertyMap *pm)
{
  HashIterator iter;
  HashCell *p;
  int n_transitions = 0;

#ifdef CXX_TRACER
  assert(Tracer::is_marked_cell(pm));
#else /* CXX_TRACER */
  assert(is_marked_cell(pm));
#endif /* CXX_TRACER */

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
      while (
#ifdef CXX_TRACER
	     !Tracer::is_marked_cell(next) && next->n_transitions == 1
#else /* CXX_TRACER */
	     !is_marked_cell(next) && next->n_transitions == 1
#endif /* CXX_TRACER */
	     ) {
#ifdef VERBOSE_WEAK
        printf("skip PropertyMap %p\n", next);
#endif /* VERBOSE_WEAK */
        next = get_transition_dest(next);
      }
#ifdef CXX_TRACER
      if (!Tracer::is_marked_cell(next) && next->n_transitions == 0) {
        p->deleted = 1;             /* TODO: use hash_delete */
        p->entry.data.u.pm = NULL;  /* make invariant check success */
        continue;
      }
#else /* CXX_TRACER */
      if (!is_marked_cell(next) && next->n_transitions == 0) {
        p->deleted = 1;             /* TODO: use hash_delete */
        p->entry.data.u.pm = NULL;  /* make invariant check success */
        continue;
      }
#endif /* CXX_TRACER */
      n_transitions++;
#ifdef VERBOSE_WEAK
#ifdef CXX_TRACER
      if (Tracer::is_marked_cell(next))
        printf("preserve PropertyMap %p because it has been marked\n", next);
#else /* CXX_TRACER */
      if (is_marked_cell(next))
        printf("preserve PropertyMap %p because it has been marked\n", next);
#endif /* CXX_TRACER */
      else
        printf("preserve PropertyMap %p because it is a branch (P=%d T=%d)\n",
               next, next->n_props, next->n_transitions);
#endif /* VERBOSE_WEAK */
      /* Resurrect if it is branching node or terminal node */
#ifdef CXX_TRACER
      PROCESS_EDGE(next);
#else /* CXX_TRACER */
      process_edge((uintptr_t) next);
#endif /* CXX_TRACER */
#ifdef MARK_STACK
#ifdef CXX_TRACER
      Tracer::process_mark_stack();
#else /* CXX_TRACER */
      process_mark_stack();
#endif /* CXX_TRACER */
#endif /* MARK_STACK */
      p->entry.data.u.pm = next;
      next->prev = pm;
#ifdef CXX_TRACER
      weak_clear_property_map_recursive<Tracer>(next);
#else /* CXX_TRACER */
      weak_clear_property_map_recursive(next);
#endif /* CXX_TRACER */
    }
  pm->n_transitions = n_transitions;
}

ACCEPTOR STATIC void weak_clear_property_maps()
{
  PropertyMapList **pp;
  for (pp = &the_context->property_map_roots; *pp != NULL; ) {
    PropertyMap *pm = (*pp)->pm;
#ifdef CXX_TRACER
    while(!Tracer::is_marked_cell(pm) && pm->n_transitions == 1)
#else /* CXX_TRACER */
    while(!is_marked_cell(pm) && pm->n_transitions == 1)
#endif /* CXX_TRACER */
      pm = get_transition_dest(pm);
#ifdef CXX_TRACER
    if (!Tracer::is_marked_cell(pm) && pm->n_transitions == 0)
#else /* CXX_TRACER */
    if (!is_marked_cell(pm) && pm->n_transitions == 0)
#endif /* CXX_TRACER */
      *pp = (*pp)->next;
    else {
      pm = (*pp)->pm;
#ifdef CXX_TRACER
      MARK_CELL(*pp);
#else /* CXX_TRACER */
      mark_cell(*pp);
#endif /* CXX_TRACER */
#ifdef CXX_TRACER
      if (!Tracer::is_marked_cell(pm)) {
        PROCESS_EDGE(pm);
        (*pp)->pm = pm;
#ifdef MARK_STACK
	Tracer::process_mark_stack();
#endif /* MARK_STACK */
      }
#else /* CXX_TRACER */
      if (!is_marked_cell(pm)) {
        process_edge((uintptr_t) pm);
#ifdef MARK_STACK
        process_mark_stack();
#endif /* MARK_STACK */
      }
#endif /* CXX_TRACER */
#ifdef CXX_TRACER
      weak_clear_property_map_recursive<Tracer>(pm);
#else /* CXX_TRACER */
      weak_clear_property_map_recursive(pm);
#endif /* CXX_TRACER */
      pp = &(*pp)->next;
    }
  }
}
#endif /* HC_SKIP_INTERNAL */

ACCEPTOR STATIC void weak_clear(void)
{
#ifdef HC_SKIP_INTERNAL
  /* !!! Do weak_clear_property_map first. This may resurrect some objects. */
#ifdef CXX_TRACER
  weak_clear_property_maps<Tracer>();
#else /* CXX_TRACER */
  weak_clear_property_maps();
#endif /* CXX_TRACER */
#endif /* HC_SKIP_INTERNAL */
#ifdef WEAK_SHAPE_LIST
#ifdef CXX_TRACER
  weak_clear_shapes<Tracer>();
#else /* CXX_TRACER */
  weak_clear_shapes();
#endif /* CXX_TRACER */
#endif /* WEAK_SHAPE_LIST */
#ifdef CXX_TRACER
  weak_clear_StrTable<Tracer>(&string_table);
#else /* CXX_TRACER */
  weak_clear_StrTable(&string_table);
#endif /* CXX_TRACER */

  /* clear cache in the context */
  the_context->exhandler_pool = NULL;
}

