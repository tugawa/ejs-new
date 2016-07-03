#include "prefix.h"
#define EXTERN
#include "header.h"

#define NOT_IMPLEMENTED() \
  printf("Sorry, instruction %s has not been implemented yet\n", insn_nemonic(get_opcode(insn)))

inline void make_insn_ptr(FunctionTable *curfn, void *const *jt
#ifdef PARALLEL
, bool inpar
#endif
) {
  int i, n_insns;
  Instruction *insns;
//  void **ptr = curfn->insn_ptr;
  InsnLabel *ptr = curfn->insn_ptr;
  n_insns = curfn->n_insns;
#ifdef PARALLEL
  insns = inpar? curfn->parallelInsns: curfn->insns;
#else
  insns = curfn->insns;
#endif
  for (i = 0; i < n_insns; i++)
    ptr[i] = jt[get_opcode(insns[i].code)];
  curfn->insn_ptr_created = true;
}

#define STRCON(x,y)  x #y
#define INCPC()      do { pc++; insn_ptr++; insns++; } while (0)
#define PRINTPC()    fprintf(stderr, "pc:%d\n", pc)
#define INCEXECUTECOUNT() insns->executeCount++
#define INSNLOAD()   insn = insns->code

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

// defines update_context()
//
#ifdef PARALLEL

#define update_context() do {                       \
  curfn = get_cf(context);                          \
  codesize = ftab_n_insns(curfn);                   \
  pc = get_pc(context);                             \
  fp = get_fp(context);                             \
  insns = (context->inParallel ? curfn->parallelInsns : curfn->insns) + pc \
  regbase = (JSValue *)&get_stack(context, fp) - 1; \
  if (!curfn->insn_ptr_created)                     \
    make_insn_ptr(curfn, jump_table, context->inParallel);                 \
  insn_ptr = curfn->insn_ptr + pc;                  \
} while (0)

#else

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

#endif // PARALLEL

#define load_regs(insn, dst, r1, r2, v1, v2) \
  dst = get_first_operand_reg(insn), \
  r1 = get_second_operand_reg(insn), \
  r2 = get_third_operand_reg(insn), \
  v1 = regbase[r1], \
  v2 = regbase[r2]

#define goto_pc_relative(d) (pc += (d), insn += (d), insn_ptr += (d))

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
  // fixnum dst imm
  //   dst : destination register
  //   imm : immediate value of the fixnum
  // $dst = imm

  ENTER_INSN(__LINE__);
  {
    Register reg = get_first_operand_reg(insn);
    regbase[reg] = cint_to_fixnum((cint)get_small_immediate(insn));
  }
  NEXT_INSN_INCPC();

I_SPECCONST:
  // specconst dst specimm
  //   dst : destination register
  //   specimm : immediate value of the special constant
  // $dst = specimm

  ENTER_INSN(__LINE__);
  {
    Register reg = get_first_operand_reg(insn);
    regbase[reg] = get_small_immediate(insn);
  }
  NEXT_INSN_INCPC();

I_NUMBER:
I_STRING:
I_REGEXP:
  // number dst disp
  // string dst disp
  // regexp dst disp
  //   dst : destination register
  //   disp : displacement of the constant position from the pc
  // $dst = insns[pc + disp]

  ENTER_INSN(__LINE__);
  {
    Register dst;
    Displacement disp;
    dst = get_first_operand_reg(insn);
    disp = get_big_disp(insn);
    regbase[dst] = insns[disp].code;
  }

  /*
  reg = getConst(index);
  setObjProp(reg, "__proto__", gRegExpProto, ATTR_ALL);
  */
  NEXT_INSN_INCPC();

I_ADD:
  // add dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 + $r2
  // If necessary, this instruction does type conversions.

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        cint s = fixnum_to_int(v1) + fixnum_to_int(v2);
        regbase[dst] =
          is_fixnum_range_cint(s)? cint_to_fixnum(s): cint_to_flonum(s);
#ifdef QUICKENING
        quickening(insns, pc, ADDFIXFIX);
#endif
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, ADDFIXFLO);
#endif
        goto ADD_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, ADDFLOFIX);
#endif
        goto ADD_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, ADDFLOFLO);
