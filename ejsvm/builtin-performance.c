/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include "prefix.h"
#define EXTERN extern
#include "header.h"

static int has_seen_time = 0;
static struct timeval first_tv;

BUILTIN_FUNCTION(perf_now)
{
  /* TODO: use rdtsc */
  double current_time;
  
  if (has_seen_time) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    current_time = (tv.tv_sec - first_tv.tv_sec) * 1000;
    current_time += (tv.tv_usec - first_tv.tv_usec) / 1000;
  } else {
    has_seen_time = 1;
    gettimeofday(&first_tv, NULL);
    current_time = 0;
  }

  set_a(context, double_to_number(context, current_time));
}

ObjBuiltinProp Performance_builtin_props[] = {
  { "now",  perf_now, 0, ATTR_DE }
};
ObjDoubleProp  Performance_double_props[] = {};
ObjGconstsProp Performance_gconsts_props[] = {};
DEFINE_PROPERTY_TABLE_SIZES_I(Performance);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
