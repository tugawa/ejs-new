/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN
#include "header.h"

static void exhandler_stack_push(Context* context, int pc, int fp);
static int exhandler_stack_pop(Context* context, int *pc, int *fp);
static void lcall_stack_push(Context* context, int pc);
static int lcall_stack_pop(Context* context, int *pc);

#define NOT_IMPLEMENTED()						\
  LOG_EXIT("Sorry, instruction %s has not been implemented yet\n",      \
           insn_nemonic(get_opcode(insn)))

#ifdef PROFILE
static char *typename(JSValue v) {
  if (is_fixnum(v)) return "fixnum";
  if (is_flonum(v)) return "flonum";
  if (is_string(v)) return "string";
  if (is_special(v)) return "special";
  if (is_simple_object(v)) return "simple_object";
  if (is_array(v)) return "array";
  if (is_function(v)) return "function";
  if (is_builtin(v)) return "builtin";
  if (is_iterator(v)) return "iterator";
  if (is_number_object(v)) return "number_object";
  if (is_boolean_object(v)) return "boolean_object";
  if (is_string_object(v)) return "string_object";
#ifdef USE_REGEXP
  if (is_regexp(v)) return "regexp";
#endif
  return "unknown";
}

#define INSN_COUNT0(iname)                              \
  (profile_flag == TRUE && insns->logflag == TRUE &&    \
   fprintf(prof_stream, "OPERAND: %s\n", #iname))
#define INSN_COUNT1(iname, v0)                                          \
  (profile_flag == TRUE && insns->logflag == TRUE &&                    \
   fprintf(prof_stream, "OPERAND: %s %s\n", #iname, typename(v0)))
#define INSN_COUNT2(iname, v0, v1)                      \
  (profile_flag == TRUE && insns->logflag == TRUE &&    \
   fprintf(prof_stream, "OPERAND: %s %s %s\n", #iname,  \
           typename(v0), typename(v1)))
#define INSN_COUNT3(iname, v0, v1, v2)                          \
  (profile_flag == TRUE && insns->logflag == TRUE &&            \
   fprintf(prof_stream, "OPERAND: %s %s %s %s\n", #iname,       \
           typename(v0), typename(v1), typename(v2)))
#else
#define INSN_COUNT0(insn)
#define INSN_COUNT1(insn, v0)
#define INSN_COUNT2(insn, v0, v1)
#define INSN_COUNT3(insn, v0, v1, v2)
#endif

inline void make_ilabel(FunctionTable *curfn, void *const *jt) {
  int i, n_insns;
  Instruction *insns;
  n_insns = curfn->n_insns;
  insns = curfn->insns;
  for (i = 0; i < n_insns; i++)
    insns[i].ilabel = jt[get_opcode(insns[i].code)];
  curfn->ilabel_created = true;
}

#define STRCON(x,y)  x #y
#define INCPC()      do { pc++; insns++; } while (0)
#define PRINTPC()    fprintf(stderr, "pc:%d\n", pc)

#ifdef DEBUG
#define INSNLOAD()                                      \
  do {                                                  \
    insn = insns->code;                                 \
    if (trace_flag == TRUE) {                           \
      printf("pc = %d, insn = %s, fp = %d\n",           \
             pc, insn_nemonic(get_opcode(insn)), fp);   \
      if (get_opcode(insn) == STRING) {                 \
        Displacement disp = get_big_disp(insn);         \
        JSValue s = get_literal(insns, disp);           \
        printf("   %s\n", string_to_cstr(s));           \
      }                                                 \
    }                                                   \
  } while (0)
#else /* DEBUG */
/*
 * #define INSNLOAD()                                                   \
 *   do {                                                               \
 *     insn = insns->code;                                              \
 *     if (trace_flag == TRUE) {                                        \
 *       printf("pc = %d, insn = %s, fp = %d\n",                        \
 *              pc, insn_nemonic(get_opcode(insn)), fp);                \
 *       fflush(stdout);                                                \
 *     }                                                                \
 *   } while (0)
 */
#define INSNLOAD() (insn = insns->code)
#endif /* DEBUG */

#ifdef PROFILE
#define ENTER_INSN(x)                                                   \
  do {                                                                  \
    if (insns->logflag == TRUE) headcount = 0, insns->count++;          \
    asm volatile(STRCON(STRCON("#enter insn, loc = ", (x)), \n\t));     \
  } while (0)
#else
#define ENTER_INSN(x)                                                   \
  asm volatile(STRCON(STRCON("#enter insn, loc = ", (x)), \n\t))
#endif

#ifdef USE_ASM2
/* if we erase the ``goto'', the code does not work ??? */
#define NEXT_INSN()                             \
  asm volatile("jmp *%0"::"r"(*insns));         \
  goto *(insns->ilabel)
#else
#define NEXT_INSN()                             \
  goto *(insns->ilabel)
#endif

#define GET_NEXT_INSN_ADDR(ins)  (jump_table[get_opcode(ins)])

#define INSN_PRINT(x)                                           \
  asm volatile(STRCON(STRCON("#jump, loc = ", (x)), \n\t))

#define NEXT_INSN_ASM(addr)                             \
  asm("jmp *%0\n\t# -- inserted main.c" : : "A" (addr))


#ifdef USE_ASM

#define NEXT_INSN_INCPC() do {                  \
    INCPC();                                    \
    INSNLOAD();                                 \
    NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(insn))     \
      } while (0)

#define NEXT_INSN_NOINCPC() do {                \
    INSNLOAD();                                 \
    NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(insn))     \
      } while (0)

