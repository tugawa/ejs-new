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
 *  phase
 */
int run_phase;         /* PHASE_INIT or PHASE_VMLOOP */

/*
 * flags
 */
int ftable_flag;       /* prints the function table */
int trace_flag;        /* prints every excuted instruction */
int lastprint_flag;    /* prints the result of the last expression */
int all_flag;          /* all flag values are true */
int cputime_flag;      /* prints the cpu time */
int repl_flag;         /* for REPL */
#ifdef HIDDEN_CLASS
int hcprint_flag;      /* prints all transitive hidden classes */
#endif
#ifdef PROFILE
int profile_flag;      /* print the profile information */
char *poutput_name;    /* name of logging file */
int coverage_flag;     /* print the coverage */
int icount_flag;       /* print instruction count */
int forcelog_flag;     /* treat every instruction as ``_log'' one */
#endif

/*
#define DEBUG_TESTTEST
*/

#if defined(USE_OBC) && defined(USE_SBC)
int obcsbc;
#endif

FILE *log_stream;
#ifdef PROFILE
FILE *prof_stream;
#endif

/*
 * parameter
 */
int regstack_limit = STACK_LIMIT; /* size of register stack (not used yet) */

#ifdef CALC_CALL
static uint64_t callcount = 0;
#endif

#define pp(v) (print_value_verbose(cxt, (v)), putchar('\n'))

/*
 * Debug function
 */
#ifdef DEBUG_TESTTEST
static void testtest(Context *cxt) {
  JSValue v, p;
  printf("Testtest: cxt->global = %016lx, gconsts.g_global = %016lx\n",
         cxt->global, gconsts.g_global);
  printf("Testtest: g_object = %016lx, g_object_proto = %016lx\n",
         gconsts.g_object, gconsts.g_object_proto);

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
  v = flonum_to_string(gconsts.g_flonum_infinity);
  printf("Infinity -> string: "); pp(v);
  v = flonum_to_string(gconsts.g_flonum_negative_infinity);
  printf("-Infinity -> string: "); pp(v);

  v = special_to_number(JS_UNDEFINED); printf("undefined -> number: "); pp(v);
  v = special_to_number(JS_NULL); printf("null -> number: "); pp(v);
  v = special_to_number(JS_TRUE); printf("true -> number: "); pp(v);
  v = special_to_number(JS_FALSE); printf("false -> number: "); pp(v);
  v = string_to_number(cstr_to_string("")); printf("\"\" -> number: "); pp(v);
  v = string_to_number(cstr_to_string("1.2"));
  printf("\"1.2\" -> number: "); pp(v);
  v = string_to_number(cstr_to_string("one"));
  printf("\"one\" -> number: "); pp(v);

  v = special_to_boolean(JS_UNDEFINED);
  printf("undefined -> boolean: "); pp(v);
  v = special_to_boolean(JS_NULL); printf("null -> boolean: "); pp(v);
  /*
   *  v = string_to_boolean(cstr_to_boolean(""));
   *  printf("\"\" -> boolean: "); pp(v);
   *  v = string_to_boolean(cstr_to_boolean("1.2"));
   *  printf("\"1.2\" -> boolean: "); pp(v);
   *  v = string_to_boolean(cstr_to_boolean("one"));
   *  printf("\"one\" -> boolean: "); pp(v);
   */
}
#endif

/*
 * processes command line options
 */
struct commandline_option {
  char *str;
  int arg;
  int *flagvar;
  char **strvar;
};

struct commandline_option  options_table[] = {
  { "-l",         0, &lastprint_flag, NULL          },
  { "-f",         0, &ftable_flag,    NULL          },
  { "-t",         0, &trace_flag,     NULL          },
  { "-a",         0, &all_flag,       NULL          },
  { "-u",         0, &cputime_flag,   NULL          },
  { "-R",         0, &repl_flag,      NULL          },
#ifdef HIDDEN_CLASS
  { "-h",         0, &hcprint_flag,   NULL          },
#endif
#ifdef PROFILE
  { "--profile",  0, &profile_flag,   NULL          },
  { "--poutput",  1, NULL,            &poutput_name },
  { "--coverage", 0, &coverage_flag,  NULL          },
  { "--icount",   0, &icount_flag,    NULL          },
  { "--forcelog", 0, &forcelog_flag,  NULL          },
#endif
  { "-s",         1, &regstack_limit, NULL          },  /* not used yet */
  { (char *)NULL, 0, NULL,            NULL          }
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
            if (o->flagvar != NULL) *(o->flagvar) = atoi(p);
            else if (o->strvar != NULL) *(o->strvar) = p;
          }
          break;
        } else
          o++;
      }
      if (o->str == (char *)NULL)
        fprintf(stderr, "unknown option: %s\n", p);
      k++;
      p = av[k];
    } else
      return k;
  }
  return k;
}

