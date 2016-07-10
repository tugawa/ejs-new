//
//  allocate.h
//  SSJSVM Project, iwasaki-lab, UEC
//
//  Sho Takada, 2012-13
//  Akira Tanimura, 2012-13
//  Akihiro Urushihara, 2013-14
//  Hideya Iwasaki, 2013
//

#ifndef ALLOCATE_H_
#define ALLOCATE_H_

#include "type.h"

//
#define STRING_TABLE_SIZE  (5000)
#define MINIMUM_ARRAY_SIZE (100)

extern StrTable stringTable;

typedef void (*builtin_function_t)(Context*, int, int);

//


#ifdef __cplusplus
extern "C" {
#endif

void initStringHash();
void initStringTable(unsigned int);
JSValue  allocateNumber(double);
StringCell* allocateString(int);
JSValue  allocateString1(const char*);
JSValue  allocateString2(const char*, const char*);
JSValue  allocateObject();
JSValue  allocateArray();
JSValue* allocateArrayData(int);
JSValue  allocateFunction();
JSValue  allocateIterator();
JSValue* allocatePropTable(int);
JSValue  allocateBuiltin();
JSValue  allocateRegExp();
JSValue  allocateBoxed(BoxType);

#ifdef __cplusplus
}
#endif

#endif // ALLOCATE_H_
