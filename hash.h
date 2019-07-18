/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */

typedef JSValue HashKey;
typedef uint64_t HashData;
typedef uint16_t Attribute;

/* Attributes */
#define ATTR_NONE (0x0)
#define ATTR_RO   (0x1) /* 001 ReadOnly */
#define ATTR_DD   (0x2) /* 010 DontDelete */
#define ATTR_DE   (0x4) /* 100 DontEnum */
#define ATTR_RODD (0x3) /* 011 */
#define ATTR_RODE (0x5) /* 101 */
#define ATTR_DDDE (0x6) /* 110 */
#define ATTR_ALL  (0x7) /* 111 */

#define is_readonly(p)    ((p) & ATTR_RO)
#define is_dont_delete(p) ((p) & ATTR_DD)
#define is_dont_enum(p)   ((p) & ATTR_DE)

#ifdef HIDDEN_CLASS
#define ATTR_TRANSITION  (0x8)  /* pointer to the next hidden class */
#define is_transition(p)  ((p) & ATTR_TRANSITION)
#endif

typedef struct hash_entry {
  HashKey key;       /* key */
  HashData data;     /* value */
  Attribute attr;    /* attribute */
} HashEntry;

typedef struct hash_cell {
  bool deleted;
  HashEntry entry;
  struct hash_cell *next;
} HashCell;

typedef struct hash_iterator {
  int index;
  HashCell *p;
} HashIterator;

typedef struct hash_table {
  HashCell **body;
  unsigned int size;
  unsigned int entry_count;
  unsigned int filled;
} HashTable;

typedef HashTable Map;

/*
 * string table
 */
typedef struct str_cons {
  JSValue str;            /* tagged pointer to a string object */
  struct str_cons *next;  /* pointer to the next strCons */
} StrCons;

typedef struct str_table {
  StrCons **obvector;     /* pointer to an array */
  unsigned int size;      /* # of entries in the array */
  unsigned int count;     /* # of non-NULL entries in the array */
} StrTable;

#define STRING_TABLE_SIZE  5000

#define INTERN_HARD  0
#define INTERN_SOFT  1

#define HASH_GET_SUCCESS  (0)
#define HASH_GET_FAILED   (1)

#define HASH_PUT_SUCCESS  (0)
#define HASH_PUT_FAILED   (1)

