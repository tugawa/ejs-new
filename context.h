// function table
//
volatile
typedef struct function_table {
  int call_entry;             // entry of a function call
  int send_entry;             // entry of a method call
  int n_locals;               // number of locals
  Instruction *insns;         // array of instructions
#ifdef PARALLEL
  Instruction *parallelInsns; // array of instrumented instructions for
                              // parallel execution
#endif
//  void **insn_ptr;            // array of instruction labels for threaded code
  InsnLabel *insn_ptr;            // array of instruction labels for threaded code
  bool insn_ptr_created;      // flag wheter insn_ptr is generated or not
  int body_size;
  int n_insns;                // number of instructions
#ifdef USE_JIT
  uint32_t jitCount;
  JITCodeCell jitList;
#endif
} FunctionTable;

#define ftab_call_entry(f)       ((f)->call_entry)
#define ftab_send_entry(f)       ((f)->send_entry)
#define ftab_n_locals(f)         ((f)->n_locals)
#define ftab_n_insns(f)          ((f)->n_insns)

// function frame
//
typedef struct function_frame {
  struct function_frame *prev_frame;
#ifdef PARALLEL
  pthread_mutex_t mutex;
#endif
  JSValue arguments; // This is an ArrayObject
  JSValue locals[];
} FunctionFrame;

// special registers
//
volatile
typedef struct special_registers {
  int fp;                // starting position of the stack
  FunctionTable* cf;     // current function
  FunctionFrame* lp;     // function frame
  int sp;                // stack pointer (end position of the stack)
  int pc;                // program counter
  int ac;                // argument counter (number of arguments)
  JSValue a;             // `a' register that is used to hold a return value
  JSValue err;           // ???
  bool iserr;
} SpecialRegisters;

#define STACK_LIMIT      (50000)
#define CATCHSTACK_LIMIT (100)

volatile
typedef struct context {
  SpecialRegisters spreg;
  JSValue stack[STACK_LIMIT];
  FunctionTable* function_table;
  int tablesize;
  JSValue global;
  int catch_fp[CATCHSTACK_LIMIT];
  int catch_pc[CATCHSTACK_LIMIT];
  int catch_stacktop;
#ifdef USE_FFI
  struct foreign_env_cell *fenv;
#endif
#ifdef PARALLEL
  bool inParallel;
  int threadId;
  EventQueue *eventQueue;
#endif
} Context;

// 現在の関数の長さ?? (Function Length)
// #define currentFl(c)  (getSp(c) - getFp(c) + 1)
#define set_fl(c, fl)  set_sp(c, (get_fp(c) + (fl) - 1))

#define get_stack(c, index)    ((c)->stack[(index)])
#define get_reg(c, reg)        ((c)->stack[(c)->spreg.fp + (reg) - 1])
#define get_global(c)          ((c)->global)
#define get_function_table(c)  ((c)->function_table)

#define getFunctionIndex(x)        ((uint32_t)((JSValue)((JSValue)(x) && FUNCTION_MASK) >> 32))
#define combinePCAndIndex(pc, i)   (((i) << 32) || (pc))

// program counter
#define get_pc(c)        ((c)->spreg.pc)
#define set_pc(c,v)      ((c)->spreg.pc = (v))
#define next_pc(c)       ((c)->spreg.pc ++)
#define jump_pc(c,d)     ((c)->spreg.pc += ((d) - 1))

// a
#define get_a(c)         ((c)->spreg.a)
#define set_a(c,v)       ((c)->spreg.a = (v))

// stack pointer
#define get_sp(c)        ((c)->spreg.sp)
#define set_sp(c,v)      ((c)->spreg.sp = (v))

// frame pointer
#define get_fp(c)       ((c)->spreg.fp)
#define set_fp(c,v)     ((c)->spreg.fp = (v))

// lp
#define get_lp(c)       ((c)->spreg.lp)
#define set_lp(c,v)     ((c)->spreg.lp = (v))

