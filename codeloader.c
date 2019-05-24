/*
   codeloader.c

   eJS Project
     Kochi University of Technology
     The University of Electro-communications

     Tomoharu Ugawa, 2016-19
     Hideya Iwasaki, 2016-19
*/

#include "prefix.h"
#define EXTERN extern
#include "header.h"

/*
 * Either of the following two should be defined.
 */
// #define USE_SBC
#define USE_OBC

/*
   information of instructions
 */
static InsnInfo insn_info_table[] = {
#include "instructions-table.h"
};

char *insn_nemonic(int opcode) {
  return insn_info_table[opcode].insn_name;
}

typedef struct {
  Opcode oc;
  int size;
  InsnOperandType type;
} ConstInfo;

typedef struct {
  int n_const_info;
  ConstInfo *const_info;
} CItable;

/*
   instruction table
 */

#define LOADBUFLEN 1024

#ifdef USE_SBC
extern int insn_load_sbc(Context *, ConstantCell *, Bytecode *, int);
#endif

#ifdef USE_OBC
extern void init_constant_info(CItable *citable, int i);
extern void add_constant_info(CItable *ci, Opcode oc, int index, InsnOperandType type);
extern void const_load(Context *ctx, CItable citable, ConstantCell *constant);
extern int insn_load_obc(Context *, ConstantCell *, Bytecode *, int, CItable *);
#endif

extern uint32_t decode_escape_char(char *);
extern int print_function_table(FunctionTable *, int);
extern void print_bytecode(Instruction *, int);

FILE *file_pointer;

#ifdef USE_SBC
/*
   reads the next line from the input stream
 */
inline void step_load_code(char *buf, int buflen) {
  fgets(buf, buflen, file_pointer == NULL? stdin: file_pointer);
}

#define DELIM " \n\r"
#define DELIM2 "\n\r"
#define first_token(b) strtok(b, DELIM)
#define next_token()   strtok(NULL, DELIM)
#define next_token2()  strtok(NULL, DELIM2)

inline int check_read_token(char *buf, char *tok) {
  char *p;
  p = first_token(buf);
  if (strcmp(p, tok) != 0)
    LOG_EXIT("Error: %s is not defined", tok);
  return atoi(next_token());
}
#endif /* USE_SBC */

/*
   codeloader
 */
int code_loader(Context *ctx, FunctionTable *ftable, int start) {
  ConstantCell ctable;
  Bytecode* bytecodes;
  int nfuncs, callentry, sendentry, nlocals, ninsns;
  int i, j, ret;
#ifdef USE_SBC
  char buf[LOADBUFLEN];
#endif
#ifdef USE_OBC
  CItable citable;
  unsigned char b[2];
#endif

#ifdef USE_SBC
#define next_buf()      step_load_code(buf, LOADBUFLEN)
#define buf_to_int(s)   check_read_token(buf, s)
#endif

#ifdef USE_OBC
#define next_buf()      fread(b, sizeof(unsigned char), 2, file_pointer)
#define buf_to_int(s)   (b[0] * 256 + b[1])
#endif

#if defined(USE_SBC) && defined(USE_OBC)
  fprintf(stderr, "Fatal error: both USE_SBC and USE_OBC are defined.\n");
  exit(0);
#endif

  // checks the funclength and obtain the number of functions
  next_buf();
  nfuncs = buf_to_int("funcLength");

  // reads each function
  for (i = 0; i < nfuncs; i++) {
    // callentry
    next_buf();
    callentry = buf_to_int("callentry");

    // sendentry
    next_buf();
    sendentry = buf_to_int("sendentry");

    // numberOfLocals
    next_buf();
    nlocals = buf_to_int("numberOfLocals");

    // numberOfInstruction
    next_buf();
    ninsns = buf_to_int("numberOfInstruction");

    // initializes constant table
    init_constant_cell(&ctable, i);
#ifdef USE_OBC
    init_constant_info(&citable, i);
#endif

    // initilaizes bytecode array
    bytecodes = (Bytecode*)malloc(sizeof(Bytecode) * ninsns);
    if (bytecodes == NULL)
      LOG_EXIT("%dth func: cannnot malloc bytecode", i);

    // loads instructions for each function
    for (j = 0; j < ninsns; j++)
#ifdef USE_SBC
      ret = insn_load_sbc(ctx, &ctable, bytecodes, j);
#endif
#ifdef USE_OBC
      ret = insn_load_obc(ctx, &ctable, bytecodes, j, &citable);
#endif

#ifdef USE_OBC
    // loads constants
    const_load(ctx, citable, &ctable);
#endif

    ret = update_function_table(ftable, i + start, &ctable, bytecodes,
                                callentry, sendentry, nlocals, ninsns);
    /* end_constant_cell(&ctable); */
    free(ctable.constant_values);
#ifdef USE_OBC
    free(citable.const_info);
#endif
  }
  if (ftable_flag == TRUE)
    print_function_table(ftable, i + start);
  return nfuncs;

#undef next_buf
#undef buf_to_int
}

