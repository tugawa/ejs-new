import java.util.HashSet;
import java.util.Set;

class Branch {
}

class TagPairBranch extends Branch {
	Plan.Rule rule;
	Set<Pair<PT, PT>> condition;
	
	TagPairBranch(Plan.Rule r) {
		this.rule = r;
		condition = new HashSet<Pair<PT, PT>>();
	}
	void addCondition(PT pt0, PT pt1) {
		condition.add(new Pair<PT, PT>(pt0, pt1));
	}
	
	@Override
	public String toString() {
		String s = "L_" + rule.toString() + ":\n";
		for (Pair<PT,PT> p: condition)
			s += "case ("+p.first().name+","+p.second().name+")\n";
		s += "    " + rule.action + "; break;\n";
		return s;
	}
}

class PTBranch extends Branch {
	Set<PT> condition;
	
	PTBranch() {
		condition = new HashSet<PT>();
	}
	
	void addCondition(PT pt) {
		condition.add(pt);
	}
}