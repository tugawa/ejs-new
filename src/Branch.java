import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class Branch {
	ActionNode action;

	Branch(ActionNode action) {
		this.action = action;
	}
}

class TagPairBranch extends Branch {
	Set<Pair<PT, PT>> condition;

	TagPairBranch(ActionNode action) {
		super(action);
		condition = new HashSet<Pair<PT, PT>>();
	}
	void addCondition(PT pt0, PT pt1) {
		condition.add(new Pair<PT, PT>(pt0, pt1));
	}

	@Override
	public String toString() {
		String s = "L_" + action.toString() + ":\n";
		for (Pair<PT,PT> p: condition)
			s += "case ("+p.first().name+","+p.second().name+")\n";
		s += "    " + action.toString() + "; break;\n";
		return s;
	}
}

class PTBranch extends Branch {
	Set<PT> condition;

	PTBranch(ActionNode action) {
		super(action);
		condition = new HashSet<PT>();
	}

	void addCondition(PT pt) {
		condition.add(pt);
	}

	@Override
	public String toString() {
		String c = condition.stream().map(pt -> pt.name).collect(Collectors.joining(","));
		return "case ("+c+") -> "+action;
	}
}

class HTBranch extends Branch {
	Set<HT> condition;

	HTBranch(ActionNode action) {
		super(action);
		condition = new HashSet<HT>();
	}

	void addCondition(HT ht) {
		condition.add(ht);
	}

	@Override
	public String toString() {
		String c = condition.stream().map(ht -> ht.name).collect(Collectors.joining(","));
		return "case ("+c+") -> "+action;
	}
}