#else

#define NEXT_INSN_INCPC()   do { INCPC(); INSNLOAD(); NEXT_INSN(); } while(0)
#define NEXT_INSN_NOINCPC() do { INSNLOAD(); NEXT_INSN(); } while(0)

#endif /* USE_ASM */

#define save_context() do			\
    {						\
      set_cf(context, curfn);			\
      set_pc(context, pc);                      \
      set_fp(context,fp);                       \
    } while(0)

#define update_context() do {                           \
    curfn = get_cf(context);                            \
    codesize = ftab_n_insns(curfn);                     \
    pc = get_pc(context);                               \
    fp = get_fp(context);                               \
    insns = curfn->insns + pc;                          \
    regbase = (JSValue *)&get_stack(context, fp) - 1;   \
    if (!curfn->ilabel_created)                         \
      make_ilabel(curfn, jump_table);                   \
  } while (0)

#define load_regs(insn, dst, r1, r2, v1, v2)    \
  dst = get_first_operand_reg(insn),            \
    r1 = get_second_operand_reg(insn),          \
    r2 = get_third_operand_reg(insn),           \
    v1 = regbase[r1],                           \
    v2 = regbase[r2]

#define set_pc_relative(d) (pc += (d), insns += (d))

/*
 * executes the main loop of the vm as a threaded code
 */
int vmrun_threaded(Context* context, int border) {
  FunctionTable *curfn;
  int codesize;
  int pc;
  int fp;
  Instruction *insns;
  JSValue *regbase;
  Bytecode insn;
  /* JSValue *locals = NULL; */
  static InsnLabel jump_table[] = {
#include "instructions-label.h"
  };
#ifdef PROFILE
  int headcount = 0;
#endif

  update_context();
  /*
   * if (get_lp(context) != NULL)
   *   locals = get_lp(context)->locals;
   */

  /*
   * goes to the first instruction
   */
#ifdef USE_ASM
  INSNLOAD();
  NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(curfn->start[pc]));
#else
  ENTER_INSN(__LINE__);
  INSNLOAD();
  NEXT_INSN();
#endif

#include "vmloop-cases.inc"

  /*
   * Now the following case branches are automatically generated in
   * the file named ``vmloop-cases.def''.
   * We leave these branches by surrounding ``#if 0'' and ``#endif''
   * for the sake of coping with emergency cases.
   */

