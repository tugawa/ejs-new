package vmgen.newsynth;

import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.PTNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode.TagPair;
import vmgen.type.VMRepType;
import vmgen.type.VMRepType.HT;
import vmgen.type.VMRepType.PT;

public class SearchVisitor extends NodeVisitor {
	
	VMRepType[] rts;
	
	SearchVisitor(VMRepType[] rts) {
		this.rts = rts;
	}

	@Override
	Object visitLeaf(Leaf node) {
		return node.getRule();
	}

	@Override
	Object visitTagPairNode(TagPairNode node) {
		TagPair tag = new TagPair(rts[0].getPT(), rts[1].getPT());
		Node next = node.getChild(tag);
		return next.accept(this);
	}

	@Override
	Object visitPTNode(PTNode node) {
		PT tag = rts[node.opIndex].getPT();
		Node next = node.getChild(tag);
		return next.accept(this);
	}

	@Override
	Object visitHTNode(HTNode node) {
		if (node.isNoHT())
			return node.getChild().accept(this);
		HT tag = rts[node.opIndex].getHT();
		Node next = node.getChild(tag);
		return next.accept(this);
	}
}
