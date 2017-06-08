import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


abstract class ActionNode implements GlobalConstantOptions {
	static int nextLabel = 0;
	static String prefix = "";
	String label;
	boolean arranged;
	
	ActionNode() {
		arranged = false;
		label = String.format("L%s%d", prefix, nextLabel++);
	}

	public boolean mergable(ActionNode that) {
		return false;
	}
	
	abstract public String code();
}

class DispatchActionNode extends ActionNode {
	Set<Branch> branches;
	String dispatchExpression;

	DispatchActionNode(String dispatchExpression) {
		branches = new HashSet<Branch>();
		this.dispatchExpression = dispatchExpression;
	}

	void add(Branch b) {
		branches.add(b);
	}

	public boolean mergable(ActionNode that) {
		throw new Error("not implemented");
	}

	@Override
	public String code() {
		Branch max = null;
		for (Branch b: branches) {
			if (max == null || b.size() > max.size())
				max = b;
		}
		Branch largetBranch = max;
		StringBuffer sb = new StringBuffer();
		sb.append(label + ": ");
		sb.append("switch("+dispatchExpression+") {\n");
		branches.forEach(b -> sb.append(b.code(USE_DEFAULT_CASE && (b == largetBranch))).append("break;\n"));
		sb.append("}\n");
		return sb.toString();
	}
	@Override
	public String toString() {
		return dispatchExpression + "{" + branches.stream().map(b -> b.toString()).collect(Collectors.joining("\n")) + "}";
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
	public String code() {
		throw new Error("UnexpandedActionNode");
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
		return "(" + rule.action.split("\n")[0] + ")";
	}

	@Override
	public String code() {
		return label + ": {"+ rule.action + "\n}\n";
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

	@Override
	public String code() {
		return "goto "+destination.label+";\n";
	}

	public boolean mergable(ActionNode that_) {
		if (!(that_ instanceof RedirectActionNode)) return false;
		RedirectActionNode that = (RedirectActionNode) that_;
		return destination.mergable(that.destination);
	}
}