#ifdef USE_OBC
void string_load(Context *ctx, ConstantCell *constant, int size) {
  char *str;

  str = (char*) malloc(sizeof(char) * size);
  if (fread(str, sizeof(char), size, file_pointer) < size)
    LOG_ERR("string literal too short.");
  decode_escape_char(str);
  add_constant_string(ctx, constant, str);
  free(str);
}

void double_load(Context *ctx, ConstantCell *constant) {
  union {
    double d;
    unsigned char b[8];
  } u;
  
  fread(&u.b, sizeof(unsigned char), 8, file_pointer);
#ifdef BIG_ENDIAN
  {
    int i;
    for (i = 0; i < 4; i++) {
      unsigned char c;
      c = u.b[i]; u.b[i] = u.b[7 - i]; u.b[7 - i] = c;
    }
  }
#endif
  // printf("double loaded, value = %lf\n", u.d);
  add_constant_number(ctx, constant, u.d);
}

void const_load(Context *ctx, CItable citable, ConstantCell *constant) {
  int oi;
  unsigned char b[2];

#define next_buf()      fread(b, sizeof(unsigned char), 2, file_pointer)
#define buf_to_int(s)   (b[0] * 256 + b[1])
  
  for (oi = 0; oi < citable.n_const_info; oi++) {
    int size;

    next_buf();
    citable.const_info[oi].size = size = buf_to_int();
    if (size > 0) {
      Opcode oc = citable.const_info[oi].oc;
      switch (insn_info_table[oc].operand_type) {
      case BIGPRIMITIVE:
        switch (oc) {
        case ERROR:
        case STRING:
          string_load(ctx, constant, size);
          break;
        case NUMBER:
          double_load(ctx, constant);
          break;
#ifdef USE_REGEXP
#ifdef need_regexp
        case REGEXP:
          LOG_ERR("sorry, loading regexp is not implemented yet.");
          break;
#endif
#endif
        default:
          LOG_ERR("Error: unexpected instruction in loading constants");
          break;
        }
        break;
      case THREEOP:
        {
          InsnOperandType type = citable.const_info[oi].type;
          switch (type) {
          case STR:
            string_load(ctx, constant, size);
            break;
          case NUM:
            double_load(ctx, constant);
            break;
          default:
            LOG_ERR("Error: unexpected operand type in loading constants");
            break;
          }
          break;
        }
      default:
        LOG_ERR("Error: unexpected operand type in loading constants");
        break;
      }
    }
  }
}
#endif

/*
   initializes the code loader
 */
void init_code_loader(FILE *fp) {
  file_pointer = fp;
}

/*
   finalizes the code loader
 */
void end_code_loader() {
  if (repl_flag == TRUE)
    return;
  if (file_pointer != NULL)
    fclose(file_pointer);
}

#ifdef USE_SBC
#define NOT_OPCODE ((Opcode)(-1))

Opcode find_insn(char* s) {
  int i;
  int numinsts = sizeof(insn_info_table) / sizeof(InsnInfo);
  for (i = 0; i < numinsts; i++)
    if (strcmp(insn_info_table[i].insn_name, s) == 0)
      return i;
  // not found in the instruction table
  return NOT_OPCODE;
}
#endif /* USE_SBC */

#define LOAD_OK     0
#define LOAD_FAIL  (-1)

#ifdef USE_OBC
Bytecode convertToBc(unsigned char buf[8]) {
  int i;
  Bytecode ret;

  ret = 0;
  for (i = 0; i < 8; i++)
    ret = ret * 256 + buf[i];
  return ret;
}

#define OPTYPE_ERROR ((InsnOperandType)(-1))

