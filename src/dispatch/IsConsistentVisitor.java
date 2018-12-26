/*
   IsConsistentVisitor.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
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