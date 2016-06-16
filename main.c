//
//  main.c
//  SSJSVM Project, iwasaki-lab, UEC,
//
//  Sho Takada, 2012-13
//  Akira Tanimura, 2012-13
//  Akihiro Urushihara, 2013-14
//  Ryota Fujii, 2013-14
//  Hideya Iwasaki, 2013-16
//

#include "prefix.h"
#define EXTERN
#include "header.h"

#ifdef CALC_CALL
static uint64_t callcount = 0;
#endif

// test
//
#define pp(v) (print_value_verbose(cxt, (v)), putchar('\n'))
void testtest(Context *cxt) {
  JSValue v;
  v = JS_UNDEFINED; pp(v);
  v = JS_NULL; pp(v);
  v = JS_TRUE; pp(v);
  v = JS_FALSE; pp(v);
  v = cint_to_fixnum((cint)100); pp(v);
  v = double_to_flonum((double)3.1415); pp(v);

  v = special_to_string(JS_UNDEFINED); printf("undefined -> string: "); pp(v);
  v = special_to_string(JS_NULL); printf("null -> string: "); pp(v);
  v = special_to_string(JS_TRUE); printf("true -> string: "); pp(v);
  v = special_to_string(JS_FALSE); printf("false -> string: "); pp(v);
  v = fixnum_to_string(FIXNUM_ZERO); printf("0 -> string: "); pp(v);
  v = fixnum_to_string(FIXNUM_ONE); printf("1 -> string: "); pp(v);
  v = flonum_to_string(gobj.g_flonum_nan); printf("NaN -> string: "); pp(v);
  v = flonum_to_string(gobj.g_flonum_infinity); printf("Infinity -> string: "); pp(v);
  v = flonum_to_string(gobj.g_flonum_negative_infinity); printf("-Infinity -> string: "); pp(v);

  v = special_to_number(JS_UNDEFINED); printf("undefined -> number: "); pp(v);
  v = special_to_number(JS_NULL); printf("null -> number: "); pp(v);
  v = special_to_number(JS_TRUE); printf("true -> number: "); pp(v);
  v = special_to_number(JS_FALSE); printf("false -> number: "); pp(v);
  v = string_to_number(cstr_to_string("")); printf("\"\" -> number: "); pp(v);
  v = string_to_number(cstr_to_string("1.2")); printf("\"1.2\" -> number: "); pp(v);
  v = string_to_number(cstr_to_string("one")); printf("\"one\" -> number: "); pp(v);

  v = special_to_boolean(JS_UNDEFINED); printf("undefined -> boolean: "); pp(v);
  v = special_to_boolean(JS_NULL); printf("null -> boolean: "); pp(v);
  v = string_to_boolean(cstr_to_boolean("")); printf("\"\" -> boolean: "); pp(v);
  v = string_to_boolean(cstr_to_boolean("1.2")); printf("\"1.2\" -> boolean: "); pp(v);
  v = string_to_boolean(cstr_to_boolean("one")); printf("\"one\" -> boolean: "); pp(v);
}

