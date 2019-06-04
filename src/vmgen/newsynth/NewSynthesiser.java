/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmgen.newsynth;

import vmgen.RuleSet;
import vmgen.RuleSet.Condition;
import vmgen.Synthesiser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vmgen.InsnGen.Option;
import vmgen.newsynth.DecisionDiagram.DispatchCriterion;
import vmgen.newsynth.DecisionDiagram.HTNode;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.PTNode;
import vmgen.newsynth.DecisionDiagram.TagNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode;
import vmgen.newsynth.DecisionDiagram.TagPairNode.TagPair;

import vmgen.newsynth.LLRuleSet.LLRule;
import vmgen.type.VMDataType;
import vmgen.type.VMRepType;

public class NewSynthesiser extends Synthesiser {
    static final boolean UNSIGNED = true;
    class TagMacro extends CodeGenerateVisitor.Macro {
        @Override
        String getPTCode(String var) {
            return (UNSIGNED ? "(unsigned int) " : "")+NewSynthesiser.this.getPTCode(var);
        }

        @Override
        String getHTCode(String var) {
            return (UNSIGNED ? "(unsigned int) " : "")+NewSynthesiser.this.getHTCode(var);
        }

        @Override
        String composeTagPairCode(String... vars) {
            return (UNSIGNED ? "(unsigned int) " : "")+NewSynthesiser.this.composeTagPairCode(getPTCode(vars[0]), getPTCode(vars[1]));
        }

        @Override
        String composeTagPairLiteral(String... lits) {
            return NewSynthesiser.this.composeTagPairCode(lits);
        }

        @Override
        String getLabel() {
            return String.format("L%s%d", NewSynthesiser.this.labelPrefix, nextLabel++);
        }
    }

    static class LeafDispatch extends DispatchCriterion {
        @Override
        public boolean available(int arity) {
            return true;
        }
    }

    static class SemanticLayerGatherVisitor extends NodeVisitor<Void> {
        ArrayList<Node> nodes = new ArrayList<Node>();
        DispatchCriterion ref;

        SemanticLayerGatherVisitor(DispatchCriterion ref) {
            this.ref = ref;
        }

        ArrayList<Node> get() {
            return nodes;
        }

        @Override
        Void visitLeaf(Leaf node) {
            if (ref instanceof LeafDispatch) {
                if (!nodes.contains(node))
                    nodes.add(node);
                return null;
            }
            return null;
        }

        @Override
        <T> Void visitTagNode(TagNode<T> node) {
            if ((node instanceof DecisionDiagram.PTNode &&
                    ref instanceof DecisionDiagram.PTDispatch &&
                    node.opIndex == ((DecisionDiagram.PTDispatch) ref).opIndex) ||
                    (node instanceof DecisionDiagram.HTNode &&
                            ref instanceof DecisionDiagram.HTDispatch &&
                            node.opIndex == ((DecisionDiagram.HTDispatch) ref).opIndex)) {
                if (!nodes.contains(node))
                    nodes.add(node);
                return null;
            }
            for (Node child: node.getChildren())
                child.accept(this);
            return null;
        }
    }


    String labelPrefix;

