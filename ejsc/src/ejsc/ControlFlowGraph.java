/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ControlFlowGraph {
    class CFGNodeIterator implements Iterator<CFGNode>, Iterable<CFGNode> {
        private BitSet set;
        private int nextIndex;
        private CFGNodeIterator(BitSet set) {
            this.set = set;
            this.nextIndex = set.nextSetBit(0);
        }
        @Override
        public boolean hasNext() {
            return nextIndex != -1;
        }
        @Override
        public CFGNode next() {
            CFGNode n = nodes[nextIndex];
            nextIndex = set.nextSetBit(nextIndex + 1);
            return n;
        }
        @Override
        public Iterator<CFGNode> iterator() {
            return this;
        }
    }

    class CFGNode {
        private BitSet pred = new BitSet(nodes.length);
        private BitSet succ = new BitSet(nodes.length);
        private BCode bcode;
        CFGNode(BCode bcode) {
            this.bcode = bcode;
        }
        void addPred(CFGNode n) {
            pred.set(n.getIndex());
        }
        void addSucc(CFGNode n) {
            succ.set(n.getIndex());
        }
        public BCode getBCode() {
            return bcode;
        }
        public CFGNodeIterator getPreds() {
            return new CFGNodeIterator(pred);
        }
        public CFGNodeIterator getSuccs() {
            return new CFGNodeIterator(succ);
        }
        public int getIndex() {
            return bcode.getAddress();
        }
    }
    private HashMap<BCode, CFGNode> cfg = new HashMap<BCode, CFGNode>();
    private CFGNode[] nodes;

    ControlFlowGraph(List<BCode> bcodes) {
        nodes = new CFGNode[bcodes.size()];
        for (int i = 0; i < bcodes.size(); i++) {
            BCode bc = bcodes.get(i);
            CFGNode cfgNode = new CFGNode(bc);
            nodes[i] = cfgNode;
            cfg.put(bc, cfgNode);
        }
        for (int i = 0; i < bcodes.size(); i++) {
            BCode bc = bcodes.get(i);
            CFGNode cfgNode = nodes[i];
            if (bc.isFallThroughInstruction() && i + 1 < bcodes.size()) {
                BCode destBC = bcodes.get(i + 1);
                makeEdge(cfgNode, cfg.get(destBC));
            }
            BCode destBC = bc.getBranchTarget();
            if (destBC != null)
                makeEdge(cfgNode, cfg.get(destBC));
        }
    }

    private void makeEdge(CFGNode from, CFGNode to) {
        from.addSucc(to);
        to.addPred(from);
    }

    public Collection<CFGNode> getNodes() {
        return cfg.values();
    }

    public CFGNode get(BCode bc) {
        return cfg.get(bc);
    }
}
