/*
   main.c

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-17
     Hideya Iwasaki, 2016-17

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/

#include "prefix.h"
#define EXTERN
#include "header.h"

/*
  phase
 */
int run_phase;         // PHASE_INIT or PHASE_VMLOOP

/*
  flags
 */
int ftable_flag;       // prints the function table
int trace_flag;        // prints every excuted instruction
int lastprint_flag;    // prints the result of the last expression
int all_flag;          // all flag values are true
int cputime_flag;      // prints the cpu time
#ifdef HIDDEN_CLASS
int hcprint_flag;      // prints all transitive hidden classes
#endif

/*
  parameter
 */
int regstack_limit = STACK_LIMIT;   // size of register stack (not used yet)

FILE *log_stream;

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

  v = new_simple_object(cxt);
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
   processes command line options
 */
struct commandline_option {
  char *str;
  int arg;
  int *flagvar;
};

struct commandline_option  options_table[] = {
  { "-l", 0, &lastprint_flag     },
  { "-f", 0, &ftable_flag        },
  { "-t", 0, &trace_flag         },
  { "-a", 0, &all_flag           },
  { "-u", 0, &cputime_flag       },
#ifdef HIDDEN_CLASS
  { "-h", 0, &hcprint_flag       },
#endif
  { "-s", 1, &regstack_limit     },      // not used yet
  { (char *)NULL, 0, (int *)NULL }
};

int process_options(int ac, char *av[]) {
  int k;
  char *p;
  struct commandline_option *o;

  k = 1;
  p = av[1];
  while (k < ac) {
    if (p[0] == '-') {
      o = &options_table[0];
      while (o->str != (char *)NULL) {
        if (strcmp(p, o->str) == 0) {
          if (o->arg == 0) *(o->flagvar) = TRUE;
          else {
            k++;
            p = av[k];
            *(o->flagvar) = atoi(p);
          }
          break;
        } else
          o++;
      }
      if (o->str == (char *)NULL)
        printf("unknown option: %s\n", p);
      k++;
      p = av[k];
    } else
      return k;
  }
  return 0;
}

void print_cputime(time_t sec, suseconds_t usec) {
  printf("total CPU time = %ld.%d msec, total GC time =  %d.%d msec (#GC = %d)\n",
          sec * 1000 + usec / 1000, (int)(usec % 1000),
          gc_sec * 1000 + gc_usec / 1000, gc_usec % 1000, generation - 1);
#ifdef HIDDEN_CLASS
  printf("n_hc = %d, n_enter_hc = %d, n_exit_hc = %d\n", n_hc, n_enter_hc, n_exit_hc);
#endif
}


#ifndef NDEBUG
void **stack_start;
#endif /* NDEBUG */

/*
   main function
 */
int main(int argc, char *argv[]) {
  // If input program is given from a file, fp is set to NULL.
  FILE *fp = NULL;
  struct rusage ru0, ru1;
  int k;

#ifndef NDEBUG
  stack_start = (void **) &fp;
#endif /* NDEBUG */

  log_stream = stderr;
  lastprint_flag = ftable_flag = trace_flag = all_flag = FALSE;
  k = process_options(argc, argv);
  if (all_flag == TRUE)
    lastprint_flag = ftable_flag = trace_flag = TRUE;

  // printf("regstack_limit = %d\n", regstack_limit);
  // printf("lastprint_flag = %d, ftable_flag = %d, trace_flag = %d, k = %d\n",
  //        lastprint_flag, ftable_flag, trace_flag, k);
  if (k > 0) {
    if ((fp = fopen(argv[k], "r")) == NULL)
      LOG_EXIT("%s: No such file.\n", argv[k]);
  }

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

  run_phase = PHASE_INIT;

#ifdef USE_BOEHMGC
  GC_INIT();
#endif // USE_BOEHMGC
  init_memory();

  init_string_table(STRING_TABLE_SIZE);
  init_global_constants();
  init_global_malloc_objects();
  init_global_objects();
  init_context(function_table, gconsts.g_global, &context);
  init_builtin(context);

  srand((unsigned)time(NULL));
  init_code_loader(fp);
  n = code_loader(context, function_table);
  end_code_loader();

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
  run_phase = PHASE_VMLOOP;
  if (cputime_flag == TRUE) getrusage(RUSAGE_SELF, &ru0);
  vmrun_threaded(context, 0);
  if (cputime_flag == TRUE) getrusage(RUSAGE_SELF, &ru1);

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

  if (lastprint_flag == TRUE) {
#ifdef USE_FFI
    if (isErr(context)) {
      printf("Exception!\n");
      printJSValue(getErr(context));
    } else
      debug_print(context, n);
#else
    debug_print(context, n);
#endif  // USE_FFI
  }

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

  if (cputime_flag == TRUE) {
    time_t sec;
    suseconds_t usec;

    sec = ru1.ru_utime.tv_sec - ru0.ru_utime.tv_sec;
    usec = ru1.ru_utime.tv_usec - ru0.ru_utime.tv_usec;
    if (usec < 0) {
      sec--;
      usec += 1000000;
    }
    print_cputime(sec, usec);
  }
#ifdef HIDDEN_CLASS
  if (hcprint_flag == TRUE)
    print_all_hidden_class();
#endif


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
    printf("%016"PRIu64" (tag = %d, type = %s): ", v, get_tag(v), type_name(v));

  if (is_string(v))
    /* do nothing */;
  else if (is_number(v))
    v = number_to_string(v);
  else if (is_special(v))
    v = special_to_string(v);
  else if (is_simple_object(v))
    v = gconsts.g_string_objtostr;
  else if (is_array(v))
    v = array_to_string(context, v, gconsts.g_string_comma);
  else if (is_function(v))
    v = cstr_to_string(NULL, "function");
  else if (is_builtin(v))
    v = cstr_to_string(NULL, "builtin");
  else if (is_iterator(v))
    v = cstr_to_string(NULL, "iterator");
  else if (is_simple_iterator(v))
    v = cstr_to_string(NULL, "simple_iterator");
#ifdef USE_REGEXP
#ifdef need_regexp
  else if (is_regexp(v)) {
    printf("/%s/", regexp_pattern(v));
    return;
  }
#endif /* need_regexp */
#endif
  else if (is_string_object(v))
    v = cstr_to_string(NULL, "boxed-string");
  else if (is_number_object(v))
    v = cstr_to_string(NULL, "boxed-number");
  else if (is_boolean_object(v))
    v = cstr_to_string(NULL, "boxed-boolean");
  else
    LOG_ERR("Type Error\n");

  printf("%s", string_to_cstr(v));
}

void simple_print(JSValue v) {
  if (is_number(v))
    printf("number:%le", number_to_double(v));
  else if (is_string(v))
    printf("string:%s", string_to_cstr(v));
  else if (is_object(v))
    printf("object:object");
  else if (v == JS_TRUE)
    printf("boolean:true");
  else if (v == JS_FALSE)
    printf("boolean:false");
  else if (v == JS_UNDEFINED)
    printf("undefined:undefined");
  else if (v == JS_NULL)
    printf("object:null");
  else
    printf("unknown value");
}

/*
  debug_print
  This function is defined for the sake of the compatibility with the old VM.
 */
void debug_print(Context *context, int n) {
  // int topsize;
  JSValue res;

  // topsize = context->function_table[0].n_insns;
  res = get_a(context);
  simple_print(res);
  printf("\n");
}
