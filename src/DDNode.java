import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


abstract class DDNode implements GlobalConstantOptions {
	static int nextLabel = 0;
	static String prefix = "";
	String label;
	boolean arranged;
	
	DDNode() {
		arranged = false;
		label = String.format("L%s%d", prefix, nextLabel++);
	}

	public boolean mergable(DDNode that) {
		return false;
	}
	
	abstract public String code();
}

class DDDispatchNode extends DDNode {
	Set<Branch> branches;
	String dispatchExpression;

	DDDispatchNode(String dispatchExpression) {
		branches = new HashSet<Branch>();
		this.dispatchExpression = dispatchExpression;
	}

	void add(Branch b) {
		branches.add(b);
	}

	public boolean mergable(DDNode that) {
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

class DDUnexpandedNode extends DDNode {
	LLPlan ruleList;

	DDUnexpandedNode(LLPlan drs) {
		this.ruleList = drs;
	}

	@Override
	public String code() {
		throw new Error("UnexpandedActionNode");
	}

	@Override
	public String toString() {
		return "@" + ruleList;
	}

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
}

class DDLeaf extends DDNode {
	Plan.Rule rule;

	DDLeaf(Plan.Rule rule) {
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

	public boolean mergable(DDNode that) {
		return this == that;
	}
}

class DDRedirectNode extends DDNode {
	DDNode destination;

	DDRedirectNode(DDNode destination) {
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

	public boolean mergable(DDNode that_) {
		if (!(that_ instanceof DDRedirectNode)) return false;
		DDRedirectNode that = (DDRedirectNode) that_;
		return destination.mergable(that.destination);
	}
}