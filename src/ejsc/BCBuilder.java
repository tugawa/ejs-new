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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ejsc.CBCBuilder.FunctionCBCBuilder;

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

        List<Label>   labelsSetJumpDest   = new LinkedList<Label>();

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

        List<BCode> build() {
            List<BCode> result = new LinkedList<BCode>();
            result.add(new ICallentry(callEntry.dist(0)));
            result.add(new ISendentry(sendEntry.dist(0)));
            result.add(new INumberOfLocals(numberOfLocals));
            result.add(new INumberOfInstruction(bcodes.size()));
            result.addAll(bcodes);
            return result;
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
    
    // TODO: remove this constructor when removing CBC.
    BCBuilder(int n) {
        fBuilders = new LinkedList<FunctionBCBuilder>();
        for (int i = 0; i < n; i++)
            fBuilders.add(new FunctionBCBuilder());
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

    void closeFuncBCBuilder() {
        fbStack.pop();
    }

    CBCBuilder convertBCode() {
        CBCBuilder cbcBuilder = new CBCBuilder();
        this.assignAddress();
        this.assignFunctionIndex(false);
        for (int i = 0; i < fBuilders.size(); i++) {
            FunctionBCBuilder f = fBuilders.get(i);
            cbcBuilder.openFunctionBCBuilder(f);
            FunctionCBCBuilder fCBCBuilder = cbcBuilder.getFunctionCBCBuilder(i);
            fCBCBuilder.topLevel = f.topLevel;
            fCBCBuilder.logging = f.logging;
            for (BCode bcode: f.bcodes) {
                CBCode cbc = changeToCompactBCode(bcode);
                if (cbc == null)
                    throw new Error("undefined cbc code:" + bcode.toString());
                fCBCBuilder.bcodes.add(cbc);
            }
            // Replace label
            for (int j = 0; j < fCBCBuilder.bcodes.size(); j++) {
                BCode bcode = f.bcodes.get(j);
                CBCode cbcode = fCBCBuilder.bcodes.get(j);
                if (cbcode instanceof ICBCJump) {
                    ICBCJump cbc = (ICBCJump) cbcode;
                    IJump bc = (IJump) bcode;
                    CBCode labelCBC = fCBCBuilder.bcodes.get(bc.label.getDestBCode().number);
                    cbc.label.replaceDestCBCode(labelCBC);
                    labelCBC.labels.add(cbc.label);
                }
                if (cbcode instanceof ICBCJumptrue) {
                    ICBCJumptrue cbc = (ICBCJumptrue) cbcode;
                    IJumptrue bc = (IJumptrue) bcode;
                    CBCode labelCBC = fCBCBuilder.bcodes.get(bc.label.getDestBCode().number);
                    cbc.label.replaceDestCBCode(labelCBC);
                    labelCBC.labels.add(cbc.label);
                }
                if (cbcode instanceof ICBCJumpfalse) {
                    ICBCJumpfalse cbc = (ICBCJumpfalse) cbcode;
                    IJumpfalse bc = (IJumpfalse) bcode;
                    CBCode labelCBC = fCBCBuilder.bcodes.get(bc.label.getDestBCode().number);
                    cbc.label.replaceDestCBCode(labelCBC);
                    labelCBC.labels.add(cbc.label);
                }
                if (cbcode instanceof ICBCPushhandler) {
                    ICBCPushhandler cbc = (ICBCPushhandler) cbcode;
                    IPushhandler bc = (IPushhandler) bcode;
                    CBCode labelCBC = fCBCBuilder.bcodes.get(bc.label.getDestBCode().number);
                    cbc.label.replaceDestCBCode(labelCBC);
                    labelCBC.labels.add(cbc.label);
                }
                if (cbcode instanceof ICBCLocalcall) {
                    ICBCLocalcall cbc = (ICBCLocalcall) cbcode;
                    ILocalcall bc = (ILocalcall) bcode;
                    CBCode labelCBC = fCBCBuilder.bcodes.get(bc.label.getDestBCode().number);
                    cbc.label.replaceDestCBCode(labelCBC);
                    labelCBC.labels.add(cbc.label);
                }
            }
            // Replace callEntry and sendEntry
            fCBCBuilder.setEntry(new CBCLabel(), new CBCLabel());
            CBCode send = fCBCBuilder.bcodes.get(f.sendEntry.getDestBCode().number);
            fCBCBuilder.sendEntry.replaceDestCBCode(send);
            send.labels.add(fCBCBuilder.sendEntry);
            CBCode call = fCBCBuilder.bcodes.get(f.callEntry.getDestBCode().number);
            fCBCBuilder.callEntry.replaceDestCBCode(call);
            call.labels.add(fCBCBuilder.callEntry);
        }
        return cbcBuilder;
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
    
    List<BCode> build() {
        // build fBuilders.
        List<BCode> result = new LinkedList<BCode>();

        int nfunc = assignFunctionIndex(true);
        nfunc++; // toplevel function
        result.add(new IFuncLength(nfunc));

        /* top level */
        FunctionBCBuilder first = fBuilders.get(0);
        int topLevelNumberOfInstructions = 0;
        for (FunctionBCBuilder fb : fBuilders) {
            if (fb.topLevel)
                topLevelNumberOfInstructions += fb.getNumberOfInstructions();
        }
        topLevelNumberOfInstructions++; // last iret
        result.add(new ICallentry(first.callEntry.dist(0)));
        result.add(new ISendentry(first.sendEntry.dist(0)));
        result.add(new INumberOfLocals(0));
        result.add(new INumberOfInstruction(topLevelNumberOfInstructions));
        for (FunctionBCBuilder fb : fBuilders) {
            if (fb.topLevel)
                result.addAll(fb.getInstructions());
        }
        result.add(new IRet());

        for (FunctionBCBuilder fb : fBuilders) {
            if (!fb.topLevel)
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

    CBCode changeToCompactBCode(BCode bc) {
        if (bc instanceof IAdd)
            return new ICBCAdd((IAdd) bc);
        if (bc instanceof ISub)
            return new ICBCSub((ISub) bc);
        if (bc instanceof IMul)
            return new ICBCMul((IMul) bc);
        if (bc instanceof IDiv)
            return new ICBCDiv((IDiv) bc);
        if (bc instanceof IMod)
            return new ICBCMod((IMod) bc);
        if (bc instanceof IBitor)
            return new ICBCBitor((IBitor) bc);
        if (bc instanceof IBitand)
            return new ICBCBitand((IBitand) bc);
        if (bc instanceof ILeftshift)
            return new ICBCLeftshift((ILeftshift) bc);
        if (bc instanceof IRightshift)
            return new ICBCRightshift((IRightshift) bc);
        if (bc instanceof IUnsignedrightshift)
            return new ICBCUnsignedrightshift((IUnsignedrightshift) bc);
        if (bc instanceof IEqual)
            return new ICBCEqual((IEqual) bc);
        if (bc instanceof IEq)
            return new ICBCEq((IEq) bc);
        if (bc instanceof ILessthan)
            return new ICBCLessthan((ILessthan) bc);
        if (bc instanceof ILessthanequal)
            return new ICBCLessthanequal((ILessthanequal) bc);
        if (bc instanceof INot)
            return new ICBCNot((INot) bc);
        if (bc instanceof IGetglobalobj)
            return new ICBCGetglobalobj((IGetglobalobj) bc);
        if (bc instanceof INewargs)
            return new ICBCNewargs();
        if (bc instanceof INewframe)
            return new ICBCNewframe((INewframe) bc);
        if (bc instanceof IMakeclosure)
            return new ICBCMakeclosure((IMakeclosure) bc);
        if (bc instanceof IRet)
            return new ICBCRet();
        if (bc instanceof IIsundef)
            return new ICBCIsundef((IIsundef) bc);
        if (bc instanceof IIsobject)
            return new ICBCIsobject((IIsobject) bc);
        if (bc instanceof IInstanceof)
            return new ICBCInstanceof((IInstanceof) bc);
        if (bc instanceof ICall)
            return new ICBCCall((ICall) bc);
        if (bc instanceof ISend)
            return new ICBCSend((ISend) bc);
        if (bc instanceof INew)
            return new ICBCNew((INew) bc);
        if (bc instanceof INewsend)
            return new ICBCNewsend((INewsend) bc);
        if (bc instanceof IMakesimpleiterator)
            return new ICBCMakesimpleiterator((IMakesimpleiterator) bc);
        if (bc instanceof INextpropnameidx)
            return new ICBCNextpropnameidx((INextpropnameidx) bc);
        if (bc instanceof IJump)
            return new ICBCJump((IJump) bc);
        if (bc instanceof IJumptrue)
            return new ICBCJumptrue((IJumptrue) bc);
        if (bc instanceof IJumpfalse)
            return new ICBCJumpfalse((IJumpfalse) bc);
        if (bc instanceof IThrow)
            return new ICBCThrow((IThrow) bc);
        if (bc instanceof IPushhandler)
            return new ICBCPushhandler((IPushhandler) bc);
        if (bc instanceof IPophandler)
            return new ICBCPophandler();
        if (bc instanceof ILocalcall)
            return new ICBCLocalcall((ILocalcall) bc);
        if (bc instanceof ILocalret)
            return new ICBCLocalret();
        if (bc instanceof IPoplocal)
            return new ICBCPoplocal();
        if (bc instanceof ISetfl)
            return new ICBCSetfl((ISetfl) bc);
        if (bc instanceof IError)
            return new ICBCError((IError) bc);
        if (bc instanceof IGetglobal)
            return new ICBCGetglobal((IGetglobal) bc);
        if (bc instanceof ISetglobal)
            return new ICBCSetglobal((ISetglobal) bc);
        if (bc instanceof IGetlocal)
            return new ICBCGetlocal((IGetlocal) bc);
        if (bc instanceof ISetlocal)
            return new ICBCSetlocal((ISetlocal) bc);
        if (bc instanceof IGetarg)
            return new ICBCGetarg((IGetarg) bc);
        if (bc instanceof ISetarg)
            return new ICBCSetarg((ISetarg) bc);
        if (bc instanceof IGetprop)
            return new ICBCGetprop((IGetprop) bc);
        if (bc instanceof ISetprop)
            return new ICBCSetprop((ISetprop) bc);
        if (bc instanceof IGeta)
            return new ICBCGeta((IGeta) bc);
        if (bc instanceof ISeta)
            return new ICBCSeta((ISeta) bc);

        // MACRO code
        if (bc instanceof MSetfl)
            return new MCBCSetfl();
        if (bc instanceof MCall)
            return new MCBCCall((MCall) bc);
        if (bc instanceof MParameter)
            return new MCBCParameter((MParameter) bc);

        // convart nop instruction
        if (bc instanceof IFixnum) {
            IFixnum b = (IFixnum) bc;
            return new ICBCNop(new ARegister(b.dst), new AFixnum(b.n));
        }
        if (bc instanceof IString) {
            IString b = (IString) bc;
            Pattern pt = Pattern.compile("\n");
            Matcher match = pt.matcher(b.str);
            String str = match.replaceAll("\\\\n");
            return new ICBCNop(new ARegister(b.dst), new AString(str));
        }
        if (bc instanceof INumber) {
            INumber b = (INumber) bc;
            return new ICBCNop(new ARegister(b.dst), new ANumber(b.n));
        }
        if (bc instanceof IBooleanconst) {
            IBooleanconst b = (IBooleanconst) bc;
            return new ICBCNop(new ARegister(b.dst), new ASpecial(b.b ? "true" : "false"));
        }
        if (bc instanceof INullconst) {
            INullconst b = (INullconst) bc;
            return new ICBCNop(new ARegister(b.dst), new ASpecial("null"));
        }
        if (bc instanceof IUndefinedconst) {
            IUndefinedconst b = (IUndefinedconst) bc;
            return new ICBCNop(new ARegister(b.dst), new ASpecial("undefined"));
        }
        if (bc instanceof IRegexp) {
            IRegexp b = (IRegexp) bc;
            return new ICBCNop(new ARegister(b.dst), new ARegexp(b.idx, b.ptn));
        }
        if (bc instanceof IMove) {
            IMove b = (IMove) bc;
            if (b.src instanceof RegisterOperand) {
                Register rs = ((RegisterOperand) b.src).x;
                return new ICBCNop(new ARegister(b.dst), new ARegister(rs));
            } else
                throw new Error("not implemented");
        }

        // unsupport instruction
        if (bc instanceof ISetarray) {
            throw new Error("Setarray is unsupported");
        }
        return null;
    }
}
