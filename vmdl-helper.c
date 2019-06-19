#define EXTERN
#include "header.h"

JSValue initialize_new_object(Context* context, JSValue con, JSValue o) {
    JSValue p;
    get_prop(con, gconsts.g_string_prototype, &p);
    if (!is_object(p)) p = gconsts.g_object_proto;
    set___proto___all(context, o, p);
    return o;
}