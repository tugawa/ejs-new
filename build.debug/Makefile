EJSVM_DIR = ../ejsvm

.PRECIOUS: %.c %.h %.o

##
## configuration files
##

DATATYPES = $(EJSVM_DIR)/datatypes/default.def
OPERANDSPEC = $(EJSVM_DIR)/operand-spec/any.spec
# OPERANDSPEC = $(EJSVM_DIR)/operand-spec/fixnum-unspec.spec
SUPERINSNSPEC = $(EJSVM_DIR)/superinsn-spec/all.si
SUPERINSNTYPE = 4

##
## compile flags
##

# CFLAGS for C compiler and preprocessor
# CXXFLAGS for C++ compiler
# CPPFLAGS for C and  C++ compilers and preprocessor

CPPFLAGS = -O2 -DNDEBUG -UDEBUG $(HEAPSIZE)
# CPPFLAGS = -Os -DNDEBUG -UDEBUG $(HEAPSIZE)
# CPPFLAGS = -O0 -g $(HEAPSIZE)
CFLAGS =
CXXFLAGS =
INSNGEN_FLAGS = -Xgen:pad_cases true -Xcmp:opt_pass MR:S -Xcmp:rand_seed 0

##
## VMDL
##
VMDLC_FLAGS=-T3
USE_VMDL=false
USE_VMDL_INLINE_EXPANSION=false
USE_VMDL_CASE_SPLIT=false
# ICCSPEC=
ICC_PROF=false

##
## commands
##

CC = gcc
CXX = gcc
SED = gsed
# SED = sed
# PYTHON = python
PYTHON = python3
# CPP_VMDL = $(CPP) -E -x c -P
COCCINELLE = spatch
# COCCINELLE = spatch --python python3

##
## paths
##

INCLUDES =
# INCLUDES = -I/opt/local/include
LIBS =
# LIBS = -L/opt/local/lib

##
## options
##

OPT_REGEXP    = none
# OPT_REGEXP    = oniguruma
OPT_GC        = native
#OPT_GC        = bibop
#OPT_GC        = copy
#OPT_GC        = none
#OPT_GC        = threaded

## heap size in bytes
HEAPSIZE      = -DJS_SPACE_BYTES=10485760

## enable profiling to create customised VM
# CFLAGS       += -DPROFILE

## word size
# INSN32/64    -- instruction size
# JSVALUE32/64 -- JSValue size  (>= ponter size)
# ALIGN32/64   -- alignment of heap-allocated data
#CPPFLAGS        += -DBIT_INSN32 -DBIT_ALIGN32 -DBIT_JSVALUE32
CPPFLAGS        += -DBIT_INSN64 -DBIT_ALIGN64 -DBIT_JSVALUE64

##
## developer options
##

## accept SBC files
CPPFLAGS       += -DUSE_SBC
## GC collects internal nodes in hidden class graph
CPPFLAGS       += -DHC_SKIP_INTERNAL
## cache shapes at allocation site
CPPFLAGS       += -DALLOC_SITE_CACHE
## GC collects unused shapes (effective if ALLOC_SITE_CACHE is specified)
CPPFLAGS       += -DWEAK_SHAPE_LIST
## use inlne cache
CPPFLAGS       += -DINLINE_CACHE

## show GC profiling information (make --gc-prof available)
CPPFLAGS       += -DGC_PROF
## show hidden class related profiling information (make --hc-prof available)
CPPFLAGS       += -DHC_PROF
## show inline cache statistics
CPPFLAGS       += -DIC_PROF
## show allocation site cache statistics
CPPFLAGS       += -DAS_PROF
## show shape statisics
CPPFLAGS       += -DSHAPE_PROF


## dump hidden class graph
CPPFLAGS       += -DDUMP_HCG
## load hidden class graph from file (unstable)
CPPFLAGS       += -DLOAD_HCG

## print flonum usage
#CPPFLAGS       += -DFLONUM_PROF

## use flonum space
#CPPFLAGS       += -DFLONUM_SPACE
## use explicit mark stack rather than recursive call
#CPPFLAGS      += -DMARK_STACK

##
## MS options
##

## use 32bit object header (for BIT_JSVALUE32 env)
#CPPFLAGS       += -DGC_MS_HEADER32

##
## BiBoP options
##

## page caches the size of bitmaps
CFLAGS       += -DBIBOP_CACHE_BMP_GRANULES
## free pages of size one is linked in a separate list
CPPFLAGS       += -DBIBOP_SEGREGATE_1PAGE
## allocate pages of size one from the bottom of the heap
CPPFLAGS       += -DBIBOP_2WAY_ALLOC
## link free blocks in a page to form a freelist
CPPFLAGS       += -DBIBOP_FREELIST
## use flonum pages as if it is a hashtable to encourage sahreing flonum objects
#CPPFLAGS       += -DFLONUM_SPACE
## debug
CPPFLAGS       += -DVERIFY_BIBOP

##
## Threaded compactor options
##

## use boundary tag in Hidden Class
CPPFLAGS       += -DGC_THREADED_BOUNDARY_TAG
## use 32bit object header (for BIT_JSVALUE32 env)
#CPPFLAGS       += -DGC_THREADED_HEADER32


##
## debug/sanity check options
##

## show values of properties in GETPROP/SETPROP instructions
#CPPFLAGS   += -DDEBUG_GETPROP_VALUE

## fix rand seed
CPPFLAGS    += -DNO_SRAND

include $(EJSVM_DIR)/common.mk
