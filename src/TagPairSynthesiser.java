import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


class TagPairSynthesiser extends Synthesiser {
	@Override
	String synthesise(Plan plan) {
		// TODO Auto-generated method stub
		return null;
	}
	
	void twoOperand(TypeDefinition td, Set<Plan.Rule> rules) {
		Set<Branch> root = new HashSet<Branch>();
		for (Plan.Rule r: rules) {
			TagPairBranch b = new TagPairBranch(r);
			root.add(b);
			for(PT pt0: r.uniquePT(td, 0))
				for (PT pt1: r.uniquePT(td, 1))
					b.addCondition(pt0, pt1);
		}
		
		root.stream().forEach(b -> System.out.println(b));

		Map<Map<TypeRepresentation, Plan.Rule>, Set<TypeRepresentation>> revDispatchTable = new HashMap<Map<TypeRepresentation, Plan.Rule>, Set<TypeRepresentation>>();
		for (Plan.Rule r0: rules)
			for (TypeRepresentation tr0: r0.getTypeRepresentations(td, 0)) {
				HashMap<TypeRepresentation, Plan.Rule> innerDispatch = new HashMap<TypeRepresentation, Plan.Rule>();
				for (Plan.Rule r: rules) {
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

}
