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

        MSetfl createMSetfl() {
        	return new MSetfl();
        }

        int callentry = 0;
        int sendentry = 0;
        int numberOfLocals = 0;
        int numberOfGPRegisters;
        int numberOfArgumentRegisters;
        
        LinkedList<BCode> bcodes = new LinkedList<BCode>();

        LinkedList<Label>   labelsSetJumpDest   = new LinkedList<Label>();

        List<BCode> build() {
            int numberOfInstruction = bcodes.size();
            List<BCode> result = new LinkedList<BCode>();
            
            for (int number = 0; number < bcodes.size(); number++) {
            	BCode bcode = bcodes.get(number);
            	if (bcode instanceof MSetfl) {
            		bcode = new ISetfl(numberOfGPRegisters + numberOfArgumentRegisters);
            		bcodes.set(number, bcode);
            	}
                bcode.number = number;
            }
            result.add(new ICallentry(callentry));
            result.add(new ISendentry(sendentry));
            result.add(new INumberOfLocals(numberOfLocals));
            result.add(new INumberOfInstruction(numberOfInstruction));
            result.addAll(bcodes);
            return result;
        }
        
        void setNumberofRegisters(int gpregs, int argregs) {
        	numberOfGPRegisters = gpregs;
        	numberOfArgumentRegisters = argregs;
        }
        
        int getNumberOfRegisters() {
        	return numberOfGPRegisters + numberOfArgumentRegisters;
        }
        
    }

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
        for (Label l : fbStack.getFirst().labelsSetJumpDest) {
            l.bcode = bcode;
        }
        fbStack.getFirst().labelsSetJumpDest.clear();
        fbStack.getFirst().bcodes.add(bcode);
    }

    void pushMsetfl() {
    	push(fbStack.getFirst().createMSetfl());
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

    void setNumberOfRegisters(int gpregs, int argregs) {
    	fbStack.getFirst().setNumberofRegisters(gpregs, argregs);
    }
}