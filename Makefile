MANIFEST=compiler.mf

all: compiler.jar

compiler.jar: src $(MANIFEST)
	ant
	jar cvfm $@ $(MANIFEST) -C bin .

clean:
	rm -r ./bin
	rm compiler.jar
