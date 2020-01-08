/**
 * Obtain property of the object. It converts type of ``name'' if necessary.
 *   obj:  any type
 *   name: any type
 */
#ifdef INLINE_CACHE
JSValue get_object_prop(Context *ctx, JSValue obj, JSValue name,
			InlineCache *ic)
#else /* INLINE_CACHE */
  JSValue get_object_prop(Context *ctx, JSValue obj, JSValue name)
#endif /* INLINE_CACHE */
{
  if (!is_string(name)) {
    GC_PUSH(obj);
    name = to_string(ctx, name);
    GC_POP(obj);
  }
#ifdef INLINE_CACHE
  return get_prop_prototype_chain_with_ic(obj, name, ic);
#else /* INLINE_CACHE */
  return get_prop_prototype_chain(obj, name);
#endif /* INLINE_CACHE */
}

/*
 * obtain array element. `index' is an integer.
 * returns JS_EMPTY if `index` is out of range.
 */
JSValue get_array_element(Context *ctx, JSValue array, cint index)
{
  JSValue prop, ret;
  assert(is_array(array));

  if ((ret = get_array_element_no_proto(array, index)) != JS_EMPTY) {
    return ret;
  }

  prop = cint_to_number(ctx, index);
  prop = number_to_string(prop);
  return get_prop_prototype_chain(array, prop);
}

/*
 *  obtains array's property
 *    a: array
 *    p: property (number / string / other type)
 *  It is not necessary to check the type of `a'.
 */
JSValue get_array_prop(Context *ctx, JSValue a, JSValue p)
{
  if (is_fixnum(p))
    return get_array_element(ctx, a, fixnum_to_cint(p));

  if (!is_string(p)) {
    GC_PUSH(a);
    p = to_string(ctx, p);
    GC_POP(a);
  }
  assert(is_string(p));
  {
    JSValue num;
    GC_PUSH2(a, p);
    num = string_to_number(ctx, p);
    GC_POP2(p, a);
    if (is_fixnum(num))
      return get_array_element(ctx, a, fixnum_to_cint(num));
    else
      return get_prop_prototype_chain(a, p);
  }
}

/*
 * determines whether a[n] exists or not
 * if a[n] is not an element of body (a C array) of a, search properties of a
 *  a: array
 *  n: subscript
 */
int has_array_element(JSValue a, cint n)
{
  if (!is_array(a))
    return FALSE;
  if (n < 0 || get_jsarray_length(a) <= n)
    return FALSE;
  /* in body of 'a' */
  if (n < get_jsarray_size(a))
    return TRUE;
  /* in property of 'a' */
  return get_prop_prototype_chain(a, cint_to_string(n)) != JS_EMPTY;
}

/*
 * sets object's property
 *   o: object (but not an array)
 *   p: property (number / string / other type)
 *   v: value to be set
 * It is not necessary to check the type of `o'.
 */
int set_object_prop(Context *ctx, JSValue o, JSValue p, JSValue v)
{
  if (!is_string(p)) {
    GC_PUSH2(o, v);
    p = to_string(ctx, p);
    GC_POP2(v, o);
  }
  set_prop(ctx, o, p, v, ATTR_NONE);
  return SUCCESS;
}


/*
 * An array element is stored
 *  1. in array storage, or
 *  2. as a property
 * If 0 <= index < array.size, then the element is stored in the array storage.
 * Otherwise, it is stored as a property.
 *
 * Before judging where an element should be stored to, array storage may
 * be expanded.  If array.size <= index < ASIZE_LIMIT, the array storage
 * is expanded to the length of index.
 */

/*
 * Try to set a value into an continuous array of Array.
 * If the index is out of range of limit of continuous container,
 * handle it as a normal property.
 */
void set_array_element(Context *ctx, JSValue array, cint index, JSValue v)
{
  assert(is_array(array));

  /* 1. If array.size <= index < ASIZE_LIMIT, expand the storage */
  if (get_jsarray_size(array) <= index && index < ASIZE_LIMIT) {
    GC_PUSH2(array, v);
    reallocate_array_data(ctx, array, index + 1);
    GC_POP2(v, array);
  }

  /* 2. If 0 <= index < array.size, store the value to the storage */
  if (0 <= index && index < get_jsarray_size(array)) {
    JSValue *storage = get_jsarray_body(array);
    storage[index] = v;
  } else {
    /* 3. otherwise, store it as a property */
    JSValue prop;
    GC_PUSH2(array, v);
    prop = cint_to_number(ctx, index);
    prop = number_to_string(prop);
    GC_POP2(v, array);
    set_prop(ctx, array, prop, v, ATTR_NONE);
  }

  /* 4. Adjust `length' property. */
  {
    JSValue length_value;
    cint length;
    length_value = get_jsarray_length(array);
    assert(is_fixnum(length_value));
    length = fixnum_to_cint(length_value);
    if (length <= index)
      set_jsarray_length(array, cint_to_number(ctx, index + 1));
  }
}

