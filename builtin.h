
#define BUILTIN_FUNCTION(x) void x(Context* context, int na)
#define BUILTIN_FUNCTION_STATIC(x) static BUILTIN_FUNCTION(x)

#define LP_BUILTIN  ((FunctionFrame *)NULL)
#define CF_BUILTIN  ((FunctionTableCell *)NULL)
#define PC_BUILTIN  (-1)
