/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
#define concat(context,s1,s2)            ejs_string_concat((context), (s1), (s2))
#define getArrayProp(context,v1,v2)      get_array_prop((context), (v1), (v2))

#ifdef INLINE_CACHE
#define getObjectProp(context,v1,v2)     get_object_prop((context), (v1), (v2), NULL)
#else /* INLINE_CACHE */
#define getObjectProp(context,v1,v2)     get_object_prop((context), (v1), (v2))
#endif /* INLINE_CACHE */
#define SetArrayProp(context,v1,v2,v3)   set_array_prop((context), (v1), (v2), (v3))
#define SetObjectProp(context,v1,v2,v3)  set_object_prop((context), (v1), (v2), (v3))
#ifdef INLINE_CACHE
static inline void set_object_prop_inl(Context *ctx, JSValue obj, JSValue prop, JSValue val, InlineCache *ic) {
  assert(ic->shape == NULL || ic->shape->n_extension_slots == 0);
  if (ic->shape == object_get_shape(obj) && ic->prop_name == prop)
    jsv_to_jsobject(obj)->eprop[ic->index] = val;
  else
    set_prop_with_ic(ctx, obj, prop, val, ATTR_NONE, ic);
}
#define SetObjectPropInl(context,v1,v2,v3)  set_object_prop_inl((context), (v1), (v2), (v3), &insns->inl_cache)
#endif /* INLINE_CACHE */

#define cint_to_double(x) ((double)(x))
#define double_to_cint(x) ((cint)(x))

#define FIXNUM_EQ(v1,v2) ((int64_t) (v1) == (int64_t) (v2))
#define FIXNUM_LESSTHAN(v1,v2)   ((int64_t) (v1) < (int64_t) (v2))
#define FIXNUM_LESSTHANEQ(v1,v2) ((int64_t) (v1) <= (int64_t) (v2))
#define FIXNUM_AND(v1,v2)        ((int64_t) (v1) & (int64_t) (v2))
#define FIXNUM_OR(v1,v2)        ((int64_t) (v1) | (int64_t) (v2))


#define Object_to_primitive_hint_number(context,v) object_to_primitive((context), (v) ,HINT_NUMBER)
#define Strcmp(x1,x2)         strcmp((x1), (x2))
#define Half_fixnum_range(x)  half_fixnum_range((x))

#define IsFlonumInfinity(v)    ((v) == gconsts.g_flonum_infinity)
#define IsFlonumNegInfinity(v) ((v) == gconsts.g_flonum_negative_infinity)
#define IsFlonumNan(v)         ((v) == gconsts.g_flonum_nan)
#define IsFixnumZero(v)        ((v) == small_cint_to_fixnum((cint)0))

#define LogicalRightShift(v1, v2)   ((uint32_t)(v1) >> (uint32_t)(v2))

#define GetProp(v1, v2) get_prop((v1), (v2))
#define Get_opcode()    get_opcode(insn)
#define IsSend(op)      (((op) != CALL)? TRUE : FALSE)
#define IsTailSend(op)  (((op) == TAILSEND)? TRUE : FALSE)
#define IsNewSend(op)   (((op) == NEWSEND)? TRUE : FALSE)
#define Set_fp(context)        set_fp(context, fp)
#define Set_pc(context)        set_pc(context, pc)
#define Set_sp(n)       set_sp(context, fp - n)
#define Try_gc(context)        try_gc(context)
#define Call_function(context,fn, n, sendp)		\
  call_function(context, (fn), (n), (sendp))
#define Call_builtin(context,fn, n, sendp, newp)		\
  call_builtin(context, (fn), (n), (sendp), (newp))
#define Tailcall_function(context,fn, n, sendp)			\
  tailcall_function(context, (fn), (n), (sendp))
#define Tailcall_builtin(context,fn, n, sendp)			\
  tailcall_builtin(context, (fn), (n), (sendp), FALSE)
#ifdef ALLOC_SITE_CACHE
#define Create_simple_object_with_constructor(context, con)                      \
  create_simple_object_with_constructor(context, con, &insns->alloc_site)
