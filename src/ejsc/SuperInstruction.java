package ejsc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class SuperInstruction {
    class Environment {
        CBCode bc;

        Environment(CBCode bc) {
            this.bc = bc;
        }

        public CBCode lookup(Register src) {
            return findDefinition(bc, src);
        }

        private CBCode findDefinition(CBCode bc, Register src) {
            CBCode result = null;
            for (CBCode def : rdefa.getReachingDefinitions(bc)) {
                Register dst = def.getDestRegister();
                if (dst == src) {
                    if (result == null)
                        result = def;
                    else
                        return null;
                }
            }
            return result;
        }
    }

    CBCode makeSuperInsn(CBCode bc, Insn insn) {
        return evalSuperInsn(new Environment(bc), bc, insn);
    }

    public CBCode evalSuperInsn(Environment env, CBCode bc, Insn insn) {
        return evalCBCode(env, bc, insn);
    }

    protected CBCode evalCBCode(Environment env, CBCode bc, Insn insn) {
//        System.out.println("exec: " + bc.toString());
        Argument load1 = null, load2 = null;
        if (bc.load1 instanceof ARegister) {
            if (insn.load1.equals("reg")) {
                load1 = bc.load1;
            } else {
                ARegister load = (ARegister) bc.load1;
                CBCode b = env.lookup(load.r);
                if ((b instanceof ICBCNop) && insn.isLoad1Instance(b.load1))
                    load1 = b.load1;
            }
        }
        if (bc.load2 instanceof ARegister) {
            if (insn.load2.equals("reg")) {
                load2 = bc.load2;
            } else {
                ARegister load = (ARegister) bc.load2;
                CBCode b = env.lookup(load.r);
//                System.out.println("  " + b);
                if ((b instanceof ICBCNop) && insn.isLoad2Instance(b.load1))
                    load2 = b.load1;
            }
        }
        if (load1 == null || load2 == null)
            return null;
        return new ICBCSuperInstruction(bc.store, load1, load2, insn.newInsn);
    }

    List<CBCode> bcodes;
    CBCControlFlowGraph cfg;
    CBCReachingDefinition rdefa;

    LinkedList<Insn> insns;
    class Insn {
        String name, load1, load2, newInsn;
        Insn(String name, String load1, String load2, String newInsn) {
            this.name = name;
            this.load1 = load1;
            this.load2 = load2;
            this.newInsn = newInsn;
        }
        public boolean isLoad1Instance(Argument load1) {
            return isInstance(this.load1, load1);
        }
        public boolean isLoad2Instance(Argument load2) {
            return isInstance(this.load2, load2);
        }
        private boolean isInstance(String str, Argument load) {
            switch(str) {
            case "fix":
                if (load instanceof AFixnum && ((AFixnum) load).n < (1 << 16))
                    return true;
                return false;
            case "str":
                return load instanceof AString;
            case "number":
                return load instanceof ANumber;
            default:
                return false;
            }
        }
        public String toString() {
            return name + "(" + load1 + "," + load2 + "):" + newInsn;
        }
    }

    SuperInstruction(List<CBCode> bcodes, String specFile) throws FileNotFoundException {
        this.bcodes = bcodes;
        cfg = new CBCControlFlowGraph(bcodes);
        rdefa = new CBCReachingDefinition(bcodes);
        insns = new LinkedList<Insn>();
        Scanner sc = new Scanner(new FileInputStream(specFile));
        while (sc.hasNextLine())
            insns.add(loadInsn(sc.nextLine()));
        sc.close();
    }

    private Insn loadInsn(String insnDef) {
        insnDef = insnDef.replace(" ", "");
        int insnIndex = insnDef.indexOf('(');
        int op1Index = insnDef.indexOf(',');
        int op2Index = insnDef.indexOf(')');
        int newInsnIndex = insnDef.indexOf(':');
        String insn = insnDef.substring(0, insnIndex);
        String op1 = insnDef.substring(insnIndex + 1, op1Index);
        String op2 = insnDef.substring(op1Index + 1, op2Index);
        String newInsn = insnDef.substring(newInsnIndex + 1, insnDef.length());
        return new Insn(insn, op1, op2, newInsn);
    }

    public List<CBCode> execMakeSuperInsn() {
        List<CBCode> newBCodes = new ArrayList<CBCode>(bcodes.size());

        for (CBCode bcode : bcodes) {
            CBCode newBC = null;
            for (Insn insn : insns) {
                if (!insn.name.equals(bcode.getInsnName()))
                    continue;
                CBCode bc = makeSuperInsn(bcode, insn);
                if (bc != null)
                    newBC = bc;
            }
            if (newBC == null) {
                newBCodes.add(bcode);
            } else {
                newBC.addLabels(bcode.getLabels());
                newBCodes.add(newBC);
            }
        }
        return newBCodes;
    }
}
