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

/* String interface
 *  string_value  -> volatile raw pointer
 *  string_length
 */

static inline uint32_t update_hash(uint32_t hash, const char *s, uint32_t len)
{
  int i;
  for (i = 0; i < len; i++) {
    hash += s[i];
    hash += (hash << 10);
    hash ^= (hash >> 6);
  }
  return hash; 
}

static inline uint32_t finalise_hash(uint32_t hash)
{
  hash += hash << 3;
  hash ^= hash >> 11;
  hash += hash << 15;
  return hash;
}

static
int string_table_lookup2(const char *s1, uint32_t len1,
			 const char *s2, uint32_t len2,
			 uint32_t hash, JSValue *ret)
{
  StrCons *c;
  JSValue v;
  int index;

  index = hash % string_table.size;
  for (c = string_table.obvector[index]; c != NULL; c = c->next) {
    v = c->str;
#ifdef STROBJ_HAS_HASH
    if (string_hash(v) != hash)
      continue;
#endif /* STROBJ_HAS_HASH */
    /* REMARK: assume null termination */
    if (memcmp(s1, string_value(v), len1) == 0 &&
	memcmp(s2, string_value(v) + len1, len2 + 1) == 0) {
      *ret = v;
      return 1; /* found */
    }
  }
  return 0;  /* not found */
}

#define string_table_lookup(s,l,h,r)            \
  (string_table_lookup2((s),(l),"",0,(h),(r)))

static
void string_table_put(Context *context, JSValue v, uint32_t hash)
{
  StrCons *c;
  int index;

  assert(is_string(v));
  
  /* gc_push_tmp_root(&v); */
  GC_PUSH(v);
  c = (StrCons*) gc_malloc(context, sizeof(StrCons), HTAG_STR_CONS);
  c->str = v;
  GC_POP(v);
  index = hash % string_table.size;
  if (string_table.obvector[index] == NULL)
    string_table.count++;
  c->next = string_table.obvector[index];
  string_table.obvector[index] = c;
  /* gc_pop_tmp_root(1); */
}

/*
 * initializes the string table
 */
void init_string_table(unsigned int size) {
  StrCons **a;

  a = (StrCons **)malloc(sizeof(StrCons*) * size);
  memset(a, 0, sizeof(StrCons*) * size);
  string_table.obvector = a;
  string_table.size = size;
  string_table.count = 0;
}

JSValue string_concat_ool(Context *context, JSValue v1, JSValue v2)
{
  uint32_t hash;
  uint32_t len1, len2;
  StringCell *p;
  JSValue v;
  
  assert(is_string(v1));
  assert(is_string(v2));

  len1 = string_length(v1);
  hash = update_hash(0, string_value(v1), len1);
  len2 = string_length(v2);
  hash = update_hash(hash, string_value(v2), len2);
  hash = finalise_hash(hash);

  if (string_table_lookup2(string_value(v1), len1,
			   string_value(v2), len2, hash, &v))
    return v;

  /* gc_push_tmp_root(&v1); */
  /* gc_push_tmp_root(&v2); */
  p = allocate_string(len1 + len2);
#ifdef STROBJ_HAS_HASH
  p->hash = hash;
#endif /* STROBJ_HAS_HASH */
  memcpy(p->value, string_value(v1), len1);
  memcpy(p->value + len1, string_value(v2), len2 + 1);
  v = put_normal_string_tag(p);
  GC_PUSH(v);
  /* gc_push_tmp_root(&v); */
  string_table_put(context, v, hash);
  GC_POP(v);
  /* gc_pop_tmp_root(3); */
  return v;
}

JSValue cstr_to_string_ool(Context *context, const char *s)
{
  uint32_t hash;
  int len;
  StringCell *p;
  JSValue v;

  len = strlen(s);
  hash = update_hash(0, s, len);
  hash = finalise_hash(hash);

  if (string_table_lookup(s, len, hash, &v))
    return v;

  p = allocate_string(len);
#ifdef STROBJ_HAS_HASH
  p->hash = hash;
#endif /* STROBJ_HAS_HASH */
  memcpy(p->value, s, len + 1);
  v = put_normal_string_tag(p);
  /* gc_push_tmp_root(&v); */
  GC_PUSH(v);
  string_table_put(context, v, hash);
  /* gc_pop_tmp_root(1); */
  GC_POP(v);
  return v;
}

#ifdef need_embedded_string
/* assume little endian */
JSValue cstr_to_embedded_string(Context *ctx, char *str)
{
  JSValue v = 0;
  int len = 0;
  char* p = ((char *) &v) + 1;
  for (len = 0; *str != '\0'; len++)
    *p++ = *str++;
  v = put_embedded_string_tag(v);
  v |= len << ESTRING_LENGTH_OFFSET;
  return v;
}

JSValue ejs_embedded_string_concat(Context *ctx, JSValue str1, JSValue str2)
{
  int len = 0;
  JSValue v = 0;
  char* p = ((char *) &v) + 1;
  char* q;
  for (q = string_value(str1); *q != '\0'; len++)
    *p++ = *q++;
  for (q = string_value(str2); *q != '\0'; len++)
    *p++ = *q++;
  v = put_embedded_string_tag(v);
  v |= len << ESTRING_LENGTH_OFFSET;
  return v;
}
#endif /* need_embedded_string */
