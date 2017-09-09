
initial-message:
	@echo To build customised eJSVM:
	@echo  0. copy common.mk.template to common.mk, the common part
	@echo  "   "of the Makefile and edit it, if necessary
	@echo  1. make a build directory
	@echo  "     "$ mkdir build_xxx
	@echo  2. copy this Makefile to the build directory
	@echo  "     "$ cp Makefile build_xxx
	@echo  3. edit the Makefile
	@echo  "     "- remove the target showing this message
	@echo  "     "- uncomment the following lines
	@echo  "     "- DATATYPES: datatypes.def
	@echo  "     "- CFLAGS: debug and optimisation options
	@echo  4. make in the build directory

#DATATYPES=../datatypes/default.def
#CFLAGS=-DNDEBUG -UDEBUG -O3
#include ../common.mk

