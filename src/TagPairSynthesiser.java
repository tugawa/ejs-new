import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class LLRule {
	static class Condition {
		TypeRepresentation[] trs;

		int arity;
		boolean done;

		Condition(TypeRepresentation... trs) {
			this.trs = trs;
			arity = trs.length;
			done = false;
		}
	}

	Set<Condition> condition;
	ActionNode action;

	LLRule(Set<Condition> condition, ActionNode action) {
		this.condition = condition;
		this.action = action;
	}

	LLRule(Condition condition, ActionNode action) {
		this.condition = new HashSet<Condition>();
		this.condition.add(condition);
		this.action = action;
	}

	LLRule(Plan.Rule r, int n) {
		condition = r.condition.stream()
					.flatMap(c -> dtCondToTRCond(c, n).stream())
					.collect(Collectors.toSet());
		action = new TerminalActionNode(r);
	}

	/**
	 * Subroutine of dtCondToTRCond.
	 */
	protected void dtCondToTRCondRec(DataType[] dts, Set<Condition> result, TypeRepresentation[] part, int i, int n) {
		if (i == n) {
			result.add(new Condition(part.clone()));
			return;
		}
		DataType dt = dts[i];
		for (TypeRepresentation tr: dt.getRepresentations()) {
			part[i] = tr;
			dtCondToTRCondRec(dts, result, part, i + 1, n);
		}
	}

	/**
	 * Convert a high level condition (array of DataType) to a set of
	 * low level conditions (array of TypeRepresentation).
	 * @param dts high level condition
	 * @param n arity
	 * @return set of low level conditions
	 */
	protected Set<Condition> dtCondToTRCond(Plan.Condition dtCond, int n) {
		TypeRepresentation[] trs = new TypeRepresentation[n];
		Set<Condition> result = new HashSet<Condition>();
		dtCondToTRCondRec(dtCond.dts, result, trs, 0, n);
		return result;
	}

	public Condition find(TypeRepresentation... key) {
		NEXT_CONDITION: for (Condition c: condition) {
			if (c.arity != key.length)
				continue;
			for (int i = 0; i < c.arity; i++) {
				if (! c.trs[i].equals(key[i]))
				 continue NEXT_CONDITION;
			}
			return c;
		}
		return null;
	}

	@Override
	public String toString() {
		return "[{" +
				condition.stream().map(c -> {
					return (c.done ? "-" : "") +
						Arrays.stream(c.trs)
							.map(tr -> tr.getPT().name + (tr.hasHT() ? ("/" + tr.getHT().name) : ""))
							.collect(Collectors.joining("*"));
				}).collect(Collectors.joining(", "))
				+ "} -> " + action.toString() + "]";
	}
}

class LLPlan {
	Set<LLRule> rules;

	LLPlan(Plan plan) {
		rules = plan.getRules().stream()
				.map(r -> new LLRule(r, plan.getArity()))
				.collect(Collectors.toSet());
	}

	LLPlan() {
		rules = new HashSet<LLRule>();
	}

	/**
	 * Enumerates all PTs appearing in the n-th operands of all rules.
	 * @param n
	 * @return Set of PTs
	 */
	public Set<PT> allPTNthOperand(int n) {
		Set<PT> pts = new HashSet<PT>();
		for (LLRule r: rules) {
			for (LLRule.Condition c: r.condition)
				pts.add(c.trs[0].getPT());
		}
		return pts;
	}

	public Set<TypeRepresentation> allTRNthOperand(int n) {
		Set<TypeRepresentation> trs = new HashSet<TypeRepresentation>();
		for (LLRule r: rules) {
			for (LLRule.Condition c: r.condition)
				trs.add(c.trs[0]);
		}
		return trs;
	}

	/**
	 * Find the low level rule that matches the given condition.
	 */
	public LLRule find(TypeRepresentation... key) {
		for (LLRule r: rules) {
			if (r.find(key) != null)
				return r;
		}
		return null;
	}

	/**
	 * Find the low level rule that matches the given condition.
	 */
	public Set<LLRule> findByPT(PT... key) {
		Set<LLRule> result = new HashSet<LLRule>();
		NEXT_RULE: for (LLRule r: rules) {
			NEXT_CONDITION: for (LLRule.Condition c: r.condition) {
				if (c.arity != key.length)
					continue;
				for (int i = 0; i < c.arity; i++) {
					if (c.trs[i].getPT() != key[i])
						continue NEXT_CONDITION;
				}
				result.add(r);
				continue NEXT_RULE;
			}
		}
		return result;
	}

	/**
	 * Convert this pair-dispatch plan to a nested single-dispatch plan.
	 * @param redirect if true, create redirect actions
	 * @return nested plan
	 */
	public LLPlan convertToNestedPlan(boolean redirect) {
		LLPlan outer = new LLPlan();

		for (TypeRepresentation tr0: allTRNthOperand(0)) {
			LLPlan inner = new LLPlan();
			for (TypeRepresentation tr1: allTRNthOperand(1)) {
				LLRule r = find(tr0, tr1);
				ActionNode a = r.action;
				if (redirect)
					a = new RedirectActionNode(a);
				LLRule.Condition c = new LLRule.Condition(tr1);
				c.done = r.find(tr0, tr1).done;
				LLRule innerRule = new LLRule(c, a);
				inner.rules.add(innerRule);
			}
			LLRule.Condition outerCond = new LLRule.Condition(tr0);
			outerCond.done = inner.rules.stream()
					.flatMap(r -> r.condition.stream())
					.allMatch(c -> c.done);
			UnexpandedActionNode outerAction = new UnexpandedActionNode(inner);
			LLRule outerRule = new LLRule(outerCond, outerAction);
			outer.rules.add(outerRule);
		}

		return outer;
	}

