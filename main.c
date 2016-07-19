/*
   main.c

   SSJS Project at the University of Electro-communications

   Sho Takada, 2012-13
   Akira Tanimura, 2012-13
   Akihiro Urushihara, 2013-14
   Ryota Fujii, 2013-14
   Tomoharu Ugawa, 2013-16
   Hideya Iwasaki, 2013-16
*/

#include "prefix.h"
#define EXTERN
#include "header.h"

#ifdef CALC_CALL
static uint64_t callcount = 0;
#endif

#define pp(v) (print_value_verbose(cxt, (v)), putchar('\n'))

/*
   Debug function
 */
void testtest(Context *cxt) {
#if 0
  JSValue v, p;
  printf("Testtest: cxt->global = %016lx, gconsts.g_global = %016lx\n", cxt->global, gconsts.g_global);
  printf("Testtest: g_object = %016lx, g_object_proto = %016lx\n", gconsts.g_object, gconsts.g_object_proto);

  if (get_prop(cxt->global, cstr_to_string("Object"), &v) == SUCCESS)
    printf("Testtest: global[Object] = %016lx\n", v);
  else
    printf("Testtest: global[Object] = not found\n");

  if (get_prop(v, cstr_to_string("prototype"), &p) == SUCCESS)
    printf("Testtest: Object[prototype] = %016lx\n", p);
  else
    printf("Testtest: Object[prototype] = not found\n");

  v = new_object();
  set_obj_cstr_prop(v, "foo", cint_to_fixnum(9999), ATTR_DE);
  set_obj_cstr_prop(gconsts.g_global, "soko", v, ATTR_DE);
  set_obj_cstr_prop(gconsts.g_global, "goyo", cint_to_fixnum(8888), ATTR_DE);
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
  v = flonum_to_string(gconsts.g_flonum_nan); printf("NaN -> string: "); pp(v);
  v = flonum_to_string(gconsts.g_flonum_infinity); printf("Infinity -> string: "); pp(v);
  v = flonum_to_string(gconsts.g_flonum_negative_infinity); printf("-Infinity -> string: "); pp(v);

  v = special_to_number(JS_UNDEFINED); printf("undefined -> number: "); pp(v);
  v = special_to_number(JS_NULL); printf("null -> number: "); pp(v);
  v = special_to_number(JS_TRUE); printf("true -> number: "); pp(v);
  v = special_to_number(JS_FALSE); printf("false -> number: "); pp(v);
  v = string_to_number(cstr_to_string("")); printf("\"\" -> number: "); pp(v);
  v = string_to_number(cstr_to_string("1.2")); printf("\"1.2\" -> number: "); pp(v);
  v = string_to_number(cstr_to_string("one")); printf("\"one\" -> number: "); pp(v);

  v = special_to_boolean(JS_UNDEFINED); printf("undefined -> boolean: "); pp(v);
  v = special_to_boolean(JS_NULL); printf("null -> boolean: "); pp(v);
  // v = string_to_boolean(cstr_to_boolean("")); printf("\"\" -> boolean: "); pp(v);
  // v = string_to_boolean(cstr_to_boolean("1.2")); printf("\"1.2\" -> boolean: "); pp(v);
  // v = string_to_boolean(cstr_to_boolean("one")); printf("\"one\" -> boolean: "); pp(v);
#endif
}

/*
   main function
 */
int main(int argc, char* argv[]) {
  // If input program is given from a file, fp is set to NULL.
  FILE *fp = NULL;

#ifdef J5MODE
  gArgc = argc;
  gArgv = argv;
#else
  if (argc == 2) {
    fp = fopen(argv[1], "r");
    if (fp == NULL)
      LOG_EXIT("No such file.");
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
  init_global_constants();
  init_context(function_table, init_global(), &context);

  init_code_loader(fp);
  n = code_loader(function_table);
  end_code_loader();

#ifdef PARALLEL
  // generates bytecode for parallel execution
  generateParallelCode(function_table, n);
  //printFuncTbl(functionTable, n);
#endif // PARALLEL

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

/*
   prints a JSValue
 */
void print_value_simple(Context *context, JSValue v) {
  print_value(context, v, 0);
}

void print_value_verbose(Context *context, JSValue v) {
  print_value(context, v, 1);
}

void print_value(Context *context, JSValue v, int verbose) {
  if (verbose)
    printf("%016lx, tag = %d: ", v, get_tag(v));
  switch (get_tag(v)) {
  case T_OBJECT:
    // v = object_to_string(context, v);
    {
      Object *p;

      p = remove_object_tag(v);
      // printf("tag = T_OBJECT, header tag = %ld\n", obj_header_tag(p));
      switch (obj_header_tag(p)) {
      case HTAG_OBJECT: v = gconsts.g_string_objtostr; break;
      // case HTAG_ARRAY:  v = cstr_to_string("array"); break;
      case HTAG_ARRAY:  v = array_to_string(context, v, gconsts.g_string_comma); break;
      case HTAG_FUNCTION: v = cstr_to_string("function"); break;
      case HTAG_BUILTIN: v = cstr_to_string("builtin"); break;
      case HTAG_ITERATOR: v = cstr_to_string("iterator"); break;
      case HTAG_REGEXP:
        printf("/%s/", regexp_pattern(remove_regexp_tag(v)));
        return;
      case HTAG_BOXED_STRING: v = cstr_to_string("boxed-string"); break;
      case HTAG_BOXED_NUMBER: v = cstr_to_string("boxed-number"); break;
      case HTAG_BOXED_BOOLEAN: v = cstr_to_string("boxed-boolean"); break;
      default: v = cstr_to_string("???"); break;
      }
    }
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
