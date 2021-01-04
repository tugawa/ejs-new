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

static int rehash(HashTable *table);

static HashCell** alloc_hash_body(Context *ctx, int size)
{
  HashCell **body = (HashCell **) gc_malloc(ctx, sizeof(HashCell*) * size,
                                            CELLT_HASH_BODY);
  int i;
  for (i = 0; i < size; i++)
    body[i] = NULL;
  return body;
}

static HashCell* alloc_hash_cell(Context *ctx)
{
  HashCell *cell = (HashCell *) gc_malloc(ctx,
                                          sizeof(HashCell), CELLT_HASH_CELL);
  cell->next = NULL;
  return cell;
}

/*
 * initializes a hash table with the specified size
 */
HashTable *hash_create(Context *ctx, unsigned int size) {
  HashCell **body;
  HashTable *table;
  int i;

  if (size == 0)
    size = 1;

  table = (HashTable *)gc_malloc(ctx,
                                 sizeof(HashTable), CELLT_HASHTABLE);
  table->body = NULL;  /* tell GC no to follow this pointer */
  GC_PUSH(table);

  body = (HashCell **) gc_malloc(ctx, sizeof(HashCell*) * size,
                                 CELLT_HASH_BODY);
  for (i = 0; i < size; i++)
    body[i] = NULL;
  table->body = body;
  table->size = size;
  table->filled = 0;
  table->entry_count = 0;
  GC_POP(table);
  return table;
}

/*
 * obtains the value and attribute associated with a given key
 */
int hash_get_with_attribute(HashTable *table, HashKey key, HashData *data,
                            Attribute *attr) {
  uint32_t hval;
  HashCell *cell;

  hval = string_hash(key) % table->size;
  for (cell = table->body[hval]; cell != NULL; cell = cell->next)
    if ((JSValue)(cell->entry.key) == key) {
      /* found */
      if (data != NULL) *data = cell->entry.data;
      if (attr != NULL) *attr = cell->entry.attr;
      /* printf("hash_get_with_attr: success, *data = %d\n", *data); */
      return HASH_GET_SUCCESS;
    }
  /* not found */
  /* printf("hash_get_with_attr: fail\n"); */
  return HASH_GET_FAILED;
}

/*
 * registers a value to a hash table under a given key with an attribute
 */
int hash_put_with_attribute(Context *ctx, HashTable* table,
                            HashKey key, HashData data, Attribute attr)
{
  HashCell* cell;
  uint32_t index;

  index = string_hash(key) % table->size;
  for (cell = table->body[index]; cell != NULL; cell = cell->next) {
    if (cell->entry.key == key) {
      /* found */
      if (!is_readonly(cell->entry.attr)) {
        cell->deleted = false;
        cell->entry.data = data;
        cell->entry.attr = attr;
        return HASH_PUT_SUCCESS;
      } else
        return HASH_PUT_FAILED;
    }
  }
  /* not found */
  GC_PUSH(table);
  if (is_transition(attr))
    GC_PUSH(data);
  cell = alloc_hash_cell(ctx);
  cell->next = table->body[index];
  table->body[index] = cell;
  cell->deleted = false;
  cell->entry.key = key;
  cell->entry.data = data;
  cell->entry.attr = attr;
  if (cell->next == NULL) {
    table->entry_count++;
    if (table->entry_count > REHASH_THRESHOLD * table->size)
      rehash(table);
  }
  if (is_transition(attr))
    GC_POP(data);
  GC_POP(table);
  return HASH_PUT_SUCCESS;
}

/*
 * deletes the hash data
 */
int hash_delete(HashTable *table, HashKey key) {
  HashCell *cell, *prev;
  uint32_t index;

  index = string_hash(key) % table->size;
  for (prev = NULL, cell = table->body[index]; cell != NULL;
       prev = cell, cell = cell->next) {
    if (cell->entry.key == key) {
      /* found */
      if (!is_dont_delete(cell->entry.attr))
        return HASH_GET_FAILED;
      if (prev == NULL) {
        table->body[index] = cell->next;
      } else {
        prev->next = cell->next;
      }
      return HASH_GET_SUCCESS;
    }
  }
  return HASH_GET_FAILED;
}

/*
 * copies a hash table
 * This function is used only for copying a hash table in a hidden class.
 * This function returns the number of copied properties.
 */
