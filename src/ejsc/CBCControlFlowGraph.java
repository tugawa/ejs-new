package ejsc;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class CBCControlFlowGraph {
    static class CFGNode {
        private HashSet<CFGNode> pred = new HashSet<CFGNode>();
        private HashSet<CFGNode> succ = new HashSet<CFGNode>();
        private CBCode bcode;
        CFGNode(CBCode bcode) {
            this.bcode = bcode;
        }
        void addPred(CFGNode n) {
            pred.add(n);
        }
        void addSucc(CFGNode n) {
            succ.add(n);
        }
        public CBCode getBCode() {
            return bcode;
        }
        public HashSet<CFGNode> getPreds() {
            return pred;
        }
        public HashSet<CFGNode> getSuccs() {
            return succ;
        }
    }
    private HashMap<CBCode, CFGNode> cfg = new HashMap<CBCode, CFGNode>();

    CBCControlFlowGraph(List<CBCode> bcodes) {
        for (CBCode bc: bcodes)
            cfg.put(bc, new CFGNode(bc));
        for (int i = 0; i < bcodes.size(); i++) {
            CBCode bc = bcodes.get(i);
            CFGNode cfgNode = cfg.get(bc);
            if (bc.isFallThroughInstruction() && i + 1 < bcodes.size()) {
                CBCode destBC = bcodes.get(i + 1);
                makeEdge(cfgNode, destBC);
            }
            CBCode destBC = bc.getBranchTarget();
            if (destBC != null)
                makeEdge(cfgNode, destBC);
        }
    }

    private void makeEdge(CFGNode from, CBCode toBC) {
        CFGNode to = cfg.get(toBC);
        from.addSucc(to);
        to.addPred(from);
    }

    public Collection<CFGNode> getNodes() {
        return cfg.values();
    }

    public CFGNode get(CBCode bc) {
        return cfg.get(bc);
    }
}
