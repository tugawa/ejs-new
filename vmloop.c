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

#ifdef PROFILE
int logflag_stack[100];
int logflag_sp = -1;
#define push_logflag(f) logflag_stack[++logflag_sp] = (f|forcelog_flag)
#define pop_logflag()   logflag_sp--
#else
#define push_logflag(f)
#define pop_logflag()
#endif /* PROFILE */

#define NOT_IMPLEMENTED()                                               \
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

#define INCPC()      do { pc++; insns++; } while (0)
#define PRINTPC()    fprintf(stderr, "pc:%d\n", pc)

#ifdef DEBUG
#define INSNLOAD()                                      \
  do {                                                  \
    insn = insns->code;                                 \
    if (trace_flag == TRUE) {                           \
      printf("pc = %d, fp = %d insn = %s",              \
             pc, fp, insn_nemonic(get_opcode(insn)));   \
      if (get_opcode(insn) == STRING) {                 \
        Displacement disp = get_big_disp(insn);         \
        JSValue s = get_literal(insns, disp);           \
        printf("   %s\n", string_to_cstr(s));           \
      } else if (get_opcode(insn) == SETGLOBAL) {       \
        int r0 = get_first_operand_reg(insn);           \
        int r1 = get_second_operand_reg(insn);          \
        JSValue v0 = regbase[r0];                       \
        JSValue v1 = regbase[r1];                       \
        printf(" %d [%llx ", r0, v0);                   \
        simple_print(v0);                               \
        printf("] ");                                   \
        printf(" %d [%llx ", r1, v1);                   \
        simple_print(v1);                               \
        printf("]\n");                                  \
      } else if (get_opcode(insn) == GETGLOBAL) {       \
        int r0 = get_first_operand_reg(insn);           \
        int r1 = get_second_operand_reg(insn);          \
        JSValue v1 = regbase[r1];                       \
        printf(" %d ", r0);                             \
        printf(" %d [%llx ", r1, v1);                   \
        simple_print(v1);                               \
        printf("]\n");                                  \
      }else {                                           \
        printf(" %d %d %d\n",                           \
          get_first_operand_reg(insn),                  \
               get_second_operand_reg(insn),            \
               get_third_operand_reg(insn));            \
      }                                                 \
    }                                                   \
  } while (0)
#else /* DEBUG */
#define INSNLOAD() (insn = insns->code)
#endif /* DEBUG */

#ifdef PROFILE
#define ENTER_INSN(x)                                                   \
  do {                                                                  \
    push_logflag(insns->logflag);                                       \
    if (insns->logflag == TRUE) headcount = 0, insns->count++;          \
    asm volatile("#enter insn, loc = " #x "\n\t");                    \
  } while (0)
#else
#define ENTER_INSN(x)                                   \
  asm volatile("#enter insn, loc = " #x "\n\t")
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

#define INSN_PRINT(x)                           \
  asm volatile("#jump, loc = " #x "\n\t")

#define NEXT_INSN_ASM(addr)                             \
  asm("jmp *%0\n\t# -- inserted main.c" : : "A" (addr))

#ifdef USE_ASM

#define NEXT_INSN_INCPC() do {                  \
    pop_logflag();                              \
    INCPC();                                    \
    INSNLOAD();                                 \
    NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(insn))     \
  } while (0)

#define NEXT_INSN_NOINCPC() do {                \
    pop_logflag();                              \
    INSNLOAD();                                 \
    NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(insn))     \
  } while (0)

#else /* USE_ASM */

#define NEXT_INSN_INCPC()   do {                                \
    pop_logflag();                                              \
    INCPC();                                                    \
    INSNLOAD();                                                 \
    NEXT_INSN();                                                \
  } while(0)
#define NEXT_INSN_NOINCPC() do {                \
    pop_logflag();                              \
    INSNLOAD();                                 \
    NEXT_INSN();                                \
  } while(0)

#endif /* USE_ASM */

#define save_context() do                            \
    {                                                \
      set_cf(context, curfn);                        \
      set_pc(context, pc);                           \
      set_fp(context,fp);                            \
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
#endif /* PROFILE */

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

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
