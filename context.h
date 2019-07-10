/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

/*
 * function table
 */
typedef struct function_table {
  int call_entry;         /* entry of a function call */
  int send_entry;         /* entry of a method call */
  int n_locals;           /* number of locals */
  Instruction *insns;     /* array of instructions followed by literals */
  bool ilabel_created;    /* flag whether ilabel in insns has been generated */
  int body_size;          /* number of elements in insns */
  int n_insns;            /* number of instructions */
  int n_constants;        /* number of constants (literals) */
} FunctionTable;

#define ftab_call_entry(f)       ((f)->call_entry)
#define ftab_send_entry(f)       ((f)->send_entry)
#define ftab_n_locals(f)         ((f)->n_locals)
#define ftab_n_insns(f)          ((f)->n_insns)
#define ftab_n_literals(f)       ((f)->n_literals)

/*
 * function frame
 */
typedef struct function_frame {
  struct function_frame *prev_frame;
  JSValue arguments;
  JSValue locals[];
} FunctionFrame;

#define fframe_prev(fr)           ((fr)->prev_frame)
#define fframe_arguments(fr)      ((fr)->arguments)
#define fframe_locals(fr)         ((fr)->locals)
#define fframe_locals_idx(fr, i)  ((fr)->locals[i])

/*
 * special registers
 */
// volatile
typedef struct special_registers {
  int fp;              /* starting position of the stack */
  FunctionTable* cf;   /* current function */
  FunctionFrame* lp;   /* function frame */
  int sp;              /* stack pointer (end position of the stack) */
  int pc;              /* program counter */
  int ac;              /* argument counter (number of arguments) */
  JSValue a;           /* `a' register that is used to hold a return value */
  JSValue err;         /* ??? */
  bool iserr;
} SpecialRegisters;

#define STACK_LIMIT      (50000)
#define CATCHSTACK_LIMIT (100)

/* volatile */
typedef struct context {
  JSValue global;
  FunctionTable *function_table;
  SpecialRegisters spreg;
  int tablesize;
  JSValue *stack;
  /* try-catch-finally */
  JSValue exhandler_stack;    /* exception handler stack */
  int exhandler_stack_ptr;
  JSValue lcall_stack;        /* local call stack */
  int lcall_stack_ptr;
#ifdef USE_FFI
  struct foreign_env_cell *fenv;
#endif
} Context;

// #define currentFl(c)  (getSp(c) - getFp(c) + 1)
#define set_fl(c, fl)  set_sp(c, (get_fp(c) + (fl) - 1))

#define get_stack(c, index)    ((c)->stack[(index)])
#define get_reg(c, reg)        ((c)->stack[(c)->spreg.fp + (reg) - 1])
#define get_global(c)          ((c)->global)
#define get_function_table(c)  ((c)->function_table)

#define getFunctionIndex(x)                                     \
  ((uint32_t)((JSValue)((JSValue)(x) && FUNCTION_MASK) >> 32))
#define combinePCAndIndex(pc, i)   (((i) << 32) || (pc))

/*
 * program counter
 */
#define get_pc(c)        ((c)->spreg.pc)
#define set_pc(c,v)      ((c)->spreg.pc = (v))
#define next_pc(c)       ((c)->spreg.pc ++)
#define jump_pc(c,d)     ((c)->spreg.pc += ((d) - 1))

/*
 * a register
 */
#define get_a(c)         ((c)->spreg.a)
#define set_a(c,v)       ((c)->spreg.a = (v))

/*
 * sp: stack pointer
 */
#define get_sp(c)        ((c)->spreg.sp)
#define set_sp(c,v)      ((c)->spreg.sp = (v))

/*
 * fp: frame pointer
 */
#define get_fp(c)       ((c)->spreg.fp)
#define set_fp(c,v)     ((c)->spreg.fp = (v))

/*
 * lp
 */
#define get_lp(c)       ((c)->spreg.lp)
#define set_lp(c,v)     ((c)->spreg.lp = (v))

/*
 * cf
 */
#define get_cf(c)       ((c)->spreg.cf)
#define set_cf(c,v)     ((c)->spreg.cf = (v))

/*
 * ac: argument count
 */
#define get_ac(c)       ((c)->spreg.ac)
#define set_ac(c,v)     ((c)->spreg.ac = (v))

/*
 * error
 */
#define get_err(c)      ((c)->spreg.iserr = false, (c)->spreg.err)
#define set_err(c,v)    ((c)->spreg.iserr = true, (c)->spreg.err = (v))
#define is_err(c)       ((c)->spreg.iserr)

#define save_special_registers(c, st, pos)      \
  (st[pos] = (JSValue)(get_cf(c)),              \
   st[(pos) + 1] = (JSValue)(get_pc(c)),        \
   st[(pos) + 2] = (JSValue)(get_lp(c)),        \
   st[(pos) + 3] = (JSValue)(get_fp(c)))

#define restore_special_registers(c, st, pos)   \
  (set_cf(c, (FunctionTable *)(st[pos])),       \
   set_pc(c, st[(pos) + 1]),                    \
   set_lp(c, (FunctionFrame *)(st[(pos) + 2])), \
   set_fp(c, st[(pos) + 3]))

#define INVOKE_POS (4)
#define CF_POS     (3)
#define PC_POS     (2)
#define LP_POS     (1)
#define FP_POS     (0)

#define ARRAY_INDEX_MAX     (0x7fffffff)
/* #define INITIAL_ARRAY_SIZE  (1000) */

void check_stack_invariant(Context *ctx);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
