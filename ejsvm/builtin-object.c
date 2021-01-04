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
      ret = new_number_object(context, DEBUG_NAME("obect_constr"),
                              gshapes.g_shape_Number, arg);
    else if (is_boolean(arg))
      ret = new_boolean_object(context, DEBUG_NAME("object_constr"),
                               gshapes.g_shape_Boolean, arg);
    else if (is_string(arg))
      ret = new_string_object(context, DEBUG_NAME("object_constr"),
                              gshapes.g_shape_String, arg);
    else
      ret = new_simple_object(context, DEBUG_NAME("object_constr"),
                              gshapes.g_shape_Object);
    GC_POP(arg);
  } else
    ret = new_simple_object(context, DEBUG_NAME("object_constr"),
                            gshapes.g_shape_Object);
  GC_PUSH(ret);
  set_a(context, ret);
  GC_POP(ret);
}

BUILTIN_FUNCTION(object_toString)
{
  set_a(context, gconsts.g_string_objtostr);
}

/*
 * property table
 */
/* prototype */
ObjBuiltinProp ObjectPrototype_builtin_props[] = {
  { "toString", object_toString,  0, ATTR_DE }
};
ObjDoubleProp  ObjectPrototype_double_props[] = {};
ObjGconstsProp ObjectPrototype_gconsts_props[] = {
  { "__property_map__", (JSValue *)&gpms.g_property_map_Object, ATTR_ALL },
};
/* constructor */
ObjBuiltinProp ObjectConstructor_builtin_props[] = {};
ObjDoubleProp  ObjectConstructor_double_props[] = {};
ObjGconstsProp ObjectConstructor_gconsts_props[] = {
  { "prototype", &gconsts.g_prototype_Object,  ATTR_ALL },
};
/* instance */
ObjBuiltinProp Object_builtin_props[] = {};
ObjDoubleProp  Object_double_props[] = {};
ObjGconstsProp Object_gconsts_props[] = {};
DEFINE_PROPERTY_TABLE_SIZES_PCI(Object);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
