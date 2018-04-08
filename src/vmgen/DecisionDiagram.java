package vmgen;

import java.util.HashMap;

import vmgen.type.VMRepType;
import vmgen.type.VMRepType.HT;
import vmgen.type.VMRepType.PT;

public class DecisionDiagram {
	static final int DISPATCH_TAGPAIR = 0;
	static final int DISPATCH_PT_BASE = 10;
	static final int DISPATCH_HT_BASE = 20;
	static final int[] DISPATCH_PLAN = {
			DISPATCH_TAGPAIR,
			DISPATCH_PT_BASE + 0,
			DISPATCH_PT_BASE + 1,
			DISPATCH_HT_BASE + 0,
			DISPATCH_HT_BASE + 1
	};
	
	static abstract class Node {
		abstract void toCode(StringBuffer sb, String varNames[]);
	}
	static class Leaf extends Node {
		LLRuleSet.LLRule rule;
		Leaf(LLRuleSet.LLRule rule) {
		 	this.rule = rule;
		}
		LLRuleSet.LLRule getRule() {
			return rule;
		}
		void toCode(StringBuffer sb, String varNames[]) {
			sb.append("{").append(rule.getHLRule().action).append("}\n");
		}
	}
	static abstract class TagNode<T> extends Node {
		int opIndex;
		HashMap<T, Node> branches = new HashMap<T, Node>();
		TagNode(int opIndex) {
			this.opIndex = opIndex;
		}
		void addBranch(TreeDigger digger, T tag) {
			Node child = branches.get(tag);
			if (child == null)
				child = digger.leaf();
			else
				child = digger.dig(child);
			branches.put(tag, child);
		}
		void addLeaf(TreeDigger digger, T tag, Leaf leaf) {
			branches.put(tag, leaf);
		}
	}
	static class TagPairNode extends TagNode<TagPairNode.TagPair> {
		static class TagPair {
			@Override
			public int hashCode() {
				return (op1.getValue() << 8) + op2.getValue();
			}
			@Override
			public boolean equals(Object obj) {
				if (obj == null)
					return false;
				if (!(obj instanceof TagPair))
					return false;
				TagPair other = (TagPair) obj;
				return op1 == other.op1 && op2 == other.op2;
			}
			PT op1;
			PT op2;
			TagPair(PT op1, PT op2) {
				this.op1 = op1;
				this.op2 = op2;
			}
		};
		TagPairNode() {
			super(-1);
		}
		/*
		void addBranch(TreeDigger digger, TagPair tag) {
			System.out.println(tag);
			Node child = branches.get(tag);
			if (child == null)
				child = digger.leaf();
			else
				child = digger.dig(child);
			branches.put(tag, child);
		}
		*/
		void toCode(StringBuffer sb, String varNames[]) {
			sb.append("switch(TAG_PAIR("+varNames[0]+","+varNames[1]+")){\n");
			for (TagPair tag: branches.keySet()) {
				Node child = branches.get(tag);
				sb.append("case TAG_PAIR("+tag.op1.getName()+","+tag.op2.getName()+"):\n");
				child.toCode(sb, varNames);
				sb.append("break;\n");
			}
			sb.append("}\n");
		}
	}
	static class PTNode extends TagNode<PT> {
		PTNode(int opIndex) {
			super(opIndex);
		}
		void toCode(StringBuffer sb, String varNames[]) {
			sb.append("switch(GET_PTAG("+varNames[opIndex]+")){\n");
			for (PT tag: branches.keySet()) {
				Node child = branches.get(tag);
				sb.append("case "+tag.getName()+":\n");
				child.toCode(sb, varNames);
				sb.append("break;\n");
			}
			sb.append("}\n");
		}
	}
	static class HTNode extends TagNode<HT> {
		boolean noHT;
		Node child;
		HTNode(int opIndex) {
			super(opIndex);
			noHT = false;
		}
		void addBranch(TreeDigger digger, HT tag) {
			if (noHT) {
				if (tag != null)
					throw new Error("invalid tag assignment");
				this.child = digger.dig(this.child);
				return;
			}
			super.addBranch(digger, tag);
		}
		void addLeaf(TreeDigger digger, HT tag, Leaf leaf) {
			if (tag == null) {
				noHT = true;
				child = digger.dig(null);
				return;
			}
			super.addLeaf(digger, tag, leaf);
		}
		void toCode(StringBuffer sb, String varNames[]) {
			if (noHT) {
				child.toCode(sb, varNames);
				return;
			}
			sb.append("switch(GET_HTAG("+varNames[opIndex]+")){\n");
			for (HT tag: branches.keySet()) {
				Node child = branches.get(tag);
				sb.append("case "+tag.getName()+":\n");
				child.toCode(sb, varNames);
				sb.append("break;\n");
			}
			sb.append("}\n");
		}
	}
	