#if 0
 I_FIXNUM:
  ENTER_INSN(__LINE__);
  {
    Register dst = get_first_operand_reg(insn);
    int64_t imm = get_small_immediate(insn);
#include "insns/fixnum.def"
  }
  NEXT_INSN_INCPC();

 I_SPECCONST:
  ENTER_INSN(__LINE__);
  {
    Register dst = get_first_operand_reg(insn);
    int64_t imm = get_small_immediate(insn);
#include "insns/specconst.def"
  }
  NEXT_INSN_INCPC();

 I_STRING:
 I_REGEXP:
 I_NUMBER:
  ENTER_INSN(__LINE__);
  {
    Register dst = get_first_operand_reg(insn);
    Displacement disp = get_big_disp(insn);
#include "insns/number.def"
  }
  NEXT_INSN_INCPC();

 I_ADD:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/add.def"
  }
  NEXT_INSN_INCPC();

 I_SUB:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/sub.def"
  }
  NEXT_INSN_INCPC();

 I_MUL:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/mul.def"
  }
  NEXT_INSN_INCPC();

 I_DIV:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/div.def"
  }
  NEXT_INSN_INCPC();

 I_MOD:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/mod.def"
  }
  NEXT_INSN_INCPC();

 I_BITAND:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/bitand.def"
  }
  NEXT_INSN_INCPC();

 I_BITOR:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/bitor.def"
  }
  NEXT_INSN_INCPC();

 I_LEFTSHIFT:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/leftshift.def"
  }
  NEXT_INSN_INCPC();

 I_RIGHTSHIFT:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/rightshift.def"
  }
  NEXT_INSN_INCPC();

 I_UNSIGNEDRIGHTSHIFT:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/unsignedrightshift.def"
  }
  NEXT_INSN_INCPC();

 I_LESSTHAN:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/lessthan.def"
  }
  NEXT_INSN_INCPC();

 I_LESSTHANEQUAL:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/lessthanequal.def"
  }
  NEXT_INSN_INCPC();

 I_EQ:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/eq.def"
  }
  NEXT_INSN_INCPC();

 I_EQUAL:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/equal.def"
  }
  NEXT_INSN_INCPC();

 I_GETPROP:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/getprop.def"
  }
  NEXT_INSN_INCPC();

 I_SETPROP:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/setprop.def"
  }
  NEXT_INSN_INCPC();

 I_SETARRAY:
  ENTER_INSN(__LINE__);
  {
    Register dst = get_first_operand_reg(insn);
    Subscript s = get_second_operand_subscr(insn);
    Register src = get_third_operand_reg(insn);
#include "insns/setarray.def"
  }
  NEXT_INSN_INCPC();

 I_GETGLOBAL:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/getglobal.def"
  }
  NEXT_INSN_INCPC();

 I_SETGLOBAL:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/setglobal.def"
  }
  NEXT_INSN_INCPC();

 I_INSTANCEOF:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/instanceof.def"
  }
  NEXT_INSN_INCPC();

 I_MOVE:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/move.def"
  }
  NEXT_INSN_INCPC();

 I_TYPEOF:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/typeof.def"
  }
  NEXT_INSN_INCPC();

 I_NOT:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/not.def"
  }
  NEXT_INSN_INCPC();

 I_NEW:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/new.def"
  }
  NEXT_INSN_INCPC();

 I_GETIDX:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/getidx.def"
  }
  NEXT_INSN_INCPC();

 I_ISUNDEF:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/isundef.def"
  }
  NEXT_INSN_INCPC();

 I_ISOBJECT:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/isobject.def"
  }
  NEXT_INSN_INCPC();

 I_SETFL:
  ENTER_INSN(__LINE__);
  {
    int newfl = get_first_operand_int(insn);
#include "insns/setfl.def"
  }
  NEXT_INSN_INCPC();

 I_SETA:
  ENTER_INSN(__LINE__);
  {
    Register r = get_first_operand_reg(insn);
#include "insns/seta.def"
  }
  NEXT_INSN_INCPC();

 I_GETA:
  ENTER_INSN(__LINE__);
  {
    Register r = get_first_operand_reg(insn);
#include "insns/geta.def"
  }
  NEXT_INSN_INCPC();

 I_GETERR:
  ENTER_INSN(__LINE__);
  {
    Register r = get_first_operand_reg(insn);
#include "insns/geterr.def"
  }
  NEXT_INSN_INCPC();

 I_GETGLOBALOBJ:
  ENTER_INSN(__LINE__);
  {
    Register r = get_first_operand_reg(insn);
#include "insns/getglobalobj.def"
  }
  NEXT_INSN_INCPC();

 I_NEWARGS:
  ENTER_INSN(__LINE__);
  {
#include "insns/newargs.def"
  }
  NEXT_INSN_INCPC();

 I_NEWFRAME:
  ENTER_INSN(__LINE__);
  {
#include "insns/newframe.def"
  }
  NEXT_INSN_INCPC();

 I_RET:
  ENTER_INSN(__LINE__);
  {
#include "insns/ret.def"
  }
  NEXT_INSN_INCPC();

 I_NOP:
  ENTER_INSN(__LINE__);
  {
#include "insns/nop.def"
  }
  NEXT_INSN_INCPC();

 I_JUMP:
  ENTER_INSN(__LINE__);
  {
    Displacement disp = get_first_operand_disp(insn);
#include "insns/jump.def"
  }
  NEXT_INSN_NOINCPC();

 I_JUMPTRUE:
  ENTER_INSN(__LINE__);
  {
    Register r = get_first_operand_reg(insn);
#include "insns/jumptrue.def"
  }
  NEXT_INSN_INCPC();

 I_JUMPFALSE:
  ENTER_INSN(__LINE__);
  {
    Register r = get_first_operand_reg(insn);
#include "insns/jumpfalse.def"
  }
  NEXT_INSN_INCPC();

 I_GETLOCAL:
  ENTER_INSN(__LINE__);
  {
    Register dst = get_first_operand_reg(insn);
    int link = get_second_operand_int(insn);
    Subscript index = get_third_operand_subscr(insn);
#include "insns/getlocal.def"
  }
  NEXT_INSN_INCPC();

 I_SETLOCAL:
  ENTER_INSN(__LINE__);
  {
    int link = get_first_operand_int(insn);
    Subscript index = get_second_operand_subscr(insn);
    Register src = get_third_operand_reg(insn);
#include "insns/setlocal.def"
  }
  NEXT_INSN_INCPC();

 I_MAKECLOSURE:
  ENTER_INSN(__LINE__);
  {
    Register dst = get_first_operand_reg(insn);
    Subscript index = get_second_operand_subscr(insn);
#include "insns/makeclosure.def"
  }
  NEXT_INSN_INCPC();

 I_MAKEITERATOR:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/makeiterator.def"
  }
  NEXT_INSN_INCPC();

 I_NEXTPROPNAME:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
    Register r2 = get_third_operand_reg(insn);
