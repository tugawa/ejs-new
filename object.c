/*
   object.c

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

/*
   There are two functions and a macro that are used in allocating an
   object of a specified type, namely allocate_xxx, make_xxx, and new_xxx.
   For example, in allocating a function, allocate_function, make_function,
   and new_function are used.  Each of them has the following role.

   allocate_xxx : (function)
     This allocates a memory for an xxx and sets an appropriate object tag
     in its header.

   make_xxx : (macro)
     This only calls allocate_xxx, puts pointer tag (T_GENERIC), and
     returns a JSValue data.

   new_xxx : (function)
     This first calls make_xxx and sets various values within the returned
     object.  set_object_members is called in this function.
 */

#include "prefix.h"
#define EXTERN
#include "header.h"

#define PROP_REALLOC_THRESHOLD (0.75)

#define prop_overflow(o) \
  (obj_n_props(o) > (obj_limit_props(o) * PROP_REALLOC_THRESHOLD))

#define sign(x) ((x) > 0? 1: -1)

#define hash_put(map, name, retv, attr) \
  ((hash_put_with_attribute((map),(name),(retv),(attr)) == HASH_PUT_SUCCESS)? \
   (int)(retv): (-1))

#ifdef HIDDEN_CLASS
static HiddenClass *hclass;
int n_hc;
int n_enter_hc;
int n_exit_hc;
#endif

/*
   obtains the index of a property of an Object
   If the specified property does not exist, returns -1.

   Note that if HIDDEN_CLASS is defined and hash_get_with_attribute returns
   HASH_GET_SUCCESS, the hidden map in the object contains either the index
   of JSValue array (as a fixnum) or the pointer to the next hidden class.
   These two cases can be distinguished by investigating the ``transition
   bit'' of the obtained attribute.  For the latter case, the pointer to
   the next hidden class is temporary assigned to a static variable named
   ``hclass''.
 */
int prop_index(JSValue obj, JSValue name)
{
  HashData retv;
  int result;
#ifdef HIDDEN_CLASS
  Attribute att;
#endif

  if (!is_object(obj)) return -1;   // Is it necessary to check the type?
#ifdef HIDDEN_CLASS
  result =
    hash_get_with_attribute(obj_hidden_class_map(obj), name, &retv, &att);
  // printf("in prop_index: result = %d\n", result);
  if (result == HASH_GET_FAILED) {
    hclass = NULL;
    // printf("in prop_index: hclass = NULL, returning -1\n");
    return -1;
  }
  if (is_transition(att)) {
    hclass = (HiddenClass *)retv;
    // printf("in prop_index: is_transition, returning -1\n");
    return -1;
  } else {
    // printf("prop_index is returning %d\n", (int)retv);
    return (int)retv;
  }
#else
  // print_hash_table(obj_map(obj));
  result = hash_get(obj_map(obj), name, &retv);
  if (result == HASH_GET_FAILED) return -1;
  // printf("prop_index: "); simple_print(retv); putchar('\n');
  return (int)retv;
#endif
}

/*
   obtains the property value of the key ``name'', stores it to *ret
   and returns SUCCESS or FAIL
   This function does not follow the prototype chain.
 */
int get_prop(JSValue obj, JSValue name, JSValue *ret)
{
  int index;

  // printf("get_prop: obj = %016lx, prop = %s\n", obj, string_to_cstr(name));
  // simple_print(name); putchar('\n');
  index = prop_index(obj, name);
  // printf("get_prop: index = %d\n", index);
  if (index == -1) return FAIL;
  // simple_print(obj_prop_index(obj, index)); putchar('\n');
  *ret = obj_prop_index(obj, index);
  return SUCCESS;
}

/*
  obtains the property from an object by following the prototype chain
  (if necessary)
   o: object
   p: property, which is a string
 */
JSValue get_prop_prototype_chain(JSValue o, JSValue p) {
  JSValue ret;
  extern JSValue prototype_object(JSValue);

  // printf("get_prop_prototype_chain: o = "); simple_print(o); printf("\n");
  // printf("get_prop_prototype_chain: p = "); simple_print(p); printf("\n");
  // printf("get_prop_prototype_chain: prop = %s, obj = %016lx\n", string_to_cstr(p), o);
  // printf("Object.__proto__ = %016lx\n", gconsts.g_object_proto);
  do {
    if (get_prop(o, p, &ret) == SUCCESS) return ret;
  } while (get___proto__(o, &o) == SUCCESS);
  // is it necessary to search in the Object's prototype?
  return JS_UNDEFINED;
}

