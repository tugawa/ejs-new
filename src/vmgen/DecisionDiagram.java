package vmgen;

import java.util.HashMap;
import java.util.HashSet;

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
		abstract boolean isCompatibleTo(Node other);
		// returns a merged node
		// other should be compatible with this
		// this method does not mutate this object
		abstract Node merge(Node other);
		abstract void mergeChildren();
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
		@Override
		boolean isCompatibleTo(Node otherx) {
			if (otherx instanceof Leaf) {
				Leaf other = (Leaf) otherx;
				if (rule.getHLRule() == other.getRule().getHLRule())
					return true;
			}
			return false;
		}
		@Override
		Node merge(Node otherx) {
			return this;
		}
		@Override
		void mergeChildren() {}
		@Override
		void toCode(StringBuffer sb, String varNames[]) {
			sb.append("{").append(rule.getHLRule().action).append("}\n");
		}
	}
	static abstract class TagNode<T> extends Node {
		int opIndex;
		HashMap<T, Node> tagToChild = new HashMap<T, Node>();
		
		TagNode(int opIndex) {
			this.opIndex = opIndex;
		}
		void addBranch(TreeDigger digger, T tag) {
			Node child = tagToChild.get(tag);
			if (child == null)
				child = digger.leaf();
			else
				child = digger.dig(child);
			tagToChild.put(tag, child);
		}
		void addLeaf(TreeDigger digger, T tag, Leaf leaf) {
			tagToChild.put(tag, leaf);
		}
		@Override
		boolean isCompatibleTo(Node otherx) {
			if (otherx.getClass() != this.getClass())
				return false;
			TagNode<T> other = (TagNode<T>) otherx;
			HashSet<T> union = new HashSet<T>(tagToChild.keySet());
			union.addAll(other.tagToChild.keySet());
			for (T tag: union) {
				Node thisChild = tagToChild.get(tag);
				Node otherChild = other.tagToChild.get(tag);
				if (thisChild != null && otherChild != null && !thisChild.isCompatibleTo(otherChild))
					return false;
			}
			return true;
		}
		HashMap<Node, HashSet<T>> makeChildToTagsMap(HashMap<T, Node> tagToChild) {
			HashMap<Node, HashSet<T>> childToTags = new HashMap<Node, HashSet<T>>();
			for (T tag: tagToChild.keySet()) {
				Node child = tagToChild.get(tag);
				HashSet<T> tags = childToTags.get(child);
				if (tags == null) {
					tags = new HashSet<T>();
					childToTags.put(child, tags);
				}
				tags.add(tag);
			}
			return childToTags;
		}
		@Override
		void mergeChildren() {
			HashMap<Node, HashSet<T>> childToTags = makeChildToTagsMap(tagToChild);	
			Node[] children = new Node[childToTags.size()];
			boolean[] hasMerged = new boolean[children.length];
			{
				int i = 0;
				for (Node child: childToTags.keySet()) {
					child.mergeChildren();
					children[i++] = child;
				}
			}
			tagToChild = new HashMap<T, Node>();
			for (int i = 0; i < children.length; i++) {
				if (hasMerged[i])
					continue;
				HashSet<T> edge = childToTags.get(children[i]);
				Node merged = children[i];
				hasMerged[i] = true;
				for (int j = i + 1; j < children.length; j++) {
					if (!hasMerged[j] && merged.isCompatibleTo(children[j])) {
						edge.addAll(childToTags.get(children[j]));
						merged = merged.merge(children[j]);
						hasMerged[j] = true;
					}
				}
				for (T tag: edge)
					tagToChild.put(tag, merged);
			}
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
		@Override
		boolean isCompatibleTo(Node otherx) {
			// Since TagPair is used only as a root node, if any, isCompatibleTo will
			// not be called.
			throw new Error("isCompatibleTo for TagPairNode is called");
			//return false;
		}
		@Override
		Node merge(Node otherx) {
			throw new Error("isCompatibleTo for TagPairNode is called");
		}
		@Override
		void toCode(StringBuffer sb, String varNames[]) {
			HashMap<Node, HashSet<TagPair>> childToTags = makeChildToTagsMap(tagToChild);
			sb.append("switch(TAG_PAIR("+varNames[0]+","+varNames[1]+")){\n");
			for (Node child: childToTags.keySet()) {
				for (TagPair tag: childToTags.get(child))
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
		@Override
		void toCode(StringBuffer sb, String varNames[]) {
			HashMap<Node, HashSet<PT>> childToTags = makeChildToTagsMap(tagToChild);
			sb.append("switch(GET_PTAG("+varNames[opIndex]+")){\n");
			for (Node child: childToTags.keySet()) {
				for (PT tag: childToTags.get(child))
					sb.append("case "+tag.getName()+":\n");
				child.toCode(sb, varNames);
				sb.append("break;\n");
			}
			sb.append("}\n");
		}
		@Override
		boolean isCompatibleTo(Node otherx) {
			if (!(otherx instanceof PTNode))
				return false;
			PTNode other = (PTNode) otherx;
			if (opIndex != other.opIndex)
				return false;
			return super.isCompatibleTo(otherx);
		}
		@Override
		Node merge(Node otherx) {
			PTNode other = (PTNode) otherx;
			PTNode merged = new PTNode(opIndex);
			HashSet<PT> union = new HashSet<PT>(tagToChild.keySet());
			union.addAll(other.tagToChild.keySet());
			for (PT tag: union) {
				Node thisChild = tagToChild.get(tag);
				Node otherChild = other.tagToChild.get(tag);
				if (thisChild == null)
					merged.tagToChild.put(tag, otherChild);
				else if (otherChild == null)
					merged.tagToChild.put(tag, thisChild);
				else {
					Node mergedChild = thisChild.merge(otherChild);
					merged.tagToChild.put(tag, mergedChild);
				}
			}
			return merged;
		}
	}
	static class HTNode extends TagNode<HT> {
		boolean noHT;
		Node child;
		HTNode(int opIndex) {
			super(opIndex);
			noHT = false;
		}
		@Override
		void addBranch(TreeDigger digger, HT tag) {
			if (noHT) {
				if (tag != null)
					throw new Error("invalid tag assignment");
				this.child = digger.dig(this.child);
				return;
			}
			super.addBranch(digger, tag);
		}
		@Override
		void addLeaf(TreeDigger digger, HT tag, Leaf leaf) {
			if (tag == null) {
				noHT = true;
				child = digger.dig(null);
				return;
			}
			super.addLeaf(digger, tag, leaf);
		}
		@Override
		boolean isCompatibleTo(Node otherx) {
			if (!(otherx instanceof HTNode))
				return false;
			HTNode other = (HTNode) otherx;
			if (opIndex != other.opIndex)
				return false;
			if (noHT != other.noHT)
				return false;
			if (noHT && this.child.isCompatibleTo(other.child))
				return true;
			return super.isCompatibleTo(otherx);
		}
		@Override
		Node merge(Node otherx) {
			HTNode other = (HTNode) otherx;
			if (noHT) {
				HTNode merged = new HTNode(opIndex);
				merged.noHT = true;
				merged.child = child.merge(other.child);
				return merged;
			}
			HTNode merged = new HTNode(opIndex);
			HashSet<HT> union = new HashSet<HT>(tagToChild.keySet());
			union.addAll(other.tagToChild.keySet());
			for (HT tag: union) {
				Node thisChild = tagToChild.get(tag);
				Node otherChild = other.tagToChild.get(tag);
				if (thisChild == null)
					merged.tagToChild.put(tag, otherChild);
				else if (otherChild == null)
					merged.tagToChild.put(tag, thisChild);
				else {
					Node mergedChild = thisChild.merge(otherChild);
					merged.tagToChild.put(tag, mergedChild);
				}
			}
			return merged;
		}
		@Override
		void mergeChildren() {
			if (noHT)
				child.mergeChildren();
			else
				super.mergeChildren();
		}
		@Override
		void toCode(StringBuffer sb, String varNames[]) {
			if (noHT) {
				child.toCode(sb, varNames);
				return;
			}
			HashMap<Node, HashSet<HT>> childToTags = makeChildToTagsMap(tagToChild);
			sb.append("switch(GET_HTAG("+varNames[opIndex]+")){\n");
			for (Node child: childToTags.keySet()) {
				for (HT tag: childToTags.get(child)) 
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
		
		root.mergeChildren();
	}
	
	public String generateCode(String[] varNames) {
		StringBuffer sb = new StringBuffer();
		root.toCode(sb, varNames);
		return sb.toString();
	}
}
