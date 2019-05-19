package ejsc;
import java.util.LinkedList;
import java.util.List;

import ejsc.BCBuilder.FunctionBCBuilder;
import ejsc.CBCBuilder.FunctionCBCBuilder;

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

    BCode changeToBCode(CBCode bc) {
        // Super Instruction
        if (bc instanceof ICBCSuperInstruction) {
            ICBCSuperInstruction b = (ICBCSuperInstruction) bc;
            ISuperInstruction isi = new ISuperInstruction(b.name, b.store.toString(), b.load1.toString(), b.load2.toString());
            return isi;
        }
        // Register Register Register
        if (bc.store instanceof ARegister && bc.load1 instanceof ARegister && bc.load2 instanceof ARegister) {
            Register store, load1, load2;
            store = ((ARegister) bc.store).r;
            load1 = ((ARegister) bc.load1).r;
            load2 = ((ARegister) bc.load2).r;
            if (bc instanceof ICBCAdd)
                return new IAdd(store, load1, load2);
            if (bc instanceof ICBCSub)
                return new ISub(store, load1, load2);
            if (bc instanceof ICBCMul)
                return new IMul(store, load1, load2);
            if (bc instanceof ICBCDiv)
                return new IDiv(store, load1, load2);
            if (bc instanceof ICBCMod)
                return new IMod(store, load1, load2);
            if (bc instanceof ICBCBitor)
                return new IBitor(store, load1, load2);
            if (bc instanceof ICBCBitand)
                return new IBitand(store, load1, load2);
            if (bc instanceof ICBCLeftshift)
                return new ILeftshift(store, load1, load2);
            if (bc instanceof ICBCRightshift)
                return new IRightshift(store, load1, load2);
            if (bc instanceof ICBCUnsignedrightshift)
                return new IUnsignedrightshift(store, load1, load2);
            if (bc instanceof ICBCEqual)
                return new IEqual(store, load1, load2);
            if (bc instanceof ICBCEq)
                return new IEq(store, load1, load2);
            if (bc instanceof ICBCLessthan)
                return new ILessthan(store, load1, load2);
            if (bc instanceof ICBCLessthanequal)
                return new ILessthanequal(store, load1, load2);
            if (bc instanceof ICBCInstanceof)
                return new IInstanceof(store, load1, load2);
            if (bc instanceof ICBCGetprop)
                return new IGetprop(store, load1, load2);
            if (bc instanceof ICBCSetprop)
                return new ISetprop(store, load1, load2);
        }
        // Register Register none
        if (bc.store instanceof ARegister && bc.load1 instanceof ARegister && bc.load2 instanceof ANone) {
            Register store, load1;
            store = bc.getDestRegister();
            load1 = ((ARegister) bc.load1).r;
            if (bc instanceof ICBCNot)
                return new INot(store, load1);
            if (bc instanceof ICBCIsundef)
                return new IIsundef(store, load1);
            if (bc instanceof ICBCIsobject)
                return new IIsobject(store, load1);
            if (bc instanceof ICBCNew)
                return new INew(store, load1);
            if (bc instanceof ICBCMakesimpleiterator)
                return new IMakesimpleiterator(load1, store);
            if (bc instanceof ICBCNextpropnameidx)
                return new INextpropnameidx(load1, store);
            if (bc instanceof ICBCGetglobal)
                return new IGetglobal(store, load1);
        }
        // Register none none
        if (bc.store instanceof ARegister && bc.load1 instanceof ANone && bc.load2 instanceof ANone) {
            Register store;
            store = bc.getDestRegister();
            if (bc instanceof ICBCGetglobalobj)
                return new IGetglobalobj(store);
            if (bc instanceof ICBCGeta)
                return new IGeta(store);
        }
        // none Register none
        if (bc.store instanceof ANone && bc.load1 instanceof ARegister && bc.load2 instanceof ANone) {
            Register load1;
            load1 = ((ARegister) bc.load1).r;
            if (bc instanceof ICBCThrow)
                return new IThrow(load1);
            if (bc instanceof ICBCSeta)
                return new ISeta(load1);
        }
        // none Register Register
        if (bc.store instanceof ANone && bc.load1 instanceof ARegister && bc.load2 instanceof ARegister) {
            Register load1, load2;
            load1 = ((ARegister) bc.load1).r;
            load2 = ((ARegister) bc.load2).r;
            if (bc instanceof ICBCSetglobal)
                return new ISetglobal(load1, load2);
        }
        // none none none
        if (bc.store instanceof ANone && bc.load1 instanceof ANone && bc.load2 instanceof ANone) {
            if (bc instanceof ICBCNewargs)
                return new INewargs();
            if (bc instanceof ICBCRet)
                return new IRet();
            if (bc instanceof ICBCPophandler)
                return new IPophandler();
            if (bc instanceof ICBCLocalret)
                return new ILocalret();
            if (bc instanceof ICBCPoplocal)
                return new IPoplocal();
        }
        // Register String none
        if (bc.store instanceof ARegister && bc.load1 instanceof AString && bc.load2 instanceof ANone) {
            Register store;
            String load1;
            store = bc.getDestRegister();
            load1 = ((AString)bc.load1).s;
            if (bc instanceof ICBCError)
                return new IError(store, load1);
        }
        // none literal none
        if (bc.store instanceof ANone && bc.load1 instanceof ALiteral && bc.load2 instanceof ANone) {
            int load1 = ((ALiteral) bc.load1).n;
            if (bc instanceof ICBCSetfl)
                return new ISetfl(load1);
            // jump insn
            if (bc instanceof ICBCPushhandler)
                return new IPushhandler(new Label());
            if (bc instanceof ICBCLocalcall)
                return new ILocalcall(new Label());
            if (bc instanceof ICBCJump)
                return new IJump(new Label());
        }
        // none literal literal
        if (bc.store instanceof ANone && bc.load1 instanceof ALiteral && bc.load2 instanceof ALiteral) {
            int load1 = ((ALiteral) bc.load1).n;
            int load2 = ((ALiteral) bc.load2).n;
            if (bc instanceof ICBCNewframe)
                return new INewframe(load1, load2);
        }
        // Register literal none
        if (bc.store instanceof ARegister && bc.load1 instanceof ALiteral && bc.load2 instanceof ANone) {
            Register store;
            int load1;
            store = bc.getDestRegister();
            load1 = ((ALiteral) bc.load1).n;
            if (bc instanceof ICBCMakeclosure)
                return new IMakeclosure(store, convertingTo.getFunctionBCBuilder(load1));
        }
        // Register literal literal
        if (bc.store instanceof ARegister && bc.load1 instanceof ALiteral && bc.load2 instanceof ALiteral) {
            Register store;
            int load1, load2;
            store = bc.getDestRegister();
            load1 = ((ALiteral) bc.load1).n;
            load2 = ((ALiteral) bc.load2).n;
            if (bc instanceof ICBCGetlocal)
                return new IGetlocal(store, load1, load2);
            if (bc instanceof ICBCGetarg)
                return new IGetarg(store, load1, load2);
        }
        // literal literal Register
        if (bc.store instanceof ALiteral && bc.load1 instanceof ALiteral && bc.load2 instanceof ARegister) {
            int store, load1;
            Register load2;
            store = ((ALiteral) bc.store).n;
            load1 = ((ALiteral) bc.load1).n;
            load2 = ((ARegister) bc.load2).r;
            if (bc instanceof ICBCSetlocal)
                return new ISetlocal(store, load1, load2);
            if (bc instanceof ICBCSetarg)
                return new ISetarg(store, load1, load2);
        }
        // none Register literal
        if (bc.store instanceof ANone && bc.load1 instanceof ARegister && bc.load2 instanceof ALiteral) {
            Register load1;
            int load2;
            load1 = ((ARegister)bc.load1).r;
            load2 = ((ALiteral) bc.load2).n;
            if (bc instanceof ICBCCall)
                return new ICall(load1, load2);
            if (bc instanceof ICBCSend)
                return new ISend(load1, load2);
            if (bc instanceof ICBCNewsend)
                return new INewsend(load1, load2);
            // jump insn
            if (bc instanceof ICBCJumptrue)
                return new IJumptrue(new Label(), load1);
            if (bc instanceof ICBCJumpfalse)
                return new IJumpfalse(new Label(), load1);
        }

        // MACRO code
        if (bc instanceof MCBCSetfl)
            return new MSetfl();
        if (bc instanceof MCBCCall) {
            MCBCCall c = (MCBCCall) bc;
            return new MCall(c.receiver, c.function, c.args, c.isNew, c.isTail);
        }
        if (bc instanceof MCBCParameter)
            return new MParameter(((MCBCParameter) bc).dst);

        // convart nop instruction
        if (bc instanceof ICBCNop) {
            if (bc.store instanceof ARegister) {
                Register store = bc.getDestRegister();
                if (bc.load1 instanceof ARegister)
                    return new IMove(store, ((ARegister) bc.load1).r);
                if (bc.load1 instanceof AFixnum)
                    return new IFixnum(store, ((AFixnum) bc.load1).n);
                if (bc.load1 instanceof AString) {
                    String str = ((AString)bc.load1).s;
                    str = str.replaceAll("\\\\n", "\n");
                    str = str.replaceAll("\\\\s", " ");
                    str = str.replaceAll("\\\\\"", "\"");
                    return new IString(store, str);
                }
                if (bc.load1 instanceof ANumber)
                    return new INumber(store, ((ANumber)bc.load1).n);
                if (bc.load1 instanceof ASpecial) {
                    String s = ((ASpecial)bc.load1).s;
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
                if (bc.load1 instanceof ARegexp) {
                    ARegexp load1 = (ARegexp) bc.load1;
                    return new IRegexp(store, load1.idx, load1.ptn);
                }
            }
        }
        return null;
    }
}
