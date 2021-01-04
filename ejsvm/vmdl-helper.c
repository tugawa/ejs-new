/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

#define EXTERN
#include "header.h"

JSValue get_global_helper(Context* context, JSValue str) {
  JSValue ret;

  ret = get_prop(context->global, str);
  if (ret == JS_EMPTY)
    LOG_EXIT("GETGLOBAL: %s not found\n", string_to_cstr(str));
  return ret;
}

#ifdef INLINE_CACHE
/* Implementation of the branch of GETPROP instruction where obj is
 * a JSObject and INLINE_CACHE is enabled.
 */
JSValue get_prop_object_inl_helper(Context* context, InlineCache* ic,
                                   JSValue obj, JSValue prop)
{
  JSValue ret;
  assert(ic->shape == NULL || ic->shape->n_extension_slots == 0);
  if (ic->shape == object_get_shape(obj) && ic->prop_name == prop)
    ret = remove_jsobject_tag(obj)->eprop[ic->index];
  else
    ret = get_object_prop(context, obj, prop, ic);
  return ret;
}
#endif /* INLINE_CACHE */

JSValue instanceof_helper(JSValue v1, JSValue v2) {
  JSValue p;

  p = get_prop(v2, gconsts.g_string_prototype);
  if (is_jsobject(p))
    while ((v1 = get_prop(v1, gconsts.g_string___proto__)) != JS_EMPTY)
      if (v1 == p)
        return JS_TRUE;
  return JS_FALSE;
}

JSValue getarguments_helper(Context* context, int link, Subscript index) {
  FunctionFrame* fr = get_lp(context);
  int i;
  for (i = 0; i < link; i++) {
    fr = fframe_prev(fr);
  }
  JSValue arguments = fframe_arguments(fr);
  return get_array_prop(context, arguments, int_to_fixnum(index));
}

JSValue getlocal_helper(Context* context, int link, Subscript index) {
  FunctionFrame* fr = get_lp(context);
  int i;
  for (i = 0; i < link; i++) {
    fr = fframe_prev(fr);
  }
  return fframe_locals_idx(fr, index);
}

Displacement localret_helper(Context* context, int pc) {
  Displacement disp;
  int newpc;
  JSValue v;
  if (context->lcall_stack < 1) {
    newpc = -1;
  } else {
    context->lcall_stack_ptr--;
    v = get_array_prop(context, context->lcall_stack,
		       cint_to_number(context,
                                      (cint) context->lcall_stack_ptr));
    newpc = number_to_cint(v);
  }
  disp = (Displacement) (newpc - pc);
  return disp;
}

void setarg_helper(Context* context, int link, Subscript index, JSValue v2) {
  FunctionFrame *fr;
  JSValue arguments;
  int i;

  fr = get_lp(context);
  for (i = 0; i < link; i++) fr = fframe_prev(fr);
  // assert(index < array_size(fframe_arguments(fr)));
  // array_body_index(fframe_arguments(fr), index) = v2;
  // TODO: optimize
  arguments = fframe_arguments(fr);
  set_array_prop(context, arguments, int_to_fixnum(index), v2);
}

void setfl_helper(Context* context, JSValue *regbase, int fp, int newfl) {
  int oldfl;

  oldfl = get_sp(context) - fp + 1;
  // printf("fp = %d, newfl = %d, fp + newfl = %d\n", fp, newfl, fp + newfl);
  if (fp + newfl > STACK_LIMIT)
    LOG_EXIT("register stack overflow\n");
  set_sp(context, fp + newfl - 1);
  while (++oldfl <= newfl)
    regbase[oldfl] = JS_UNDEFINED;
}

void setglobal_helper(Context* context, JSValue str, JSValue src) {
  set_prop(context, context->global, str, src, ATTR_NONE);
}

void setlocal_helper(Context* context, int link, Subscript index, JSValue v2) {
  FunctionFrame *fr;
  int i;

  fr = get_lp(context);
  for (i = 0; i < link; i++) fr = fframe_prev(fr);
  fframe_locals_idx(fr, index) = v2;
}

JSValue nextpropnameidx_helper(JSValue itr) {
  JSValue res = JS_UNDEFINED;
  iterator_get_next_propname(itr, &res);
  return res;
}

struct Strtol_rettype Strtol(char *s, int base){
  struct Strtol_rettype ret;
  ret.r0 = strtol(s, &ret.r1, base);
  return ret;
}

struct Strtod_rettype Strtod(char *s){
  struct Strtod_rettype ret;
  ret.r0 = strtod(s, &ret.r1);
  return ret;
}

struct GetProp_rettype GetProp(JSValue obj, JSValue name){
  struct GetProp_rettype ret;
  ret.r0 = get_prop(obj, name, &ret.r1);
  return ret;
}
/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
