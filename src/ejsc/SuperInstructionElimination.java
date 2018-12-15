package ejsc;

import java.util.ArrayList;
import java.util.List;

import ejsc.Argument.ArgType;

public class SuperInstructionElimination {
    public class SuperInstructionEmulator {
        class Environment {
            CBCode bc;

            Environment(CBCode bc) {
                this.bc = bc;
            }

            public CBCode lookup(ARegister load) {
                return findDefinition(bc, load);
            }

            private CBCode findDefinition(CBCode bc, ARegister load) {
                CBCode result = null;
                for (CBCode def : rdefa.getReachingDefinitions(bc)) {
                    Register arg = def.getStoreRegister();
                    if (arg == null)
                        continue;
                    if (arg == load.reg) {
                        if (result == null)
                            result = def;
                        else
                            return null;
                    }
                }
                return result;
            }
        }

        CBCReachingDefinition rdefa;

        SuperInstructionEmulator(CBCReachingDefinition rdefa) {
            this.rdefa = rdefa;
        }

        CBCode eval(CBCode bc) {
            return eval(new Environment(bc), bc);
        }

        public CBCode eval(Environment env, CBCode bc) {
//            if (bc instanceof ICBCNop)
//                return evalICBCNop(env, (ICBCNop) bc);
            return evalCBCode(env, bc);
        }

        protected CBCode evalCBCode(Environment env, CBCode bc) {
            if (bc.load1.type != Argument.ArgType.REGISTER)
                return null;
            CBCode b = env.lookup((ARegister)bc.load1);
            if (b instanceof ICBCNop) {
                if (b.load1.isConstant())
                    bc.load1 = b.load1;
            }
            return bc;
        }

        protected CBCode evalICBCNop(Environment env, ICBCNop bc) {
            if (bc.load1.type != ArgType.REGISTER)
                return null;
            CBCode b = env.lookup((ARegister)bc.load1);
            if (b != null) {
                try {
                    return b.getClass().getDeclaredConstructor(Argument.class, Argument.class, Argument.class)
                            .newInstance(bc.store, b.load1, b.load2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    List<CBCode> bcodes;
    CBCReachingDefinition rdefa;

    SuperInstructionElimination(List<CBCode> bcodes) {
        this.bcodes = bcodes;
        rdefa = new CBCReachingDefinition(bcodes);
    }

    private CBCode computeSuperInst(CBCode bc) {
        SuperInstructionEmulator emulator = new SuperInstructionEmulator(rdefa);
        return emulator.eval(bc);
    }

    public List<CBCode> exec() {
        List<CBCode> newBCodes = new ArrayList<CBCode>(bcodes.size());

        for (CBCode bc : bcodes) {
            CBCode newBC = computeSuperInst(bc);
            if (newBC == null || bc.equals(newBC))
                newBCodes.add(bc);
            else {
                newBC.addLabels(bc.getLabels());
                newBCodes.add(newBC);
            }
        }
        return newBCodes;
    }
}
