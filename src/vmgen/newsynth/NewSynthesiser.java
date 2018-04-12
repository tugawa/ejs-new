package vmgen.newsynth;

import vmgen.RuleSet;
import vmgen.Synthesiser;

public class NewSynthesiser extends Synthesiser {
	@Override
	public String synthesise(RuleSet hlrs) {
		LLRuleSet llrs = new LLRuleSet(hlrs);
		DecisionDiagram dd = new DecisionDiagram(llrs);
		return dd.generateCode(hlrs.getDispatchVars());
	}
}
