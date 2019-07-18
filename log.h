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

#ifdef DEBUG_PRINT

#define LOG(...) fprintf(log_stream, __VA_ARGS__)
#define LOG_FUNC fprintf(log_stream, "%-16s: ", __func__)
#define LOG_ERR(...)                                    \
  do { LOG_FUNC; fprintf(log_stream, __VA_ARGS__);      \
    putc('\n', log_stream); }                           \
  while (0)
#define LOG_EXIT(...)                                   \
  do { LOG_FUNC; fprintf(log_stream, __VA_ARGS__);      \
    putc('\n', log_stream);  exit(1); }                 \
  while (0)

#else
#define LOG(...)
#define LOG_FUNC
#define LOG_ERR(...)                                                    \
  do { fprintf(stderr, __VA_ARGS__); putc('\n', stderr); } while (0)
#define LOG_EXIT(...)                                                   \
  do { fprintf(stderr,  __VA_ARGS__); putc('\n', stderr); exit(1); } while (0)

#endif /* DEBUG */

#endif /* LOG_H_ */
