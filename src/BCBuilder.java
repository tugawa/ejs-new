import java.util.LinkedList;
import java.util.List;

class BCBuilder {
    static class FunctionBCBuilder {
        static class MSetfl extends BCode {
        	MSetfl() {}
        	@Override
			public String toString() {
        		return "@MACRO setfl";
        	}
        }

        static class MCall extends BCode {
        	Register receiver;
        	Register function;
        	Register[] args;
        	boolean isNew;
        	boolean isTail;
        	MCall(Register receiver, Register function, Register[] args, boolean isNew, boolean isTail) {
        		this.receiver = receiver;
        		this.function = function;
        		this.args = args;
        		this.isNew = isNew;
        		this.isTail = isTail;
        	}
        	@Override
        	public String toString() {
        		String s ="@MACRO call";
        		if (isTail)
        			s += " [tail]";
        		s += (receiver == null ? " function" : (" method " + receiver));
        		s += " " + function;
        		for (Register r: args)
        			s += " " + r;
        		return s;
        	}
        }
        
        MSetfl createMSetfl() {
        	return new MSetfl();
        }
        
        MCall createMCall(Register receiver, Register function, Register[] args, boolean isNew, boolean isTail) {
        	int nArgs = args.length;
        	if (numberOfArgumentRegisters < nArgs)
        		numberOfArgumentRegisters = nArgs;
        	return new MCall(receiver, function, args, isNew, isTail);
        }

        int callentry = 0;
        int sendentry = 0;
        int numberOfLocals;
        int numberOfGPRegisters;
        int numberOfArgumentRegisters = 0;
        
        LinkedList<BCode> bcodes = new LinkedList<BCode>();

        LinkedList<Label>   labelsSetJumpDest   = new LinkedList<Label>();

        List<BCode> build() {
            List<BCode> result = new LinkedList<BCode>();
            int numberOfOutRegisters = NUMBER_OF_LINK_REGISTERS + numberOfArgumentRegisters + 1; /* + 1 for this-object register */
            int totalNumberOfRegisters = numberOfGPRegisters + numberOfOutRegisters;
            
            Register[] argRegs = new Register[numberOfArgumentRegisters + 1];
            for (int i = 0; i < numberOfArgumentRegisters + 1; i++)
            	argRegs[i] = new Register(numberOfGPRegisters + NUMBER_OF_LINK_REGISTERS + i + 1); /* + 1 because of 1-origin */
            
            for (int number = 0; number < bcodes.size(); ) {
            	BCode bcode = bcodes.get(number);
            	/* macro expansion */
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
            	}
            	/* set address */
                bcode.number = number;
                number++;
            }
            
            result.add(new ICallentry(callentry));
            result.add(new ISendentry(sendentry));
            result.add(new INumberOfLocals(numberOfLocals));
            result.add(new INumberOfInstruction(bcodes.size()));
            result.addAll(bcodes);
            return result;
        }
        
        void setNumberOfGPRegisters(int gpregs) {
        	numberOfGPRegisters = gpregs;
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
        fbStack.getFirst().labelsSetJumpDest.push(label);
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
        return this.fbStack.getFirst().bcodes.getLast();
    }

    int getFBIdx() {
        return fBuilders.size() - 2;
    }

    void setSendentry(int n) {
        this.fbStack.getFirst().sendentry = n;
    }

    void setNumberOfLocals(int n) {
        this.fbStack.getFirst().numberOfLocals = n;
    }

    void setNumberOfGPRegisters(int gpregs) {
    	fbStack.getFirst().setNumberOfGPRegisters(gpregs);
    }
}