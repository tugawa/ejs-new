import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class Plan {
	static class Condition {
		DataType[] dts;

		Condition(String tn1, String tn2) {
			dts = new DataType[]{DataType.get(tn1), DataType.get(tn2)};
		}
	};

	static class Rule {
		Set<Condition> condition;
		String action;

		Set<PT> uniquePT(TypeDefinition td, int i) {
			return DataType.uniquePT(getTypeRepresentations(td, i));
		}

		Set<TypeRepresentation> getTypeRepresentations(TypeDefinition td, int i) {
			return DataType.typeRepresentationOf(condition.stream().map(c -> c.dts[i]).distinct());
		}

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
	}

	Set<Rule> rules;

	void twoOperand(TypeDefinition td, Set<Rule> rules) {
		Set<Branch> root = new HashSet<Branch>();
		for (Rule r: rules) {
			TagPairBranch b = new TagPairBranch(r);
			root.add(b);
			for(PT pt0: r.uniquePT(td, 0))
				for (PT pt1: r.uniquePT(td, 1))
					b.addCondition(pt0, pt1);
		}

		root.stream().forEach(b -> System.out.println(b));

		Map<Map<TypeRepresentation, Rule>, Set<TypeRepresentation>> revDispatchTable = new HashMap<Map<TypeRepresentation, Rule>, Set<TypeRepresentation>>();
		for (Rule r0: rules)
			for (TypeRepresentation tr0: r0.getTypeRepresentations(td, 0)) {
				HashMap<TypeRepresentation, Rule> innerDispatch = new HashMap<TypeRepresentation, Rule>();
				for (Rule r: rules) {
					for (TypeRepresentation tr1: r.getTypeRepresentations(td, 1))
						innerDispatch.put(tr1, r);
				}
				Set<TypeRepresentation> trs = revDispatchTable.get(innerDispatch);
				if (trs == null) {
					trs = new HashSet<TypeRepresentation>();
					revDispatchTable.put(innerDispatch, trs);
				}
				trs.add(tr0);
			}

		Set<PTBranch> secondLevel = new HashSet<PTBranch>();
		revDispatchTable.values().forEach(trs -> {
			PTBranch b = new PTBranch();
			secondLevel.add(b);
			DataType.uniquePT(trs).stream().forEach(pt -> b.addCondition(pt));
		});

		secondLevel.forEach(b -> System.out.println(b));
	}

	Plan() {
		rules = new HashSet<Rule>();

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

	Plan(Set<Rule> rules) {
	    this.rules = rules;
	}
}
