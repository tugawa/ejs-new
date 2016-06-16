//
//  prefix.h
//  SSJSVM Project, Iwasaki-lab, UEC
//
//  Sho Takada, 2012-13
//  Akira Tanimura 2012-13
//  Akihiro Urushihara, 2013-14
//  Ryota Fujii, 2013-14
//  Hideya Iwasaki, 2013-16
//

#ifndef PREFIX_H_
#define PREFIX_H_

// compilation options
//

#define DEBUG 1
#define DEBUG_PRINT

#define STROBJ_HAS_HASH

//#define QUICKENING
#define USE_THRESHOLD
//#define PRINT_QUICKENING_COUNT
//#define CALC_TIME
//#define USE_PAPI
//#define USE_FASTGLOBAL
//#define USE_ASM2
//#define CALC_CALL
// #define USE_REGEXP
// #define USE_BOEHMGC

//#define LASTEXPR_PRINT

#ifndef USER_DEF

#define USE_JIT
#define USE_JIT_DEBUG
//#define USE_JIT_PROP_TYPEMAP
#define LLVMIR_PASS ("/Users/Akihiro/output.s")

#endif

// If you want to compile for J5MODE, define this.
// #define J5MODE

#ifdef J5MODE
#ifdef USE_REGEXP
#undef USE_REGEXP
#endif // USE_REGEXP
#ifdef USE_BOEHMGC
#undef USE_BOEHMGC
#endif // USE_HOEHMGC
#ifdef USE_JIT
#undef USE_JIT
#endif // USE_JIT
#endif // J5MODE

#ifdef DEBUG_PRINT

#define LOG(...) fprintf(stderr, __VA_ARGS__)
#define LOG_FUNC fprintf(stderr, "%-16s: ", __func__)
#define LOG_ERR(...) do { LOG_FUNC; fprintf(stderr, __VA_ARGS__); } while (0)
#define LOG_EXIT(...) do { LOG_FUNC; fprintf(stderr, __VA_ARGS__); exit(1); } while (0)

#else
#define LOG
#define LOG_FUNC
#define LOG_ERR
#define LOG_EXIT(...) exit(1)

#endif // DEBUG

#ifdef CALC_CALL
#define CALLCOUNT_UP() callcount++
#else
#define CALLCOUNT_UP()
#endif

#endif // PREFIX_H_
