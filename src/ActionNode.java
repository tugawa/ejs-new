import java.util.HashSet;
import java.util.Set;


class ActionNode {
}

class DispatchActionNode extends ActionNode {
	Set<Branch> branches;

	DispatchActionNode() {
		branches = new HashSet<Branch>();
	}

	void add(Branch b) {
		branches.add(b);
	}
}

class UnexpandedActionNode extends ActionNode {
	LLPlan ruleList;
	UnexpandedActionNode(LLPlan drs) {
		this.ruleList = drs;
	}

	@Override
	public String toString() {
		return "@" + ruleList;
	}
}

class TerminalActionNode extends ActionNode {
	Plan.Rule rule;

	TerminalActionNode(Plan.Rule rule) {
		this.rule = rule;
	}

	@Override
	public String toString() {
		return "(" + rule.action + ")";
	}
}

class RedirectActionNode extends ActionNode {
	ActionNode destination;

	RedirectActionNode(ActionNode destination) {
		this.destination = destination;
	}

	@Override
	public String toString() {
		return ">" + destination;
	}
}