int hash_copy(Context *ctx, HashTable *from, HashTable *to) {
  int i, fromsize, tosize;
  HashCell *cell, *new;
  uint32_t index;
  int n, ec;

  fromsize = from->size;
  tosize = to->size;
  n = 0;
  ec = 0;
  cell = NULL;
  GC_PUSH3(from, to, cell);
  for (i = 0; i < fromsize; i++) {
    for (cell = from->body[i]; cell != NULL; cell = cell->next) {
      /* we do not copy the transition entry. */
      if (is_transition(cell->entry.attr))
        continue;
      if (cell->deleted)
        continue;
      index = string_hash(cell->entry.key) % tosize;
      new = alloc_hash_cell(ctx);
      new->deleted = false;
      new->entry = cell->entry;
      if (to->body[index] == NULL) ec++;   /* increments entry count */
      new->next = to->body[index];
      to->body[index] = new;
      n++;
    }
  }
  to->entry_count = ec;
  to->filled = from->filled;
  GC_POP3(cell, to, from);
  return n;
}

static int rehash(HashTable *table) {
  int size = table->size;
  int newsize = size * 2;
  HashIterator iter;
  HashCell *p;
  HashCell** newhash;

  GC_PUSH(table);
  newhash = alloc_hash_body(NULL, newsize);
  GC_PUSH(newhash);

  iter = createHashIterator(table);
  while (nextHashCell(table, &iter, &p) != FAIL) {
    uint32_t index = string_hash(p->entry.key) % newsize;
    p->next = newhash[index];
    newhash[index] = p;
  }
  table->body = newhash;
  table->size = newsize;

  GC_POP2(newhash, table);
  return 0;
}

int init_hash_iterator(HashTable *t, HashIterator *h) {
  int i, size;

  size = t->size;
  for (i = 0; i < size; i++) {
    if (t->body[i] != NULL) {
      h->p = t->body[i];
      h->index = i;
      return TRUE;
    }
  }
  h->p = NULL;
  return FALSE;
}

/*
 * Find the next cell.
 * param iter: Startig point of serach.  `iter->p == NULL' directs to
 * start searching from the begining of the list at `iter->index + 1'.
 */
static void advance_iterator(HashTable *table, HashIterator *iter)
{
  HashCell *p;
  int i;
  
  for (p = iter->p; p != NULL; p = p->next)
    if (!p->deleted) {
      iter->p = p;
      return;
    }
  for (i = iter->index + 1; i < table->size; i++)
    for (p = table->body[i]; p != NULL; p = p->next)
      if (!p->deleted) {
        iter->p = p;
        iter->index = i;
        return;
      }
  iter->p = NULL;
}

HashIterator createHashIterator(HashTable *table)
{
  HashIterator iter;

  iter.p = NULL;
  iter.index = -1;
  advance_iterator(table, &iter);
  return iter;
}

int nextHashCell(HashTable *table, HashIterator *iter, HashCell **pp)
{
  advance_iterator(table, iter);  /* Save the case the current cell is deleted
                                   * after the last call of `nextHashCell'. */
  if (iter->p == NULL)
    return FAIL;
  *pp = iter->p;
  iter->p = iter->p->next;
  advance_iterator(table, iter);
  return SUCCESS;
}

/*
 * prints a hash table (for debugging)
 */
void print_hash_table(HashTable *tab) {
  HashCell *p;
  unsigned int i, ec;

  printf("HashTable %p: size = %d, entry_count = %d\n",
         tab, tab->size, tab->entry_count);
  ec = 0;
  for (i = 0; i < tab->size; i++) {
    if ((p = tab->body[i]) == NULL) continue;
    ec++;
    do {
      printf(" (%d: (", i);
      printf("0x%"PRIJSValue" = ", p->entry.key); simple_print(p->entry.key);
      printf(", ");
      printf("0x%"PRIx64, p->entry.data.u.index);
      printf("))\n");
    } while ((p = p->next) != NULL);
    /* if (ec >= tab->entry_count) break; */
  }
  printf("end HashTable\n");

}

/* Local Variables:      */
/* mode: c               */
/* c-basic-offset: 2     */
/* indent-tabs-mode: nil */
/* End:                  */
