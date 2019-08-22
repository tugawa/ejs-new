package dispatch;

import dispatch.DecisionDiagram.HTNode;
import dispatch.DecisionDiagram.Leaf;
import dispatch.DecisionDiagram.PTNode;
import dispatch.DecisionDiagram.TagNode;
import dispatch.DecisionDiagram.TagPairNode;

class NodeVisitor<R> {
    R visitLeaf(Leaf node) {
        return null;
    }
    <T> R visitTagNode(TagNode<T> other) {
        return null;
    }
    R visitTagPairNode(TagPairNode node) {
        return visitTagNode(node);
    }
    R visitPTNode(PTNode node) {
        return visitTagNode(node);
    }
    R visitHTNode(HTNode node) {
        return visitTagNode(node);
    }
}