#include "prefix.h"
#define EXTERN extern
#include "header.h"

// constructor for an object
//
BUILTIN_FUNCTION(object_constr)
{
  JSValue rsv, ret, arg;
  Tag tag;

  builtin_prologue();
  rsv = args[0];

  printf("called object_constr, na = %d\n", na);
  // If this is called with `new', which kind of object is allocated
  // depends on the type of the first argument.
  if (na > 0) {
    arg = args[1];
    tag = get_tag(arg);
    switch(tag){
    case T_OBJECT:
      ret = arg;
      break;
    case T_FIXNUM:
    case T_FLONUM:
      ret = new_number(arg);
      set_prop_all(ret, gconsts.g_string___proto__, gconsts.g_number_proto);
      break;
    case T_SPECIAL:
      if (is_true(arg) || is_false(arg)) {
        ret = new_boolean(arg);
        set_prop_all(ret, gconsts.g_string___proto__, gconsts.g_boolean_proto);
      } else {
        ret = new_object();
        set_prop_all(ret, gconsts.g_string___proto__, gconsts.g_object_proto);
      }
      break;
    case T_STRING:
      ret = new_string(arg);
      set_prop_all(ret, gconsts.g_string___proto__, gconsts.g_string_proto);
      break;
    }
  } else {
    //    printf("object_constr: na == 0, calls new_object\n");
    //    printf("gconsts.g_object_proto = %016lx\n", gconsts.g_object_proto);
    ret = new_object();
    //    printf("ret = %016lx\n", ret);
    set_prop_all(ret, gconsts.g_string___proto__, gconsts.g_object_proto);
  }
  set_a(context, ret);
}

BUILTIN_FUNCTION(object_toString)
{
  set_a(context, gconsts.g_string_objtostr);
}

ObjBuiltinProp object_funcs[] = {
  { "toString",       object_toString,       0, ATTR_DE },
#ifdef PARALLEL
  {  "setShared",     objectProtoSetShared,  0, ATTR_DE },
#endif
  { NULL,             NULL,                  0, ATTR_DE }
};

void init_builtin_object(void)
{
  JSValue obj, proto;
  gconsts.g_object = obj =
    new_builtin_with_constr(object_constr, object_constr, 0);
  gconsts.g_object_proto = proto = new_object();
  set_prop_de(obj, gconsts.g_string_prototype, proto);
  {
    ObjBuiltinProp *p = object_funcs;
    while (p->name != NULL) {
      set_obj_cstr_prop(proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
}

