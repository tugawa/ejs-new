#define concat(s1,s2)      ejs_string_concat(context, (s1), (s2))
#define toString(v)        to_string(context, (v))
/* #define FlonumToCdouble(f) to_double(context, (f)) */
#define FlonumToCdouble(v) flonum_to_double((v))
#define CdoubleToNumber(x) double_to_number((x))
#define FixnumToCint(v)    fixnum_to_cint((v))
#define CintToNumber(x)    cint_to_number((x))
#define toCdouble(v)       to_double(context, (v))
#define toNumber(v)        to_number(context, (v))
