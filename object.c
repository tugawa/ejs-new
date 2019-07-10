/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

/*
 * There are two functions and a macro that are used in allocating an
 * object of a specified type, namely allocate_xxx, make_xxx, and new_xxx.
 * For example, in allocating a function, allocate_function, make_function,
 * and new_function are used.  Each of them has the following role.
 *
 * allocate_xxx : (function)
 *   This allocates a memory for an xxx and sets an appropriate object tag
 *   in its header.
 *
 * make_xxx : (macro)
 *   This only calls allocate_xxx, puts pointer tag (T_GENERIC), and
 *   returns a JSValue data.
 *
 * new_xxx : (function)
 *   This first calls make_xxx and sets various values within the returned
 *   object.  set_object_members is called in this function.
 */

#include "prefix.h"
#define EXTERN
#include "header.h"

#define PROP_REALLOC_THRESHOLD (0.75)

#define prop_overflow(o)                                                \
  (obj_n_props(o) > (obj_limit_props(o) * PROP_REALLOC_THRESHOLD))

#define sign(x) ((x) > 0? 1: -1)

#define hash_put(map, name, retv, attr)                                 \
  ((hash_put_with_attribute((map),(name),(retv),(attr)) == HASH_PUT_SUCCESS)? \
   (int)(retv): (-1))

#ifdef HIDDEN_CLASS
static HiddenClass *hclass;
int n_hc;
int n_enter_hc;
int n_exit_hc;

#ifdef PROFILE
static int profile_object_count;
#endif /* PROFILE */
#endif

/*
 * obtains the index of a property of an Object
 * If the specified property does not exist, returns -1.
 *
 * Note that if HIDDEN_CLASS is defined and hash_get_with_attribute returns
 * HASH_GET_SUCCESS, the hidden map in the object contains either the index
 * of JSValue array (as a fixnum) or the pointer to the next hidden class.
 * These two cases can be distinguished by investigating the ``transition
 * bit'' of the obtained attribute.  For the latter case, the pointer to
 * the next hidden class is temporary assigned to a static variable named
 * ``hclass''.
 */
static int prop_index(JSValue obj, JSValue name) {
  HashData retv;
  int result;
#ifdef HIDDEN_CLASS
  Attribute att;
#endif

  ASSERT_OBJECT(obj);

#ifdef HIDDEN_CLASS
  result =
    hash_get_with_attribute(obj_hidden_class_map(obj), name, &retv, &att);
  /* printf("in prop_index: result = %d\n", result); */
  if (result == HASH_GET_FAILED) {
    hclass = NULL;
    /* printf("in prop_index: hclass = NULL, returning -1\n"); */
    return -1;
  }
  if (is_transition(att)) {
    hclass = (HiddenClass *)retv;
    /* printf("in prop_index: is_transition, returning -1\n"); */
    return -1;
  } else {
    /* printf("prop_index is returning %d\n", (int)retv); */
    return (int)retv;
  }
#else
  /* print_hash_table(obj_map(obj)); */
  result = hash_get(obj_map(obj), name, &retv);
  if (result == HASH_GET_FAILED) return -1;
  /* printf("prop_index: "); simple_print(retv); putchar('\n'); */
  return (int)retv;
#endif
}

/*
 * obtains the property value of the key ``name'', stores it to *ret
 * and returns SUCCESS or FAIL
 * This function does not follow the prototype chain.
 */
int get_prop(JSValue obj, JSValue name, JSValue *ret) {
  int index;

  /*
   * printf("get_prop: obj = %016lx, prop = %s\n", obj, string_to_cstr(name));
   * simple_print(name); putchar('\n');
   */
  index = prop_index(obj, name);
  /* printf("get_prop: index = %d\n", index); */
  if (index == -1) return FAIL;
  /* simple_print(obj_prop_index(obj, index)); putchar('\n'); */
#ifdef EMBED_PROP
  *ret = get_obj_prop_index(obj, index);
#else /* EMBED_PROP */
  *ret = obj_prop_index(obj, index);
#endif /* EMBED_PROP */
  return SUCCESS;
}

/*
 * obtains the property from an object by following the prototype chain
 * (if necessary)
 *   o: object
 *   p: property, which is a string
 */
JSValue get_prop_prototype_chain(JSValue o, JSValue p) {
  JSValue ret;
  extern JSValue prototype_object(JSValue);

  /*
   * printf("get_prop_prototype_chain: o = "); simple_print(o); printf("\n");
   * printf("get_prop_prototype_chain: p = "); simple_print(p); printf("\n");
   * printf("get_prop_prototype_chain: prop = %s, obj = %016lx\n",
   *         string_to_cstr(p), o);
   * printf("Object.__proto__ = %016lx\n", gconsts.g_object_proto);
   */
  do {
    if (get_prop(o, p, &ret) == SUCCESS) return ret;
  } while (get___proto__(o, &o) == SUCCESS);
  /* is it necessary to search in the Object's prototype? */
  return JS_UNDEFINED;
}

/*
 * obtains object's property
 *   o: object (but not an array)
 *   p: property (number / string / other type)
 * It is not necessary to check the type of `o'.
 */
JSValue get_object_prop(Context *ctx, JSValue o, JSValue p) {
  /*
   * if (p is not a string) p = to_string(p);
   *   returns the value regsitered under the property p
   * }
   */
  /* printf("get_object_prop, o = %016lx, p = %016lx\n", o, p); */
  if (!is_string(p)) {
    GC_PUSH(o);
    p = to_string(ctx, p);
    GC_POP(o);
  }
  return get_prop_prototype_chain(o, p);
}

/*
 * determines whether an object has a property by following the prototype chain
 * if the object has the property, TRUE, else FALSE
 *  o: object
 *  p: property, which is a string
 */
