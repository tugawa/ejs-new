package ejsc;
import java.util.ArrayList;
import java.util.List;

import ejsc.BCodeEvaluator.FixnumValue;
import ejsc.BCodeEvaluator.NumberValue;
import ejsc.BCodeEvaluator.StringValue;
import ejsc.BCodeEvaluator.Value;

public class ConstantPropagation {
	static class ConstantEvaluator extends BCodeEvaluator {
		class Environment extends BCodeEvaluator.Environment {			
			@Override
			public BCodeEvaluator.Value lookup(BCode bc, Register r) {
				return findAndEvalDefinition(bc, r);
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
			
			private Value findAndEvalDefinition(BCode bc, Register src) {
				BCode def = findDefinition(bc, src);
				if (def == null)
					return null;
				return eval(def);
			}
		}
		
		ReachingDefinition rdefa;
		
		ConstantEvaluator(ReachingDefinition rdefa) {
			this.rdefa = rdefa;
		}
		
		Value eval(BCode bc) {
			return eval(new Environment(), bc);
		}
	}
	
	List<BCode> bcodes;
	ReachingDefinition rdefa;
	
	ConstantPropagation(List<BCode> bcodes) {
		this.bcodes = bcodes;
		rdefa = new ReachingDefinition(bcodes);
	}
	
	private Value computeConstant(BCode bc) {
		ConstantEvaluator evaluator = new ConstantEvaluator(rdefa);
		return evaluator.eval(bc);
	}
	
	private BCode createConstantInstruction(Register r, Value v) {
		if (v instanceof FixnumValue)
			return new IFixnum(r, ((FixnumValue) v).getIntValue());
		if (v instanceof NumberValue)
			return new INumber(r, ((NumberValue) v).getDoubleValue());
		if (v instanceof StringValue)
			return new IString(r, ((StringValue) v).getStringValue());
		return null;
	}
		
	public List<BCode> exec() {
		List<BCode> newBCodes = new ArrayList<BCode>(bcodes.size());

		for (BCode bc: bcodes) {
			Value v = computeConstant(bc);
			if (v == null)
				newBCodes.add(bc);
			else {
				BCode newBC = createConstantInstruction(bc.getDestRegister(), v);
				newBC.addLabels(bc.getLabels());
				newBCodes.add(newBC);
			}
		}
		
		return newBCodes;
	}
}
