package ejsc;
import java.util.LinkedList;
import java.util.List;

import ejsc.BCBuilder.FunctionBCBuilder;

class CBCBuilder {
    static class FunctionCBCBuilder {
        MCBCSetfl createMCBCSetfl() {
            return new MCBCSetfl();
        }
        
        MCBCCall createMCBCCall(MCall call) {
            int nArgs = call.args.length;
            if (numberOfArgumentRegisters < nArgs)
                numberOfArgumentRegisters = nArgs;
            return new MCBCCall(call);
        }

        CBCLabel callEntry;
        CBCLabel sendEntry;
        int numberOfLocals;
        int numberOfGPRegisters;
        int numberOfArgumentRegisters = 0;
        
        List<CBCode> bcodes = new LinkedList<CBCode>();

        void expandMacro(Main.Info info) {
            int numberOfOutRegisters = NUMBER_OF_LINK_REGISTERS + numberOfArgumentRegisters + 1; /* + 1 for this-object register */
            int totalNumberOfRegisters = numberOfGPRegisters + numberOfOutRegisters;
            
            Register[] argRegs = new Register[numberOfArgumentRegisters + 1];
            for (int i = 0; i < numberOfArgumentRegisters + 1; i++)
                argRegs[i] = new Register(numberOfGPRegisters + NUMBER_OF_LINK_REGISTERS + i + 1); /* + 1 because of 1-origin */
            
            for (int number = 0; number < bcodes.size(); ) {
                CBCode bcode = bcodes.get(number);
                if (bcode instanceof MCBCSetfl) {
                    MCBCSetfl msetfl = (MCBCSetfl) bcode;
                    ISetfl isetfl = new ISetfl(totalNumberOfRegisters);
                    bcodes.set(number, new ICBCSetfl(isetfl));
                    bcodes.get(number).addLabels(msetfl.getLabels());
                    continue;
                } else if (bcode instanceof MCBCCall) {
                    MCBCCall mcall = (MCBCCall) bcode;
                    int pc = number;
                    int nUseArgReg = mcall.args.length + 1;
                    int thisRegOffset = numberOfArgumentRegisters + 1 - nUseArgReg; /* + 1 because of 1-origin */
                    bcodes.remove(number);
                    if (mcall.receiver != null) {
                        IMove bc = new IMove(argRegs[thisRegOffset], mcall.receiver);
                        bcodes.add(pc++, new ICBCNop(new ARegister(bc.dst), new ARegister(bc.src)));
                    }
                    for (int i = 0; i < mcall.args.length; i++) {
                        IMove bc = new IMove(argRegs[thisRegOffset + 1 + i], mcall.args[i]);
                        bcodes.add(pc++, new ICBCNop(new ARegister(bc.dst), new ARegister(bc.src)));
                    }
                    if (mcall.isNew)
                        bcodes.add(pc++, new ICBCNewsend(new INewsend(mcall.args.length, mcall.function)));
                    else if (mcall.receiver == null)
                        bcodes.add(pc++, new ICBCCall(new ICall(mcall.args.length, mcall.function)));
                    else
                        bcodes.add(pc++, new ICBCSend(new ISend(mcall.args.length, mcall.function)));
                    bcodes.get(number).addLabels(mcall.getLabels());
                    continue;
                } else if (bcode instanceof MCBCParameter) {
                    bcodes.remove(number);
                    bcodes.get(number).addLabels(bcode.getLabels());
                    continue;
                }
                number++;
            }
        }

        void setJumpDist() {
            // set jump dist
            for (CBCode bcode : bcodes) {
                if (bcode instanceof ICBCJump)
                    ((ICBCJump) bcode).resolveJumpDist();
                if (bcode instanceof ICBCJumptrue)
                    ((ICBCJumptrue) bcode).resolveJumpDist();
                if (bcode instanceof ICBCJumpfalse)
                    ((ICBCJumpfalse) bcode).resolveJumpDist();
                if (bcode instanceof ICBCPushhandler)
                    ((ICBCPushhandler) bcode).resolveJumpDist();
                if (bcode instanceof ICBCLocalcall)
                    ((ICBCLocalcall) bcode).resolveJumpDist();
            }
        }

        void assignAddress() {
            int number = 0;
            for (CBCode bcode : bcodes) {
                bcode.number = number;
                // opecode + argument num
                number += 2 + bcode.getArgsNum();
            }
        }

