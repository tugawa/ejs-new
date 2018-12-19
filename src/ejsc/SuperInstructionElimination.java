package ejsc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SuperInstructionElimination {
    public class SuperInstructionEmulator {
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

        CBCControlFlowGraph cfg;
        CBCReachingDefinition rdefa;
        HashSet<CBCode> searchedCode;

        private CBCode removeCode;

        SuperInstructionEmulator(CBCControlFlowGraph cfg, CBCReachingDefinition rdefa) {
            this.cfg = cfg;
            this.rdefa = rdefa;
        }

        CBCode getRemoveCode() {
            return removeCode;
        }

        CBCode evalLoad(CBCode bc) {
            return evalLoad(new Environment(bc), bc);
        }
        CBCode evalStore(CBCode bc) {
            return evalStore(new Environment(bc), bc);
        }

        public CBCode evalLoad(Environment env, CBCode bc) {
            return evalCBCode(env, bc);
        }
        public CBCode evalStore(Environment env, CBCode bc) {
            if (bc instanceof ICBCNop)
                return evalICBCNop(env, (ICBCNop) bc);
            return bc;
        }

        boolean isMergeableCode(CBCode useBC, CBCode defBC) {
            // checking load1 is constant or register
            if (!(defBC.load1.isConstant || defBC.load1 instanceof ARegister || defBC.load1 instanceof ANone))
                return false;
            // checking load2 is constant or register
            if (!(defBC.load2.isConstant || defBC.load2 instanceof ARegister || defBC.load2 instanceof ANone))
                return false;

            searchedCode = new HashSet<CBCode>();
            if (!dfsDef(useBC, defBC, defBC, ((ARegister) defBC.store).r))
                return false;

            if (defBC.load1 instanceof ARegister) {
                searchedCode = new HashSet<CBCode>();
                Register loadReg = ((ARegister) defBC.load1).r;
                if (!dfsUse(useBC, defBC, useBC, loadReg))
                    return false;
            }

            if (defBC.load2 instanceof ARegister) {
                searchedCode = new HashSet<CBCode>();
                Register loadReg = ((ARegister) defBC.load2).r;
                if (!dfsUse(useBC, defBC, useBC, loadReg))
                    return false;
            }

            return true;
        }

        boolean dfsDef(CBCode useBC, CBCode defBC, CBCode bc, Register searchReg) {
            if (!searchedCode.add(bc))
                return true;
            if (!(bc == defBC || bc == useBC) && bc.getSrcRegisters().contains(searchReg))
                return false;
            if (bc != defBC && bc.getDestRegister() == searchReg)
                return true;
            CBCControlFlowGraph.CFGNode node = cfg.get(bc);
            for (CBCControlFlowGraph.CFGNode b: node.getSuccs())
                if(!dfsDef(useBC, defBC, b.getBCode(), searchReg)) return false;
            return true;
        }

        boolean dfsUse(CBCode useBC, CBCode defBC, CBCode bc, Register searchReg) {
            if (!searchedCode.add(bc) || bc == defBC)
                return true;
            if (bc.getDestRegister() == searchReg)
                return false;
            CBCControlFlowGraph.CFGNode node = cfg.get(bc);
            for (CBCControlFlowGraph.CFGNode b: node.getPreds())
                if(!dfsUse(useBC, defBC, b.getBCode(), searchReg)) return false;
            return true;
        }

        protected CBCode evalCBCode(Environment env, CBCode bc) {
            if (bc.load1 instanceof ARegister) {
                ARegister load1 = (ARegister) bc.load1;
                CBCode b = env.lookup(load1.r);
                if ((b instanceof ICBCNop) && b.load1.isConstant)
                    bc.load1 = b.load1;
            }
            if (bc.load2 instanceof ARegister) {
                ARegister load2 = (ARegister) bc.load2;
                CBCode b = env.lookup(load2.r);
                if (b instanceof ICBCNop) {
                    if (b.load1 instanceof AShortFixnum || b.load1 instanceof AFixnum)
                        bc.load2 = b.load1;
                }
            }
            return bc;
        }

        protected CBCode evalICBCNop(Environment env, ICBCNop bc) {
            if (!(bc.load1 instanceof ARegister))
                return bc;
            ARegister load1 = (ARegister) bc.load1;
            CBCode b = env.lookup(load1.r);
            if (b != null) {
                // ignore arguments parameter
                if (b instanceof MCBCParameter)
                    return bc;
                if (!isMergeableCode(bc, b)) {
                    return bc;
                }
                try {
                    removeCode = b;
                    return b.getClass().getDeclaredConstructor(Argument.class, Argument.class, Argument.class)
                            .newInstance(bc.store, b.load1, b.load2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return bc;
        }
    }

    List<CBCode> bcodes;
    CBCControlFlowGraph cfg;
    CBCReachingDefinition rdefa;

    SuperInstructionElimination(List<CBCode> bcodes) {
        this.bcodes = bcodes;
        cfg = new CBCControlFlowGraph(bcodes);
        rdefa = new CBCReachingDefinition(bcodes);
    }

    private CBCode computeLoadSIE(CBCode bc) {
        SuperInstructionEmulator emulator = new SuperInstructionEmulator(cfg, rdefa);
        return emulator.evalLoad(bc);
    }

    public List<CBCode> execLoadSIE() {
        List<CBCode> newBCodes = new ArrayList<CBCode>(bcodes.size());

        for (CBCode bc : bcodes) {
            CBCode newBC = computeLoadSIE(bc);
            if (bc.equals(newBC))
                newBCodes.add(bc);
            else {
                newBC.addLabels(bc.getLabels());
                newBCodes.add(newBC);
            }
        }
        return newBCodes;
    }

    public List<CBCode> execStoreSIE() {
        List<CBCode> newBCodes = new ArrayList<CBCode>(bcodes.size());
        HashSet<CBCode> removeCodes = new HashSet<CBCode>();

        for (CBCode bc : bcodes) {
            SuperInstructionEmulator emulator = new SuperInstructionEmulator(cfg, rdefa);

            CBCode newBC = emulator.evalStore(bc);
            if (newBC == null || bc.equals(newBC))
                newBCodes.add(bc);
            else {
                newBC.addLabels(bc.getLabels());
                newBCodes.add(newBC);
            }

            CBCode rmBC = emulator.getRemoveCode();
            if (rmBC != null)
                removeCodes.add(rmBC);
        }
        for (CBCode bc: removeCodes) {
            int index = newBCodes.indexOf(bc);
            newBCodes.remove(index);
            CBCode b = newBCodes.get(index);
            b.addLabels(bc.getLabels());
        }
        return newBCodes;
    }
}
