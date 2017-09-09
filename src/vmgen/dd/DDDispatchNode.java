package vmgen.dd;

import java.util.HashSet;
import java.util.Set;

public class DDDispatchNode extends DDNode {
	public Set<Branch> branches;
	String dispatchExpression;

	public DDDispatchNode(String dispatchExpression) {
		branches = new HashSet<Branch>();
		this.dispatchExpression = dispatchExpression;
	}

	public void add(Branch b) {
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
		StringBuilder sb = new StringBuilder('\n');
		sb.append(dispatchExpression)
		  .append("{\n");
		for (Branch b: branches)
			sb.append(b.toString()).append('\n');
		sb.append("}\n");
		return sb.toString();
	}
}