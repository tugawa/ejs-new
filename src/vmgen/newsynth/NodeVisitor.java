package vmgen.newsynth;

import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.PTNode;
import vmgen.newsynth.DecisionDiagram.TagNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode;

class NodeVisitor {
	Object visitLeaf(Leaf node) {
		return null;
	}
	<T> Object visitTagNode(TagNode<T> other) {
		return null;
	}
	Object visitTagPairNode(TagPairNode node) {
		return visitTagNode(node);
	}
	Object visitPTNode(PTNode node) {
		return visitTagNode(node);
	}
	Object visitHTNode(HTNode node) {
		return visitTagNode(node);
	}
}