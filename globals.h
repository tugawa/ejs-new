/*
   globals.h

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

#define FUNCTION_TABLE_LIMIT  (100)

EXTERN FunctionTable function_table[FUNCTION_TABLE_LIMIT];
EXTERN StrTable string_table;

// global constant objects
//
EXTERN struct global_constant_objects {
  // JSValue g_const_true;
  // JSValue g_const_false;
  // JSValue g_const_undefined;
  // JSValue g_const_null;
  JSValue g_identity;
  JSValue g_object;
  JSValue g_object_proto;
  JSValue g_array;
  JSValue g_array_proto;
  JSValue g_function_proto;
  JSValue g_number;
  JSValue g_number_proto;
  JSValue g_string;
  JSValue g_string_proto;
  JSValue g_boolean;
  JSValue g_boolean_proto;
  JSValue g_date;
  JSValue g_date_proto;
  JSValue g_regexp;
  JSValue g_regexp_proto;
  JSValue g_fixnum_to_string;
  JSValue g_flonum_to_string;
//  JSValue g_string_to_index;

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

  JSValue g_global;
  JSValue g_math;
} gconsts;

EXTERN struct global_malloced_objects {
#ifdef HIDDEN_CLASS
  HiddenClass *g_hidden_class_0;
#endif
} gobjects;