InsnOperandType si_optype(Opcode oc, int i) {
  switch (i) {
    case 0:
      return insn_info_table[oc].op0;
    case 1:
      return insn_info_table[oc].op1;
    case 2:
      return insn_info_table[oc].op2;
    default:
      return OPTYPE_ERROR;
  }
}
#endif

#ifdef USE_SBC
#define load_op(op_type, op)                           \
do {                                                   \
  op = 0;                                              \
  switch (op_type) {                                   \
  case NONE:                                           \
    break;                                             \
  case LIT:                                            \
    { op = atoi(next_token()); }                       \
    break;                                             \
  case STR:                                            \
    {                                                  \
      char *str;                                       \
      uint32_t len;                                    \
      str = next_token();                              \
      if (str == NULL) str = "";                       \
      else len = decode_escape_char(str);              \
      op = add_constant_string(ctx, constant, str);    \
    }                                                  \
    break;                                             \
  case NUM:                                            \
    {                                                  \
      double number;                                   \
      number = atof(next_token());                     \
      op = add_constant_number(ctx, constant, number); \
    }                                                  \
    break;                                             \
  case SPEC:                                           \
    { op = specstr_to_jsvalue(next_token()); }         \
    break;                                             \
  default:                                             \
    return LOAD_FAIL;                                  \
  }                                                    \
} while(0)
#endif /* USE_SBC */

/*
   loads an instruction
 */
#ifdef USE_SBC
int insn_load_sbc(Context *ctx, ConstantCell *constant, Bytecode *bytecodes, int pc) {
  char buf[LOADBUFLEN];
  char *tokp;
  Opcode oc;

  step_load_code(buf, LOADBUFLEN);
  tokp = first_token(buf);
  oc = find_insn(tokp);
  if (oc == NOT_OPCODE) {
    // instruction is not found in the instruction info table
    LOG_ERR("Illegal instruction: %s", tokp);
    bytecodes[pc] = (Bytecode)(-1);
    return LOAD_FAIL;
  }
  switch (insn_info_table[oc].operand_type) {
  case SMALLPRIMITIVE:
    {
      Register dst;
      dst = atoi(next_token());
      switch (oc) {
      case FIXNUM:
        bytecodes[pc] = makecode_fixnum(dst, atoi(next_token()));
        break;
      case SPECCONST:
        bytecodes[pc] =
          makecode_specconst(dst, specstr_to_jsvalue(next_token()));
        break;
      default:
        return LOAD_FAIL;
      }
      return LOAD_OK;
    }

  case BIGPRIMITIVE:
    {
      Register dst;
      dst = atoi(next_token());
      switch (oc) {
      case NUMBER:
        {
          double number;
          int index;
          number = atof(next_token());
          // writes the number into the constant table
          index = add_constant_number(ctx, constant, number);
          bytecodes[pc] = makecode_number(dst, index);
        }
        break;
      case STRING:
      case ERROR:
        {
          char *str;
          int index;
          uint32_t len;
          // str = next_token2();
          str = next_token();
          if (str == NULL) str = "";
          else len = decode_escape_char(str);
          index = add_constant_string(ctx, constant, str);
          if (oc == STRING)
            bytecodes[pc] = makecode_string(dst, index);
          else
            bytecodes[pc] = makecode_error(dst, index);
        }
        break;
#ifdef USE_REGEXP
#ifdef need_regexp
      case REGEXP:
        {
          char *str;
          int flag, index;
          uint32_t len;
          flag = atoi(next_token());
          str = next_token();
          if (str == NULL) str = "";
          else len = decode_escape_char(str);
          index = add_constant_regexp(ctx, constant, str, flag);
          bytecodes[pc] = makecode_regexp(dst, index);
        }
        break;
#endif /* need_regexp */
#endif
      default:
        return LOAD_FAIL;
      }
      return LOAD_OK;
    }

  case THREEOP:
    {
      Register op0, op1, op2;
      load_op(insn_info_table[oc].op0, op0);
      load_op(insn_info_table[oc].op1, op1);
      load_op(insn_info_table[oc].op2, op2);
      bytecodes[pc] = makecode_three_operands(oc, op0, op1, op2);
      return LOAD_OK;
    }

  case TWOOP:
    {
      Register op0, op1;
      op0 = atoi(next_token());
      op1 = atoi(next_token());
      bytecodes[pc] = makecode_two_operands(oc, op0, op1);
      return LOAD_OK;
    }

  case ONEOP:
    {
      Register op;
      op = atoi(next_token());
      bytecodes[pc] = makecode_one_operand(oc, op);
      return LOAD_OK;
    }

  case ZEROOP:
    {
      bytecodes[pc] = makecode_no_operand(oc);
      return LOAD_OK;
    }

  case UNCONDJUMP:
    {
      Displacement disp;
      disp = (Displacement)atoi(next_token());
      bytecodes[pc] = makecode_jump(oc, disp);
      return LOAD_OK;
    }

  case CONDJUMP:
    {
      Displacement disp;
      Register src;
      src = atoi(next_token());
      disp = (Displacement)atoi(next_token());
      bytecodes[pc] = makecode_cond_jump(oc, src, disp);
      return LOAD_OK;
    }

  case GETVAR:
    {
      Subscript link, offset;
      Register reg;
      link = atoi(next_token());
      offset = atoi(next_token());
      reg = atoi(next_token());
      bytecodes[pc] = makecode_getvar(oc, link, offset, reg);
      return LOAD_OK;
    }

  case SETVAR:
    {
      Subscript link, offset;
      Register reg;
      link = atoi(next_token());
      offset = atoi(next_token());
      reg = atoi(next_token());
      bytecodes[pc] = makecode_setvar(oc, link, offset, reg);
      return LOAD_OK;
    }

  case MAKECLOSUREOP:
    {
      Register dst;
      uint16_t index;
      dst = atoi(next_token());
      index = (uint16_t)atoi(next_token());
      bytecodes[pc] = makecode_makeclosure(oc, dst, index);
      return LOAD_OK;
    }

  case CALLOP:
    {
      Register closure;
      uint16_t argc;
      closure = atoi(next_token());
      argc = atoi(next_token());
      bytecodes[pc] = makecode_call(oc, closure, argc);
      return LOAD_OK;
    }

  default:
    {
      LOG_EXIT("Illegal instruction: %s\n", tokp);
      return LOAD_FAIL;
    }
  }
}
#endif /* USE_SBC */

