package vmgen.newsynth;

import java.util.ArrayList;
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

class CodeGenerateVisitor extends NodeVisitor<Void> {
	static final boolean USE_GOTO = true;
	static final boolean PAD_CASES = false;
	static class Macro {
		int nextLabel = 0;

		String getPTCode(String var) {
			return "GET_PTAG("+var+")";
		}
		String getHTCode(String var) {
			return "GET_HTAG("+var+")";
		}
		String composeTagPairCode(String... vars) {
			return "TAG_PAIR("+vars[0]+", "+vars[1]+")";
		}
		String composeTagPairLiteral(String... vars) {
			return "TAG_PAIR("+vars[0]+", "+vars[1]+")";
		}
		String getLabel() {
			return String.format("L%d", nextLabel++);
		}
	}
	
	StringBuffer sb = new StringBuffer();
	Macro tagMacro;
	String[] varNames;
	HashMap<Node, String> labels = new HashMap<Node, String>();
	
	public CodeGenerateVisitor(String[] varNames, Macro tagMacro) {
		this.varNames = varNames;
		this.tagMacro = tagMacro;
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
	
	boolean processSharedNode(Node node) {
		if (USE_GOTO) {
			String label = labels.get(node);
			if (label != null) {
				sb.append("goto ").append(label).append(";\n");
				return true;
			}
			label = tagMacro.getLabel();
			labels.put(node, label);
			sb.append(label).append(":");
		}
		return false;
	}
	
	@Override
	Void visitLeaf(Leaf node) {
		if (processSharedNode(node))
			return null;
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
	Void visitTagPairNode(TagPairNode node) {
		if (processSharedNode(node))
			return null;
		HashMap<Node, LinkedHashSet<TagPairNode.TagPair>> childToTags = node.getChildToTagsMap();
		sb.append("switch(").append(tagMacro.composeTagPairCode(varNames[0], varNames[1])).append("){");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node+"("+childToTags.size()+")");
		sb.append('\n');
		
		LinkedHashSet<Integer> tagValues = new LinkedHashSet<Integer>();
		int max = 0;
		for (TagPair tag: node.getEdges()) {
			int v = tag.getValue();
			tagValues.add(v);
			if (v > max)
				max = v;
		}

		for (Node child: childToTags.keySet()) {
			for (TagPairNode.TagPair tag: childToTags.get(child)) {
				sb.append("case ").append(tagMacro.composeTagPairLiteral(tag.op1.getName(), tag.op2.getName())).append(":\n");
				if (PAD_CASES) {
					for (int v = tag.getValue() - 1; v >= 0; v--) {
						if (tagValues.contains(v))
							break;
						sb.append("case "+v+":\n");
					}
					if (tag.getValue() == max)
						sb.append("default:\n");
				}
			}
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
	Void visitPTNode(PTNode node) {
		if (processSharedNode(node))
			return null;
		HashMap<Node, LinkedHashSet<PT>> childToTags = node.getChildToTagsMap();
		sb.append("switch(").append(tagMacro.getPTCode(varNames[node.getOpIndex()])).append("){");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node+"("+childToTags.size()+")");
		sb.append('\n');
		
		LinkedHashSet<Integer> tagValues = new LinkedHashSet<Integer>();
		int max = 0;
		for (PT tag: node.getEdges()) {
			int v = tag.getValue();
			tagValues.add(v);
			if (v > max)
				max = v;
		}
		
		for (Node child: childToTags.keySet()) {
			for (PT tag: childToTags.get(child)) {
				sb.append("case "+tag.getName()+":\n");
				if (PAD_CASES) {
					for (int v = tag.getValue() - 1; v >= 0; v--) {
						if (tagValues.contains(v))
							break;
						sb.append("case "+v+":\n");
					}
					if (tag.getValue() == max)
						sb.append("default:\n");
				}
			}
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
	Void visitHTNode(HTNode node) {
		if (processSharedNode(node))
			return null;
		if (node.isNoHT()) {
			node.getChild().accept(this);
			return null;
		}
		HashMap<Node, LinkedHashSet<HT>> childToTags = node.getChildToTagsMap();
		sb.append("switch(").append(tagMacro.getHTCode(varNames[node.getOpIndex()])).append("){");
		if (DecisionDiagram.DEBUG_COMMENT)
			sb.append(" // "+node+"("+childToTags.size()+")");
		sb.append('\n');
		
		LinkedHashSet<Integer> tagValues = new LinkedHashSet<Integer>();
		int max = 0;
		for (HT tag: node.getEdges()) {
			int v = tag.getValue();
			tagValues.add(v);
			if (v > max)
				max = v;
		}
		
		for (Node child: childToTags.keySet()) {
			for (HT tag: childToTags.get(child))  {
				sb.append("case "+tag.getName()+":\n");
				if (PAD_CASES) {
					for (int v = tag.getValue() - 1; v >= 0; v--) {
						if (tagValues.contains(v))
							break;
						sb.append("case "+v+":\n");
					}
					if (tag.getValue() == max)
						sb.append("default:\n");
				}
			}
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