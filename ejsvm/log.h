/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#ifndef LOG_H_
#define LOG_H_

#if defined(COCCINELLE_CHECK)

#define LOG(...)
#define LOG_FUNC
#define LOG_ERR(...) return
#define LOG_EXIT(...) return
#define LOG_EXIT2(context, ...) return

#define ASSERT_OBJECT(o)

#elif defined(DEBUG_PRINT)

#define LOG(...) fprintf(log_stream, __VA_ARGS__)
#define LOG_FUNC fprintf(log_stream, "%-16s: ", __func__)
#define LOG_ERR(...)                                    \
  do { LOG_FUNC; fprintf(log_stream, __VA_ARGS__);      \
    putc('\n', log_stream); }                           \
  while (0)

#ifdef DEBUG
#define LOG_EXIT(...)                           \
  do {                                          \
    LOG_FUNC;                                   \
    fprintf(log_stream, __VA_ARGS__);           \
    putc('\n', log_stream);                     \
    abort();                                    \
  } while (0)
#define LOG_EXIT2(context, ...)                 \
  do {                                          \
    LOG_FUNC;                                   \
    fprintf(log_stream, __VA_ARGS__);           \
    putc('\n', log_stream);                     \
    print_backtrace(context);                   \
    abort();                                    \
  } while (0)
#else /* DEBUG */
#define LOG_EXIT(...)                           \
  do {                                          \
    LOG_FUNC;                                   \
    fprintf(log_stream, __VA_ARGS__);           \
    putc('\n', log_stream);                     \
    exit(1);                                    \
  } while (0)
#define LOG_EXIT2(context, ...)                 \
  do {                                          \
    LOG_FUNC;                                   \
    fprintf(log_stream, __VA_ARGS__);           \
    putc('\n', log_stream);                     \
    print_backtrace(context);                   \
    abort();                                    \
  } while (0)
#endif /* DEBUG */

#define ASSERT_OBJECT(o) do {                               \
    if (!is_object(o))                                      \
      LOG_EXIT("assertion failed. not an object.");         \
  } while (0)

#else

#define LOG(...)
#define LOG_FUNC
#define LOG_ERR(...)
#define LOG_EXIT(...) exit(1)
#define LOG_EXIT2(context, ...) exit(1)
#define ASSERT_OBJECT(o)

#endif /* DEBUG */

#endif /* LOG_H_ */

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
