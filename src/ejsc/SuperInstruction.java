package ejsc;

import java.util.ArrayList;
import java.util.List;

import ejsc.Main.Info.SISpecInfo;
import ejsc.Main.Info.SISpecInfo.SISpec;

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

    CBCode makeSuperInsn(CBCode bc, SISpec insn) {
        return evalSuperInsn(new Environment(bc), bc, insn);
    }

    public CBCode evalSuperInsn(Environment env, CBCode bc, SISpec insn) {
        return evalCBCode(env, bc, insn);
    }

    protected CBCode evalCBCode(Environment env, CBCode bc, SISpec spec) {
        Argument store = null, load1 = null, load2 = null;
        if (spec.op0.equals("-") || spec.op0.equals("_")) {
            store = bc.store;
        } else {
            ARegister s = (ARegister) bc.store;
            CBCode b = env.lookup(s.r);
            if ((b instanceof ICBCNop) && isTypeInstance(spec.op0, b.load1))
                load1 = b.load1;
        }
        if (spec.op1.equals("-") || spec.op1.equals("_")) {
            load1 = bc.load1;
        } else {
            ARegister l = (ARegister) bc.load1;
            CBCode b = env.lookup(l.r);
            if ((b instanceof ICBCNop) && isTypeInstance(spec.op1, b.load1))
                load1 = b.load1;
        }
        if (spec.op2.equals("-") || spec.op2.equals("_")) {
            load2 = bc.load2;
        } else {
            ARegister l = (ARegister) bc.load2;
            CBCode b = env.lookup(l.r);
            if ((b instanceof ICBCNop) && isTypeInstance(spec.op2, b.load1))
                load2 = b.load1;
        }
        if (store == null || load1 == null || load2 == null)
            return null;
        return new ICBCSuperInstruction(bc.store, load1, load2, spec.siName);
    }

    boolean isTypeInstance(String type, Argument load) {
        switch(type) {
        case "fixnum":
            return (load instanceof AFixnum && ((AFixnum) load).n < (1 << 16));
        case "string":
            return load instanceof AString;
        case "flonum":
            return load instanceof ANumber;
        case "special":
            return load instanceof ASpecial;
        default:
            return false;
        }
    }

    List<CBCode> bcodes;
    CBCControlFlowGraph cfg;
    CBCReachingDefinition rdefa;

    SuperInstruction(List<CBCode> bcodes) {
        this.bcodes = bcodes;
        cfg = new CBCControlFlowGraph(bcodes);
        rdefa = new CBCReachingDefinition(bcodes);
    }

    public List<CBCode> execMakeSuperInsn() {
        List<CBCode> newBCodes = new ArrayList<CBCode>(bcodes.size());

        for (CBCode bcode : bcodes) {
            if (!SISpecInfo.containByInsnName(bcode.getInsnName())) {
                newBCodes.add(bcode);
                continue;
            }
            CBCode newBC = null;
            for (SISpec spec : SISpecInfo.getSISpecsByInsnName(bcode.getInsnName())) {
                CBCode bc = makeSuperInsn(bcode, spec);
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