#else /* ALLOC_SITE_CACHE */
#define Create_simple_object_with_constructor(context, con)                      \
  create_simple_object_with_constructor(context, con)
#endif /* ALLOC_SITE_CACHE */
#ifdef INLINE_CACHE
/* Implementation of the branch of GETPROP instruction where obj is
 * a JSObject and INLINE_CACHE is enabled.
 */
static inline JSValue get_prop_object_inl_helper(Context* context, InlineCache* ic,
                                   JSValue obj, JSValue prop)
{
  JSValue ret;
  assert(ic->shape == NULL || ic->shape->n_extension_slots == 0);
  if (ic->shape == object_get_shape(obj) && ic->prop_name == prop)
    ret = jsv_to_jsobject(obj)->eprop[ic->index];
  else
    ret = get_object_prop(context, obj, prop, ic);
  return ret;
}
#define Get_prop_object_inl(context, obj, prop)                           \
  get_prop_object_inl_helper(context, &insns->inl_cache, obj, prop)
#endif /* INLINE_CACHE */
#define GetProp(v1, v2)             get_prop((v1), (v2))
#define Get_globalobj(context)      ((context)->global)
#define Isundefined(v1)             true_false(is_undefined((v1)))
#define Isobject(v1)                is_object((v1))
#define Jump(d0)                    set_pc_relative((d0))
#define Lcall_stack_push()          lcall_stack_push(context, pc)
#define Lcall_stack_pop()           lcall_stack_pop(context, pc)

#define NotImplemented()            NOT_IMPLEMENTED()
#define GOTO(l)                     goto l
#define Ret_minus_one()             return -1
#define Error()                     regbase[r0] = 0

#define Nop()                       asm volatile("#NOP Instruction\n")
#define Not(obj)                    true_false(obj == JS_FALSE || obj == FIXNUM_ZERO || obj == gconsts.g_flonum_nan || obj == gconsts.g_string_empty)
#define Get_literal(d1)             get_literal(insns, d1)

#define Seta(v0)                   set_a(context, v0)
#define Setarray(dst, index, src)  (array_body_index(v0, s1) = v2)

#define IsEmptyCstring(str)        ((str)[0] == '\0')
#define CstrToString(cstr)         cstr_to_string(NULL, (cstr))
#define PutLnChar()                putchar('\n')
#define AllocateJSArray(context, size)  ((JSValue *)gc_malloc((context), sizeof(JSValue) * (size), HTAG_ARRAY_DATA))
#define AllocateCintArray(size)  ((cint *)malloc(sizeof(cint) * (size))
#define AllocateCdoubleArray(size)  ((double *)malloc(sizeof(double) * (size))

#define NewSimpleObject(context, name, os)     new_simple_object((context), (name), (os))
#define NewBooleanObject(context, name, os, v) new_boolean_object((context), (name), (os), (v))
#define NewStringObject(context, name, os, v)  new_string_object((context), (name), (os), (v))
#define NewNumberObject(context, name, os, v)  new_number_object((context), (name), (os), (v))

#define int32_to_cint(v)             ((cint)(v))
#define cint_to_int32(v)             ((int32_t)(v))
#define cint_to_uint32(v)            ((uint32_t)(v))
#define fixnum_to_intjsv_t(v)        ((intjsv_t)(v))

/*
 * For Pair-Assignment operations
 */

struct Strtol_rettype{cint r0;/* return value of strtol */ char* r1; /* endptr of strtol */};

struct Strtod_rettype{double r0;/* return value of strtod */ char* r1; /* endptr of strtod */};

static inline struct Strtol_rettype Strtol(char *s, int base){
  struct Strtol_rettype ret;
  ret.r0 = strtol(s, &ret.r1, base);
  return ret;
}

static inline struct Strtod_rettype Strtod(char *s){
  struct Strtod_rettype ret;
  ret.r0 = strtod(s, &ret.r1);
  return ret;
}

/*
 * Wrapped instructions
 *
 * NOTE:
 *   The variable name "wrapped_return_value" is special.
 *   This name is used to recieve a return value of wrapped functions.
 *   eg. Getargument() in getarg instruction
 */

