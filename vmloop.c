/*
   vmloop.c

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-17
     Hideya Iwasaki, 2016-17

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/

#include "prefix.h"
#define EXTERN
#include "header.h"

static void exhandler_stack_push(Context* context, int pc, int fp);
static int exhandler_stack_pop(Context* context, int *pc, int *fp);
static void lcall_stack_push(Context* context, int pc);
static int lcall_stack_pop(Context* context, int *pc);

#define NOT_IMPLEMENTED()						\
  LOG_EXIT("Sorry, instruction %s has not been implemented yet\n", insn_nemonic(get_opcode(insn)))

inline void make_insn_ptr(FunctionTable *curfn, void *const *jt) {
  int i, n_insns;
  Instruction *insns;
//  void **ptr = curfn->insn_ptr;
  InsnLabel *ptr = curfn->insn_ptr;
  n_insns = curfn->n_insns;
  insns = curfn->insns;
  for (i = 0; i < n_insns; i++)
    ptr[i] = jt[get_opcode(insns[i].code)];
  curfn->insn_ptr_created = true;
}

#define STRCON(x,y)  x #y
#define INCPC()      do { pc++; insn_ptr++; insns++; } while (0)
#define PRINTPC()    fprintf(stderr, "pc:%d\n", pc)
#define INCEXECUTECOUNT() insns->executeCount++
//#define INSNLOAD()   insn = insns->code
//#define INSNLOAD()   (insn = insns->code, printf("pc = %d, insn = %s\n", pc, insn_nemonic(get_opcode(insn))))
#define INSNLOAD()                                                   \
  do {                                                               \
    insn = insns->code;                                              \
    if (trace_flag == TRUE)                                          \
      printf("pc = %d, insn = %s, fp = %d\n",                        \
             pc, insn_nemonic(get_opcode(insn)), fp);                \
  } while (0)

// defines ENTER_INSN(x)
//
#ifdef USE_ASM
#define ENTER_INSN(x)                                                \
do {                                                                 \
  INCEXECUTECOUNT();                                                 \
  asm volatile(STRCON(STRCON("#enter insn, loc = ", (x)), \n\t));    \
} while (0)

#else
#ifdef PRINT_QUICKENING_COUNT
#define ENTER_INSN(x)                                                \
do{                                                                  \
  INCEXECUTECOUNT();                                                 \
  asm volatile(STRCON(STRCON("#enter insn, loc = ", (x)), \n\t));    \
} while (0)

#else
#define ENTER_INSN(x)                                                \
  asm volatile(STRCON(STRCON("#enter insn, loc = ", (x)), \n\t))
#endif // PRINT_QUICKENING_COUNT
#endif // USE_ASM

// defines NEXT_INSN()
//
#ifdef USE_ASM2
// if we erase the ``goto'', the code does not work ???
#define NEXT_INSN() \
  asm volatile("jmp *%0"::"r"(*insn_ptr)); \
  goto **insn_ptr
#else
#define NEXT_INSN() \
  goto **insn_ptr
#endif

#define GET_NEXT_INSN_ADDR(ins)  (jump_table[get_opcode(ins)])

#define INSN_PRINT(x) \
  asm volatile(STRCON(STRCON("#jump, loc = ", (x)), \n\t))

#define NEXT_INSN_ASM(addr) \
  asm("jmp *%0\n\t# -- inserted main.c" : : "A" (addr))


// defines NEXT_INSN_INCPC() and NEXT_INSN_NOINCPC()
//
#ifdef USE_ASM

#define NEXT_INSN_INCPC() do {                                 \
  INCPC();                                                     \
  INSNLOAD();                                                  \
  NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(insn))                      \
} while (0)

#define NEXT_INSN_NOINCPC() do {                               \
  INSNLOAD();                                                  \
  NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(insn))                      \
} while (0)

#else

#define NEXT_INSN_INCPC()   INCPC(); INSNLOAD(); NEXT_INSN()
#define NEXT_INSN_NOINCPC() INSNLOAD(); NEXT_INSN()

#endif // USE_ASM

#define save_context() do			\
{						\
  set_cf(context, curfn);			\
  set_pc(context, pc);				\
  set_fp(context,fp);				\
} while(0)

#define update_context() do {                       \
  curfn = get_cf(context);                          \
  codesize = ftab_n_insns(curfn);                   \
  pc = get_pc(context);                             \
  fp = get_fp(context);                             \
  insns = curfn->insns + pc;                        \
  regbase = (JSValue *)&get_stack(context, fp) - 1; \
  if (!curfn->insn_ptr_created)                     \
    make_insn_ptr(curfn, jump_table);               \
  insn_ptr = curfn->insn_ptr + pc;                  \
} while (0)

#define load_regs(insn, dst, r1, r2, v1, v2) \
  dst = get_first_operand_reg(insn), \
  r1 = get_second_operand_reg(insn), \
  r2 = get_third_operand_reg(insn), \
  v1 = regbase[r1], \
  v2 = regbase[r2]

#define set_pc_relative(d) (pc += (d), insns += (d), insn_ptr += (d))

// executes the main loop of the vm as a threaded code
//
int vmrun_threaded(Context* context, int border) {
  FunctionTable *curfn;
  int codesize;
  int pc;
  int fp;
  Instruction *insns;
  JSValue *regbase;
  InsnLabel *insn_ptr;
  Bytecode insn;
  // JSValue *locals = NULL;
  static InsnLabel jump_table[] = {
#include "instructions-label.h"
  };

  update_context();
  /*
  if (get_lp(context) != NULL)
    locals = get_lp(context)->locals;
  */

