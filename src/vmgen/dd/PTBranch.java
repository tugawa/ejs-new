package vmgen.dd;

import java.util.HashSet;
import java.util.Set;

import vmgen.type.VMRepType;

public class PTBranch extends Branch {
	Set<VMRepType.PT> condition;

	public PTBranch(DDNode action) {
		super(action);
		condition = new HashSet<VMRepType.PT>();
	}

	public void addCondition(VMRepType.PT pt) {
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
			for (VMRepType.PT pt : condition)
				sb.append("case ").append(pt.getName()).append(":\n");
			sb.append(action.code());
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("case (");
		if (condition.size() > 0) {
			for (VMRepType.PT pt : condition)
				sb.append(pt).append(", ");
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("): ").append(action);
		return sb.toString();
	}
}