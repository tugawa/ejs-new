package vmgen.dd;

import vmgen.LLPlan;
import vmgen.LLRule;

public class DDUnexpandedNode extends DDNode {
	public LLPlan ruleList;

	public DDUnexpandedNode(LLPlan drs) {
		this.ruleList = drs;
	}

	public void canonicaliseRuleList() {
		ruleList.canonicalise();
	}

	@Override
	public String code() {
		throw new Error("UnexpandedActionNode");
	}

	@Override
	public String toString() {
		return "@" + ruleList;
	}

	/*
	public boolean mergable(DDNode that_) {
		if (!(that_ instanceof DDUnexpandedNode)) return false;
		DDUnexpandedNode that = (DDUnexpandedNode) that_;
		for (LLRule innerRule0: ruleList.rules) {
			NEXT_C0: for (LLRule.Condition c0: innerRule0.condition) {
				for (LLRule innerRule1: that.ruleList.rules) {
					NEXT_C1: for (LLRule.Condition c1: innerRule1.condition) {
						if (c0.done && c1.done)
							continue NEXT_C0;
						if (c0.done != c1.done)
							return false;
						for (int i = 0; i < c0.arity; i++) {
							if (!c0.trs[i].equals(c1.trs[i]))
								continue NEXT_C1;
						}
						if (innerRule0.action.mergable(innerRule1.action))
							continue NEXT_C0;
						return false;
					}
				}
				return false;
			}
		}
		return true;
	}
	*/
}