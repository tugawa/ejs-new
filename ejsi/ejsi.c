/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <sys/types.h>

#define DEFAULT_PROMPT          "eJSi> "
#define DEFAULT_EJSVM           "./ejsvm"
#define DEFAULT_EJSVM_SPEC      "./ejsvm.spec"
#define DEFAULT_EJSC_JAR        "./ejsc.jar"
#define DEFAULT_TMPDIR          "/tmp"

char *prompt, *ejsvm, *ejsc_jar, *ejsvm_spec, *tmpdir;
int verbose;

#define BUFLEN 1024

char frontend_buf[BUFLEN + 1];
char ejsc_command[BUFLEN + 1];
unsigned char output_buf[BUFLEN + 1];

#define N_TMPJS    128

char tmpjs_filename[N_TMPJS];
char tmpobc_filename[N_TMPJS];

#define R (0)
#define W (1)

#define TRUE   1
#define FALSE  0

int pipe_p2c[2];
int pipe_c2p[2];

struct commandline_option {
  char *str;
  int arg;
  int *flagvar;
  char **strvar;
};

struct commandline_option options_table[] = {
  { "-p",         1, NULL,            &prompt       },
  { "--prompt",   1, NULL,            &prompt       },
  { "-v",         1, NULL,            &ejsvm,       },
  { "--ejsvm",    1, NULL,            &ejsvm,       },
  { "-c",         1, NULL,            &ejsc_jar     },
  { "--ejsc",     1, NULL,            &ejsc_jar     },
  { "-s",         1, NULL,            &ejsvm_spec   },
  { "--spec",     1, NULL,            &ejsvm_spec   },
  { "-t",         1, NULL,            &tmpdir       },
  { "--tmpdir",   1, NULL,            &tmpdir       },
  { "--verbose",  0, &verbose,        NULL          },
  { (char *)NULL, 1, NULL,            NULL          }
};