int has_prop_prototype_chain(JSValue o, JSValue p) {
  JSValue ret;
  extern JSValue prototype_object(JSValue);
  do {
    if (get_prop(o, p, &ret) == SUCCESS) return TRUE;
  } while (get_prop(o, gconsts.g_string___proto__, &o) == SUCCESS);
  /* is it necessary to search in the Object's prototype? */
  return FALSE;
}

/*
 * determines whether a[n] exists or not
 * if a[n] is not an element of body (an C array) of a, search properties of a
 *  a: array
 *  n: subscript
 */
int has_array_element(JSValue a, cint n) {
  if (!is_array(a)) return FALSE;
  if (n < 0 || array_length(a) <= n) return FALSE;
  /* in body of a */
  /* is it ok that a[n] (0 <= n < len) always exists? */
  if (n < array_size(a)) return TRUE;
  /* in property of a */
  return has_prop_prototype_chain(a, cint_to_string(n));
}

/*
 *  obtains array's property
 *    a: array
 *    p: property (number / string / other type)
 *  It is not necessary to check the type of `a'.
 */
JSValue get_array_prop(Context *ctx, JSValue a, JSValue p) {
  /*
   * if (p is a number) {
   *   if (p is within the range of an subscript of an array)
   *     returns the p-th element of a
   *   else {
   *     p = number_to_string(idx);
   *     returns the value regsitered under the property p
   *   }
   * } else {
   *   if (p is not a string) p = to_string(p);
   *   s = string_to_number(p);
   *   if (s is within the range of an subscript of an array)
   *     returns the s-th element of a
   *   else
   *     returns the value regsitered under the property p
   * }
   *
   * I am afraid that the above definition is incorrect.
   *
   * } else if (p is a string) {
   *   s = string_to_number(p);
   *   if (s is within the range of an subscript of an array)
   *     returns the s-th element of a
   *   else
   *     returns the value regsitered under the property p
   * } else {
   *   p = to_string(p);
   *   returns the value regsitered under the property p
   * }
   *
   */

  if (is_fixnum(p)) {
    cint n;
    n = fixnum_to_cint(p);
    if (0 <= n && n < array_size(a)) {
      return (n < array_length(a))? array_body_index(a, n): JS_UNDEFINED;
    }
    p = fixnum_to_string(p);
    return get_prop_prototype_chain(a, p);
  }

  if (!is_string(p)) {
    GC_PUSH(a);
    p = to_string(ctx, p);
    GC_POP(a);
  }
  /* assert: is_string(p) == true */
  {
    JSValue num;
    cint n;
    num = string_to_number(p);
    if (is_fixnum(num)) {
      n = fixnum_to_cint(num);
      if (0 <= n && n < array_size(a)) {
        return (n < array_length(a))? array_body_index(a, n): JS_UNDEFINED;
      }
    }
    return get_prop_prototype_chain(a, p);
  }
}

#ifdef RICH_HIDDEN_CLASS
#ifdef EMBED_PROP
static void expand_overflow_props(Context *ctx, JSValue obj,
                                  int old_size, int new_size)
{
  JSValue *props;
  GC_PUSH(obj);
  if (old_size == 0) {
    props = allocate_prop_table(new_size);
    props[0] = obj_ovf_props(obj, obj_hidden_class(obj));
  } else {
    HiddenClass *hc = obj_hidden_class(obj);
    props = reallocate_prop_table(ctx, (JSValue *) obj_ovf_props(obj, hc),
                                  old_size, new_size);
  }
  obj_ovf_props(obj, obj_hidden_class(obj)) = (JSValue) props;
  GC_POP(obj);
}

static int compute_new_limit_props(int n_embedded, int n_limit, int n_prop)
{
  if (n_limit == 0) {
    if (n_prop > n_embedded)
      return 1 + PSIZE_DELTA;
  } else {
    if (n_prop > n_embedded + n_limit - 1) {
      CHECK_INCREASE_PROPERTY(n_embedded + n_limit - 1);
      return n_limit + PSIZE_DELTA;
    }
  }
  return n_limit;
}
#endif /* EMBED_PROP */

/*
 * sets an object's property value with its attribute
 */
