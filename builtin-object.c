#include "prefix.h"
#define EXTERN extern
#include "header.h"

// constructor for an object
//
BUILTIN_FUNCTION(object_constr)
{
  JSValue rsv;
  uint64_t tag;
  JSValue ret;

  builtin_prologue();
  rsv = args[0];

  // If this is called with `new', which kind of object is allocated
  // depends on the type of the first argument.
  if (na > 0) {
    JSValue arg;
    arg = args[1];
    tag = get_tag(arg);
    switch(tag){
    case T_OBJECT:
      ret = arg;
      break;
    case T_FIXNUM:
    case T_FLONUM:
      ret = new_number(arg);
      set_obj_prop(ret, "__proto__", gobj.g_number_proto, ATTR_ALL);
      break;
    case T_SPECIAL:
      if (is_true(arg) || is_false(arg)) {
        ret = new_boolean(arg);
        set_obj_prop(ret, "__proto__", gobj.g_boolean_proto, ATTR_ALL);
      } else {
        ret = new_object();
        set_obj_prop(ret, "__proto__", gobj.g_object_proto, ATTR_ALL);
      }
      break;
    case T_STRING:
      ret = new_string(arg);
      set_obj_prop(ret, "__proto__", gobj.g_string_proto, ATTR_ALL);
      break;
    }
  } else {
    ret = new_object();
    set_obj_prop(ret, "__proto__", gobj.g_object_proto, ATTR_ALL);
  }
  set_a(context, ret);
}

BUILTIN_FUNCTION(object_toString)
{
  set_a(context, gobj.g_string_objtostr);
}

void init_builtin_object(void)
{
}

