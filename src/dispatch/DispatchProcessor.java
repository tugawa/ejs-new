/*
   NewSynthesiser.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
 */
package dispatch;

import java.util.ArrayList;

import dispatch.DecisionDiagram.Node;
import dispatch.LLRuleSet.LLRule;
import type.VMDataType;
import type.VMRepType;

public class DispatchProcessor {
    static final boolean CMP_VERIFY_DIAGRAM = true;
    
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
    public String translate(RuleSet hlrs, DispatchPlan dispatchPlan) {
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
        // Step 3-1. Optimisation (combine consistent nodes)
        //
        dd.combineRelative();
        
        //
        // Step 3-2. Optimization (skip branchless nodes)
        //
        dd.skipBranchless();
        
        verifyODDInvariants(dd, hlrs, llrs, dispatchPlan);
        
        //
        // Step 4. Code Generation
        //
        return dd.generateCode(hlrs.getDispatchVars(), new TagMacro());
    }
    
    
    //////
    //////  verification
    //////
    
    public void verifyODDInvariants(DecisionDiagram dd, RuleSet hlrs, LLRuleSet llrs, DispatchPlan dispatchPlan) {
        if (CMP_VERIFY_DIAGRAM) {
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

    }

}

