/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#define FUNCTION_TABLE_LIMIT  (100)

EXTERN FunctionTable function_table[FUNCTION_TABLE_LIMIT];
EXTERN StrTable string_table;

/*
 * global constant objects
 */
EXTERN struct global_constant_objects {
  Shape *g_shape_Object;
  Shape *g_shape_Function;
  Shape *g_shape_Array;
  Shape *g_shape_Number;
  Shape *g_shape_String;
  Shape *g_shape_Boolean;

  JSValue g_prototype_Object;
  JSValue g_prototype_Function;
  JSValue g_prototype_Array;
  JSValue g_prototype_Number;
  JSValue g_prototype_String;
  JSValue g_prototype_Boolean;

  JSValue g_ctor_Object;
  JSValue g_ctor_Function;
  JSValue g_ctor_Array;
  JSValue g_ctor_Number;
  JSValue g_ctor_String;
  JSValue g_ctor_Boolean;
#ifdef USE_REGEXP
  JSValue g_ctor_RegExp;
#endif /* USE_REGEXP */

  JSValue g_fixnum_to_string;
  JSValue g_flonum_to_string;

  JSValue g_string___hidden_class__;
  JSValue g_string_prototype;
  JSValue g_string___proto__;
  JSValue g_string_tostring;
  JSValue g_string_valueof;
  JSValue g_string_boolean;
  JSValue g_string_number;
  JSValue g_string_object;
  JSValue g_string_string;
  JSValue g_string_true;
  JSValue g_string_false;
  JSValue g_string_null;
  JSValue g_string_undefined;
  JSValue g_string_length;
  JSValue g_string_objtostr;
  JSValue g_string_empty;
  JSValue g_string_comma;
  JSValue g_string_blank;

  JSValue g_flonum_infinity;
  JSValue g_flonum_negative_infinity;
  JSValue g_flonum_nan;

  JSValue g_boolean_true;
  JSValue g_boolean_false;

  JSValue g_null;
  JSValue g_undefined;

  JSValue g_global;
  JSValue g_math;
  JSValue g_regexp;
} gconsts;

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
