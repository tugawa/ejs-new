package vmgen.dd;

import java.util.HashSet;
import java.util.Set;

import vmgen.type.VMRepType;

public class HTBranch extends Branch {
	Set<VMRepType.HT> condition;

	public HTBranch(DDNode action) {
		super(action);
		condition = new HashSet<VMRepType.HT>();
	}

	public void addCondition(VMRepType.HT ht) {
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
			for (VMRepType.HT ht : condition)
				sb.append("case ").append(ht.getName()).append(":\n");
			sb.append(action.code());
			return sb.toString();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("case (");
		if (condition.size() > 0) {
			for (VMRepType.HT hp : condition)
				sb.append(hp).append(", ");
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("): ").append(action);
		return sb.toString();
	}
}