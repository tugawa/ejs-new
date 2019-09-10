/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import dispatch.LLRuleSet.LLRule;
import type.VMRepType;
import type.VMRepType.HT;
import type.VMRepType.PT;

import vmdlc.Option;

public class DecisionDiagram {
    public static int MERGE_LEVEL = 2; // 0-2: 0 is execution spped oriendted, 2 is size oriented

    static class PTDispatch extends DispatchPlan.DispatchCriterion  {
        int opIndex;
        public PTDispatch(int n) {
            opIndex = n;
        }
        @Override
        public boolean available(int arity) {
            return opIndex < arity;
        }
        int getOpIndex() {
            return opIndex;
        }
    }

    static class HTDispatch extends DispatchPlan.DispatchCriterion {
        int opIndex;
        public HTDispatch(int n) {
            opIndex = n;
        }
        @Override
        public boolean available(int arity) {
            return opIndex < arity;
        }
        int getOpIndex() {
            return opIndex;
        }
    }

    static abstract class Node implements Comparable<Node> {
        static int lastId = 0;
        static java.util.Random r = new java.util.Random();
        static {
            r.setSeed(0);
        }
        static void srand(int n) {
            r.setSeed(n);
        }

        int id = r.nextInt(10000); //lastId++;
        int genOrder = lastId++;

        @Override
        public int hashCode() {
            return id;
        }

        abstract <R> R accept(NodeVisitor<R> visitor);

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

        // other should be consistent with this
        // this method does not mutate this object
        abstract Node combine(Node other);

        @Override
        public int compareTo(Node other) {
            if (this.id == other.id) {
                return this.genOrder - other.genOrder;
            }
            return this.id - other.id;
        }
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
        <R> R accept(NodeVisitor<R> visitor) {
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
        Node combine(Node otherx) {
            return this;
        }
    }

    static abstract class TagNode<T> extends Node {
        int opIndex;
        TreeMap<T, Node> branches = new TreeMap<T, Node>();

        TagNode(int opIndex) {
            this.opIndex = opIndex;
        }
        void addBranch(TreeDigger digger, T tag) {
            Node child = branches.get(tag);
            child = digger.dig(child);
            branches.put(tag, child);
        }
        @Override
        <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitTagNode(this);
        }
        @Override
        ArrayList<Node> getChildren() {
            TreeSet<Node> s = new TreeSet<Node>();
            for (T tag: branches.keySet())
                s.add(branches.get(tag));
            return new ArrayList<Node>(s);
        }
        Set<T> getEdges() {
            return branches.keySet();
        }
        void replaceChild(T tag, Node child) {
            branches.replace(tag, child);
        }
        Node getChild(T tag) {
            return branches.get(tag);
        }
        int getOpIndex() {
            return opIndex;
        }
        void makeCombinedNode(TagNode<T> n1, TagNode<T> n2) {
            TreeSet<T> union = new TreeSet<T>(n1.branches.keySet());
            union.addAll(n2.branches.keySet());
            for (T tag: union) {
                Node c1 = n1.branches.get(tag);
                Node c2 = n2.branches.get(tag);
                if (c1 != null)
                    branches.put(tag, c1);
                else
                    branches.put(tag, c2);
            }
        }
        TreeMap<Node, TreeSet<T>> getChildToTagsMap() {
            TreeMap<Node, TreeSet<T>> childToTags = new TreeMap<Node, TreeSet<T>>();
            for (T tag: branches.keySet()) {
                Node child = branches.get(tag);
                TreeSet<T> tags = childToTags.get(child);
                if (tags == null) {
                    tags = new TreeSet<T>();
                    childToTags.put(child, tags);
                }
                tags.add(tag);
            }
            return childToTags;
        }
    }

    static class TagPairNode extends TagNode<TagPairNode.TagPair> {
        static class TagPair implements Comparable<TagPair> {
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
            @Override
            public int compareTo(TagPair other) {
                return this.hashCode() - other.hashCode();
            }
            public int getValue() {
                return (op2.getValue() << 3 | op1.getValue());
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
        <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitTagPairNode(this);
        }
        @Override
        Node combine(Node otherx) {
            throw new Error("combine for TagPairNode is called");
        }
    }

