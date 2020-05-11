/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#ifndef HEADER_H_
#define HEADER_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stdbool.h>
#include <ctype.h>
#include <inttypes.h>
#include <assert.h>
#include <math.h>
#include <float.h>
#include <time.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <setjmp.h>

#ifdef USE_BOEHMGC
#include <gc.h>
#endif

#ifndef __USE_GNU
#define __USE_GNU
#endif /* __USE_GNU */
#ifdef DEBUG
#define DEBUG_NAME(name) name
#else /* DEBUG */
#define DEBUG_NAME(name) ""
#endif /* DEBUG */

#ifdef USE_BOEHMGC
/* #define malloc(n) GC_malloc(n) */
#define malloc(n) GC_MALLOC(n)
#define realloc(p, size) do { memcpy(malloc((size)), (p), (size));} while (0)
#define free GC_FREE
#endif /* USE_BOEHMGC */

#define SUCCESS  1
#define FAIL     0

#define TRUE     1
#define FALSE    0

#define HINT_NUMBER 1
#define HINT_STRING 0

#define PHASE_INIT   0
#define PHASE_VMLOOP 1

#define FILE_OBC   1
#define FILE_SBC   2

#include "prefix.h"
#include "types.h"
#include "context.h"
#include "gc.h"

#ifdef COPYGC
#include "copy-collector.h"
#else /* MARKSWEEP */
#include "marksweep-collector.h"
#ifdef BIBOP
#include "bibop-space.h"
#else /* BIBOP */
#include "freelist-space.h"
#endif /* BIBIOP */
#endif /* MARKSWEEP */

#include "hash.h"
#include "log.h"
#include "instructions.h"
#include "builtin.h"
#include "globals.h"
#include "extern.h"
#ifdef USE_VMDL
#include "vmdl-helper.h"
#endif /* USE_VMDL */

#include "context-inl.h"
#include "types-inl.h"
#include "gc-inl.h"

#ifdef COPYGC
#else /* MARKSWEEP */
#ifdef BIBOP
#include "bibop-space-inl.h"
#else /* BIBOP */
#include "freelist-space-inl.h"
#endif /* BIBOP */
#endif /* MARKSWEEP */

#endif /* HEADER_H_ */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