void print_cputime(time_t sec, suseconds_t usec) {
  printf("total CPU time = %ld.%d msec, total GC time =  %d.%d msec (#GC = %d)\n",
         sec * 1000 + usec / 1000, (int)(usec % 1000),
         gc_sec * 1000 + gc_usec / 1000, gc_usec % 1000, generation - 1);
#ifdef HIDDEN_CLASS
  printf("n_hc = %d, n_enter_hc = %d, n_exit_hc = %d\n",
         n_hc, n_enter_hc, n_exit_hc);
#endif
}

#ifdef PROFILE
void print_coverage(FunctionTable *ft, int n) {
  unsigned int loginsns = 0; /* number of logflag-set instructiones */
  unsigned int einsns = 0;   /* number of executed logflag-set instructions */
  int i, j;

  for (i = 0; i < n; i++) {
    Instruction *insns = ft[i].insns;
    int ninsns = ft[i].n_insns;
    for (j = 0; j < ninsns; j++) {
      if (insns[j].logflag == TRUE) {
        loginsns++;
        if (insns[j].count > 0) einsns++;
      }
    }
  }
  printf("coverage of logflag-set instructions = %d/%d", einsns, loginsns);
  if (loginsns > 0)
    printf(" = %7.3f%%", (double)einsns * 100 / (double)loginsns);
  putchar('\n');
}

void print_icount(FunctionTable *ft, int n) {
  int i, j;
  unsigned int *ic;

  if ((ic = (unsigned int *)malloc(sizeof(unsigned int) * numinsts)) == NULL) {
    fprintf(stderr, "Allocating instruction count table failed\n");
    return;
  }
  for (i = 0; i < numinsts; i++) ic[i] = 0;
  for (i = 0; i < n; i++) {
    Instruction *insns = ft[i].insns;
    int ninsns = ft[i].n_insns;
    for (j = 0; j < ninsns; j++)
      if (insns[j].logflag == TRUE)
        ic[(int)(get_opcode(insns[j].code))] += insns[j].count;
  }
  printf("instruction count\n");
  for (i = 0; i < numinsts; i++)
    printf("%3d: %10d  %s\n", i, ic[i], insn_nemonic(i));
  free(ic);
}
#endif

#ifndef NDEBUG
void **stack_start;
#endif /* NDEBUG */

#if defined(USE_OBC) && defined(USE_SBC)
/*
 * If the name ends with ".sbc", file_type returns FILE_SBC;
 * otherwise, returns FILE_OBC.
 */
int file_type(char *name) {
  int nlen = strlen(name);

  if (nlen >= 5 && name[nlen - 4] == '.' && name[nlen - 3] == 's' &&
      name[nlen - 2] == 'b' && name[nlen - 1] == 'c')
    return FILE_SBC;
  return FILE_OBC;
}
#endif

/*
 * main function
 */