    static class PTNode extends TagNode<PT> {
        PTNode(int opIndex) {
            super(opIndex);
        }
        @Override
        <R> R accept(NodeVisitor<R> visitor) {
            return visitor.visitPTNode(this);
        }
        @Override
        Node combine(Node otherx) {
            PTNode other = (PTNode) otherx;
            PTNode combined = new PTNode(opIndex);
            combined.makeCombinedNode(this, other);
            return combined;
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
        <R> R accept(NodeVisitor<R> visitor) {
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
        void replaceChild(Node node) {
            child = node;
        }
        @Override
        Node combine(Node otherx) {
            HTNode other = (HTNode) otherx;
            if (noHT || other.noHT) {
                HTNode combined = new HTNode(opIndex);
                combined.noHT = true;
                combined.child = getChildren().get(0);
                return combined;
            }
            HTNode combined = new HTNode(opIndex);
            combined.makeCombinedNode(this, other);
            return combined;
        }
    }

    class TreeDigger {
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
            if (planIndex == dispatchPlan.size())
                return new Leaf(rule);

            DispatchPlan.DispatchCriterion dispatchCriterion = dispatchPlan.get(planIndex++);
            if (!dispatchCriterion.available(arity))
                return dig(nodex);
            if (dispatchCriterion instanceof DispatchPlan.TagPairDispatch) {
                TagPairNode node = nodex == null ? new TagPairNode() : (TagPairNode) nodex;
                node.addBranch(this, new TagPairNode.TagPair(rts[0].getPT(), rts[1].getPT()));
                return node;
            } else if (dispatchCriterion instanceof PTDispatch) {
                int opIndex = ((PTDispatch) dispatchCriterion).getOpIndex();
                PTNode node = nodex == null ? new PTNode(opIndex) : (PTNode) nodex;
                node.addBranch(this, rts[opIndex].getPT());
                return node;
            } else if (dispatchCriterion instanceof HTDispatch) {
                int opIndex = ((HTDispatch) dispatchCriterion).getOpIndex();
                HTNode node = nodex == null ? new HTNode(opIndex) : (HTNode) nodex;
                node.addBranch(this, rts[opIndex].getHT());
                return node;
            } else
                return dig(nodex);
        }
    }

    Node root;
    List<DispatchPlan.DispatchCriterion> dispatchPlan;

    public DecisionDiagram(LLRuleSet rs, DispatchPlan dispatchPlan) {
        this.dispatchPlan = dispatchPlan.getPlan();

        if (rs.getRules().size() == 0)
            return;
        for (LLRuleSet.LLRule r : rs.getRules()) {
            TreeDigger digger = new TreeDigger(r);
            root = digger.dig(root);
        }
    }

    public boolean isEmpty() {
        return root == null;
    }

    public String generateCode(String[] varNames, CodeGenerateVisitor.Macro tagMacro, Option option, Map<Node, Set<String>> typeLabels, String labelPrefix) {
        return generateCodeForNode(root, varNames, tagMacro, option, typeLabels, labelPrefix);
    }

    public void skipBranchless() {
        root = skipBranchless(root);
    }

    public void combineRelative() {
        combineRelative(root);
    }

    ////
    // static method
    ////

    static String generateCodeForNode(Node node, String[] varNames, CodeGenerateVisitor.Macro tagMacro, Option option, Map<Node, Set<String>> typeLabels, String labelPrefix) {
        CodeGenerateVisitor gen = new CodeGenerateVisitor(varNames, tagMacro, option, typeLabels, labelPrefix);
        node.accept(gen);
        return gen.toString();
    }

    static boolean isConsistent(Node a, Node b) {
        IsConsistentVisitor v = new IsConsistentVisitor(a);
        return  (Boolean) b.accept(v);
    }

    static Node skipBranchless(Node node) {
        SkipBranchlessVisitor v = new SkipBranchlessVisitor();
        return (Node) node.accept(v);
    }

    static public void combineRelative(Node node) {
        RelativeCombine m = new RelativeCombine();
        m.combineRelative(node);
    }

    ////
    // Debug Code
    ////

    public LLRule search(VMRepType[] rts) {
        SearchVisitor v = new SearchVisitor(rts);
        return (LLRule) root.accept(v);
    }

    /*
    static String debugGenerateCodeForNode(Node node) {
        return generateCodeForNode(node, new String[] {"a", "b", "c", "d", "e"}, new CodeGenerateVisitor.Macro());
    }
     */
}
