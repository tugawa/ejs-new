/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

/*
 * Both instructions-opcode.h and instructions-tables.h are generated
 * from the file instructions.def by the gsed (gnu sed) command.
 */

#ifndef INSTRUCTIONS_H_
#define INSTRUCTIONS_H_

typedef enum {
#include "instructions-opcode.h"
} Opcode;

/*
 * Operand Type
 */
typedef enum {
  /*   0
   *  ---------------------------------------------
   *  |          |          |                     |
   *  ---------------------------------------------
   *    opcode     register     immediate value
   *
   * Note that the immediate value has no tag for fixnum constant
   * but it has a tag for special constant.
   */
  SMALLPRIMITIVE,

  /*   1
   *  ---------------------------------------------
   *  |          |          |                     |
   *  ---------------------------------------------
   *    opcode     register    index of the constant table /
   &                           displacement to the constant
  */
  BIGPRIMITIVE,

  /*   2
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode     register   register   register
   */
  THREEOP,

  /*   3
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode     register   register   not used
   */
  TWOOP,

  /*   4
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode     register   not used   not used
   */
  ONEOP,

  /*   5
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode     not used   not used   not used
   */
  ZEROOP,

  /*   6
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode   displacement not used   not used
   */
  UNCONDJUMP,

  /*   7
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode     src       displacement not used
   *               register
   */
  CONDJUMP,

  /*   8
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode       link      offset    register
   */
  GETVAR,

  /*   9
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode       link      offset    register
   */
  SETVAR,

  /*  10
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode     dst       subscript   not used
   *               register
   */
  MAKECLOSUREOP,

  /*  11
   *  ---------------------------------------------
   *  |          |          |          |          |
   *  ---------------------------------------------
   *    opcode     func        nargs     not used
   *               register
   */
  CALLOP,

  /* 12 */ TRYOP,
  /* 13 */ UNKNOWNOP
} OperandType;

/*
 * Super Operand Type
 */
typedef enum {
  NONE,
  LIT,
  STR,
  NUM,
  /*  REGEXP, */
  SPEC
} InsnOperandType;

typedef struct insn_info {
  char *insn_name;               /* nemonic */
  OperandType otype;             /* operand type */
  InsnOperandType op0, op1, op2; /* insn type */
} InsnInfo;

/*
 * bytecode
 */
typedef uint64_t Bytecode;
typedef uint32_t Counter;

/*
 * adderss of a label for an instruction
 */
typedef void *InsnLabel;

/*
 * instruction
 */
typedef struct instruction {
  InsnLabel ilabel;  /* It is important that ilabel is the first member */
  Bytecode code;
#ifdef PROFILE
  Counter count;  /* counter */
  int logflag;    /* whether this instrution writes log info or not */
#endif
} Instruction;

/*
 * Each function has an array of Instructions, followed by its constant
 * table (literals), which is an array of JSValues.
 */
#define get_literal(insns, disp)  (((JSValue *)(insns))[disp])

#define OPCODE_OFFSET         (48)
#define FIRST_OPERAND_OFFSET  (32)
#define SECOND_OPERAND_OFFSET (16)
#define CONST_SUBSCR_OFFSET   SECOND_OPERAND_OFFSET

#define OPCODE_MASK             ((Bytecode)(0xffff000000000000))
#define OPERAND_MASK            (0xffff)
#define FIRST_OPERAND_MASK      ((Bytecode)(0x0000ffff00000000))
#define SECOND_OPERAND_MASK     ((Bytecode)(0x00000000ffff0000))
#define THIRD_OPERAND_MASK      ((Bytecode)(0x000000000000ffff))

#define SMALLPRIMITIVE_IMMMASK  ((Bytecode)(0x00000000ffffffff))
#define BIGPRIMITIVE_SUBSCRMASK ((Bytecode)(0x00000000ffffffff))

#define CONST_SUBSCR_MASK         ((Bytecode)(0x00000000ffff0000))

#define three_operands(op1, op2, op3)           \
  (((Bytecode)(op1) << FIRST_OPERAND_OFFSET) |  \
   ((Bytecode)(op2) << SECOND_OPERAND_OFFSET) | \
   (Bytecode)(op3))

#define makecode_three_operands(oc, op1, op2, op3)                      \
  (((Bytecode)(oc) << OPCODE_OFFSET) | three_operands(op1, op2, op3))