#ifdef USE_OBC
int insn_load_obc(Context *ctx, ConstantCell *constant, Bytecode *bytecodes, int pc, CItable *citable) {
  unsigned char buf[sizeof(Bytecode)];
  Opcode oc;
  int index, i;

  if (fread(buf, sizeof(unsigned char), sizeof(Bytecode), file_pointer)
        != sizeof(Bytecode))
    LOG_ERR("Error: cannot read %dth bytecode", pc);
  oc = buf[0] * 256 + buf[1];

  switch (insn_info_table[oc].operand_type) {
    case BIGPRIMITIVE:
      switch (oc) {
        case ERROR:
        case STRING:
        case NUMBER:
#ifdef USE_REGEXP
#ifdef need_regexp
        case REGEXP:
#endif
#endif
          index = buf[4] * 256 + buf[5];
          add_constant_info(citable, oc, index, NONE);
          bytecodes[pc] = convertToBc(buf);
          return LOAD_OK;
        default:
          return LOAD_FAIL;
      }
      break;

    case THREEOP:
      for (i = 0; i < 3; i++) {
        InsnOperandType type = si_optype(oc, i);
        if (type == OPTYPE_ERROR) return LOAD_FAIL;
        if (type == STR || type == NUM ) {
          index = buf[i * 2 + 2] * 256 + buf[i * 2 + 3];
          add_constant_info(citable, oc, index, type);
        }
      }
      // fall through
    default:
      bytecodes[pc] = convertToBc(buf);
      return LOAD_OK;
  }
}
#endif

/*
   initilizes the contant table
 */ 
void init_constant_cell(ConstantCell *constant, int i) {
  JSValue* p;
  constant->n_constant_values = 0;
  p = (JSValue*)malloc(sizeof(JSValue) * (CONSTANT_LIMIT));
  if (p == NULL)
    LOG_EXIT("%dth func: cannot malloc constant_cell", i);
  constant->constant_values = p;
}

void init_constant_info(CItable *citable, int i) {
  ConstInfo* p;
  citable->n_const_info = 0;
  p = (ConstInfo*)malloc(sizeof(ConstInfo) * CONSTANT_LIMIT);
  if (p == NULL)
    LOG_EXIT("%dth func: cannot malloc constant_info", i);
  citable->const_info = p;
}

/*
   finlaizes the constant table
 */
