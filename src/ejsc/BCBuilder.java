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
                    if (mcall.receiver != null)
                        bcodes.add(pc++, new IMove(argRegs[thisRegOffset], mcall.receiver));
                    for (int i = 0; i < mcall.args.length; i++)
                        bcodes.add(pc++, new IMove(argRegs[thisRegOffset + 1 + i], mcall.args[i]));
                    if (mcall.isNew)
                        bcodes.add(pc++, new INewsend(mcall.args.length, mcall.function));
                    else if (mcall.receiver == null)
                        bcodes.add(pc++, new ICall(mcall.args.length, mcall.function));
                    else
                        bcodes.add(pc++, new ISend(mcall.args.length, mcall.function));
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

        List<BCode> build(Main.Info info) {
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
    static final int MAX_CHAR = 127;
    static final int MIN_CHAR = -128;

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

    CBCBuilder convertBCode() {
        CBCBuilder cbcBuilder = new CBCBuilder();
        this.assignAddress();
        for (int i = 0; i < fBuilders.size(); i++) {
            FunctionBCBuilder f = fBuilders.get(i);
            cbcBuilder.openFunctionBCBuilder(f);
            FunctionCBCBuilder fCBCBuilder = cbcBuilder.getFunctionCBCBuilder(i);
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
    
    List<BCode> build(Main.Info info) {
        // build fBuilders.
        List<BCode> result = new LinkedList<BCode>();
        result.add(new IFuncLength(fBuilders.size()));
        for (FunctionBCBuilder fb : fBuilders) {
            result.addAll(fb.build(info));
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

        // MACRO code
        if (bc instanceof MSetfl)
            return new MCBCSetfl();
        if (bc instanceof MCall)
            return new MCBCCall((MCall) bc);
        if (bc instanceof MParameter)
            return new MCBCParameter((MParameter) bc);

        // convart nop instruction
        if (bc instanceof IFixnum) {
            Argument l1;
            IFixnum b = (IFixnum) bc;
            if (b.n >= MIN_CHAR && b.n <= MAX_CHAR)
                l1 = new AShortFixnum(b.n);
            else
                l1 = new AFixnum(b.n);
            return new ICBCNop(new ARegister(b.dst), l1);
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
        if (bc instanceof IGetglobal) {
            IGetglobal b = (IGetglobal) bc;
            return new ICBCNop(new ARegister(b.dst), new AGlobal(b.lit));
        }
        if (bc instanceof ISetglobal) {
            ISetglobal b = (ISetglobal) bc;
            return new ICBCNop(new AGlobal(b.lit), new ARegister(b.src));
        }
        if (bc instanceof IGetlocal) {
            IGetlocal b = (IGetlocal) bc;
            return new ICBCNop(new ARegister(b.dst), new ALocal(b.depth, b.n));
        }
        if (bc instanceof ISetlocal) {
            ISetlocal b = (ISetlocal) bc;
            return new ICBCNop(new ALocal(b.depth, b.n), new ARegister(b.src));
        }
        if (bc instanceof IGetarg) {
            IGetarg b = (IGetarg) bc;
            return new ICBCNop(new ARegister(b.dst), new AArgs(b.depth, b.n));
        }
        if (bc instanceof ISetarg) {
            ISetarg b = (ISetarg) bc;
            return new ICBCNop(new AArgs(b.depth, b.n), new ARegister(b.src));
        }
        if (bc instanceof IGetprop) {
            IGetprop b = (IGetprop) bc;
            return new ICBCNop(new ARegister(b.dst), new AProp(b.obj, b.prop));
        }
        if (bc instanceof ISetprop) {
            ISetprop b = (ISetprop) bc;
            return new ICBCNop(new AProp(b.obj, b.prop), new ARegister(b.src));
        }
        if (bc instanceof ISetarray) {
            ISetarray b = (ISetarray) bc;
            return new ICBCNop(new AArray(b.ary, b.n), new ARegister(b.src));
        }
        if (bc instanceof IMove) {
            IMove b = (IMove) bc;
            return new ICBCNop(new ARegister(b.dst), new ARegister(b.src));
        }
        if (bc instanceof IGeta) {
            IGeta b = (IGeta) bc;
            return new ICBCNop(new ARegister(b.dst), new AAreg());
        }
        if (bc instanceof ISeta) {
            ISeta b = (ISeta) bc;
            return new ICBCNop(new AAreg(), new ARegister(b.src));
        }
        return null;
    }
}
