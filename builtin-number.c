#include "prefix.h"
#define EXTERN extern
#include "header.h"

// constructor of a number 
//
BUILTIN_FUNCTION(number_constr)
{
  JSValue rsv;

  builtin_prologue();  
  rsv = new_number(FIXNUM_ZERO);
  set_obj_prop(rsv, "__proto__", gconsts.g_number_proto, ATTR_ALL);
  if (na > 0)
    number_object_value(rsv) = to_number(context, args[1]);
  set_a(context, rsv);
}

// constructor of a number (not OBJECT)
//
BUILTIN_FUNCTION(number_constr_nonew)
{
  JSValue rsv, ret;
  
  builtin_prologue();
  rsv = args[0];
  if (na > 0)
    ret = to_number(context, args[1]);
  else
    ret = FIXNUM_ZERO;
  set_a(context, ret);
}

BUILTIN_FUNCTION(number_toString)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];
  if (is_number_object(rsv)) {
    if (na == 0)
      set_a(context, primitive_to_string(number_object_value(rsv)));

    // 引数を取り、１０進数指定か、または正しくない場合の処理
    else if (args[1] == JS_UNDEFINED || args[1] == FIXNUM_TEN)
      set_a(context, primitive_to_string(number_object_value(rsv)));

    // n進数文字列変換
    else {

      if(!get_tag(args[1]) == T_FIXNUM){
        LOG_ERR("args[1] is not a fixnum.");
        set_a(context, JS_UNDEFINED); }

      int n = (int)fixnum_to_int(args[1]);
      JSValue v = number_object_value(rsv);
      char map[36] = "0123456789abcdefghijklmnopqrstuvwxyz";

      int i, ff, acc;
      uint32_t numeric;
      double decimal;
      int nlen, dlen;
      char str[100];
      nlen = dlen = 0;


      // 小数点部と分離させる
      if(get_tag(v) == T_FIXNUM){
        numeric = (int)fixnum_to_int(v);
        decimal = 0.0;
      }else{
        numeric = (int)(flonum_to_double(v));
        decimal = flonum_to_double(v) - numeric; }

      // 整数部の展開
      while(numeric >= n){
        str[nlen++] = map[numeric%n];
        numeric /= n; }
      str[nlen++] = map[numeric];

      // 整数部をひっくり返す
      for(i=0; i<nlen/2; i++){
        ff = str[nlen-1-i];
        str[nlen-1-i] = str[i];
        str[i] = ff; }
      str[nlen++] = '.';

      // 小数部の展開
      // 小数点制度は以下の式により決定する (実装依存個所)
      acc = (int)(48/((int)(log(n)/log(2))));
      while((decimal != 0.0) && (dlen < acc)){
        str[nlen+dlen++] = map[(int)(decimal*n)];
        decimal = decimal*n - (int)(decimal*n); }
      str[nlen+dlen++] = '\0';

      set_a(context, cstr_to_string(strdup(str)));
    }

  }else if(is_number(rsv)){
    set_a(context, primitive_to_string(rsv));

  }else{

    // Type Error 例外処理をする [FIXME]
    LOG_EXIT("Number Instance's valueOf received not Number Instance\n");
  }
}

BUILTIN_FUNCTION(number_valueOf)
{
  JSValue rsv;

  builtin_prologue();
  rsv = args[0];
  if (is_number_object(rsv))
    set_a(context, number_object_value(rsv));
  else
    LOG_EXIT("Receiver of valueOf is not a Number instance\n");
}

ObjBuiltinProp number_funcs[] = {
  { "valueOf",        number_valueOf,       0, ATTR_DE },
  { "toString",       number_toString,      0, ATTR_DE },
  { NULL,             NULL,                 0, ATTR_DE }
};

ObjDoubleProp number_values[] = {
  { "MAX_VALUE", DBL_MAX,               ATTR_ALL },
  { "MIN_VALUE", DBL_MIN,               ATTR_ALL },
  { NULL,        0.0,                   ATTR_ALL }
};

void init_builtin_number(void)
{
  gconsts.g_number =
    new_builtin_with_constr(number_constr_nonew, number_constr, 1);
  gconsts.g_number_proto = new_number(FIXNUM_ZERO);
  set_obj_prop(gconsts.g_number, "prototype", gconsts.g_number_proto, ATTR_DE);
  set_obj_prop(gconsts.g_number, "INFINITY", gconsts.g_flonum_infinity, ATTR_ALL);
  set_obj_prop(gconsts.g_number, "NEGATIVE_INFINITY",
             gconsts.g_flonum_negative_infinity, ATTR_ALL);
  set_obj_prop(gconsts.g_number, "NaN", gconsts.g_flonum_nan, ATTR_ALL);
  set_obj_prop(gconsts.g_number_proto, "__proto__", gconsts.g_object_proto, ATTR_ALL);
  {
    ObjBuiltinProp *p = number_funcs;
    while (p->name != NULL) {
      set_obj_prop(gconsts.g_number_proto, p->name, new_builtin(p->fn, p->na), p->attr);
      p++;
    }
  }
  {
    ObjDoubleProp *p = number_values;
    while (p->name != NULL) {
      set_obj_prop(gconsts.g_number, p->name, double_to_flonum(p->value), p->attr);
      p++;
    }
  }
}