int main(int argc, char *argv[]) {
  /* If input program is given from a file, fp is set to NULL. */
  FILE *fp = NULL;
  struct rusage ru0, ru1;
  int base_function = 0;
  int k, iter, nf;
  int n = 0;
  Context *context;

#ifdef CALC_TIME
  long long s, e;
#endif

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
#endif /* USE_PAPI */


#ifndef NDEBUG
  stack_start = (void **) &fp;
#endif /* NDEBUG */

  repl_flag = lastprint_flag = ftable_flag = trace_flag = all_flag = FALSE;
#ifdef PROFILE
  poutput_name = NULL;
  profile_flag = coverage_flag = icount_flag = forcelog_flag = FALSE;
#endif
  k = process_options(argc, argv);
  if (all_flag == TRUE) {
    lastprint_flag = ftable_flag = trace_flag = TRUE;
#ifdef PROFILE
    coverage_flag = icount_flag = TRUE;
#endif
  }
  if (repl_flag == TRUE)
    lastprint_flag = TRUE;

  /* set number of iterations */
  iter = (repl_flag == TRUE)? 0x7fffffff: argc;

#ifdef CALC_CALL
  callcount = 0;
#endif

  log_stream = stderr;

#ifdef PROFILE
  if (poutput_name == NULL)
    prof_stream = stdout;
  else if ((prof_stream = fopen(poutput_name, "w")) == NULL) {
    fprintf(stderr, "Opening prof file %s failed. Instead stdout is used.\n",
            poutput_name);
    prof_stream = stdout;
  }
#endif

  run_phase = PHASE_INIT;

#ifdef USE_BOEHMGC
  GC_INIT();
#endif
  init_memory();

  init_string_table(STRING_TABLE_SIZE);
  init_global_constants();
  init_global_malloc_objects();
  init_global_objects();
  init_context(function_table, gconsts.g_global, &context);
  init_builtin(context);
  srand((unsigned)time(NULL));

  for (; k < iter; k++) {
#if defined(USE_OBC) && defined(USE_SBC)
    obcsbc = FILE_OBC;
#endif
    if (k >= argc)
      fp = stdin;   /* stdin always use OBC */
    else {
      if ((fp = fopen(argv[k], "r")) == NULL)
        LOG_EXIT("%s: No such file.\n", argv[k]);
#if defined(USE_OBC) && defined(USE_SBC)
      obcsbc = file_type(argv[k]);
#endif
    }
    init_code_loader(fp);
    base_function = n;
    nf = code_loader(context, function_table, n);
    end_code_loader();
    if (nf > 0) n += nf;
    else if (fp != stdin) {
        LOG_ERR("code_loader returns %d\n", nf);
        continue;
    } else
      /* stdin is closed possibly by pressing ctrl-D */
      break;

    /* obtains the time before execution */
#ifdef USE_PAPI
    if (eventsize > 0) {
      int papi_result = PAPI_start_counters(events, eventsize);
      if (papi_result != 0)
        LOG_EXIT("papi failed:%d\n", papi_result);
    }
#endif /* USE_PAPI */

#ifdef CALC_TIME
    s = PAPI_get_real_usec();
#endif

    /* enters the VM loop */
    run_phase = PHASE_VMLOOP;
    if (cputime_flag == TRUE) getrusage(RUSAGE_SELF, &ru0);

    reset_context(context, &function_table[base_function]);
    vmrun_threaded(context, 0);

    if (cputime_flag == TRUE) getrusage(RUSAGE_SELF, &ru1);

    /* obtains the time after execution */
#ifdef CALC_TIME
    e = PAPI_get_real_usec();
#endif

#ifdef USE_PAPI
    if (eventsize > 0)
      PAPI_stop_counters(values, eventsize);
#endif

#ifndef USE_PAPI
#ifndef CALC_TIME
#ifndef CALC_CALL

    if (lastprint_flag == TRUE)
      debug_print(context, n);

#endif /* CALC_CALL */
#endif /* CALC_TIME */
#endif /* USE_PAPI */

#ifdef USE_PAPI
    if (eventsize > 0) {
      int i;
      for (i = 0; i < eventsize; i++)
        LOG("%"PRId64"\n", values[i]);
      LOG("%15.15e\n", ((double)values[1]) / (double)values[0]);
      LOG("L1 Hit Rate:%lf\n",
          ((double)values[0])/((double)values[0] + values[1]));
      LOG("L2 Hit Rate:%lf\n",
          ((double)values[2])/((double)values[2] + values[3]));
      LOG("L3 Hit Rate:%lf\n",
          ((double)values[4])/((double)values[4] + values[5]));
    }
#endif /* USE_PAPI */

#ifdef CALC_TIME
    LOG("%"PRId64"\n", e - s);
#endif

#ifdef CALC_CALL
    LOG("%"PRId64"\n", callcount);
#endif

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
    
    if (repl_flag == TRUE) {
      printf("\xff");
      fflush(stdout);
    }
  }
#ifdef PROFILE
#ifdef HIDDEN_CLASS
  if (hcprint_flag == TRUE)
    print_all_hidden_class();
#endif
  if (coverage_flag == TRUE)
    print_coverage(function_table, n);
  if (icount_flag == TRUE)
    print_icount(function_table, n);
  if (prof_stream != NULL)
    fclose(prof_stream);
#endif

  return 0;
}

/*
 * prints a JSValue
 */
void print_value_simple(Context *context, JSValue v) {
  print_value(context, v, 0);
}

void print_value_verbose(Context *context, JSValue v) {
  print_value(context, v, 1);
}

void print_value(Context *context, JSValue v, int verbose) {
  if (verbose)
    printf("%016"PRIx64" (tag = %d, type = %s): ", v, get_tag(v), type_name(v));

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
 * debug_print
 * This function is defined for the sake of the compatibility with the old VM.
 */
void debug_print(Context *context, int n) {
  /* int topsize; */
  JSValue res;

  /* topsize = context->function_table[0].n_insns; */
  res = get_a(context);
  simple_print(res);
  printf("\n");
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