        List<CBCode> build(Main.Info info) {
            List<CBCode> result = new LinkedList<CBCode>();
            result.add(new ICBCCallentry(callEntry.dist(0)));
            result.add(new ICBCSendentry(sendEntry.dist(0)));
            result.add(new ICBCNumberOfLocals(numberOfLocals));
            result.add(new ICBCNumberOfInstruction(bcodes.size()));
            int sum = 0;
            for (CBCode bcode : bcodes)
                sum += bcode.getArgsNum();
            result.add(new ICBCNumberOfArgument(sum));
            result.addAll(bcodes);
            return result;
        }
        
        void setEntry(CBCLabel call, CBCLabel send) {
                callEntry = call;
                sendEntry = send;
        }
        
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if (callEntry != null)
                sb.append("callEntry: ").append(callEntry.dist(0)).append(": ").append(callEntry.getDestCBCode()).append("\n");
            if (sendEntry != null)
                sb.append("sendEntry: ").append(sendEntry.dist(0)).append(": ").append(sendEntry.getDestCBCode()).append("\n");
            for (CBCode i: bcodes)
                sb.append(i.number).append(": ").append(i).append("\n");
            return sb.toString();
        }
    }

    static final int NUMBER_OF_LINK_REGISTERS = 4;

    private LinkedList<FunctionCBCBuilder> fBuilders = new LinkedList<FunctionCBCBuilder>();

    void openFunctionBCBuilder(FunctionBCBuilder fBC) {
        FunctionCBCBuilder fCBC = new FunctionCBCBuilder();
        fCBC.numberOfArgumentRegisters = fBC.numberOfArgumentRegisters;
        fCBC.numberOfGPRegisters = fBC.numberOfGPRegisters;
        fCBC.numberOfLocals = fBC.numberOfLocals;
        fBuilders.add(fCBC);
    }

    FunctionCBCBuilder getFunctionCBCBuilder(int index) {
        return fBuilders.get(index);
    }

    void expandMacro(Main.Info info) {
        for (FunctionCBCBuilder f: fBuilders)
            f.expandMacro(info);
    }

    void setJumpDist() {
        for (FunctionCBCBuilder f: fBuilders)
            f.setJumpDist();
    }
 
    void assignAddress() {
        for (FunctionCBCBuilder f: fBuilders)
            f.assignAddress();
    }

    List<CBCode> build(Main.Info info) {
        // build fBuilders.
        List<CBCode> result = new LinkedList<CBCode>();
        result.add(new ICBCFuncLength(fBuilders.size()));
        for (FunctionCBCBuilder fb : fBuilders)
            result.addAll(fb.build(info));
        return result;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (FunctionCBCBuilder f: fBuilders)
            sb.append(f.toString()).append("\n");
              
        return sb.toString();
    }

    // optimisation method
    void optimisation(Main.Info info) {
        boolean global = true;
        for (CBCBuilder.FunctionCBCBuilder fb : fBuilders) {
            if (global) {
                global = false;
                continue;
            }

            if (info.optPrintOptimisation) {
                System.out.println("====== before optimisation CBC ======");
                System.out.println(fb);
            }

            if (info.optCBCSuperInstruction) {
                SuperInstructionElimination sie = new SuperInstructionElimination(fb.bcodes);
                fb.bcodes = sie.execLoadSIE();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after cbc load sie ======");
                    System.out.println(fb);
                }
            }
            if (info.optCBCRedunantInstructionElimination) {
                fb.assignAddress();
                CBCRedundantInstructionElimination rie = new CBCRedundantInstructionElimination(fb.bcodes);
                fb.bcodes = rie.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after cbc rie ======");
                    System.out.println(fb);
                }
            }
            if (info.optCBCSuperInstruction) {
                SuperInstructionElimination sie = new SuperInstructionElimination(fb.bcodes);
                fb.bcodes = sie.execStoreSIE();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after cbc store sie ======");
                    System.out.println(fb);
                }
            }
            if (info.optCBCRedunantInstructionElimination) {
                fb.assignAddress();
                CBCRedundantInstructionElimination rie = new CBCRedundantInstructionElimination(fb.bcodes);
                fb.bcodes = rie.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after cbc rie ======");
                    System.out.println(fb);
                }
            }
            /*
            if (info.optCBCRegisterAssignment) {
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
            */
        }
    }
}
