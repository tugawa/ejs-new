/*
   builtin.h

   SSJS Project at the University of Electro-communications

   Sho Takada, 2012-13
   Akira Tanimura, 2012-13
   Akihiro Urushihara, 2013-14
   Ryota Fujii, 2013-14
   Tomoharu Ugawa, 2013-16
   Hideya Iwasaki, 2013-16
*/

#define BUILTIN_FUNCTION(x) void x(Context *context, int fp, int na)
// #define BUILTIN_FUNCTION(x) void x(Context *context, int na)
#define BUILTIN_FUNCTION_STATIC(x) static BUILTIN_FUNCTION(x)

#define get_args()  ((JSValue *)(&(get_stack(context, fp))))

#define builtin_prologue() JSValue *args = get_args()

// #define builtin_prologue() \
//   int fp; JSValue *args; fp = get_fp(context); args = get_args()

#define max(a, b) ((a) > (b) ? (a) : (b))
#define min(a, b) ((a) < (b) ? (a) : (b))

typedef struct obj_builtin_prop {
  char *name;
  builtin_function_t fn;
  int na;
  Attribute attr;
} ObjBuiltinProp;

typedef struct obj_double_prop {
  char *name;
  double value;
  Attribute attr;
} ObjDoubleProp;

typedef struct obj_gconsts_prop {
  char *name;
  JSValue *addr;
  Attribute attr;
} ObjGconstsProp;
