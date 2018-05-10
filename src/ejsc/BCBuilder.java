/*
   BCBuilder.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Takafumi Kataoka, 2017-18
     Tomoharu Ugawa, 2017-18
     Hideya Iwasaki, 2017-18

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/
package ejsc;
import java.util.LinkedList;
import java.util.List;

class BCBuilder {
	static class FunctionBCBuilder {
        MSetfl createMSetfl() {
        	return new MSetfl();
        }
        
        MCall createMCall(Register receiver, Register function, Register[] args, boolean isNew, boolean isTail) {
        	int nArgs = args.length;
        	if (numberOfArgumentRegisters < nArgs)
        		numberOfArgumentRegisters = nArgs;
        	return new MCall(receiver, function, args, isNew, isTail);
        }

        Label callEntry;
        Label sendEntry;
        int numberOfLocals;
        int numberOfGPRegisters;
        int numberOfArgumentRegisters = 0;
        
        List<BCode> bcodes = new LinkedList<BCode>();

        List<Label>   labelsSetJumpDest   = new LinkedList<Label>();

        void expandMacro() {
            int numberOfOutRegisters = NUMBER_OF_LINK_REGISTERS + numberOfArgumentRegisters + 1; /* + 1 for this-object register */
            int totalNumberOfRegisters = numberOfGPRegisters + numberOfOutRegisters;
            
            Register[] argRegs = new Register[numberOfArgumentRegisters + 1];
            for (int i = 0; i < numberOfArgumentRegisters + 1; i++)
            	argRegs[i] = new Register(numberOfGPRegisters + NUMBER_OF_LINK_REGISTERS + i + 1); /* + 1 because of 1-origin */
            
            for (int number = 0; number < bcodes.size(); ) {
            	BCode bcode = bcodes.get(number);
            	if (bcode instanceof MSetfl) {
            		MSetfl msetfl = (MSetfl) bcode;
            		ISetfl isetfl = new ISetfl(totalNumberOfRegisters);
            		bcodes.set(number, isetfl);
            		bcodes.get(number).addLabels(msetfl.getLabels());
            		continue;
            	} else if (bcode instanceof MCall) {
            		MCall mcall = (MCall) bcode;
            		int pc = number;
            		int nUseArgReg = mcall.args.length + 1;
            		int thisRegOffset = numberOfArgumentRegisters + 1 - nUseArgReg; /* + 1 because of 1-origin */
            		bcodes.remove(number);
            		if (mcall.receiver != null)
            			bcodes.add(pc++, new IMove(argRegs[thisRegOffset], mcall.receiver));
            		for (int i = 0; i < mcall.args.length; i++)
            			bcodes.add(pc++, new IMove(argRegs[thisRegOffset + 1 + i], mcall.args[i]));
            		if (mcall.isNew)
            			bcodes.add(pc++, new INewsend(mcall.function, mcall.args.length));
            		else if (mcall.receiver == null)
            			bcodes.add(pc++, new ICall(mcall.function, mcall.args.length));
            		else
            			bcodes.add(pc++, new ISend(mcall.function, mcall.args.length));
            		bcodes.get(number).addLabels(mcall.getLabels());
            		continue;
            	} else if (bcode instanceof MParameter) {
            		bcodes.remove(number);
            		bcodes.get(number).addLabels(bcode.getLabels());
            		continue;
            	}
            	number++;
            }
        }

        void assignAddress() {
            for (int number = 0; number < bcodes.size(); number++)
                bcodes.get(number).number = number;
        }

        List<BCode> build() {
            List<BCode> result = new LinkedList<BCode>();
            result.add(new ICallentry(callEntry.dist(0)));
            result.add(new ISendentry(sendEntry.dist(0)));
            result.add(new INumberOfLocals(numberOfLocals));
            result.add(new INumberOfInstruction(bcodes.size()));
            result.addAll(bcodes);
            return result;
        }
        
        void setEntry(Label call, Label send) {
        		callEntry = call;
        		sendEntry = send;
        }
        
        void setNumberOfGPRegisters(int gpregs) {
        	numberOfGPRegisters = gpregs;
        }
        
        @Override
        public String toString() {
        	StringBuffer sb = new StringBuffer();
        	if (callEntry != null)
        		sb.append("callEntry: ").append(callEntry.dist(0)).append(": ").append(callEntry.getDestBCode()).append("\n");
        	if (sendEntry != null)
            	sb.append("sendEntry: ").append(sendEntry.dist(0)).append(": ").append(sendEntry.getDestBCode()).append("\n");
        	for (BCode i: bcodes)
        		sb.append(i.number).append(": ").append(i).append("\n");
        	return sb.toString();
        }
    }

	static final int NUMBER_OF_LINK_REGISTERS = 4;

    private LinkedList<FunctionBCBuilder> fbStack = new LinkedList<FunctionBCBuilder>();
    private LinkedList<FunctionBCBuilder> fBuilders = new LinkedList<FunctionBCBuilder>();

    int maxNumOfArgsOfCallingFunction = 0;

    void openFunctionBCBuilder() {
        FunctionBCBuilder bcb = new FunctionBCBuilder();
        fbStack.push(bcb);
        fBuilders.add(bcb);
    }

    void closeFuncBCBuilder() {
        fbStack.pop();
    }

    void expandMacro() {
    	for (FunctionBCBuilder f: fBuilders)
    		f.expandMacro();
    }
 
    void assignAddress() {
    	for (FunctionBCBuilder f: fBuilders)
    		f.assignAddress();    	
    }
    
    List<BCode> build() {
        // build fBuilders.
        List<BCode> result = new LinkedList<BCode>();
        result.add(new IFuncLength(fBuilders.size()));
        for (FunctionBCBuilder fb : fBuilders) {
            result.addAll(fb.build());
        }
        return result;
    }

    void push(Label label) {
        fbStack.getFirst().labelsSetJumpDest.add(label);
    }

    void push(BCode bcode) {
        bcode.addLabels(fbStack.getFirst().labelsSetJumpDest);
        fbStack.getFirst().labelsSetJumpDest.clear();
        fbStack.getFirst().bcodes.add(bcode);
    }

    void pushMsetfl() {
    	push(fbStack.getFirst().createMSetfl());
    }
    
    void pushMCall(Register receiver, Register function, Register[] args, boolean isNew, boolean isTail) {
    	push(fbStack.getFirst().createMCall(receiver, function, args,  isNew, isTail));
    }
    
    BCode getLastBCode() {
    		List<BCode> bcodes = fbStack.getFirst().bcodes;
        return bcodes.get(bcodes.size() - 1);
    }

    int getFBIdx() {
        return fBuilders.size() - 2;
    }

    void setEntry(Label call, Label send) {
        this.fbStack.getFirst().setEntry(call, send);
    }

    void setNumberOfLocals(int n) {
        this.fbStack.getFirst().numberOfLocals = n;
    }

    void setNumberOfGPRegisters(int gpregs) {
    	fbStack.getFirst().setNumberOfGPRegisters(gpregs);
    }

    @Override
    public String toString() {
    	StringBuffer sb = new StringBuffer();
    	for (FunctionBCBuilder f: fBuilders)
    		sb.append(f.toString()).append("\n");
    		  
    	return sb.toString();
    }
    
    // optimisation method
    void optimisation(Main.Info info) {
        boolean global = true;
        for (BCBuilder.FunctionBCBuilder fb : fBuilders) {
            if (global) {
                global = false;
                continue;
            }

            if (info.optPrintOptimisation) {
                System.out.println("====== before optimisation ======");
                System.out.println(fb);
            }
            
            if (info.optConstantPropagation) {
                ConstantPropagation cp = new ConstantPropagation(fb.bcodes);
                fb.bcodes = cp.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after const ======");
                    System.out.println(fb);
                }
            }

            if (info.optCommonConstantElimination) {
                CommonConstantElimination cce = new CommonConstantElimination(fb.bcodes);
                fb.bcodes = cce.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after cce ======");
                    System.out.println(fb);
                }
            }

            if (info.optCopyPropagation) {
                CopyPropagation cp = new CopyPropagation(fb.bcodes);
                cp.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after copy ======");
                    System.out.println(fb);
                }

            }

            if (info.optRedunantInstructionElimination) {
                fb.assignAddress();
                RedundantInstructionElimination rie = new RedundantInstructionElimination(fb.bcodes);
                fb.bcodes = rie.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after rie ======");
                    System.out.println(fb);
                }
            }

            if (info.optRegisterAssignment) {
                DeadCodeElimination dce = new DeadCodeElimination(fb.bcodes);
                fb.bcodes = dce.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after dead code elimination ======");
                    System.out.println(fb);
                }
                RegisterAssignment ra = new RegisterAssignment(fb.bcodes, true);
                fb.bcodes = ra.exec();
                int maxr = ra.getMaxRegNum();
                fb.numberOfGPRegisters = maxr;
                if (info.optPrintOptimisation) {
                    System.out.println("====== after reg ======");
                    System.out.println(fb);
                }
            }
            
            if (info.optRedunantInstructionElimination) {
                fb.assignAddress();
                RedundantInstructionElimination rie = new RedundantInstructionElimination(fb.bcodes);
                fb.bcodes = rie.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after rie ======");
                    System.out.println(fb);
                }
            }

            if (info.optRegisterAssignment) {
                DeadCodeElimination dce = new DeadCodeElimination(fb.bcodes);
                fb.bcodes = dce.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after dead code elimination ======");
                    System.out.println(fb);
                }
                RegisterAssignment ra = new RegisterAssignment(fb.bcodes, true);
                fb.bcodes = ra.exec();
                int maxr = ra.getMaxRegNum();
                fb.numberOfGPRegisters = maxr;
                if (info.optPrintOptimisation) {
                    System.out.println("====== after reg ======");
                    System.out.println(fb);
                }
            }

        	}
    }
}