#endif
    ADD_FLOFLO:
        d = x1 + x2;
        regbase[dst] =
          is_fixnum_range_double(d)? double_to_fixnum(d): double_to_flonum(d);
      }
      break;
   
    case TP_STRSTR:
      {
#ifdef STROBJ_HAS_HASH
        char *s1, *s2;
        uint32_t len1, len2;

        s1 = string_value(v1); len1 = string_length(v1);
        s2 = string_value(v2); len2 = string_length(v2);
        regbase[dst] = str_intern2(s1, len1, s2, len2,
                                   calc_hash2(s1, s2), INTERN_HARD);
#else
        regbase[dst] = cstr_to_string2(string_to_cstr(v1), string_to_cStr(v2));
#endif
#ifdef QUICKENING
        quickening(insns, pc, ADDSTRSTR);
#endif
      }
      break;

    default:
      {
        // For other cases, use slow_add function.
        regbase[dst] = slow_add(context, v1, v2);
#ifdef QUICKENING
        quickening(insns, pc, fastAddOpcode(tag));
#endif
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_SUB:
  // sub dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 - $r2
  // If necessary, this instruction does type conversions.

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        cint s = fixnum_to_cint(v1) - fixnum_to_cint(v2);
        regbase[dst] =
          is_fixnum_range_cint(s)? cint_to_fixnum(s): cint_to_flonum(s);
#ifdef QUICKENING
        quickening(insns, pc, SUBFIXFIX);
#endif
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, SUBFIXFLO);
#endif
        goto SUB_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, SUBFLOFIX);
#endif
        goto SUB_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, SUBFLOFLO);
#endif
    SUB_FLOFLO:
        d = x1 - x2;
        regbase[dst] =
          is_fixnum_range_double(d)? double_to_fixnum(d): double_to_flonum(d);
      }
      break;

    default:
      {
        regbase[dst] = slow_sub(context, v1, v2);
#ifdef QUICKENING
        quickening(insns, pc, fastSubOpcode(tag));
#endif
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_MUL:
  // mul dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 * $r2
  // If necessary, this instruction does type conversions.

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
// printf("Entered MUL\n");
// print_value_verbose(context, v1); printf("\n");
// print_value_verbose(context, v2); printf("\n");
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        cint n1, n2, p;
        n1 = fixnum_to_cint(v1);
        n2 = fixnum_to_cint(v2);
        if (half_fixnum_range(n1) && half_fixnum_range(n2)) {
          p = n1 * n2;
          regbase[dst] = cint_to_fixnum(p);
#ifdef QUICKENING
          quickening(insns, pc, MULFIXFIXSMALL);
#endif
        } else {
          x1 = (double)n1;
          x2 = (double)n2;
#ifdef QUICKENING
          quickening(insns, pc, MULFIXFIX);
#endif
          goto MUL_FLOFLO;
        }
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, MULFIXFLO);
#endif
        goto MUL_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, MULFLOFIX);
#endif
        goto MUL_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, MULFLOFLO);
#endif
    MUL_FLOFLO:
        d = x1 * x2;
        regbase[dst] =
          is_fixnum_range_double(d)? double_to_fixnum(d): double_to_flonum(d);
      }
      break;

    default:
      {
        regbase[dst] = slow_mul(context, v1, v2);
#ifdef QUICKENING
        quickening(insns, pc, fastMulOpcode(tag));
#endif
      }
      break;
    }
// printf("End of MUL\n");
// print_value_verbose(context, regbase[dst]); printf("\n");
  }
  NEXT_INSN_INCPC();

I_DIV:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_MOD:
  // mod dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 % $r2
  // If necessary, this instruction does type conversions.

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    double x1, x2, d;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(v1), get_tag(v2))) {
    case TP_FIXFIX:
      {
        if (v2 == FIXNUM_ZERO)
          regbase[dst] = gconsts.g_flonum_nan;
        else {
          cint s = fixnum_to_cint(v1) % fixnum_to_cint(v2);
          // mod value should be in the fixnum range.
          regbase[dst] = cint_to_fixnum(s);
        }
#ifdef QUICKENING
        quickening(insns, pc, MODFIXFIX);
#endif
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, MODFIXFLO);
#endif
        goto MOD_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, MODFLOFIX);
#endif
        goto MOD_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
#ifdef QUICKENING
        quickening(insns, pc, MODFLOFLO);
#endif
    MOD_FLOFLO:
        if (isinf(x1) || x2 == 0.0f)
          regbase[dst] = gconsts.g_flonum_nan;
        else {
          d = x1 / x2;
          d = d >= 0 ? floor(d) : ceil(d);
          d = x1 - (d * x2);
          regbase[dst] =
            is_fixnum_range_double(d)? double_to_fixnum(d): double_to_flonum(d);
        }
      }
      break;

    default:
      {
        regbase[dst] = slow_mod(context, v1, v2);
#ifdef QUICKENING
        quickening(insns, pc, fastModOpcode(tag));
#endif
      }
      break;
    }
  }
  NEXT_INSN_INCPC();

