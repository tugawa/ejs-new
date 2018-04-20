package vmgen.newsynth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.TagNode;

public class MergeChildrenVisitor extends NodeVisitor {

	@Override
	Object visitLeaf(Leaf node) {
		return null;
	}

	@Override
	<T> Object visitTagNode(TagNode<T> node) {
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
				if (!DecisionDiagram.checkMergeCriteria(cj, merged))
					continue;
				merged = merged.merge(cj);
				edge.addAll(childToTags.get(cj));
				hasMerged[j] = true;
			}
			for (T tag: edge)
				newBranches.put(tag, merged);
		}
		node.branches = newBranches;
		/*
		if (branches.values().size() == 2) {
			Iterator<Node> it = branches.values().iterator();
			Node a = it.next();
			Node b = it.next();
			if (a instanceof HTNode && b instanceof HTNode) {
				HTNode hta = (HTNode) a;
				HTNode htb = (HTNode) b;
				if (htb.isNoHT()) {
					HTNode t = hta;
					hta = htb;
					htb = t;
				}
				if (!htb.isNoHT() && htb.getChildren().size() == 1) {
					System.out.println("special merge "+hta+" "+htb);
					if (isCompatible(hta.getChild(), htb.getChildren().get(0))) {
						Node merged = hta.superMerge(htb);
						for (T tag: branches.keySet())
							branches.replace(tag, merged);
						System.out.println("success");
					}
				}
			}
		}
		*/
		return null;
	}

	@Override
	Object visitHTNode(HTNode node) {
		if (node.isNoHT())
			return node.getChild().accept(this);
		else
			return visitTagNode(node);
	}
}
