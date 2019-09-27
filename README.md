# eJS -- JavaScript for embedded systems.

eJS is a framework to generate JavaScript VMs that are specialised
for applications.

## Contents

* `ejsvm` -- eJSVM, JavaScript virtual machine (core)
* `vmdl` -- VMDL compiler to generate specialised type-based dispatcher used in eJSVM
* `vmgen` -- VMgen is the previous version of VM generator. VMDL compiler is the current mainstream.
* `ejsc` -- eJS compiler to compile JavaScript programs into bytecode files executed by eJSVM.
* `ejsi` -- eJS interpreter interface.

## Quick Start

### Check requirements
Following build tools are required.

* C compiler (GCC or Clang)
* JDK 8 or higher
* make
* Ant
* sed
* Ruby
* Python 3

Following tools are optional.

* Coccinelle

### Prepare external libraries

Download the following libraries into `ejsc/libs` if they are not
contained in the repository.

* antlr-4.5.3-complete.jar
* javax.json-1.0.4.jar

### Build

1. Create build directory
```
$ mkdir build.debug
$ copy build.debug
```

2. Create `Makefile` from template `ejsvm/Makefile.template`
```
$ cp ../ejsvm/Makefile.template Makefile
```

3. Edit `Makefile`. The `commands` and `paths` section may need to be changed.

4. Build by `make`.
```
$ make -j
```

5. Followings are generated.
  * `ejsvm` -- VM
  * `ejsc.jar` -- compiler
  * `ejsi` -- Interpreter user interface

### execute

#### Interpreter mode
Simply execute `ejsi` in the build directory.
```
$ ejsi
```

#### Compile & run
The following steps demonstrate compilation and execution of a JavaScript
program `a.js`. Pathes to `ejsc.jar` and `ejsvm` should be replaced
appropriately.

1. compile to SBC style bytecode
```
$ java -jar ejsc.jar -O a.js -o a.sbc
```

2. execute
```
$ ./ejsvm a.sbc
```


