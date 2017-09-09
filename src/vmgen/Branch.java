package vmgen;
import java.util.HashSet;
import java.util.Set;

abstract class Branch {
	DDNode action;

	Branch(DDNode action) {
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

	TagPairBranch(DDNode action) {
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
			StringBuilder sb = new StringBuilder();
			for (Pair<PT, PT> tp : condition)
				sb.append("case TAG_PAIR(")
				  .append(tp.first().name).append(", ")
				  .append(tp.second().name).append("):\n");
			sb.append(action.code());
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("case (");
		if (condition.size() > 0) {
			for (Pair<PT, PT> tp : condition)
				sb.append(tp.first()).append("*").append(tp.second()).append(", ");
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(") -> ").append(action);
		return sb.toString();
	}
}

class PTBranch extends Branch {
	Set<PT> condition;

	PTBranch(DDNode action) {
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
			return "default: \n" + action.code();
		} else {
			StringBuilder sb = new StringBuilder();
			for (PT pt : condition)
				sb.append("case ").append(pt.name).append(":\n");
			sb.append(action.code());
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("case (");
		if (condition.size() > 0) {
			for (PT pt : condition)
				sb.append(pt).append(", ");
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(") -> ").append(action);
		return sb.toString();
	}
}

class HTBranch extends Branch {
	Set<HT> condition;

	HTBranch(DDNode action) {
		super(action);
		condition = new HashSet<HT>();
	}

	void addCondition(HT ht) {
		if(ht == null) throw new Error();
		condition.add(ht);
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
			StringBuilder sb = new StringBuilder();
			for (HT ht : condition)
				sb.append("case ").append(ht.name).append(":\n");
			sb.append(action.code());
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("case (");
		if (condition.size() > 0) {
			for (HT hp : condition)
				sb.append(hp).append(", ");
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(") -> ").append(action);
		return sb.toString();
	}
}