int set_prop_with_attribute(Context *ctx, JSValue obj, JSValue name,
                            JSValue v, Attribute attr)
{
  int index;

  index = prop_index(obj, name);
  if (index == -1) {
    HiddenClass *hc = obj_hidden_class(obj);
    int prev_limit = hidden_n_limit_props(hc);
    index = hidden_n_props(hc);
    if (hidden_htype(hc) == HTYPE_TRANSIT) {
      HiddenClass *nhc = hclass;
      if (nhc == NULL) {
        int ret;
        GC_PUSH3(obj, name, v);
        nhc = new_hidden_class(ctx, hc);
        GC_POP3(v, name, obj);
        hc = obj_hidden_class(obj);
        ret = hash_put_with_attribute(hidden_map(nhc), name, index, attr);
        if (ret != HASH_PUT_SUCCESS)
          return FAIL;
        hidden_n_entries(nhc)++;
        hash_put_with_attribute(hidden_map(hc), name, (HashData)nhc,
                                ATTR_NONE | ATTR_TRANSITION);
        hidden_n_entries(hc)++;
      }
      if (prev_limit < hidden_n_limit_props(nhc)) {
#ifdef EMBED_PROP
        GC_PUSH3(obj, v, nhc);
        expand_overflow_props(ctx, obj, prev_limit, hidden_n_limit_props(nhc));
        GC_POP3(nhc, v, obj);
#else /* EMBED_PROP */
        JSValue * props;
        GC_PUSH3(obj, v, nhc);
        props = reallocate_prop_table(ctx, obj_prop(obj), prev_limit,
                                      hidden_n_limit_props(nhc));
        GC_POP3(nhc, v, obj);
        obj_prop(obj) = props;
#endif /* EMBED_PROP */
      }
      hidden_n_exit(obj_hidden_class(obj))++;
      n_exit_hc++;
      obj_hidden_class(obj) = nhc;
      hidden_n_enter(nhc)++;
      n_enter_hc++;
#ifdef PROFILE
      if (obj_profile_id(obj))
        hidden_n_profile_enter(nhc)++;
#endif /* PROFILE */
    } else { /* hidden_htype(hc) == HTYPE_GROW */
      int ret;
#ifdef EMBED_PROP
      int new_limit = compute_new_limit_props(hidden_n_embedded_props(hc),
                                              prev_limit,
                                              hidden_n_props(hc) + 1);
      if (new_limit > prev_limit) {
        hidden_n_limit_props(hc) = new_limit;
        GC_PUSH4(obj, name, v, hc);
        expand_overflow_props(ctx, obj, prev_limit, new_limit);
        GC_POP4(hc, v, name, obj);
      }
#else /* EMBED_PROP */
      if (prev_limit <= hidden_n_props(hc)) {
        JSValue *props;
        int new_limit;
        CHECK_INCREASE_PROPERTY(hidden_n_props(hc));
        new_limit = hidden_n_props(hc) + PSIZE_DELTA;
        hidden_n_limit_props(hc) = new_limit;
        GC_PUSH2(obj, v);
        props = reallocate_prop_table(ctx, obj_prop(obj),
                                      hidden_n_props(hc), new_limit);
        GC_POP2(v, obj);
        obj_prop(obj) = props;
      }
#endif /* EMBED_PROP */
      ret = hash_put_with_attribute(hidden_map(hc), name, index, attr);
      hidden_n_props(hc)++;
      if (ret != HASH_PUT_SUCCESS)
        return FAIL;
    }
  }
#ifdef EMBED_PROP
  set_obj_prop_index(obj, index, v);
#else /* EMBED_PROP */
  obj_prop_index(obj, index) = v;
#endif /* EMBED_PROP */
  return SUCCESS;
}
#else /* RICH_HIDDEN_CLASS */
/*
 * sets an object's property value with its attribute
 */
int set_prop_with_attribute(Context *ctx, JSValue obj, JSValue name, JSValue v, Attribute attr) {
  uint64_t retv, newsize;
  int index, r;
#ifdef HIDDEN_CLASS
  HiddenClass *nexth, *oh;
#endif /* HIDDEN_CLASS */

  /*
   * printf("set_prop_with_attribute: obj = %p, name = %s, v = %p\n",
   *        obj, string_to_cstr(name), v);
   */
  index = prop_index(obj, name);
  /* printf("set_prop_with_index: index = %d\n", index); */
  if (index == -1) {
    /* The specified property is not registered in the hash table. */
    retv = obj_n_props(obj);
    if (retv >= obj_limit_props(obj)) {
      int newsize;
      CHECK_INCREASE_PROPERTY(retv);
      newsize = retv + PSIZE_DELTA;
      GC_PUSH3(obj, name, v);
      obj_prop(obj) = reallocate_prop_table(ctx, obj_prop(obj), retv, newsize);
      GC_POP3(v, name, obj);
      obj_limit_props(obj) = newsize;
    }
    index = (cint)retv;
    /* printf("set_prop_with_index: index = %d\n", index); */
#ifdef HIDDEN_CLASS
    oh = obj_hidden_class(obj);    /* source hidden class */
    /*
     * if (hidden_htype(oh) == HTYPE_TRANSIT)
     *   print_hidden_class("transit_hidden_class: from (TRANSIT)", oh);
     * else
     *   print_hidden_class("transit_hidden_class: from (GROW)", oh);
     */
    if (hidden_htype(oh) == HTYPE_TRANSIT) {
      nexth = hclass;    /* NULL or pointer to the next hidden class */
      if (nexth == NULL) {
        /* printf("transit_hidden_class: making the next hidden class\n"); */
        GC_PUSH4(obj, name, v, oh);
        nexth = new_hidden_class(ctx, oh);
        GC_POP4(oh, v, name, obj);
        /*
         * print_hidden_class("transit_hidden_class: to (before put)", nexth);
         */
        GC_PUSH2(nexth, oh);
        r = hash_put_with_attribute(hidden_map(nexth), name, index, attr);
        if (r != HASH_PUT_SUCCESS) {
          GC_POP2(oh, nexth);
          return FAIL;
        }
        hidden_n_entries(nexth)++;
        hash_put_with_attribute(hidden_map(oh), name, (HashData)nexth,
                                ATTR_NONE | ATTR_TRANSITION);
        GC_POP2(oh, nexth);
        hidden_n_entries(oh)++;
      }
      hidden_n_exit(obj_hidden_class(obj))++;
      n_exit_hc++;
      obj_hidden_class(obj) = nexth;
      hidden_n_enter(nexth)++;
      n_enter_hc++;
#ifdef PROFILE
      if (obj_profile_id(obj))
        hidden_n_profile_enter(nexth)++;
#endif /* PROFILE */
    } else {                  /* hidden_htype(oh) == HTYPE_GROW */
      nexth = oh;
      GC_PUSH(nexth);
      r = hash_put_with_attribute(hidden_map(nexth), name, index, attr);
      GC_POP(nexth);
      if (r != HASH_PUT_SUCCESS) return FAIL;
      hidden_n_entries(nexth)++;
    }
    /* print_hidden_class("transit_hidden_class: to (after put)", nexth); */
#else /* HIDDEN_CLASS */
    GC_PUSH(obj);
    r = hash_put_with_attribute(obj_map(obj), name, index, attr);
    GC_POP(obj);
    if (r != HASH_PUT_SUCCESS) return FAIL;
#endif /* HIDDEN_CLASS */
    (obj_n_props(obj)) = index + 1;
  }
  obj_prop_index(obj, index) = v;
  return SUCCESS;
}
#endif /* RICH_HIDDEN_CLASS */

