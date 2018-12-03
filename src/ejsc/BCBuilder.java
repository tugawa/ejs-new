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
                    if (info.optCompactByteCode)
                        bcodes.set(number, new ICBCSetfl(isetfl));
                    else
                        bcodes.set(number, isetfl);
            		bcodes.get(number).addLabels(msetfl.getLabels());
            		continue;
            	} else if (bcode instanceof MCall) {
            		MCall mcall = (MCall) bcode;
            		int pc = number;
            		int nUseArgReg = mcall.args.length + 1;
            		int thisRegOffset = numberOfArgumentRegisters + 1 - nUseArgReg; /* + 1 because of 1-origin */
            		bcodes.remove(number);
                    if (info.optCompactByteCode) {
                        if (mcall.receiver != null)
                            bcodes.add(pc++, new ICBCMove(new IMove(argRegs[thisRegOffset], mcall.receiver)));
                        for (int i = 0; i < mcall.args.length; i++)
                            bcodes.add(pc++, new ICBCMove(new IMove(argRegs[thisRegOffset + 1 + i], mcall.args[i])));
                        if (mcall.isNew)
                            bcodes.add(pc++, new ICBCNewsend(new INewsend(mcall.function, mcall.args.length)));
                        else if (mcall.receiver == null)
                            bcodes.add(pc++, new ICBCCall(new ICall(mcall.function, mcall.args.length)));
                        else
                            bcodes.add(pc++, new ICBCSend(new ISend(mcall.function, mcall.args.length)));
                    } else {
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
                    }
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

        void setJumpDist() {
            // set jump dist
            for (BCode bcode : bcodes) {
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

        void assignAddressCBC() {
            int number = 0;
            for (BCode bcode : bcodes) {
                bcode.number = number;
                // opecode + argument num
                number += 2 + bcode.getArgsNum();
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
            if (info.optCompactByteCode) {
                int sum = 0;
                for (BCode bcode : bcodes) {
                    sum += bcode.getArgsNum();
                }
                result.add(new INumberOfArgument(sum));
            }
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

    void transCBC() {
        assignAddress();
        for (FunctionBCBuilder f: fBuilders) {
            for (int i = 0; i < f.bcodes.size(); i++) {
                BCode bcode = f.bcodes.get(i);
                CBCode cbc = changeCBC(bcode);
                if (cbc != null)
                    f.bcodes.set(i, cbc);
            }
            // Replace label
            for (int i = 0; i < f.bcodes.size(); i++) {
                BCode bcode = f.bcodes.get(i);
                if (bcode instanceof ICBCJump) {
                    ICBCJump cbc = (ICBCJump) bcode;
                    BCode bc = f.bcodes.get(cbc.label.getDestBCode().number);
                    cbc.label.replaceDestBCode(bc);
                }
                if (bcode instanceof ICBCJumptrue) {
                    ICBCJumptrue cbc = (ICBCJumptrue) bcode;
                    BCode bc = f.bcodes.get(cbc.label.getDestBCode().number);
                    cbc.label.replaceDestBCode(bc);
                }
                if (bcode instanceof ICBCJumpfalse) {
                    ICBCJumpfalse cbc = (ICBCJumpfalse) bcode;
                    BCode bc = f.bcodes.get(cbc.label.getDestBCode().number);
                    cbc.label.replaceDestBCode(bc);
                }
                if (bcode instanceof ICBCPushhandler) {
                    ICBCPushhandler cbc = (ICBCPushhandler) bcode;
                    BCode bc = f.bcodes.get(cbc.label.getDestBCode().number);
                    cbc.label.replaceDestBCode(bc);
                }
                if (bcode instanceof ICBCLocalcall) {
                    ICBCLocalcall cbc = (ICBCLocalcall) bcode;
                    BCode bc = f.bcodes.get(cbc.label.getDestBCode().number);
                    cbc.label.replaceDestBCode(bc);
                }
            }
            // Replace callEntry and sendEntry
            Label send = new Label(f.bcodes.get(f.sendEntry.getDestBCode().number));
            Label call = new Label(f.bcodes.get(f.callEntry.getDestBCode().number));
            f.setEntry(call, send);
        }
    }

    void expandMacro(Main.Info info) {
        for (FunctionBCBuilder f: fBuilders)
            f.expandMacro(info);
    }

    void setJumpDist() {
        for (FunctionBCBuilder f: fBuilders)
            f.setJumpDist();
    }
 
    void assignAddressCBC() {
        for (FunctionBCBuilder f: fBuilders)
            f.assignAddressCBC();
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

    void optimisationCBC(Main.Info info) {
        for (BCBuilder.FunctionBCBuilder fb : fBuilders) {
            if (info.optPrintOptimisation) {
                System.out.println("====== before optimisation CBC ======");
                System.out.println(fb);
            }

            if (info.optSuperInstruction) {
                SuperInstructionElimination sie = new SuperInstructionElimination(fb.bcodes);
                fb.bcodes = sie.exec();
                if (info.optPrintOptimisation) {
                    System.out.println("====== after sie ======");
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

    CBCode changeCBC(BCode bc) {
        if (bc instanceof IFixnum)
            return new ICBCFixnum((IFixnum) bc);
        if (bc instanceof INumber)
            return new ICBCNumber((INumber) bc);
        if (bc instanceof INumber)
            return new ICBCNumber((INumber) bc);
        if (bc instanceof IString)
            return new ICBCString((IString) bc);
        if (bc instanceof IBooleanconst)
            return new ICBCBooleanconst((IBooleanconst) bc);
        if (bc instanceof INullconst)
            return new ICBCNullconst((INullconst) bc);
        if (bc instanceof IUndefinedconst)
            return new ICBCUndefinedconst((IUndefinedconst) bc);
        if (bc instanceof IRegexp)
            return new ICBCRegexp((IRegexp) bc);
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
        if (bc instanceof ISetarray)
            return new ICBCSetarray((ISetarray) bc);
        if (bc instanceof IMakeclosure)
            return new ICBCMakeclosure((IMakeclosure) bc);
        if (bc instanceof IGeta)
            return new ICBCGeta((IGeta) bc);
        if (bc instanceof ISeta)
            return new ICBCSeta((ISeta) bc);
        if (bc instanceof IRet)
            return new ICBCRet();
        if (bc instanceof IMove)
            return new ICBCMove((IMove) bc);
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
        if (bc instanceof INumberOfLocals)
            return new ICBCNumber((INumber) bc);
        if (bc instanceof INumberOfInstruction)
            return new ICBCNumber((INumber) bc);
        if (bc instanceof INumberOfArgument)
            return new ICBCNumber((INumber) bc);
        if (bc instanceof IError)
            return new ICBCError((IError) bc);
        return null;
    }
}