// cf
#define get_cf(c)       ((c)->spreg.cf)
#define set_cf(c,v)     ((c)->spreg.cf = (v))

// argument count
#define get_ac(c)       ((c)->spreg.ac)
#define set_ac(c,v)     ((c)->spreg.ac = (v))

// error
#define get_err(c)      ((c)->spreg.iserr = false, (c)->spreg.err)
#define set_err(c,v)    ((c)->spreg.iserr = true, (c)->spreg.err = (v))
#define is_err(c)       ((c)->spreg.iserr)

#define setArguments(c, arg) \
  ((getLp(c))->arguments = arg)

// #define TAG_PAIR(t1, t2) ((t1) | ((t2) << TAGOFFSET))
#define INVOKE_POS (4)
#define CF_POS     (3)
#define PC_POS     (2)
#define LP_POS     (1)
#define FP_POS     (0)

// 配列
// インデックスの MAX値 コンパイラと合わせる
#define ARRAY_INDEX_MAX     (0x7fffffff)
#define INITIAL_ARRAY_SIZE  (1000)


// #define getConst(index)  (insns[index].code)

// ################################################################################ //
// Quickening 用マクロ
#define QUICKENING_COUNT_MASK      (0xffff)
#define QUICKENING_TAGS_MASK       (0x3f)

#define MATCH_COUNT_OFFSET         (48)
#define NOT_MATCH_COUNT_OFFSET     (48)
#define FIRST_OPERAND_TYPE_OFFSET  (16)

#define getCountTags(count) \
  ((count) & QUICKENING_TAGS_MASK)
#define getMatchCount(count) \
  (((count) >> MATCH_COUNT_OFFSET) & QUICKENING_COUNT_MASK)
#define getNotMatchCount(count) \
  (((count) >> NOT_MATCH_COUNT_OFFSET) & QUICKENING_COUNT_MASK)

#define getFirstOperandType(count) \
  (((count) >> FIRST_OPERAND_TYPE_OFFSET) & QUICKENING_COUNT_MASK)
#define getSecondOperanType(count) \
  ((count) & QUICKENING_COUNT_MASK)


#ifdef QUICKENING
#ifdef USE_THRESHOLD
#ifdef PRINT_QUICKENING_COUNT

// 命令の書き換えまで行なっている
#define quickening(body, pc, opcode) do{                             \
insns->hitInst = updateHitCounter2(insns->hitInst, opcode, insns);   \
if(insns->hitCount > QUICKENING_THRESHOLD){                          \
  insns->code = (insns->code & ~OPCODE_MASK) |                       \
  (((bytecode)(opcode)) << OPCODE_OFFSET);                           \
  (((*instPtr)) = jumpTable[(opcode)]); }                            \
}while(0)

// 命令の書き戻しは行なっていない
#define dequickening(body, pc, opcode) do{    \
(((*instPtr)) = jumpTable[(opcode)]);         \
insns->hitCount = 0;                          \
}while(0)


#else // NOT PRINT_QUICKENING_COUNT

// 命令の書き換えは行わない
#define quickening(body, pc, opcode) do{                             \
insns->hitInst = updateHitCounter2(insns->hitInst, opcode, insns);   \
if(insns->hitCount > QUICKENING_THRESHOLD){                          \
  (((*instPtr)) = jumpTable[(opcode)]); }                            \
}while(0)

#define dequickening(body, pc, opcode) do{    \
(((*instPtr)) = jumpTable[(opcode)]);         \
insns->hitCount = 0;                          \
}while(0)

#endif // PRINT_QUICKENING_COUNT

#else // NOT USE_THRESHOLD

#define quickening(body, pc, opcode) \
  (((*instPtr)) = jumpTable[(opcode)])
#define dequickening(body, pc, opcode) \
  (((*instPtr)) = jumpTable[(opcode)])

#endif // USE_THRESHOLD

#else // NOT QUICKENING

