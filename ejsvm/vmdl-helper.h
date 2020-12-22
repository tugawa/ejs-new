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
#define getObjectProp(context,v1,v2)     get_object_prop((context), (v1), (v2))
#define SetArrayProp(context,v1,v2,v3)   set_array_prop((context), (v1), (v2), (v3))
#define SetObjectProp(context,v1,v2,v3)  set_object_prop((context), (v1), (v2), (v3))

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
#define IsFixnumZero(v)        ((v) == cint_to_fixnum((cint)0))
#define Fixnum_Zero()          FIXNUM_ZERO
#define Flonum_Infinity()      gconsts.g_flonum_infinity
#define Flonum_NegInfinity()   gconsts.g_flonum_negative_infinity
#define Flonum_Nan()           gconsts.g_flonum_nan

#define Floor(d)  floor((d))
#define Ceil(d)   ceil((d))
#define LogicalRightShift(v1, v2)   ((uint32_t)(v1) >> (uint32_t)(v2))

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
#define Update_context()          update_context()
#define Save_context()            save_context()
#define New_normal_object(context)       new_normal_object((context))
#define New_normal_function(ss)   new_normal_function((context), ss)
#define Initialize_new_object(context,con, o)   initialize_new_object((context), con, o)
#define Next_insn_noincpc()       NEXT_INSN_NOINCPC()
#define Next_insn_incpc()         NEXT_INSN_INCPC()
#define JS_undefined()            JS_UNDEFINED

#define Get_a(context)                     get_a((context))
#define Get_err(context)                   get_err((context))
#define Get_global(context,v1)              get_global_helper((context), v1)
#define Get_globalobj(context)             ((context)->global)
#define Instanceof(v1, v2)          instanceof_helper(v1, v2)
#define Isundefined(v1)             true_false(is_undefined((v1)))
#define Isobject(v1)                is_object((v1))
#define Jump(d0)                    set_pc_relative((d0))
#define Lcall_stack_push()          lcall_stack_push(context, pc)
#define Lcall_stack_pop()           lcall_stack_pop(context, pc)

#define Nop()                       asm volatile("#NOP Instruction\n")
#define Not(obj)                    true_false(obj == JS_FALSE || obj == FIXNUM_ZERO || obj == gconsts.g_flonum_nan || obj == gconsts.g_string_empty)
#define Get_literal(d1)             get_literal(insns, d1)

#define New_normal_iterator(context,obj)  new_normal_iterator((context), obj)
#define Logexit(str)                LOG_EXIT(str)

#define Getarguments(context,link, index)  getarguments_helper((context), link, index)
#define Getlocal(context,link, index)      getlocal_helper((context), link, index)
#define Localret()                 localret_helper(context, pc)
#define Pushhandler(d0)            exhandler_stack_push(context, pc + d0, fp)

#define Seta(v0)                   set_a(context, v0)
#define Setarg(i0, s1, v2)         setarg_helper(context, i0, s1, v2)
#define Setarray(dst, index, src)  (array_body_index(v0, s1) = v2)
#define Setfl(i0)                  setfl_helper(context, regbase, fp, i0)
#define Setglobal(str, src)        setglobal_helper(context, str, src)
#define Setlocal(link, index, v)   setlocal_helper(context, link, index, v)

#define Strtol(p, q, n)            strtol((p), &(q), n)
#define Strtod(p, q)               strtod((p), &(q))
#define IsEmptyCstring(str)        ((str)[0] == '\0')
#define CstrToString(cstr)         cstr_to_string(NULL, (cstr))
#define GetProp(v1, v2, v3)        get_prop((v1), (v2), &(v3))
#define PutLnChar()                putchar('\n')
#define AllocateJSArray(context, size)  ((JSValue *)gc_malloc((context), sizeof(JSValue) * (size), HTAG_ARRAY_DATA))
#define AllocateCintArray(size)  ((cint *)malloc(sizeof(cint) * (size))
#define AllocateCdoubleArray(size)  ((double *)malloc(sizeof(double) * (size))

#define Pophandler()					\
  int newpc, handler_fp;				\
  exhandler_stack_pop(context, &newpc, &handler_fp);
#define Poplocal()				\
  int newpc;					\
  lcall_stack_pop(context, &newpc);

#define Ret()							\
  if (fp == border) return 1;					\
  JSValue* stack = &get_stack(context, 0);			\
  restore_special_registers(context, stack, fp - 4);

/*
#define Newframe(frame_len)						\
  FunctionFrame* fr = new_frame(context, get_cf(context),		\
				get_lp(context), frame_len);		\
  set_lp(context, fr);

#define Makearguments(make_arguments)					\
  int num_of_args = get_ac(context);					\
  save_context();							\
  JSValue args = new_normal_array_with_size(context, num_of_args);	\
  update_context();							\
  int i;								\
  for (i = 0; i < num_of_args; i++)					\
    array_body_index(args, i) = regbase[i + 2];				\
  fframe_arguments(fr) = args;						\
  fframe_locals_idx(fr, 0) = args;
*/

#define Newframe(frame_len, make_arguments)						\
  save_context(); \
  FunctionFrame* fr = new_frame(context, get_cf(context),		\
	get_lp(context), frame_len);		\
  set_lp(context, fr); \
  update_context(); \
  if(make_arguments){  \
    int num_of_args = get_ac(context);					\
    save_context();							\
    JSValue args = new_normal_array_with_size(context, num_of_args);	\
    update_context();							\
    int i;								\
    for (i = 0; i < num_of_args; i++)					\
      array_body_index(args, i) = regbase[i + 2];				\
    fframe_arguments(fr) = args;						\
    fframe_locals_idx(fr, 0) = args; \
  }


#define Throw(context)							\
  int newpc, handler_fp;					\
  exhandler_stack_pop(context, &newpc, &handler_fp);		\
  while (handler_fp != fp) {					\
    JSValue *stack = &get_stack(context, 0);			\
    restore_special_registers(context, stack, fp - 4);		\
    set_sp(context, fp - 5);					\
    update_context();						\
  }								\
  Displacement disp = (Displacement) (newpc - pc);		\
  set_pc_relative(disp);


#define NotImplemented()            NOT_IMPLEMENTED()
#define Nextpropnameidx(ite)        nextpropnameidx_helper(ite)

#define GOTO(l)                     goto l


/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