	protected boolean mergable(LLRule r0, LLRule r1) {
		if (!(r0.action instanceof UnexpandedActionNode))
			throw new Error("attempted to merge an ActionNode that is not an UnexpandedActionNode: "+ r0);
		if (!(r1.action instanceof UnexpandedActionNode))
			throw new Error("attempted to merge an ActionNode that is not an UnexpandedActionNode: "+ r1);
		LLPlan llplan0 = ((UnexpandedActionNode) r0.action).ruleList;
		LLPlan llplan1 = ((UnexpandedActionNode) r1.action).ruleList;

		for (LLRule innerRule0: llplan0.rules) {
			NEXT_C0: for (LLRule.Condition c0: innerRule0.condition) {
				for (LLRule innerRule1: llplan1.rules) {
					NEXT_C1: for (LLRule.Condition c1: innerRule1.condition) {
						if (c0.done && c1.done)
							continue NEXT_C0;
						for (int i = 0; i < c0.arity; i++) {
							if (!c0.trs[i].equals(c1.trs[i]))
								continue NEXT_C1;
						}
						if (((RedirectActionNode) innerRule0.action).destination ==
							((RedirectActionNode) innerRule1.action).destination)
							continue NEXT_C0;
						return false;
					}
				}
				return false;
			}
		}
		return true;
	}

	/**
	 * Convert itself to its canonical form.
	 * 1. Canonical form has no rules all of whose conditions has been "done".
	 * 2. All actions of canonical form are distinct.  (Rules that has the same
	 *    action are merged.)
	 */
	public void canonicalise() {
		Set<LLRule> result = new HashSet<LLRule>();

		rules.stream()
		.filter(r -> r.condition.stream().anyMatch(c -> !c.done))
		.forEach(r -> {
			for (LLRule newr: result)
				if (mergable(newr, r)) {
					newr.condition.addAll(r.condition);
					return;
				}
			result.add(r);
		});
		rules = result;
	}

	@Override
	public String toString() {
		System.out.println(rules.size());
		return rules.stream().map(dr -> dr.toString()).collect(Collectors.joining("\n"));
	}
}

class TagPairSynthesiser extends Synthesiser {
	@Override
	String synthesise(Plan plan) {
		LLPlan dispatchRuleList = new LLPlan(plan);
		System.out.println(dispatchRuleList);
		tagPairDispatch(dispatchRuleList);
		LLPlan nestedRuleList = dispatchRuleList.convertToNestedPlan(true);
		System.out.println(nestedRuleList);
		nestedRuleList.canonicalise();
		System.out.println(nestedRuleList);

		// TODO Auto-generated method stub
		//tagPairDispatch(dispatchRuleList);
		return null;
	}

	Set<TypeRepresentation> conditionNthOperand(Set<TypeRepresentation[]> conds, int n) {
		return conds.stream().map(c -> c[n]).collect(Collectors.toSet());
	}

	ActionNode findTerminalActionNode(DispatchActionNode disp, Plan.Rule r) {
		for (Branch b: disp.branches) {
			ActionNode a = b.action;
			if (a instanceof TerminalActionNode && ((TerminalActionNode) a).rule == r)
				return a;
		}
		throw new Error("no TerminalActionNode with action "+ r);
	}

	DispatchActionNode tagPairDispatch(LLPlan llplan) {
		DispatchActionNode disp = new DispatchActionNode();
		Map<LLRule, TagPairBranch> revDisp = new HashMap<LLRule, TagPairBranch>();

		for (LLRule r: llplan.rules) {
			TagPairBranch b = new TagPairBranch(r.action);
			disp.add(b);
			revDisp.put(r, b);
		}

		for(PT pt0: llplan.allPTNthOperand(0))
			for (PT pt1: llplan.allPTNthOperand(1)) {
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

	public static void main(String[] args) throws FileNotFoundException {
		TypeDefinition td = new TypeDefinition();
		td.load("datatype/small.dtdef");
		System.out.println(td);
		System.out.println(DataType.uniquePT(DataType.typeRepresentationOf(Stream.of("fixnum", "string", "array").map(n -> DataType.get(n)))));
		System.out.println(DataType.uniquePT(DataType.typeRepresentationOf(Stream.of(
				"simple_object",
				"array",
				"string").map(n -> DataType.get(n)))));

        ProcDefinition procDef = new ProcDefinition();
        procDef.load("datatype/sample.idef");
        System.out.println(procDef);
        InstDefinition instDef = (InstDefinition) procDef.defs.get(0);
        Plan p = new Plan(instDef.dispatchVars.length, instDef.toRules());

        new TagPairSynthesiser().synthesise(p);
	}

}