/*
   obtains object's property
     o: object (but not an array)
     p: property (number / string / other type)
   It is not necessary to check the type of `o'.
 */
JSValue get_object_prop(Context *ctx, JSValue o, JSValue p) {
  /*
    if (p is not a string) p = to_string(p);
      returns the value regsitered under the property p
    }
  */
  // printf("get_object_prop, o = %016lx, p = %016lx\n", o, p);
  if (!is_string(p)) p = to_string(ctx, p);
  return get_prop_prototype_chain(o, p);
}

/*
  determin whether an object has a property by following the prototype chain
  if the object has the property, TRUE, else FALSE
   o: object
   p: property, which is a string
 */
int has_prop_prototype_chain(JSValue o, JSValue p) {
  JSValue ret;
  extern JSValue prototype_object(JSValue);
  do {
    if (get_prop(o, p, &ret) == SUCCESS) return TRUE;
  } while (get_prop(o, gconsts.g_string___proto__, &o) == SUCCESS);
  // is it necessary to search in the Object's prototype?
  return FALSE;
}

/*
  determin whether a[n] exists or not
  if a[n] is not an element of body (an C array) of a, search properties of a
   a: array
   n: subscript
 */
int has_array_element(JSValue a, cint n) {
  if (!is_array(a)) return FALSE;
  if (n < 0 || array_length(a) <= n) return FALSE;
  /* in body of a */
  /* is it ok that a[n] (0 <= n < len) always exists? */
  if (n < array_size(a)) return TRUE;
  /* in property of a */
  return has_prop_prototype_chain(a, cint_to_string(n));

//if (!is_array(a)) return FALSE;
///* in body of a */
///* is it ok that a[n] (0 <= n < len) always exists? */
//if (0 <= n && n < array_size(a))
//  return (n < array_length(a))? TRUE: FALSE;
///* in property of a */
//return has_prop_prototype_chain(a, cint_to_string(n));
}

/*
   obtains array's property
     a: array
     p: property (number / string / other type)
   It is not necessary to check the type of `a'.
 */
