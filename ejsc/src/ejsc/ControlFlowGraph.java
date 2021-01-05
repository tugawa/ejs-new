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
    private CFGNode[] nodes;

    ControlFlowGraph(List<BCode> bcodes) {
        nodes = new CFGNode[bcodes.size()];
        for (int i = 0; i < bcodes.size(); i++) {
            BCode bc = bcodes.get(i);
            CFGNode cfgNode = new CFGNode(bc);
            nodes[i] = cfgNode;
        }
        for (int i = 0; i < bcodes.size(); i++) {
            BCode bc = bcodes.get(i);
            CFGNode cfgNode = nodes[i];
            if (bc.isFallThroughInstruction() && i + 1 < bcodes.size())
                makeEdge(cfgNode, nodes[i + 1]);
            BCode destBC = bc.getBranchTarget();
            if (destBC != null)
                makeEdge(cfgNode, nodes[destBC.getAddress()]);
        }
    }

    private void makeEdge(CFGNode from, CFGNode to) {
        from.addSucc(to);
        to.addPred(from);
    }

    public CFGNode[] getNodes() {
        return nodes;
    }

    class DescendingNodeIterator implements Iterable<CFGNode>, Iterator<CFGNode> {
        private int index;

        private DescendingNodeIterator() {
            index = nodes.length;
        }

        @Override
        public boolean hasNext() {
            return index > 0;
        }

        @Override
        public CFGNode next() {
            return nodes[--index];
        }

        @Override
        public Iterator<CFGNode> iterator() {
            return this;
        }
    }

    public Iterable<CFGNode> getNodeDesc() {
        return new DescendingNodeIterator();
    }

    public CFGNode get(BCode bc) {
        return nodes[bc.getAddress()];
    }
}
