#ifdef ICCPROF

#include "iccprof.h"

char *one_op_names[] = { "call", "new", "tailcall", "getglobal", "isobject", "isundef", "jumpfalse",
  "jumptrue", "makeiterator", "move", "nextpropnameidx", "not", "seta", "setarg", "setlocal" };
char *two_op_names[] = {"add", "bitand", "bitor", "div", "eq", "equal", "getprop", "leftshift", "lessthan",
  "lessthanequal","mod", "mul", "rightshift", "sub", "unsignedrightshift", "instanceof", "setarray", "setglobal" };
char *three_op_names[] = { "setprop" };

int icc_value2index(JSValue value){
  switch (get_tag(value)){
    case T_FIXNUM: return 0;
    case T_FLONUM: return 1;
    case T_STRING: return 2;
    case T_SPECIAL: return 3;
    case T_GENERIC:
      switch (HEADER0_GET_TYPE(((header_t *) value)[-1])){
        case HTAG_SIMPLE_OBJECT: return 4;
        case HTAG_ARRAY: return 5;
        case HTAG_FUNCTION: return 6;
        case HTAG_BUILTIN: return 7;
        case HTAG_ITERATOR: return 8;
        case HTAG_REGEXP: return 9;
        case HTAG_BOXED_STRING: return 10;
        case HTAG_BOXED_NUMBER: return 11;
        case HTAG_BOXED_BOOLEAN: return 12;
        default: break;
      }
    default: break;
  }
  fprintf(stderr, "Illigal value in icc_value2index");
  assert(0);
  return -1;
}

char *icc_index2type_name(int index){
  switch (index){
      case 0: return "fixnum";
      case 1: return "flonum";
      case 2: return "string";
      case 3: return "special";
      case 4: return "simple_object";
      case 5: return "array";
      case 6: return "function";
      case 7: return "builtin";
      case 8: return "iterator";
      case 9: return "regexp";
      case 10: return "string_object";
      case 11: return "number_object";
      case 12: return "boolean_object";
      default: break;
    }
  fprintf(stderr, "Illigal value in icc_index2type_name");
  assert(0);
  return NULL;
}

long *get_1op_insn_counter(char *iname){
  if(strcmp(iname, "call") == 0) return icc_call;
  if(strcmp(iname, "new") == 0) return icc_new;
  if(strcmp(iname, "tailcall") == 0) return icc_tailcall;
  if(strcmp(iname, "getglobal") == 0) return icc_getglobal;
  if(strcmp(iname, "isobject") == 0) return icc_isobject;
  if(strcmp(iname, "isundef") == 0) return icc_isundef;
  if(strcmp(iname, "jumpfalse") == 0) return icc_jumpfalse;
  if(strcmp(iname, "jumptrue") == 0) return icc_jumptrue;
  if(strcmp(iname, "makeiterator") == 0) return icc_makeiterator;
  if(strcmp(iname, "move") == 0) return icc_move;
  if(strcmp(iname, "nextpropnameidx") == 0) return icc_nextpropnameidx;
  if(strcmp(iname, "not") == 0) return icc_not;
  if(strcmp(iname, "seta") == 0) return icc_seta;
  if(strcmp(iname, "setarg") == 0) return icc_setarg;
  if(strcmp(iname, "setlocal") == 0) return icc_setlocal;
  fprintf(stderr, "Illigal value in get_1op_insn_counter: %s", iname);
  assert(0);
  return NULL;
}
long **get_2op_insn_counter(char *iname){
  if(strcmp(iname, "add") == 0) return icc_add;
  if(strcmp(iname, "bitand") == 0) return icc_bitand;
  if(strcmp(iname, "bitor") == 0) return icc_bitor;
  if(strcmp(iname, "div") == 0) return icc_div;
  if(strcmp(iname, "eq") == 0) return icc_eq;
  if(strcmp(iname, "equal") == 0) return icc_equal;
  if(strcmp(iname, "getprop") == 0) return icc_getprop;
  if(strcmp(iname, "leftshift") == 0) return icc_leftshift;
  if(strcmp(iname, "lessthan") == 0) return icc_lessthan;
  if(strcmp(iname, "lessthanequal") == 0) return icc_lessthanequal;
  if(strcmp(iname, "mod") == 0) return icc_mod;
  if(strcmp(iname, "mul") == 0) return icc_mul;
  if(strcmp(iname, "rightshift") == 0) return icc_rightshift;
  if(strcmp(iname, "sub") == 0) return icc_sub;
  if(strcmp(iname, "unsignedrightshift") == 0) return icc_unsignedrightshift;
  if(strcmp(iname, "instanceof") == 0) return icc_instanceof;
  if(strcmp(iname, "setarray") == 0) return icc_setarray;
  if(strcmp(iname, "setglobal") == 0) return icc_setglobal;
  fprintf(stderr, "Illigal value in get_2op_insn_counter: %s", iname);
  assert(0);
  return NULL;
}

long ***get_3op_insn_counter(char *iname){
  if(strcmp(iname, "setprop") == 0) return icc_setprop;
  fprintf(stderr, "Illigal value in get_3op_insn_counter: %s", iname);
  assert(0);
  return NULL;
}

