//
//  instructions.h
//  SSJSVM Project, Iwasaki-lab, UEC
//
//  Sho Takada, 2012-13
//  Akira Tanimura 2012-13
//  Akihiro Urushihara, 2013-14
//  Hideya Iwasaki, 2013-16
//  Ryota Fujii, 2013-14
//
// Both instructions-opcode.h and instructions-tables.h are generated
// from the file instructions.def by the gsed (gnu sed) command.

#ifndef INSTRUCTIONS_H_
#define INSTRUCTIONS_H_

typedef enum {
#include "instructions-opcode.h"
} Opcode;

// Operand Type
//
typedef enum {
  /*  0 */ SMALLPRIMITIVE,
  /*  1 */ BIGPRIMITIVE,
  /*  2 */ THREEOP,
  /*  3 */ TWOOP,
  /*  4 */ ONEOP,
  /*  5 */ ZEROOP,
  /*  6 */ UNCONDJUMP,
  /*  7 */ CONDJUMP,
  /*  8 */ GETVAR,
  /*  9 */ SETVAR,
  /* 10 */ MAKECLOSUREOP,
  /* 11 */ CALLOP,
  /* 12 */ TRYOP,
  /* 13 */ UNKNOWNOP
} OperandType;

typedef struct insn_info {
  char *insn_name;          // nemonic
  OperandType operand_type; // type
} InsnInfo;

// bytecode
typedef uint64_t Bytecode;
typedef uint32_t Counter;

// instruction
//
typedef struct instruction {
  Bytecode code;
#ifdef USE_THRESHOLD
  Opcode hitInst;
  Counter hitCount;
  Counter missCount;
#endif
#ifdef PRINT_QUICKENING_COUNT
  Counter quickeningCount;
  Counter executeCount;
#endif
} Instruction;

#define OPCODE_OFFSET         (48)
#define FIRST_OPERAND_OFFSET  (32)
#define SECOND_OPERAND_OFFSET (16)
#define CONSTINDEX_OFFSET     SECOND_OPERAND_OFFSET

#define SMALLPRIMITIVE_IMMMASK  (0xffffffff)
#define OPERAND_MASK            (0xffff)
#define OPCODE_MASK             ((Bytecode)(0xffff000000000000))
#define CONSTINDEX_MASK         ((Bytecode)(0x00000000ffff0000))

#define three_operands(op1, op2, op3) \
  (((Bytecode)(op1) << FIRST_OPERAND_OFFSET) | \
   ((Bytecode)(op2) << SECOND_OPERAND_OFFSET) | \
   (Bytecode)(op3))

#define makecode_three_operands(oc, op1, op2, op3) \
  (((Bytecode)(oc) << OPCODE_OFFSET) | three_operands(op1, op2, op3))

#define makecode_two_operands(oc, op1, op2) \
  makecode_three_operands(oc, op1, op2, 0)

#define makecode_one_operand(oc, op) \
  makecode_three_operands(oc, op, 0, 0)

#define makecode_no_operand(oc) \
  makecode_three_operands(oc, 0, 0, 0)

// macros for making various instructions
//
#define makecode_fixnum(dst, imm) \
  makecode_two_operands(FIXNUM, dst, imm)

#define makecode_specconst(dst, imm) \
  makecode_two_operands(SPECCONST, dst, imm)

#define makecode_number(dst, index) \
  makecode_two_operands(NUMBER, dst, index)

#define makecode_string(dst, index) \
  makecode_two_operands(STRING, dst, index)

#define makecode_error(dst, index) \
  makecode_two_operands(ERROR, dst, index)

#define makecode_regexp(dst, index, flag) \
  makecode_three_operands(REGEXP, (dst), (index), (flag))

#define makecode_arith(nemonic, op1, op2, op3) \
  makecode_three_operands(nemonic, op1, op2, op3)

#define makecode_comp(nemonic, op1, op2, op3) \
  makecode_three_operands(nemonic, op1, op2, op3)

#define makecode_bit(nemonic, op1, op2, op3) \
  makecode_three_operands(nemonic, op1, op2, op3)

#define makecode_getprop(op1, op2, op3) \
  makecode_three_operands(GETPROP, op1, op2, op3)