I_BITAND:
  // bitand dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 & r2

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    cint x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
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
  // bitor dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 | $r2

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    cint x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
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
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_RIGHTSHIFT:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_UNSIGNEDRIGHTSHIFT:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_LESSTHAN:
  // lessthan dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 < $r2
  // Note that `greaterthan' instruction is not supported.

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    double x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
    case TP_FIXFIX:
      regbase[dst] = (cint)v1 < (cint)v2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANFIXFIX);
      break;
    case TP_FIXFLO:
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = x1 < x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANFIXFLO);
      break;
    case TP_FLOFIX:
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      regbase[dst] = x1 < x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANFLOFIX);
      break;
    case TP_FLOFLO:
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = x1 < x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANFLOFLO);
      break;
    case TP_STRSTR:
      quickening(insns, pc, LESSTHANSTRSTR);
    default:
      regbase[dst] = slow_lessthan(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_LESSTHANEQUAL:
  // lessthan dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 <= $r2

  ENTER_INSN(__LINE__);
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    double x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
    case TP_FIXFIX:
      regbase[dst] = (cint)v1 <= (cint)v2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANEQUALFIXFIX);
      break;
    case TP_FIXFLO:
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = x1 <= x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANEQUALFIXFLO);
      break;
    case TP_FLOFIX:
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      regbase[dst] = x1 <= x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANEQUALFLOFIX);
      break;
    case TP_FLOFLO:
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = x1 <= x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, LESSTHANEQUALFLOFLO);
      break;
    case TP_STRSTR:
      quickening(insns, pc, LESSTHANEQUALSTRSTR);
    default:
      regbase[dst] = slow_lessthanequal(context, v1, v2);
      break;
    }
  }
  NEXT_INSN_INCPC();

I_EQ:
  // eq dst r1 r2
  //   dst : destination register
  //   r1, r2 : source registers
  // $dst = $r1 === $r2

  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  {
    Register dst, r1, r2;
    JSValue v1, v2;
    uint64_t tag;
    double x1, x2;

    load_regs(insn, dst, r1, r2, v1, v2);
    if (v1 == v2) {
      regbase[dst] = is_nan(v1)? JS_FALSE: JS_TRUE;
      goto EQ_END;
    }
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
    case TP_FIXFLO:
      x1 = fixnum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = x1 == x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, EQFIXFLO);
      break;
    case TP_FLOFIX:
      x1 = flonum_to_double(v1);
      x2 = fixnum_to_double(v2);
      regbase[dst] = x1 == x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, EQFLOFIX);
      break;
    case TP_FLOFLO:
      x1 = flonum_to_double(v1);
      x2 = flonum_to_double(v2);
      regbase[dst] = x1 == x2? JS_TRUE: JS_FALSE;
      quickening(insns, pc, EQFLOFLO);
      break;
    case TP_STRSTR:
      {
        char *s1, *s2;
        s1 = string_to_cstr(v1);
        s2 = string_to_cstr(v2);
        regbase[dst] = strcmp(s1, s2)? JS_FALSE: JS_TRUE;
        quickening(insns, pc, EQSTRSTR);
      }
      break;
    default:
      regbase[dst] = JS_FALSE;
      break;
    }
  }
EQ_END:
  NEXT_INSN_INCPC();

I_EQUAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETPROP:
  // getprop dst obj idx
  //   $dst = $obj[idx]

  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue o, idx;

    printf("getprop\n");
    dst = get_first_operand_reg(insn);
    o = regbase[get_second_operand_reg(insn)];
    idx = regbase[get_third_operand_reg(insn)];
    if (is_array(o))
      regbase[dst] = get_array_prop(context, o, idx);
    else if (is_object(o))
      regbase[dst] = get_object_prop(context, o, idx);
    else {
      o = to_object(context, o);
      if (!is_object(o))
        regbase[dst] = JS_UNDEFINED;
      else
        regbase[dst] = get_object_prop(context, o, idx);
    }
  }
  NEXT_INSN_INCPC();

I_SETPROP:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETARRAY:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETGLOBAL:
  // getglobal dst reg
  //   dst : destination register
  //   reg : register that has a pointer to a string object
  // $dst = property value for the string in the global object

  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue str, ret;

    dst = get_first_operand_reg(insn);
    str = regbase[get_second_operand_reg(insn)];