#define makecode_two_operands(oc, op1, op2)     \
  makecode_three_operands(oc, op1, op2, 0)

#define makecode_one_operand(oc, op)            \
  makecode_three_operands(oc, op, 0, 0)

#define makecode_no_operand(oc)                 \
  makecode_three_operands(oc, 0, 0, 0)

#define makecode_smallprimitive(oc, op, imm)            \
  (((Bytecode)(oc) << OPCODE_OFFSET) |                  \
   ((Bytecode)(op) << FIRST_OPERAND_OFFSET) |           \
   ((Bytecode)((imm) & SMALLPRIMITIVE_IMMMASK)))

#define makecode_bigprimitive(oc, op, index)            \
  (((Bytecode)(oc) << OPCODE_OFFSET) |                  \
   ((Bytecode)(op) << FIRST_OPERAND_OFFSET) |           \
   ((Bytecode)((index) & BIGPRIMITIVE_SUBSCRMASK)))

/*
 * macros for making various instructions
 */
#define makecode_fixnum(dst, imm)               \
  makecode_smallprimitive(FIXNUM, dst, imm)

#define makecode_specconst(dst, imm)            \
  makecode_smallprimitive(SPECCONST, dst, imm)

#define makecode_number(dst, index)             \
  makecode_bigprimitive(NUMBER, dst, index)

#define makecode_string(dst, index)             \
  makecode_bigprimitive(STRING, dst, index)

#define makecode_error(dst, index)              \
  makecode_bigprimitive(ERROR, dst, index)

#define makecode_regexp(dst, index)             \
  makecode_bigprimitive(REGEXP, dst, index)

#define makecode_arith(nemonic, op1, op2, op3)          \
  makecode_three_operands(nemonic, op1, op2, op3)

#define makecode_comp(nemonic, op1, op2, op3)           \
  makecode_three_operands(nemonic, op1, op2, op3)

#define makecode_bit(nemonic, op1, op2, op3)            \
  makecode_three_operands(nemonic, op1, op2, op3)

#define makecode_getprop(op1, op2, op3)                 \
  makecode_three_operands(GETPROP, op1, op2, op3)

#define makecode_setprop(op1, op2, op3)                 \
  makecode_three_operands(SETPROP, op1, op2, op3)

#define makecode_getglobal(op1, op2, op3)               \
  makecode_three_operands(GETGLOBAL, op1, op2, op3)

#define makecode_fastgetglobal(op1, op2, op3)           \
  makecode_three_operands(FASTGETGLOBAL, op1, op2, op3)

#define makecode_slowgetglobal(op1, op2, op3)           \
  makecode_three_operands(SLOWGETGLOBAL, op1, op2, op3)

#define makecode_setglobal(op1, op2, op3)               \
  makecode_three_operands(SETGLOBAL, op1, op2, op3)

#define makecode_fastsetglobal(op1, op2, op3)           \
  makecode_three_operands(FASTSETGLOBAL, op1, op2, op3)

#define makecode_slowsetglobal(op1, op2, op3)           \
  makecode_three_operands(SLOWSETGLOBAL, op1, op2, op3)

#define makecode_jump(opcode, disp)                                     \
  makecode_one_operand(opcode, ((int16_t)(disp) & OPERAND_MASK))

#define makecode_cond_jump(opcode, src, disp)                           \
  makecode_two_operands(opcode, src, ((int16_t)(disp) & OPERAND_MASK))

#define makecode_getvar(opcode, op1, op2, op3)          \
  makecode_three_operands(opcode, op1, op2, op3)

#define makecode_setvar(opcode, op1, op2, op3)          \
  makecode_three_operands(opcode, op1, op2, op3)

#define makecode_makeclosure(opcode, dst, index)        \
  makecode_two_operands(opcode, dst, index)

#define makecode_call(opcode, closure, argsc)   \
  makecode_two_operands(opcode, closure, argc)

/*
 * macros for getting a specified part from a Bytecode
 */
#define get_opcode(code)                                        \
  ((Opcode)(((Bytecode)(code) & OPCODE_MASK) >> OPCODE_OFFSET))

#define get_first_operand(code)                                 \
  (((Bytecode)(code) >> FIRST_OPERAND_OFFSET) & OPERAND_MASK)

#define get_second_operand(code)                                \
  (((Bytecode)(code) >> SECOND_OPERAND_OFFSET) & OPERAND_MASK)

#define get_third_operand(code) (((Bytecode)(code)) & OPERAND_MASK)

