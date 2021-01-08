/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#define concat(context,s1,s2)      ejs_string_concat((context), (s1), (s2))
//#define toString(context,v)        to_string((context), (v))
/* #define FlonumToCdouble(f) to_double(context, (f)) */
#define getArrayProp(context,v1,v2)      get_array_prop((context), (v1), (v2))

#ifdef INLINE_CACHE
#define getObjectProp(context,v1,v2)     get_object_prop((context), (v1), (v2), NULL)
#else /* INLINE_CACHE */
#define getObjectProp(context,v1,v2)     get_object_prop((context), (v1), (v2))
#endif /* INLINE_CACHE */
#define SetArrayProp(context,v1,v2,v3)   set_array_prop((context), (v1), (v2), (v3))
#define SetObjectProp(context,v1,v2,v3)  set_object_prop((context), (v1), (v2), (v3))
#ifdef INLINE_CACHE
extern void set_object_prop_inl(Context *ctx, JSValue obj, JSValue prop, JSValue val, InlineCache *ic);
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
extern JSValue get_global_helper(Context* context, JSValue str);
#define Get_global(context,v1)              get_global_helper((context), v1)
#ifdef INLINE_CACHE
extern JSValue get_prop_object_inl_helper(Context *, InlineCache *, JSValue, JSValue);
#define Get_prop_object_inl(context, obj, prop)                           \
  get_prop_object_inl_helper(context, &insns->inl_cache, obj, prop)
#endif /* INLINE_CACHE */
#define GetProp(v1, v2)             get_prop((v1), (v2))
#define Get_globalobj(context)      ((context)->global)
#define Instanceof(v1, v2)          instanceof_helper(v1, v2)
#define Isundefined(v1)             true_false(is_undefined((v1)))
#define Isobject(v1)                is_object((v1))
#define Jump(d0)                    set_pc_relative((d0))
#define Lcall_stack_push()          lcall_stack_push(context, pc)
#define Lcall_stack_pop()           lcall_stack_pop(context, pc)

#define Nop()                       asm volatile("#NOP Instruction\n")
#define Not(obj)                    true_false(obj == JS_FALSE || obj == FIXNUM_ZERO || obj == gconsts.g_flonum_nan || obj == gconsts.g_string_empty)
#define Get_literal(d1)             get_literal(insns, d1)

#define Getarguments(context,link, index)  getarguments_helper((context), link, index)
#define Getlocal(context,link, index)      getlocal_helper((context), link, index)
#define Localret()                 localret_helper(context, pc)

#define Seta(v0)                   set_a(context, v0)
#define Setarg(i0, s1, v2)         setarg_helper(context, i0, s1, v2)
#define Setarray(dst, index, src)  (array_body_index(v0, s1) = v2)
#define Setfl(i0)                  setfl_helper(context, regbase, fp, i0, curfn, pc)
extern void setglobal_helper(Context* context, JSValue str, JSValue src);
#define Setglobal(str, src)        setglobal_helper(context, str, src)
#define Setlocal(link, index, v)   setlocal_helper(context, link, index, v)

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

struct Strtol_rettype{cint r0;/* return value of strtol */ char* r1; /* endptr of strtol */};
extern struct Strtol_rettype Strtol(char*, int);

struct Strtod_rettype{double r0;/* return value of strtod */ char* r1; /* endptr of strtod */};
extern struct Strtod_rettype Strtod(char*);

//struct GetProp_rettype{cint r0;/* return value of get_prop */ JSValue r1; /* obtained JSValue of get_prop */};
//extern struct GetProp_rettype GetProp(JSValue, JSValue);

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
                                                                          \
    save_context();                                                       \
    fr = new_frame(context, get_cf(context), get_lp(context), frame_len); \
    set_lp(context, fr);                                                  \
    update_context();                                                     \
                                                                          \
    if (make_arguments) {                                                 \
      JSValue *body;                                                      \
      num_of_args = get_ac(context);                                      \
      save_context();                                                     \
      args = new_array_object(context, DEBUG_NAME("arguments"),           \
                              gshapes.g_shape_Array, num_of_args);        \
      update_context();                                                   \
                                                                          \
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

#define NotImplemented()            NOT_IMPLEMENTED()
#define Nextpropnameidx(ite)        nextpropnameidx_helper(ite)

#define GOTO(l)                     goto l


/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
