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
#include <string.h>
#include <ctype.h>

size_t cstr_length(const char *p)
{
  size_t l = 0;
  while (*p++ != '\0')
    l++;
  return l;
}

const char *ccstr_skip_space(const char *str)
{
  while (isspace(*str)) str++;
  return str;
}

char *cstr_skip_space(char *str)
{
  while (isspace(*str)) str++;
  return str;
}

JSValue cstr_parse_int(Context *ctx, const char *p, int radix)
{
  int sign = 1;
  cint limit;
  cint n;
  double x;

  p = ccstr_skip_space(p);

  if (p[0] == '-') {
    sign = -1;
    p += 1;
  } else if (p[0] == '+')
    p += 1;

  if (radix == PARSE_INT_RADIX_AUTO) {
    if (p[0] == '0' && (p[1] == 'x' || p[1] == 'X')) {
      radix = 16;
      p += 2;
    } else
      radix = 10;
  }
  limit = ((((cuint) 1) << (sizeof(cint) * 8 - 1)) - 1) / radix;

#define NO_VALID_DIGIT -1
  n = NO_VALID_DIGIT;
  x = 0.0;
  while (*p != '\0') {
    char c = *p++;
    int d;
    if ('0' <= c && c <= '9')
      d = c - '0';
    else if ('a' <= c && c <= 'z')
      d = c - 'a' + 10;
    else if ('A' <= c && c <= 'Z')
      d = c - 'A' + 10;
    else
      break;
    if (d >= radix)
      break;
    if (n == NO_VALID_DIGIT)
      n = d;
    else if (x != 0.0)
      x = x * radix + d;
    else {
      if (n > limit)
	x = ((double) n) * radix + d;
      else
	n = n * radix + d;
    }
  }

  if (n == NO_VALID_DIGIT)
    return gconsts.g_flonum_nan;
  if (n == 0 && sign == -1)
    x = -0.0;
  if (x != 0.0)
    return double_to_number(ctx, x);
  else
    return cint_to_number(ctx, sign * n);
#undef NO_VALID_DIGIT
}


JSValue cstr_parse_float(Context *ctx, const char* p)
{
  int sign = 1;
  int esign = 1;
  int64_t n;
  int en, exp;
#define S_SIGN    0
#define S_AS      1
#define S_A       2
#define S_BS      3
#define S_B       4
#define S_EXPSIGN 5
#define S_EXP     6
  int state;
  double x, factor;

#define FRACTION_MAX     (1LL << 52)
#define LOG_FRACTION_MAX 16
#define EXP_MAX          1024
#define EXP_MIN          (-1023)

  p = ccstr_skip_space(p);
  n = 0;
  en = 0;
  exp = 0;
  state = S_SIGN;
  while (*p != '\0') {
    char c = *p++;
    if (c == '+' || c == '-') {
      int s = c == '+' ? 1 : -1;
      if (state == S_SIGN) {
	sign = s;
	state = S_AS;
      } else if (state == S_EXPSIGN) {
	esign = s;
	state = S_EXP;
      } else
	break;
    } else if (c == 'e' || c == 'E') {
      if (state == S_A || state == S_B) {
	state = S_EXPSIGN;
      } else
	break;
    } else if (c == '.') {
      if (state == S_SIGN || state == S_AS)
	state = S_BS;
      else if (state == S_A)
	state = S_B;
      else
	break;
    } else if ('0' <= c && c <= '9') {
      int d = c - '0';
      if (state == S_SIGN || state == S_AS || state == S_A) {
	if (n >= FRACTION_MAX)
	  en += 1;
	else
	  n = n * 10 + d;
	state = S_A;
      } else if (state == S_BS || state == S_B) {
	if (n < FRACTION_MAX) {
	  n = n * 10 + d;
	  en -= 1;
	}
	state = S_B;
      } else if (state == S_EXPSIGN || state == S_EXP) {
	exp = exp * 10 + d;
	if (esign == 1) {
	  if (en + exp >= EXP_MAX + LOG_FRACTION_MAX)
	    break;
	} else {
	  if (en + -exp <= EXP_MIN - LOG_FRACTION_MAX)
	    break;
	}
	state = S_EXP;
      }
    } else
      break;
  }

  if (state == S_SIGN || state == S_AS || state == S_BS)
    return gconsts.g_flonum_nan;


  exp = en + esign * exp;
  if (n != 0) {
    while (n % 10 == 0) {
      n /= 10;
      exp += 1;
    }
  }
  x = (double) (sign * n);

  if (exp >= 0)
    factor = 10.0;
  else {
    factor = 0.1;
    exp = -exp;
  }
  while (exp > 0) {
    if ((exp & 1) == 1)
      x *= factor;
    factor = factor * factor;
    exp >>= 1;
  }

  return double_to_number(ctx, x);
}

