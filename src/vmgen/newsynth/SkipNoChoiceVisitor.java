package vmgen.newsynth;

import java.util.ArrayList;
import java.util.HashMap;

import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.TagNode;

public class SkipNoChoiceVisitor extends NodeVisitor {

	@Override
	Object visitLeaf(Leaf node) {
		return node;
	}

	@Override
	<T> Object visitTagNode(TagNode<T> node) {
		ArrayList<Node> children = node.getChildren();
		if (children.size() == 1)
			return children.get(0).accept(this);
		HashMap<Node, Node> replace = new HashMap<Node, Node>();
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