#include "insns/nextpropname.def"
  }
  NEXT_INSN_INCPC();

 I_NEXTPROPNAMEIDX:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    Register r1 = get_second_operand_reg(insn);
#include "insns/nextpropnameidx.def"
  }
  NEXT_INSN_INCPC();
  
 I_SEND:
 I_NEWSEND:
 I_CALL:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    int nargs = get_second_operand_int(insn);
#include "insns/call.def"
  }
  NEXT_INSN_INCPC();

 I_TAILSEND:
 I_TAILCALL:
  ENTER_INSN(__LINE__);
  {
    Register r0 = get_first_operand_reg(insn);
    int nargs = get_second_operand_int(insn);
#include "insns/tailcall.def"
  }
  NEXT_INSN_INCPC();

 I_PUSHHANDLER:
  ENTER_INSN(__LINE__);
  {
    Displacement disp = get_first_operand_disp(insn);
#include "insns/pushhandler.def"
  }
  NEXT_INSN_INCPC();

 I_POPHANDLER:
  ENTER_INSN(__LINE__);
  {
#include "insns/pophandler.def"
  }
  NEXT_INSN_INCPC();

 I_THROW:
  ENTER_INSN(__LINE__);
  {
#include "insns/throw.def"
  }
  NEXT_INSN_NOINCPC();

 I_LOCALCALL:
  ENTER_INSN(__LINE__);
  {
    Displacement disp = get_first_operand_disp(insn);
#include "insns/localcall.def"
  }
  NEXT_INSN_NOINCPC();

 I_LOCALRET:
  ENTER_INSN(__LINE__);
  {
#include "insns/localret.def"
  }
  NEXT_INSN_INCPC();

 I_POPLOCAL:
  ENTER_INSN(__LINE__);
  {
#include "insns/poplocal.def"
  }
  NEXT_INSN_INCPC();

 I_ERROR:
  ENTER_INSN(__LINE__);
  {
    Register dst = get_first_operand_reg(insn);
    Displacement disp = get_big_disp(insn);
#include "insns/error.def"
  }
  NEXT_INSN_INCPC();

 I_UNKNOWN:
  ENTER_INSN(__LINE__);
  {
#include "insns/unknown.def"
  }
  NEXT_INSN_INCPC();

 I_END:
  ENTER_INSN(__LINE__);
  {
#include "insns/end.def"
  }
  NEXT_INSN_INCPC();

 I_LIGHTCALL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LIGHTSEND:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LIGHTTAILCALL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LIGHTTAILSEND:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSTRFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFIXSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSTRFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFLOSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFIXSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSPEFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSTRSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSPESTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDFLOSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSPEFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_ADDSPESPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSTRFIX:
  ENTER_INSN(__LINE__);
  NEXT_INSN_INCPC();

 I_SUBFIXSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSTRFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBFLOSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBFIXSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSPEFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSTRSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSPESTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBFLOSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSPEFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SUBSPESPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFIXFIXSMALL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSTRFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFIXSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSTRFLO:
  ENTER_INSN(__LINE__);
  NEXT_INSN_INCPC();

 I_MULFLOSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFIXSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSPEFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSTRSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSPESTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULFLOSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSPEFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MULSPESPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVFLOFIX:
  ENTER_INSN(__LINE__);
  NEXT_INSN_INCPC();

 I_DIVFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSTRFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVFIXSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSTRFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVFLOSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVFIXSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSPEFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSTRSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSPESTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVFLOSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSPEFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_DIVSPESPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODSTRFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFIXSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODSTRFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFLOSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFIXSPE:
  ENTER_INSN(__LINE__);
  NEXT_INSN_INCPC();

 I_MODSPEFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODSTRSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODSPESTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODFLOSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODSPEFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_MODSPESPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANEQUALFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANEQUALFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANEQUALFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANEQUALFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_LESSTHANEQUALSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFIXFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFIXFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFLOFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFLOFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSTRSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSTRFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFIXSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSTRFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFLOSTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFIXSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSPEFIX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSTRSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSPESTR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALFLOSPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSPEFLO:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALSPESPE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_EQUALOBJOBJ:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_FASTGETGLOBAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SLOWGETGLOBAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_FASTSETGLOBAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SLOWSETGLOBAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_FASTGETLOCAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_SLOWGETLOCAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

