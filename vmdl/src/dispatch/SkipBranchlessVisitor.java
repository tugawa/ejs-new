/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package dispatch;

import java.util.ArrayList;
import java.util.TreeMap;

import dispatch.DecisionDiagram.Leaf;
import dispatch.DecisionDiagram.Node;
import dispatch.DecisionDiagram.TagNode;


public class SkipBranchlessVisitor extends NodeVisitor<Node> {

    @Override
    Node visitLeaf(Leaf node) {
        return node;
    }

    @Override
    <T> Node visitTagNode(TagNode<T> node) {
        ArrayList<Node> children = node.getChildren();
        if (children.size() == 1)
            return children.get(0).accept(this);
        TreeMap<Node, Node> replace = new TreeMap<Node, Node>();
        for (Node before: children) {
            Node after = (Node) before.accept(this);
            replace.put(before, after);
        }
        for (T tag: node.getEdges()) {
            Node before = node.getChild(tag);
            Node after = replace.get(before);
            node.replaceChild(tag, after);
        }
        return node;
    }

}