#ifdef USE_FASTGLOBAL
#else
    if (get_prop(context->global, str, &ret) == FAIL)
      LOG_EXIT("GETGLOBAL: %s not found\n", string_to_cstr(str));
    regbase[dst] = ret;
#endif
  }
  NEXT_INSN_INCPC();

I_SETGLOBAL:
  // setglobal reg src
  //   reg : register that has a pointer to a string object
  //   src : property value to be set
  // property value for the string in the global object = $src
  
  ENTER_INSN(__LINE__);
  {
    JSValue str, src;

    str = regbase[get_first_operand_reg(insn)];
    src = regbase[get_second_operand_reg(insn)];

#ifdef USE_FASTGLOBAL
#else
    if (set_prop(context->global, str, src) == FAIL)
      LOG_EXIT("SETGLOBAL: setting a value of %s failed\n", string_to_cstr(str));
#endif
  }
  NEXT_INSN_INCPC();

I_INSTANCEOF:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_MOVE:
  // move dst src
  //   dst : destination register
  //   src : source register
  // $dst = $src

  ENTER_INSN(__LINE__);
  {
    regbase[get_first_operand_reg(insn)] = regbase[get_second_operand_reg(insn)];
  }
  NEXT_INSN_INCPC();

I_TYPEOF:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_NOT:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_NEW:
  // new dst con

  ENTER_INSN(__LINE__);
  {
    Register dst;
    JSValue con;

    dst = get_first_operand_reg(insn);
    con = regbase[get_second_operand_reg(insn)];
    // The definition of NEW in the current ssjsvm seems to be incorrect.
    NOT_IMPLEMENTED();
  }
  NEXT_INSN_INCPC();

I_GETIDX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_ISUNDEF:
  // isundef dst reg
  //   $dst = $reg == undefined

  ENTER_INSN(__LINE__);
  {
    Register dst, reg;

    dst = get_first_operand_reg(insn);
    reg = get_second_operand_reg(insn);
    regbase[dst] = is_undefined(reg)? JS_TRUE: JS_FALSE;
  }
  NEXT_INSN_INCPC();

I_ISOBJECT:
  // isobject dst reg
  //   $dst = $reg is an Object or not

  ENTER_INSN(__LINE__);
  {
    Register dst, reg;

    dst = get_first_operand_reg(insn);
    reg = get_second_operand_reg(insn);
    regbase[dst] = is_object(reg)? JS_TRUE: JS_FALSE;
  }
  NEXT_INSN_INCPC();

I_SETFL:
  // setfl newfl
  //   newfl : number of elements between fp and sp (after growing sp)
  // sp = fp + newfl - 1

  ENTER_INSN(__LINE__);
  {
    int newfl, oldfl;

    newfl = get_first_operand_int(insn);
    oldfl = get_sp(context) - fp + 1;
    set_sp(context, fp + newfl - 1);
    while (++oldfl <= newfl)
      regbase[oldfl] = JS_UNDEFINED;
  }
  NEXT_INSN_INCPC();

I_SETA:
  // seta src
  //   src : source register
  // a = $src

  ENTER_INSN(__LINE__);
  {
    set_a(context, regbase[get_first_operand_reg(insn)]);
  }
  NEXT_INSN_INCPC();

I_GETA:
  // geta dst
  //   dst : destination register
  // $dst = a

  ENTER_INSN(__LINE__);
  {
    regbase[get_first_operand_reg(insn)] = get_a(context);
  }
  NEXT_INSN_INCPC();

I_GETERR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETGLOBALOBJ:
  // getglobalobj dst
  // $dst <- global object

  ENTER_INSN(__LINE__);
  {
    regbase[get_first_operand_reg(insn)] = context->global;
  }
  NEXT_INSN_INCPC();

I_NEWARGS:
  // newargs

  ENTER_INSN(__LINE__);
  {
    int na;
    FunctionFrame *fr;
    JSValue args;
    ArrayCell *a;
    int i;

    na = get_ac(context);

    // allocates a new function frame into which arguments array is stored
    // However, is it correct?
    // fr = new_frame(get_cf(context), fframe_prev(get_lp(context))); ???
    fr = new_frame(get_cf(context), get_lp(context));
    set_lp(context, fr);
    args = new_array_with_size(na);
    a = remove_array_tag(args);
    // Note that the i-th arg is regbase[i + 2].
    //   (regbase[1] is the receiver)
    for (i = 0; i < na; i++)
      array_body_index(a, i) = regbase[i + 2];
    fframe_arguments(fr) = args;
  }
  NEXT_INSN_INCPC();

