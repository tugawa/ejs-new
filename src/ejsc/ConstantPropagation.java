/*
   ConstantPropagation.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Taiki Fujimoto, 2018
     Tomoharu Ugawa, 2018
     Hideya Iwasaki, 2018

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/
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
			BCode bc;
			
			Environment(BCode bc) {
				this.bc = bc;
			}
			
			@Override
			public BCodeEvaluator.Value lookup(Register r) {
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
			return eval(new Environment(bc), bc);
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