int set_array_prop(Context *ctx, JSValue array, JSValue prop, JSValue v)
{
  JSValue index_prop;

  /* 1. If prop is fixnum, do element access. */
  if (is_fixnum(prop)) {
    cint index = fixnum_to_cint(prop);
    set_array_element(ctx, array, index, v);
    return SUCCESS;
  }

  /* 2. Convert prop to a string. */
  GC_PUSH2(array, v);
  if (!is_string(prop))
    prop = to_string(ctx, prop);

  /* 3. If prop is fixnum-like, do element access. */
  GC_PUSH(prop);
  index_prop = string_to_number(ctx, prop);
  GC_POP3(prop, v, array);
  if (is_fixnum(index_prop)) {
    cint index = fixnum_to_cint(index_prop);
    set_array_element(ctx, array, index, v);
    return SUCCESS;
  }

  /* 4. If prop is `length', adjust container size. */
  if (prop == gconsts.g_string_length) {
    double double_length;
    int32_t length;
    if (!is_number(v))
      v = to_number(ctx, v);
    double_length = number_to_double(v);
    length = (int32_t) double_length;
    if (double_length != (double) length || length < 0)
      LOG_EXIT("invalid array length");
    /* 4.1. If length is less than ASIZE_LIMIT, adjust container size. */
    if (length <= ASIZE_LIMIT) {
      uint32_t size = get_jsarray_size(array);
      if (size != length)
	reallocate_array_data(ctx, array, length);
    }
    /* 4.2. If new length is smaller, delete numerical properties. */
    if (length < get_jsarray_length(array)) {
      Shape *os = object_get_shape(array);
      PropertyMap *pm = os->pm;
      HashIterator iter = createHashIterator(pm->map);
      HashCell *p;
      while (nextHashCell(pm->map, &iter, &p) != FAIL) {
	if (!is_transition(p->entry.attr)) {
	  JSValue key = (JSValue) p->entry.key;
	  JSValue number_key;
	  double double_key;
	  int32_t int32_key;
	  assert(is_string(key));
	  number_key = string_to_number(ctx, key);
	  double_key = number_to_double(number_key);
	  int32_key = (int32_t) double_key;
	  if (int32_key >= 0 && double_key == (double) int32_key)
	    set_prop(ctx, array, key, JS_EMPTY, ATTR_NONE);
	}
      }
    }
    /* 4.3 Set length property. */
    set_jsarray_length(array, v);
    return SUCCESS;
  }

  /* 5. Set normal property */
  set_prop(ctx, array, prop, v, ATTR_NONE);
  return SUCCESS;
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
int delete_object_prop(JSValue obj, HashKey key)
{
  int index;
  Attribute attr;

  if (!is_object(obj))
    return FAIL;

  /* Set corresponding property as JS_UNDEFINED */
  index = prop_index(jsv_to_jsobject(obj), key, &attr, NULL);
  if (index == - 1)
    return FAIL;
  object_set_prop(obj, index, JS_UNDEFINED);

  /* Delete map */
  LOG_EXIT("delete is not implemented");
  return SUCCESS;
}

/*
 * delete a[n]
 * Note that this function does not change a.length
 */
int delete_array_element(JSValue a, cint n)
{
  if (n < get_jsarray_size(a)) {
    JSValue *body = get_jsarray_body(a);
    body[n] = JS_UNDEFINED;
    return SUCCESS;
  }
  return delete_object_prop(a, cint_to_string(n));
}

/*
 * obtains the next property name in an iterator
 * iter:Iterator
 */
int iterator_get_next_propname(JSValue iter, JSValue *name)
{
  int size = get_jsnormal_iterator_size(iter);
  int index = get_jsnormal_iterator_index(iter);
  if(index < size) {
    JSValue *body = get_jsnormal_iterator_body(iter);
    *name = body[index++];
    set_jsnormal_iterator_index(iter, index);
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
int set_regexp_members(Context *ctx, JSValue re, char *pat, int flag)
{
  OnigOptionType opt;
  OnigErrorInfo err;
  char *e;

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
int regexp_flag(JSValue re)
{
  int flag;

  flag = 0;
  if (regexp_global(re)) flag |= F_REGEXP_GLOBAL;
  if (regexp_ignorecase(re)) flag |= F_REGEXP_IGNORE;
  if (regexp_multiline(re)) flag |= F_REGEXP_MULTILINE;
  return flag;
}
#endif

/*
 * makes a simple iterator object
 */
JSValue new_iterator(Context *ctx, JSValue obj) {
  JSValue iter;
  int index = 0;
  int size = 0;
  JSValue tmpobj;

  GC_PUSH(obj);
  iter = ptr_to_normal_iterator(allocate_iterator(ctx));

  /* allocate an itearator */
  tmpobj = obj;
  do {
    PropertyMap *pm = object_get_shape(tmpobj)->pm;
    size += pm->n_props - pm->n_special_props;
    tmpobj = get_prop(tmpobj, gconsts.g_string___proto__);
  } while (tmpobj != JS_NULL);
  GC_PUSH(iter);
  allocate_iterator_data(ctx, iter, size);

  /* fill the iterator with object properties */
  do {
    HashTable *ht;
    HashIterator hi;
    HashCell *p;
    JSValue *body;

    ht = object_get_shape(obj)->pm->map;
    init_hash_iterator(ht, &hi);

    body = get_jsnormal_iterator_body(iter);
    while (nextHashCell(ht, &hi, &p) == SUCCESS) {
      if ((JSValue)p->entry.attr & (ATTR_DE | ATTR_TRANSITION))
        continue;
      body[index++] = (JSValue)p->entry.key;
    }
    obj = get_prop(obj, gconsts.g_string___proto__);
  } while (obj != JS_NULL);
  GC_POP2(iter, obj);
  return iter;
}

/*  data conversion functions */
char *space_chomp(char *str)
{
  while (isspace(*str)) str++;
  return str;
}

double cstr_to_double(char* cstr)
{
  char* endPtr;
  double ret;
  ret = strtod(cstr, &endPtr);
  while (isspace(*endPtr)) endPtr++;
  if (*endPtr == '\0') return ret;
  else return NAN;
}


/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