int process_options(int ac, char *av[]) {
  int k;
  char *p;
  struct commandline_option *o;

  prompt = DEFAULT_PROMPT;
  ejsvm = DEFAULT_EJSVM;
  ejsc_jar = DEFAULT_EJSC_JAR;
  ejsvm_spec = DEFAULT_EJSVM_SPEC;
  tmpdir = DEFAULT_TMPDIR;
  verbose = FALSE;

  k = 1;
  p = av[1];
  while (k < ac) {
    if (p[0] == '-') {
      if (strcmp(p, "--") == 0) return k + 1;
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

int write_js(char buf[], int len) {
  int fd;
  int i;

  if ((fd = open(tmpjs_filename, O_WRONLY | O_CREAT, 0644)) < 0) {
      fprintf(stderr, "opening js file %s faild\n", tmpjs_filename);
      return -1;
  }

  write(fd, "it = (", 6);
  write(fd, buf, len);     /* len is the length of buf without '\0' */
  write(fd, ")\n", 2);       /* write ')', and '\n' */

  close(fd);
  return 0;
}

void close_pipe(int p[2]) {
  close(p[R]);
  close(p[W]);
}

int emptyp(char *b) {
  char c;
  while ((c = *b++) != '\0') {
    if (' ' < c && c < 0x7f)
      return FALSE;
  }
  return TRUE;
}

void remove_tmpfiles(void) {
  unlink(tmpjs_filename);
  unlink(tmpobc_filename);
}

int do_frontend(int pid) {
  int i, j, len, n, fn;
  int fd;
  char buf[BUFLEN];
  unsigned char tmp[2];

  snprintf(tmpjs_filename, N_TMPJS, "%s/tmp%d.js", tmpdir, pid);
  snprintf(tmpobc_filename, N_TMPJS, "%s/tmp%d.obc", tmpdir, pid);
  if (verbose == TRUE) {
    printf("tmpjs_filename = %s\n", tmpjs_filename);
    printf("tmpobc_filename = %s\n", tmpobc_filename);
  }
  remove_tmpfiles();

  fn = 0;
  for (i = 0; ; i++) {
    printf("%s", prompt);
    fflush(stdout);
    if (fgets(buf, BUFLEN, stdin) == NULL)
      return 0;
    if (emptyp(buf) == TRUE)
      continue;
    len = strlen(buf);
    buf[--len] = '\0';    /* write '\0' to override a newline character */

    if (len == 0)
      continue;

    if (strcmp(buf, "exit") == 0)
      return 0;

    if (write_js(buf, len) < 0) {
      fprintf(stderr, "creating JS file failed\n");
      return -1;
    }

    snprintf(ejsc_command, BUFLEN,
             "java -jar %s -fn %d -O --spec %s --out-obc %s",
             ejsc_jar, fn, ejsvm_spec, tmpjs_filename);
    if (verbose == TRUE)
      printf("ejsc_command = %s\n", ejsc_command);
    system(ejsc_command);

    if ((fd = open(tmpobc_filename, O_RDONLY)) < 0) {
      fprintf(stderr, "cannot open %s\n", tmpobc_filename);
      return -1;
    }

    if (read(fd, tmp, 2) != 2) {
      fprintf(stderr, "reading obc file failed\n");
      continue;
    }

    /* check the magic number */
    if (tmp[0] != 0xec) {
      fprintf(stderr, "unexpected magic number 0x%x of obc file\n", tmp[0]);
      continue;
    }
    /* tmp[1] is a hash value, which is not checked here */

    /* writes magic number and hash value */
    write(pipe_p2c[W], tmp, 2);

    if (read(fd, tmp, 2) != 2) {
      fprintf(stderr, "reading obc file failed\n");
      continue;
    }
    /* tmp[0] and tmp[1] are number of functions */
    write(pipe_p2c[W], tmp, 2);
    fn += tmp[0] * 256 + tmp[1];

    while ((n = read(fd, output_buf, BUFLEN)) > 0)
      write(pipe_p2c[W], output_buf, n);

    close(fd);
    remove_tmpfiles();

    printf("it = ");
    while ((n = read(pipe_c2p[R], output_buf, BUFLEN)) > 0) {
      if (output_buf[n - 1] == 0xff) {
        output_buf[n - 1] = '\0';
        printf("%s", output_buf);
        break;
      } else {
        output_buf[n] = '\0';
        printf("%s", output_buf);
      }
    }
    if (n > 0) continue;

    /*
     * The read system call returned 0 or negative value.
     * Possibly the pipe has been closed.
     */
    break;
  }
  return 0;
}

int main(int argc, char *argv[]) {
  int pid;
  int k, i;
  char **av;

  k = process_options(argc, argv);

  if (pipe(pipe_c2p) < 0) {
    fprintf(stderr, "creating pipe_c2p failed\n");
    return 1;
  }
  if (pipe(pipe_p2c) < 0) {
    fprintf(stderr, "creating pipe_p2c failed\n");
    close_pipe(pipe_c2p);
    return 1;
  }
    
  av = malloc(sizeof(char *) * (argc - k + 3));
                                  /* +3 is for ejsvm, -R, and NULL */
  av[0] = ejsvm;
  av[1] = "-R";          /* -R option is always necessary */
  for (i = 2; k < argc; i++, k++)
    av[i] = argv[k];
  av[i] = NULL;

  if (verbose == TRUE) {
    i = 0;
    while (av[i] != NULL) {
      printf("%d: %s\n", i, av[i]);
      i++;
    }
  }

  pid = fork();
  if (pid < 0) {
    fprintf(stderr, "fork failed\n");
    close_pipe(pipe_c2p);
    close_pipe(pipe_p2c);
    return 1;
  }
  if (pid == 0) {
    /* child: exec ejsvm */
    dup2(pipe_p2c[R], 0);
    dup2(pipe_c2p[W], 1);
    close_pipe(pipe_c2p);
    close_pipe(pipe_p2c);
    if (execv(ejsvm, av) < 0)
      fprintf(stderr, "exec %s failed in child process\n", ejsvm);
    return 1;
  }
    
  /* parent: frontend */
  close(pipe_p2c[R]);
  close(pipe_c2p[W]);
  do_frontend(pid);
  close(pipe_p2c[W]);
  close(pipe_c2p[R]);
  return 0;
}

