/*
   NewSynthesiser.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
 */
package vmgen.newsynth;

import vmgen.RuleSet;
import vmgen.Synthesiser;

import java.util.ArrayList;

import vmgen.InsnGen.Option;
import vmgen.newsynth.DecisionDiagram.DispatchCriterion;
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

    String labelPrefix;

    @Override
    public String synthesise(RuleSet hlrs, String labelPrefix, vmgen.InsnGen.Option option) {
        this.labelPrefix = labelPrefix;
        /*
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
        */
        ArrayList<DecisionDiagram.DispatchCriterion> dispatchPlan = new ArrayList<DecisionDiagram.DispatchCriterion>();
        if (option.getOption(Option.AvailableOptions.CMP_USE_TAGPAIR, true))
            dispatchPlan.add(new DecisionDiagram.TagPairDispatch());
        for (int i = 0; i < 5; i++)
            dispatchPlan.add(new DecisionDiagram.PTDispatch(i));
        for (int i = 0; i < 5; i++)
            dispatchPlan.add(new DecisionDiagram.HTDispatch(i));

        LLRuleSet llrs = new LLRuleSet(hlrs);
        DecisionDiagram dd = new DecisionDiagram(dispatchPlan, llrs, option);

        if (option.getOption(Option.AvailableOptions.CMP_VERIFY_DIAGRAM, true)) {
            for (LLRule llr: llrs.getRules()) {
                VMRepType[] rts = llr.getVMRepTypes();
                LLRule found = dd.search(rts);
                if (llr.getHLRule() != found.getHLRule())
                    throw new Error("wrong decision diagram");
            }
        }		
        return dd.generateCode(hlrs.getDispatchVars(), new TagMacro());
    }
}
