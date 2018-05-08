MANIFEST=compiler.mf

all: newejsc.jar

newejsc.jar: src $(MANIFEST)
	ant
	jar cvfm $@ $(MANIFEST) -C bin .
	@echo '***********************************************************'
	@echo '** newejsc.jar is created.'
	@echo '**'
	@echo '** WARNING: compiler.jar is no longer created automatically.'
	@echo '**          When you are developing ejsc, you should use'
	@echo '**          newejsc.jar, which is not tracked by git.'
	@echo '**          DO NOT CHANGE compiler.jar except when you merged'
	@echo '**          your change to the develop branch.  Latest stable'
	@echo '**          version of compiler.jar should be tracked by git'
	@echo '**          so that ejsvm can always be tested with a stable'
	@echo '**          compiler.'
	@echo '***********************************************************'

compiler.jar: src $(MANIFEST)
	ant
	jar cvfm $@ $(MANIFEST) -C bin .

clean:
	rm -r ./bin
	rm newejsc.jar