// goes to the first instruction
//
#ifdef USE_ASM
  INSNLOAD();
  NEXT_INSN_ASM(GET_NEXT_INSN_ADDR(curfn->start[pc]));
#else
  ENTER_INSN(__LINE__);
  INSNLOAD();
  NEXT_INSN();
#endif

I_FIXNUM:
  /*
     fixnum dst imm
       dst : destination register
       imm : immediate value of the fixnum
     $dst = imm
   */
  ENTER_INSN(__LINE__);
  {
    Register reg = get_first_operand_reg(insn);
    regbase[reg] = cint_to_fixnum((cint)get_small_immediate(insn));
  }
  NEXT_INSN_INCPC();

I_SPECCONST:
  /*
     specconst dst specimm
      dst : destination register
       specimm : immediate value of the special constant
     $dst = specimm
   */
  ENTER_INSN(__LINE__);
  {
    Register reg = get_first_operand_reg(insn);
    regbase[reg] = get_small_immediate(insn);
  }
  NEXT_INSN_INCPC();

I_NUMBER:
I_STRING:
I_REGEXP:
  /*
     number dst disp
     string dst disp
     regexp dst disp
       dst : destination register
       disp : displacement of the constant position from the pc
     $dst = insns[pc + disp]
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    Displacement disp;
    dst = get_first_operand_reg(insn);
    disp = get_big_disp(insn);
    regbase[dst] = insns[disp].code;
  }
  NEXT_INSN_INCPC();

I_ADD:
  /*
     add dst r1 r2
      dst : destination register
       r1, r2 : source registers
     $dst = $r1 + $r2
     If necessary, this instruction does type conversions.
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        cint s = fixnum_to_cint(v1) + fixnum_to_cint(v2);
        regbase[dst] = cint_to_number(s);
      }
      break;
    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        goto ADD_FLOFLO;
      }
    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        goto ADD_FLOFLO;
      }
    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
    ADD_FLOFLO:
        d = x1 + x2;
        regbase[dst] = double_to_number(d);
      }
      break;
    case TP_STRSTR:
      {
#ifdef STROBJ_HAS_HASH
        char *s1, *s2;
        uint32_t len1, len2;

        s1 = string_value(v1); len1 = string_length(v1);
        s2 = string_value(v2); len2 = string_length(v2);
        regbase[dst] = str_intern2(context, s1, len1, s2, len2,
                                   calc_hash2(s1, s2), INTERN_HARD);
#else
        regbase[dst] = cstr_to_string2(string_to_cstr(v1), string_to_cStr(v2));
#endif
      }
      break;

    default:
      {
        regbase[dst] = slow_add(context, v1, v2);
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_SUB:
  /*
     sub dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 - $r2
     If necessary, this instruction does type conversions.
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        cint s = fixnum_to_cint(v1) - fixnum_to_cint(v2);
        regbase[dst] = cint_to_number(s);
      }
      break;
    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        goto SUB_FLOFLO;
      }
    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        goto SUB_FLOFLO;
      }
    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
    SUB_FLOFLO:
        d = x1 - x2;
        regbase[dst] = double_to_number(d);
      }
      break;
    default:
      {
        regbase[dst] = slow_sub(context, v1, v2);
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_MUL:
  /*
      mul dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 * $r2
     If necessary, this instruction does type conversions.
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        cint n1, n2, p;
        n1 = fixnum_to_cint(v1);
        n2 = fixnum_to_cint(v2);
        if (half_fixnum_range(n1) && half_fixnum_range(n2)) {
          p = n1 * n2;
          regbase[dst] = cint_to_fixnum(p);
        } else {
          x1 = (double)n1;
          x2 = (double)n2;
          goto MUL_FLOFLO;
        }
      }
      break;
    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        goto MUL_FLOFLO;
      }
    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        goto MUL_FLOFLO;
      }
    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
    MUL_FLOFLO:
        d = x1 * x2;
        regbase[dst] = double_to_number(d);
      }
      break;
    default:
      {
        regbase[dst] = slow_mul(context, v1, v2);
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_DIV:
  ENTER_INSN(__LINE__);
  /*
     div dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 / $r2
     If necessary, this instruction does type conversions.
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        int n1, n2, s;
        n1 = fixnum_to_cint(v1);
        if (v2 == FIXNUM_ZERO) {
          if (n1 > 0) regbase[dst] = gconsts.g_flonum_infinity;
          else if (n1 == 0) regbase[dst] = gconsts.g_flonum_nan;
          else regbase[dst] = gconsts.g_flonum_negative_infinity;
        } else {
          n2 = fixnum_to_cint(v2);
          s = n1 / n2;
          regbase[dst] =
            (n1 == n2 * s)? cint_to_fixnum(s):
                            double_to_flonum((double)n1 / (double)n2);
        }
      }
      break;
    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        goto DIV_FLOFLO;
      }
    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        goto DIV_FLOFLO;
      }
    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
    DIV_FLOFLO:
        d = x1 / x2;
        if (isinf(d)) regbase[dst] = d > 0? gconsts.g_flonum_infinity:
                                            gconsts.g_flonum_negative_infinity;
        else if (isnan(d)) regbase[dst] = gconsts.g_flonum_nan;
        else regbase[dst] = double_to_number(d);
      }
      break;
    default:
      {
        regbase[dst] = slow_div(context, v1, v2);
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_MOD:
  /*
     mod dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 % $r2
     If necessary, this instruction does type conversions.
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        if (v2 == FIXNUM_ZERO)
          regbase[dst] = gconsts.g_flonum_nan;
        else {
          cint s = fixnum_to_cint(v1) % fixnum_to_cint(v2);
          regbase[dst] = cint_to_fixnum(s);
        }
      }
      break;
    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
      }
      goto MOD_FLOFLO;
    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        goto MOD_FLOFLO;
      }
    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
    MOD_FLOFLO:
        if (isinf(x1) || x2 == 0.0f)
          regbase[dst] = gconsts.g_flonum_nan;
        else {
          d = x1 / x2;
          d = d >= 0 ? floor(d) : ceil(d);
          d = x1 - (d * x2);
          regbase[dst] = double_to_number(d);
        }
      }
      break;
    default:
      {
        regbase[dst] = slow_mod(context, v1, v2);
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_BITAND:
  /*
     bitand dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 & $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    cint x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      regbase[dst] = v1 & v2;
      break;
    case TP_FIXFLO:
      x1 = fixnum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum(x1 & x2);
      break;
    case TP_FLOFIX:
      x1 = flonum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum(x1 & x2);
      break;
    case TP_FLOFLO:
      x1 = flonum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum(x1 & x2);
      break;
    default:
      regbase[dst] = slow_bitand(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_BITOR:
  /*
     bitor dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 | $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    cint x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      regbase[dst] = v1 | v2;
      break;
    case TP_FIXFLO:
      x1 = fixnum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum(x1 | x2);
      break;
    case TP_FLOFIX:
      x1 = flonum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum(x1 | x2);
      break;
    case TP_FLOFLO:
      x1 = flonum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = int_to_fixnum(x1 | x2);
      break;
    default:
      regbase[dst] = slow_bitor(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_LEFTSHIFT:
  /*
     leftshift dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 << $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    int32_t x1;
    cint x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      x1 = (int32_t)fixnum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 << x2));
      break;
    case TP_FIXFLO:
      x1 = (int32_t)fixnum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 << x2));
      break;
    case TP_FLOFIX:
      x1 = (int32_t)flonum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 << x2));
      break;
    case TP_FLOFLO:
      x1 = (int32_t)flonum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 << x2));
      break;
    default:
      regbase[dst] = slow_leftshift(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_RIGHTSHIFT:
  /*
     rightshift dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 >> $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    int32_t x1;
    cint x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      x1 = (int32_t)fixnum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    case TP_FIXFLO:
      x1 = (int32_t)fixnum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    case TP_FLOFIX:
      x1 = (int32_t)flonum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    case TP_FLOFLO:
      x1 = (int32_t)flonum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    default:
      regbase[dst] = slow_rightshift(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_UNSIGNEDRIGHTSHIFT:
  /*
     unsingedrightshift dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 >>> $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    uint32_t x1;
    cint x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      x1 = (uint32_t)fixnum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    case TP_FIXFLO:
      x1 = (uint32_t)fixnum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    case TP_FLOFIX:
      x1 = (uint32_t)flonum_to_cint(v1);
      x2 = fixnum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    case TP_FLOFLO:
      x1 = (uint32_t)flonum_to_cint(v1);
      x2 = flonum_to_cint(v2);
      regbase[dst] = cint_to_fixnum((cint)(x1 >> x2));
      break;
    default:
      regbase[dst] = slow_unsignedrightshift(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_LESSTHAN:
  /*
     lessthan dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 < $r2
     Note that `greaterthan' instruction is not supported.
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    double x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      regbase[dst] = true_false((int64_t)v1 < (int64_t)v2);
      break;
    case TP_FIXFLO:
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = true_false(x1 < x2);
      break;
    case TP_FLOFIX:
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      regbase[dst] = true_false(x1 < x2);
      break;
    case TP_FLOFLO:
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = true_false(x1 < x2);
      break;
    case TP_STRSTR:
      regbase[dst] =
        true_false(strcmp(string_to_cstr(v1), string_to_cstr(v2)) < 0);
      break;
    default:
      regbase[dst] = slow_lessthan(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_LESSTHANEQUAL:
  /*
     lessthanequal dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 <= $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    Tag tag;
    double x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      regbase[dst] = true_false((int64_t)v1 <= (int64_t)v2);
      break;
    case TP_FIXFLO:
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = true_false(x1 <= x2);
      break;
    case TP_FLOFIX:
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      regbase[dst] = true_false(x1 <= x2);
      break;
    case TP_FLOFLO:
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = true_false(x1 <= x2);
      break;
    case TP_STRSTR:
      regbase[dst] =
        true_false(strcmp(string_to_cstr(v1), string_to_cstr(v2)) <= 0);
      break;
    default:
      regbase[dst] = slow_lessthanequal(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_EQ:
  /*
     eq dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 === $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2, ret;
    double x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    if (v1 == v2)
      ret = false_true(is_nan(v1));
    else if (is_flonum(v1) && is_flonum(v2)) {
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
      ret = true_false(x1 == x2);
    } else
      ret = JS_FALSE;
    regbase[dst] = ret;
  }
  NEXT_INSN_INCPC();

I_EQUAL:
  /*
     equal dst r1 r2
       dst : destination register
       r1, r2 : source registers
     $dst = $r1 == $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2, ret;
    Tag tag;
    double x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    if (v1 == v2)
      ret = false_true(is_nan(v1));
    else {
      ret = JS_FALSE;
      tag = TAG_PAIR(get_tag(v1), get_tag(v2));
      switch (tag) {
      case TP_FIXFIX:
      case TP_FIXFLO:
      case TP_FLOFIX:
      case TP_STRSTR:
      case TP_SPEFLO:
      case TP_FLOSPE:
      case TP_OBJOBJ:
        ret = JS_FALSE;
        break;
      case TP_FLOFLO:
FLOFLO:
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
        ret = true_false(x1 == x2);
        break;
      case TP_SPESPE:
        ret = true_false((is_undefined(v1) && is_null(v2)) ||
                         (is_null(v1) && is_undefined(v2)));
        break;
      case TP_STRFIX:
STRFIX:
        v1 = string_to_number(v1);
        ret = true_false(v1 == v2);
        break;
      case TP_STRFLO:
STRFLO:
        v1 = string_to_number(v1);
        if (is_flonum(v1)) goto FLOFLO;
        ret = JS_FALSE;
        break;
      case TP_FIXSTR:
FIXSTR:
        v2 = string_to_number(v2);
        ret = true_false(v1 == v2);
        break;
      case TP_FLOSTR:
FLOSTR:
        v2 = string_to_number(v2);
        if (is_flonum(v2)) goto FLOFLO;
        ret = JS_FALSE;
        break;
      case TP_SPEFIX:
        if (v1 == JS_UNDEFINED) ret = JS_FALSE;
        else if (v1 == JS_NULL) ret = JS_FALSE;
        else {
          if (v1 == JS_TRUE) v1 = FIXNUM_ONE;
          else if (v1 == JS_FALSE) v1 = FIXNUM_ZERO;
          ret = true_false(v1 == v2);
        }
        break;
      case TP_FIXSPE:
        if (v2 == JS_UNDEFINED) ret = JS_FALSE;
        else if (v2 == JS_NULL) ret = JS_FALSE;
        else {
          if (v2 == JS_TRUE) v2 = FIXNUM_ONE;
          else if (v2 == JS_FALSE) v2 = FIXNUM_ZERO;
          ret = true_false(v1 == v2);
        }
        break;
      case TP_SPESTR:
        if (v1 == JS_UNDEFINED) ret = JS_FALSE;
        else if (v1 == JS_NULL) ret = JS_FALSE;
        else {
SPESTR:
          if (v1 == JS_TRUE) v1 = FIXNUM_ONE;
          else if (v1 == JS_FALSE) v1 = FIXNUM_ZERO;
          goto FIXSTR;
        }
        break;
      case TP_STRSPE:
        if (v2 == JS_UNDEFINED) ret = JS_FALSE;
        else if (v2 == JS_NULL) ret = JS_FALSE;
        else {
STRSPE:
          if (v2 == JS_TRUE) v2 = FIXNUM_ONE;
          else if (v2 == JS_FALSE) v2 = FIXNUM_ZERO;
          goto STRFIX;
        }
        break;
      case TP_OBJFIX:
        v1 = object_to_primitive(context, v1, HINT_NUMBER);
        ret = true_false(v1 == v2);
        break;
      case TP_FIXOBJ:
        v2 = object_to_primitive(context, v2, HINT_NUMBER);
        ret = true_false(v1 == v2);
        break;
      case TP_OBJFLO:
        v1 = object_to_primitive(context, v1, HINT_NUMBER);
        if (is_flonum(v1)) goto FLOFLO;
        ret = JS_FALSE;
        break;
      case TP_FLOOBJ:
        v2 = object_to_primitive(context, v2, HINT_NUMBER);
        if (is_flonum(v2)) goto FLOFLO;
        ret = JS_FALSE;
        break;
      case TP_OBJSTR:
        v1 = object_to_primitive(context, v1, HINT_NUMBER);
        if (is_fixnum(v1)) goto FIXSTR;
        else if (is_flonum(v1)) goto FLOSTR;
        else if (is_string(v1)) ret = true_false(v1 == v2);
        else if (is_boolean(v1)) goto SPESTR;
        break;
      case TP_STROBJ:
        v2 = object_to_primitive(context, v2, HINT_NUMBER);
        if (is_fixnum(v2)) goto STRFIX;
        else if (is_flonum(v2)) goto STRFLO;
        else if (is_string(v2)) ret = true_false(v1 == v2);
        else if (is_boolean(v2)) goto STRSPE;
        break;
      case TP_OBJSPE:
        v1 = object_to_primitive(context, v1, HINT_NUMBER);
        if (is_number(v1)) ret = JS_FALSE;
        else if (is_string(v1)) goto STRSPE;
        else if (is_boolean(v1)) ret = true_false(v1 == v2);
          ret = true_false(v1 == v2);
        break;
      case TP_SPEOBJ:
        v2 = object_to_primitive(context, v2, HINT_NUMBER);
        if (is_number(v2)) ret = JS_FALSE;
        else if (is_string(v2)) goto SPESTR;
        else if (is_boolean(v2)) ret = true_false(v1 == v2);
        break;
      }
    }
    regbase[dst] = ret;
  }
  NEXT_INSN_INCPC();

I_GETPROP:
  /*
     getprop dst obj idx
       $dst = $obj[$idx]
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue o, idx;

    dst = get_first_operand_reg(insn);
    o = regbase[get_second_operand_reg(insn)];
    idx = regbase[get_third_operand_reg(insn)];
    if (is_array(o)) {
      regbase[dst] = get_array_prop(context, o, idx);
    } else if (is_object(o)) {
      regbase[dst] = get_object_prop(context, o, idx);
    } else {
if (o == JS_UNDEFINED) printf("GETPROP: !!!!!\n");
      o = to_object(context, o);
      if (!is_object(o))
        regbase[dst] = JS_UNDEFINED;
      else
        regbase[dst] = get_object_prop(context, o, idx);
    }
    /*
    printf("getprop: idx = "); print_value_simple(context, idx);
    printf(" ; o = "); print_value_simple(context, o);
    printf(" ; result = "); print_value_simple(context, regbase[dst]);
    printf("\n");
    */
  }
  NEXT_INSN_INCPC();

