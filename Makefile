CC = clang
CFLAGS = -std=gnu89 -Wall -Wno-format -g -DUSER_DEF -O3
# CFLAGS = -std=gnu89 -Wall -Wno-format -g -DUSER_DEF -DUSE_REGEXP
# LIBS = -L/usr/local/lib -lc -lm -lonig
LIBS = -L/usr/local/lib -lc -lm -lonig -lgc

CFLAGS += -I/opt/local/include
LIBS   += -L/opt/local/lib

GENERATED_HFILES = \
         instructions-opcode.h \
	 instructions-table.h \
	 instructions-label.h

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
	 gc.h \
	 cell_header.h

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

SEDCOM_GEN_INSN_OPCODE = 's/^\([a-z][a-z]*\).*/\U\1,/'
SEDCOM_GEN_INSN_TABLE  = 's/^\([a-z][a-z]*\)  *\([A-Z][A-Z]*\).*/  { "\1", \2 },/'
SEDCOM_GEN_INSN_LABEL  = 's/^\([a-z][a-z]*\).*/\&\&I_\U\1,/'
SED = gsed
RUBY = ruby

ssjsvm :: $(OFILES)
	$(CC) -o $@ $^ $(LIBS)

instructions-opcode.h: instructions.def
	$(SED) -e $(SEDCOM_GEN_INSN_OPCODE) instructions.def > $@

instructions-table.h: instructions.def
	$(SED) -e $(SEDCOM_GEN_INSN_TABLE) instructions.def > $@

instructions-label.h: instructions.def
	$(SED) -e $(SEDCOM_GEN_INSN_LABEL) instructions.def > $@

instructions.h: instructions-opcode.h instructions-table.h

cell_header.h: cell_header.def
	$(RUBY) $< > $@

%.o: %.c $(HFILES)
	$(CC) -c $(CFLAGS) -o $@ $<

clean:
	rm $(OFILES) $(GENERATED_HFILES)
