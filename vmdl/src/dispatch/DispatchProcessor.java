/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package dispatch;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import dispatch.DecisionDiagram.Node;
import dispatch.DecisionDiagram.Leaf;
import dispatch.DecisionDiagram.HTNode;
import dispatch.DecisionDiagram.PTNode;
import dispatch.DecisionDiagram.TagPairNode;
import dispatch.DecisionDiagram.TagPairNode.TagPair;
import dispatch.LLRuleSet.LLRule;
import dispatch.RuleSet.OperandDataTypes;
import type.VMDataType;
import type.VMRepType;
import vmdlc.Option;

public class DispatchProcessor {
    static public void srand(int seed) {
        DecisionDiagram.Node.srand(seed);
    }

    static final boolean UNSIGNED = true;

    class TagMacro extends CodeGenerateVisitor.Macro {        
        @Override
        String getPTCode(String var) {
            return (UNSIGNED ? "(unsigned int) " : "")+"get_tag("+var+")";
        }

        @Override
        String getHTCode(String var) {
            return (UNSIGNED ? "(unsigned int) " : "")+"gc_obj_header_type((void*) clear_tag("+var+"))";
        }

        @Override
        String composeTagPairCode(String... vars) {
            return (UNSIGNED ? "(unsigned int) " : "")+"TAG_PAIR("+getPTCode(vars[0])+", "+getPTCode(vars[1])+")";
        }

        @Override
        String composeTagPairLiteral(String... lits) {
            return (UNSIGNED ? "(unsigned int) " : "")+"TAG_PAIR("+lits[0]+", "+lits[1]+")";
        }

        @Override
        String getLabel() {
            return String.format("L%s%d", labelPrefix, nextLabel++);
        }
    }

    String labelPrefix = "";

    public DispatchProcessor() {}

    public void setLabelPrefix(String prefix) {
        labelPrefix = prefix;
    }

    //
    // Translate half-normalised rule set into datatype dispatching code
    //
    public String translate(RuleSet hlrs, DispatchPlan dispatchPlan, Option option, String currentFunctionName, String currentMatchLabelName) {
        //
        // Step 1-3. Decompose VMDatatype to VMreptype in rules
        //
        LLRuleSet llrs = new LLRuleSet(hlrs);
        if (llrs.getRules().size() == 0)
            return "";

        //
        // Step 2. Construct a decision tree
        //
        DecisionDiagram dd = new DecisionDiagram(llrs, dispatchPlan);


        //
        // Step 3. Optimisation
        //
        String passes = option.getOption(Option.AvailableOptions.CMP_OPT_PASS, "MR:S");
        for (String pass: passes.split(":")) {
            switch(pass) {

            case "MR":
                // combine consistent nodes
                dd.combineRelative(); break;
            case "S":
                // skip branchless nodes
                dd.skipBranchless();  break;
            }
        }

        //
        // Step 4. Verify diagram
        //
        if (option.getOption(Option.AvailableOptions.CMP_VERIFY_DIAGRAM, true)) {
            verifyODDInvariants(dd, hlrs, llrs, dispatchPlan);
        }


        //
        // Step 5.
        //
        Map<Node, Set<String>> typeLabels;
        if (option.getOption(Option.AvailableOptions.GEN_ADD_TYPELABEL, false))
            typeLabels = addTypeLabels(dd, hlrs);
        else
            typeLabels = null;

        //
        // Step 6. Code Generation
        //
        String labelPrefix = option.getOption(Option.AvailableOptions.GEN_LABEL_PREFIX, currentFunctionName) + currentMatchLabelName;
        return dd.generateCode(hlrs.getDispatchVars(), new TagMacro(), option, typeLabels, labelPrefix);
    }


    //////
    //////  verification
    //////

    public void verifyODDInvariants(DecisionDiagram dd, RuleSet hlrs, LLRuleSet llrs, DispatchPlan dispatchPlan) {
        //
        // Check consistent node pairs are not remaining.
        // This property is not always the case (see paper).
        // However, we expect this is holds in practical cases.
        // If not, we need to improve the optimiser.
        //
        ArrayList<DispatchPlan.DispatchCriterion> testDispatchPlan = new ArrayList<DispatchPlan.DispatchCriterion>(dispatchPlan.getPlan());
        testDispatchPlan.add(new SemanticLayerGatherVisitor.LeafDispatch());
        for (DispatchPlan.DispatchCriterion dc: testDispatchPlan) {
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

        //
        // Check if the optimised ODD defines all the cases in the given rules.
        //
        boolean hasError = false;
        for (LLRule llr: llrs.getRules()) {
            VMRepType[] rts = llr.getVMRepTypes();
            LLRule found = dd.search(rts);
            if (llr.getHLRule() != found.getHLRule()) {
                hasError = true;
                System.out.println("wrong decision diagram: ");
                for (VMRepType r: rts) {
                    if (found == null)
                        System.out.println(" "+r+" -> not found");
                    else
                        System.out.println(" "+r+" -> "+llr.hlr.action + " => " + found.hlr.action);
                }
            }
        }
        if (hasError) {
            System.out.println("======== Rule Set Begin ========");
            System.out.println(hlrs.dump());
            System.out.println("======== Rule Set End ========");
        }
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
        StringBuffer sb = new StringBuffer();
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
            for (OperandDataTypes templateCondition: r.getCondition()) {
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