I_SETPROP:
  /*
     setprop obj prop val
       obj : object into which (prop,val) pair is set
       prop : property name
       val : value
     $obj[$prop] = $val
   */
  ENTER_INSN(__LINE__);
  {
    JSValue o, p, v;

    o = regbase[get_first_operand_reg(insn)];
    p = regbase[get_second_operand_reg(insn)];
    v = regbase[get_third_operand_reg(insn)];
    if (is_array(o))
      set_array_prop(context, o, p, v);
    else if (is_object(o))
      set_object_prop(context, o, p, v);
    else
      LOG_EXIT("setprop: first operand is not an object\n");
  }
  NEXT_INSN_INCPC();

I_SETARRAY:
  /*
     setarray dst subscript src
       dst : register that holds an array
       subscript : array's subscript
       src : register that has the assigned value
     $dst[$reg] = $src
   */
  ENTER_INSN(__LINE__);
  {
    JSValue a, v;
    Subscript s;

    a = regbase[get_first_operand_reg(insn)];
    s = get_second_operand_subscr(insn);
    v = regbase[get_third_operand_reg(insn)];
    // It is unnecessary to typecheck the values.
    array_body_index(a, s) = v;
  }
  NEXT_INSN_INCPC();

I_GETGLOBAL:
  /*
     getglobal dst reg
       dst : destination register
       reg : register that has a pointer to a string object
     $dst = property value for the string in the global object
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue str, ret;

    dst = get_first_operand_reg(insn);
    str = regbase[get_second_operand_reg(insn)];
    if (get_prop(context->global, str, &ret) == FAIL)
      LOG_EXIT("GETGLOBAL: %s not found\n", string_to_cstr(str));
    /*
    printf("getglobal: dst = %d, str = %s, ret = ", dst, string_to_cstr(str));
    print_value_verbose(context, ret); putchar('\n');
    */
    regbase[dst] = ret;
  }
  NEXT_INSN_INCPC();

