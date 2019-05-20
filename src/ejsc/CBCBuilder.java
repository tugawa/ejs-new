package ejsc;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;

import ejsc.BCBuilder.FunctionBCBuilder;

class CBCBuilder {
    static class FunctionCBCBuilder {
        MCBCSetfl createMCBCSetfl() {
            return new MCBCSetfl(null);
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
        boolean topLevel;
        boolean logging;
        
        List<CBCode> bcodes = new LinkedList<CBCode>();

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

        void insnOrdering() {
            for (int i = 0; i < bcodes.size(); i++) {
                CBCode bcode = bcodes.get(i);
                bcode.number = i;
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
    private BCBuilder convertingTo;
    BCBuilder convertBCode() {
        BCBuilder bcBuilder = new BCBuilder(fBuilders.size());
        convertingTo = bcBuilder;
        this.insnOrdering();
        
        for (int i = 0; i < fBuilders.size(); i++) {
            FunctionCBCBuilder f = fBuilders.get(i);
            FunctionBCBuilder fBCBuilder = bcBuilder.getFunctionBCBuilder(i);
            fBCBuilder.numberOfArgumentRegisters = f.numberOfArgumentRegisters;
            fBCBuilder.numberOfGPRegisters = f.numberOfGPRegisters;
            fBCBuilder.numberOfLocals = f.numberOfLocals;
            fBCBuilder.topLevel = f.topLevel;
            fBCBuilder.logging = f.logging;
            for (CBCode cbcode: f.bcodes) {
                BCode bc = changeToBCode(cbcode);
                if (bc == null)
                    throw new Error("undefined cbc code:" + cbcode.toString());
                fBCBuilder.bcodes.add(bc);
            }
            // Replace label
            for (int j = 0; j < fBCBuilder.bcodes.size(); j++) {
                CBCode cbcode = f.bcodes.get(j);
                BCode bcode = fBCBuilder.bcodes.get(j);
                if (bcode instanceof IJump) {
                    IJump bc = (IJump) bcode;
                    ICBCJump cbc = (ICBCJump) cbcode;
                    BCode label = fBCBuilder.bcodes.get(cbc.label.getDestCBCode().number);
                    bc.label.replaceDestBCode(label);
                    label.labels.add(bc.label);
                }
                if (bcode instanceof IJumptrue) {
                    IJumptrue bc = (IJumptrue) bcode;
                    ICBCJumptrue cbc = (ICBCJumptrue) cbcode;
                    BCode label = fBCBuilder.bcodes.get(cbc.label.getDestCBCode().number);
                    bc.label.replaceDestBCode(label);
                    label.labels.add(bc.label);
                }
                if (bcode instanceof IJumpfalse) {
                    IJumpfalse bc = (IJumpfalse) bcode;
                    ICBCJumpfalse cbc = (ICBCJumpfalse) cbcode;
                    BCode label = fBCBuilder.bcodes.get(cbc.label.getDestCBCode().number);
                    bc.label.replaceDestBCode(label);
                    label.labels.add(bc.label);
                }
                if (bcode instanceof IPushhandler) {
                    IPushhandler bc = (IPushhandler) bcode;
                    ICBCPushhandler cbc = (ICBCPushhandler) cbcode;
                    BCode label = fBCBuilder.bcodes.get(cbc.label.getDestCBCode().number);
                    bc.label.replaceDestBCode(label);
                    label.labels.add(bc.label);
                }
                if (bcode instanceof ILocalcall) {
                    ILocalcall bc = (ILocalcall) bcode;
                    ICBCLocalcall cbc = (ICBCLocalcall) cbcode;
                    BCode label = fBCBuilder.bcodes.get(cbc.label.getDestCBCode().number);
                    bc.label.replaceDestBCode(label);
                    label.labels.add(bc.label);
                }
            }
            // Replace callEntry and sendEntry
            fBCBuilder.setEntry(new Label(), new Label());
            BCode send = fBCBuilder.bcodes.get(f.sendEntry.getDestCBCode().number);
            fBCBuilder.sendEntry.replaceDestBCode(send);
            send.labels.add(fBCBuilder.sendEntry);
            BCode call = fBCBuilder.bcodes.get(f.callEntry.getDestCBCode().number);
            fBCBuilder.callEntry.replaceDestBCode(call);
            call.labels.add(fBCBuilder.callEntry);
        }
        return bcBuilder;
    }

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

    void setJumpDist() {
        for (FunctionCBCBuilder f: fBuilders)
            f.setJumpDist();
    }
 
    void insnOrdering() {
        for (FunctionCBCBuilder f: fBuilders)
            f.insnOrdering();
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

    public void makeSuperInstruction(Main.Info info) {
        boolean global = true;
        for (CBCBuilder.FunctionCBCBuilder fb : fBuilders) {
            if (global) {
                global = false;
                continue;
            }

            SuperInstruction si = new SuperInstruction(fb.bcodes);
            fb.bcodes = si.execMakeSuperInsn();
            if (info.optPrintOptimisation) {
                System.out.println("====== after cbc load sie ======");
                System.out.println(fb);
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
            
        }
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
            /*
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
            */
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

    SrcOperand toSrcOperand(Argument ax) {
        if (ax instanceof ARegister) {
            ARegister a = (ARegister) ax;
            return new RegisterOperand(a.r);
        } else if (ax instanceof AFixnum) {
            AFixnum a = (AFixnum) ax;
            return new FixnumOperand(a.n);
        } else if (ax instanceof ANumber) {
            ANumber a = (ANumber) ax;
            return new FlonumOperand(a.n);
        } else if (ax instanceof AString) {
            AString a = (AString) ax;
            return new StringOperand(a.s);
        } else if (ax instanceof ASpecial) {
            ASpecial a = (ASpecial) ax;
            switch (a.s) {
            case "true":
                return new SpecialOperand(SpecialOperand.V.TRUE);
            case "false":
                return new SpecialOperand(SpecialOperand.V.FALSE);
            case "null":
                return new SpecialOperand(SpecialOperand.V.NULL);
            case "undefined":
                return new SpecialOperand(SpecialOperand.V.UNDEFINED);
            default:
                throw new Error("unknown special");
            }
        } else
            throw new Error("unknown Argment");
    }

    BCode changeToBCode(CBCode cbcx) {
        // Super Instruction
        if (cbcx instanceof ICBCSuperInstruction) {
            ICBCSuperInstruction cbc = (ICBCSuperInstruction) cbcx;
            BCode bc = cbc.originalInsn;
            if (bc == null)
                throw new Error("Base instruction of a superinstruction is unknown: "+cbc);
            if (bc instanceof ISetprop) {
                SrcOperand op1 = toSrcOperand(cbc.store);
                SrcOperand op2 = toSrcOperand(cbc.load1);
                SrcOperand op3 = toSrcOperand(cbc.load2);
                return new ISetprop(op1, op2, op3);
            }
            try {
                // TODO: other type of constructor (only threeop is supported)
                Constructor<? extends BCode> ctor = bc.getClass().getDeclaredConstructor(Register.class, SrcOperand.class, SrcOperand.class);
                Register op1 = ((ARegister) cbc.store).r;
                SrcOperand op2 = toSrcOperand(cbc.load1);
                SrcOperand op3 = toSrcOperand(cbc.load2);
                BCode convertedBC = ctor.newInstance(op1, op2, op3);
                return convertedBC;
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        // Register Register Register
        if (cbcx.store instanceof ARegister && cbcx.load1 instanceof ARegister && cbcx.load2 instanceof ARegister) {
            Register store, load1, load2;
            store = ((ARegister) cbcx.store).r;
            load1 = ((ARegister) cbcx.load1).r;
            load2 = ((ARegister) cbcx.load2).r;
            if (cbcx instanceof ICBCAdd)
                return new IAdd(store, load1, load2);
            if (cbcx instanceof ICBCSub)
                return new ISub(store, load1, load2);
            if (cbcx instanceof ICBCMul)
                return new IMul(store, load1, load2);
            if (cbcx instanceof ICBCDiv)
                return new IDiv(store, load1, load2);
            if (cbcx instanceof ICBCMod)
                return new IMod(store, load1, load2);
            if (cbcx instanceof ICBCBitor)
                return new IBitor(store, load1, load2);
            if (cbcx instanceof ICBCBitand)
                return new IBitand(store, load1, load2);
            if (cbcx instanceof ICBCLeftshift)
                return new ILeftshift(store, load1, load2);
            if (cbcx instanceof ICBCRightshift)
                return new IRightshift(store, load1, load2);
            if (cbcx instanceof ICBCUnsignedrightshift)
                return new IUnsignedrightshift(store, load1, load2);
            if (cbcx instanceof ICBCEqual)
                return new IEqual(store, load1, load2);
            if (cbcx instanceof ICBCEq)
                return new IEq(store, load1, load2);
            if (cbcx instanceof ICBCLessthan)
                return new ILessthan(store, load1, load2);
            if (cbcx instanceof ICBCLessthanequal)
                return new ILessthanequal(store, load1, load2);
            if (cbcx instanceof ICBCInstanceof)
                return new IInstanceof(store, load1, load2);
            if (cbcx instanceof ICBCGetprop)
                return new IGetprop(store, load1, load2);
            if (cbcx instanceof ICBCSetprop)
                return new ISetprop(store, load1, load2);
        }
        // Register Register none
        if (cbcx.store instanceof ARegister && cbcx.load1 instanceof ARegister && cbcx.load2 instanceof ANone) {
            Register store, load1;
            store = cbcx.getDestRegister();
            load1 = ((ARegister) cbcx.load1).r;
            if (cbcx instanceof ICBCNot)
                return new INot(store, load1);
            if (cbcx instanceof ICBCIsundef)
                return new IIsundef(store, load1);
            if (cbcx instanceof ICBCIsobject)
                return new IIsobject(store, load1);
            if (cbcx instanceof ICBCNew)
                return new INew(store, load1);
            if (cbcx instanceof ICBCMakesimpleiterator)
                return new IMakesimpleiterator(load1, store);
            if (cbcx instanceof ICBCNextpropnameidx)
                return new INextpropnameidx(load1, store);
            if (cbcx instanceof ICBCGetglobal)
                return new IGetglobal(store, load1);
        }
        // Register none none
        if (cbcx.store instanceof ARegister && cbcx.load1 instanceof ANone && cbcx.load2 instanceof ANone) {
            Register store;
            store = cbcx.getDestRegister();
            if (cbcx instanceof ICBCGetglobalobj)
                return new IGetglobalobj(store);
            if (cbcx instanceof ICBCGeta)
                return new IGeta(store);
        }
        // none Register none
        if (cbcx.store instanceof ANone && cbcx.load1 instanceof ARegister && cbcx.load2 instanceof ANone) {
            Register load1;
            load1 = ((ARegister) cbcx.load1).r;
            if (cbcx instanceof ICBCThrow)
                return new IThrow(load1);
            if (cbcx instanceof ICBCSeta)
                return new ISeta(load1);
        }
        // none Register Register
        if (cbcx.store instanceof ANone && cbcx.load1 instanceof ARegister && cbcx.load2 instanceof ARegister) {
            Register load1, load2;
            load1 = ((ARegister) cbcx.load1).r;
            load2 = ((ARegister) cbcx.load2).r;
            if (cbcx instanceof ICBCSetglobal)
                return new ISetglobal(load1, load2);
        }
        // none none none
        if (cbcx.store instanceof ANone && cbcx.load1 instanceof ANone && cbcx.load2 instanceof ANone) {
            if (cbcx instanceof ICBCNewargs)
                return new INewargs();
            if (cbcx instanceof ICBCRet)
                return new IRet();
            if (cbcx instanceof ICBCPophandler)
                return new IPophandler();
            if (cbcx instanceof ICBCLocalret)
                return new ILocalret();
            if (cbcx instanceof ICBCPoplocal)
                return new IPoplocal();
        }
        // Register String none
        if (cbcx.store instanceof ARegister && cbcx.load1 instanceof AString && cbcx.load2 instanceof ANone) {
            Register store;
            String load1;
            store = cbcx.getDestRegister();
            load1 = ((AString)cbcx.load1).s;
            if (cbcx instanceof ICBCError)
                return new IError(store, load1);
        }
        // none literal none
        if (cbcx.store instanceof ANone && cbcx.load1 instanceof ALiteral && cbcx.load2 instanceof ANone) {
            int load1 = ((ALiteral) cbcx.load1).n;
            if (cbcx instanceof ICBCSetfl)
                return new ISetfl(load1);
            // jump insn
            if (cbcx instanceof ICBCPushhandler)
                return new IPushhandler(new Label());
            if (cbcx instanceof ICBCLocalcall)
                return new ILocalcall(new Label());
            if (cbcx instanceof ICBCJump)
                return new IJump(new Label());
        }
        // none literal literal
        if (cbcx.store instanceof ANone && cbcx.load1 instanceof ALiteral && cbcx.load2 instanceof ALiteral) {
            int load1 = ((ALiteral) cbcx.load1).n;
            int load2 = ((ALiteral) cbcx.load2).n;
            if (cbcx instanceof ICBCNewframe)
                return new INewframe(load1, load2 == 0 ? false : true);
        }
        // Register literal none
        if (cbcx.store instanceof ARegister && cbcx.load1 instanceof ALiteral && cbcx.load2 instanceof ANone) {
            Register store;
            int load1;
            store = cbcx.getDestRegister();
            load1 = ((ALiteral) cbcx.load1).n;
            if (cbcx instanceof ICBCMakeclosure)
                return new IMakeclosure(store, convertingTo.getFunctionBCBuilder(load1));
        }
        // Register literal literal
        if (cbcx.store instanceof ARegister && cbcx.load1 instanceof ALiteral && cbcx.load2 instanceof ALiteral) {
            Register store;
            int load1, load2;
            store = cbcx.getDestRegister();
            load1 = ((ALiteral) cbcx.load1).n;
            load2 = ((ALiteral) cbcx.load2).n;
            if (cbcx instanceof ICBCGetlocal)
                return new IGetlocal(store, load1, load2);
            if (cbcx instanceof ICBCGetarg)
                return new IGetarg(store, load1, load2);
        }
        // literal literal Register
        if (cbcx.store instanceof ALiteral && cbcx.load1 instanceof ALiteral && cbcx.load2 instanceof ARegister) {
            int store, load1;
            Register load2;
            store = ((ALiteral) cbcx.store).n;
            load1 = ((ALiteral) cbcx.load1).n;
            load2 = ((ARegister) cbcx.load2).r;
            if (cbcx instanceof ICBCSetlocal)
                return new ISetlocal(store, load1, load2);
            if (cbcx instanceof ICBCSetarg)
                return new ISetarg(store, load1, load2);
        }
        // none Register literal
        if (cbcx.store instanceof ANone && cbcx.load1 instanceof ARegister && cbcx.load2 instanceof ALiteral) {
            Register load1;
            int load2;
            load1 = ((ARegister)cbcx.load1).r;
            load2 = ((ALiteral) cbcx.load2).n;
            if (cbcx instanceof ICBCCall)
                return new ICall(load1, load2);
            if (cbcx instanceof ICBCSend)
                return new ISend(load1, load2);
            if (cbcx instanceof ICBCNewsend)
                return new INewsend(load1, load2);
            // jump insn
            if (cbcx instanceof ICBCJumptrue)
                return new IJumptrue(new Label(), load1);
            if (cbcx instanceof ICBCJumpfalse)
                return new IJumpfalse(new Label(), load1);
        }

        // MACRO code
        if (cbcx instanceof MCBCSetfl)
            return new MSetfl();
        if (cbcx instanceof MCBCCall) {
            MCBCCall c = (MCBCCall) cbcx;
            return new MCall(c.receiver, c.function, c.args, c.isNew, c.isTail);
        }
        if (cbcx instanceof MCBCParameter)
            return new MParameter(((MCBCParameter) cbcx).dst);

        // convart nop instruction
        if (cbcx instanceof ICBCNop) {
            if (cbcx.store instanceof ARegister) {
                Register store = cbcx.getDestRegister();
                if (cbcx.load1 instanceof ARegister)
                    return new IMove(store, ((ARegister) cbcx.load1).r);
                if (cbcx.load1 instanceof AFixnum)
                    return new IFixnum(store, ((AFixnum) cbcx.load1).n);
                if (cbcx.load1 instanceof AString) {
                    String str = ((AString)cbcx.load1).s;
                    str = str.replaceAll("\\\\n", "\n");
                    str = str.replaceAll("\\\\s", " ");
                    str = str.replaceAll("\\\\\"", "\"");
                    return new IString(store, str);
                }
                if (cbcx.load1 instanceof ANumber)
                    return new INumber(store, ((ANumber)cbcx.load1).n);
                if (cbcx.load1 instanceof ASpecial) {
                    String s = ((ASpecial)cbcx.load1).s;
                    switch(s) {
                    case "true":
                        return new IBooleanconst(store, true);
                    case "false":
                        return new IBooleanconst(store, false);
                    case "null":
                        return new INullconst(store);
                    case "undefined":
                        return new IUndefinedconst(store);
                    }
                }
                if (cbcx.load1 instanceof ARegexp) {
                    ARegexp load1 = (ARegexp) cbcx.load1;
                    return new IRegexp(store, load1.idx, load1.ptn);
                }
            }
        }
        return null;
    }
}
