package vmgen.newsynth;

import vmgen.RuleSet;
import vmgen.Synthesiser;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.LLRuleSet.LLRule;
import vmgen.type.VMRepType;

public class NewSynthesiser extends Synthesiser {
	static final boolean UNSIGNED = false;
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
	
	static final boolean DEBUG_VERIFY_DIAGRAM = true;
	
	String labelPrefix;
	
	@Override
	public String synthesise(RuleSet hlrs, String labelPrefix) {
		this.labelPrefix = labelPrefix;
		LLRuleSet llrs = new LLRuleSet(hlrs);
		DecisionDiagram dd = new DecisionDiagram(llrs);
		if (DEBUG_VERIFY_DIAGRAM) {
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
