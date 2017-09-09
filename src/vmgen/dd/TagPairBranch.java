package vmgen.dd;

import java.util.HashSet;
import java.util.Set;

import vmgen.Pair;
import vmgen.type.VMRepType;

public class TagPairBranch extends Branch {
	Set<Pair<VMRepType.PT, VMRepType.PT>> condition;

	public TagPairBranch(DDNode action) {
		super(action);
		condition = new HashSet<Pair<VMRepType.PT, VMRepType.PT>>();
	}
	public void addCondition(VMRepType.PT pt0, VMRepType.PT pt1) {
		condition.add(new Pair<VMRepType.PT, VMRepType.PT>(pt0, pt1));
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
			for (Pair<VMRepType.PT, VMRepType.PT> tp : condition)
				sb.append("case TAG_PAIR(")
				  .append(tp.first().getName()).append(", ")
				  .append(tp.second().getName()).append("):\n");
			sb.append(action.code());
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("case (");
		if (condition.size() > 0) {
			for (Pair<VMRepType.PT, VMRepType.PT> tp : condition)
				sb.append(tp.first()).append("*").append(tp.second()).append(", ");
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("): ").append(action);
		return sb.toString();
	}
}