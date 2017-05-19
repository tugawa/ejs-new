import java.io.FileNotFoundException;
import java.util.Set;

public class SimpleSynthesiser extends Synthesiser {
	
	String ptOfOperand(int i) {
		return "GET_PT(v" + (i + 1) + ")";
	}
	
	String htOfOperand(int i) {
		return "GET_HT(v" + (i + 1) + ")";
	}
	
	@Override
	String synthesise(Plan plan) {
		Set<Plan.Rule> rules = plan.getRules();
		StringBuilder code = new StringBuilder();
		
		for (Plan.Rule r: rules) {
			String cstr = r.condition.stream()
			.map(c -> SynthesiseCondition(c, plan.arity, 0, ""))
			.reduce((acc, term) -> {
				if (acc == null)
					return term;
				else
					return acc + " || " + term;
			}).get();
			
			code.append("if (" + cstr + ") {\n");
			code.append(r.action + "\n");
			code.append("} else \n");
		}
		code.append("{}");
		
		return code.toString();
	}	
	
	String SynthesiseCondition(Plan.Condition c, int n, int i, String part) {
		if (i == n)
			return "(" + part + ")";
		
		return c.dts[i].getRepresentations().stream().map(tr -> {
			String s = part;
			if (i > 0)
				s += " && ";
			s += ptOfOperand(i) + " == " + tr.getPT().name;
			if (tr.hasHT())
				s += " && " + htOfOperand(i) + " == " + tr.getHT().name;
			return SynthesiseCondition(c, n, i + 1, s);
		})
		.reduce((acc, term) -> {
			if (acc == null)
				return term;
			else
				return acc + " || " + term;
		}).get();
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition td = new TypeDefinition();
		td.load("datatype/embstr.dtdef");
		Plan plan = new Plan();
		System.out.println(new SimpleSynthesiser().synthesise(plan));
	}
}
