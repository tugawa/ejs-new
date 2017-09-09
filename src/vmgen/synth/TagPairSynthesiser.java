package vmgen.synth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import vmgen.LLPlan;
import vmgen.LLRule;
import vmgen.Plan;
import vmgen.dd.DDDispatchNode;
import vmgen.dd.DDNode;
import vmgen.dd.TagPairBranch;
import vmgen.type.VMRepType;

public class TagPairSynthesiser extends SwitchSynthesiser {
	@Override
	public
	String synthesise(Plan plan) {
		LLPlan dispatchRuleList = new LLPlan(plan);
		if (PRINT_PASS) {
			System.out.println("-------- LLPlan --------");
			System.out.println(dispatchRuleList);
		}
		DDNode root = tagPairDispatch(dispatchRuleList);
		Stream<VMRepType.PT[]> undispatched = dispatchRuleList.rules.stream()
			.flatMap(r -> r.condition.stream())
			.filter(c -> !c.done)
			.map(c -> new VMRepType.PT[] {c.trs[0].getPT(), c.trs[1].getPT()})
			.distinct();
		LLPlan nestedRuleList = dispatchRuleList.convertToNestedPlan(true);
		if (PRINT_PASS) {
			System.out.println("-------- nested LLPlan (after tag-pair filter) --------");
			System.out.println(nestedRuleList);
		}
		nestedRuleList.canonicalise();
		if (PRINT_PASS) {
			System.out.println("-------- canonicalised nested LLPlan --------");
			System.out.println(nestedRuleList);
		}
		DDDispatchNode d = nestedDispatch(nestedRuleList);
//		System.out.println(d);
		TagPairBranch b = new TagPairBranch(d);
		undispatched.forEach(pt -> b.addCondition(pt[0], pt[1]));
		((DDDispatchNode) root).add(b);
		if (PRINT_PASS) {
			System.out.println("-------- unoptimised decision tree --------");
			System.out.println(root);
		}
//		System.out.println("----------------");
		root = simplify(root);
//		System.out.println(root);
//		System.out.println("----------------");
		arrangeTerminalNode(root);
//		System.out.println(root);

//		System.out.println(root.code());
		// TODO Auto-generated method stub
		//tagPairDispatch(dispatchRuleList);
		if (PRINT_PASS) {
			System.out.println("-------- optimised decision tree --------");
			System.out.println(root);
		}
		return root.code();
	}

	DDDispatchNode tagPairDispatch(LLPlan llplan) {
		DDDispatchNode disp = new DDDispatchNode(getTagPairCode(llplan.getDispatchVars()));
		Map<LLRule, TagPairBranch> revDisp = new HashMap<LLRule, TagPairBranch>();

		for (LLRule r: llplan.rules) {
			TagPairBranch b = new TagPairBranch(r.action);
			disp.add(b);
			revDisp.put(r, b);
		}

		for(VMRepType.PT pt0: llplan.allPTNthOperand(0))
			for (VMRepType.PT pt1: llplan.allPTNthOperand(1)) {
				Set<LLRule> match = llplan.findByPT(pt0, pt1);
				if (match.size() == 1) {
					LLRule r = match.iterator().next();
					TagPairBranch b = revDisp.get(r);
					b.addCondition(pt0, pt1);
					r.condition.forEach(c -> {
						if (c.trs[0].getPT() == pt0 && c.trs[1].getPT() == pt1)
							c.done = true;
					});
				}
			}

		return disp;
	}

	/*
	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition td = new TypeDefinition();
		if (DEBUG_WITH_SMALL)
			td.load("datatype/small.dtdef");
		else
			td.load("datatype/ssjs.dtdef");
		System.out.println(td);
        ProcDefinition procDef = new ProcDefinition();
        if (DEBUG_WITH_SMALL)
        	procDef.load("datatype/sample.idef");
        else
        	procDef.load("datatype/add.idef");
        System.out.println(procDef);
        ProcDefinition.InstDefinition instDef = (ProcDefinition.InstDefinition) procDef.instDefs.get(0);
        String[] dispatchVars = new String[instDef.dispatchVars.length];
        for (int i = 0; i < instDef.dispatchVars.length; i++) {
        	String s = instDef.dispatchVars[i];
        	dispatchVars[i] = s.substring(1);
        }
        Plan p = new Plan(dispatchVars, instDef.tdDef.rules);
        Synthesiser sy = new TagPairSynthesiser();
        System.out.println(sy.synthesise(p));
	}
	*/
}
