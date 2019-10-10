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
 * constructor of a number 
 */
BUILTIN_FUNCTION(number_constr)
{
  JSValue rsv;

  builtin_prologue();
  rsv = new_number_object(context, DEBUG_NAME("number_constr"),
                          gshapes.g_shape_Number, FIXNUM_ZERO);
  GC_PUSH(rsv);
  /* set___proto___all(context, rsv, gconsts.g_number_proto); */
  if (na > 0)
    set_jsnumber_object_value(rsv, to_number(context, args[1]));
  set_a(context, rsv);
  GC_POP(rsv);
}

/*
 * Number(arg)
 *   If this is called without `new', it acts as a type conversion
 *   function to a number.
 */
BUILTIN_FUNCTION(number_constr_nonew)
{
  JSValue ret;
  
  builtin_prologue();
  ret = (na > 0)? to_number(context, args[1]): FIXNUM_ZERO;
  set_a(context, ret);
}

BUILTIN_FUNCTION(number_toString)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];
  if (is_number_object(rsv)) {
    if (na == 0 || args[1] == FIXNUM_TEN || args[1] == JS_UNDEFINED)
      set_a(context, number_to_string(get_jsnumber_object_value(rsv)));
    else {

      if(!is_fixnum(args[1])){
        LOG_ERR("args[1] is not a fixnum.");
        set_a(context, JS_UNDEFINED); }

      cint n = (int)fixnum_to_cint(args[1]);
      JSValue v = get_jsnumber_object_value(rsv);
      char map[36] = "0123456789abcdefghijklmnopqrstuvwxyz";

      int i, ff, acc;
      uint32_t numeric;
      double decimal;
      int nlen, dlen;
      char str[100];
      nlen = dlen = 0;


      /*
       * divides the number into numeric and decimal parts
       */
      if(is_fixnum(v)) {
        numeric = (int) fixnum_to_cint(v);
        decimal = 0.0;
      }else{
        numeric = (int) flonum_to_double(v);
        decimal = flonum_to_double(v) - numeric; }

      /*
       * makes a string for the numeric part in the reverse order
       */
      while(numeric >= n){
        str[nlen++] = map[numeric%n];
        numeric /= n; }
      str[nlen++] = map[numeric];

      /*
       * corrects the order of the numeric string
       */
      for(i=0; i<nlen/2; i++){
        ff = str[nlen-1-i];
        str[nlen-1-i] = str[i];
        str[i] = ff; }
      str[nlen++] = '.';

      /*
       * makes a string for the decimal part
       * accuracy is determined by the following expression
       */
      acc = (int)(48/((int)(log(n)/log(2))));
      while((decimal != 0.0) && (dlen < acc)){
        str[nlen+dlen++] = map[(int)(decimal*n)];
        decimal = decimal*n - (int)(decimal*n); }
      str[nlen+dlen++] = '\0';

      set_a(context, cstr_to_string(context, str));
    }

  } else if (is_number(rsv))
    set_a(context, number_to_string(rsv));
  else {

    /*
     * Type Error [FIXME]
     */ 
    LOG_EXIT("Number Instance's valueOf received not Number Instance\n");
  }
}

BUILTIN_FUNCTION(number_valueOf)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];
  if (is_number_object(rsv))
    set_a(context, get_jsnumber_object_value(rsv));
  else
    LOG_EXIT("Receiver of valueOf is not a Number instance\n");
}

/*
 * property table
 */

/* prototype */
ObjBuiltinProp NumberPrototype_builtin_props[] = {
  { "valueOf",        number_valueOf,       0, ATTR_DE },
  { "toString",       number_toString,      0, ATTR_DE },
};
ObjDoubleProp  NumberPrototype_double_props[] = {
};
ObjGconstsProp NumberPrototype_gconsts_props[] = {};
/* constructor */
ObjBuiltinProp NumberConstructor_builtin_props[] = {};
ObjDoubleProp  NumberConstructor_double_props[] = {
  { "MAX_VALUE", DBL_MAX,               ATTR_ALL },
  { "MIN_VALUE", DBL_MIN,               ATTR_ALL },
};
ObjGconstsProp NumberConstructor_gconsts_props[] = {
  { "prototype", &gconsts.g_prototype_Number,  ATTR_ALL },
  { "INFINITY",  &gconsts.g_flonum_infinity,   ATTR_ALL },
  { "NEGATIVE_INFINITY", &gconsts.g_flonum_negative_infinity, ATTR_ALL },
  { "NaN",       &gconsts.g_flonum_nan,        ATTR_ALL },
};
/* instance */
ObjBuiltinProp Number_builtin_props[] = {};
ObjDoubleProp  Number_double_props[] = {};
ObjGconstsProp Number_gconsts_props[] = {};
DEFINE_PROPERTY_TABLE_SIZES_PCI(Number);

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
