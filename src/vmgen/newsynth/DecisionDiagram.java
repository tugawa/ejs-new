package vmgen.newsynth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;

import vmgen.RuleSet;
import vmgen.RuleSet.Rule;
import vmgen.newsynth.LLRuleSet.LLRule;
import vmgen.type.VMRepType;
import vmgen.type.VMRepType.HT;
import vmgen.type.VMRepType.PT;

public class DecisionDiagram {
	public static final boolean DEBUG_COMMENT = true;
	public static final int MERGE_LEVEL = 2; // 0-2: 0 is execution spped oriendted, 2 is size oriented
	
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
		abstract Object accept(NodeVisitor visitor);		
		int depth() {
			int max = 0;
			for (Node child: getChildren()) {
				int d = child.depth();
				if (d > max)
					max = d;
			}
			return max + 1;
		}
		abstract ArrayList<Node> getChildren();
		
		abstract boolean isSingleLeafTree();
		// slt should be SIngelLeafTree
		abstract boolean isAbsobable(Node slt);
		// returns a merged node
		// other should be compatible with this
		// this method does not mutate this object
		abstract Node merge(Node other);
		abstract void mergeChildren();
		abstract Node skipNoChoice();
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
		Object accept(NodeVisitor visitor) {
			return visitor.visitLeaf(this);
		}
		@Override
		ArrayList<Node> getChildren() {
			return new ArrayList<Node>();
		}
		boolean hasSameHLRule(Leaf other) {
			return getRule().getHLRule() == other.getRule().getHLRule();
		}
		@Override
		boolean isSingleLeafTree() {
			return true;
		}
		@Override
		boolean isAbsobable(Node otherx) {
			return hasSameHLRule((Leaf) otherx);
		}
		@Override
		Node merge(Node otherx) {
			return this;
		}
		@Override
		void mergeChildren() {}
		@Override
		Node skipNoChoice() {
			return this;
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
			child = digger.dig(child);
			branches.put(tag, child);
		}
		@Override
		Object accept(NodeVisitor visitor) {
			return visitor.visitTagNode(this);
		}
		@Override
		ArrayList<Node> getChildren() {
			LinkedHashSet<Node> s = new LinkedHashSet<Node>();
			for (T tag: branches.keySet())
				s.add(branches.get(tag));
			return new ArrayList<Node>(s);
		}
		int getOpIndex() {
			return opIndex;
		}
		boolean hasCompatibleBranches(TagNode<T> other) {
			if (opIndex != other.opIndex)
				return false;
			LinkedHashSet<T> union = new LinkedHashSet<T>(branches.keySet());
			union.addAll(other.branches.keySet());
			for (T tag: union) {
				Node thisChild = branches.get(tag);
				Node otherChild = other.branches.get(tag);
				if (thisChild != null && otherChild != null && !isCompatible(thisChild, otherChild))
					return false;
			}
			return true;
		}
		@Override
		boolean isSingleLeafTree() {
			ArrayList<Node> children = getChildren();
			if (children.size() == 1)
				return children.get(0).isSingleLeafTree();
			return false;
		}
		@Override
		boolean isAbsobable(Node sltx) {
			if (sltx.getClass() != getClass())
				throw new Error("class mismatch");
			TagNode<T> slt = (TagNode<T>) sltx;
			ArrayList<Node> children = getChildren();
			for (T tag: slt.branches.keySet()) {
				Node sltChild = slt.branches.get(tag);
				if (branches.get(tag) != null) {
					Node child = branches.get(tag);
					if (!child.isAbsobable(sltChild))
						return false;
				} else {
					boolean found = false;
					for (Node child: children)
						if (child.isAbsobable(sltChild)) {
							found = true;
							break;
						}
					if (!found)
						return false;
				}
			}
			return true;
		}
		void makeMergedNode(TagNode<T> n1, TagNode<T> n2) {
			LinkedHashSet<T> union = new LinkedHashSet<T>(n1.branches.keySet());
			union.addAll(n2.branches.keySet());
			for (T tag: union) {
				Node c1 = n1.branches.get(tag);
				Node c2 = n2.branches.get(tag);
				if (c1 == null)
					branches.put(tag, c2);
				else if (c2 == null)
					branches.put(tag, c1);
				else {
					Node child = c1.merge(c2);
					branches.put(tag, child);
				}
			}
			mergeChildren();
		}
		HashMap<Node, LinkedHashSet<T>> makeChildToTagsMap(HashMap<T, Node> tagToChild) {
			HashMap<Node, LinkedHashSet<T>> childToTags = new HashMap<Node, LinkedHashSet<T>>();
			for (T tag: tagToChild.keySet()) {
				Node child = tagToChild.get(tag);
				LinkedHashSet<T> tags = childToTags.get(child);
				if (tags == null) {
					tags = new LinkedHashSet<T>();
					childToTags.put(child, tags);
				}
				tags.add(tag);
			}
			return childToTags;
		}
		HashMap<Node, LinkedHashSet<T>> getChildToTagsMap() {
			return makeChildToTagsMap(branches);
		}
		@Override
		void mergeChildren() {
			/*
			if (this instanceof TagPairNode) {
				for (T tag: branches.keySet()) {
					Node child = branches.get(tag);
					child.mergeChildren();
				}
				return;
			}
			*/
			HashMap<Node, LinkedHashSet<T>> childToTags = makeChildToTagsMap(branches);	
			Node[] children = new Node[childToTags.size()];
			boolean[] hasMerged = new boolean[children.length];
			{
				int i = 0;
				for (Node child: childToTags.keySet()) {
					child.mergeChildren();
					children[i++] = child;
				}
			}
			branches = new HashMap<T, Node>();
			for (int i = 0; i < children.length; i++) {
				if (hasMerged[i])
					continue;
				LinkedHashSet<T> edge = childToTags.get(children[i]);
				Node merged = children[i];
				hasMerged[i] = true;
				for (int j = i + 1; j < children.length; j++) {
					if (!hasMerged[j] && isCompatible(merged, children[j])) {
						if (!checkMergeCriteria(children[j], merged))
							continue;
						merged = merged.merge(children[j]);
						edge.addAll(childToTags.get(children[j]));
						hasMerged[j] = true;
					}
				}
				for (T tag: edge)
					branches.put(tag, merged);
			}
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
		}
		@Override
		Node skipNoChoice() {
			HashMap<Node, LinkedHashSet<T>> childToTags = makeChildToTagsMap(branches);	
			if (childToTags.size() == 1) {
				return childToTags.keySet().iterator().next().skipNoChoice();
			}
			HashMap<Node, Node> replace = new HashMap<Node, Node>();
			for (Node before: childToTags.keySet()) {
				Node after = before.skipNoChoice();
				replace.put(before, after);
			}
			for (T tag: branches.keySet()) {
				Node before = branches.get(tag);
				branches.replace(tag, replace.get(before));
			}
			return this;
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
		Object accept(NodeVisitor visitor) {
			return visitor.visitTagPairNode(this);
		}
		@Override
		Node merge(Node otherx) {
			throw new Error("merge for TagPairNode is called");
		}
	}
	static class PTNode extends TagNode<PT> {
		PTNode(int opIndex) {
			super(opIndex);
		}
		@Override
		Object accept(NodeVisitor visitor) {
			return visitor.visitPTNode(this);
		}
		@Override
		Node merge(Node otherx) {
			PTNode other = (PTNode) otherx;
			PTNode merged = new PTNode(opIndex);
			merged.makeMergedNode(this, other);
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
			if (tag == null) {
				if (branches.size() != 0)
					throw new Error("invalid tag assignment");
				noHT = true;
				child = digger.dig(child);
			} else
				super.addBranch(digger, tag);
		}
		@Override
		Object accept(NodeVisitor visitor) {
			return visitor.visitHTNode(this);
		}
		@Override
		ArrayList<Node> getChildren() {
			if (noHT) {
				ArrayList<Node> r = new ArrayList<Node>(1);
				r.add(child);
				return r;
			}
			return super.getChildren();
		}
		boolean isNoHT() {
			return noHT;
		}
		Node getChild() {
			return child;
		}
		@Override
		boolean isAbsobable(Node sltx) {
			HTNode slt = (HTNode) sltx;
			if (slt.isNoHT()) {
				ArrayList<Node> children = getChildren();
				if (children.size() == 1)
					return children.get(0).isAbsobable(slt.getChild());
				return false;
			}
			return super.isAbsobable(sltx);
		}
		@Override
		Node merge(Node otherx) {
			HTNode other = (HTNode) otherx;
			if (noHT) {
				HTNode merged = new HTNode(opIndex);
				merged.noHT = true;
				merged.child = child.merge(other.getChildren().get(0));
				return merged;
			}
			HTNode merged = new HTNode(opIndex);
			merged.makeMergedNode(this, other);
			return merged;
		}
		// this.noTH == true && other.noTH == false
		Node superMerge(HTNode other) {
			HTNode merged = new HTNode(opIndex);
			merged.noHT = true;
			merged.branches = other.branches;
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
		Node skipNoChoice() {
			if (noHT)
				return child.skipNoChoice();
			return super.skipNoChoice();
		}
	}
	
