import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

abstract class Branch {
	ActionNode action;

	Branch(ActionNode action) {
		this.action = action;
	}
	abstract public int size();
	public String code() {
		return code(false);
	}
	abstract public String code(boolean isDefaultCase);
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
	public int size() {
		return condition.size();
	}

	@Override
	public String code(boolean isDefaultCase) {
		if (isDefaultCase) {
			return "default: \n" + action.code();
		} else {
			return condition.stream()
					.map(p -> "case TAG_PAIR("+p.first().name+", "+p.second().name+"):")
					.collect(Collectors.joining(" ")) + "\n" +
					action.code();
		}
	}

	@Override
	public String toString() {
		String c = condition.stream().map(pair -> pair.first() + "*" + pair.second()).collect(Collectors.joining(","));
		return "case ("+c+") -> "+action;
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
	public int size() {
		return condition.size();
	}

	@Override
	public String code(boolean isDefaultCase) {
		if (isDefaultCase) {
			return "default: \n"+ action.code();
		} else {
			return condition.stream()
					.map(pt -> "case "+pt.name+":")
					.collect(Collectors.joining(" ")) + "\n" +
					action.code();
		}
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
		if(ht == null) throw new Error();
		condition.add(ht);
	}

	@Override
	public String code(boolean isDefaultCase) {
		if (isDefaultCase) {
			return "default: \n"+ action.code();
		} else {
			return condition.stream()
					.map(ht -> "case "+ht.name+":")
					.collect(Collectors.joining(" ")) + "\n" +
					action.code();
		}
	}

	@Override
	public int size() {
		return condition.size();
	}

	@Override
	public String toString() {
		String c = condition.stream().map(ht -> ht.name).collect(Collectors.joining(","));
		return "case ("+c+") -> "+action;
	}
}
