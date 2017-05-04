
.PHONY: all
all: doc.pdf


doc.dvi: doc.tex
	platex doc.tex

doc.pdf: doc.dvi
	dvipdfmx doc.dvi