#define makecode_setprop(op1, op2, op3) \
  makecode_three_operands(SETPROP, op1, op2, op3)

#define makecode_getglobal(op1, op2, op3) \
  makecode_three_operands(GETGLOBAL, op1, op2, op3)

#define makecode_fastgetglobal(op1, op2, op3) \
  makecode_three_operands(FASTGETGLOBAL, op1, op2, op3)

#define makecode_slowgetglobal(op1, op2, op3) \
  makecode_three_operands(SLOWGETGLOBAL, op1, op2, op3)

#define makecode_setglobal(op1, op2, op3) \
  makecode_three_operands(SETGLOBAL, op1, op2, op3)

#define makecode_fastsetglobal(op1, op2, op3) \
  makecode_three_operands(FASTSETGLOBAL, op1, op2, op3)

#define makecode_slowsetglobal(op1, op2, op3) \
  makecode_three_operands(SLOWSETGLOBAL, op1, op2, op3)

#define makecode_jump(disp) \
  makecode_one_operand(JUMP, ((int16_t)(disp) & OPERAND_MASK))

#define makecode_cond_jump(opcode, src, disp) \
  makecode_two_operands(opcode, src, ((int16_t)(disp) & OPERAND_MASK))

#define makecode_getvar(opcode, op1, op2, op3) \
  makecode_three_operands(opcode, op1, op2, op3)

#define makecode_setvar(opcode, op1, op2, op3) \
  makecode_three_operands(opcode, op1, op2, op3)

#define makecode_makeclosure(opcode, dst, index) \
  makecode_two_operands(opcode, dst, index)

#define makecode_call(opcode, closure, argsc) \
  makecode_two_operands(opcode, closure, argc)

#define makecode_try(disp) \
  makecode_one_operand(TRY, ((int16_t)(disp) & OPERAND_MASK))

// adderss of a label for an instruction
//
typedef void *InsnLabel;

// macros for getting a specified part from a Bytecode
//
#define get_opcode(code) \
  ((Opcode)(((Bytecode)(code) & OPCODE_MASK) >> OPCODE_OFFSET))

#define get_first_operand(code) \
  ((Register)(((Bytecode)(code) >> FIRST_OPERAND_OFFSET) & OPERAND_MASK))

#define get_second_operand(code) \
  ((Register)(((Bytecode)(code) >> SECOND_OPERAND_OFFSET) & OPERAND_MASK))

#define get_third_operand(code) \
  ((Register)(((Bytecode)(code)) & OPERAND_MASK))

#define get_small_immediate(code) ((int)(get_second_operand(code)))

/*
#define calc_displacement(numOfInst, codeIndex, constIndex) \
  (numOfInst - (codeIndex + 1) + constIndex)
*/

#define calc_displacement(ninsns, code_index, const_index) \
  ((ninsns) - (code_index) + (const_index))

#define get_const_index(code) \
  ((uint16_t)(((code) & CONSTINDEX_MASK) >> CONSTINDEX_OFFSET))

#define update_displacement(code, disp) \
  (((code) & ~CONSTINDEX_MASK) | \
   (((disp) & OPERAND_MASK) << CONSTINDEX_OFFSET))

#define get_displacement(code) \
  ((uint16_t)(((code) & CONSTINDEX_MASK) >> CONSTINDEX_OFFSET))

#define get_uncondjump_displacement(code) \
   ((Displacement)(get_first_operand(code)))

#define get_condjump_displacement(code) \
   ((Displacement)(get_second_operand(code)))

#define STRING_TABLE_LIMIT    (3000)
#define NUMBER_TABLE_LIMIT    (3000)
#define CONSTANT_LIMIT        (10000)

#define INITIAL_HASH_SIZE       (1000)
#define INITIAL_PROPTABLE_SIZE  (1000)

#define SMALLNUM_OPCODE 0

// extern HashTable *instNameTable;
extern int numberOfFunctions;

// constant table
// This has the number of constants and the pointer to the constant table.
//
typedef struct constant_cell {
  int n_constant_values;        // number of constant values
  JSValue *constant_values;     // pointer to the array of constant values
} ConstantCell;

// typedef void (*builtin_function_t)(Context*, int);

#endif // INSTRUCTIONS_H_
