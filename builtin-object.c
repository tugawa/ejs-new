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
      ret = new_number_object(context, arg);
    else if (is_boolean(arg))
      ret = new_boolean_object(context, arg);
    else if (is_string(arg))
      ret = new_string_object(context, arg);
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

  proto = gconsts.g_object_proto;
  GC_PUSH(proto);

  gconsts.g_object = obj =
    new_builtin_with_constr(ctx, object_constr, object_constr, 0);
  GC_PUSH(obj);
  set_prototype_all(ctx, obj, proto);

  /*
   * not implemented yet
   * set_obj_cstr_prop(g_object_proto, "hasOwnPropaty",
   *            new_builtin(objectProtoHasOwnPropaty, 0), ATTR_DE);
   */
  {
    ObjBuiltinProp *p = object_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name, 
                        new_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
  }
  GC_POP2(obj, proto);
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