#define Pophandler()					                \
  do {                                        \
    UnwindProtect *p;                         \
    p = context->exhandler_stack_top;         \
    if (p != NULL) {                          \
      context->exhandler_stack_top = p->prev; \
      p->prev = context->exhandler_pool;      \
      context->exhandler_pool = p;            \
    }                                         \
  } while(0)

#define Pushhandler(d0)                                               \
  do {                                                                \
    UnwindProtect *p;                                                 \
                                                                      \
    if (context->exhandler_pool != NULL) {                            \
      p = context->exhandler_pool;                                    \
      context->exhandler_pool = p->prev;                              \
    } else {                                                          \
      save_context();                                                 \
      p = (UnwindProtect *) gc_malloc(context, sizeof(UnwindProtect), \
                                      CELLT_UNWIND);                  \
      update_context();                                               \
    }                                                                 \
    p->fp = fp;                                                       \
    p->pc = pc + d0;                                                  \
    p->lp = get_lp(context);                                          \
    p->lcall_stack_ptr = context->lcall_stack_ptr;                    \
    p->_jmp_buf = &jmp_buf;                                           \
    p->prev = context->exhandler_stack_top;                           \
    context->exhandler_stack_top = p;                                 \
  } while(0)

#define Poplocal()				            \
  do {                                \
    int newpc;					              \
    lcall_stack_pop(context, &newpc); \
  } while(0)

#define Ret()							                           \
do {                                                 \
  if (fp == border) return 1;					               \
  JSValue* stack = &get_stack(context, 0);			     \
  restore_special_registers(context, stack, fp - 4); \
} while(0)

#define Newframe(i0, i1)						                                      \
  do {                                                                    \
    int frame_len = (i0);                                                 \
    int make_arguments = (i1);                                            \
    FunctionFrame *fr;                                                    \
    int num_of_args, i;                                                   \
    JSValue args;                                                         \
    save_context();                                                       \
    fr = new_frame(context, get_cf(context), get_lp(context), frame_len); \
    set_lp(context, fr);                                                  \
    update_context();                                                     \
    if (make_arguments) {                                                 \
      JSValue *body;                                                      \
      num_of_args = get_ac(context);                                      \
      save_context();                                                     \
      GC_PUSH(fr);                                                        \
      args = new_array_object(context, DEBUG_NAME("arguments"),           \
                              gshapes.g_shape_Array, num_of_args);        \
      GC_POP(fr);                                                         \
      update_context();                                                   \
      body = get_jsarray_body(args);                                      \
      for (i = 0; i < num_of_args; i++) {                                 \
        body[i] = regbase[i + 2];                                         \
      }                                                                   \
      fframe_arguments(fr) = args;                                        \
      fframe_locals_idx(fr, 0) = args;                                    \
    }                                                                     \
  } while(0)

#define Exitframe(context)               \
  do {                                   \
    FunctionFrame *fr = get_lp(context); \
    set_lp(context, fframe_prev(fr));    \
  } while(0)

#define Makeclosure(context, ss) \
  new_function_object((context), DEBUG_NAME("insn:makeclosure"), gshapes.g_shape_Function, (ss))

// static inline JSValue get_global_helper(Context* context, JSValue str) {
//   JSValue ret;
//   ret = get_prop(context->global, str);
//   if (ret == JS_EMPTY)
//     LOG_EXIT("GETGLOBAL: %s not found\n", string_to_cstr(str));
//   return ret;
// }
// #define Get_global(context,v1)              get_global_helper((context), v1)
#define Get_global(context,v1)                                     \
  do {                                                             \
    JSValue ret;                                                   \
    ret = get_prop((context)->global, (v1));                       \
    if (ret == JS_EMPTY)                                           \
      LOG_EXIT("GETGLOBAL: %s not found\n", string_to_cstr((v1))); \
    wrapped_return_value = ret;                                    \
  } while(0)

