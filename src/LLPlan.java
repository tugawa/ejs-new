import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
							.map(tr -> tr.name)
							.collect(Collectors.joining("*"));
				}).collect(Collectors.joining(", "))
				+ "} -> " + action.toString() + "]";
	}
}

public class LLPlan {
	String[] dispatchVars;
	Set<LLRule> rules;

	LLPlan(Plan plan) {
		dispatchVars = plan.dispatchVars;
		rules = plan.getRules().stream()
				.map(r -> new LLRule(r, plan.getArity()))
				.collect(Collectors.toSet());
	}

	LLPlan(String[] dispatchVars) {
		this.dispatchVars = dispatchVars;
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
	 * Convert this tuple-dispatch plan to a nested single-dispatch plan.
	 * @param redirect if true, create redirect actions
	 * @return nested plan
	 */
	public LLPlan convertToNestedPlan(boolean redirect, TypeRepresentation[] dispatchVals) {
		int level = dispatchVals.length;
		LLPlan outer = new LLPlan(new String[] {dispatchVars[level]});
		for (TypeRepresentation tr: allTRNthOperand(level)) {
			TypeRepresentation[] nextVals = new TypeRepresentation[level + 1];
			System.arraycopy(dispatchVals, 0, nextVals, 0, level);
			nextVals[level] = tr;
			if (nextVals.length == dispatchVars.length) {
				LLRule r = find(nextVals);
				LLRule.Condition c = new LLRule.Condition(tr);
				c.done = r.find(nextVals).done;
				LLRule newRule = new LLRule(c, r.action);
				outer.rules.add(newRule);
			} else {
				LLPlan inner = convertToNestedPlan(redirect, nextVals);
				LLRule.Condition outerCond = new LLRule.Condition(tr);
				outerCond.done = inner.rules.stream()
						.flatMap(r -> r.condition.stream())
						.allMatch(c -> c.done);
				UnexpandedActionNode outerAction = new UnexpandedActionNode(inner);
				LLRule outerRule = new LLRule(outerCond, outerAction);
				outer.rules.add(outerRule);
			}
		}
		return outer;
	}

	public LLPlan convertToNestedPlan(boolean redirect) {
		return convertToNestedPlan(redirect, new TypeRepresentation[]{});
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
	 * 3. All inner plans are in canonical form.
	 */
	public void canonicalise() {
		Set<LLRule> result = new HashSet<LLRule>();

		rules.stream()
		.filter(r -> r.condition.stream().anyMatch(c -> !c.done))
		.map(r -> {
			Set<LLRule.Condition> cond = r.condition.stream().filter(c -> !c.done).collect(Collectors.toSet());
			return new LLRule(cond, r.action);
		})
		.forEach(r -> {
			for (LLRule newr: result)
				if (newr.action.mergable(r.action)) {
					newr.condition.addAll(r.condition);
					return;
				}
			result.add(r);
		});
		result.forEach(r -> {
			if (r.action instanceof UnexpandedActionNode)
				((UnexpandedActionNode) r.action).ruleList.canonicalise();
		});
		rules = result;
	}

	@Override
	public String toString() {
		return rules.stream().map(dr -> dr.toString()).collect(Collectors.joining("\n"));
	}
}