	static class TreeDigger {
		final LLRuleSet.LLRule rule;
		final VMRepType[] rts;
		final int arity;
		final RuleSet.Rule hlr;
		int planIndex;
		
		TreeDigger(LLRuleSet.LLRule r) {
			rule = r;
			rts = r.getVMRepTypes();
			arity = rts.length;
			hlr = r.getHLRule();
			planIndex = 0;
		}
		
		Node leaf() {
			return new Leaf(rule);
		}
		
		Node dig(Node nodex) {
			if (planIndex == DISPATCH_PLAN.length)
				throw new Error("ambigous" + rule.rts[0].getName() + rule.rts[1].getName());
			
			if (nodex == null)
				return leaf();

			int dispatchType = DISPATCH_PLAN[planIndex++];
			if (dispatchType == DISPATCH_TAGPAIR && arity == 2) {
				TagPairNode node;
				if (nodex instanceof Leaf) {
					if (((Leaf) nodex).getRule() == rule) {
						throw new Error("LL-Rule duplicate");
						// return nodex;
					}
					node = new TagPairNode();
					Leaf leaf = (Leaf) nodex;
					VMRepType[] leafRTS = leaf.getRule().getVMRepTypes();
					node.addLeaf(this, new TagPairNode.TagPair(leafRTS[0].getPT(), leafRTS[1].getPT()), (Leaf) nodex);
				} else
					node = (TagPairNode) nodex;
				node.addBranch(this, new TagPairNode.TagPair(rts[0].getPT(), rts[1].getPT()));
				return node;
			} else if (DISPATCH_PT_BASE <= dispatchType &&
					   dispatchType < DISPATCH_HT_BASE &&
					   dispatchType - DISPATCH_PT_BASE < arity) {
				int opIndex = dispatchType - DISPATCH_PT_BASE;
				PTNode node;
				if (nodex instanceof Leaf) {
					if (((Leaf) nodex).getRule() == rule) {
						throw new Error("LL-Rule duplicate");
						// return nodex;
					}
					node = new PTNode(opIndex);
					Leaf leaf = (Leaf) nodex;
					VMRepType[] leafRTS = leaf.getRule().getVMRepTypes();
					node.addLeaf(this, leafRTS[opIndex].getPT(), leaf);
				} else
					node = (PTNode) nodex;
				node.addBranch(this, rts[opIndex].getPT());
				return node;
			} else if (DISPATCH_HT_BASE <= dispatchType &&
					   dispatchType - DISPATCH_HT_BASE < arity) {
				int opIndex = dispatchType - DISPATCH_HT_BASE;
				HTNode node;
				if (nodex instanceof Leaf) {
					if (((Leaf) nodex).getRule() == rule) {
						// throw new Error("LL-Rule duplicate");
						return nodex;
					}
					node = new HTNode(opIndex);
					Leaf leaf = (Leaf) nodex;
					VMRepType[] leafRTS = leaf.getRule().getVMRepTypes();
					System.out.println(leafRTS[0].getName());
					System.out.println("conflict: ("+leafRTS[0].getName()+","+leafRTS[1].getName()+") vs ("+rts[0].getName()+","+rts[1].getName()+")");
					node.addLeaf(this, leafRTS[opIndex].getHT(), leaf);
				} else
					node = (HTNode) nodex;
				node.addBranch(this, rts[opIndex].getHT());
				return node;
			} else
				throw new Error("invalid dispatch plan:"+dispatchType);
		}
	}
	
	Node root;
	
	public DecisionDiagram(LLRuleSet rs) {
		if (rs.getRules().size() == 0)
			return;
		for (LLRuleSet.LLRule r : rs.getRules()) {
			TreeDigger digger = new TreeDigger(r);
			root = digger.dig(root);
		}
	}
	
	
	public String generateCode(String[] varNames) {
		StringBuffer sb = new StringBuffer();
		root.toCode(sb, varNames);
		return sb.toString();
	}
}
