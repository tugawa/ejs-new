#define concat(s1,s2)      ejs_string_concat(context, (s1), (s2))
#define toString(v)        to_string(context, (v))
/* #define FlonumToCdouble(f) to_double(context, (f)) */
#define FlonumToCdouble(v) flonum_to_double((v))
#define CdoubleToNumber(x) double_to_number((x))
#define CdoubleToFlonum(x) double_to_flonum((x))
#define FixnumToCint(v)    fixnum_to_cint((v))
#define FixnumToCdouble(v) fixnum_to_double((v))
#define CintToNumber(x)    cint_to_number((x))
#define CintToFixnum(x)    cint_to_fixnum((x))
#define FlonumToCint(v)    flonum_to_cint((v))
#define toCdouble(v)       to_double(context, (v))
#define toNumber(v)        to_number(context, (v))
#define toObject(v)        to_object(context, (v))
#define getArrayProp(v1,v2)      get_array_prop(context, (v1), (v2))
#define getObjectProp(v1,v2)     get_object_prop(context, (v1), (v2))
#define SetArrayProp(v1,v2,v3)   set_array_prop(context, (v1), (v2), (v3))
#define SetObjectProp(v1,v2,v3)  set_object_prop(context, (v1), (v2), (v3))
#define String_to_cstr(v)  string_to_cstr((v))


#define FIXNUM_EQ(v1,v2) ((int64_t) (v1) == (int64_t) (v2))
#define FIXNUM_LESSTHAN(v1,v2)   ((int64_t) (v1) < (int64_t) (v2))
#define FIXNUM_LESSTHANEQ(v1,v2) ((int64_t) (v1) <= (int64_t) (v2))
#define FIXNUM_AND(v1,v2)        ((int64_t) (v1) & (int64_t) (v2))
#define FIXNUM_OR(v1,v2)        ((int64_t) (v1) | (int64_t) (v2))


#define Object_to_primitive_hint_number(v) object_to_primitive(context, (v) ,HINT_NUMBER)
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
#define Set_fp()        set_fp(context, fp)
#define Set_pc()        set_pc(context, pc)
#define Try_gc()        try_gc(context)
#define Call_function(fn, n, sendp)        call_function(context, (fn), (n), (sendp))
#define Call_builtin(fn, n, sendp, newp)   call_builtin(context, (fn), (n), (sendp), (newp))
#define Tailcall_function(fn, n, sendp)    tailcall_function(context, (fn), (n), (sendp))
#define Tailcall_builtin(fn, n, sendp)   tailcall_builtin(context, (fn), (n), (sendp), FALSE)
#define Update_context()          update_context()
#define Save_context()            save_context()
#define New_normal_object()       new_normal_object(context)
#define Initialize_new_object(con, o)   initialize_new_object(context, con, o)
#define Next_insn_noincpc()       NEXT_INSN_NOINCPC()
#define Next_insn_incpc()         NEXT_INSN_INCPC()
#define JS_undefined()            JS_UNDEFINED