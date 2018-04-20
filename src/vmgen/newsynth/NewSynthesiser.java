package vmgen.newsynth;

import vmgen.RuleSet;
import vmgen.Synthesiser;
import vmgen.newsynth.DecisionDiagram.Node;
import vmgen.newsynth.LLRuleSet.LLRule;
import vmgen.type.VMRepType;

public class NewSynthesiser extends Synthesiser {
	static final boolean DEBUG_VERIFY_DIAGRAM = true;
	@Override
	public String synthesise(RuleSet hlrs) {
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
		return dd.generateCode(hlrs.getDispatchVars());
	}
}
