/*
   MergeChildrenVisitor.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
 */
package vmgen.newsynth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.TagNode;

public class MergeChildrenVisitor extends NodeVisitor<Void> {
    @Override
    Void visitLeaf(Leaf node) {
        return null;
    }

    @Override
    <T> Void visitTagNode(TagNode<T> node) {
        HashMap<Node, LinkedHashSet<T>> childToTags = node.getChildToTagsMap();
        ArrayList<Node> children = new ArrayList<Node>(childToTags.keySet());
        boolean[] hasMerged = new boolean[children.size()];
        for (Node child: children)
            child.accept(this);

        HashMap<T, Node> newBranches = new HashMap<T, Node>();
        for (int i = 0; i < children.size(); i++) {
            if (hasMerged[i])
                continue;
            Node ci = children.get(i);
            LinkedHashSet<T> edge = new LinkedHashSet<T>(childToTags.get(ci));
            Node merged = ci;
            hasMerged[i] = true;
            for (int j = i + 1; j < children.size(); j++) {
                if (hasMerged[j])
                    continue;
                Node cj = children.get(j);
                if (!DecisionDiagram.isCompatible(merged, cj))
                    continue;
                merged = merged.merge(cj);
                edge.addAll(childToTags.get(cj));
                hasMerged[j] = true;
            }
            for (T tag: edge)
                newBranches.put(tag, merged);
        }
        node.branches = newBranches;
        return null;
    }

    @Override
    Void visitHTNode(HTNode node) {
        if (node.isNoHT())
            return node.getChild().accept(this);
        else
            return visitTagNode(node);
    }
}
