/*
   builtin-boolean.c

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-17
     Hideya Iwasaki, 2016-17

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/

#include "prefix.h"
#define EXTERN extern
#include "header.h"

// constructor of a boolean
//
BUILTIN_FUNCTION(boolean_constr)
{
  JSValue rsv;

  builtin_prologue();  
  rsv = new_normal_boolean_object(context, JS_TRUE);
  set___proto___all(context, rsv, gconsts.g_boolean_proto);
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

void init_builtin_boolean(Context *ctx)
{
  JSValue b, proto;

  gconsts.g_boolean = b = new_normal_builtin(ctx, boolean_constr, 1);
  gconsts.g_boolean_proto = proto =
    new_boolean_object(ctx, JS_FALSE, HSIZE_NORMAL, PSIZE_NORMAL);
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
}
