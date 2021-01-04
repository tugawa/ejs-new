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

#include "iccprof.h"

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
#ifdef HC_PROF
int hcprint_flag;      /* prints all transitive hidden classes */
#endif /* HC_PROF */
#ifdef PROFILE
int profile_flag;      /* print the profile information */
char *poutput_name;    /* name of logging file */
int coverage_flag;     /* print the coverage */
int icount_flag;       /* print instruction count */
int forcelog_flag;     /* treat every instruction as ``_log'' one */
#endif
#ifdef ICCPROF
char *iccprof_name;    /* name of instruction-call-count profile file */
#endif /* ICCPROF */
#ifdef GC_PROF
int gcprof_flag;       /* print GC profile information */
#endif /* GC_PROF */

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
#ifdef ICCPROF
FILE *iccprof_fp;
#endif /* ICCPROF */

/*
 * parameter
 */
int regstack_limit = STACK_LIMIT; /* size of register stack in # of JSValues */
#ifdef JS_SPACE_BYTES
int heap_limit = JS_SPACE_BYTES; /* heap size in bytes */
#else /* JS_SPACE_BYTES */
int heap_limit = 1 * 1024 * 1024;
#endif /* JS_SPACE_BYTES */

#ifdef CALC_CALL
static uint64_t callcount = 0;
#endif

#define pp(v) (print_value_verbose(cxt, (v)), putchar('\n'))

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
#ifdef DEBUG
  { "-f",         0, &ftable_flag,    NULL          },
#endif /* DEBUG */
  { "-t",         0, &trace_flag,     NULL          },
  { "-a",         0, &all_flag,       NULL          },
  { "-u",         0, &cputime_flag,   NULL          },
  { "-R",         0, &repl_flag,      NULL          },
#ifdef HC_PROF
  { "--hc-prof",  0, &hcprint_flag,   NULL          },
#endif /* HC_PROF */
#ifdef PROFILE
  { "--profile",  0, &profile_flag,   NULL          },
  { "--poutput",  1, NULL,            &poutput_name },
  { "--coverage", 0, &coverage_flag,  NULL          },
  { "--icount",   0, &icount_flag,    NULL          },
  { "--forcelog", 0, &forcelog_flag,  NULL          },
#endif /* PROFILE */
#ifdef ICCPROF
  { "--iccprof",  1, NULL,            &iccprof_name },
#endif /* ICCPROF */
#ifdef GC_PROF
  { "--gc-prof",  0, &gcprof_flag,    NULL          },
#endif /* GC_PROF */
  { "-m",         1, &heap_limit,     NULL          },
  { "-s",         1, &regstack_limit, NULL          },
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
  printf("total CPU time = %ld.%03d msec, total GC time =  %d.%03d msec (#GC = %d)\n",
         sec * 1000 + usec / 1000, (int)(usec % 1000),
         gc_sec * 1000 + gc_usec / 1000, gc_usec % 1000, generation - 1);
}

#ifdef GC_PROF
void print_gc_prof()
{
  int i;
  uint64_t total_live_bytes = 0;
  uint64_t total_live_count = 0;

  for (i = 0; i <= NUM_DEFINED_CELL_TYPES; i++) {
    total_live_bytes += pertype_live_bytes[i];
    total_live_count += pertype_live_count[i];
  }

  printf("GC: %"PRId64" %"PRId64" ", total_alloc_bytes, total_alloc_count);
  printf("%"PRId64" %"PRId64" ",
         generation > 1 ? total_live_bytes / (generation - 1) : 0,
         generation > 1 ? total_live_count / (generation - 1) : 0);
  for (i = 0; i <= NUM_DEFINED_CELL_TYPES; i++) {
    printf(" %"PRId64" ", pertype_alloc_bytes[i]);
    printf(" %"PRId64" ", pertype_alloc_count[i]);
    printf(" %"PRId64" ",
           generation > 1 ? pertype_live_bytes[i] / (generation - 1) : 0);
    printf(" %"PRId64" ",
           generation > 1 ? pertype_live_count[i] / (generation - 1) : 0);
  }
  printf("\n");

  printf("total alloc bytes = %"PRId64"\n", total_alloc_bytes);
  printf("total alloc count = %"PRId64"\n", total_alloc_count);
  for (i = 0; i < 255; i++)
    if (pertype_alloc_count[i] > 0) {
      printf("  type %02x ", i);
      printf("a.bytes = %7"PRId64" ", pertype_alloc_bytes[i]);
      printf("a.count = %5"PRId64" ", pertype_alloc_count[i]);
      printf("l.bytes = %7"PRId64" ",
             generation > 1 ? pertype_live_bytes[i] / (generation - 1) : 0);
      printf("l.count = %4"PRId64" ",
             generation > 1 ? pertype_live_count[i] / (generation - 1) : 0);
      printf("%s\n", CELLT_NAME(i));
    }
}
#endif /* GC_PROF */

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
#ifdef ICCPROF
  if(iccprof_name != NULL){
    if ((iccprof_fp = fopen(iccprof_name, "w")) == NULL)
      fprintf(stderr, "Opening prof file %s failed.\n", iccprof_name);
  }
#endif /* ICCPROF */

  run_phase = PHASE_INIT;

#ifdef USE_BOEHMGC
  GC_INIT();
#endif
  init_memory(heap_limit);

  init_string_table(STRING_TABLE_SIZE);
  init_context(regstack_limit, &context);
  init_global_constants();
  init_meta_objects(context);
  init_global_objects(context);
  reset_context(context, function_table);
  context->global = gconsts.g_global;
#ifndef NO_SRAND
  srand((unsigned)time(NULL));
#endif /* NO_SRAND */

#ifdef ICCPROF
  iccprof_init();
#endif /* ICCPROF */

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
    enable_gc(context);
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

#ifdef FLONUM_PROF
    {
      extern void double_hash_flush(void);
      double_hash_flush();
    }
#endif /* FLONUM_PROF */

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

#ifdef GC_PROF
    if (gcprof_flag == TRUE)
      print_gc_prof();
#endif /* GC_PROF */

    if (repl_flag == TRUE) {
      printf("\xff");
      fflush(stdout);
    }
  }
#ifdef HC_PROF
  if (hcprint_flag == TRUE)
    hcprof_print_all_hidden_class();
#endif /* HC_PROF */
#ifdef PROFILE
  if (coverage_flag == TRUE)
    print_coverage(function_table, n);
  if (icount_flag == TRUE)
    print_icount(function_table, n);
  if (prof_stream != NULL)
    fclose(prof_stream);
#endif /* PROFILE */
#ifdef ICCPROF
  if(iccprof_fp != NULL){
    write_icc_profile(iccprof_fp);
    fclose(iccprof_fp);
  }
#endif /* ICCPROF */

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
    printf("%"PRIJSValue" (tag = %d, type = %s): ",
           v, get_ptag(v).v, type_name(v));

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
  else if (is_regexp(v)) {
    printf("/%s/", regexp_pattern(v));
    return;
  }
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
