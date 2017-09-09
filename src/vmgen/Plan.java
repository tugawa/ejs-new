package vmgen;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Plan implements GlobalConstantOptions {
	static class Condition {
		VMDataType[] dts;

		Condition(String tn1) {
		    dts = new VMDataType[]{VMDataType.get(tn1)};
		}

		Condition(String tn1, String tn2) {
			dts = new VMDataType[]{VMDataType.get(tn1), VMDataType.get(tn2)};
		}
	};

	static class Rule {
		Set<Condition> condition;
		String action;

		/*
		Set<PT> uniquePT(TypeDefinition td, int i) {
			Set<PT> unique = new HashSet<PT>();
			for (VMRepType rt: getTypeRepresentations(td, i))
				if (rt.hasUniquePT(VMRepType.all()))
					unique.add(rt.getPT());
			return unique;
		}
		*/

		Rule(String action, Condition...  condition) {
			this.action = action;
			this.condition = new HashSet<Condition>();
			for (Condition c: condition)
				this.condition.add(c);
		}

		Rule(String action, List<Condition> condition) {
		    this.action = action;
		    this.condition = new HashSet<Condition>();
		    for (Condition c: condition)
		        this.condition.add(c);
		}

		Rule(String action, Set<Condition> condition) {
		    this.action = action;
		    this.condition = condition;
		}
	}

/*<<<<<<< HEAD
	int arity;
	String[] vars;
=======*/
	String[] dispatchVars;
// >>>>>>> develop
	Set<Rule> rules;

	Set<Rule> getRules() {
		return rules;
	}
	int getArity() {
		return dispatchVars.length;
	}

	Plan() {
		rules = new HashSet<Rule>();
		dispatchVars = new String[]{"v1", "v2"};

		/* generate dummy data that looks like add */
		rules.add(new Rule("fixfix", new Condition("fixnum", "fixnum")));
		rules.add(new Rule("fixflo", new Condition("fixnum", "flonum")));
		rules.add(new Rule("flofix", new Condition("flonum", "fixnum")));
		rules.add(new Rule("floflo", new Condition("flonum", "flonum")));
		rules.add(new Rule("strstr", new Condition("string", "string")));
		rules.add(new Rule("strflo", new Condition("string", "flonum")));
		rules.add(new Rule("flostr", new Condition("flonum", "string")));
		rules.add(new Rule("strspe", new Condition("string", "special")));
		rules.add(new Rule("spestr", new Condition("special", "string")));
		rules.add(new Rule("strfix", new Condition("string", "fixnum")));
		rules.add(new Rule("fixstr", new Condition("fixnum", "string")));
		rules.add(new Rule("to_primitive",
							new Condition("simple_object", "simple_object"),
							new Condition("simple_object", "array"),
							new Condition("array", "simple_object"),
							new Condition("array", "array")));
	}

/*<<<<<<< HEAD
	Plan(String[] vars, Set<Rule> rules) {
	    this.arity = vars.length;
	    this.vars = vars;
=======*/
	Plan(String[] dispatchVars, Set<Rule> rules) {
	    this.dispatchVars = dispatchVars;
// >>>>>>> develop
	    this.rules = rules;
	}
}
