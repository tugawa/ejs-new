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

#define HASH_SKIP (27)

/* #define REHASH_THRESHOLD (0.5) */
#define REHASH_THRESHOLD (0.9)


/*
 * initializes a hash table with the specified size
 */
HashTable *hash_create(Context *ctx, unsigned int size)
{
  HashTable *table;
  int i;
  table = (HashTable *)
    gc_malloc(ctx, sizeof(HashTable) + sizeof(struct property) * size,
              CELLT_HASHTABLE);
  table->n_props = size; /* TODO: eliminate duplication */
  table->transitions = NULL;
  for (i = 0; i < size; i++) {
    table->entry[i].key = JS_UNDEFINED;
    table->entry[i].attr = 0;
  }

  return table;
}

/*
 * obtains the value and attribute associated with a given key
 */
int hash_get_with_attribute(HashTable *table, HashKey key, HashData *data,
                            Attribute *attr)
{
  int i;

  assert(is_string(key));
  
  for (i = 0; i < table->n_props; i++)
    if (table->entry[i].key == key) {
      assert(!is_transition(table->entry[i].attr));
      if (data != NULL) data->u.index = i;
      if (attr != NULL) *attr = table->entry[i].attr;
      return HASH_GET_SUCCESS;
    }
  if (table->transitions != NULL) {
    TransitionTable *ttable = table->transitions;
    for (i = 0; i < ttable->n_transitions; i++)
      if (ttable->transition[i].key == key) {
        if (data != NULL) data->u.pm = ttable->transition[i].pm;
        if (attr != NULL) *attr = ATTR_TRANSITION;
        return HASH_GET_SUCCESS;
      }
  }

  return HASH_GET_FAILED;
}

/*
 * registers a value to a hash table under a given key with an attribute
 */
int hash_put_property(Context *ctx, HashTable *table,
                      HashKey key, uint32_t index, Attribute attr)
{
  assert(table->entry[index].key == JS_UNDEFINED ||
         table->entry[index].key == key);
  assert(!is_transition(attr));
  
  if (is_readonly(table->entry[index].attr))
    return HASH_PUT_FAILED;
  table->entry[index].key = key;
  table->entry[index].attr = attr;
  return HASH_PUT_SUCCESS;
}

void hash_put_transition(Context *ctx, HashTable *table,
                         HashKey key, PropertyMap *pm)
{
  int i;
  TransitionTable *ttable;
  int n_transitions;
  if (table->transitions == NULL)
    n_transitions = 1;
  else
    n_transitions = table->transitions->n_transitions + 1;
  
  GC_PUSH3(table, key, pm);
  ttable = (TransitionTable *)
    gc_malloc(ctx, sizeof(TransitionTable) +
              sizeof(struct transition) * n_transitions,
              CELLT_TRANSITIONS);
  GC_POP3(pm, key, table);

  for (i = 0; i < n_transitions - 1; i++)
    ttable->transition[i] = table->transitions->transition[i];
  ttable->transition[i].key = key;
  ttable->transition[i].pm = pm;
  ttable->n_transitions = n_transitions;
  table->transitions = ttable;
}

/*
 * deletes the hash data
 */
int hash_delete(HashTable *table, HashKey key) {
  /* TO BE IMPLEMENTED */
  abort();
  return HASH_GET_FAILED;
}

/*
 * copies a hash table
 * This function is used only for copying a hash table in a hidden class.
 * This function returns the number of copied properties.
 */
int hash_copy(Context *ctx, HashTable *from, HashTable *to)
{
  int i, n = 0;
  assert(from->n_props <= to->n_props);
  
  for (i = 0; i < from->n_props; i++) {
    if (from->entry[i].key != JS_UNDEFINED) {
      n++;
      to->entry[i].key = from->entry[i].key;
    }
  }

  return n;
}


HashPropertyIterator createHashPropertyIterator(HashTable *table)
{
  HashPropertyIterator iter;
  iter.i = 0;
  return iter;
}

int nextHashPropertyCell(HashTable *table,
                         HashPropertyIterator *iter,
                         JSValue *key,
                         uint32_t *index,
                         Attribute *attr)
{
  if (iter->i < table->n_props) {
    if (table->entry[iter->i].key != JS_UNDEFINED) {
      *key = table->entry[iter->i].key;
      *index = iter->i;
      *attr = table->entry[iter->i].attr;
      iter->i++;
      return SUCCESS;
    }
    iter->i++;
  }
  return FAIL;
}

HashTransitionIterator createHashTransitionIterator(HashTable *table)
{
  HashTransitionIterator iter;
  iter.i = 0;
  return iter;
}

int nextHashTransitionCell(HashTable *table,
                           HashTransitionIterator *iter,
                           HashTransitionCell **pp)
{
  if (table->transitions == NULL)
    return FAIL;
  while (iter->i < table->transitions->n_transitions) {
    if (table->transitions->transition[iter->i].key != JS_UNDEFINED) {
      *pp = &table->transitions->transition[iter->i];
      iter->i++;
      return SUCCESS;
    }
    iter->i++;
  }
  return FAIL;
}

/*
 * prints a hash table (for debugging)
 */
void print_hash_table(HashTable *tab)
{
  /* TODO */
}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