void iccprof_init(){
  int i,j,k,l;

  for(i=0; i<ONE_OP_SIZE; i++){
    if((one_ops[i] = (long *)malloc(TYPE_SIZE * sizeof(long))) == NULL){
      fprintf(stderr, "Fail to allocate memory in iccprof_init");
      return;
    }
    for(j=0; j<TYPE_SIZE; j++){
      one_ops[i][j] = 0;
    }
  }
  for(i=0; i<TWO_OP_SIZE; i++){
    if((two_ops[i] = (long **)malloc(TYPE_SIZE * sizeof(long*))) == NULL){
      fprintf(stderr, "Fail to allocate memory in iccprof_init");
      return;
    }
    for(j=0; j<TYPE_SIZE; j++){
      if((two_ops[i][j] = (long *)malloc(TYPE_SIZE * sizeof(long))) == NULL){
        fprintf(stderr, "Fail to allocate memory in iccprof_init");
        return;
      }
      for(k=0; k<TYPE_SIZE; k++){
        two_ops[i][j][k] = 0;
      }
    }
  }
  for(i=0; i<THREE_OP_SIZE; i++){
    if((three_ops[i] = (long ***)malloc(TYPE_SIZE * sizeof(long**))) == NULL){
      fprintf(stderr, "Fail to allocate memory in iccprof_init");
      return;
    }
    for(j=0; j<TYPE_SIZE; j++){
      if((three_ops[i][j] = (long **)malloc(TYPE_SIZE * sizeof(long*))) == NULL){
        fprintf(stderr, "Fail to allocate memory in iccprof_init");
        return;
      }
      for(k=0; k<TYPE_SIZE; k++){
        if((three_ops[i][j][k] = (long *)malloc(TYPE_SIZE * sizeof(long))) == NULL){
          fprintf(stderr, "Fail to allocate memory in iccprof_init");
          return;
        }
        for(l=0; l<TYPE_SIZE; l++){
          three_ops[i][j][k][l] = 0;
        }
      }
    }
  }

  icc_call = one_ops[0];
  icc_new = one_ops[1];
  icc_tailcall = one_ops[2];
  icc_getglobal = one_ops[3];
  icc_isobject = one_ops[4];
  icc_isundef = one_ops[5];
  icc_jumpfalse = one_ops[6];
  icc_jumptrue = one_ops[7];
  icc_makeiterator = one_ops[8];
  icc_move = one_ops[9];
  icc_nextpropnameidx = one_ops[10];
  icc_not = one_ops[11];
  icc_seta = one_ops[12];
  icc_setarg = one_ops[13];
  icc_setlocal = one_ops[14];
  icc_add = two_ops[0];
  icc_bitand = two_ops[1];
  icc_bitor = two_ops[2];
  icc_div = two_ops[3];
  icc_eq = two_ops[4];
  icc_equal = two_ops[5];
  icc_getprop = two_ops[6];
  icc_leftshift = two_ops[7];
  icc_lessthan = two_ops[8];
  icc_lessthanequal = two_ops[9];
  icc_mod = two_ops[10];
  icc_mul = two_ops[11];
  icc_rightshift = two_ops[12];
  icc_sub = two_ops[13];
  icc_unsignedrightshift = two_ops[14];
  icc_instanceof = two_ops[15];
  icc_setarray = two_ops[16];
  icc_setglobal = two_ops[17];
  icc_setprop = three_ops[0];
}
void write_icc_profile(FILE *fp){
  int i,j,k,l;

  for(i=0; i<ONE_OP_SIZE; i++){
    fprintf(fp, "#INSN %s\n", one_op_names[i]);
    for(j=0; j<TYPE_SIZE; j++){
      if(one_ops[i][j] == 0) continue; /* Skip 0 times called. */
      fprintf(fp, "#OPRN %s %ld\n", icc_index2type_name(j), one_ops[i][j]);
    }
  }
  for(i=0; i<TWO_OP_SIZE; i++){
    fprintf(fp, "#INSN %s\n", two_op_names[i]);
    for(j=0; j<TYPE_SIZE; j++){
      for(k=0; k<TYPE_SIZE; k++){
        if(two_ops[i][j][k] == 0) continue; /* Skip 0 times called. */
        fprintf(fp, "#OPRN %s,%s %ld\n", icc_index2type_name(j), icc_index2type_name(k), two_ops[i][j][k]);
      }
    }
  }
  for(i=0; i<THREE_OP_SIZE; i++){
    fprintf(fp, "#INSN %s\n", three_op_names[i]);
    for(j=0; j<TYPE_SIZE; j++){
      for(k=0; k<TYPE_SIZE; k++){
        for(l=0; l<TYPE_SIZE; l++){
          if(three_ops[i][j][k][l] == 0) continue; /* Skip 0 times called. */
          fprintf(fp, "#OPRN %s,%s,%s %ld\n", icc_index2type_name(j), icc_index2type_name(k), icc_index2type_name(l), three_ops[i][j][k][l]);
        }
      }
    }
  }
}
#endif