// main
//
int main(int argc, char* argv[]) {

  // display arguments
  /*
  int i=0;
  LOG("%d\n",argc);
  for (i=0; i<argc; i++) {
    LOG("%s\n",argv[i]);
  }
  */

  // If input program is given from a file, fp is set to NULL.
  FILE *fp = NULL;

#ifdef J5MODE
  gArgc = argc;
  gArgv = argv;
#else
  if (argc == 2) {
    fp = fopen(argv[1], "r");
    if (fp == NULL) {
      LOG_EXIT("No such file.");
    }
  }
#endif // J5MODE

#ifdef CALC_CALL
  callcount = 0;
#endif // CALC_CALL

#ifdef CALC_TIME
  long long s, e;
#endif // CALC_TIME

#ifdef USE_PAPI

#ifdef CALC_MSP
  int events[] = {PAPI_BR_MSP};
#elif defined CALC_ICM
  int events[] = {PAPI_L1_ICM, PAPI_L2_ICM};
#elif defined CALC_TCM
  int events[] = {PAPI_L1_TCM, PAPI_L2_TCM};
#else
  int events[] = {};
#endif

  int eventsize = sizeof(events)/sizeof(int);
  long long *values = malloc(sizeof(long long) * eventsize);
#endif // USE_PAPI

  int n;
  Context *context;

#ifdef USE_BOHEMGC
  GC_INIT();
#endif // USE_BOHEMGC

  init_string_table(STRING_TABLE_SIZE);
  init_code_loader(fp);
  n = code_loader(function_table);
  end_code_loader();

#ifdef PARALLEL
  // generates bytecode for parallel execution
  generateParallelCode(function_table, n);
  //printFuncTbl(functionTable, n);
#endif // PARALLEL

  init_global_constants();
  init_context(function_table, init_global(), &context);
  // LOG("fp:%d\n", getFp(context));
  // LOG("sp:%d\n", getSp(context));

#ifdef USE_JIT
  initJITCompiler();
#endif // USE_JIT

  // obtains the time before execution
#ifdef USE_PAPI
  if (eventsize > 0) {
    int papi_result = PAPI_start_counters(events, eventsize);
    if (papi_result != 0)
      LOG_EXIT("papi failed:%d\n", papi_result);
  }
#endif // USE_PAPI

#ifdef CALC_TIME
  s = PAPI_get_real_usec();
#endif // CALC_TIME

  testtest(context);

  // enters the VM loop
  vmrun_threaded(context, 0);

  // obtains the time after execution
#ifdef CALC_TIME
  e = PAPI_get_real_usec();
#endif // CALC_TIME

#ifdef USE_PAPI
  if (eventsize > 0)
    PAPI_stop_counters(values, eventsize);
#endif // USE_PAPI

#ifndef USE_PAPI
#ifndef CALC_TIME
#ifndef CALC_CALL

#ifdef LASTEXPR_PRINT
// outputs the results of the last expression
#ifdef USE_FFI
  if(isErr(context)){
    printf("Exception!\n");
    printJSValue(getErr(context));
  }else{
    debugPrint(context, n);
  }
#else
  debugPrint(context, n);
#endif  // USE_FFI
#endif  // LASTEXPR_PRINT

#endif // CALC_CALL
#endif // CALC_TIME
#endif // USE_PAPI

#ifdef USE_PAPI
  if (eventsize > 0) {
    int i;
    for (i = 0; i < eventsize; i++)
      LOG("%"PRId64"\n", values[i]);
    LOG("%15.15e\n", ((double)values[1]) / (double)values[0]);
    LOG("L1 Hit Rate:%lf\n", ((double)values[0])/((double)values[0] + values[1]));
    LOG("L2 Hit Rate:%lf\n", ((double)values[2])/((double)values[2] + values[3]));
    LOG("L3 Hit Rate:%lf\n", ((double)values[4])/((double)values[4] + values[5]));
  }
#endif // USE_PAPI

#ifdef CALC_TIME
  LOG("%"PRId64"\n", e - s);
#endif // CALC_TIME

#ifdef CALC_CALL
  LOG("%"PRId64"\n", callcount);
#endif // CALC_CALL

  return 0;
}

// prints a JSValue
//
void print_value_simple(Context *context, JSValue v) {
  print_value(context, v, 0);
}

void print_value_verbose(Context *context, JSValue v) {
  print_value(context, v, 1);
}

void print_value(Context *context, JSValue v, int verbose) {
  if (verbose)
    printf("%016lx, tag = %lu: ", v, get_tag(v));
  switch (get_tag(v)) {
  case T_OBJECT:
    v = object_to_string(context, v);
    break;
  case T_STRING:
    break;
  case T_FIXNUM:
    v = fixnum_to_string(v);
    break;
  case T_FLONUM:
    v = flonum_to_string(v);
    break;
  case T_SPECIAL:
    v = special_to_string(v);
    break;
  default:
    LOG_ERR("Type Error");
    break;
  }
  printf("%s", string_to_cstr(v));
}