	static class TreeDigger {
		final LLRuleSet.LLRule rule;
		final VMRepType[] rts;
		final int arity;
		int planIndex;
		
		TreeDigger(LLRuleSet.LLRule r) {
			rule = r;
			rts = r.getVMRepTypes();
			arity = rts.length;
			planIndex = 0;
		}
		
		Node dig(Node nodex) {
			if (planIndex == DISPATCH_PLAN.length)
				return new Leaf(rule);
			
			int dispatchType = DISPATCH_PLAN[planIndex++];
			if (dispatchType == DISPATCH_TAGPAIR && arity == 2) {
				TagPairNode node = nodex == null ? new TagPairNode() : (TagPairNode) nodex;
				node.addBranch(this, new TagPairNode.TagPair(rts[0].getPT(), rts[1].getPT()));
				return node;
			} else if (DISPATCH_PT_BASE <= dispatchType &&
					   dispatchType < DISPATCH_HT_BASE &&
					   dispatchType - DISPATCH_PT_BASE < arity) {
				int opIndex = dispatchType - DISPATCH_PT_BASE;
				PTNode node = nodex == null ? new PTNode(opIndex) : (PTNode) nodex;
				node.addBranch(this, rts[opIndex].getPT());
				return node;
			} else if (DISPATCH_HT_BASE <= dispatchType &&
					   dispatchType - DISPATCH_HT_BASE < arity) {
				int opIndex = dispatchType - DISPATCH_HT_BASE;
				HTNode node = nodex == null ? new HTNode(opIndex) : (HTNode) nodex;
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
		
//		System.out.println(generateCode(new String[] {"b1", "b2"}));

		root.mergeChildren();
		
//		System.out.println(generateCode(new String[] {"a1", "a2"}));
		
		root = root.skipNoChoice();

		//root.mergeChildren();
	}
	
	public String generateCode(String[] varNames) {
		return generateCodeForNode(root, varNames);
	}
	
	static String generateCodeForNode(Node node, String[] varNames) {
		CodeGenerateVisitor gen = new CodeGenerateVisitor(varNames);
		node.accept(gen);
		return gen.toString();
	}

	static boolean isCompatible(Node a, Node b) {
		IsCompatibleVisitor v = new IsCompatibleVisitor(a);
		return (Boolean) b.accept(v);
	}
	
	// precondition: a.isCompatibleTo(b)
	static boolean checkMergeCriteria(Node a, Node b) {
		if (a.isSingleLeafTree() && b.isSingleLeafTree()) {
			if (a.depth() != b.depth())
				throw new Error("depth does not match");
			return a.isAbsobable(b);
		}
		if (MERGE_LEVEL == 0) {
			return !(a.isSingleLeafTree() || b.isSingleLeafTree());
		} else if (MERGE_LEVEL <= 1) {
			if (a.isSingleLeafTree())
				return b.isAbsobable(a);
			if (b.isSingleLeafTree())
				return a.isAbsobable(a);
		}
		return true;
	}
}
