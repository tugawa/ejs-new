/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include <stdlib.h>
#include <stdio.h>
#include "prefix.h"
#define EXTERN extern
#include "header.h"
#include "log.h"

static Context *the_context;
#include "gc-visitor-inl.h"

#ifdef AS_PROF
extern "C" {extern void print_as_prof(Context *ctx);}
#endif /* AS_PROF */

/*
 * Constants
 */

enum gc_phase {
  PHASE_INACTIVE,
  PHASE_INITIALISE,
  PHASE_MARK,
  PHASE_WEAK,
  PHASE_SWEEP,
  PHASE_FINALISE,
};

/*
 * Variables
 */
static enum gc_phase gc_phase = PHASE_INACTIVE;

/*
 * Tracer
 */
#include "mark-tracer.h"

/*
 * GC
 */

void garbage_collection(Context *ctx)
{
  /* initialise */
  gc_phase = PHASE_INITIALISE;
  the_context = ctx;

  /* mark */
  gc_phase = PHASE_MARK;
  scan_roots<DefaultTracer>(ctx);
  DefaultTracer::process_mark_stack();

  /* profile */
#ifdef CHECK_MATURED
  check_matured();
#endif /* CHECK_MATURED */

#ifdef AS_PROF
  printf("==========AFTER MARK PHASE=========\n");
  print_as_prof(ctx);
#endif /* AS_PROF */

  /* weak */
  gc_phase = PHASE_WEAK;
  weak_clear<DefaultTracer>(ctx);

  /* sweep */
  gc_phase = PHASE_SWEEP;
  sweep();

  /* finalise */
  gc_phase = PHASE_FINALISE;

  gc_phase = PHASE_INACTIVE;
}
