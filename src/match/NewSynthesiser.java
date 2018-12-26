/*
   NewSynthesiser.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
 */
package match;

import vmgen.RuleSet;
import vmgen.Synthesiser;

import java.util.ArrayList;

import vmgen.InsnGen.Option;
import vmgen.newsynth.DecisionDiagram.DispatchCriterion;
import vmgen.newsynth.DecisionDiagram.Leaf;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.DecisionDiagram.TagNode;
import vmgen.newsynth.LLRuleSet.LLRule;
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
        if (option.getOption(Option.AvailableOptions.CMP_USE_TAGPAIR, true))
            dispatchPlan.add(new DecisionDiagram.TagPairDispatch());
        for (int i = 0; i < 5; i++)
            dispatchPlan.add(new DecisionDiagram.PTDispatch(i));
        for (int i = 0; i < 5; i++)
            dispatchPlan.add(new DecisionDiagram.HTDispatch(i));

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
        return dd.generateCode(hlrs.getDispatchVars(), new TagMacro());
    }
}
