/*
   header.h

   SSJS Project at the University of Electro-communications

   Sho Takada, 2012-13
   Akira Tanimura, 2012-13
   Akihiro Urushihara, 2013-14
   Ryota Fujii, 2013-14
   Tomoharu Ugawa, 2013-16
   Hideya Iwasaki, 2013-16
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

#ifdef USE_REGEXP
#include <oniguruma.h>
#endif // USE_REGEXP

#ifdef USE_BOEHMGC
#include <gc.h>
#endif

#ifdef PARALLEL
#include <pthread.h>
#include "SSJSVM_parallel.h"
#include "SSJSVM_parallel_thread.h"
#include "SSJSVM_parallel_tcp.h"
#include "SSJSVM_event_forward.h"
#endif

#ifndef __USE_GNU
#define __USE_GNU
#endif // __USE_GNU

typedef uint64_t JSValue;

#define BYTES_IN_JSVALUE (sizeof(JSValue))
#define BITS_IN_JSVALUE  (BYTES_IN_JSVALUE * 8)

#ifdef USE_BOEHMGC
// #define malloc(n) GC_malloc(n)
#define malloc(n) GC_MALLOC(n)
#define realloc(p, size) do { memcpy(malloc((size)), (p), (size));} while (0)
#define free GC_FREE
#endif // USE_BOEHMGC

#define SUCCESS  1
#define FAIL     0

#define TRUE     1
#define FALSE    0

#define HINT_NUMBER 1
#define HINT_STRING 0

#include "log.h"
#include "instructions.h"
#include "context.h"
#include "hash.h"
#include "types.h"
#include "gc.h"
#include "builtin.h"
#include "globals.h"
#include "extern.h"

#endif // HEADER_H_
