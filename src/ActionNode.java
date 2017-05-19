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

class TerminalActionNode extends ActionNode {
	Plan.Rule rule;
	
	TerminalActionNode(Plan.Rule rule) {
		this.rule = rule;
	}
}

class ForwardActionNode extends ActionNode {
	ActionNode destination;
	
	ForwardActionNode() {}
	
	void set(ActionNode destination) {
		this.destination = destination;
	}
}