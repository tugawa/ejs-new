## Uncomment after editing options and paths
#
# all: ejsvm-with-suffix
#

CC = clang

###
### Options
###

# GC=native|boehmgc|none
OPT_GC=native

# REGEXP=oniguruma|none
OPT_REGEXP=oniguruma

###
### Paths
###

INCLUDES=-I/usr/local/include
LIBS=-L/usr/local/lib


CFLAGS = -std=gnu89 -Wall -Wno-unused-label -Wno-format -g -DUSER_DEF -O3 $(INCLUDES)
LIBS += -lm


SUFFIX:=
ifeq ($(typesgen),true)
	SUFFIX:=$(SUFFIX)_t
	CFLAGS+=-DUSE_TYPES_GENERATED=1
endif

ifeq ($(OPT_GC),native)
	CFLAGS+=-DUSE_NATIVEGC=1
endif
ifeq ($(OPT_GC),boehmgc)
	CFLAGS+=-DUSE_BOEHMGC=1
	LIBS+=-lgc
endif

ifeq ($(OPT_REGEXP),oniguruma)
	CFLAGS+=-DUSE_REGEXP=1
	LIBS+=-lonig
endif


GENERATED_HFILES = \
         instructions-opcode.h \
	 instructions-table.h \
	 instructions-label.h \
	 cell-header.h \
	 types-generated.h

HFILES = $(GENERATED_HFILES) \
         prefix.h \
         context.h \
         header.h \
	 builtin.h \
	 hash.h \
	 instructions.h \
	 types.h \
	 globals.h \
	 extern.h \
	 log.h \
	 gc.h

OFILES = allocate.o \
         builtin-array.o \
         builtin-boolean.o \
         builtin-global.o \
         builtin-math.o \
         builtin-number.o \
         builtin-object.o \
         builtin-regexp.o \
         builtin-string.o \
         call.o \
         codeloader.o \
         context.o \
         conversion.o \
         hash.o \
         init.o \
	 string.o \
         object.o \
         operations.o \
         vmloop.o \
         gc.o \
         main.o

INSN_FILES = \
         insns/add.inc \
         insns/bitand.inc \
         insns/bitor.inc \
         insns/call.inc \
         insns/div.inc \
         insns/end.inc \
         insns/eq.inc \
         insns/equal.inc \
         insns/error.inc \
         insns/fixnum.inc \
         insns/geta.inc \
         insns/getarg.inc \
         insns/geterr.inc \
         insns/getglobal.inc \
         insns/getglobalobj.inc \
         insns/getidx.inc \
         insns/getlocal.inc \
         insns/getprop.inc \
         insns/instanceof.inc \
         insns/isobject.inc \
         insns/isundef.inc \
         insns/jump.inc \
         insns/jumpfalse.inc \
         insns/jumptrue.inc \
         insns/leftshift.inc \
         insns/lessthan.inc \
         insns/lessthanequal.inc \
         insns/localcall.inc \
         insns/localret.inc \
         insns/makeclosure.inc \
         insns/makeiterator.inc \
         insns/mod.inc \
         insns/move.inc \
         insns/mul.inc \
         insns/new.inc \
         insns/newargs.inc \
         insns/nextpropname.inc \
         insns/nop.inc \
         insns/not.inc \
         insns/number.inc \
         insns/pophandler.inc \
         insns/poplocal.inc \
         insns/pushhandler.inc \
         insns/ret.inc \
         insns/rightshift.inc \
         insns/seta.inc \
         insns/setarg.inc \
         insns/setarray.inc \
         insns/setfl.inc \
         insns/setglobal.inc \
         insns/setlocal.inc \
         insns/setprop.inc \
         insns/specconst.inc \
         insns/sub.inc \
         insns/tailcall.inc \
         insns/throw.inc \
         insns/typeof.inc \
         insns/unknown.inc \
         insns/unsignedrightshift.inc

SEDCOM_GEN_INSN_OPCODE = \
  -e 's/^\([a-z][a-z]*\).*/\U\1,/' -e '/^\/\/.*/d'
SEDCOM_GEN_INSN_TABLE = \
  -e 's/^\([a-z][a-z]*\)  *\([A-Z][A-Z]*\).*/  { "\1", \2 },/' -e '/^\/\/.*/d'
SEDCOM_GEN_INSN_LABEL = \
  -e 's/^\([a-z][a-z]*\).*/\&\&I_\U\1,/' -e '/^\/\/.*/d'
SED = gsed
RUBY = ruby

message:
	@echo "This is a template of Makefile.  Copy Makefile to your own"
	@echo "Makefile such as Makefile.mine, edit it, and make with your"
	@echo "own Makefile."
	@echo "   make -f Makefile.mine"

ejsvm-with-suffix: ejsvm$(SUFFIX)

clean: clean$(SUFFIX)

ejsvm$(SUFFIX) :: $(patsubst %.o, %$(SUFFIX).o, $(OFILES))
	$(CC) -o $@ $^ $(LIBS)

instructions-opcode.h: instructions.def
	$(SED) $(SEDCOM_GEN_INSN_OPCODE) instructions.def > $@

instructions-table.h: instructions.def
	$(SED) $(SEDCOM_GEN_INSN_TABLE) instructions.def > $@

instructions-label.h: instructions.def
	$(SED) $(SEDCOM_GEN_INSN_LABEL) instructions.def > $@

instructions.h: instructions-opcode.h instructions-table.h

cell-header.h: cell-header.def
	$(RUBY) $< > $@

vmloop-cases.inc: instructions.def gen-vmloop-cases.rb
	$(RUBY) gen-vmloop-cases.rb < instructions.def > $@

vmloop$(SUFFIX).o: vmloop.c vmloop-cases.inc $(INSN_FILES) $(HFILES)

types-generated.h: datatypes.def
	java -cp vmgen/vmgen.jar TypesGen $< > $@

%$(SUFFIX).o: %.c $(HFILES)
	$(CC) -c $(CFLAGS) -o $@ $<

clean:
	rm -f *.o $(GENERATED_HFILES) vmloop-cases.inc
