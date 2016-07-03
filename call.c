#include "prefix.h"
#define EXTERN
#include "header.h"

#define save_special_registers(c, st, pos) \
  (st[pos] = (JSValue)(get_cf(c)), \
   st[(pos) + 1] = (JSValue)(get_pc(c)), \
   st[(pos) + 2] = (JSValue)(get_lp(c)), \
   st[(pos) + 3] = (JSValue)(get_fp(c)))


#define restore_special_registers(c, st, pos) \
  (set_cf(c, (FunctionTable *)(st[pos])), \
   set_pc(c, st[(pos) + 1]), \
   set_lp(c, (FunctionFrame *)(st[(pos) + 2])), \
   set_fp(c, st[(pos) + 3]))

// calls a function
//
/*
   When this function is called, the stack is:

           ...
   pos:    place where CF is saved
           place where PC is saved
           place where LP is saved
           place where FP is saved
           receiver                        <-- this place is new fp
           arg1
           arg2
           ...
   sp:     argN
*/

void call_function(Context *context, JSValue fn, int nargs, int sendp) {
  FunctionCell *f;
  FunctionTable *t;
  JSValue *stack;
  int sp, fp;
  int pos;

  f = remove_function_tag(fn);
  sp = get_sp(context);
  fp = get_fp(context);
  stack = &get_stack(context, 0);

  // saves special registers into the stack
  pos = sp - nargs - 4;
  save_special_registers(context, stack, pos);

  // sets special registers
  set_fp(context, sp - nargs);
  set_ac(context, nargs);
  set_lp(context, func_environment(f));
  t = func_table_entry(f);
  set_cf(context, t);
  if (sendp == TRUE)
    set_pc(context, ftab_call_entry(t));
  else
    set_pc(context, ftab_send_entry(t));
}

// call a function at the tail position

void tailcall_function(Context *context, JSValue fn, int nargs, int sendp) {
  FunctionCell *f;
  FunctionTable *t;
  int fp;

  f = remove_function_tag(fn);
  fp = get_fp(context);
  set_sp(context, fp + nargs);
  set_ac(context, nargs);
  set_lp(context, func_environment(f));
  t = func_table_entry(f);
  set_cf(context, t);
  if (sendp == TRUE)
    set_pc(context, ftab_call_entry(t));
  else
    set_pc(context, ftab_send_entry(t));
}

// calls a builtin function
//
/*
   When this function is called, the stack is:

           ...
   pos:    place where CF is saved
           place where PC is saved
           place where LP is saved
           place where FP is saved
           receiver                        <-- this place is new fp
           arg1
           arg2
           ...
   sp:     argN
*/

void call_builtin(Context *context, JSValue fn, int nargs, int sendp, int constrp) {
  BuiltinCell *b;
  builtin_function_t body;
  JSValue *stack;
  int na;
  int sp, fp;
  int pos;

  b = remove_builtin_tag(fn);
  body = (constrp == TRUE)? builtin_constructor(fn): builtin_body(fn);
  na = builtin_n_args(b);

  sp = get_sp(context);
  fp = get_fp(context);
  stack = &get_stack(context, 0);

  // saves special registers into the stack
  pos = sp - nargs - 4;
  save_special_registers(context, stack, pos);

  // sets the value of the receiver to the global object if it is not set yet
  if (sendp == FALSE)
    stack[sp - nargs] = context->global;

  while (nargs < na) {
    stack[++sp] = JS_UNDEFINED;
    nargs++;
  }

  /*
  {
    int i;
    for (i = 1; i <= nargs + 1; i++)
      printf("i = %d, addr = %p, %016lx\n", i, &stack[sp - nargs - 1 + i], stack[sp - nargs - 1 + i]);
  }
  */

  // sets special registers
  set_fp(context, sp - nargs);
  set_lp(context, NULL);    // it seems that these three lines are unnecessary
  set_pc(context, -1);
  set_cf(context, NULL);

  set_ac(context, nargs);
  (*body)(context, nargs);    // real-n-args?
  restore_special_registers(context, stack, pos);
}

// calls a builtin function at a tail position

void tailcall_builtin(Context *context, JSValue fn, int nargs, int sendp, int constrp) {
  BuiltinCell *b;
  builtin_function_t body;
  JSValue *stack;
  int na;
  int fp;

  b = remove_builtin_tag(fn);
  body = (constrp == TRUE)? builtin_constructor(fn): builtin_body(fn);
  na = builtin_n_args(b);

  fp = get_fp(context);
  stack = &get_stack(context, 0);

  // sets the value of the receiver to the global object if it is not set yet
  if (sendp == FALSE)
    stack[0] = context->global;

  while (nargs < na)
    stack[++nargs + fp] = JS_UNDEFINED;

  // sets special registers
  set_sp(context, fp + nargs);
  set_lp(context, NULL);    // it seems that these three lines are unnecessary
  set_pc(context, -1);
  set_cf(context, NULL);
  set_ac(context, nargs);
  (*body)(context, nargs);    // real-n-args?
  restore_special_registers(context, stack, fp - 4);
}
