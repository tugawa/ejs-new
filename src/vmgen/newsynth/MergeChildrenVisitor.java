package vmgen.newsynth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.TagNode;

public class MergeChildrenVisitor extends NodeVisitor<Void> {

	static class IsSingleLeafTreeVisitor extends NodeVisitor<Boolean> {

		@Override
		Boolean visitLeaf(Leaf node) {
			return true;
		}

		@Override
		<T> Boolean visitTagNode(TagNode<T> node) {
			ArrayList<Node> children = node.getChildren();
			if (children.size() == 1)
				return children.get(0).accept(this);
			return false;
		}
	}
	
	static class IsAbsobableVisitor extends NodeVisitor<Boolean> {
		Node absoberx;
		
		IsAbsobableVisitor(Node absober) {
			this.absoberx = absober;
		}
		
		@Override
		Boolean visitLeaf(Leaf slt) {
			return ((Leaf) absoberx).hasSameHLRule(slt);
		}
		
		@Override
		<T> Boolean visitTagNode(TagNode<T> slt) {
			if (absoberx.getClass() != slt.getClass())
				throw new Error("class mismatch");
			TagNode<T> absober = (TagNode<T>) absoberx;
			ArrayList<Node> children = absober.getChildren();
			for (T tag: slt.branches.keySet()) {
				Node sltChild = slt.branches.get(tag);
				Node child = absober.getChild(tag);
				if (child != null) {
					absoberx = child;
					if (!sltChild.accept(this))
						return false;
				} else {
					boolean found = false;
					for (Node c: children) {
						absoberx = c;
						if (sltChild.accept(this)) {
							found = true;
							break;
						}
					}
					if (!found)
						return false;
				}
			}
			return true;
		}
		
		@Override
		Boolean visitHTNode(HTNode slt) {
			if (slt.isNoHT()) {
				ArrayList<Node> children = absoberx.getChildren();
				if (children.size() == 1) {
					absoberx = children.get(0);
					return slt.getChild().accept(this);
				}
				return false;
			}
			return visitTagNode(slt);
		}
	}

	@Override
	Void visitLeaf(Leaf node) {
		return null;
	}

	<T> boolean hasCompatibleBranches(TagNode<T> na, TagNode<T> nb) {
		if (na.getOpIndex() != nb.getOpIndex())
			throw new Error("opIndex mismatch");
		Set<T> union = na.getEdges();
		union.addAll(nb.getEdges());
		for (T tag: union) {
			Node ca = na.getChild(tag);
			Node cb = nb.getChild(tag);
			if (ca != null && cb != null && !DecisionDiagram.isCompatible(ca, cb))
				return false;
		}
		return true;
	}

	@Override
	<T> Void visitTagNode(TagNode<T> node) {
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
				if (!checkMergeCriteria(cj, merged))
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
		
		// this.noTH == true && other.noTH == false
		// Node superMerge(HTNode other) {
		//	HTNode merged = new HTNode(opIndex);
		//	merged.noHT = true;
		//	merged.branches = other.branches;
		//  return merged;
		// }

		*/
		return null;
	}

	@Override
	Void visitHTNode(HTNode node) {
		if (node.isNoHT())
			return node.getChild().accept(this);
		else
			return visitTagNode(node);
	}
	
	static boolean isSingleLeafTree(Node node) {
		IsSingleLeafTreeVisitor v = new IsSingleLeafTreeVisitor();
		return node.accept(v);
	}
	
	// absobee must be a single leaf tree
	static boolean isAbsobable(Node absober, Node absobee) {
		IsAbsobableVisitor v = new IsAbsobableVisitor(absober);
		return absobee.accept(v);
	}

	// precondition: a.isCompatibleTo(b)
	static boolean checkMergeCriteria(Node a, Node b) {
		if (isSingleLeafTree(a) && isSingleLeafTree(b)) {
			if (a.depth() != b.depth())
				throw new Error("depth does not match");
			return isAbsobable(a, b);
		}
		if (DecisionDiagram.MERGE_LEVEL == 0) {
			return !(isSingleLeafTree(a) || isSingleLeafTree(b));
		} else if (DecisionDiagram.MERGE_LEVEL <= 1) {
			if (isSingleLeafTree(a))
				return isAbsobable(b, a);
			if (isSingleLeafTree(b))
				return isAbsobable(a, a);
		}
		return true;
	}
}
