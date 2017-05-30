/*
   builtin-object.c

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

/*
   constructor for an object
 */
BUILTIN_FUNCTION(object_constr)
{
  JSValue rsv, ret, arg;
  Tag tag;

  builtin_prologue();
  rsv = args[0];
  // printf("called object_constr, na = %d\n", na);
  /*
     If this is called with `new', which kind of object is allocated
     depends on the type of the first argument.
  */
  if (na > 0) {
    arg = args[1];
    tag = get_tag(arg);
    switch(tag){
    case T_GENERIC:
      ret = arg;
      break;
    case T_FIXNUM:
    case T_FLONUM:
      ret = new_normal_number(context, arg);
      // set___proto___all(context, ret, gconsts.g_number_proto);
      break;
    case T_SPECIAL:
      if (is_true(arg) || is_false(arg)) {
        ret = new_normal_boolean(context, arg);
        // set___proto___all(context, ret, gconsts.g_boolean_proto);
      } else {
        ret = new_normal_object(context);
        // set___proto___all(context, ret, gconsts.g_object_proto);
      }
      break;
    case T_STRING:
      ret = new_normal_string(context, arg);
      // set___proto___all(context, ret, gconsts.g_string_proto);
      break;
    }
  } else {
    //    printf("object_constr: na == 0, calls new_object\n");
    //    printf("gconsts.g_object_proto = %016lx\n", gconsts.g_object_proto);
    ret = new_normal_object(context);
    //    printf("ret = %016lx\n", ret);
    // set___proto___all(context, ret, gconsts.g_object_proto);
  }
  set_a(context, ret);
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

  gconsts.g_object = obj =
    // new_builtin_with_constr(ctx, object_constr, object_constr, 0, HSIZE_NORMAL, PSIZE_NORMAL);
    new_normal_builtin_with_constr(ctx, object_constr, object_constr, 0);
#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_object", obj_hidden_class(obj));
#endif
#endif
  proto = gconsts.g_object_proto;
  set_prototype_de(ctx, obj, proto);
#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_object", obj_hidden_class(obj));
#endif
#endif

  // not implemented yet
  // set_obj_cstr_prop(g_object_proto, "hasOwnPropaty",
  //            new_builtin(objectProtoHasOwnPropaty, 0), ATTR_DE);

  gconsts.g_function_proto = new_normal_object(ctx);
  {
    ObjBuiltinProp *p = object_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(ctx, proto, p->name, 
                        new_normal_builtin(ctx, p->fn, p->na), p->attr);
      p++;
    }
  }
#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
  print_hidden_class("g_function_proto", obj_hidden_class(gconsts.g_function_proto));
#endif
#endif
}
