package vmgen.newsynth;

import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.PTNode;
import vmgen.newsynth.DecisionDiagram.TagNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode;

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