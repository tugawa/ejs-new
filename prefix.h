/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#ifndef PREFIX_H_
#define PREFIX_H_

/*
  compilation options
*/

#ifndef NDEBUG
#define DEBUG 1
#define DEBUG_PRINT
#endif

#define STROBJ_HAS_HASH

/* #define CALC_TIME */
/* #define USE_PAPI */
/* #define USE_FASTGLOBAL */
/* #define USE_ASM2 */
/* #define CALC_CALL */

#define HIDDEN_CLASS

#define USE_OBC

#ifdef CALC_CALL
#define CALLCOUNT_UP() callcount++
#else
#define CALLCOUNT_UP()
#endif

#endif /* PREFIX_H_ */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
