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

#define not_implemented(s)                                              \
  LOG_EXIT("%s is not implemented yet\n", (s)); set_a(context, JS_UNDEFINED)

BUILTIN_FUNCTION(function_constr)
{
  not_implemented("function_constr");
}

BUILTIN_FUNCTION(function_prototype_fun)
{
  not_implemented("function_prototype_fun");
}

BUILTIN_FUNCTION(function_apply)
{
  builtin_prologue();
  JSValue fn;
  JSValue thisobj;
  JSValue as = args[2];
  JSValue ret;
  JSValue alen_jsv;
  int alen = 0;

  if (as == JS_UNDEFINED || as == JS_NULL)
    as = new_array_object(context, DEBUG_NAME("function_apply"),
                          gshapes.g_shape_Array, 0);
  else if (!is_array(as))
    LOG_EXIT("apply: the second argument is expected to be an array");

  GC_PUSH(as);

  if ((alen_jsv = get_prop_prototype_chain(as, gconsts.g_string_length))
      != JS_EMPTY)
    /* gccheck reports an error about alen_jsv falsely here */
    alen = number_to_cint(to_number(context, alen_jsv));
  else
    alen = 0;

  fn = args[0];
  thisobj = args[1];

  if (is_function(fn))
    ret = invoke_function(context, thisobj, fn, TRUE, as, alen);
  else if (is_builtin(fn))
    ret = invoke_builtin(context, thisobj, fn, TRUE, as, alen);
  else
    LOG_EXIT("apply: the receiver has to be a function/builtin");

  GC_POP(as);

  set_a(context, ret);
}

BUILTIN_FUNCTION(function_toString)
{
  JSValue ret;
  builtin_prologue();
  args = NULL;     /* suppress warning message */
  ret = cstr_to_string(context, "[function]");
  set_a(context, ret);
  return;
}

/*
 * property table
 */

/* prototype */
ObjBuiltinProp FunctionPrototype_builtin_props[] = {
  { "toString", function_toString, 0, ATTR_DE },
  { "apply",    function_apply,    2, ATTR_DE },
};
ObjDoubleProp  FunctionPrototype_double_props[] = {};
ObjGconstsProp FunctionPrototype_gconsts_props[] = {};
/* constructor */
ObjBuiltinProp FunctionConstructor_builtin_props[] = {};
ObjDoubleProp  FunctionConstructor_double_props[] = {};
ObjGconstsProp FunctionConstructor_gconsts_props[] = {
  { "prototype", &gconsts.g_prototype_Function, ATTR_ALL },
};
/* instance */
ObjBuiltinProp Function_builtin_props[] = {};
ObjDoubleProp  Function_double_props[] = {};
ObjGconstsProp Function_gconsts_props[] = {
  { "prototype", &gconsts.g_null,   ATTR_NONE }, /* placeholder */
};
DEFINE_PROPERTY_TABLE_SIZES_PCI(Function);

/* instance */
ObjBuiltinProp Builtin_builtin_props[] = {};
ObjDoubleProp  Builtin_double_props[] = {};
ObjGconstsProp Builtin_gconsts_props[] = {};
DEFINE_PROPERTY_TABLE_SIZES_I(Builtin);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