I_SETGLOBAL:
  /*
     setglobal reg src
       reg : register that has a pointer to a string object
       src : property value to be set
     property value for the string in the global object = $src
   */
  ENTER_INSN(__LINE__);
  {
    JSValue str, src;

    str = regbase[get_first_operand_reg(insn)];
    src = regbase[get_second_operand_reg(insn)];
    if (set_prop_none(context, context->global, str, src) == FAIL)
      LOG_EXIT("SETGLOBAL: setting a value of %s failed\n", string_to_cstr(str));
  }
  NEXT_INSN_INCPC();

I_INSTANCEOF:
  /*
     instanceof dst r1 r2
       $dst = $r1 instanceof $r2
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue v1, v2, p, ret;

    dst = get_first_operand_reg(insn);
    v1 = regbase[get_second_operand_reg(insn)];
    v2 = regbase[get_third_operand_reg(insn)];
    ret = JS_FALSE;
    if (is_object(v1) && is_object(v2) &&
        get_prop(v2, gconsts.g_string_prototype, &p) == SUCCESS) {
      while (get___proto__(v1, &v1) == SUCCESS)
        if (v1 == p) {
          ret = JS_TRUE;
          break;
        }
    }
    regbase[dst] = ret;
  }
  NEXT_INSN_INCPC();

I_MOVE:
  /*
     move dst src
       dst : destination register
       src : source register
     $dst = $src
   */
  ENTER_INSN(__LINE__);
  {
/*
printf("MOVE: &regbase[%d] = %p, value = ", get_first_operand_reg(insn), &regbase[get_first_operand_reg(insn)]); print_value_simple(context, regbase[get_second_operand_reg(insn)]); printf("\n");
*/
    regbase[get_first_operand_reg(insn)] = regbase[get_second_operand_reg(insn)];
  }
  NEXT_INSN_INCPC();

