package vmgen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vmgen.dd.DDLeaf;
import vmgen.dd.DDNode;
import vmgen.type.VMDataType;
import vmgen.type.VMRepType;

public class LLRule {
	public static class Condition {
		public VMRepType[] trs;

		public int arity;
		public boolean done;

		Condition(VMRepType... rts) {
			this.trs = rts;
			arity = rts.length;
			done = false;
		}
		
		/**
		 * Create a tuple of VMRepType from the given tuple of VMDataType.
		 * A VMDataType may have multiple VMRepType.  When VMDataType_i
		 * has n_i VMRepTypes, a tuple of VMDataType (VMDataType_0, ..., VM_DataType_{m-1})
		 * has n_0 * ... * n_{m-1} tuples of VMRepTypes.
		 * @param dts tuple of VMDataType (high-level condition)
		 * @param index This constructor creates index-th tuple of VMRepType
		 */
		Condition(VMDataType[] dts, int index) {
			arity = dts.length;
			trs = new VMRepType[arity];
			for (int i = 0; i < arity; i++) {
				List<VMRepType> vmRepTypes = dts[i].getVMRepTypes();
				int base = vmRepTypes.size();
				trs[i] = vmRepTypes.get(index % base);
				index /= base;
			}
			done = false;
		}
	}

	public Set<Condition> condition;
	public DDNode action;

	LLRule(Set<Condition> condition, DDNode action) {
		this.condition = condition;
		this.action = action;
	}

	LLRule(Condition condition, DDNode action) {
		this.condition = new HashSet<Condition>();
		this.condition.add(condition);
		this.action = action;
	}

	/**
	 * Creates LLRule from (high level) Rule
	 * @param r (high level) Rule
	 */
	LLRule(Plan.Rule r) {
		condition = new HashSet<Condition>();
		for (Plan.Condition dtc: r.condition) {
			int nRtc = 1;
			for (VMDataType dt: dtc.dts)
				nRtc *= dt.getVMRepTypes().size();
			for (int i = 0; i < nRtc; i++)
				condition.add(new Condition(dtc.dts, i));
		}
		action = new DDLeaf(r);
	}


	public Condition find(VMRepType... key) {
		NEXT_CONDITION: for (Condition c: condition) {
			if (c.arity != key.length)
				continue;
			for (int i = 0; i < c.arity; i++) {
				if (! c.trs[i].equals(key[i]))
				 continue NEXT_CONDITION;
			}
			return c;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[{");
		if (condition.size() > 0) {
			for (Condition c: condition) {
				if (c.done)
					sb.append("(done)");
				for (VMRepType rt: c.trs)
					sb.append(rt.getName()).append("*");
				sb.delete(sb.length() - 1, sb.length());
				sb.append(", ");
			}
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append("} -> ").append(action).append("]");
		return sb.toString();
	}
}