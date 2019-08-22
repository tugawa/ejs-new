package dispatch;

import java.util.ArrayList;

import dispatch.DecisionDiagram.Leaf;
import dispatch.DecisionDiagram.Node;
import dispatch.DecisionDiagram.TagNode;

class SemanticLayerGatherVisitor extends NodeVisitor<Void> {
    static class LeafDispatch extends DispatchPlan.DispatchCriterion {
        @Override
        public boolean available(int arity) {
            return true;
        }
    }

    ArrayList<Node> nodes = new ArrayList<Node>();
    DispatchPlan.DispatchCriterion ref;

    SemanticLayerGatherVisitor(DispatchPlan.DispatchCriterion ref) {
        this.ref = ref;
    }

    ArrayList<Node> get() {
        return nodes;
    }

    @Override
    Void visitLeaf(Leaf node) {
        if (ref instanceof SemanticLayerGatherVisitor.LeafDispatch) {
            if (!nodes.contains(node))
                nodes.add(node);
            return null;
        }
        return null;
    }

    @Override
    <T> Void visitTagNode(TagNode<T> node) {
        if ((node instanceof DecisionDiagram.PTNode &&
             ref instanceof DecisionDiagram.PTDispatch &&
             node.opIndex == ((DecisionDiagram.PTDispatch) ref).opIndex) ||
            (node instanceof DecisionDiagram.HTNode &&
             ref instanceof DecisionDiagram.HTDispatch &&
             node.opIndex == ((DecisionDiagram.HTDispatch) ref).opIndex)) {
            if (!nodes.contains(node))
                nodes.add(node);
            return null;
        }
        for (Node child: node.getChildren())
            child.accept(this);
        return null;
    }
}