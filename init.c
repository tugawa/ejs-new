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
 * initilaizes global constants
 */
void init_global_constants(void) {
  int i;
  for (i = 0; i < sizeof(gconsts)/sizeof(JSValue); i++)
    ((JSValue *)&gconsts)[i] = JS_UNDEFINED;

  /* string constants */
  gconsts.g_string___hidden_class__ = cstr_to_string(NULL, "__hidden_class__");
  gconsts.g_string_prototype = cstr_to_string(NULL, "prototype");
  gconsts.g_string___proto__ = cstr_to_string(NULL, "__proto__");
  gconsts.g_string_tostring  = cstr_to_string(NULL, "toString");
  gconsts.g_string_valueof   = cstr_to_string(NULL, "valueOf");
  gconsts.g_string_boolean   = cstr_to_string(NULL, "boolean");
  gconsts.g_string_number    = cstr_to_string(NULL, "number");
  gconsts.g_string_object    = cstr_to_string(NULL, "object");
  gconsts.g_string_string    = cstr_to_string(NULL, "string");
  gconsts.g_string_true      = cstr_to_string(NULL, "true");
  gconsts.g_string_false     = cstr_to_string(NULL, "false");
  gconsts.g_string_null      = cstr_to_string(NULL, "null");
  gconsts.g_string_undefined = cstr_to_string(NULL, "undefined");
  gconsts.g_string_length    = cstr_to_string(NULL, "length");
  gconsts.g_string_objtostr  = cstr_to_string(NULL, "[object Object]");
  gconsts.g_string_empty     = cstr_to_string(NULL, "");
  gconsts.g_string_comma     = cstr_to_string(NULL, ",");
  gconsts.g_string_blank     = cstr_to_string(NULL, " ");

  /* numbers */
  gconsts.g_flonum_infinity  = double_to_flonum(INFINITY);
  gconsts.g_flonum_negative_infinity = double_to_flonum(-INFINITY);
  gconsts.g_flonum_nan       = double_to_flonum(NAN);
}

/*
 * initilaizes global malloc-ed objects
 */
