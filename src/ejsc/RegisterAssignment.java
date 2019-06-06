/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class RegisterAssignment {
    static final boolean DEBUG_SHOW_MAKING_ASSIGNMENT = false;
    static final boolean DEBUG_SHOW_FINAL_ASSIGNMENT = false;

    static final boolean ENABLE_COALESCING = true;

    static class RealRegister extends Register {
        private RealRegister(int n) {
            super(n);
        }
    }

    static class RegisterFactory {
        private final HashMap<Integer, RealRegister> regs = new HashMap<Integer, RealRegister>();
        private int maxRegNum = 0;
        RealRegister get(int n) {
            RealRegister r = regs.get(n);
            if (r != null)
                return r;
            r = new RealRegister(n);
            regs.put(n, r);
            if (n >= maxRegNum)
                maxRegNum = n;
            return r;
        }
        public int getMaxRegNum() {
            return maxRegNum;
        }
    }

    final boolean removeMove;
    RegisterFactory rf = new RegisterFactory();
    List<BCode> bcodes;
    LiveRegisterAnalyser lra;
    HashMap<Register, RealRegister> assign = new HashMap<Register, RealRegister>();

    public RegisterAssignment(List<BCode> bcodes, boolean removeMove) {
        this.removeMove = removeMove;
        this.bcodes = bcodes;
        lra = new LiveRegisterAnalyser(bcodes);
    }

    private boolean[] getRegUsageBitmap(BCode bc) {
        boolean[] used = new boolean[rf.getMaxRegNum() + 1];
        for (Register lr: lra.getLiveRegisters(bc)) {
            RealRegister rr = assign.get(lr);
            if (rr != null)
                used[rr.getRegisterNumber()] = true;
        }
        return used;
    }

    private boolean checkConflict(BCode thisBC, Register from, RealRegister to) {
        for (BCode bc: bcodes) {
            if (bc == thisBC)
                continue;
            Set<Register> liveRegs = lra.getLiveRegisters(bc);
            Register dst = bc.getDestRegister();
            if (!liveRegs.contains(from) && (dst == null || dst != from))
                continue;
            for (Register r: liveRegs) {
                if (assign.get(r) == to)
                    return true;
            }
            if (dst != null) {
                if (assign.get(dst) == to)
                    return true;
            }
        }
        return false;
    }

    private void makeAssignment() {
        for (BCode bcx: bcodes) {
            boolean[] used = getRegUsageBitmap(bcx);
            //            for (Register lr: bcx.getSrcRegisters())
            //                if (lr != null && assign.get(lr) == null) {
            //                    System.out.println(bcx+" | r"+lr+" | "+showAssignment());
            //                    throw new Error("source register is not assigned");
            //                }

            if (bcx instanceof MParameter) {
                MParameter bc = (MParameter) bcx;     
                Register dst = bc.dst;
                if (dst.getRegisterNumber() <= rf.getMaxRegNum() && used[dst.getRegisterNumber()])
                    throw new Error("cannot assign register to parameter");
                RealRegister rdst = rf.get(dst.getRegisterNumber());
                assign.put(dst, rdst);
                continue;
            } 

            /* coalesce */
            if (ENABLE_COALESCING && bcx instanceof IMove){
                IMove bc = (IMove) bcx;
                if (bc.src instanceof RegisterOperand) {
                    Register src = ((RegisterOperand) bc.src).get();
                    Register dst = bc.dst;
                    if (assign.get(dst) == null) {
                        RealRegister rsrc = assign.get(src);
                        if (rsrc != null) {
                            if (!checkConflict(bc, dst, rsrc)) {
                                assign.put(dst, rsrc);
                                continue;
                            }
                        } else {
                            int n = 1;
                            for (; n < used.length; n++)
                                if (!used[n] && !checkConflict(bc, dst, rf.get(n)) && !checkConflict(bc, src, rf.get(n)))
                                    break;
                            assign.put(dst, rf.get(n));
                            assign.put(src, rf.get(n));
                            continue;
                        }
                    }
                }
            }

            /* default case */
            Register dst = bcx.getDestRegister();
            if (dst != null && assign.get(dst) == null) {
                int n = 1;
                for (; n < used.length; n++)
                    if (!used[n] && !checkConflict(bcx, dst, rf.get(n)))
                        break;
                if (DEBUG_SHOW_MAKING_ASSIGNMENT) {
                    System.out.println(bcx);
                    System.out.println("put "+dst+" -> "+rf.get(n));
                }
                assign.put(dst, rf.get(n));
            }
        }
    }

    private void replaceSrcOperandField(BCode bc, Field f) throws IllegalArgumentException, IllegalAccessException {
        SrcOperand opx = (SrcOperand) f.get(bc);
        if (opx != null && opx instanceof RegisterOperand) {
            RegisterOperand op = (RegisterOperand) opx;
            Register r = op.get();
            RealRegister rr = assign.get(r);
            if (rr == null)
                throw new Error("internal error");
            op.set(rr);
        }
    }

    private void replaceSrcOperandArrayField(BCode bc, Field f) throws IllegalArgumentException, IllegalAccessException {
        SrcOperand[] ops = (SrcOperand[]) f.get(bc);
        for (int i = 0; i < ops.length; i++) {
            SrcOperand opx = ops[i];
            if (opx != null && opx instanceof RegisterOperand) {
                RegisterOperand op = (RegisterOperand) opx;
                Register r = op.get();
                RealRegister rr = assign.get(r);
                if (rr == null)
                    throw new Error("internal error");
                op.set(rr);
            }
        }
    }

    private void replaceDstRegister(BCode bc) {
        Register r = bc.dst;
        if (r != null) {
            RealRegister rr = assign.get(r);
            if (rr == null)
                throw new Error("internal error");
            bc.dst = rr;
        }
    }

    private List<BCode> replaceRegisters() {
        ArrayList<BCode> newBCodes = new ArrayList<BCode>(bcodes.size());
        List<Label> labels = new ArrayList<Label>();

        for (BCode bcx: bcodes) {
            if (DEBUG_SHOW_FINAL_ASSIGNMENT)
                System.out.println(showAssignment()+": "+LiveRegisterAnalyser.showRegs(lra.getLiveRegisters(bcx))+bcx);
            try {
                Class<? extends BCode> c = bcx.getClass();
                for (Field f: c.getDeclaredFields()) {
                    if (f.getType() == SrcOperand.class)
                        replaceSrcOperandField(bcx, f);
                    else if (f.getType() == SrcOperand[].class)
                        replaceSrcOperandArrayField(bcx, f);
                }
                replaceDstRegister(bcx);

                if (removeMove) {
                    if (bcx instanceof IMove) {
                        IMove bc = (IMove) bcx;
                        if (bc.src instanceof RegisterOperand) {
                            Register src = ((RegisterOperand) bc.src).get();
                            if (src == bc.dst) {
                                labels.addAll(bc.getLabels());
                                continue;
                            }
                        }
                    }
                }
                bcx.addLabels(labels);
                labels.clear();

                newBCodes.add(bcx);
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        return newBCodes;
    }

    public List<BCode> exec() {
        makeAssignment();
        return replaceRegisters();
    }

    public int getMaxRegNum() {
        return rf.getMaxRegNum();
    }

    public String showAssignment() {
        String s = "{";
        for (Register r: assign.keySet()) {
            RealRegister rr = assign.get(r);
            s += "r"+r+"=>r"+rr+" ";
        }
        return s + "}";
    }

    public String showLiveAssignment(BCode bc) {
        String s = "{";
        for (Register r: lra.getLiveRegisters(bc)) {
            RealRegister rr = assign.get(r);
            s += "r"+r+"(r"+rr+") ";
        }
        return s + "}";
    }
}
