/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
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
        boolean topLevel = false;  // true if this is the top-level program of a file
        boolean logging = false;
        int index = -1;

        List<BCode> bcodes = new LinkedList<BCode>();
        List<Label> labelsSetJumpDest = new LinkedList<Label>();

        void expandMacro(Main.Info info) {
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
                    // TODO: check superinstruction spec
                    if (mcall.receiver != null) {
                        if (mcall.receiver instanceof RegisterOperand) {
                            RegisterOperand receiver = (RegisterOperand) mcall.receiver;
                            bcodes.add(pc++, new IMove(argRegs[thisRegOffset], receiver.get()));
                        } else
                            throw new Error("not implemented");
                    }
                    for (int i = 0; i < mcall.args.length; i++) {
                        if (mcall.args[i] instanceof RegisterOperand) {
                            RegisterOperand arg = (RegisterOperand) mcall.args[i];
                            bcodes.add(pc++, new IMove(argRegs[thisRegOffset + 1 + i], arg.get()));
                        } else
                            throw new Error("not implemented");
                    }
                    if (mcall.function instanceof RegisterOperand) {
                        RegisterOperand function = (RegisterOperand) mcall.function;
                        if (mcall.isNew)
                            bcodes.add(pc++, new INewsend(function.get(), mcall.args.length));
                        else if (mcall.receiver == null)
                            bcodes.add(pc++, new ICall(function.get(), mcall.args.length));
                        else
                            bcodes.add(pc++, new ISend(function.get(), mcall.args.length));
                    } else
                        throw new Error("not implemented");
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

        /**
         * Append instruction sequence and adjust related meta-data.
         * Parameter fb should be a toplevel function that has not arguments and no locals.
         * Ignore "logging" flag of FunctionBCBuilder. It should be invalid.
         * @param fb function to be appended to this function.
         */
        void append(FunctionBCBuilder fb) {
            assert(fb.topLevel);
            assert(fb.numberOfArgumentRegisters == 0);
            assert(fb.numberOfLocals == 0);

            bcodes.addAll(fb.bcodes);
            numberOfGPRegisters = Math.max(numberOfGPRegisters, fb.numberOfGPRegisters);
        }

        void assignAddress() {
            for (int i = 0; i < bcodes.size(); i++) {
                BCode bcode = bcodes.get(i);
                bcode.number = i;
            }
        }

        void replaceInstructionsForLogging() {
            if (!logging)
                return;
            for (BCode bc: bcodes)
                bc.logInsn();
        }

        int getNumberOfInstructions() {
            return bcodes.size();
        }

        List<BCode> getInstructions() {
            return bcodes;
        }

        void setEntry(Label call, Label send) {
            callEntry = call;
            sendEntry = send;
        }

        void setNumberOfGPRegisters(int gpregs) {
            numberOfGPRegisters = gpregs;
        }

        void setTopLevel() {
            topLevel = true;
        }

        void setLogging(boolean logging) {
            this.logging = logging;
        }

        boolean getLogging() {
            return logging;
        }

        void setIndex(int index) {
            this.index = index;
        }

        int getIndex() {
            if (index == -1)
                return hashCode();  // debug
            return index;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("[").append(hashCode()).append("]\n");
            if (callEntry != null)
                sb.append("callEntry: ").append(callEntry.dist(0)).append(": ").append(callEntry.getDestBCode()).append("\n");
            if (sendEntry != null)
                sb.append("sendEntry: ").append(sendEntry.dist(0)).append(": ").append(sendEntry.getDestBCode()).append("\n");
            if (topLevel)
                sb.append(" toplevel\n");
            if (logging)
                sb.append(" logging\n");
            for (BCode i: bcodes)
                sb.append(i.number).append(": ").append(i).append("\n");
            return sb.toString();
        }
    }

    static final int NUMBER_OF_LINK_REGISTERS = 4;
    static final int MAX_CHAR = 127;
    static final int MIN_CHAR = -128;

    private LinkedList<FunctionBCBuilder> fbStack;
    private LinkedList<FunctionBCBuilder> fBuilders;

    int maxNumOfArgsOfCallingFunction = 0;

    BCBuilder() {
        fbStack = new LinkedList<FunctionBCBuilder>();
        fBuilders = new LinkedList<FunctionBCBuilder>();        
    }

    FunctionBCBuilder openFunctionBCBuilder() {
        FunctionBCBuilder bcb = new FunctionBCBuilder();
        fbStack.push(bcb);
        fBuilders.add(bcb);
        return bcb;
    }

    FunctionBCBuilder getFunctionBCBuilder(int index) {
        return fBuilders.get(index);
    }

    List<FunctionBCBuilder> getFunctionBCBuilders() {
        return fBuilders;
    }

    void closeFuncBCBuilder() {
        fbStack.pop();
    }

    void expandMacro(Main.Info info) {
        for (FunctionBCBuilder f: fBuilders)
            f.expandMacro(info);
    }

    void assignAddress() {
        for (FunctionBCBuilder f: fBuilders)
            f.assignAddress();    	
    }

    void replaceInstructionsForLogging() {
        for (FunctionBCBuilder f: fBuilders)
            f.replaceInstructionsForLogging();
    }

    /*
     * Assigns indexes to functions.
     * If excludeToplevel is true, this keep index of top level function -1.
     * Returns the number of functions that are assigned indexes.
     */
    int assignFunctionIndex(boolean excludeToplevel) {
        int index = 0;
        for (FunctionBCBuilder fb: fBuilders)
            if (!excludeToplevel || !fb.topLevel)
                fb.setIndex(index++);
        return index;
    }

    void mergeTopLevel() {
        FunctionBCBuilder toplevel = null;

        // append second and following toplevel functions to the first one
        for (int i = 0; i < fBuilders.size(); i++) {
            FunctionBCBuilder fb = fBuilders.get(i);
            if (fb.topLevel) {
                if (toplevel == null)
                    toplevel = fb;
                else {
                    toplevel.append(fb);
                    fBuilders.remove(i);
                    i--;
                }
            }
        }
        if (toplevel == null)
            throw new Error("no toplevel function");

        // add IRET
        toplevel.bcodes.add(new IRet());
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

    void setEntry(Label call, Label send) {
        this.fbStack.getFirst().setEntry(call, send);
    }

    void setNumberOfLocals(int n) {
        this.fbStack.getFirst().numberOfLocals = n;
    }

    void setNumberOfGPRegisters(int gpregs) {
        fbStack.getFirst().setNumberOfGPRegisters(gpregs);
    }

    void setTopLevel() {
        fbStack.getFirst().setTopLevel();
    }

    void setLogging(boolean logging) {
        fbStack.getFirst().setLogging(logging);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (FunctionBCBuilder f: fBuilders)
            sb.append(f.toString()).append("\n");

        return sb.toString();
    }

    // optimisation method
    void optimisation(String optPass, boolean verbose) {
        for (String opt: optPass.split(":")) {
            if (opt.equals(""))
                continue;

            long startTime = System.currentTimeMillis();
            boolean global = false;
            for (BCBuilder.FunctionBCBuilder fb : fBuilders) {
                if (global) {
                    global = false;
                    continue;
                }
                if (verbose) {
                    System.out.println("====== BEFORE "+opt+" Optimization ======");
                    System.out.println(fb);
                }

                switch (opt) {
                case "const":
                case "superinsn": {
                    ConstantPropagation cp = new ConstantPropagation(fb.bcodes);
                    fb.bcodes = cp.exec();
                    break;
                }
                case "cce": {
                    CommonConstantElimination cce = new CommonConstantElimination(fb.bcodes);
                    fb.bcodes = cce.exec();
                    break;
                }
                case "copy": {
                    CopyPropagation cp = new CopyPropagation(fb.bcodes);
                    cp.exec();
                    break;
                }
                case "rie": {
                    fb.assignAddress();
                    RedundantInstructionElimination rie = new RedundantInstructionElimination(fb.bcodes);
                    fb.bcodes = rie.exec();
                    break;
                }
                case "dce": {
                    DeadCodeElimination dce = new DeadCodeElimination(fb.bcodes);
                    fb.bcodes = dce.exec();
                    break;
                }
                case "reg": {
                    RegisterAssignment ra = new RegisterAssignment(fb.bcodes, true);
                    fb.bcodes = ra.exec();
                    int maxr = ra.getMaxRegNum();
                    fb.numberOfGPRegisters = maxr;
                    break;
                }
                default:
                    throw new Error("Unknown optimization: "+opt);
                }
                long endTime = System.currentTimeMillis();
                if (verbose) {
                    System.out.println("====== AFTER "+opt+" Optimization ("+(endTime - startTime)+" ms) ======");
                    System.out.println(fb);
                }
            }
        }
    }
}