I_TYPEOF:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_NOT:
  /*
     not dst reg
       dst : destination register
       reg  : source register
     $dst = ! $reg
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue v;

    dst = get_first_operand_reg(insn);
    v = regbase[get_second_operand_reg(insn)];
    regbase[dst] =
      true_false(v == JS_FALSE || v == FIXNUM_ZERO ||
                 v == gconsts.g_flonum_nan || v == gconsts.g_string_empty);
  }
  NEXT_INSN_INCPC();

I_NEW:
  /*
     new dst con
       $dst = new object created by $con
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue con, o, p;

    dst = get_first_operand_reg(insn);
    con = regbase[get_second_operand_reg(insn)];
    if (is_function(con)) {
      save_context(); // GC
      o = new_normal_object(context);
      update_context(); // GC
      // printf("NEW: is_function, o = %lx\n", o);
      get_prop(con, gconsts.g_string_prototype, &p);
      if (!is_object(p)) p = gconsts.g_object_proto;
      set___proto___all(context, o, p);
    } else
      o = JS_UNDEFINED;
#if 0
    if (is_builtin(con))
      //	con == gconsts.g_object ||
      //        con == gconsts.g_array || con == gconsts.g_string ||
      //        con == gconsts.g_number || con == gconsts.g_boolean)
      o = JS_UNDEFINED;
    else if (is_function(con)) {
      o = new_normal_object();
      get_prop(con, gconsts.g_string_prototype, &p);
      if (!is_object(p)) p = gconsts.g_object_proto;
      set___proto___all(context, o, gconsts.g_object_proto);
    } else
      LOG_EXIT("NEW: not a constructor");
#endif
    regbase[dst] = o;
  }
  NEXT_INSN_INCPC();

I_GETIDX:
  ENTER_INSN(__LINE__);
  {
    printf("getidx instruction is now obsolete\n");
  }
  NEXT_INSN_INCPC();

I_ISUNDEF:
  /*
     isundef dst reg
       $dst = $reg == undefined
   */
  ENTER_INSN(__LINE__);
  {
    JSValue v;
    Register dst;

    dst = get_first_operand_reg(insn);
    v = regbase[get_second_operand_reg(insn)];
    regbase[dst] = is_undefined(v)? JS_TRUE: JS_FALSE;
  }
  NEXT_INSN_INCPC();

