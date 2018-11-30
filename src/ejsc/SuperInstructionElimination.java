package ejsc;

import java.util.ArrayList;
import java.util.List;

public class SuperInstructionElimination {
	public class SuperInstructionEmulator {
		class Environment {
			BCode bc;

			Environment(BCode bc) {
				this.bc = bc;
			}

			public BCode lookup(Register r) {
				return findDefinition(bc, r);
			}

			private BCode findDefinition(BCode bc, Register src) {
				BCode result = null;
				for (BCode def : rdefa.getReachingDefinitions(bc)) {
					if (def.getDestRegister() == src) {
						if (result == null)
							result = def;
						else
							return null;
					}
				}
				if (result instanceof IFixnum) {
					if (((IFixnum) result).n < (1 << 16))
						return result;
					else
						return null;
				}
				return null;
			}
		}

		ReachingDefinition rdefa;

		SuperInstructionEmulator(ReachingDefinition rdefa) {
			this.rdefa = rdefa;
		}

		BCode eval(BCode bc) {
			return eval(new Environment(bc), bc);
		}

		public BCode eval(Environment env, BCode bc) {
			if (bc instanceof IGetprop)
				return evalIGetprop(env, (IGetprop) bc);
			if (bc instanceof IAdd)
				return evalIAdd(env, (IAdd) bc);
			return null;
		}

		protected BCode evalIAdd(Environment env, IAdd bc) {
			BCode b = env.lookup(bc.src2);
			if (b == null)
				return null;
			//return new IAddFixnum(bc.dst, bc.src1, ((IFixnum) b).n);
			return null;
		}

		protected BCode evalIGetprop(Environment env, IGetprop bc) {
			BCode b = env.lookup(bc.prop);
			if (b == null)
				return null;
			//return new IGetpropFix(bc.dst, bc.obj, ((IFixnum) b).n);
			return null;
		}
	}

	List<BCode> bcodes;
	ReachingDefinition rdefa;

	SuperInstructionElimination(List<BCode> bcodes) {
		this.bcodes = bcodes;
		rdefa = new ReachingDefinition(bcodes);
	}

	private BCode computeSuperInst(BCode bc) {
		SuperInstructionEmulator emulator = new SuperInstructionEmulator(rdefa);
		return emulator.eval(bc);
	}

	public List<BCode> exec() {
		List<BCode> newBCodes = new ArrayList<BCode>(bcodes.size());

		for (BCode bc : bcodes) {
			BCode newBC = computeSuperInst(bc);
			if (newBC == null || bc.equals(newBC))
				newBCodes.add(bc);
			else {
				newBC.addLabels(bc.getLabels());
				newBCodes.add(newBC);
			}
		}
		return newBCodes;
	}
}