// static inline JSValue nextpropnameidx_helper(JSValue itr) {
//   JSValue res = JS_UNDEFINED;
//   iterator_get_next_propname(itr, &res);
//   return res;
// }
// #define Nextpropnameidx(ite)        nextpropnameidx_helper(ite)
#define Nextpropnameidx(ite)                 \
  do {                                       \
    JSValue res = JS_UNDEFINED;              \
    iterator_get_next_propname((ite), &res); \
    wrapped_return_value = res;              \
  } while(0)

// static inline void setlocal_helper(Context* context, int link, Subscript index, JSValue v2) {
//   FunctionFrame *fr;
//   int i;
//   fr = get_lp(context);
//   for (i = 0; i < link; i++) fr = fframe_prev(fr);
//   fframe_locals_idx(fr, index) = v2;
// }
// #define Setlocal(link, index, v)   setlocal_helper(context, link, index, v)
#define Setlocal(context, link, index, v)              \
  do {                                                 \
    FunctionFrame *fr;                                 \
    int i;                                             \
    fr = get_lp((context));                            \
    for (i = 0; i < (link); i++) fr = fframe_prev(fr); \
    fframe_locals_idx(fr, (index)) = (v);              \
  } while(0)

// static inline void setglobal_helper(Context* context, JSValue str, JSValue src) {
//   set_prop(context, context->global, str, src, ATTR_NONE);
// }
// #define Setglobal(str, src)        setglobal_helper(context, str, src)
#define Setglobal(context, str, src) \
  set_prop((context), (context)->global, (str), (src), ATTR_NONE)

// static inline void setfl_helper(Context* context, JSValue *regbase, int fp, int newfl, FunctionTable *curfn, int pc) {
//   int oldfl;
//   oldfl = get_sp(context) - fp + 1;
//   // printf("fp = %d, newfl = %d, fp + newfl = %d\n", fp, newfl, fp + newfl);
//   if (fp + newfl > regstack_limit){
//     set_cf(context, curfn);
//     set_pc(context, pc);
//     set_fp(context, fp);
//     LOG_EXIT2(context, "register stack overflow\n");
//   }
//   set_sp(context, fp + newfl - 1);
//   while (++oldfl <= newfl)
//     regbase[oldfl] = JS_UNDEFINED;
// }
// #define Setfl(i0)                  setfl_helper(context, regbase, fp, i0, curfn, pc)
#define Setfl(context, i0)                               \
  do {                                                   \
    int oldfl;                                           \
    oldfl = get_sp((context)) - fp + 1;                  \
    if (fp + (i0) > regstack_limit){                     \
      set_cf((context), curfn);                          \
      set_pc((context), pc);                             \
      set_fp((context), fp);                             \
      LOG_EXIT2((context), "register stack overflow\n"); \
    }                                                    \
    set_sp((context), fp + (i0) - 1);                    \
    while (++oldfl <= (i0))                              \
      regbase[oldfl] = JS_UNDEFINED;                     \
  } while(0)

// static inline void setarg_helper(Context* context, int link, Subscript index, JSValue v2) {
//   FunctionFrame *fr;
//   JSValue arguments;
//   int i;
//   fr = get_lp(context);
//   for (i = 0; i < link; i++) fr = fframe_prev(fr);
//   arguments = fframe_arguments(fr);
//   set_array_prop(context, arguments, small_cint_to_fixnum(index), v2);
// }
// #define Setarg(i0, s1, v2)         setarg_helper(context, i0, s1, v2)
#define Setarg(context, i0, s1, v2)                                          \
  do {                                                                       \
    FunctionFrame *fr;                                                       \
    JSValue arguments;                                                       \
    int i;                                                                   \
    fr = get_lp((context));                                                  \
    for (i = 0; i < (i0); i++) fr = fframe_prev(fr);                         \
    arguments = fframe_arguments(fr);                                        \
    set_array_prop((context), arguments, small_cint_to_fixnum((s1)), (v2)); \
  } while(0)