/*
 * sets object's property
 *   o: object (but not an array)
 *   p: property (number / string / other type)
 *   v: value to be set
 * It is not necessary to check the type of `o'.
 */
int set_object_prop(Context *ctx, JSValue o, JSValue p, JSValue v) {
  if (!is_string(p)) {
    GC_PUSH2(o, v);
    p = to_string(ctx, p);
    GC_POP2(v, o);
  }
  /* printf("set_object_prop: "); print_value_verbose(ctx, p); printf("\n"); */
  return set_prop_none(ctx, o, p, v);
}

/*
 *  set_array_index_value
 *  a is an array and n is a subscript where n >= 0.
 *  This function is called when
 *    a[n] <- v (in this case, setlength is False)
 *    or
 *    a.length <- n + 1 (in this case, setlength is True)
 *
 *  In the latter case, it is not necessary to do a[n] <- v, but
 *  it may be necessary to shrink the array.
 *
 *  returns
 *    SUCCESS: the above assignment is performed
 *    FAIL   : the above assignment has not been done yet because n is
 *             outside, but expanding array has been done if necessary
 */
int set_array_index_value(Context *ctx, JSValue a, cint n, JSValue v,
                          int setlength) {
  cint len, size, adatamax;
  int i;

  len = array_length(a);
  size = array_size(a);
  adatamax = (size <= ASIZE_LIMIT)? ASIZE_LIMIT: size;
  /* printf("set_array_index_value: n = %d\n", n); */
  if (n < adatamax) {
    if (size <= n) {
      /*
       * It is necessary to expand the array, but since n is less than
       *  ASIZE_LIMIT, it is possible to expand the array data.
       */
      cint newsize;
      while ((newsize = increase_asize(size)) <= n) size = newsize;
      GC_PUSH2(a, v);
      reallocate_array_data(ctx, a, newsize);
      GC_POP2(v, a);
    }
    /*
     * If len <= n, expands the array.  It should be noted that
     * if len >= n, this for loop does nothing
     */
    for (i = len; i <= n; i++) /* i < n? */
      array_body_index(a, i) = JS_UNDEFINED;
  } else {
    /*
     * Since n is outside of the range of array data, stores the
     * value into the hash table of the array.
     */
    if (size < ASIZE_LIMIT) {
      /* The array data is not fully expanded, so we expand it */
      GC_PUSH2(a, v);
      reallocate_array_data(ctx, a, ASIZE_LIMIT);
      GC_POP2(v, a);
      for (i = len; i < ASIZE_LIMIT; i++)
        array_body_index(a, i) = JS_UNDEFINED;
      adatamax = ASIZE_LIMIT;
    }
  }
  if (len <= n || setlength == TRUE) {
    array_length(a) = n + 1;
    GC_PUSH2(a, v);
    set_prop_none(ctx, a, gconsts.g_string_length, cint_to_fixnum(n + 1));
    GC_POP2(v, a);
  }
  if (setlength == TRUE && n < len && adatamax <= len) {
    remove_array_props(a, adatamax, len);
  }
  if (n < adatamax && setlength == FALSE) {
    array_body_index(a, n) = v;
    return SUCCESS;
  }
  return FAIL;
}

/*
 * sets array's property
 *   a: array
 *   p: property (number / string / other type)
 *   v: value to be set
 * It is not necessary to check the type of `a'.
 */
int set_array_prop(Context *ctx, JSValue a, JSValue p, JSValue v) {
  if (is_fixnum(p)) {
    cint n;

    n = fixnum_to_cint(p);
    if (0 <= n && n < MAX_ARRAY_LENGTH) {
      GC_PUSH3(a, p, v);
      if (set_array_index_value(ctx, a, n, v, FALSE) == SUCCESS) {
        GC_POP3(v, p, a);
        return SUCCESS;
      }
      GC_POP3(v, p, a);
    }
    p = fixnum_to_string(p);
    return set_object_prop(ctx, a, p, v);
  }

  if (!is_string(p)) {
    GC_PUSH2(a, v);
    p = to_string(ctx, p);
    GC_POP2(v, a);
  }

  {  /* assert: p == string */
    JSValue num;
    cint n;

    num = string_to_number(p);
    if (is_fixnum(num)) {
      n = fixnum_to_cint(num);
      if (0 <= n && n < MAX_ARRAY_LENGTH) {
        GC_PUSH3(a, p, v);
        if (set_array_index_value(ctx, a, n, v, FALSE) == SUCCESS) {
          GC_POP3(v, p, a);
          return SUCCESS;
        }
        GC_POP3(v, p, a);
      }
      return set_object_prop(ctx, a, p, v);
    }
    if (p == gconsts.g_string_length && is_fixnum(v)) {
      cint n;
      n = fixnum_to_cint(v);
      if (0 <= n && n < MAX_ARRAY_LENGTH) {
        /*
         * The property name is "length" and the given value is a fixnum.
         * Thus, expands / shrinks the array.
         */
        GC_PUSH3(a, p, v);
        if (set_array_index_value(ctx, a, n - 1, JS_UNDEFINED, TRUE)
            == SUCCESS) {
          GC_POP3(v, p, a);
          return SUCCESS;
        }
        GC_POP3(v, p, a);
      }
    }
    return set_object_prop(ctx, a, p, v);
  }
}

/*
 * removes array data whose subscript is between `from' and `to'
 * that are stored in the property table.
 * implemented tentatively
 */
void remove_array_props(JSValue a, cint from, cint to) {
  /* printf("%d-%d\n",from,to); */
  for (; from < to ; from++)
    delete_array_element(a, from);
}

/*
 * delete the hash cell with key and the property of the object
 * NOTE:
 *   The function does not reallocate (shorten) the prop array of the object.
 *   It must be improved.
 * NOTE:
 *   When using hidden class, this function does not delete a property
 *   of an object but merely sets the corresponding property as JS_UNDEFINED,
 */