void end_constant_cell(ConstantCell *constant) {
  // do nothing
}

/*
   converts a special JS string (for a constant) into a JSValue
 */
JSValue specstr_to_jsvalue(const char *str) {
  if (strcmp(str, "true") == 0)
    return JS_TRUE;
  else if (strcmp(str, "false") == 0)
    return JS_FALSE;
  else if (strcmp(str, "null") == 0)
    return JS_NULL;
  else if (strcmp(str, "undefined") == 0)
    return JS_UNDEFINED;
  else
    // undefined name
    LOG_EXIT("%s is an undefined symbol.", str);
}

char *jsvalue_to_specstr(JSValue v) {
  char *s;
  if (v == JS_TRUE) s = "true";
  else if (v == JS_FALSE) s = "false";
  else if (v == JS_NULL) s = "null";
  else if (v == JS_UNDEFINED) s = "undefined";
  else s = "unknown_specstr";
  return s;
}

#ifdef USE_OBC
void add_constant_info(CItable *ci, Opcode c, int index, InsnOperandType t) {
  if (index >= CONSTANT_LIMIT)
    LOG_ERR("Error: index %d is out of range of constant info table", index);
  (ci->const_info)[index].oc = c;
  (ci->const_info)[index].type = t;
  if (index + 1 > ci->n_const_info) ci->n_const_info = index + 1;
}
#endif

/*
   adds a number into the constant table.

   Currently, the same constant numbers might be added to the table
   more than once.  It might be better to avoid such duplication.
 */
int add_constant_number(Context *ctx, ConstantCell *constant, double x) {
  int index;

  index = constant->n_constant_values++;
  (constant->constant_values)[index] = double_to_number(x);
  return index;
}

/*
   adds a string into the constant table.
 */
int add_constant_string(Context *ctx, ConstantCell *constant, char *str) {
  JSValue a;
  int index;

  index = constant->n_constant_values++;
  a = cstr_to_string(NULL, str);
  // printf("updateConstantString: str = %s, a = %lld (%s)\n",
  //        str, a, stringToCStr(a));
  (constant->constant_values)[index] = a;
  return index;
}

#ifdef USE_REGEXP
#ifdef need_regexp
/*
   adds a regexp into the constant table.
 */
int add_constant_regexp(Context *ctx, ConstantCell *constant, char *pat, int flag) {
  JSValue re;
  int index;

  index = constant->n_constant_values++;
  // re = new_regexp();
  if ((re = new_normal_regexp(ctx, pat, flag)) != JS_UNDEFINED) {
    (constant->constant_values)[index] = re;
    return index;
  } else {
    LOG_ERR("an error occured in making regex.");
    return -1;
  }
}
#endif /* need_regexp */
#endif

int update_function_table(FunctionTable *ftable, int index,
                          ConstantCell *constant, Bytecode *bytecodes,
                          int callentry, int sendentry,
                          int nlocals, int ninsns) {
  int i;
  Opcode oc;
  Instruction *insns;
  void **insnptr;
  int bodysize;

#ifdef J5MODE
  if (index == 0) {
    ninsns++;
  }
#endif

  bodysize = ninsns + constant->n_constant_values;
  insns = (Instruction *)malloc(sizeof(Instruction) * bodysize);
  // The above shold be the following???
  // insns = (Instruction *)malloc(sizeof(Instruction) * ninsns +
  //                            sizeof(JSvalue) * constant->n_constant_values);
  insnptr = (void **)malloc(sizeof(void *) * ninsns);
  int loopnum = ninsns;

#ifdef J5MODE
  loopnum += (index == 0)? -1: 0;
#endif

  // rewrites the operand of STRING/NUMBER/ERROR/REGEXP instraction
  // as a relative displacement
  for (i = 0; i < loopnum; i++) {
    insnptr[i] = NULL;
    oc = get_opcode(bytecodes[i]);
    InsnOperandType op1, op2;
    op1 = insn_info_table[oc].op1;
    if (op1 == STR || op1 == NUM) {
      Subscript ss;
      Displacement disp;
      ss = (Subscript)get_second_operand_disp(bytecodes[i]);
      disp = calc_displacement(ninsns, i, ss);
      bytecodes[i] = update_second_operand_disp(bytecodes[i], disp);
    }
    op2 = insn_info_table[oc].op2;
    if (op2 == STR || op2 == NUM) {
      Subscript ss;
      Displacement disp;
      ss = (Subscript)get_third_operand_disp(bytecodes[i]);
      disp = calc_displacement(ninsns, i, ss);
      bytecodes[i] = update_third_operand_disp(bytecodes[i], disp);
    }
    if (oc == STRING || oc == NUMBER || oc == ERROR
#ifdef USE_REGEXP
#ifdef need_regexp
        || oc == REGEXP
#endif /* need_regexp */
#endif
        ) {
      Subscript ss;
      Displacement disp;
#ifdef USE_SBC
      ss = get_big_subscr(bytecodes[i]);
#endif
#ifdef USE_OBC
      ss = (bytecodes[i] & 0x00000000ffff0000)>>16;
#endif
      disp = calc_displacement(ninsns, i, ss);
      bytecodes[i] = update_displacement(bytecodes[i], disp);
    }
  }

  // fills the insns array
  for (i = 0; i < loopnum; i++) {
    insns[i].code = bytecodes[i];
#ifdef USE_THRESHOLD
    insns[i].hitCount = 0;
    insns[i].missCount = 0;
#endif
  }

  // writes the constants
  for (; i < bodysize; i++) {
    insns[i].code = constant->constant_values[i - ninsns];
  }

  ftable[index].insn_ptr_created = false;
  ftable[index].insns = insns;
  ftable[index].insn_ptr = insnptr;
  ftable[index].call_entry = callentry;
  ftable[index].send_entry = sendentry;
  ftable[index].n_locals = nlocals;
  ftable[index].body_size = bodysize;
  ftable[index].n_insns = ninsns;

  return 0;
}

