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
    as = new_array(context, 0);
  else if (!is_array(as))
    LOG_EXIT("apply: the second argument is expected to be an array");

  fn = args[0];
  thisobj = args[1];

  if (get_prop(as, gconsts.g_string_length, &alen_jsv) == SUCCESS)
    alen = number_to_cint(to_number(context, alen_jsv));
  else
    alen = 0;

  if (is_function(fn))
    ret = invoke_function(context, thisobj, fn, TRUE, as, alen);
  else if (is_builtin(fn))
    ret = invoke_builtin(context, thisobj, fn, TRUE, as, alen);
  else
    LOG_EXIT("apply: the receiver has to be a function/builtin");
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

ObjBuiltinProp function_funcs[] = {
  { "toString", function_toString, 0, ATTR_DE },
  { "apply",    function_apply,    2, ATTR_DE },
  { NULL, NULL, 0, ATTR_DE }
};

void init_builtin_function(Context *ctx)
{
  JSValue proto;

  /* TODO: implement an empty builtin function for prototype of Function
   *   Function.prototype is not an ordinary object but an empty
   *   function.
   */
  gconsts.g_function_proto = proto = new_normal_object(ctx);
  GC_PUSH(proto);
  set___proto___all(ctx, proto, gconsts.g_object_proto);
  hidden_proto(gobjects.g_hidden_class_function) = proto;
  hidden_proto(gobjects.g_hidden_class_builtin) = proto;

  gconsts.g_function =
    new_builtin_with_constr(ctx, function_constr, function_constr, 0);
  set_prototype_all(ctx, gconsts.g_function, proto);
  {
    ObjBuiltinProp *p = function_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name,
                        new_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
  }
  GC_POP(proto);
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
