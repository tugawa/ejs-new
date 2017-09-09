package vmgen.dd;

import vmgen.Plan;
import vmgen.Plan.Rule;

public class DDLeaf extends DDNode {
	Plan.Rule rule;

	public DDLeaf(Plan.Rule rule) {
		this.rule = rule;
	}

	@Override
	public String toString() {
		return "{" + rule.action.split("\n")[0] + "}";
	}

	@Override
	public String code() {
		return label + ": {"+ rule.action + "\n}\n";
	}

	public boolean mergable(DDNode that) {
		return this == that;
	}
}