/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package dispatch;

import dispatch.DecisionDiagram.Leaf;
import dispatch.DecisionDiagram.Node;
import dispatch.DecisionDiagram.TagNode;

class IsConsistentVisitor extends NodeVisitor<Boolean> {
    Node root;
    Node currentNodex;

    IsConsistentVisitor(Node root) {
        this.root = root;
        currentNodex = root;
    }

    @Override
    Boolean visitLeaf(Leaf other) {
        if (currentNodex instanceof Leaf) {
            Leaf currentNode = (Leaf) currentNodex;
            return currentNode.hasSameHLRule(other);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    <T> Boolean visitTagNode(TagNode<T> other) {
        if (currentNodex.getClass() != other.getClass())
            throw new Error("class mismatch");
        TagNode<T> currentNode = (TagNode<T>) currentNodex;
        if (currentNode.getOpIndex() != other.getOpIndex())
            throw new Error("opIndex mismatch");

        if (currentNode.getChildren().size() == 1 && other.getChildren().size() == 1)
            return currentNode.getChildren().get(0) == other.getChildren().get(0);
        else if (currentNode.getChildren().size() > 1 && other.getChildren().size() > 1){
            for (T tag: currentNode.branches.keySet()) {
                Node thisChild = currentNode.getChild(tag);
                Node otherChild = other.getChild(tag);
                if (otherChild != null && otherChild != thisChild)
                    return false;
            }
            return true;
        } else
            return false;
    }
}