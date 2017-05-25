#
# deletes comment lines
#
/^\/\/.*/d
#
# LABELONLY
#
s/^\([a-z][a-z]*\)  *.*  *LABELONLY.*/I_\U\1\E:/
#
# SMALLPRIMITIVE --- insn dst imm
#
s/^\([a-z][a-z]*\)  *SMALLPRIMITIVE.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register dst = get_first_operand_reg(insn);\
    int64_t imm = get_small_immediate(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# BIGPRIMITIVE --- insn dst disp
#
s/^\([a-z][a-z]*\)  *BIGPRIMITIVE.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register dst = get_first_operand_reg(insn);\
    Displacement disp = get_big_disp(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# special case --- setarray dst s src
#
s/^\(setarray\)  *THREEOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register dst = get_first_operand_reg(insn);\
    Subscript s = get_second_operand_subscr(insn);\
    Register src = get_third_operand_reg(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# THREEOP --- insn r0 r1 r2
#
s/^\([a-z][a-z]*\)  *THREEOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register r0 = get_first_operand_reg(insn);\
    Register r1 = get_second_operand_reg(insn);\
    Register r2 = get_third_operand_reg(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# TWOOP --- insn r0 r1
#
s/^\([a-z][a-z]*\)  *TWOOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register r0 = get_first_operand_reg(insn);\
    Register r1 = get_second_operand_reg(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# special case --- setfl newfl
#
s/^\(setfl\)  *ONEOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    int newfl = get_first_operand_int(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# ONEOP --- insn r
#
s/^\([a-z][a-z]*\)  *ONEOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register r = get_first_operand_reg(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# special case --- throw
#
s/^\(throw\)  *ZEROOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_NOINCPC();\
/
#
# ZEROOP --- insn
#
s/^\([a-z][a-z]*\)  *ZEROOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# special case --- pushhandler disp
#
s/^\(pushhandler\)  *UNCONDJUMP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Displacement disp = get_first_operand_disp(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# UNCONDJUMP --- insn disp
#
s/^\([a-z][a-z]*\)  *UNCONDJUMP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Displacement disp = get_first_operand_disp(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_NOINCPC();\
/
#
# CONDJUMP --- insn r disp
#
s/^\([a-z][a-z]*\)  *CONDJUMP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register r = get_first_operand_reg(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# GETVAR --- insn dst link index
#
s/^\([a-z][a-z]*\)  *GETVAR.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register dst = get_first_operand_reg(insn);\
    int link = get_second_operand_int(insn);\
    Subscript index = get_third_operand_subscr(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# SETVAR --- insn link index src
#
s/^\([a-z][a-z]*\)  *SETVAR.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    int link = get_first_operand_int(insn);\
    Subscript index = get_second_operand_subscr(insn);\
    Register src = get_third_operand_reg(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# MAKECLOSURE --- insn dst index
#
s/^\([a-z][a-z]*\)  *MAKECLOSURE.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register dst = get_first_operand_reg(insn);\
    Subscript index = get_second_operand_subscr(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
# CALLOP --- insn fn args
#
s/^\([a-z][a-z]*\)  *CALLOP.*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
    Register r0 = get_first_operand_reg(insn);\
    int nargs = get_second_operand_int(insn);\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
#
#
#
s/^\([a-z][a-z]*\)  *\([A-Z][A-Z]*\).*/I_\U\1\E:\
  ENTER_INSN(__LINE__);\
  {\
#include "insns\/\1.def"\
  }\
  NEXT_INSN_INCPC();\
/
