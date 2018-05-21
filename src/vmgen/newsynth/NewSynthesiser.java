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
import vmgen.InsnGen.Option;
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
        LLRuleSet llrs = new LLRuleSet(hlrs);
        DecisionDiagram dd = new DecisionDiagram(llrs, option);
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