I_ISOBJECT:
  /*
     isobject dst reg
       $dst = $reg is an Object or not
   */
  ENTER_INSN(__LINE__);
  {
    Register dst, reg;

    dst = get_first_operand_reg(insn);
    reg = get_second_operand_reg(insn);
    regbase[dst] = true_false(is_object(reg));
  }
  NEXT_INSN_INCPC();

I_SETFL:
  /*
     setfl newfl
       newfl : number of elements between fp and sp (after growing sp)
     sp = fp + newfl - 1
   */
  ENTER_INSN(__LINE__);
  {
    int newfl, oldfl;

    newfl = get_first_operand_int(insn);
    oldfl = get_sp(context) - fp + 1;
    // printf("fp = %d, newfl = %d, fp + newfl = %d\n", fp, newfl, fp + newfl);
    if (fp + newfl > STACK_LIMIT)
      LOG_EXIT("register stack overflow\n");
    set_sp(context, fp + newfl - 1);
    while (++oldfl <= newfl)
      regbase[oldfl] = JS_UNDEFINED;
  }
  NEXT_INSN_INCPC();

I_SETA:
  /*
     seta src
       src : source register
     a = $src
   */
  ENTER_INSN(__LINE__);
  {
    set_a(context, regbase[get_first_operand_reg(insn)]);
  }
  NEXT_INSN_INCPC();

I_GETA:
  /*
     geta dst
       dst : destination register
     $dst = a
   */
  ENTER_INSN(__LINE__);
  {
    regbase[get_first_operand_reg(insn)] = get_a(context);
  }
  NEXT_INSN_INCPC();

I_GETERR:
  /*
     geterr dst
       dst : destination register
     $dst = err
     I don't know in which situation this instruction is necessary.
   */
  ENTER_INSN(__LINE__);
  {
    regbase[get_first_operand_reg(insn)] = get_err(context);
  }
  NEXT_INSN_INCPC();

I_GETGLOBALOBJ:
  /*
     getglobalobj dst
     $dst = global object
   */
  ENTER_INSN(__LINE__);
  {
    regbase[get_first_operand_reg(insn)] = context->global;
    if (context->global == JS_UNDEFINED) printf("GETGLOBALOBJ: !!!!\n");
  }
  NEXT_INSN_INCPC();

I_NEWARGS:
  /*
     newargs
   */
  ENTER_INSN(__LINE__);
  {
    int na;
    FunctionFrame *fr;
    JSValue args;
    int i;

    na = get_ac(context);

    /*
       allocates a new function frame into which arguments array is stored
     */
    // However, is it correct?
    // fr = new_frame(get_cf(context), fframe_prev(get_lp(context))); ???
    fr = new_frame(get_cf(context), get_lp(context));
    set_lp(context, fr);
    save_context(); // GC
    args = new_normal_array_with_size(context, na);
    update_context(); // GC
    /*
       Note that the i-th arg is regbase[i + 2].
       (regbase[1] is the receiver)
     */
    for (i = 0; i < na; i++)
      array_body_index(args, i) = regbase[i + 2];
    fframe_arguments(fr) = args;
  }
  NEXT_INSN_INCPC();

I_RET:
  /*
     ret
     returns from the function
   */
  ENTER_INSN(__LINE__);
  {
    JSValue *stack;

    if (fp == border)
      return 1;
    stack = &get_stack(context, 0);
    restore_special_registers(context, stack, fp - 4);
    set_sp(context, fp - 5);
    update_context();
  }
  NEXT_INSN_INCPC();

I_NOP:
  /*
     nop
   */
  ENTER_INSN(__LINE__);
  {
    asm volatile("#NOP Instruction\n");
  }
  NEXT_INSN_INCPC();

I_JUMP:
  /*
     jump disp
     pc = pc + $disp
   */
  ENTER_INSN(__LINE__);
  {
    Displacement disp;
    disp = get_first_operand_disp(insn);
    set_pc_relative(disp);
  }
  NEXT_INSN_NOINCPC();

I_JUMPTRUE:
  /*
     jumptrue src disp
     if ($src) pc = pc + $disp
   */
  ENTER_INSN(__LINE__);
  {
    JSValue v;
    Displacement disp;

    v = regbase[get_first_operand_reg(insn)];
    if (to_boolean(v) == JS_TRUE) {
      disp = get_second_operand_disp(insn);
      set_pc_relative(disp);
      NEXT_INSN_NOINCPC();
    }
  }
  NEXT_INSN_INCPC();

I_JUMPFALSE:
  /*
     jumpfalse src disp
     if (!$src) pc = pc + $disp
   */
  ENTER_INSN(__LINE__);
  {
    JSValue v;
    Displacement disp;

    v = regbase[get_first_operand_reg(insn)];
    if (to_boolean(v) == JS_FALSE) {
      disp = get_second_operand_disp(insn);
      set_pc_relative(disp);
      NEXT_INSN_NOINCPC();
    }
  }
  NEXT_INSN_INCPC();

