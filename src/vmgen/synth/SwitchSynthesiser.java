package vmgen.synth;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import vmgen.LLPlan;
import vmgen.LLRule;
import vmgen.Plan;
import vmgen.dd.Branch;
import vmgen.dd.DDDispatchNode;
import vmgen.dd.DDLeaf;
import vmgen.dd.DDNode;
import vmgen.dd.DDRedirectNode;
import vmgen.dd.DDUnexpandedNode;
import vmgen.dd.HTBranch;
import vmgen.dd.PTBranch;
import vmgen.type.VMRepType;


public class SwitchSynthesiser extends Synthesiser {
	@Override
	public String synthesise(Plan plan) {
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
		DDNode root = nestedDispatch(nestedRuleList);
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

	DDNode simplify(DDNode n_) {
		if (n_ instanceof DDDispatchNode) {
			DDDispatchNode n = (DDDispatchNode) n_;
			n.branches.forEach(b -> b.action = simplify(b.action));
			n.branches.removeIf(b -> {
				if (b.size() == 0)
					return true;
				DDNode a = b.action;
				while (a instanceof DDRedirectNode)
					a = ((DDRedirectNode) a).destination;
				if (a instanceof DDDispatchNode)
					if (((DDDispatchNode) a).branches.size() == 0)
						return true;
				return false;
			});
			if (n.branches.size() == 1)
				return n.branches.iterator().next().action;
			return n;
		} else if (n_ instanceof DDRedirectNode) {
			return simplify(((DDRedirectNode) n_).destination);
		} else if (n_ instanceof DDLeaf)
			return n_;
		else if (n_ instanceof DDUnexpandedNode)
			return n_;
		throw new Error("Undexpcted action node: "+ n_);
	}

	void arrangeTerminalNode(DDNode root) {
		Queue<DDNode> queue = new LinkedList<DDNode>();
		queue.add(root);
		while (!queue.isEmpty()) {
			DDNode a_ = queue.element();
			queue.remove();

			if (a_ instanceof DDDispatchNode) {
				DDDispatchNode a = (DDDispatchNode) a_;
				for (Branch b: a.branches) {
					while (b.action instanceof DDRedirectNode)
						b.action = ((DDRedirectNode) b.action).destination;
					if (b.action.arranged)
						b.action = new DDRedirectNode(b.action);
					else
						queue.add(b.action);
					b.action.arranged = true;
				}
			}
		}
	}

	DDDispatchNode nestedDispatch(LLPlan llplan) {
		DDDispatchNode disp = new DDDispatchNode(getPTCode(llplan.getDispatchVars()));
		Map<LLRule, PTBranch> revDisp = new HashMap<LLRule, PTBranch>();
		Map<DDUnexpandedNode, DDNode> cache = new HashMap<DDUnexpandedNode, DDNode>();

		for (LLRule r: llplan.rules) {
			DDNode a = r.action;
			if (a instanceof DDUnexpandedNode) {
				DDUnexpandedNode unexpanded = (DDUnexpandedNode) a;
				DDNode expanded = nestedDispatch(unexpanded.ruleList);
				cache.put(unexpanded, expanded);
				a = expanded;
			}
			PTBranch b = new PTBranch(a);
			disp.add(b);
			revDisp.put(r, b);
		}

		/* pointer tag */
		for (VMRepType.PT pt: llplan.allPTNthOperand(0)) {
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
			DDDispatchNode htDisp = new DDDispatchNode(getHTCode(llplan.getDispatchVars()));
			PTBranch others = new PTBranch(htDisp);
			disp.add(others);

			llplan.rules.stream()
			.flatMap(r -> r.condition.stream())
			.map(c -> c.trs[0].getPT())
			.forEach(pt -> others.addCondition(pt));
			llplan.rules.forEach(r -> {
				DDNode a = r.action;
				if (a instanceof DDUnexpandedNode)
					a = cache.get(a);
				a = new DDRedirectNode(a);
				HTBranch b = new HTBranch(a);
				htDisp.add(b);
				r.condition.stream().map(c -> c.trs[0].getHT()).forEach(ht -> {
					b.addCondition(ht);
				});
			});
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
        String[] dispatchVars = Arrays.stream(instDef.dispatchVars)
        		.map(s -> s.substring(1, s.length()))
        		.collect(Collectors.toList())
        		.toArray(new String[]{});
        Plan p = new Plan(dispatchVars, instDef.tdDef.rules);
        Synthesiser sy = new SwitchSynthesiser();
        System.out.println(sy.synthesise(p));
	}
	*/
}