void init_global_malloc_objects(void) {
  gobjects.g_hidden_class_top =
    new_empty_hidden_class(NULL,             /* context */
                           HSIZE_BIG,        /* map size */
                           PSIZE_BIG,        /* embedded props */
                           0,                /* # of normal props */
                           0,                /* # of special props */
                           HTYPE_TRANSIT);   /* type */

  gobjects.g_hidden_class_0 =
    new_empty_hidden_class(NULL,              /* context */
                           HSIZE_NORMAL,      /* map size */
                           PSIZE_NORMAL,      /* embedded props */
                           0,                 /* # of normal props */
                           0,                 /* # of special props */
                           HTYPE_TRANSIT);    /* type */

  gobjects.g_hidden_class_array =
    new_empty_hidden_class(NULL,                 /* context */
                           ARRAY_NORMAL_PROPS,   /* map size */
                           ARRAY_EMBEDDED_PROPS, /* embedded props */
                           ARRAY_NORMAL_PROPS,   /* # of normal props */
                           ARRAY_SPECIAL_PROPS,  /* # of special props */
                           HTYPE_TRANSIT);       /* type */
  hash_put_with_attribute(hidden_map(gobjects.g_hidden_class_array),
                          gconsts.g_string_length,
                          ARRAY_PROP_INDEX_LENGTH,
                          ATTR_DDDE);

  gobjects.g_hidden_class_function =
    new_empty_hidden_class(NULL,                 /* context */
                           FUNC_NORMAL_PROPS,    /* map size */
                           FUNC_EMBEDDED_PROPS,  /* embedded props */
                           FUNC_NORMAL_PROPS,    /* # of normal props */
                           FUNC_SPECIAL_PROPS,   /* # of special props */
                           HTYPE_TRANSIT);       /* type */
  hash_put_with_attribute(hidden_map(gobjects.g_hidden_class_function),
                          gconsts.g_string_prototype,
                          FUNC_PROP_INDEX_PROTOTYPE,
                          ATTR_DDDE);

  gobjects.g_hidden_class_builtin =
    new_empty_hidden_class(NULL,                   /* context */
                           BUILTIN_NORMAL_PROPS,   /* map size */
                           BUILTIN_EMBEDDED_PROPS, /* embedded props */
                           BUILTIN_NORMAL_PROPS,   /* # of normal props */
                           BUILTIN_SPECIAL_PROPS,  /* # of special props */
                           HTYPE_TRANSIT);         /* type */
  hash_put_with_attribute(hidden_map(gobjects.g_hidden_class_builtin),
                          gconsts.g_string_prototype,
                          BUILTIN_PROP_INDEX_PROTOTYPE,
                          ATTR_DDDE);

  gobjects.g_hidden_class_boxed_number =
    new_empty_hidden_class(NULL,                    /* context */
                           BOXED_NORMAL_PROPS,      /* map size */
                           BOXED_EMBEDDED_PROPS,    /* embedded props */
                           BOXED_NORMAL_PROPS,      /* # of normal props */
                           BOXED_SPECIAL_PROPS,     /* # of special props */
                           HTYPE_TRANSIT);          /* type */
  gobjects.g_hidden_class_boxed_boolean =
    new_empty_hidden_class(NULL,                    /* context */
                           BOXED_NORMAL_PROPS,      /* map size */
                           BOXED_EMBEDDED_PROPS,    /* embedded props */
                           BOXED_NORMAL_PROPS,      /* # of normal props */
                           BOXED_SPECIAL_PROPS,     /* # of special props */
                           HTYPE_TRANSIT);          /* type */

  gobjects.g_hidden_class_boxed_string =
    new_empty_hidden_class(NULL,                    /* context */
                           BOXEDSTR_NORMAL_PROPS,   /* map size */
                           BOXEDSTR_EMBEDDED_PROPS, /* embedded props */
                           BOXEDSTR_NORMAL_PROPS,   /* # of normal props */
                           BOXED_SPECIAL_PROPS,     /* # of special props */
                           HTYPE_TRANSIT);
  hash_put_with_attribute(hidden_map(gobjects.g_hidden_class_boxed_string),
                          gconsts.g_string_length,
                          BOXEDSTR_PROP_INDEX_LENGTH,
                          ATTR_ALL);

#ifdef USE_REGEXP
  gobjects.g_hidden_class_regexp =
    new_empty_hidden_class(NULL,                /* context */
                           REX_NORMAL_PROPS,    /* map size */
                           REX_EMBEDDED_PROPS,  /* embedded props */
                           REX_NORMAL_PROPS,    /* # of normal props */
                           REX_SPECIAL_PROPS,   /* # of special props */
                           HTYPE_TRANSIT);      /* type */
#endif /* USE_REGEXP */
}

/*
 * initializes global objects
 * 2017/03/30: moved from init_global
 */
void init_global_objects(void) {
  /*
   * g_object_proto: Object.prototype, which is set to __proto__ property
   *                 when an object is created.
   *   Object.prototype === {}.__proto__
   *   Object.prototype.__proto__ === null
   *   Object.prototype.prototype is not defined.
   *                   (Object.prototype is not used as a constructor)
   */
  gconsts.g_object_proto =
    new_object_with_class(NULL, gobjects.g_hidden_class_top);
  hidden_proto(gobjects.g_hidden_class_0) = gconsts.g_object_proto;
  /*
   * g_blobal: The global object, which contains global variables.
   * g_math:   The "Math" object.
   */
  /* TODO: give a special hidden class with large embedded properties */
  gconsts.g_global = new_normal_object(NULL);
  gconsts.g_math = new_normal_object(NULL);

#ifdef HIDDEN_DEBUG
  print_hidden_class("g_object_proto",
                     obj_hidden_class(gconsts.g_object_proto));
  print_hidden_class("g_global", obj_hidden_class(gconsts.g_global));
  print_hidden_class("g_math", obj_hidden_class(gconsts.g_math));
#endif /* HIDDEN_DEBUG */

}

/*
 * initializes builtin
 */
void init_builtin(Context *ctx) {
  init_builtin_function(ctx);  /* must be called first */
  init_builtin_object(ctx);
  init_builtin_array(ctx);
  init_builtin_number(ctx);
  init_builtin_string(ctx);
  init_builtin_boolean(ctx);
  init_builtin_math(ctx);
#ifdef USE_REGEXP
  init_builtin_regexp(ctx);
#endif /* USE_REGEXP */

  /* calls init_buitin_global after gconsts is properly set up */
  init_builtin_global(ctx);
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
