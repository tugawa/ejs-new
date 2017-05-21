import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


class ActionNode {
	public boolean mergable(ActionNode that) {
		return false;
	}
}

class DispatchActionNode extends ActionNode {
	Set<Branch> branches;
	String kind;

	DispatchActionNode(String kind) {
		branches = new HashSet<Branch>();
		this.kind = kind;
	}

	void add(Branch b) {
		branches.add(b);
	}

	public boolean mergable(ActionNode that) {
		throw new Error("not implemented");
	}

	@Override
	public String toString() {
		return kind + "{" + branches.stream().map(b -> b.toString()).collect(Collectors.joining("\n")) + "}";
	}
}

class UnexpandedActionNode extends ActionNode {
	LLPlan ruleList;
	ActionNode expanded;

	UnexpandedActionNode(LLPlan drs) {
		this.ruleList = drs;
	}

	void setExpanded(ActionNode expanded) {
		this.expanded = expanded;
	}

	@Override
	public String toString() {
		if (expanded == null)
			return "@" + ruleList;
		else
			return ">!" + expanded;
	}

	public boolean mergable(ActionNode that_) {
		if (!(that_ instanceof UnexpandedActionNode)) return false;
		UnexpandedActionNode that = (UnexpandedActionNode) that_;
		if (expanded != null || that.expanded != null)
			return false;
		for (LLRule innerRule0: ruleList.rules) {
			NEXT_C0: for (LLRule.Condition c0: innerRule0.condition) {
				for (LLRule innerRule1: that.ruleList.rules) {
					NEXT_C1: for (LLRule.Condition c1: innerRule1.condition) {
						if (c0.done && c1.done)
							continue NEXT_C0;
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

	public boolean mergable(ActionNode that) {
		return this == that;
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

	public boolean mergable(ActionNode that_) {
		if (!(that_ instanceof RedirectActionNode)) return false;
		RedirectActionNode that = (RedirectActionNode) that_;
		return destination.mergable(that.destination);
	}
}