int delete_object_prop(JSValue obj, HashKey key) {
  int index;

  if (!is_object(obj)) return FAIL;

  /* Set corresponding property as JS_UNDEFINED */
  index = prop_index(obj, key);
  if (index == - 1) return FAIL;
#ifdef EMBED_PROP
  set_obj_prop_index(obj, index, JS_UNDEFINED);
#else /* EMBED_PROP */
  obj_prop_index(obj, index) = JS_UNDEFINED;
#endif /* EMBED_PROP */

  /* Delete map */
#ifdef HIDDEN_CLASS
  /*
   * LOG("To delete properties of an object is not completely implemented when using hidden class (instead set a property to undefined)\n");
   */
#else
  /* Free the HashCell */
  if(hash_delete(obj_map(obj), key) == HASH_GET_FAILED) return FAIL;
#endif
  return SUCCESS;
}

/*
 * delete a[n]
 * Note that this function does not change a.length
 */
int delete_array_element(JSValue a, cint n) {
  if (n < array_size(a)) {
    array_body_index(a, n) = JS_UNDEFINED;
    return SUCCESS;
  }
  return delete_object_prop(a, cint_to_string(n));
}

/*
 * obtains the next property name in an iterator
 * iter:Iterator
 */
int iterator_get_next_propname(JSValue iter, JSValue *name) {
  int size = iterator_size(iter);
  int index = iterator_index(iter);
  if(index < size) {
    *name = iterator_body_index(iter,index++);
    iterator_index(iter) = index;
    return SUCCESS;
  }else{
    *name = JS_UNDEFINED;
    return FAIL;
  }
}

#ifdef USE_REGEXP
/*
 * sets a regexp's members and makes an Oniguruma's regexp object
 */
int set_regexp_members(Context *ctx, JSValue re, char *pat, int flag) {
  OnigOptionType opt;
  OnigErrorInfo err;
  char *e;

  /*
   * The original code in SSJSVM_codeloader.c used ststrdup function,
   * which is defined in hash.c.  But I don't know the reason why
   * ststrdup is used instead of the standard strdup function.
   *
   * regexp_pattern(re) = ststrdup(str);
   */
  regexp_pattern(re) = strdup(pat);

  opt = ONIG_OPTION_NONE;

  if (flag & F_REGEXP_GLOBAL) {
    regexp_global(re) = true;
    set_obj_cstr_prop(ctx, re, "global", JS_TRUE, ATTR_ALL);
  } else
    set_obj_cstr_prop(ctx, re, "global", JS_FALSE, ATTR_ALL);

  if (flag & F_REGEXP_IGNORE) {
    opt |= ONIG_OPTION_IGNORECASE;
    regexp_ignorecase(re) =  true;
    set_obj_cstr_prop(ctx, re, "ignoreCase", JS_TRUE, ATTR_ALL);
  } else
    set_obj_cstr_prop(ctx, re, "ignoreCase", JS_FALSE, ATTR_ALL);

  if (flag & F_REGEXP_MULTILINE) {
    opt |= ONIG_OPTION_MULTILINE;
    regexp_multiline(re) = true;
    set_obj_cstr_prop(ctx, re, "multiline", JS_TRUE, ATTR_ALL);
  } else
    set_obj_cstr_prop(ctx, re, "multiline", JS_FALSE, ATTR_ALL);

  e = pat + strlen(pat);
  if (onig_new(&(regexp_reg(re)), (OnigUChar *)pat, (OnigUChar *)e, opt,
               ONIG_ENCODING_ASCII, ONIG_SYNTAX_DEFAULT, &err) == ONIG_NORMAL)
    return SUCCESS;
  else
    return FAIL;
}

/*
 * returns a flag value from a ragexp objext
 */
int regexp_flag(JSValue re) {
  int flag;

  flag = 0;
  if (regexp_global(re)) flag |= F_REGEXP_GLOBAL;
  if (regexp_ignorecase(re)) flag |= F_REGEXP_IGNORE;
  if (regexp_multiline(re)) flag |= F_REGEXP_MULTILINE;
  return flag;
}
#endif

/*
 * sets object's member values.
 *   The sizes of the map and the property table of Takada VM are
 *   INITIAL_HASH_SIZE (100) and INITIAL_PROPTABLE_SIZE (100), respectively.
 *   But these sizes seems to be too large for a normal object (e.g.,
 *   non-builtin object).
 *   Thus, we changed the definition of set_object_members so that their
 *   sizes are large for a builtin object and small for another object.
 */

/*
 * sets members relating to property table
 *   hsize: size of the hash table
 *   psize: size of the property table
 *
 * If HIDDEN_CLASS is defined, hsize == 0 means that g_hidden_class_0
 * should be used instead of allocating a new hidden class.
 */
#ifdef ARRAY_EMBED_PROP
static void set_object_members_with_class(Object *p, HiddenClass *hc)
{
  size_t i;
  size_t n_embedded = hidden_n_embedded_props(hc);
#ifdef PROFILE
  if (logflag()) {
    p->profile_id = ++profile_object_count;
    hidden_n_profile_enter(hc)++;
  } else
    p->profile_id = 0;
#endif /* PROFILE */
  p->klass = hc;
  hidden_n_enter(p->klass)++;
  n_enter_hc++;
  for (i = 0; i < n_embedded; i++)
    p->eprop[i] = JS_UNDEFINED;
}

