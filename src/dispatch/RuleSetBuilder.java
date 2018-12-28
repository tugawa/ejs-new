package dispatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dispatch.RuleSet.OperandDataTypes;
import type.VMDataType;

public class RuleSetBuilder {
    public static class Node {
    }
    
    public static class TrueNode extends Node {
    }
    
    public class AtomicNode extends Node {
        int opIndex;
        VMDataType dt;
        boolean neg;
        
        public AtomicNode(String opName, VMDataType dt) {
            this.opIndex = lookup(opName);
            this.dt = dt;
            neg = false;
        }
        
        @Override
        public String toString() {
            return "["+opIndex+":"+dt+"]";
        }
    }
    
    public static class AndNode extends Node {
        Node left, right;
        
        public AndNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }
        
        @Override
        public String toString() {
            return "(and "+left+" "+right+")";
        }
    }
    
    public static class OrNode extends Node {
        Node left, right;
        
        public OrNode(Node left, Node right) {
            this.left = left;
            this.right = right;
        }
        
        @Override
        public String toString() {
            return "(or "+left+" "+right+")";
        }
    }
    
    public static class NotNode extends Node {
        Node child;
        
        public NotNode(Node child) {
            this.child = child;
        }
        
        @Override
        public String toString() {
            return "(not "+child+")";
        }
    }
    
    String[] opNames;
    
    public RuleSetBuilder(String[] opNames) {
        this.opNames = opNames;
    }
    
    int lookup(String name) {
        for (int i = 0; i < opNames.length; i++)
            if (opNames[i].equals(name))
                return i;
        throw new Error("Unknown operand name: " + name);
    }
    
    Node removeNotNode(Node xn, boolean neg) {
        if (xn instanceof TrueNode)
            return xn;
        else if (xn instanceof AtomicNode) {
            ((AtomicNode) xn).neg = neg;
            return xn;
        } else if (xn instanceof AndNode) {
            AndNode n = (AndNode) xn;
            n.left = removeNotNode(n.left, neg);
            n.right = removeNotNode(n.right, neg);
            if (neg)
                return new OrNode(n.left, n.right);
            return n;
        } else if (xn instanceof OrNode) {
            OrNode n = (OrNode) xn;
            n.left = removeNotNode(n.left, neg);
            n.right = removeNotNode(n.right, neg);
            if (neg)
                return new AndNode(n.left, n.right);
            return n;
        } else if (xn instanceof NotNode)
            return removeNotNode(((NotNode) xn).child, !neg);
        throw new Error("Unkown datatype expr node");
    }
    
    // siftUpOrNode moves a single or-node in subtree xn to the root of the tree.
    // If it finds an or-node, it returns the converted tree.
    // Otherwise, it returns null.
    OrNode siftUpOrNode(Node xn) {
        if (xn instanceof TrueNode)
            throw new Error("TrueNode should not appear in this step.");
        else if (xn instanceof AtomicNode)
            return null;
        else if (xn instanceof AndNode) {
            AndNode n = (AndNode) xn;
            OrNode left = siftUpOrNode(n.left);
            if (left != null) {
                /* left should be an or-node */
                AndNode leftAnd = new AndNode(left.left, n.right);
                AndNode rightAnd = new AndNode(left.right, n.right);
                return new OrNode(leftAnd, rightAnd);
            }
            OrNode right = siftUpOrNode(n.right);
            if (right != null) {
                /* right should be an or-node */
                AndNode leftAnd = new AndNode(n.left, right.left);
                AndNode rightAnd = new AndNode(n.left, right.right);
                return new OrNode(leftAnd, rightAnd);
            }
            return null;
        } else if (xn instanceof OrNode)
            return (OrNode) xn;
        else if (xn instanceof NotNode)
            throw new Error("NotNode should not appear in this step.");
        throw new Error("Unknown datatype expr node");
    }
    
    Node toDNF(Node xn) {
        if (xn instanceof TrueNode)
            return xn;
        else if (xn instanceof AtomicNode)
            return xn;
        else if (xn instanceof AndNode) {
            AndNode n = (AndNode) xn;
            OrNode orNode = siftUpOrNode(n);
            if (orNode != null)
                return toDNF(orNode);
            return n;
        } else if (xn instanceof OrNode) {
            OrNode n = (OrNode) xn;
            n.left = toDNF(n.left);
            n.right = toDNF(n.right);
            return n;
        } else if (xn instanceof NotNode)
            throw new Error("NotNode should not appear in this step.");
        throw new Error("Unknown datatype expr node");
    }
    
    Node normalise(Node root) {
        // Step 1. Remove NotNode
        root = removeNotNode(root, false);
        // Step 2. Convert to disjunctive normal form
        root = toDNF(root);
        
        return root;
    }

    void andTreeToTable(Node xn, int[] table) {
        if (xn instanceof TrueNode)
            throw new Error("TrueNode should not appear in this step.");
        else if (xn instanceof AtomicNode) {
            AtomicNode n = (AtomicNode) xn;
            if (n.neg)
                table[n.opIndex] &= ~(1 << n.dt.getID());
            else
                table[n.opIndex] &= (1 << n.dt.getID());
            return;
        } else if (xn instanceof AndNode) {
            AndNode n = (AndNode) xn;
            andTreeToTable(n.left, table);
            andTreeToTable(n.right, table);
            return;
        } else if (xn instanceof OrNode)
            throw new Error("OrNode should not appear in this step.");
        else if (xn instanceof NotNode)
            throw new Error("NotNode should not appear in this step.");
        throw new Error("unknown datatype expr node");
    }
    
    int[] newTable(Set<int[]> tables) {
        int[] table = new int[opNames.length];
        for (int i = 0; i < opNames.length; i++)
            table[i] = (1 << VMDataType.size()) - 1;
        tables.add(table);
        return table;
    }
    
    void traverseOrTree(Node xn, Set<int[]> tables) {
        if (xn instanceof TrueNode) {
            newTable(tables);
            return;
        } else if (xn instanceof AtomicNode) {
            andTreeToTable(xn, newTable(tables));
            return;
        } else if (xn instanceof AndNode) {
            andTreeToTable(xn, newTable(tables));
            return;
        } else if (xn instanceof OrNode) {
            OrNode n = (OrNode) xn;
            traverseOrTree(n.left, tables);
            traverseOrTree(n.right, tables);
            return;
        } else if (xn instanceof NotNode)
            throw new Error("NotNode should not appear in this step.");
        throw new Error("unknown datatype expr node");
    }
    
    Set<int[]> DNFToDispatchConditionTables(Node root) {
        Set<int[]> tables = new HashSet<int[]>();
        traverseOrTree(root, tables);
        return tables;
    }
    
    static public class CaseActionPair {
        Node kase;
        String action;
        public CaseActionPair(Node kase, String action) {
            this.kase = kase;
            this.action = action;
        }
    }
    
    static public class Rule {
        Set<int[]> tables;
        Set<OperandDataTypes> condition;
        String action;
        Rule(Set<int[]> tables, String action) {
            this.tables = tables;
            this.action = action;
            condition = new HashSet<OperandDataTypes>();
        }
    }
    
    void fillRules(int opIndex, VMDataType[] dts, List<Rule> rules) {
        if (opIndex >= opNames.length) {
            int[] dtids = new int[dts.length];
            for (int i = 0; i < dts.length; i++)
                dtids[i] = dts[i].getID();
            for (Rule rule: rules)
                NEXT_TABLE: for (int[] table: rule.tables) {
                    for (int i = 0; i < dts.length; i++)
                        if ((table[i] & (1 << dtids[i])) == 0)
                            continue NEXT_TABLE;
                    rule.condition.add(new OperandDataTypes(dts.clone()));
                    return;
                }
            return;
        }
        
        for (VMDataType dt: VMDataType.all()) {
            dts[opIndex] = dt;
            fillRules(opIndex + 1, dts, rules);
        }
    }
    
    public List<Set<VMDataType[]>> computeVmtVecCondList(List<Node> condAstList) {
        List<Rule> rules = new ArrayList<Rule>();
        
        for (Node condAst: condAstList) {
            Node dnf = normalise(condAst);
            Set<int[]> tables = DNFToDispatchConditionTables(dnf);
            rules.add(new Rule(tables, null));  /* TODO: do not use Rule */
        }
        
        fillRules(0, new VMDataType[opNames.length], rules);
        
        List<Set<VMDataType[]>> possibleTypeCombList = new ArrayList<Set<VMDataType[]>>();
        for (Rule rule: rules) {
            Set<VMDataType[]> possibleTypeCombs = new HashSet<VMDataType[]>();
            for (OperandDataTypes ots: rule.condition)
                possibleTypeCombs.add(ots.dts);
            possibleTypeCombList.add(possibleTypeCombs);
        }
        return possibleTypeCombList;
    }
    
    public RuleSet createRuleSet(List<CaseActionPair> caps) {
        List<Rule> rules = new ArrayList<Rule>();
        
        for (CaseActionPair cap: caps) {
            Node dnf = normalise(cap.kase);
            Set<int[]> tables = DNFToDispatchConditionTables(dnf);
            rules.add(new Rule(tables, cap.action));
        }
        
        fillRules(0, new VMDataType[opNames.length], rules);
        
        Set<RuleSet.Rule> rs = new HashSet<RuleSet.Rule>();
        for (Rule rule: rules) {
            if (rule.condition.size() > 0) {
                RuleSet.Rule r = new RuleSet.Rule(rule.action, rule.condition);
                rs.add(r);
            }
        }
        return new RuleSet(opNames, rs);
    }
    
    static void printTables(Set<int[]> tables) {
        for(int[] table: tables) {
            for (int vec: table) {
                for (int i = 0; i < VMDataType.size(); i++)
                    System.out.print(((vec & (1 << i)) == 0) ? "0" : "1");
                System.out.println();
            }
            System.out.println();
        }
    }
    
    /*
    public static void main(String[] args) {
        DataTypeExpression dte = new DataTypeExpression(new String[] {"a", "b", "c"});
        
        Node a = dte.new AtomicNode("a", VMDataType.get("fixnum"));
        Node b = dte.new AtomicNode("b", VMDataType.get("fixnum"));
        Node c = dte.new AtomicNode("b", VMDataType.get("string"));
        Node d = dte.new AtomicNode("b", VMDataType.get("fixnum"));
        Node orl = new OrNode(a, b);
        Node orr = new OrNode(c, d);
        Node and = new AndNode(orl, orr);
        
        Node x = dte.new AtomicNode("a", VMDataType.get("fixnum"));
        
        List<CaseActionPair> caps = new ArrayList<CaseActionPair>();
        caps.add(new CaseActionPair(and, "A"));
        caps.add(new CaseActionPair(x, "B"));
        
        RuleSet rs = dte.createRuleSet(caps);
        for (RuleSet.Rule r: rs.getRules()) {
            System.out.println("Rule action "+r.action);
            for (OperandDataTypes dts: r.condition) {
                for (VMDataType dt: dts.dts)
                    System.out.print(dt+" ");
                System.out.println();
            }
        }
    }
    */
}