JSValue get_array_prop(Context *ctx, JSValue a, JSValue p) {
  /*
    if (p is a number) {
      if (p is within the range of an subscript of an array)
        returns the p-th element of a
      else {
        p = number_to_string(idx);
        returns the value regsitered under the property p
      }
    } else {
      if (p is not a string) p = to_string(p);
      s = string_to_number(p);
      if (s is within the range of an subscript of an array)
        returns the s-th element of a
      else
        returns the value regsitered under the property p
    }

    I am afraid that the above definition is incorrect.

    } else if (p is a string) {
      s = string_to_number(p);
      if (s is within the range of an subscript of an array)
        returns the s-th element of a
      else
        returns the value regsitered under the property p
    } else {
      p = to_string(p);
      returns the value regsitered under the property p
    }

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

  if (!is_string(p))
    p = to_string(ctx, p);
  /* assert: is_string(p) == true */ {
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

/*
   sets an object's property value with its attribute
 */
int set_prop_with_attribute(Context *ctx, JSValue obj, JSValue name, JSValue v, Attribute attr) {
  uint64_t retv, newsize;
  int index, r;
#ifdef HIDDEN_CLASS
  HiddenClass *nexth, *oh;
#endif // HIDDEN_CLASS

  // printf("set_prop_with_attribute: obj = %p, name = %s, v = %p\n",
  //        obj, string_to_cstr(name), v);
  index = prop_index(obj, name);
  // printf("set_prop_with_index: index = %d\n", index);
  if (index == -1) {
    // The specified property is not registered in the hash table.
    retv = obj_n_props(obj);
    if (retv >= obj_limit_props(obj)) {
      // The property array is full
      //      printf("proptable full: obj_n_props(obj) = %d, obj_limit_props(obj) = %d\n",
      //             obj_n_props(obj), obj_limit_props(obj));
      if ((newsize = increase_psize(retv)) == retv) {
        LOG_EXIT("proptable overflow\n");
        return FAIL;
      }
      gc_push_tmp_root3(&obj, &name, &v);
      obj_prop(obj) = reallocate_prop_table(ctx, obj_prop(obj), retv, newsize);
      gc_pop_tmp_root(3);
      obj_limit_props(obj) = newsize;
      //      printf("proptable expansion succeeded: obj_n_props(obj) = %d, obj_limit_props(obj) = %d\n",
      //             obj_n_props(obj), obj_limit_props(obj));
    }
    index = (cint)retv;
    // printf("set_prop_with_index: index = %d\n", index);
#ifdef HIDDEN_CLASS
    oh = obj_hidden_class(obj);    // source hidden class
    /*
    if (hidden_htype(oh) == HTYPE_TRANSIT)
      print_hidden_class("transit_hidden_class: from (TRANSIT)", oh);
    else
      print_hidden_class("transit_hidden_class: from (GROW)", oh);
    */
    if (hidden_htype(oh) == HTYPE_TRANSIT) {
      nexth = hclass;    // NULL or pointer to the next hidden class
      if (nexth == NULL) {
        // printf("transit_hidden_class: making the next hidden class\n");
        gc_push_tmp_root3(&obj, &name, &v);
        nexth = new_hidden_class(ctx, oh);
        gc_pop_tmp_root(3);
        // print_hidden_class("transit_hidden_class: to (before put)", nexth);
        gc_push_tmp_root_malloc2((void **)&nexth, (void **)&oh);
        r = hash_put_with_attribute(hidden_map(nexth), name, index, attr);
        if (r != HASH_PUT_SUCCESS) {
          gc_pop_tmp_root_malloc(2);
          return FAIL;
        }
        hidden_n_entries(nexth)++;
        hash_put_with_attribute(hidden_map(oh), name, (HashData)nexth,
                          ATTR_NONE | ATTR_TRANSITION);
        gc_pop_tmp_root_malloc(2);
        hidden_n_entries(oh)++;
      }
      hidden_n_exit(obj_hidden_class(obj))++;
      n_exit_hc++;
      obj_hidden_class(obj) = nexth;
      hidden_n_enter(nexth)++;
      n_enter_hc++;
    } else {                  // hidden_htype(oh) == HTYPE_GROW
      nexth = oh;
      gc_push_tmp_root_malloc((void **)&nexth);
      r = hash_put_with_attribute(hidden_map(nexth), name, index, attr);
      gc_pop_tmp_root_malloc(1);
      if (r != HASH_PUT_SUCCESS) return FAIL;
      hidden_n_entries(nexth)++;
    }
    // print_hidden_class("transit_hidden_class: to (after put)", nexth);
#else // HIDDEN_CLASS
    gc_push_tmp_root(&obj);
    // printf("set_prop_with_index: putting attribute, name = %s, index = %d\n", string_to_cstr(name), index);
    r = hash_put_with_attribute(obj_map(obj), name, index, attr);
    // printf("set_prop_with_index: hash_put_with_attribute done, r = %d\n", r);
    gc_pop_tmp_root(1);
    if (r != HASH_PUT_SUCCESS) return FAIL;
    // print_hash_table(obj_map(obj));
#endif // HIDDEN_CLASS
    (obj_n_props(obj)) = index + 1;
  }
  obj_prop_index(obj, index) = v;
  return SUCCESS;
}

/*
   sets object's property
     o: object (but not an array)
     p: property (number / string / other type)
     v: value to be set
   It is not necessary to check the type of `o'.
 */
int set_object_prop(Context *ctx, JSValue o, JSValue p, JSValue v) {
  if (!is_string(p)) p = to_string(ctx, p);
  // printf("set_object_prop: "); print_value_verbose(ctx, p); printf("\n");
  return set_prop_none(ctx, o, p, v);
}

/*
    set_array_index_value
    a is an array and n is a subscript where n >= 0.
    This function is called when
      a[n] <- v (in this case, setlength is False)
      or
      a.length <- n + 1 (in this case, setlength is True)
    
    In the latter case, it is not necessary to do a[n] <- v, but
    it may be necessary to shrink the array.

    returns
      SUCCESS: the above assignment is performed
      FAIL   : the above assignment has not been done yet because n is
               outside, but expanding array has been done if necessary
 */
int set_array_index_value(Context *ctx, JSValue a, cint n, JSValue v, int setlength) {
  cint len, size, adatamax;
  int i;

  len = array_length(a);
  size = array_size(a);
  adatamax = (size <= ASIZE_LIMIT)? ASIZE_LIMIT: size;
  // printf("set_array_index_value: n = %d\n", n);
  if (n < adatamax) {
    if (size <= n) {
      /*
        It is necessary to expand the array, but since n is less than
        ASIZE_LIMIT, it is possible to expand the array data.
       */
      cint newsize;
      while ((newsize = increase_asize(size)) <= n) size = newsize;
      reallocate_array_data(ctx, a, newsize);
    }
    /* If len <= n, expands the array.  It should be noted that
       if len >= n, this for loop does nothing */
    for (i = len; i <= n; i++) // i < n?
      array_body_index(a, i) = JS_UNDEFINED;
  } else {
    /*
      Since n is outside of the range of array data, stores the
      value into the hash table of the array.
    */
    if (size < ASIZE_LIMIT) {
      /* The array data is not fully expanded, so we expand it */
      reallocate_array_data(ctx, a, ASIZE_LIMIT);
      for (i = len; i < ASIZE_LIMIT; i++)
        array_body_index(a, i) = JS_UNDEFINED;
      adatamax = ASIZE_LIMIT;
    }
  }
  if (len <= n || setlength == TRUE) {
    array_length(a) = n + 1;
    set_prop_none(ctx, a, gconsts.g_string_length, cint_to_fixnum(n + 1));
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
   sets array's property
     a: array
     p: property (number / string / other type)
     v: value to be set
   It is not necessary to check the type of `a'.
 */
int set_array_prop(Context *ctx, JSValue a, JSValue p, JSValue v) {
  if (is_fixnum(p)) {
    cint n;

    n = fixnum_to_cint(p);
    if (0 <= n && n < MAX_ARRAY_LENGTH) {
      if (set_array_index_value(ctx, a, n, v, FALSE) == SUCCESS)
	return SUCCESS;
    }
    p = fixnum_to_string(p);
    return set_object_prop(ctx, a, p, v);
  }

  if (!is_string(p))
    p = to_string(ctx, p);
  /* assert: p == string */ {
    JSValue num;
    cint n;

    num = string_to_number(p);
    if (is_fixnum(num)) {
      n = fixnum_to_cint(num);
      if (0 <= n && n < MAX_ARRAY_LENGTH) {
	if (set_array_index_value(ctx, a, n, v, FALSE) == SUCCESS)
	  return SUCCESS;
      }
      return set_object_prop(ctx, a, p, v);
    }
    if (p == gconsts.g_string_length && is_fixnum(v)) {
      cint n;
      n = fixnum_to_cint(v);
      if (0 <= n && n < MAX_ARRAY_LENGTH) {
	/*
	  The property name is "length" and the given value is a fixnum.
	  Thus, expands / shrinks the array.
	*/
	if (set_array_index_value(ctx, a, n - 1, JS_UNDEFINED, TRUE)
	    == SUCCESS)
	  return SUCCESS;
      }
    }
    return set_object_prop(ctx, a, p, v);
  }
}

/*
   removes array data whose subscript is between `from' and `to'
   that are stored in the property table.
   implemented tentatively
 */
void remove_array_props(JSValue a, cint from, cint to) {
  // printf("%d-%d\n",from,to);
  for (; from < to ; from++)
    delete_array_element(a, from);
}

/*
   delete the hash cell with key and the property of the object
   NOTE:
     The function does not reallocate (shorten) the prop array of the object.
     It must be improved.
   NOTE:
     When using hidden class, this function does not delete a property of an object
     but merely sets the corresponding property as JS_UNDEFINED,
 */
int delete_object_prop(JSValue obj, HashKey key) {
  int index;

  if (!is_object(obj)) return FAIL;

  /* Set corresponding property as JS_UNDEFINED */
  index = prop_index(obj, key);
  if (index == - 1) return FAIL;
  obj_prop_index(obj, index) = JS_UNDEFINED;

  /* Delete map */
#ifdef HIDDEN_CLASS
  // LOG("To delete properties of an object is not completely implemented when using hidden class (instead set a property to undefined)\n"); // TODO
#else
  /* Free the HashCell */
  if(hash_delete(obj_map(obj), key) == HASH_GET_FAILED) return FAIL;
#endif
  return SUCCESS;
}

/*
   delete a[n]
   Note that this function does not change a.length
 */
int delete_array_element(JSValue a, cint n) {
  if (n < array_size(a)) {
    array_body_index(a, n) = JS_UNDEFINED;
    return SUCCESS;
  }
  return delete_object_prop(a, cint_to_string(n));
}

#if 0
static inline void setArrayBody(JSValue array, int size)
{
  if(size < MINIMUM_ARRAY_SIZE)
    size = MINIMUM_ARRAY_SIZE;
  ((ArrayCell*)array)->body = allocateArrayData(size);
  setArraySize(array, size);
}
#endif

/*
   obtains the next key of the property
 */
int next_propname(JSValue obj, HashIterator *iter, HashKey *key)
{
  HashEntry e;
  int r;
  Map *a;

#ifdef HIDDEN_CLASS
  a = obj_hidden_class_map(obj);
  while ((r = __hashNext(a, iter, &e)) != FAIL &&
         ((e.attr & ATTR_DE) || (e.attr & ATTR_TRANSITION)));
#else
  a = obj_map(obj);
  while ((r = __hashNext(a, iter, &e)) != FAIL && (e.attr & ATTR_DE));
#endif

  if (r == FAIL) return FAIL;
  *key = e.key;
  return SUCCESS;
}

/*
   obtains the next property name in an iterator
 */
int get_next_propname(JSValue iter, JSValue *name) {
  if (hash_next(iterator_object_map(iter), &(iterator_iter(iter)), name) == SUCCESS) {
    // printf("in get_next_propname 0: name = %016lx: ", *name); simple_print(*name); printf("\n");
    *name = iterator_object_prop_index(iter, (int)(*name));
    // printf("in get_next_propname 1: name = %016lx: ", *name); simple_print(*name); printf("\n");
    return SUCCESS;
  }
  *name = JS_UNDEFINED;
  return FAIL;
}

#ifdef USE_REGEXP
/*
   sets a regexp's members and makes an Oniguruma's regexp object
 */
int set_regexp_members(Context *ctx, JSValue re, char *pat, int flag) {
  OnigOptionType opt;
  OnigErrorInfo err;
  char *e;

  /*
     The original code in SSJSVM_codeloader.c used ststrdup function,
     which is defined in hash.c.  But I don't know the reason why
     ststrdup is used instead of the standard strdup function.

     regexp_pattern(re) = ststrdup(str);
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
   returns a flag value from a ragexp objext
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
   sets object's member values.
     The sizes of the map and the property table of Takada VM are
     INITIAL_HASH_SIZE (100) and INITIAL_PROPTABLE_SIZE (100), respectively.
     But these sizes seems to be too large for a normal object (e.g.,
     non-builtin object).
     Thus, we changed the definition of set_object_members so that their
     sizes are large for a builtin object and small for another object.
 */

/*
   The following arrays are sizes of map (hash) and property table.
   The subscript given to these arrays is either PHASE_INIT (0) or
   PHASE_VMLOOP (1).  The current phase number is stored in the global
   variable `run_phase'.
 */
// static int hsize_table[] = { HSIZE_BIG, HSIZE_NORMAL };
// static int psize_table[] = { PSIZE_BIG, PSIZE_NORMAL };

/*
   sets members relating to property table
     hsize: size of the hash table
     psize: size of the property table

   If HIDDEN_CLASS is defined, hsize == 0 means that g_hidden_class_0
   should be used instead of allocating a new hidden class.
 */            
void set_object_members(Object *p, int hsize, int psize) {
#ifdef HIDDEN_CLASS
  p->class = ((hsize == 0)?
              gobjects.g_hidden_class_0:
              new_empty_hidden_class(NULL, hsize, HTYPE_GROW));
  hidden_n_enter(p->class)++;
  n_enter_hc++;
#else
  Map *a;
  a = malloc_hashtable();
  hash_create(a, hsize);
  p->map = a;
#endif
  p->prop = allocate_prop_table(psize);
  p->n_props = 0;
  p->limit_props = psize;
}

/*
  makes a simple object whose __proto__ property is not set yet
    hsize: size of the hash table
    psize: size of the array of property values
 */
JSValue new_simple_object_without_prototype(Context *ctx, int hsize, int psize) {
  JSValue ret;
  Object *p;

  //  printf("new_simple_object_without_prototype\n");
  ret = make_simple_object(ctx);
  p = remove_simple_object_tag(ret);
  set_object_members(p, hsize, psize);
  return ret;
}
  
/*
  makes a new simple object
    hsize: size of the hash table
    psize: size of the array of property values
 */
JSValue new_simple_object(Context *ctx, int hsize, int psize) {
  JSValue ret;
  Object *p;

  // printf("new_simple_object\n");
  ret = make_simple_object(ctx);
  p = remove_simple_object_tag(ret);
  set_object_members(p, hsize, psize);
  set___proto___all(ctx, ret, gconsts.g_object_proto);
  return ret;
}

/*
   makes a new array
 */
JSValue new_array(Context *ctx, int hsize, int vsize) {
  JSValue ret;

  ret = make_array(ctx);
  disable_gc();  // disable GC unitl Array is properly initialised
  set_object_members(array_object_p(ret), hsize, vsize);
  set___proto___all(ctx, ret, gconsts.g_array_proto);
  allocate_array_data_critical(ret, 0, 0);
  set_prop_none(ctx, ret, gconsts.g_string_length, FIXNUM_ZERO);
  gc_push_tmp_root(&ret);
  enable_gc(ctx);
  gc_pop_tmp_root(1);
  return ret;
}

/*
   makes a new array with size
 */
JSValue new_array_with_size(Context *ctx, int size, int hsize, int vsize)
{
  JSValue ret;

  ret = make_array(ctx);
  disable_gc();  // disable GC unitl Array is properly initialised
  set_object_members(array_object_p(ret), hsize, vsize);
  allocate_array_data_critical(ret, size, size);
  set_prop_none(ctx, ret, gconsts.g_string_length, int_to_fixnum(size));
  gc_push_tmp_root(&ret);
  enable_gc(ctx);
  gc_pop_tmp_root(1);
  return ret;
}

/*
   makes a function
   The name of this function was formerly new_closure.
 */
JSValue new_function(Context *ctx, Subscript subscr, int hsize, int vsize)
{
  JSValue ret;

  ret = make_function();
  disable_gc();
  set_object_members(func_object_p(ret), hsize, vsize);
  func_table_entry(ret) = &(ctx->function_table[subscr]);
  func_environment(ret) = get_lp(ctx);
  gc_push_tmp_root(&ret);
  enable_gc(ctx);
  set_prototype_none(ctx, ret, new_normal_object(ctx));
  set___proto___none(ctx, ret, gconsts.g_function_proto);
  gc_pop_tmp_root(1);
  return ret;
}

/*
   makes a new built-in function object with constructor
 */
JSValue new_builtin_with_constr(Context *ctx, builtin_function_t f, builtin_function_t cons, int na, int hsize, int psize) {
  JSValue ret;

  ret = make_builtin();
  set_object_members(builtin_object_p(ret), hsize, psize);
  builtin_body(ret) = f;
  builtin_constructor(ret) = cons;
  builtin_n_args(ret) = na;
  set_prototype_none(ctx, ret, new_normal_object(ctx));
  // TODO: g_object_proto should be g_builtin_proto
  set___proto___none(ctx, ret, gconsts.g_object_proto);
  return ret;
}

/*
   makes a new built-in function object
 */
JSValue new_builtin(Context *ctx, builtin_function_t f, int na, int hsize, int psize) {
  return new_builtin_with_constr(ctx, f, builtin_not_a_constructor, na, hsize, psize);
}

/*
   makes an iterator object
 */
JSValue new_iterator(Context *ctx, JSValue obj, int hsize, int vsize) {
  JSValue ret;
  HashIterator *hi;
  HashKey key;

  ret = make_iterator();
  set_object_members(iterator_object_p(ret), hsize, vsize);
  hi = &(iterator_iter(ret));
  do {
#ifdef HIDDEN_CLASS
#ifdef HIDDEN_DEBUG
    print_hidden_class("do in new_iterator", obj_hidden_class(obj));
#endif
    init_hash_iterator(obj_hidden_class_map(obj), hi);
#else
    init_hash_iterator(obj_map(obj), hi);
#endif
    while (next_propname(obj, hi, &key)) {
      set_prop_none(ctx, ret, key, key);
    }
    /*
    if (is_array(obj)) {
      int len, i;
#define N 100;
      char ind[N];
      len = array_length(obj);
      for (i = 0; i < len; i++) {
        snprintf(&ind[0], N - 1, "%d", i);
      }
#undef N
    }
    */
  } while (get___proto__(obj, &obj) == SUCCESS);
  init_hash_iterator(iterator_object_map(ret), hi);
  // print_object_properties(ret);
  return ret;
}

#ifdef USE_REGEXP
/*
   makes a new regexp
 */
JSValue new_regexp(Context *ctx, char *pat, int flag, int hsize, int vsize) {
  JSValue ret;

  ret = make_regexp();
  set_object_members(regexp_object_p(ret), hsize, vsize);
  // pattern field is set in set_regexp_members
  // regexp_pattern(ret) = NULL;
  regexp_reg(ret) = NULL;
  regexp_global(ret) = false;
  regexp_ignorecase(ret) = false;
  regexp_multiline(ret) = false;
  regexp_lastindex(ret) = 0;
  set___proto___none(ctx, ret, gconsts.g_regexp_proto);
  return (set_regexp_members(ctx, ret, pat, flag) == SUCCESS)? ret: JS_UNDEFINED;
}
#endif // USE_REGEXP

/*
   makes a new boxed number
 */
JSValue new_number(Context *ctx, JSValue v, int hsize, int psize) {
  JSValue ret;

  ret = make_number_object();
  set_object_members(boxed_object_p(ret), hsize, psize);
  number_object_value(ret) = v;
  set___proto___none(ctx, ret, gconsts.g_number_proto);
  return ret;
}

/*
   makes a new boxed boolean
 */
JSValue new_boolean(Context *ctx, JSValue v, int hsize, int psize) {
  JSValue ret;

  ret = make_boolean_object();
  set_object_members(boxed_object_p(ret), hsize, psize);
  boolean_object_value(ret) = v;
  set___proto___none(ctx, ret, gconsts.g_boolean_proto);
  return (JSValue)ret;
}

/*
   makes a new boxed string
 */
JSValue new_string(Context *ctx, JSValue v, int hsize, int psize) {
  JSValue ret;

  ret = make_string_object();
  set_object_members(boxed_object_p(ret), hsize, psize);
  string_object_value(ret) = v;
  set___proto___none(ctx, ret, gconsts.g_string_proto);
  // A boxed string has a property ``length'' whose associated value
  // is the length of the string.
  set_prop_all(ctx, ret, gconsts.g_string_length, int_to_fixnum(strlen(string_to_cstr(v))));
  return ret;
}

// data conversion functions
//
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
  allocates a new empty hidden class
 */
HiddenClass *new_empty_hidden_class(Context *ctx, int hsize, int htype) {
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
  gc_push_tmp_root_malloc((void **)&c);
  enable_gc(ctx);
  gc_pop_tmp_root_malloc(1);
  n_hc++;
  return c;
}

/*
  allocates a new hidden class on the basis of oldc and name
 */
HiddenClass *new_hidden_class(Context *ctx, HiddenClass *oldc) {
  HiddenClass *c;
  Map *a;
  int ne;

  c = (HiddenClass *)gc_malloc(ctx, sizeof(HiddenClass), HTAG_HIDDEN_CLASS);
  disable_gc();
  a = malloc_hashtable();
  hash_create(a, oldc->map->size);
  hidden_map(c) = a;
  ne = hash_copy(ctx, hidden_map(oldc), a);
  hidden_n_entries(c) = ne;
  hidden_htype(c) = HTYPE_TRANSIT;
  hidden_n_enter(c) = 0;
  hidden_n_exit(c) = 0;
  gc_push_tmp_root_malloc((void **)&c);
  enable_gc(ctx);
  gc_pop_tmp_root_malloc(1);
  n_hc++;
  return c;
}

void print_hidden_class(char *s, HiddenClass *hc) {
  printf("======= %s start ======\n", s);
  printf("HC: %p (n_entries = %d, htype = %d, n_enter = %d, n_exit = %d)\n",
         hc, hc->n_entries, hc->htype, hc->n_enter, hc->n_exit);
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
        print_hidden_class_recursive(string_to_cstr(p->entry.key), (HiddenClass *)p->entry.data);
    } while ((p = p->next) != NULL);
  }
}

void print_all_hidden_class(void) {
  print_hidden_class_recursive("hidden_class_0", gobjects.g_hidden_class_0);
}

#endif