I_GETARG:
  /*
     getarg dst link index
     $dst = value of the index-th argument in the link-th function frame
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    int link;
    Subscript index;
    FunctionFrame *fr;
    JSValue arguments;
    int i;

    dst = get_first_operand_reg(insn);
    link = get_second_operand_int(insn);
    index = get_third_operand_subscr(insn);
    fr = get_lp(context);
    for (i = 0; i < link; i++) fr = fframe_prev(fr);
    //regbase[dst] = array_body_index(fframe_arguments(fr), index);
    arguments = fframe_arguments(fr);
    // TODO: optimise
    regbase[dst] = get_array_prop(context, arguments, int_to_fixnum(index));
  }
  NEXT_INSN_INCPC();

I_GETLOCAL:
  /*
     getlocal dst link index
     $dst = value of the index-th local variable in the link-th function frame
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    int link;
    Subscript index;
    FunctionFrame *fr;
    int i;

    dst = get_first_operand_reg(insn);
    link = get_second_operand_int(insn);
    index = get_third_operand_subscr(insn);
    fr = get_lp(context);
    for (i = 0; i < link; i++) fr = fframe_prev(fr);
    regbase[dst] = fframe_locals_idx(fr, index);
  }
  NEXT_INSN_INCPC();

I_SETARG:
  /*
     setarg link index src
   */
  ENTER_INSN(__LINE__);
  {
    int link;
    Subscript index;
    Register src;
    FunctionFrame *fr;
    JSValue arguments;
    int i;

    link = get_first_operand_int(insn);
    index = get_second_operand_subscr(insn);
    src = get_third_operand_reg(insn);
    fr = get_lp(context);
    for (i = 0; i < link; i++) fr = fframe_prev(fr);

    // assert(index < array_size(fframe_arguments(fr)));
    // array_body_index(fframe_arguments(fr), index) = regbase[src];
    // TODO: optimise
    arguments = fframe_arguments(fr);
    set_array_prop(context, arguments, int_to_fixnum(index), regbase[src]);
  }
  NEXT_INSN_INCPC();

I_SETLOCAL:
  /*
     setlocal link index src
   */
  ENTER_INSN(__LINE__);
  {
    int link;
    Subscript index;
    Register src;
    FunctionFrame *fr;
    int i;

    link = get_first_operand_int(insn);
    index = get_second_operand_subscr(insn);
    src = get_third_operand_reg(insn);
    fr = get_lp(context);
    for (i = 0; i < link; i++) fr = fframe_prev(fr);
    fframe_locals_idx(fr, index) = regbase[src];
  }
  NEXT_INSN_INCPC();

I_MAKECLOSURE:
  /*
     makeclosure dst subscr
       dst : destination register
       subscr : subscript of the function table
     $dst = new closure
   */
  ENTER_INSN(__LINE__);
  {
    Register dst;
    Subscript ss;

    /*
       `subscr' is the subscript of the function table EXCEPT the
       main function.  Since the main function comes first in the
       function table, the subecript should be added by 1.
     */
    dst = get_first_operand_reg(insn);
    ss = get_second_operand_subscr(insn) + 1;
    save_context(); // GC
    regbase[dst] = new_normal_function(context, ss);
    update_context();  // GC
  }
  NEXT_INSN_INCPC();

I_MAKEITERATOR:
  /*
     makeiterator obj dst
       dst : destination register
     $dst = iterator object for iterating $obj
   */ 
  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue obj;

    obj = regbase[get_first_operand_reg(insn)];
    dst = get_second_operand_reg(insn);
    if (!is_object(obj))
      LOG_EXIT("makeiterator: not an object\n");
    regbase[dst] = new_normal_iterator(context, obj);
    // printf("makeiterator: iter = %016lx ", regbase[dst]); simple_print(regbase[dst]); printf("\n");
  }
  NEXT_INSN_INCPC();

I_NEXTPROPNAME:
  /*
     nextpropname obj itr dst
       obj : object
       itr : iterator for enumerating properties in obj
       dst : destination
     $dst = the next property name of $obj in $itr
   */
  ENTER_INSN(__LINE__);
  {
    JSValue obj, itr, res, val;
    Register dst;

    obj = regbase[get_first_operand_reg(insn)];
    itr = regbase[get_second_operand_reg(insn)];
    dst = get_third_operand_reg(insn);
    res = JS_UNDEFINED;
    // printf("nextpropname: itr = %016lx ", itr); simple_print(itr); printf("\n");
    while (1) {
      int r;
      r = get_next_propname(itr, &res);
      if (r != SUCCESS) break;
      if ((val = get_prop_prototype_chain(obj, res)) != JS_UNDEFINED) break;
    }
    regbase[dst] = res;
  }
  NEXT_INSN_INCPC();

