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

/*
 * constructor for an object
 */
BUILTIN_FUNCTION(object_constr)
{
  JSValue ret, arg;

  builtin_prologue();
  /*
   * If this is called with `new', which kind of object is allocated
   * depends on the type of the first argument.
   *
   * ES5 specification Sec 15.2.2.1 requires not to create an object
   * if the argument is native ECMAScript object, i.e, those that are
   * not String, Boolean, or Number.  Though JavaScript Core seems
   * to create an object for an Array argument.
   *
   * TODO: use dispacher generator
   */
  if (na > 0) {
    arg = args[1];
    GC_PUSH(arg);
    if (is_object(arg))
      ret = arg;
    else if (is_number(arg))
      ret = new_normal_number_object(context, arg);
    else if (is_boolean(arg))
      ret = new_normal_boolean_object(context, arg);
    else if (is_string(arg))
      ret = new_normal_string_object(context, arg);
    else
      ret = new_normal_object(context);
    GC_POP(arg);
  } else
    ret = new_normal_object(context);
  GC_PUSH(ret);
  set_a(context, ret);
  GC_POP(ret);
}

BUILTIN_FUNCTION(object_toString)
{
  set_a(context, gconsts.g_string_objtostr);
}

ObjBuiltinProp object_funcs[] = {
  { "toString",       object_toString,       0, ATTR_DE },
  { NULL,             NULL,                  0, ATTR_DE }
};

void init_builtin_object(Context *ctx)
{
  JSValue obj, proto;

  obj = new_normal_builtin_with_constr(ctx, object_constr, object_constr, 0);
  // new_builtin_with_constr(ctx, object_constr, object_constr, 0, HSIZE_NORMAL, PSIZE_NORMAL);
  GC_PUSH(obj);
  gconsts.g_object = obj;
#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_object", obj_hidden_class(obj));
#endif
#endif
  proto = gconsts.g_object_proto;
  GC_PUSH(proto);
  set_prototype_de(ctx, obj, proto);
#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_object", obj_hidden_class(obj));
#endif
#endif

  /*
   * not implemented yet
   * set_obj_cstr_prop(g_object_proto, "hasOwnPropaty",
   *            new_builtin(objectProtoHasOwnPropaty, 0), ATTR_DE);
   */
  /*
   * The next line is unnecessary because init_builtin_function has
   * been already called and gconsts.g_function_proto has been
   * properly set.
   *
   * gconsts.g_function_proto = new_normal_object(ctx);
   */
  {
    ObjBuiltinProp *p = object_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name, 
                        new_normal_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
  }
  GC_POP2(proto, obj);
#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_function_proto",
                     obj_hidden_class(gconsts.g_function_proto));
#endif
#endif
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
