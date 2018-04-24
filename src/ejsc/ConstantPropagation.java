package ejsc;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConstantPropagation {
	static interface ConstantValue {
		abstract public BCode createInstruction(BCode bc);
	}

	static class FixnumValue implements ConstantValue {
		private int n;
		FixnumValue(int n) {
			this.n = n;
		}
		int getValue() {
			return n;
		}
		@Override
		public BCode createInstruction(BCode bc) {
			BCode newBC = new IFixnum(bc.getDestRegister(), n);
			newBC.addLabels(bc.getLabels());
			return newBC;
		}
	}
	
	static class ConstantEvaluator {
		ReachingDefinition rdefa;
		
		ConstantEvaluator(ReachingDefinition rdefa) {
			this.rdefa = rdefa;
		}
		
		ConstantValue eval(BCode bcx) {
			if (bcx instanceof IMove) {
				IMove bc = (IMove) bcx;
				Register src = bc.src;
				return findAndEvalDefinition(bc, src);
			} else if (bcx instanceof IAdd) {
				IAdd bc = (IAdd) bcx;
				Register src1 = bc.src1;
				Register src2 = bc.src2;
				ConstantValue v1 = findAndEvalDefinition(bc, src1);
				if (v1 == null)
					return null;
				ConstantValue v2 = findAndEvalDefinition(bc, src2);
				if (v2 == null)
					return null;
				if (v1 instanceof FixnumValue && v2 instanceof FixnumValue) {
					long n1 = ((FixnumValue) v1).getValue();
					long n2 = ((FixnumValue) v2).getValue();
					if (n1 + n2 < Integer.MIN_VALUE || n1 + n2 > Integer.MAX_VALUE)
						return null;
					return new FixnumValue((int) (n1 + n2));
				} else
					return null;
			} else if (bcx instanceof IFixnum) {
				IFixnum bc = (IFixnum) bcx;
				return new FixnumValue(bc.n);
			} else {
				return null;
			}
		}
		
		private BCode findDefinition(BCode bc, Register src) {
			BCode result = null;
			for (BCode def: rdefa.getReachingDefinitions(bc)) {
				if (def.getDestRegister() == src) {
					if (result == null)
						result = def;
					else
						return null;
				}
			}
			return result;
		}
		
		private ConstantValue findAndEvalDefinition(BCode bc, Register src) {
			BCode def = findDefinition(bc, src);
			if (def == null)
				return null;
			return eval(def);
		}
	}
	
	List<BCode> bcodes;
	ReachingDefinition rdefa;
	
	ConstantPropagation(List<BCode> bcodes) {
		this.bcodes = bcodes;
		rdefa = new ReachingDefinition(bcodes);
	}
	
	private ConstantValue computeConstant(BCode bc) {
		ConstantEvaluator evaluator = new ConstantEvaluator(rdefa);
		return evaluator.eval(bc);
	}
	
	public List<BCode> exec() {
		List<BCode> newBCodes = new ArrayList<BCode>(bcodes.size());

		for (BCode bc: bcodes) {
			ConstantValue v = computeConstant(bc);
			if (v == null)
				newBCodes.add(bc);
			else
				newBCodes.add(v.createInstruction(bc));
		}
		
		return newBCodes;
	}
}
