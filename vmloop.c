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

#define goto_pc_relative(d)   pc += (d), insn += (d), insn_ptr += (d)

// executes the main loop of the vm as a threaded code
//
int vmrun_threaded(Context* context, int border) {
  FunctionTable *curfn;
  int codesize;
  int pc;
  int fp;
  Instruction *insns;
  JSValue *regbase;
//  void **insn_ptr;
  InsnLabel *insn_ptr;
  Bytecode insn;
  // JSValue *locals = NULL;
//  static void *const jump_table[] = {
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
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
    case TP_FIXFIX:
      {
        cint s = fixnum_to_int(v1) + fixnum_to_int(v2);
        regbase[dst] =
          is_fixnum_range_cint(s)? cint_to_fixnum(s): cint_to_flonum(s);
        quickening(insns, pc, ADDFIXFIX);
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, ADDFIXFLO);
        goto ADD_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        quickening(insns, pc, ADDFLOFIX);
        goto ADD_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, ADDFLOFLO);
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
        quickening(insns, pc, ADDSTRSTR);
      }
      break;

    default:
      {
        // For other cases, use slow_add function.
        regbase[dst] = slow_add(context, v1, v2);
        quickening(insns, pc, fastAddOpcode(tag));
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
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
    case TP_FIXFIX:
      {
        cint s = fixnum_to_cint(v1) - fixnum_to_cint(v2);
        regbase[dst] =
          is_fixnum_range_cint(s)? cint_to_fixnum(s): cint_to_flonum(s);
        quickening(insns, pc, SUBFIXFIX);
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, SUBFIXFLO);
        goto SUB_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        quickening(insns, pc, SUBFLOFIX);
        goto SUB_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, SUBFLOFLO);
    SUB_FLOFLO:
        d = x1 - x2;
        regbase[dst] =
          is_fixnum_range_double(d)? double_to_fixnum(d): double_to_flonum(d);
      }
      break;

    default:
      {
        regbase[dst] = slow_sub(context, v1, v2);
        quickening(insns, pc, fastSubOpcode(tag));
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
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
    case TP_FIXFIX:
      {
        if ((v1 <= int_to_fixnum((long)0x7fffffff)) &&
            (v1 >= int_to_fixnum(-0x80000000)) &&
            (v2 <= int_to_fixnum((long)0x7fffffff)) &&
            (v2 >= int_to_fixnum(-0x80000000))) {
          cint s = fixnum_to_cint(v1) * fixnum_to_cint(v2);
          regbase[dst] =
            is_fixnum_range_cint(s)? cint_to_fixnum(s): cint_to_flonum(s);
          quickening(insns, pc, MULFIXFIXSMALL);
        } else {
          x1 = fixnum_to_double(v1);
          x2 = fixnum_to_double(v2);
          quickening(insns, pc, MULFIXFIX);
          goto MUL_FLOFLO;
        }
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, MULFIXFLO);
        goto MUL_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        quickening(insns, pc, MULFLOFIX);
        goto MUL_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, MULFLOFLO);
    MUL_FLOFLO:
        d = x1 * x2;
        regbase[dst] =
          is_fixnum_range_double(d)? double_to_fixnum(d): double_to_flonum(d);
      }
      break;

    default:
      {
        regbase[dst] = slow_mul(context, v1, v2);
        quickening(insns, pc, fastMulOpcode(tag));
      }
      break;
    }
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
    switch (tag = TAG_PAIR(get_tag(r1), get_tag(r2))) {
    case TP_FIXFIX:
      {
        if (v2 == FIXNUM_ZERO)
          regbase[dst] = gobj.g_flonum_nan;
        else {
          cint s = fixnum_to_cint(v1) % fixnum_to_cint(v2);
          // mod value should be in the fixnum range.
          regbase[dst] = cint_to_fixnum(s);
        }
        quickening(insns, pc, MODFIXFIX);
      }
      break;

    case TP_FIXFLO:
      {
        x1 = fixnum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, MODFIXFLO);
        goto MOD_FLOFLO;
      }

    case TP_FLOFIX:
      {
        x1 = flonum_to_double(v1);
        x2 = fixnum_to_double(v2);
        quickening(insns, pc, MODFLOFIX);
        goto MOD_FLOFLO;
      }

    case TP_FLOFLO:
      {
        x1 = flonum_to_double(v1);
        x2 = flonum_to_double(v2);
        quickening(insns, pc, MODFLOFLO);
    MOD_FLOFLO:
        if (isinf(x1) || x2 == 0.0f)
          regbase[dst] = gobj.g_flonum_nan;
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
        quickening(insns, pc, fastModOpcode(tag));
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
  // $dst = $r1 | r2

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
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
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
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETGLOBAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
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
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETIDX:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_ISUNDEF:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_ISOBJECT:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETFL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETA:
  // seta src
  //   src : source register
  // a = $src
  ENTER_INSN(__LINE__);
  set_a(context, regbase[get_first_operand_reg(insn)]);
  NEXT_INSN_INCPC();

I_GETA:
  // geta dst
  //   dst : destination register
  // $dst = a
  ENTER_INSN(__LINE__);
  regbase[get_first_operand_reg(insn)] = get_a(context);
  NEXT_INSN_INCPC();

I_GETERR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETGLOBALOBJ:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_NEWARGS:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_RET:
  // ret
  // returns from the function
  ENTER_INSN(__LINE__);
  if (fp == border)
    return 1;
  pop_special_registers(context, fp, regbase);
  update_context();
  NEXT_INSN_INCPC();

I_NOP:
  // nop
  
  ENTER_INSN(__LINE__);
  asm volatile("#NOP Instruction\n");
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
  //   if (%src) pc = pc + disp

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
  //   if (!%src) pc = pc + disp

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
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_GETLOCAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETARG:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SETLOCAL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_MAKECLOSURE:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_MAKEITERATOR:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_NEXTPROPNAME:
  ENTER_INSN(__LINE__);
  NEXT_INSN_INCPC();

I_CALL:
  // call fn nargs
  //
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_SEND:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
  NEXT_INSN_INCPC();

I_TAILCALL:
  ENTER_INSN(__LINE__);
  NOT_IMPLEMENTED();
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