#ifdef J5MODE
 I_ARG:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_PRINT:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

 I_NEWLINE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

#endif /* J5MODE */

#endif /* if 0 */
}

static void exhandler_stack_push(Context* context, int pc, int fp)
{
  cint sp = context->exhandler_stack_ptr;

  set_array_index_value(context, context->exhandler_stack, sp++,
			cint_to_number((cint) pc), FALSE);
  set_array_index_value(context, context->exhandler_stack, sp++,
			cint_to_number((cint) fp), FALSE);
  context->exhandler_stack_ptr = sp;
}

static int exhandler_stack_pop(Context* context, int *pc, int *fp)
{
  cint sp = context->exhandler_stack_ptr;
  JSValue v;
  if (sp < 2)
    return -1;
  sp--;
  v = get_array_prop(context, context->exhandler_stack, cint_to_number(sp));
  *fp = number_to_cint(v);
  sp--;
  v = get_array_prop(context, context->exhandler_stack, cint_to_number(sp));
  *pc = number_to_cint(v);
  context->exhandler_stack_ptr = sp;
  return 0;
}

static void lcall_stack_push(Context* context, int pc)
{
  set_array_index_value(context, context->lcall_stack,
			context->lcall_stack_ptr++,
			cint_to_number((cint) pc), FALSE);
}

static int lcall_stack_pop(Context* context, int *pc)
{
  JSValue v;
  if (context->lcall_stack < 1)
    return -1;
  context->lcall_stack_ptr--;
  v = get_array_prop(context, context->lcall_stack,
		     cint_to_number((cint) context->lcall_stack_ptr));
  *pc = number_to_cint(v);
  return 0;
}

