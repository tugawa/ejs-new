#include "prefix.h"
#define EXTERN extern
#include "header.h"

// loading a constant value
//

BUILTIN_FUNCTION(builtin_const_true)
{
  set_a(context, gconsts.g_string_true);
}

BUILTIN_FUNCTION(builtin_const_false)
{
  set_a(context, gconsts.g_string_false);
}

BUILTIN_FUNCTION(builtin_const_undefined)
{
  set_a(context, gconsts.g_string_undefined);
}

BUILTIN_FUNCTION(builtin_const_null)
{
  set_a(context, gconsts.g_string_null);
}

// identity function
//

BUILTIN_FUNCTION(builtin_identity)
{
  builtin_prologue();
  set_a(context, args[0]);
}

// functions for data conversion

#define TEMPSIZE 1000

BUILTIN_FUNCTION(builtin_fixnum_to_string)
{
  builtin_prologue();  
  set_a(context, fixnum_to_string(args[0]));
}

BUILTIN_FUNCTION(builtin_flonum_to_string)
{
  builtin_prologue();  
  set_a(context, fixnum_to_string(args[0]));
}

#if 0
BUILTIN_FUNCTION(builtin_string_to_index)
{
  builtin_prologue();
  set_a(context, string_to_index(args[0]));
}

BUILTIN_FUNCTION(dateProtoToString){}
BUILTIN_FUNCTION(dateProtoToDateString){}
BUILTIN_FUNCTION(dateProtoToTimeString){}
BUILTIN_FUNCTION(dateProtoToLocaleString){}
BUILTIN_FUNCTION(dateProtoToLocaleDateString){}
BUILTIN_FUNCTION(dateProtoToLocaleTimeString){}
BUILTIN_FUNCTION(dateProtoValueOf){}
BUILTIN_FUNCTION(dateProtoGetTime){}
BUILTIN_FUNCTION(dateProtoGetFullYear){}
BUILTIN_FUNCTION(dateProtoGetUTCFullYear){}
BUILTIN_FUNCTION(dateProtoGetMonth){}
BUILTIN_FUNCTION(dateProtoGetUTCMonth){}
BUILTIN_FUNCTION(dateProtoGetDate){}
BUILTIN_FUNCTION(dateProtoGetUTCDate){}
BUILTIN_FUNCTION(dateProtoGetDay){}
BUILTIN_FUNCTION(dateProtoGetUTCDay){}
BUILTIN_FUNCTION(dateProtoGetHours){}
BUILTIN_FUNCTION(dateProtoGetUTCHours){}
BUILTIN_FUNCTION(dateProtoGetMinutes){}
BUILTIN_FUNCTION(dateProtoGetUTCMinutes){}
BUILTIN_FUNCTION(dateProtoGetSeconds){}
BUILTIN_FUNCTION(dateProtoGetUTCSeconds){}
BUILTIN_FUNCTION(dateProtoGetMilliseconds){}
BUILTIN_FUNCTION(dateProtoGetUTCMilliseconds){}
BUILTIN_FUNCTION(dateProtoGetTimezoneOffset){}
BUILTIN_FUNCTION(dateProtoSetTime){}
BUILTIN_FUNCTION(dateProtoSetMillisecnods){}
BUILTIN_FUNCTION(dateProtoSetUTCMillisecnods){}
BUILTIN_FUNCTION(dateProtoSetSeconds){}
BUILTIN_FUNCTION(dateProtoSetUTCSeconds){}
BUILTIN_FUNCTION(dateProtoSetMinutes){}
BUILTIN_FUNCTION(dateProtoSetUTCMinutes){}
BUILTIN_FUNCTION(dateProtoSetHours){}
BUILTIN_FUNCTION(dateProtoSetUTCHours){}
BUILTIN_FUNCTION(dateProtoSetDate){}
BUILTIN_FUNCTION(dateProtoSetUTCDate){}
BUILTIN_FUNCTION(dateProtoSetMonth){}
BUILTIN_FUNCTION(dateProtoSetUTCMonth){}
BUILTIN_FUNCTION(dateProtoSetFullYear){}
BUILTIN_FUNCTION(dateProtoSetUTCFullYear){}
BUILTIN_FUNCTION(dateProtoToUTCString){}
#endif