int print_function_table(FunctionTable *ftable, int nfuncs) {
  int i, j;

  printf("number of functions = %d\n", nfuncs);
  for (i = 0; i < nfuncs; i++) {
    printf("function #%d\n", i);
    printf("call_entry: %d\n", ftable[i].call_entry);
    printf("send_entry: %d\n", ftable[i].send_entry);
    printf("n_locals: %d\n" ,ftable[i].n_locals);
    printf("n_insns: %d\n", ftable[i].n_insns);
    printf("body_size: %d\n", ftable[i].body_size);
    for (j = 0; j < ftable[i].n_insns; j++) {
      printf("%03d: %016"PRIx64" --- ", j, ftable[i].insns[j].code);
      print_bytecode(ftable[i].insns, j);
    }
    for (; j < ftable[i].body_size; j++) {
      JSValue o;
      o = ftable[i].insns[j].code;
      printf("%03d: %016"PRIx64" --- ", j, o);
      if (is_flonum(o))
        printf("FLONUM %lf\n", flonum_value(o));
      else if (is_string(o))
        printf("STRING \"%s\"\n", string_value(o));
#ifdef USE_REGEXP
#ifdef need_regexp
      else if (is_regexp(o))
        printf("REGEXP \"%s\"\n", regexp_pattern(o));
#endif /* need_regexp */
#endif
      else
        printf("Unexpected JSValue\n");
    }
  }
  return 0;
}

/*
   prints a bytecode instruction
 */