static void set_object_members(Object *p, int hsize, int psize)
{
  if (hsize == 0)
    set_object_members_with_class(p, gobjects.g_hidden_class_0);
  else {
    HiddenClass *hc = new_empty_hidden_class(NULL, hsize, psize,
                                             0, 0, HTYPE_GROW);
    set_object_members_with_class(p, hc);
  }
}
#else /* ARRAY_EMBED_PROP */
static void set_object_members(Object *p, int hsize, int psize)
{
#ifdef PROFILE
  if (logflag())
    p->profile_id = ++profile_object_count;
  else
    p->profile_id = 0;
#endif /* PROFILE */
#ifdef HIDDEN_CLASS
#ifdef RICH_HIDDEN_CLASS
  p->klass = ((hsize == 0)?
              gobjects.g_hidden_class_0:
              new_empty_hidden_class(NULL, hsize, psize, HTYPE_GROW));
#else /* RICH_HIDDEN_CLASS */
  p->klass = ((hsize == 0)?
              gobjects.g_hidden_class_0:
              new_empty_hidden_class(NULL, hsize, HTYPE_GROW));
#endif /* RICH_HIDDEN_CLASS */
  hidden_n_enter(p->klass)++;
  n_enter_hc++;
#ifdef PROFILE
  if (p->profile_id)
    p->klass->n_profile_enter++;
#endif /* PROFILE */
#else
  Map *a;
  a = malloc_hashtable();
  hash_create(a, hsize);
  p->map = a;
#endif
#ifdef RICH_HIDDEN_CLASS
#ifdef EMBED_PROP
  {
    int i;
    for (i = 0; i < psize; i++)
      p->eprop[i] = JS_UNDEFINED;
  }
#else /* EMBED_PROP */
  p->prop = allocate_prop_table(hidden_n_limit_props(p->klass));
#endif /* EMBED_PROP */
#else /* RICH_HIDDEN_CLASS */
  p->prop = allocate_prop_table(psize);
  p->n_props = 0;
  p->limit_props = psize;
#endif /* RICH_HIDDEN_CLASS */
}
#endif /* ARRAY_EMBED_PROP */

/*
 * makes a simple object whose __proto__ property is not set yet
 *   hsize: size of the hash table
 *   psize: size of the array of property values
 */
JSValue new_simple_object_without_prototype(Context *ctx, int hsize,
                                            int psize) {
  JSValue ret;
  Object *p;

  /*  printf("new_simple_object_without_prototype\n"); */
#ifdef EMBED_PROP
  ret = make_simple_object(ctx, psize);
#else /* EMBED_PROP */
  ret = make_simple_object(ctx);
#endif /* EMBED_PROP */
  p = remove_simple_object_tag(ret);
  set_object_members(p, hsize, psize);
  return ret;
}

/*
 * makes a new simple object
 *   hsize: size of the hash table
 *   psize: size of the array of property values
 */
JSValue new_simple_object(Context *ctx, int hsize, int psize) {
  JSValue ret;
  Object *p;

  /* printf("new_simple_object\n"); */
#ifdef EMBED_PROP
  ret = make_simple_object(ctx, psize);
#else /* EMBED_PROP */
  ret = make_simple_object(ctx);
#endif /* EMBED_PROP */
  GC_PUSH(ret);
  p = remove_simple_object_tag(ret);
  set_object_members(p, hsize, psize);
  set___proto___all(ctx, ret, gconsts.g_object_proto);
  GC_POP(ret);
  return ret;
}

/*
 * makes a new array with size
 */
JSValue new_array_with_size(Context *ctx, int size, int hsize, int vsize)
{
  JSValue ret;

  ret = make_array(ctx);
  disable_gc();  /* disable GC unitl Array is properly initialised */
#ifdef EMBED_PROP
#ifdef ARRAY_EMBED_PROP
  set_object_members_with_class(array_object_p(ret),
                                gobjects.g_hidden_class_array);
#else /* ARRAY_EMBED_PROP */
  set_object_members(array_object_p(ret), hsize, 1);
#endif /* ARRAY_EMBED_PROP */
#else /* EMBED_PROP */
  set_object_members(array_object_p(ret), hsize, vsize);
#endif /* EMBED_PROP */
  set___proto___all(ctx, ret, gconsts.g_array_proto);
  allocate_array_data_critical(ret, size, size);
  set_prop_ddde(ctx, ret, gconsts.g_string_length, int_to_fixnum(size));
  GC_PUSH(ret);
  enable_gc(ctx);
  GC_POP(ret);
  return ret;
}

/*
 * makes a function
 * The name of this function was formerly new_closure.
 */
JSValue new_function(Context *ctx, Subscript subscr, int hsize, int vsize) {
  JSValue ret;

  ret = make_function();
  disable_gc();
#ifdef EMBED_PROP
  set_object_members(func_object_p(ret), hsize, 1);
#else
  set_object_members(func_object_p(ret), hsize, vsize);
#endif /* EMBED_PROP */
  func_table_entry(ret) = &(ctx->function_table[subscr]);
  func_environment(ret) = get_lp(ctx);
  GC_PUSH(ret);
  enable_gc(ctx);
  set_prototype_none(ctx, ret, new_normal_object(ctx));
  set___proto___none(ctx, ret, gconsts.g_function_proto);
  GC_POP(ret);
  return ret;
}

/*
 *  makes a new built-in function object with constructor
 */
JSValue new_builtin_with_constr(Context *ctx, builtin_function_t f,
                                builtin_function_t cons, int na, int hsize,
                                int psize) {
  JSValue ret;

  ret = make_builtin();
#ifdef EMBED_PROP
  set_object_members(builtin_object_p(ret), hsize, 1);
#else /* EMBED_PROP */
  set_object_members(builtin_object_p(ret), hsize, psize);
#endif /* EMBED_PROP */
  builtin_body(ret) = f;
  builtin_constructor(ret) = cons;
  builtin_n_args(ret) = na;
  GC_PUSH(ret);
  set_prototype_none(ctx, ret, new_normal_object(ctx));
  /* TODO: g_object_proto should be g_builtin_proto */
  /* set___proto___none(ctx, ret, gconsts.g_object_proto); */
  set___proto___none(ctx, ret, gconsts.g_builtin_proto);
  GC_POP(ret);
  return ret;
}

/*
 *  makes a new built-in function object
 */
