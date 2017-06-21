import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class SwitchSynthesiser extends Synthesiser {
	@Override
	String synthesise(Plan plan) {
		LLPlan dispatchRuleList = new LLPlan(plan);
		if (PRINT_PASS) {
			System.out.println("-------- LLPlan --------");
			System.out.println(dispatchRuleList);
		}
//		System.out.println(dispatchRuleList);
		LLPlan nestedRuleList = dispatchRuleList.convertToNestedPlan(true);
//		System.out.println(nestedRuleList)
		nestedRuleList.canonicalise();
		if (PRINT_PASS) {
			System.out.println("-------- canonicalised nested LLPlan --------");
			System.out.println(nestedRuleList);
		}
		ActionNode root = nestedDispatch(nestedRuleList);
//		System.out.println(d);
//		System.out.println(root);
//		System.out.println("----------------");
		if (PRINT_PASS) {
			System.out.println("-------- unoptimised decision tree --------");
			System.out.println(root);
		}
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

	ActionNode simplify(ActionNode n_) {
		if (n_ instanceof DispatchActionNode) {
			DispatchActionNode n = (DispatchActionNode) n_;
			n.branches.forEach(b -> b.action = simplify(b.action));
			n.branches.removeIf(b -> {
				if (b.size() == 0)
					return true;
				ActionNode a = b.action;
				while (a instanceof RedirectActionNode)
					a = ((RedirectActionNode) a).destination;
				if (a instanceof DispatchActionNode)
					if (((DispatchActionNode) a).branches.size() == 0)
						return true;
				return false;
			});
			if (n.branches.size() == 1)
				return n.branches.iterator().next().action;
			return n;
		} else if (n_ instanceof RedirectActionNode) {
			return simplify(((RedirectActionNode) n_).destination);
		} else if (n_ instanceof TerminalActionNode)
			return n_;
		else if (n_ instanceof UnexpandedActionNode) {
			UnexpandedActionNode n = (UnexpandedActionNode) n_;
			if (n.expanded != null)
				return simplify(n.expanded);
		}
		throw new Error("Undexpcted action node: "+ n_);
	}

	void arrangeTerminalNode(ActionNode root) {
		Queue<ActionNode> queue = new LinkedList<ActionNode>();
		queue.add(root);
		while (!queue.isEmpty()) {
			ActionNode a_ = queue.element();
			queue.remove();

			if (a_ instanceof DispatchActionNode) {
				DispatchActionNode a = (DispatchActionNode) a_;
				for (Branch b: a.branches) {
					while (true) {
						if (b.action instanceof RedirectActionNode)
							b.action = ((RedirectActionNode) b.action).destination;
						else if (b.action instanceof UnexpandedActionNode)
							b.action = ((UnexpandedActionNode) b.action).expanded;
						else
							break;
					}
					if (b.action.arranged)
						b.action = new RedirectActionNode(b.action);
					else
						queue.add(b.action);
					b.action.arranged = true;
				}
			}
		}
	}

	DispatchActionNode nestedDispatch(LLPlan llplan) {
		DispatchActionNode disp = new DispatchActionNode(getPTCode(llplan.dispatchVars));
		Map<LLRule, PTBranch> revDisp = new HashMap<LLRule, PTBranch>();

		for (LLRule r: llplan.rules) {
			ActionNode a = r.action;
			if (a instanceof UnexpandedActionNode) {
				a = nestedDispatch(((UnexpandedActionNode) a).ruleList);
				((UnexpandedActionNode) r.action).setExpanded(a);
			}
			PTBranch b = new PTBranch(a);
			disp.add(b);
			revDisp.put(r, b);
		}

		/* pointer tag */
		for (PT pt: llplan.allPTNthOperand(0)) {
			Set<LLRule> match = llplan.findByPT(pt);
			if (match.size() == 1) {
				LLRule r = match.iterator().next();
				PTBranch b = revDisp.get(r);
				b.addCondition(pt);
				r.condition.forEach(c -> {
					if (c.trs[0].getPT() == pt)
						c.done = true;
				});
			}
		}

		/* header type */
		llplan.canonicalise();
		if (llplan.rules.size() > 0) {
			DispatchActionNode htDisp = new DispatchActionNode(getHTCode(llplan.dispatchVars));
			PTBranch others = new PTBranch(htDisp);
			disp.add(others);

			llplan.rules.stream()
			.flatMap(r -> r.condition.stream())
			.map(c -> c.trs[0].getPT())
			.forEach(pt -> others.addCondition(pt));

			llplan.rules.forEach(r -> {
				ActionNode a = new RedirectActionNode(r.action);
				HTBranch b = new HTBranch(a);
				htDisp.add(b);
				r.condition.stream().map(c -> c.trs[0].getHT()).forEach(ht -> {
					b.addCondition(ht);
				});
			});
		}

		return disp;
	}

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
        String[] dispatchVars = Arrays.stream(instDef.dispatchVars)
        		.map(s -> s.substring(1, s.length()))
        		.collect(Collectors.toList())
        		.toArray(new String[]{});
        Plan p = new Plan(dispatchVars, instDef.tdDef.rules);
        Synthesiser sy = new SwitchSynthesiser();
        System.out.println(sy.synthesise(p));
	}

}