void print_bytecode(Instruction *insns, int j) {
  Bytecode code;
  Opcode oc;
  OperandType t;

  code = insns[j].code;
  oc = get_opcode(code);
  t = insn_info_table[oc].operand_type;
  printf("%s ", insn_info_table[oc].insn_name);
  switch (t) {
  case SMALLPRIMITIVE:
    {
      Register dst;
      int imm;
      dst = get_first_operand_reg(code);
      switch (oc) {
      case FIXNUM:
        imm = get_small_immediate(code);
        printf("%d %d", dst, imm);
        break;
      case SPECCONST:
        imm = get_small_immediate(code);
        printf("%d %s", dst, jsvalue_to_specstr(imm));
        break;
      default:
        printf("???");
        break;
      }
    }
    break;
  case BIGPRIMITIVE:
    {
      Register dst;
      Displacement disp;
      JSValue o;
      dst = get_first_operand_reg(code);
      disp = get_big_disp(code);
      o = (JSValue)(insns[j + disp].code);
      // printf("j = %d, disp = %d, o = %p\n", j, disp, (char *)o);
      switch (oc) {
      case NUMBER:
        if (is_flonum(o))
          printf("%d %f", dst, flonum_value(o));
        else
          printf("Object type mismatched: tag = %d", get_tag(o));
        break;
      case STRING:
      case ERROR:
        if (is_string(o))
          printf("%d \"%s\"", dst, string_value(o));
        else
          printf("Object type mismatched: tag = %d", get_tag(o));
        break;
#ifdef USE_REGEXP
#ifdef need_regexp
      case REGEXP:
        if (is_regexp(o))
          printf("%d %d \"%s\"", dst, regexp_flag(o), regexp_pattern(o));
        else
          printf("Object type mismatched: tag = %d", get_tag(o));
        break;
#endif /* need_regexp */
#endif // USE_REGEXP
      default:
        printf("???");
        break;
      }
    }
    break;
  case THREEOP:
    {
      Register dst, r1, r2;
      dst = get_first_operand_reg(code);
      r1 = get_second_operand_reg(code);
      r2 = get_third_operand_reg(code);
      printf("%d %d %d", dst, r1, r2);
    }
    break;
  case TWOOP:
    {
      Register dst, r1;
      dst = get_first_operand_reg(code);
      r1 = get_second_operand_reg(code);
      printf("%d %d", dst, r1);
    }
    break;
  case ONEOP:
    {
      Register dst;
      dst = get_first_operand_reg(code);
      printf("%d", dst);
    }
    break;
  case ZEROOP:
    break;
  case UNCONDJUMP:
  case TRYOP:
    {
      Displacement disp;
      disp = get_first_operand_disp(code);
      printf("%d", j + disp);
    }
    break;
  case CONDJUMP:          
    {
      Register r;
      Displacement disp;
      r = get_first_operand_reg(code);
      disp = get_second_operand_disp(code);
      printf("%d %d", r, j + disp);
    }
    break;
  case GETVAR:
    {
      Register dst;
      Subscript link, ss;
      link = get_first_operand_subscr(code);
      ss = get_second_operand_subscr(code);
      dst = get_third_operand_reg(code);
      printf("%d %d %d", link, ss, dst);
    }
    break;
  case SETVAR:
    {
      Register src;
      Subscript link, ss;
      link = get_first_operand_subscr(code);
      ss = get_second_operand_subscr(code);
      src = get_third_operand_reg(code);
      printf("%d %d %d", link, ss, src);
    }
    break;
  case MAKECLOSUREOP:
    {
      Register dst;
      Subscript ss;
      dst = get_first_operand_reg(code);
      ss = get_second_operand_subscr(code);
      printf("%d %d", dst, ss);
    }
    break;
  case CALLOP:
    {
      Register f;
      int na;
      f = get_first_operand_reg(code);
      na = get_second_operand_int(code);
      printf("%d %d", f, na);
    }
    break;
  case UNKNOWNOP:
    break;
  }
  putchar('\n');
}

uint32_t decode_escape_char(char *str) {
  char *src, *dst;
  int dq;
  char c;

  src = dst = str;
  dq = 0;
  if ((c = *src++) == '\"') {
    dq = 1;
    c = *src++;
  }
  while (1) {
    if (dq == 1 && c == '\"') break;
    if (dq == 0 && c == '\0') break;
    if (c != '\\') {
      *dst++ = c;
      c = *src++;
      continue;
    }
    switch (c = *src++) {
    case '0': *dst++ = '\0'; break;
    case 'a': *dst++ = '\a'; break;
    case 'b': *dst++ = '\b'; break;
    case 'f': *dst++ = '\f'; break;
    case 'n': *dst++ = '\n'; break;
    case 'r': *dst++ = '\r'; break;
    case 't': *dst++ = '\t'; break;
    case 'v': *dst++ = '\v'; break;
    case 's': *dst++ = ' '; break;
    case '\\': *dst++ = '\\'; break;
    case '\'': *dst++ = '\''; break;
    case '\"': *dst++ = '\"'; break;
    case 'x':
      {
        int k = 0, i;
        for (i = 0; i <= 1; i++) {
          c = *src++;
          if (c == ' ') c = '0';
          k <<= 4;
          if ('0' <= c && c <= '9') k += c - '0';
          else if ('a' <= c && c <= 'f') k += c + 10 - 'a';
          else if ('A' <= c && c <= 'F') k += c + 10 - 'A';
        }
        *dst++ = (char)k;
      }
      break;
    default: *dst++ = c; break;
    }
    c = *src++;
  }
  *dst = '\0';
  return (uint32_t)(dst - str);
}