JSValue new_builtin(Context *ctx, builtin_function_t f, int na, int hsize,
                    int psize) {
  return new_builtin_with_constr(ctx, f, builtin_not_a_constructor, na,
                                 hsize, psize);
}

/*
 * makes a simple iterator object
 */
JSValue new_iterator(Context *ctx, JSValue obj) {
  JSValue iter;
  int index = 0;
  int size = 0;
  JSValue tmpobj = obj;

  iter = make_iterator();

  /* allocate an itearator */
  do {
    /*
     * printf("Object %016llx: (type = %d, n_props = %lld)\n",
     *        obj, obj_header_tag(tmpobj), obj_n_props(tmpobj));
     */
#ifdef RICH_HIDDEN_CLASS
    size += hidden_n_props(obj_hidden_class(tmpobj));
#else /* RICH_HIDDEN_CLASS */
    size += obj_n_props(tmpobj);
#endif /* RICH_HIDDEN_CLASS */
  } while (get___proto__(tmpobj, &tmpobj) == SUCCESS);
  /* printf("size = %d\n", size); */
  GC_PUSH2(iter, obj);
  allocate_iterator_data(ctx, iter, size);

  /* fill the iterator with object properties */
  do {
    HashTable *ht;
    HashIterator hi;
    HashCell *p;

#ifdef HIDDEN_CLASS
    ht = obj_hidden_class_map(obj);
#else
    ht = obj_map(obj);
#endif
    init_hash_iterator(ht, &hi);

    while (nextHashCell(ht, &hi, &p) == SUCCESS) {
#ifdef HIDDEN_CLASS
      if ((JSValue)p->entry.attr & (ATTR_DE | ATTR_TRANSITION)) continue;
#else
      if ((JSValue)p->entry.attr & ATTR_DE) continue;
#endif
      /* printf("key = "); simple_print((JSValue)p->entry.key); putchar('\n'); */
      iterator_body_index(iter, index++) = (JSValue)p->entry.key;
    }
  } while (get___proto__(obj, &obj) == SUCCESS);
  GC_POP2(obj, iter);
  return iter;
}

#ifdef USE_REGEXP
/*
 * makes a new regexp
 */
JSValue new_regexp(Context *ctx, char *pat, int flag, int hsize, int vsize) {
  JSValue ret;

  ret = make_regexp();
#ifdef EMBED_PROP
  set_object_members(regexp_object_p(ret), hsize, 1);
#else /* EMBED_PROP */
  set_object_members(regexp_object_p(ret), hsize, vsize);
#endif /* EMBED_PROP */
  /* pattern field is set in set_regexp_members */
  /* regexp_pattern(ret) = NULL; */
  regexp_reg(ret) = NULL;
  regexp_global(ret) = false;
  regexp_ignorecase(ret) = false;
  regexp_multiline(ret) = false;
  regexp_lastindex(ret) = 0;
  set___proto___none(ctx, ret, gconsts.g_regexp_proto);
  return
    (set_regexp_members(ctx, ret, pat, flag) == SUCCESS)? ret: JS_UNDEFINED;
}
#endif /* USE_REGEXP */

/*
 * makes a new boxed number
 */
JSValue new_number_object(Context *ctx, JSValue v, int hsize, int psize) {
  JSValue ret;

  GC_PUSH(v);
  ret = make_number_object(ctx);
  GC_PUSH(ret);
#ifdef EMBED_PROP
  set_object_members(number_object_object_ptr(ret), hsize, 1);
#else /* EMBED_PROP */
  set_object_members(number_object_object_ptr(ret), hsize, psize);
#endif /* EMBED_PROP */
  number_object_value(ret) = v;
  set___proto___none(ctx, ret, gconsts.g_number_proto);
  GC_POP2(ret,v);
  return ret;
}

/*
 * makes a new boxed boolean
 */
JSValue new_boolean_object(Context *ctx, JSValue v, int hsize, int psize) {
  JSValue ret;

  /* We do not need to gc_push v because v should be a boolean */
  GC_PUSH(v);
  ret = make_boolean_object(ctx);
  GC_POP(v);
#ifdef EMBED_PROP
  set_object_members(boolean_object_object_ptr(ret), hsize, 1);
#else /* EMBED_PROP */
  set_object_members(boolean_object_object_ptr(ret), hsize, psize);
#endif /* EMBED_PROP */
  boolean_object_value(ret) = v;
  GC_PUSH(ret);
  set___proto___none(ctx, ret, gconsts.g_boolean_proto);
  GC_POP(ret);
  return ret;
}

/*
 * makes a new boxed string
 */
JSValue new_string_object(Context *ctx, JSValue v, int hsize, int psize) {
  JSValue ret;

  GC_PUSH(v);
  ret = make_string_object(ctx);
#ifdef EMBED_PROP
  set_object_members(string_object_object_ptr(ret), hsize, 1);
#else /* EMBED_PROP */
  set_object_members(string_object_object_ptr(ret), hsize, psize);
#endif /* EMBED_PROP */
  string_object_value(ret) = v;
  GC_PUSH(ret);
  set___proto___none(ctx, ret, gconsts.g_string_proto);
  /*
   * A boxed string has a property ``length'' whose associated value
   * is the length of the string.
   */
  set_prop_all(ctx, ret, gconsts.g_string_length,
               int_to_fixnum(strlen(string_to_cstr(v))));
  GC_POP2(ret,v);
  return ret;
}

/*  data conversion functions */
char *space_chomp(char *str) {
  while (isspace(*str)) str++;
  return str;
}

double cstr_to_double(char* cstr) {
  char* endPtr;
  double ret;
  ret = strtod(cstr, &endPtr);
  while (isspace(*endPtr)) endPtr++;
  if (*endPtr == '\0') return ret;
  else return NAN;
}

#ifdef HIDDEN_CLASS
/*
 * allocates a new empty hidden class
 */
