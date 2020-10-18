#ifndef ICCPROF_H_
#define ICCPROF_H_

#ifdef ICCPROF

#define EXTERN extern
#include "header.h"
#include "cell-header.h"
#include "types.h"

#define TYPE_SIZE 13
#define ONE_OP_SIZE 15
#define TWO_OP_SIZE 18
#define THREE_OP_SIZE 1

/*
long icc_add[TYPE_SIZE][TYPE_SIZE];
long icc_bitand[TYPE_SIZE][TYPE_SIZE];
long icc_bitor[TYPE_SIZE][TYPE_SIZE];
long icc_call[TYPE_SIZE];
long icc_div[TYPE_SIZE][TYPE_SIZE];
long icc_eq[TYPE_SIZE][TYPE_SIZE];
long icc_equal[TYPE_SIZE][TYPE_SIZE];
long icc_getprop[TYPE_SIZE][TYPE_SIZE];
long icc_leftshift[TYPE_SIZE][TYPE_SIZE];
long icc_lessthan[TYPE_SIZE][TYPE_SIZE];
long icc_lessthanequal[TYPE_SIZE][TYPE_SIZE];
long icc_mod[TYPE_SIZE][TYPE_SIZE];
long icc_mul[TYPE_SIZE][TYPE_SIZE];
long icc_new[TYPE_SIZE];
long icc_rightshift[TYPE_SIZE][TYPE_SIZE];
long icc_setprop[TYPE_SIZE][TYPE_SIZE][TYPE_SIZE];
long icc_sub[TYPE_SIZE][TYPE_SIZE];
long icc_tailcall[TYPE_SIZE];
long icc_unsignedrightshift[TYPE_SIZE][TYPE_SIZE];
long icc_getglobal[TYPE_SIZE];
long icc_instanceof[TYPE_SIZE][TYPE_SIZE];
long icc_isobject[TYPE_SIZE];
long icc_isundef[TYPE_SIZE];
long icc_jumpfalse[TYPE_SIZE];
long icc_jumptrue[TYPE_SIZE];
long icc_makeiterator[TYPE_SIZE];
long icc_move[TYPE_SIZE];
long icc_nextpropnameidx[TYPE_SIZE];
long icc_not[TYPE_SIZE];
long icc_seta[TYPE_SIZE];
long icc_setarg[TYPE_SIZE];
long icc_setarray[TYPE_SIZE][TYPE_SIZE];
long icc_setglobal[TYPE_SIZE][TYPE_SIZE];
long icc_setlocal[TYPE_SIZE];
*/
long *icc_call;
long *icc_new;
long *icc_tailcall;
long *icc_getglobal;
long *icc_isobject;
long *icc_isundef;
long *icc_jumpfalse;
long *icc_jumptrue;
long *icc_makeiterator;
long *icc_move;
long *icc_nextpropnameidx;
long *icc_not;
long *icc_seta;
long *icc_setarg;
long *icc_setlocal;
long **icc_add;
long **icc_bitand;
long **icc_bitor;
long **icc_div;
long **icc_eq;
long **icc_equal;
long **icc_getprop;
long **icc_leftshift;
long **icc_lessthan;
long **icc_lessthanequal;
long **icc_mod;
long **icc_mul;
long **icc_rightshift;
long **icc_sub;
long **icc_unsignedrightshift;
long **icc_instanceof;
long **icc_setarray;
long **icc_setglobal;
long ***icc_setprop;

int icc_value2index(JSValue value);
char *icc_index2type_name(int index);
long *get_1op_insn_counter(char *iname);
long **get_2op_insn_counter(char *iname);
long ***get_3op_insn_counter(char *iname);
void iccprof_init();
void write_icc_profile(FILE *fp);

long *one_ops[ONE_OP_SIZE];
long **two_ops[TWO_OP_SIZE];
long ***three_ops[THREE_OP_SIZE];
/*
  long *one_ops[] = { icc_call, icc_new, icc_tailcall, icc_getglobal, icc_isobject, icc_isundef, icc_jumpfalse,
    icc_jumptrue, icc_makeiterator, icc_move, icc_nextpropnameidx, icc_not, icc_seta, icc_setarg, icc_setlocal };
  long **two_ops[] = { icc_add, icc_bitand, icc_bitor, icc_div, icc_eq, icc_equal,
    icc_getprop, icc_leftshift, icc_lessthan, icc_lessthanequal, icc_mod, icc_mul,
    icc_rightshift, icc_sub, icc_unsignedrightshift, icc_instanceof, icc_setarray, icc_setglobal };
  long ***three_ops[] = { icc_setprop };
*/
  char *one_op_names[ONE_OP_SIZE];
  char *two_op_names[TWO_OP_SIZE];
  char *three_op_names[THREE_OP_SIZE];


#endif
#endif /* ICCPROF_H_ */