## Uncomment after editing options and paths
#
# all: ejsvm
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


CFLAGS = -std=gnu89 -Wall -Wno-format -g -DUSER_DEF -O3 $(INCLUDES)
LIBS += -lm

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
	 cell_header.h

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
         object.o \
         operations.o \
         vmloop.o \
         gc.o \
         main.o

INSN_FILES = \
         insns/add.def \
         insns/bitand.def \
         insns/bitor.def \
         insns/call.def \
         insns/div.def \
         insns/end.def \
         insns/eq.def \
         insns/equal.def \
         insns/error.def \
         insns/fixnum.def \
         insns/geta.def \
         insns/getarg.def \
         insns/geterr.def \
         insns/getglobal.def \
         insns/getglobalobj.def \
         insns/getidx.def \
         insns/getlocal.def \
         insns/getprop.def \
         insns/instanceof.def \
         insns/isobject.def \
         insns/isundef.def \
         insns/jump.def \
         insns/jumpfalse.def \
         insns/jumptrue.def \
         insns/leftshift.def \
         insns/lessthan.def \
         insns/lessthanequal.def \
         insns/localcall.def \
         insns/localret.def \
         insns/makeclosure.def \
         insns/makeiterator.def \
         insns/mod.def \
         insns/move.def \
         insns/mul.def \
         insns/new.def \
         insns/newargs.def \
         insns/nextpropname.def \
         insns/nop.def \
         insns/not.def \
         insns/number.def \
         insns/pophandler.def \
         insns/poplocal.def \
         insns/pushhandler.def \
         insns/ret.def \
         insns/rightshift.def \
         insns/seta.def \
         insns/setarg.def \
         insns/setarray.def \
         insns/setfl.def \
         insns/setglobal.def \
         insns/setlocal.def \
         insns/setprop.def \
         insns/specconst.def \
         insns/sub.def \
         insns/tailcall.def \
         insns/throw.def \
         insns/typeof.def \
         insns/unknown.def \
         insns/unsignedrightshift.def

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

ejsvm :: $(OFILES)
	$(CC) -o $@ $^ $(LIBS)

instructions-opcode.h: instructions.def
	$(SED) $(SEDCOM_GEN_INSN_OPCODE) instructions.def > $@

instructions-table.h: instructions.def
	$(SED) $(SEDCOM_GEN_INSN_TABLE) instructions.def > $@

instructions-label.h: instructions.def
	$(SED) $(SEDCOM_GEN_INSN_LABEL) instructions.def > $@

instructions.h: instructions-opcode.h instructions-table.h

cell_header.h: cell_header.def
	$(RUBY) $< > $@

vmloop-cases.def: instructions.def gen-vmloop-cases.sed
	$(SED) -f gen-vmloop-cases.sed instructions.def > $@

vmloop.o: vmloop.c vmloop-cases.def $(INSN_FILES) $(HFILES)

%.o: %.c $(HFILES)
	$(CC) -c $(CFLAGS) -o $@ $<

clean:
	rm $(OFILES) $(GENERATED_HFILES) vmloop-cases.def
