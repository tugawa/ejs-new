/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN
#include "header.h"

/*
 * calls a function
 *
 * When this function is called, the stack is:
 *
 *         ...
 * pos:    place where CF is saved
 *         place where PC is saved
 *         place where LP is saved
 *         place where FP is saved
 *         receiver                        <-- this place is new fp
 *         arg1
 *         arg2
 *         ...
 *  sp:     argN
 */
void call_function(Context *context, JSValue fn, int nargs, int sendp) {
  FunctionTable *t;
  JSValue *stack;
  int sp, pos;

  sp = get_sp(context);
  stack = &get_stack(context, 0);

  /*
   * saves special registers into the stack
   */
  pos = sp - nargs - 4;
  save_special_registers(context, stack, pos);

  /*
   * sets special registers
   */
  set_fp(context, sp - nargs);
  set_ac(context, nargs);
  set_lp(context, func_environment(fn));
  t = func_table_entry(fn);
  set_cf(context, t);
  if (sendp == TRUE)
    set_pc(context, ftab_send_entry(t));
  else
    set_pc(context, ftab_call_entry(t));
}

/*
 * call a function at the tail position
 */
void tailcall_function(Context *context, JSValue fn, int nargs, int sendp) {
  FunctionTable *t;
  int fp;

  fp = get_fp(context);
  set_sp(context, fp + nargs);
  set_ac(context, nargs);
  set_lp(context, func_environment(fn));
  t = func_table_entry(fn);
  set_cf(context, t);
  if (sendp == TRUE)
    set_pc(context, ftab_call_entry(t));
  else
    set_pc(context, ftab_send_entry(t));
}

/*
 * calls a builtin function
 *
 * When this function is called, the stack is:
 *
 *         ...
 * pos:    place where CF is saved
 *         place where PC is saved
 *         place where LP is saved
 *         place where FP is saved
 *         receiver                        <-- this place is new fp
 *         arg1
 *         arg2
 *         ...
 * sp:     argN
 */
void call_builtin(Context *context, JSValue fn, int nargs, int sendp,
                  int constrp) {
  builtin_function_t body;
  JSValue *stack;
  int na;
  int sp, fp;

  body = (constrp == TRUE)? builtin_constructor(fn): builtin_body(fn);
  na = builtin_n_args(fn);

  sp = get_sp(context);
  fp = get_fp(context);
  stack = &get_stack(context, 0);

  /*
   * printf("call_builtin: sp = %d, fp = %d, stack = %p, nargs = %d, sendp = %d, constrp = %d\n",
   *  sp, fp, stack, nargs, sendp, constrp);
   */

  /*
   * The original code called save_special_registers here, but this seems
   * to be unnecessary because builtin function code does not manipulate
   * special registers.  However, since the compiler takes rooms from
   * stack[pos] to stack[pos + 3] for saving the values of special registers,
   * it may be necessary to fill these rooms with harmless values, e.g.,
   * FIXNUM_ZERO to make the GC work correctly.
   * 2017/03/15:  It seems to be unnecessary bacause setfl instruction
   * stores JS_UNDEFINEDs into the stack area.
   *
   *  pos = sp - nargs - 4;
   *  save_special_registers(context, stack, pos);
   */

  /*
   * sets the value of the receiver to the global object if it is not set yet
   */
  if (sendp == FALSE)
    stack[sp - nargs] = context->global;

  while (nargs < na) {
    stack[++sp] = JS_UNDEFINED;
    nargs++;
  }

  set_sp(context, sp);
  (*body)(context, sp - nargs, nargs);    /* real-n-args? */
}

/*
 * calls a builtin function at a tail position
 */
void tailcall_builtin(Context *context, JSValue fn, int nargs, int sendp,
                      int constrp) {
  builtin_function_t body;
  JSValue *stack;
  int na;
  int fp;

  body = (constrp == TRUE)? builtin_constructor(fn): builtin_body(fn);
  na = builtin_n_args(fn);

  fp = get_fp(context);
  stack = &get_stack(context, 0);

  /*
   * sets the value of the receiver to the global object if it is not set yet
   */
  if (sendp == FALSE)
    stack[0] = context->global;

  while (nargs < na)
    stack[++nargs + fp] = JS_UNDEFINED;

  set_sp(context, fp + nargs);
  (*body)(context, fp, nargs);    /* real-n-args? */
  stack = &get_stack(context, 0); /* stack may be moved by GC */
  restore_special_registers(context, stack, fp - 4);
}

/*
 * Invokes a function fn with arguments args in a new vmloop.
 * `as' is guaranteed to be an array.
 */
JSValue invoke_function(Context *context, JSValue receiver, JSValue fn,
                        int sendp, JSValue as, int nargs) {
  FunctionTable *t;
  JSValue *stack, ret;
  int sp, newfp, pos, oldfp, oldsp, i;

  /* printf("invoke_function: nargs = %d\n", nargs); */
  stack = &get_stack(context, 0);
  oldsp = sp = get_sp(context);
  oldfp = get_fp(context);
  pos = sp + 1;           /* place where cf register will be saved */
  sp += 5;                /* makes room for cf, pc, lp, fp, and receiver */
  stack[sp] = receiver;   /* stores the receiver */
  newfp = sp;             /* place where the receiver is stored */
  for (i = 0; i < nargs; i++)   /* copies the actual arguments */
    stack[++sp] = array_body_index(as, i);
  save_special_registers(context, stack, pos);

  /*
   * sets special registers
   */
  set_fp(context, newfp);
  set_sp(context, sp);
  set_ac(context, nargs);
  set_lp(context, func_environment(fn));
  t = func_table_entry(fn);
  set_cf(context, t);
  if (sendp == TRUE)
    set_pc(context, ftab_send_entry(t));
  else
    set_pc(context, ftab_call_entry(t));
  vmrun_threaded(context, newfp);
  ret = get_a(context);
  restore_special_registers(context, stack, pos);
  set_fp(context, oldfp);
  set_sp(context, oldsp);
  return ret;
}

/*
 * invokes a builtin function
 */
JSValue invoke_builtin(Context *context, JSValue receiver, JSValue fn,
                       int sendp, JSValue as, int nargs) {
  int oldsp, sp, i;
  JSValue *stack;

  oldsp = sp = get_sp(context);
  stack = &get_stack(context, 0);
  stack[++sp] = receiver;       /* set receiver */
  for (i = 0; i < nargs; i++)   /* copies the actual arguments */
    stack[++sp] = array_body_index(as, i);
  set_sp(context, sp);
  call_builtin(context, fn, nargs, sendp, FALSE);
  set_sp(context, oldsp);
  return get_a(context);
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
