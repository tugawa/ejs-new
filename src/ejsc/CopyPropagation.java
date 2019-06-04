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
import java.util.HashMap;
import java.util.List;

import ejsc.ControlFlowGraph.CFGNode;

public class CopyPropagation {
    List<BCode> bcodes;
    ControlFlowGraph cfg;
    ReachingDefinition rda;

    CopyPropagation(List<BCode> bcodes) {
        this.bcodes = bcodes;
        cfg = new ControlFlowGraph(bcodes);
        rda = new ReachingDefinition(cfg);
    }

    private Register getReplaceRegister(BCode bc, Register src) {
        BCode def = findDefinition(bc, src);
        if (def instanceof IMove) {
            IMove imove = (IMove) def;
            if (imove.src instanceof RegisterOperand) {
                Register defSrc = ((RegisterOperand) imove.src).get();
                if (!isDefinedOnAnyPath(def, bc, defSrc))
                    return defSrc;
            }
        }
        return null;
    }

    private BCode findDefinition(BCode bc, Register src) {
        BCode result = null;
        for (BCode def: rda.getReachingDefinitions(bc)) {
            if (def.getDestRegister() == src) {
                if (result == null)
                    result = def;
                else
                    return null;
            }
        }
        return result;
    }

    private boolean dfs(CFGNode n, CFGNode to, Register r, boolean defined, HashMap<CFGNode, Boolean> visited) {
        if (n == to && defined)
            return true;
        Boolean v = visited.get(n);
        if (v != null && (v || !defined))
            return false;
        visited.put(n, defined);

        if (n.getBCode().getDestRegister() == r)
            defined = true;
        for (CFGNode succ: n.getSuccs()) {
            if (dfs(succ, to, r, defined, visited))
                return true;
        }
        return false;
    }

    private boolean isDefinedOnAnyPath(BCode fromBC, BCode toBC, Register r) {
        HashMap<CFGNode, Boolean> visited = new HashMap<CFGNode, Boolean>();
        CFGNode from = cfg.get(fromBC);
        CFGNode to = cfg.get(toBC);
        return dfs(from, to, r, false, visited);
    }

    public void exec() {
        boolean update = true;
        while (update) {
            update = false;
            for (ControlFlowGraph.CFGNode n: cfg.getNodes()) {
                BCode bcx = n.getBCode();
                try {
                    Class<? extends BCode> c = bcx.getClass();
                    for (Field f: c.getDeclaredFields()) {
                        if (f.getType() == SrcOperand.class) {
                            SrcOperand opx = (SrcOperand) f.get(bcx);
                            if (opx instanceof RegisterOperand) {
                                RegisterOperand op = (RegisterOperand) opx;
                                Register r = op.get();
                                Register rr = getReplaceRegister(bcx, r);
                                if (rr != null) {
                                    update = true;
                                    op.set(rr);
                                }
                            }
                        } else if (f.getType() == SrcOperand[].class) {
                            SrcOperand[] opxs = (SrcOperand[]) f.get(bcx);
                            for (int i = 0; i < opxs.length; i++) {
                                if (opxs[i] instanceof RegisterOperand) {
                                    RegisterOperand op = (RegisterOperand) opxs[i];
                                    Register r = op.get();
                                    Register rr = getReplaceRegister(bcx, r);
                                    if (rr != null) {
                                        update = true;
                                        op.set(rr);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }
    }
}
