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

#if 0
#ifdef DEBUG_PRINT

#define LOG(...) fprintf(stderr, __VA_ARGS__)
#define LOG_FUNC fprintf(stderr, "%-16s: ", __func__)
#define LOG_ERR(...) do { LOG_FUNC; fprintf(stderr, __VA_ARGS__); } while (0)
#define LOG_EXIT(...)                                                   \
  do { LOG_FUNC; fprintf(stderr, __VA_ARGS__); exit(1); } while (0)

#else
#define LOG
#define LOG_FUNC
#define LOG_ERR
#define LOG_EXIT(...) exit(1)

#endif /* DEBUG_PRINT */
#endif

#ifdef CALC_CALL
#define CALLCOUNT_UP() callcount++
#else
#define CALLCOUNT_UP()
#endif

#endif /* PREFIX_H_ */