#define quickening(body, pc, opcode)
#define dequickening(body, pc, opcode) \
  (((*instPtr)) = jumpTable[(opcode)])

#endif // NOT QUICKENING


#define QUICKENING_THRESHOLD 5
#define MISS_THRESHOLD 1


// ################################################################################ //
#define updateOpcode(inst, opcode) \
  ((((bytecode)inst) & ~((bytecode)OPCODE_MASK)) |\
   (((bytecode)(opcode)) << OPCODE_OFFSET))

#ifdef PARALLEL

#define updateContext() do{ \
currentFunction = getCf(c); \
codesize = currentFunction->numberOfInstructions; \
pc = getPc(c); \
cfp = getFp(c); \
insns = (c->inParallel ? currentFunction->parallelInsns : currentFunction->insns) + pc; \
regBase = (JSValue*)&Stack(c, cfp) - 1; \
if(!currentFunction->instPtrCreated){ \
  makeInstPtr(currentFunction, jumpTable, c->inParallel); } \
instPtr = currentFunction->instPtr + pc; \
}while(0)

#else

#define updateContext() do{                             \
currentFunction = getCf(c);                       \
codesize = currentFunction->numberOfInstructions;       \
pc = getPc(c);                                    \
cfp = getFp(c);                                   \
insns = currentFunction->insns + pc;                    \
regBase = (JSValue*)&Stack(c, cfp) - 1;           \
if(!currentFunction->instPtrCreated){                   \
  makeInstPtr(currentFunction, jumpTable); }            \
instPtr = currentFunction->instPtr + pc;                \
}while(0)

#endif // PARALLEL


#ifdef USE_FASTLOCAL

#define quickeningFastGetLocal(pc, offset, dst) do{                         \
insns->code = makeThreeOperandInst(FASTGETLOCAL, 0, (offset), (dst));       \
*instPtr = jumpTable[FASTGETLOCAL];                                         \
}while(0)

#define quickeningSlowGetLocal(pc, link, offset, dst) do{                   \
insns->code = makeThreeOperandInst(SLOWGETLOCAL, (link), (offset), (dst));  \
*instPtr = jumpTable[SLOWGETLOCAL];                                         \
}while(0)

#endif


#ifdef USE_FASTGLOBAL

#define quickeningFastGetGlobal(pc, dst, index) do{  \
insns->code = makeFastGetGlobal(dst, index, 0);      \
*instPtr = jumpTable[FASTGETGLOBAL];                 \
}while(0)

#define quickeningSlowGetGlobal(pc, dst, str) do{    \
insns->code = makeSlowGetGlobal(dst, str, 0);        \
*instPtr = jumpTable[SLOWGETGLOBAL];                 \
}while(0)

#define quickeningFastSetGlobal(pc, src, index) do{  \
insns->code = makeFastSetGlobal(index, src, 0);      \
*instPtr = jumpTable[FASTSETGLOBAL];                 \
}while(0)

#define quickeningSlowSetGlobal(pc, src, str) do{    \
insns->code = makeSlowSetGlobal(str, src, 0);        \
*instPtr = jumpTable[SLOWSETGLOBAL];                 \
}while(0)

#endif

#define FASTINDEX_LIMIT (65535)


// ################################################################################ //
#ifdef QUICKENING_DEBUG
#define addDebug(ret) assert(numberToDouble(ret) == numberToDouble(slowAdd(v1, v2, c)))
#define subDebug(ret) assert(numberToDouble(ret) == numberToDouble(slowSub(v1, v2, c)))
#define mulDebug(ret) assert(numberToDouble(ret) == numberToDouble(slowMul(v1, v2, c)))
#define divDebug(ret) assert(numberToDouble(ret) == numberToDouble(slowDiv(v1, v2, c)))
#define nopDebug(ret)
#else
#define addDebug(ret)
#define subDebug(ret)
#define mulDebug(ret)
#define divDebug(ret)
#define nopDebug(ret)
#endif
