package vmgen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vmgen.LLRule.Condition;
import vmgen.dd.DDUnexpandedNode;
import vmgen.type.VMRepType;

public class LLPlan {
	private String[] dispatchVars;
	public Set<LLRule> rules;

	public LLPlan(Plan plan) {
		dispatchVars = plan.getDispatchVars();
		rules = new HashSet<LLRule>();
		for (Plan.Rule hr: plan.getRules())
			rules.add(new LLRule(hr));
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
	public Set<VMRepType.PT> allPTNthOperand(int n) {
		Set<VMRepType.PT> pts = new HashSet<VMRepType.PT>();
		for (LLRule r: rules) {
			for (LLRule.Condition c: r.condition)
				pts.add(c.trs[n].getPT());
		}
		return pts;
	}

	public Set<VMRepType> allTRNthOperand(int n) {
		Set<VMRepType> trs = new HashSet<VMRepType>();
		for (LLRule r: rules) {
			for (LLRule.Condition c: r.condition)
				trs.add(c.trs[n]);
		}
		return trs;
	}

	/**
	 * Find the low level rule that matches the given condition.
	 */
	public LLRule find(VMRepType... key) {
		for (LLRule r: rules) {
			if (r.find(key) != null)
				return r;
		}
		return null;
	}

	/**
	 * Find the low level rule that matches the given condition.
	 */
	public Set<LLRule> findByPT(VMRepType.PT... key) {
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
	public LLPlan convertToNestedPlan(boolean redirect, VMRepType[] dispatchVals) {
		int level = dispatchVals.length;
		LLPlan outer = new LLPlan(new String[] {dispatchVars[level]});
		for (VMRepType tr: allTRNthOperand(level)) {
			VMRepType[] nextVals = new VMRepType[level + 1];
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
				DDUnexpandedNode outerAction = new DDUnexpandedNode(inner);
				LLRule outerRule = new LLRule(outerCond, outerAction);
				outer.rules.add(outerRule);
			}
		}
		return outer;
	}

	public LLPlan convertToNestedPlan(boolean redirect) {
		//return convertToNestedPlan(redirect, new VMRepType[]{});
		
		LLPlan nestedPlan = new LLPlan(new String[] {dispatchVars[0]});
		for (LLRule r: rules) {
			for (Condition c: r.condition) {
				LLPlan p = nestedPlan;
				ArrayList<LLRule.Condition> path = new ArrayList<LLRule.Condition>();
				for (int i = 0; i < c.arity; i++) {
					LLRule edge = p.find(c.trs[i]);
					if (edge == null) {
						LLRule.Condition singleCondition = new LLRule.Condition(c.trs[i]);
						singleCondition.done = true;
						if (i == c.arity - 1) {
							if (!c.done) {
								singleCondition.done = false;
								for (LLRule.Condition cc: path)
									cc.done = false;
							}
							edge = new LLRule(singleCondition, r.action);
							p.rules.add(edge);
							continue;
						} else {
							System.out.println("c.arity = "+c.arity+", i = "+i);
							LLPlan lower = new LLPlan(new String[] {dispatchVars[i+1]});
							DDUnexpandedNode lowerNode = new DDUnexpandedNode(lower);
							edge = new LLRule(singleCondition, lowerNode);
							p.rules.add(edge);
						}
					}
					path.add(edge.condition.iterator().next());
					p = ((DDUnexpandedNode) edge.action).ruleList;
				}
			}
		}
		return nestedPlan;
	}

	/*
	protected boolean mergable(LLRule r0, LLRule r1) {
		if (!(r0.action instanceof DDUnexpandedNode))
			throw new Error("attempted to merge an ActionNode that is not an UnexpandedActionNode: "+ r0);
		if (!(r1.action instanceof DDUnexpandedNode))
			throw new Error("attempted to merge an ActionNode that is not an UnexpandedActionNode: "+ r1);
		LLPlan llplan0 = ((DDUnexpandedNode) r0.action).ruleList;
		LLPlan llplan1 = ((DDUnexpandedNode) r1.action).ruleList;

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
						if (((DDRedirectNode) innerRule0.action).destination ==
							((DDRedirectNode) innerRule1.action).destination)
							continue NEXT_C0;
						return false;
					}
				}
				return false;
			}
		}
		return true;
	}
	*/

	/**
	 * Convert itself to its canonical form.
	 * 1. Canonical form has no rules all of whose conditions has been "done".
	 * 2. No rule of canonical form have conditions that are "done".
	 * 3. All actions of canonical form are distinct.  (Rules that has the same
	 *    action are merged.)
	 * 4. All inner plans are in canonical form.
	 */
	public void canonicalise() {
		Set<LLRule> result = new HashSet<LLRule>();

		NEXT_RULE: for (LLRule r: rules) {
			Set<LLRule.Condition> filteredConditions = new HashSet<LLRule.Condition>();
			for (LLRule.Condition c: r.condition)
				if (!c.done)
					filteredConditions.add(c);

			if (filteredConditions.size() == 0)
				continue;  // 1.
			else if (filteredConditions.size() < r.condition.size())
				r = new LLRule(filteredConditions, r.action);  // 2.

			for (LLRule newRule: result) {
				if (newRule.action.mergable(r.action)) {
					newRule.condition.addAll(r.condition);  // 3.
					continue NEXT_RULE;
				}
			}

			if (r.action instanceof DDUnexpandedNode)
				((DDUnexpandedNode) r.action).canonicaliseRuleList(); // 4.
	
			result.add(r);
		}
		rules = result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		if (rules.size() > 0) {
			for (LLRule r: rules)
				sb.append(r).append("\n");
			sb.delete(sb.length() - 1, sb.length());
		}
		return sb.toString();
	}

	public String[] getDispatchVars() {
		return dispatchVars;
	}

	public void setDispatchVars(String[] dispatchVars) {
		this.dispatchVars = dispatchVars;
	}
}
