/*
 * eJS Project
 * Kochi University of Technology
 * the University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at the University of
 * Electro-communications, which was contributed by the following members.
 */

#include "prefix.h"
#define EXTERN extern
#include "header.h"

/*
 * constructor of a boolean
 */
BUILTIN_FUNCTION(boolean_constr)
{
  JSValue rsv;

  builtin_prologue();  
  rsv = new_normal_boolean_object(context, JS_TRUE);
#ifndef HIDDEN_CLASS_PROTO
  GC_PUSH(rsv);
  set___proto___all(context, rsv, gconsts.g_boolean_proto);
#endif /* HIDDEN_CLASS_PROTO */
  if (na > 0)
    boolean_object_value(rsv) = to_boolean(args[1]);
  set_a(context, rsv);
#ifndef HIDDEN_CLASS_PROTO
  GC_POP(rsv);
#endif /* HIDDEN_CLASS_PROTO */
}

BUILTIN_FUNCTION(boolean_valueOf)
{
  JSValue arg;

  builtin_prologue();  
  arg = args[0];
  if (is_boolean_object(arg))
    arg = boolean_object_value(arg);
  set_a(context, arg);
}

ObjBuiltinProp boolean_funcs[] = {
  { "valueOf",        boolean_valueOf,    0, ATTR_DE },
  { NULL,             NULL,               0, ATTR_DE }
};

void init_builtin_boolean(Context *ctx)
{
  JSValue b, proto;

  b = new_normal_builtin(ctx, boolean_constr, 1);
  GC_PUSH(b);
  gconsts.g_boolean = b;
  proto = new_boolean_object(ctx, JS_FALSE, HSIZE_NORMAL, PSIZE_NORMAL);
  gconsts.g_boolean_proto = proto;
  GC_PUSH(proto);
#ifdef HIDDEN_CLASS_PROTO
  hidden_proto(gobjects.g_hidden_class_boxed_boolean) = proto;
#endif /* HIDDEN_CLASS_PROTO */
  set_prototype_de(ctx, b, proto);
  set___proto___all(ctx, proto, gconsts.g_object_proto);
  {
    ObjBuiltinProp *p = boolean_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name,
                        new_normal_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
  }
  GC_POP2(proto, b);
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
