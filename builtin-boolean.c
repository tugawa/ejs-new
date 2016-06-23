#include "prefix.h"
#define EXTERN extern
#include "header.h"

// constructor of a boolean
//
BUILTIN_FUNCTION(boolean_constr)
{
  JSValue rsv;

  builtin_prologue();  
  rsv = new_boolean(JS_TRUE);
  set_obj_prop(rsv, "__proto__", gconsts.g_boolean_proto, ATTR_ALL);
  if (na > 0)
    boolean_object_value(rsv) = to_boolean(args[1]);
  set_a(context, rsv);
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

void init_builtin_boolean(void)
{
  gconsts.g_boolean = new_builtin(boolean_constr, 1);
  gconsts.g_boolean_proto = new_object();
  set_obj_prop(gconsts.g_boolean, "prototype", gconsts.g_boolean_proto, ATTR_ALL);
  {
    ObjBuiltinProp *p = boolean_funcs;
    while (p->name != NULL) {
      set_obj_prop(gconsts.g_boolean_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}
