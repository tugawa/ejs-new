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
 * constructor of a boolean
 */
BUILTIN_FUNCTION(boolean_constr)
{
  JSValue rsv;

  builtin_prologue();  
  rsv = new_boolean_object(context, JS_TRUE);
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

/*
 * property table
 */

/* prototype */
ObjBuiltinProp BooleanPrototype_builtin_props[] = {
  { "valueOf",        boolean_valueOf,    0, ATTR_DE },
};
ObjDoubleProp  BooleanPrototype_doulbe_props[] = {};
ObjGconstsProp BooleanPrototype_gconsts_props[] = {};
/* constructor */
ObjBuiltinProp BooleanConstructor_builtin_props[] = {};
ObjDoubleProp  BooleanConstructor_doulbe_props[] = {};
ObjGconstsProp BooleanConstructor_gconsts_props[] = {
  { "prototype", &gconsts.g_prototype_Boolean,  0, ATTR_ALL },
};
/* instance */
ObjBuiltinProp Boolean_builtin_props[] = {};
ObjDoubleProp  Boolean_doulbe_props[] = {};
ObjGconstsProp Boolean_gconsts_props[] = {};
DEFINE_BUILTIN_TABLE_SIZES_PCI(Boolean);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
