CC = clang
CFLAGS = -std=gnu89 -Wall -g -DUSER_DEF
LIBS = -lc -lm

HFILES = prefix.h \
         context.h \
         header.h \
	 builtin.h \
	 hash.h \
	 instructions.h \
	 types.h \
	 globals.h \
	 extern.h \
         instructions-opcode.h \
	 instructions-table.h

OFILES = allocate.o \
	 builtin.o \
         codeloader.o \
         context.o \
	 conversion.o \
	 hash.o \
	 init.o \
	 object.o \
	 operations.o \
	 vmloop.o \
	 main.o 

SEDCOM_GEN_INSN_OPCODE = 's/^\([a-z][a-z]*\).*/\U\1,/'
SEDCOM_GEN_INSN_TABLE  = 's/^\([a-z][a-z]*\)  *\([A-Z][A-Z]*\).*/  { "\1", \2 },/'
SEDCOM_GEN_INSN_LABEL  = 's/^\([a-z][a-z]*\).*/\&\&I_\U\1,/'
SED = gsed

ssjsvm :: $(OFILES)
	$(CC) -o $@ $^ $(LIBS)

instructions-opcode.h: instructions.def
	$(SED) -e $(SEDCOM_GEN_INSN_OPCODE) instructions.def > $@

instructions-table.h: instructions.def
	$(SED) -e $(SEDCOM_GEN_INSN_TABLE) instructions.def > $@

instructions-label.h: instructions.def
	$(SED) -e $(SEDCOM_GEN_INSN_LABEL) instructions.def > $@

instructions.h: instructions-opcode.h instructions-table.h

%.o: %.c $(HFILES)
	$(CC) -c $(CFLAGS) -o $@ $<