#ifdef RICH_HIDDEN_CLASS
#ifdef ARRAY_EMBED_PROP
HiddenClass *new_empty_hidden_class(Context *ctx, int hsize, int psize,
                                    int n_prop, int n_special, int htype)
#else /* ARRAY_EMBED_PROP */
HiddenClass *new_empty_hidden_class(Context *ctx, int hsize,
                                    int psize, int htype)
#endif /* ARRAY_EMBED_PROP */
#else /* RICH_HIDDEN_CLASS */
HiddenClass *new_empty_hidden_class(Context *ctx, int hsize, int htype)
#endif /* RICH_HIDDEN_CLASS */
{
  HiddenClass *c;
  Map *a;

  c = (HiddenClass *)gc_malloc(ctx, sizeof(HiddenClass), HTAG_HIDDEN_CLASS);
  disable_gc();
  a = malloc_hashtable();
  hash_create(a, hsize);
  hidden_map(c) = a;
  hidden_n_entries(c) = 0;
  hidden_htype(c) = htype;
  hidden_n_enter(c) = 0;
  hidden_n_exit(c) = 0;
#ifdef RICH_HIDDEN_CLASS
#ifdef ARRAY_EMBED_PROP
  hidden_n_props(c) = n_prop;
#else /* ARRAY_EMBED_PROP */
  hidden_n_props(c) = 0;
#endif /* ARRAY_EMBED_PROP */
#ifdef EMBED_PROP
  hidden_n_limit_props(c) = 0;
  hidden_n_embedded_props(c) = psize;
#ifdef ARRAY_EMBED_PROP
  hidden_n_special_props(c) = n_special;
#endif /* ARRAY_EMBED_PROP */
#else /* EMBED_PROP */
  hidden_n_limit_props(c) = psize;
#endif /* EMBED_PROP */
#endif /* RICH_HIDDEN_CLASS */
#ifdef PROFILE
  hidden_prev(c) = NULL;
  hidden_n_profile_enter(c) = 0;
#endif /* PROFILE */
  GC_PUSH(c);
  enable_gc(ctx);
  GC_POP(c);
  n_hc++;
  return c;
}

/*
 * allocates a new hidden class on the basis of oldc and name
 */
HiddenClass *new_hidden_class(Context *ctx, HiddenClass *oldc) {
  HiddenClass *c;
  Map *a;
  int ne;

  GC_PUSH(oldc);
  c = (HiddenClass *)gc_malloc(ctx, sizeof(HiddenClass), HTAG_HIDDEN_CLASS);
  GC_PUSH(c);
  a = malloc_hashtable();
  hash_create(a, oldc->map->size);
  hidden_map(c) = a;
  ne = hash_copy(ctx, hidden_map(oldc), a);
  GC_POP2(c,oldc);
  hidden_n_entries(c) = ne;
  hidden_htype(c) = HTYPE_TRANSIT;
  hidden_n_enter(c) = 0;
  hidden_n_exit(c) = 0;
#ifdef RICH_HIDDEN_CLASS
  hidden_n_props(c) = hidden_n_props(oldc) + 1;
#ifdef EMBED_PROP
  {
    int n_embedded = hidden_n_embedded_props(oldc);
    int n_limit = hidden_n_limit_props(oldc);
    int n_prop = hidden_n_props(c);
    hidden_n_embedded_props(c) = n_embedded;
    hidden_n_limit_props(c) =
      compute_new_limit_props(n_embedded, n_limit, n_prop);
  }
#else /* EMBED_PROP */
  if (hidden_n_props(c) <= hidden_n_limit_props(oldc))
    hidden_n_limit_props(c) = hidden_n_limit_props(oldc);
  else {
    CHECK_INCREASE_PROPERTY(hidden_n_limit_props(oldc));
    hidden_n_limit_props(c) = hidden_n_limit_props(oldc) + PSIZE_DELTA;
  }
#endif /* EMBED_PROP */
#ifdef ARRAY_EMBED_PROP
  hidden_n_special_props(c) = hidden_n_special_props(oldc);
#endif /* ARRAY_EMBED_PROP */
#endif /* RICH_HIDDEN_CLASS */
#ifdef PROFILE
  hidden_prev(c) = oldc;
  hidden_n_profile_enter(c) = 0;
#endif /* PROFILE */
  n_hc++;
  return c;
}

#ifdef PROFILE
void print_hidden_class(char *s, HiddenClass *hc) {
  printf("======= %s start ======\n", s);
#ifdef PROFILE
  printf("HC: %s %p %d %p (n_entries = %d, htype = %d, n_enter = %d, n_exit = %d)\n",
         s, hc, hc->n_profile_enter++, hc->prev,
         hc->n_entries, hc->htype, hc->n_enter, hc->n_exit);
#else
  printf("HC: %p (n_entries = %d, htype = %d, n_enter = %d, n_exit = %d)\n",
         hc, hc->n_entries, hc->htype, hc->n_enter, hc->n_exit);
#endif /* PROFILE */
  print_hash_table(hc->map);
  printf("======= %s end ======\n", s);
}

static int nhc = 1;

void print_hidden_class_recursive(char *s, HiddenClass *hc) {
  HashTable *tab;
  HashCell *p;
  int i;
  char buf[128];

  sprintf(buf, "%d: %s", nhc, s);
  print_hidden_class(buf, hc);
  nhc++;
  tab = hc->map;
  for (i = 0; i < tab->size; i++) {
    if ((p = tab->body[i]) == NULL) continue;
    do {
      if (is_transition(p->entry.attr))
        print_hidden_class_recursive(string_to_cstr(p->entry.key),
                                     (HiddenClass *)p->entry.data);
    } while ((p = p->next) != NULL);
  }
}

void print_all_hidden_class(void) {
  print_hidden_class_recursive("hidden_class_0", gobjects.g_hidden_class_0);
}
#endif /* PROFILE */
#endif

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
