package vmgen.newsynth;

import java.util.HashMap;
import java.util.LinkedHashSet;

import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.PTNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode.TagPair;
import vmgen.type.VMRepType;
import vmgen.type.VMRepType.HT;
import vmgen.type.VMRepType.PT;

class CodeGenerateVisitor extends NodeVisitor {
	StringBuffer sb = new StringBuffer();
	String[] varNames;
	public CodeGenerateVisitor(String[] varNames) {
		this.varNames = varNames;
	}
	@Override
	public String toString() {
		return sb.toString();
	}
	@Override
	Object visitLeaf(Leaf node) {
		sb.append("{");
		if (DecisionDiagram.DEBUG_COMMENT) {
			sb.append(" //");
			for (VMRepType rt: node.getRule().getVMRepTypes())
				sb.append(" ").append(rt.getName());
			sb.append(" ").append(node);
		}
		sb.append(node.getRule().getHLRule().action).append("}\n");
		return null;
	}
	@Override
	Object visitTagPairNode(TagPairNode node) {
		HashMap<Node, LinkedHashSet<TagPairNode.TagPair>> childToTags = node.getChildToTagsMap();
		sb.append("switch(TAG_PAIR("+varNames[0]+","+varNames[1]+")){");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node+"("+childToTags.size()+")");
		sb.append('\n');
		for (Node child: childToTags.keySet()) {
			for (TagPairNode.TagPair tag: childToTags.get(child))
				sb.append("case TAG_PAIR("+tag.op1.getName()+","+tag.op2.getName()+"):\n");
			child.accept(this);
			sb.append("break;\n");
		}
		sb.append("}");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node);
		sb.append('\n');
		return null;
	}
	@Override
	Object visitPTNode(PTNode node) {
		HashMap<Node, LinkedHashSet<PT>> childToTags = node.getChildToTagsMap();
		sb.append("switch(GET_PTAG("+varNames[node.getOpIndex()]+")){");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node+"("+childToTags.size()+")");
		sb.append('\n');
		for (Node child: childToTags.keySet()) {
			for (PT tag: childToTags.get(child))
				sb.append("case "+tag.getName()+":\n");
			child.accept(this);
			sb.append("break;\n");
		}
		sb.append("}");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node);
		sb.append('\n');
		return null;
	}
	@Override
	Object visitHTNode(HTNode node) {
		if (node.isNoHT()) {
			node.getChild().accept(this);
			return null;
		}
		HashMap<Node, LinkedHashSet<HT>> childToTags = node.getChildToTagsMap();
		sb.append("switch(GET_HTAG("+varNames[node.getOpIndex()]+")){");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node+"("+childToTags.size()+")");
		sb.append('\n');
		for (Node child: childToTags.keySet()) {
			for (HT tag: childToTags.get(child)) 
				sb.append("case "+tag.getName()+":\n");
			child.accept(this);
			sb.append("break;\n");
		}
		sb.append("}");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append("// "+node);
		sb.append('\n');
		return null;
	}
}