I_CALL:
I_SEND:
I_NEWSEND:
  /*
     call fn nargs
     send fn nargs
     newsend fn nargs
   */
  ENTER_INSN(__LINE__);
  {
    JSValue fn;
    int nargs;
    Opcode op;
    int sendp, newp;

    op = get_opcode(insn);
    sendp = (op != CALL)? TRUE: FALSE;
    newp = (op == NEWSEND)? TRUE: FALSE;
    fn = regbase[get_first_operand_reg(insn)];
    nargs = get_second_operand_int(insn);
    set_fp(context, fp);
    set_pc(context, pc);
    if (is_function(fn)) {
#ifdef CALC_CALL
      callcount++;
#endif
      // function
      call_function(context, fn, nargs, sendp);
      update_context();
      NEXT_INSN_NOINCPC();
    } else if (is_builtin(fn)) {
      // builtin function
      call_builtin(context, fn, nargs, sendp, newp);
      update_context();  // GC
      NEXT_INSN_INCPC();
#ifdef USE_FFI
      if (isErr(context)) {
        LOG_EXIT("CALL/SEND: exception by builtin");
      }
#endif
    }
#ifdef USE_FFI
    else if (isForeign(fn)) {
      call_foreign(context, fn, nargs, false, false);
      if (!isErr(context)) {
        NEXT_INSTRUCTION_INCPC();
      } else {
        int catchPlace = getCatchFp(context);
        int tempFp;
        while(cfp > border && cfp > catchPlace){
          tempFp = regBase[-FP_POS];
          setFp(context, tempFp);
          setLp(context, (FunctionFrame*)regBase[-LP_POS]);
          setPc(context, (int)regBase[-PC_POS]);
          setCf(context, (FunctionTableCell*)regBase[-CF_POS]);
          setSp(context, cfp);
          updateContext();
        }

        if(catchPlace < border){
          return -1; }

        setPc(context, getCatchPc(context));
        updateContext();
        catchStackPop(context);
        NEXT_INSTRUCTION_NOINCPC();
      }
    }
#endif
    else {
      print_value_simple(context, fn);
      printf(" is called in CALL/SEND/NEWSEND instruction\n");
      if (newp)
	LOG_EXIT("Not a constructor\n");
      else
	LOG_EXIT("CALL/SEND\n");
    }
  }
  NEXT_INSN_INCPC();

I_TAILCALL:
I_TAILSEND:
  /*
     tailcall fn nargs
     tailsend fn nargs
   */
  ENTER_INSN(__LINE__);
  {
    JSValue fn;
    int nargs;
    int sendp;

    sendp = (get_opcode(insn) == TAILSEND)? TRUE: FALSE;
    fn = regbase[get_first_operand_reg(insn)];
    nargs = get_second_operand_int(insn);
    set_fp(context, fp);
    set_pc(context, pc);

    if (is_function(fn)) {
#ifdef CALC_CALL
      callcount++;
#endif
      // function
      tailcall_function(context, fn, nargs, sendp);
      update_context();
      NEXT_INSN_NOINCPC();
    } else if (is_builtin(fn)) {
      tailcall_builtin(context, fn, nargs, sendp, FALSE);
      update_context();        // is this necessary? => yes. moving GC
      NEXT_INSN_INCPC();
    } else
      LOG_EXIT("TAILCALL/TAILSEND: not a function\n");
  }
  NEXT_INSN_INCPC();

I_PUSHHANDLER:
  ENTER_INSN(__LINE__);
  {
    Displacement disp;
    disp = get_first_operand_disp(insn);
    exhandler_stack_push(context, pc + disp, fp);
    NEXT_INSN_INCPC();
  }

I_POPHANDLER:
  ENTER_INSN(__LINE__);
  {
    int newpc;
    int handler_fp;
    exhandler_stack_pop(context, &newpc, &handler_fp);
    NEXT_INSN_INCPC();
  }

I_THROW:
  ENTER_INSN(__LINE__);
  {
    Displacement disp;
    int newpc;
    int handler_fp;
    exhandler_stack_pop(context, &newpc, &handler_fp);
    while (handler_fp != fp) {
      JSValue *stack;
      stack = &get_stack(context, 0);
      restore_special_registers(context, stack, fp - 4);
      set_sp(context, fp - 5);
      update_context();      /* TODO: optimise */
    }
    disp = (Displacement) (newpc - pc);
    set_pc_relative(disp);
    NEXT_INSN_NOINCPC();
  }

I_LOCALCALL:
  ENTER_INSN(__LINE__);
  {
    Displacement disp;
    disp = get_first_operand_disp(insn);
    lcall_stack_push(context, pc);
    set_pc_relative(disp);
    NEXT_INSN_NOINCPC();
  }

I_LOCALRET:
  ENTER_INSN(__LINE__);
  {
    Displacement disp;
    int newpc;
    lcall_stack_pop(context, &newpc);
    disp = (Displacement) (newpc - pc);
    set_pc_relative(disp);
    NEXT_INSN_INCPC();   /* need INCPC; local_call_stack has the address of local call */
  }

I_POPLOCAL:
  ENTER_INSN(__LINE__);
  {
    int newpc;
    lcall_stack_pop(context, &newpc);
    NEXT_INSN_INCPC();
  }

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

#endif // J5MODE

I_ERROR:
  ENTER_INSN(__LINE__);
  return -1;

I_UNKNOWN:
  ENTER_INSN(__LINE__);
  return -1;

I_END:
  ENTER_INSN(__LINE__);
  return 1;
}

static void exhandler_stack_push(Context* context, int pc, int fp)
{
  int sp = context->exhandler_stack_ptr;

  set_array_index_value(context, context->exhandler_stack, sp++,
			cint_to_number(pc), FALSE);
  set_array_index_value(context, context->exhandler_stack, sp++,
			cint_to_number(fp), FALSE);
  context->exhandler_stack_ptr = sp;
}

static int exhandler_stack_pop(Context* context, int *pc, int *fp)
{
  int sp = context->exhandler_stack_ptr;
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
			cint_to_number(pc), FALSE);
}

static int lcall_stack_pop(Context* context, int *pc)
{
  JSValue v;
  if (context->lcall_stack < 1)
    return -1;
  context->lcall_stack_ptr--;
  v = get_array_prop(context, context->lcall_stack,
		     cint_to_number(context->lcall_stack_ptr));
  *pc = number_to_cint(v);
  return 0;
}