// static inline InstructionDisplacement localret_helper(Context* context, int pc) {
//   InstructionDisplacement disp;
//   int newpc;
//   JSValue v;
//   if (context->lcall_stack < 1) {
//     newpc = -1;
//   } else {
//     context->lcall_stack_ptr--;
//     v = get_array_prop(context, context->lcall_stack,
// 		       cint_to_number(context,
//                                       (cint) context->lcall_stack_ptr));
//     newpc = number_to_cint(v);
//   }
//   disp = (InstructionDisplacement) (newpc - pc);
//   return disp;
// }
// #define Localret() localret_helper(context, pc)
#define Localret(context)                                              \
  do {                                                                 \
    InstructionDisplacement disp;                                      \
    int newpc;                                                         \
    JSValue v;                                                         \
    if (context->lcall_stack < 1) {                                    \
      newpc = -1;                                                      \
    } else {                                                           \
      context->lcall_stack_ptr--;                                      \
      v = get_array_prop(context, context->lcall_stack,                \
            cint_to_number(context, (cint) context->lcall_stack_ptr)); \
      newpc = number_to_cint(v);                                       \
    }                                                                  \
    disp = (InstructionDisplacement) (newpc - pc);                     \
    set_pc_relative(disp);                                             \
  } while(0)

// static inline JSValue getlocal_helper(Context* context, int link, Subscript index) {
//   FunctionFrame* fr = get_lp(context);
//   int i;
//   for (i = 0; i < link; i++) {
//     fr = fframe_prev(fr);
//   }
//   return fframe_locals_idx(fr, index);
// }
// #define Getlocal(context,link, index)      getlocal_helper((context), link, index)
#define Getlocal(context, link, index)                     \
  do {                                                     \
    FunctionFrame* fr = get_lp((context));                 \
    int i;                                                 \
    for (i = 0; i < (link); i++) {                         \
      fr = fframe_prev(fr);                                \
    }                                                      \
    wrapped_return_value = fframe_locals_idx(fr, (index)); \
  } while(0)

// static inline JSValue getarguments_helper(Context* context, int link, Subscript index) {
//   FunctionFrame* fr = get_lp(context);
//   int i;
//   for (i = 0; i < link; i++) {
//     fr = fframe_prev(fr);
//   }
//   JSValue arguments = fframe_arguments(fr);
//   return get_array_element(context, arguments, index);
// }
// #define Getarguments(context, link, index)  getarguments_helper((context), link, index)
#define Getarguments(context, link, index)                                   \
  do {                                                                       \
    FunctionFrame* fr = get_lp((context));                                   \
    int i;                                                                   \
    for (i = 0; i < (link); i++) {                                           \
      fr = fframe_prev(fr);                                                  \
    }                                                                        \
    JSValue arguments = fframe_arguments(fr);                                \
    wrapped_return_value = get_array_element((context), arguments, (index)); \
  } while(0)

// static inline JSValue instanceof_helper(JSValue v1, JSValue v2) {
//   JSValue ctor_prototype = get_prop(v2, gconsts.g_string_prototype);
//   if (!is_jsobject(ctor_prototype))
//     return JS_FALSE;
//   JSValue __proto__ = v1;
//   JSValue ret = JS_FALSE;
//   while ((__proto__ = get_prop(__proto__, gconsts.g_string___proto__)) != JS_EMPTY)
//     if (__proto__ == ctor_prototype) {
//       ret = JS_TRUE;
//       break;
//     }
//   return ret;
// }
// #define Instanceof(v1, v2)          instanceof_helper(v1, v2)
#define Instanceof(v1, v2)                                                            \
  do {                                                                                \
    JSValue ctor_prototype = get_prop((v2), gconsts.g_string_prototype);              \
    if (!is_jsobject(ctor_prototype)){                                                \
      wrapped_return_value = JS_FALSE;                                                \
      break;                                                                          \
    }                                                                                 \
    JSValue __proto__ = (v1);                                                         \
    JSValue ret = JS_FALSE;                                                           \
    while ((__proto__ = get_prop(__proto__, gconsts.g_string___proto__)) != JS_EMPTY) \
      if (__proto__ == ctor_prototype) {                                              \
        ret = JS_TRUE;                                                                \
        break;                                                                        \
      }                                                                               \
    wrapped_return_value = ret;                                                       \
  } while(0)

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