#define get_first_operand_reg(code) ((Register)(get_first_operand(code)))

#define get_second_operand_reg(code) ((Register)(get_second_operand(code)))

#define get_third_operand_reg(code)  ((Register)(get_third_operand(code)))

#define get_first_operand_value(code) (regbase[get_first_operand_reg(code)])

#define get_second_operand_value(code) (regbase[get_second_operand_reg(code)])

#define get_third_operand_value(code)  (regbase[get_third_operand_reg(code)])

#define get_first_operand_disp(code) ((Displacement)(get_first_operand(code)))

#define get_second_operand_disp(code)           \
  ((Displacement)(get_second_operand(code)))

#define get_third_operand_disp(code)  ((Displacement)(get_third_operand(code)))

#define get_first_operand_subscr(code) ((Subscript)(get_first_operand(code)))

#define get_second_operand_subscr(code) ((Subscript)(get_second_operand(code)))

#define get_third_operand_subscr(code)  ((Subscript)(get_third_operand(code)))

/*
 * #define get_small_immediate(code) \
 *  (((Bytecode)(code)) & SMALLPRIMITIVE_IMMMASK)
 */

#define get_small_immediate(code)                                       \
  (Bytecode)((int32_t)(((Bytecode)(code)) & SMALLPRIMITIVE_IMMMASK))

#define get_first_operand_disp(code) ((Displacement)(get_first_operand(code)))

#define get_second_operand_disp(code)           \
  ((Displacement)(get_second_operand(code)))

#define get_third_operand_disp(code) ((Displacement)(get_third_operand(code)))

#define update_first_operand_disp(code, disp)           \
  makecode_three_operands(get_opcode(code), disp,       \
                          get_second_operand_reg(code), \
                          get_third_operand_reg(code))

#define update_second_operand_disp(code, disp)                          \
  makecode_three_operands(get_opcode(code), get_first_operand_reg(code), \
                          disp, get_third_operand_reg(code))

#define update_third_operand_disp(code, disp)                           \
  makecode_three_operands(get_opcode(code), get_first_operand_reg(code), \
                          get_second_operand_reg(code), disp)

#define get_big_subscr(code) (((Bytecode)(code)) & BIGPRIMITIVE_SUBSCRMASK)

#define get_big_disp(code)                                              \
  (Bytecode)((int32_t)(((Bytecode)(code)) & BIGPRIMITIVE_SUBSCRMASK))

/* #define get_small_immediate(code) ((int)(get_second_operand(code))) */

#define get_first_operand_int(code) ((int)((int16_t)(get_first_operand(code))))

#define get_second_operand_int(code)            \
  ((int)((int16_t)(get_second_operand(code))))

#define get_third_operand_int(code) ((int)((int16_t)(get_third_operand(code))))

/*
 * #define calc_displacement(numOfInst, codeIndex, constIndex) \
 *  (numOfInst - (codeIndex + 1) + constIndex)
 * #define calc_displacement(ninsns, code_subscr, const_subscr) \
 *  ((ninsns) - (code_subscr) + (const_subscr))
 */

#define calc_displacement(ninsns, code_subscr, const_subscr)            \
  (((ninsns) - (code_subscr)) * (sizeof(Instruction) / sizeof(JSValue)) \
   + (const_subscr))

/*
 * #define get_const_index(code) \
 *  ((uint16_t)(((code) & CONSTINDEX_MASK) >> CONSTINDEX_OFFSET))
 *
 * #define update_displacement(code, disp) \
 *   (((code) & ~CONSTINDEX_MASK) | \
 *   (((disp) & OPERAND_MASK) << CONSTINDEX_OFFSET))
 */
#define update_displacement(code, disp)                                 \
  makecode_bigprimitive(get_opcode(code), get_first_operand_reg(code), disp)

/*
 * #define get_displacement(code) \
 *   ((uint16_t)(((code) & CONSTINDEX_MASK) >> CONSTINDEX_OFFSET))
 */
#define STRING_TABLE_LIMIT    (3000)
#define NUMBER_TABLE_LIMIT    (3000)
#define CONSTANT_LIMIT        (10000)

#define INITIAL_HASH_SIZE       (100)
#define INITIAL_PROPTABLE_SIZE  (100)

#define SMALLNUM_OPCODE 0

#define LOAD_OK     0
#define LOAD_FAIL  (-1)

#endif /* INSTRUCTIONS_H_ */
