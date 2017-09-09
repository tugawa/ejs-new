package vmgen;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleSynthesiser extends Synthesiser {
	@Override
	String synthesise(Plan plan) {
	    // this.plan = plan;
		Set<Plan.Rule> rules = plan.getRules();
		StringBuilder code = new StringBuilder();

		for (Plan.Rule r: rules) {
			code.append("if (");
			code.append(r.condition.stream()
							.map(c -> {
								String s = "(";
								for (int i = 0; i < plan.dispatchVars.length; i++) {
									if (i > 0) s += " && ";
									s += "is_"+c.dts[i].name+"("+plan.dispatchVars[i]+")";
								}
								return s + ")";
							}).collect(Collectors.joining(" || ")));
			code.append(") {\n");
			code.append(r.action + "\n");
			code.append("} else \n");
		}
		code.append("{}");

		return code.toString();
	}

	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition td = new TypeDefinition();
		td.load("datatype/embstr.dtdef");
		Plan plan = new Plan();
		System.out.println(new SimpleSynthesiser().synthesise(plan));
	}
}
