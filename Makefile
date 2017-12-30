MANIFEST=compiler.mf

all: newejsc.jar

newejsc.jar: src $(MANIFEST)
	ant
	jar cvfm $@ $(MANIFEST) -C bin .
	@echo '***********************************************************'
	@echo '** WARNING: I created newejsc.jar'
	@echo '**          You need to speficy the make target explicitly'
	@echo '**          to create compiler.jar, e.g., type:'
	@echo '**            > make compiler.jar'
	@echo '***********************************************************'

compiler.jar: src $(MANIFEST)
	ant
	jar cvfm $@ $(MANIFEST) -C bin .

clean:
	rm -r ./bin
	rm compiler.jar