I_RET:
  // ret
  // returns from the function

  ENTER_INSN(__LINE__);
  {
    if (fp == border)
      return 1;
    pop_special_registers(context, fp, regbase);
    update_context();
  }
  NEXT_INSN_INCPC();

I_NOP:
  // nop
  
  ENTER_INSN(__LINE__);
  {
    asm volatile("#NOP Instruction\n");
  }
  NEXT_INSN_INCPC();

I_JUMP:
  // jump disp
  //   pc = pc + disp

  ENTER_INSN(__LINE__);
  {
    Displacement disp;
    disp = get_first_operand_disp(insn);
    goto_pc_relative(disp);
  }
  NEXT_INSN_NOINCPC();

I_JUMPTRUE:
  // jumptrue src disp
  //   if ($src) pc = pc + disp

  ENTER_INSN(__LINE__);
  {
    Register src;
    Displacement disp;

    src = get_first_operand_reg(insn);
    if (is_true(regbase[src])) {
      disp = get_second_operand_disp(insn);
      goto_pc_relative(disp);
    } else {
      // not implemented yet
    }
  }
  NEXT_INSN_NOINCPC();

I_JUMPFALSE:
  // jumpfalse src disp
  //   if (!$src) pc = pc + disp

  ENTER_INSN(__LINE__);
  {
    Register src;
    Displacement disp;
    JSValue v;

    src = get_first_operand_reg(insn);
    v = regbase[src];
    if (is_false(v) || is_undefined(v) || is_null(v)) {
      disp = get_second_operand_disp(insn);
      goto_pc_relative(disp);
    } else {
      // not implemented yet
    }
  }
  NEXT_INSN_NOINCPC();

I_GETARG:
  // gerarg dst link index
  //   $dst = value of the index-th argument in the link-th function frame
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
    regbase[dst] = array_body_index(fframe_arguments(fr), index);
  }
  NEXT_INSN_INCPC();

I_GETLOCAL:
  // getlocal dst link index
  //   $dst = value of the index-th local variable in the link-th function frame
  
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
  // setarg link index src

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
    array_body_index(fframe_arguments(fr), index) = regbase[src];
  }
  NEXT_INSN_INCPC();

I_SETLOCAL:
  // setlocal link index src

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
  // makeclosure dst subscr
  //   dst : destination register
  //   subscr : subscript of the function table
  // $dst = new closure

  ENTER_INSN(__LINE__);
  {
    Register dst;
    Subscript ss;

    // `subscr' is the subscript of the function table EXCEPT the
    // main function.  Since the main function comes first in the
    // function table, the subecript should be added by 1.
    dst = get_first_operand_reg(insn);
    ss = get_second_operand_subscr(insn) + 1;
    regbase[dst] = new_function(context, ss);
  }
  NEXT_INSN_INCPC();

I_MAKEITERATOR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_NEXTPROPNAME:
  ENTER_INSN(__LINE__);
  NEXT_INSN_INCPC();

I_CALL:
I_SEND:
  // call fn nargs
  // send fn nargs

  ENTER_INSN(__LINE__);
  {
    JSValue fn;
    int nargs;
    int sendp;

    sendp = (get_opcode(insn) == SEND)? TRUE: FALSE;
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
      call_builtin(context, fn, nargs, sendp, FALSE);
      NEXT_INSN_INCPC();
#ifdef USE_FFI
      if (isErr(context)) {
        LOG_EXIT("CALL/SEND: exception by builtin");
      }
#endif
    }
#ifdef USE_FFI
    else if (isForeign(funcv)) {
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
    else{
      LOG_EXIT("CALL/SEND");
    }
  }
  NEXT_INSN_INCPC();

// I_SEND:
//   ENTER_INSN(__LINE__);
//   NOT_IMPLEMENTED();
//   NEXT_INSN_INCPC();

I_TAILCALL:
  // tailcall fn nargs

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
    }
  }
  NEXT_INSN_INCPC();

I_TAILSEND:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_NEWSEND:
  NOT_IMPLEMENTED();
  ENTER_INSN(__LINE__);
  NEXT_INSN_INCPC();

I_TRY:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_THROW:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_FINALLY:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
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

#ifdef PARALLEL
I_GETGLOBALPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETGLOBALPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETPROPPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETPROPPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETARGPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETLOCALPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETARGPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETLOCALPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_CALLPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SENDPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_TAILCALLPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_TAILSENDPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_LIGHTCALLPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_LIGHTSENDPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_LIGHTTAIL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_LIGHTTAILSENDPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_RETPAR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

#endif // PRARLLEL
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
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_UNKNOWN:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_END:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();
}
