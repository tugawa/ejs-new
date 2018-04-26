package ejsc;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import ejsc.ControlFlowGraph.CFGNode;

public class CopyPropagation {
	List<BCode> bcodes;
	ControlFlowGraph cfg;
	ReachingDefinition rda;
	CopyPropagation(List<BCode> bcodes) {
		this.bcodes = bcodes;
		cfg = new ControlFlowGraph(bcodes);
		rda = new ReachingDefinition(cfg);
	}
	
	private Register getReplaceRegister(BCode bc, Register src) {
		BCode def = findDefinition(bc, src);
		if (def instanceof IMove) {
			Register defSrc = ((IMove) def).src;
			if (!isDefinedOnAnyPath(def, bc, defSrc))
				return defSrc;
		}
		return src;
	}
	
	private BCode findDefinition(BCode bc, Register src) {
		BCode result = null;
		for (BCode def: rda.getReachingDefinitions(bc)) {
			if (def.getDestRegister() == src) {
				if (result == null)
					result = def;
				else
					return null;
			}
		}
		return result;
	}
	
	private boolean dfs(CFGNode n, CFGNode to, Register r, boolean defined, HashMap<CFGNode, Boolean> visited) {
		if (n == to && defined)
			return true;
		Boolean v = visited.get(n);
		if (v != null && (v || !defined))
			return false;
		visited.put(n, defined);
		
		if (n.getBCode().getDestRegister() == r)
			defined = true;
		for (CFGNode succ: n.getSuccs()) {
			if (dfs(succ, to, r, defined, visited))
				return true;
		}
		return false;
	}
	
	private boolean isDefinedOnAnyPath(BCode fromBC, BCode toBC, Register r) {
		HashMap<CFGNode, Boolean> visited = new HashMap<CFGNode, Boolean>();
		CFGNode from = cfg.get(fromBC);
		CFGNode to = cfg.get(toBC);
		return dfs(from, to, r, false, visited);
	}
	
	public void exec() {
		for (ControlFlowGraph.CFGNode n: cfg.getNodes()) {
			BCode bcx = n.getBCode();
			
			if (bcx instanceof IGetprop) {
				IGetprop bc = (IGetprop) bcx;
				bc.obj = getReplaceRegister(bc, bc.obj);
				bc.prop = getReplaceRegister(bc, bc.prop);
			} else if (bcx instanceof IMove) {
				IMove bc = (IMove) bcx;
				bc.src = getReplaceRegister(bc, bc.src);
			}
		}
	}
}