    @Override
    public String synthesise(RuleSet hlrs, String labelPrefix, vmgen.InsnGen.Option option) {
        this.labelPrefix = labelPrefix;

        int srand;
        if ((srand = option.getOption(Option.AvailableOptions.CMP_RAND_SEED, -1)) >= 0) {
            DecisionDiagram.Node.srand(srand);
        }

        ArrayList<DecisionDiagram.DispatchCriterion> dispatchPlan = new ArrayList<DecisionDiagram.DispatchCriterion>();
        String layers = option.getOption(Option.AvailableOptions.CMP_TREE_LAYER, "p0:p1:h0:h1");
        for (String layer: layers.split(":")) {
            if (layer.equals("tp"))
                dispatchPlan.add(new DecisionDiagram.TagPairDispatch());
            else {
                int opIndex = Integer.parseInt(layer.substring(1));
                if (layer.charAt(0) == 'p')
                    dispatchPlan.add(new DecisionDiagram.PTDispatch(opIndex));
                else if (layer.charAt(0) == 'h')
                    dispatchPlan.add(new DecisionDiagram.HTDispatch(opIndex));
                else
                    throw new Error();
            }
        }

        LLRuleSet llrs = new LLRuleSet(hlrs);
        DecisionDiagram dd = new DecisionDiagram(dispatchPlan, llrs, option);
        if (dd.isEmpty())
            return "";

        // optimize
        String passes = option.getOption(Option.AvailableOptions.CMP_OPT_PASS, "MR:S");
        for (String pass: passes.split(":")) {
            switch(pass) {
            case "MR": dd.combineRelative(); break;
            case "S":  dd.skipBranchless();  break;
            }
        }

        ArrayList<DecisionDiagram.DispatchCriterion> testDispatchPlan = new ArrayList<DecisionDiagram.DispatchCriterion>(dispatchPlan);
        testDispatchPlan.add(new LeafDispatch());
        for (DispatchCriterion dc: testDispatchPlan) {
            SemanticLayerGatherVisitor gv = new SemanticLayerGatherVisitor(dc);
            dd.root.accept(gv);
            ArrayList<Node> nodes = gv.get();
            for (int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                for (int j = i + 1; j < nodes.size(); j++) {
                    Node m = nodes.get(j);
                    if (DecisionDiagram.isConsistent(n, m)) {
                        System.out.print("remaining consistent nodes: ");
                        System.out.println(n +" "+ m);
                    }
                }
            }
        }

        if (option.getOption(Option.AvailableOptions.CMP_VERIFY_DIAGRAM, true)) {
            for (LLRule llr: llrs.getRules()) {
                VMRepType[] rts = llr.getVMRepTypes();
                LLRule found = dd.search(rts);
                if (llr.getHLRule() != found.getHLRule()) {
                    System.out.println("wrong decision diagram: ");
                    for (VMRepType r: rts)
                        System.out.println(" "+r);
                }
            }
        }

        Map<Node, Set<String>> typeLabels;
        if (option.getOption(Option.AvailableOptions.GEN_ADD_TYPELABEL, false))
            typeLabels = addTypeLabels(dd, hlrs);
        else
            typeLabels = null;

        return dd.generateCode(hlrs.getDispatchVars(), new TagMacro(), typeLabels);
    }


    static class DeterministicSearchVisitor extends NodeVisitor<Node> {
        VMDataType[] dts;

        DeterministicSearchVisitor(VMDataType[] dts) {
            this.dts = dts;
        }

        VMRepType getUniqueVMRepType(int index) {
            if (dts.length < index)
                return null;
            if (dts[index] == null)
                return null;
            List<VMRepType> rts = dts[index].getVMRepTypes();
            if (rts.size() > 1)
                return null;
            return rts.get(0);
        }

        @Override
        Node visitLeaf(Leaf node) {
            return node;
        }

        @Override
        Node visitTagPairNode(TagPairNode node) {
            if (dts.length != 2)
                return node;
            VMRepType rt0 = getUniqueVMRepType(0);
            VMRepType rt1 = getUniqueVMRepType(1);
            if (rt0 == null || rt1 == null)
                return node;
            TagPair tp = new TagPair(rt0.getPT(), rt1.getPT());
            Node next = node.getChild(tp);
            return next.accept(this);
        }

        @Override
        Node visitPTNode(PTNode node) {
            VMRepType rt = getUniqueVMRepType(node.opIndex);
            if (rt == null)
                return node;
            Node next = node.getChild(rt.getPT());
            return next.accept(this);
        }

        @Override
        Node visitHTNode(HTNode node) {
            if (node.isNoHT())
                return node.getChild().accept(this);

            VMRepType rt = getUniqueVMRepType(node.opIndex);
            if (rt == null)
                return node;
            Node next = node.getChild(rt.getHT());
            return next.accept(this);
        }
    }

    private void addLabel(Map<Node, Set<String>> labels, VMDataType[] dts, Node node) {
        StringBuffer sb = new StringBuffer(labelPrefix);
        for (VMDataType dt: dts) {
            if (dt == null)
                sb.append("_any");
            else
                sb.append("_").append(dt.getName());
        }
        String label = sb.toString();
        if (!labels.containsKey(node))
            labels.put(node, new HashSet<String>());
        labels.get(node).add(label);
    }

    private Map<Node, Set<String>> addTypeLabels(DecisionDiagram dd, RuleSet hlrs) {
        Map<Node, Set<String>> labels = new HashMap<Node, Set<String>>();
        for (RuleSet.Rule r: hlrs.getRules()) {
            for (Condition templateCondition: r.getCondition()) {
                VMDataType[] templateDts = templateCondition.dts;
                for (int mask = 0; mask < (1 << templateDts.length); mask++) {
                    VMDataType[] dts = templateDts.clone();
                    for (int i = 0; i < templateDts.length; i++)
                        if ((mask & (1 << i)) != 0)
                            dts[i] = null;
                    DeterministicSearchVisitor v = new DeterministicSearchVisitor(dts);
                    Node node = dd.root.accept(v);
                    addLabel(labels, dts, node);
                }
            }
        }
        return labels;
    }
}
