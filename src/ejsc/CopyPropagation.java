/*
   CopyPropagation.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Taiki Fujimoto, 2018
     Tomoharu Ugawa, 2018
     Hideya Iwasaki, 2018

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/
package ejsc;
import java.lang.reflect.Field;
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
			if (!isDefinedOnAnyPath(def, bc, defSrc)) {
				return defSrc;
			}
		}
		return null;
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
		boolean update = true;
		while (update) {
			update = false;
			for (ControlFlowGraph.CFGNode n: cfg.getNodes()) {
				BCode bcx = n.getBCode();
				try {
					Class<? extends BCode> c = bcx.getClass();
					for (Field f: c.getDeclaredFields()) {
						if (f.getType() == Register.class) {
							Register r = (Register) f.get(bcx);
							Register rr = getReplaceRegister(bcx, r);
							if (rr != null) {
								update = true;
								f.set(bcx, rr);
							}
						} else if (f.getType() == Register[].class) {
							Register[] rs = (Register[]) f.get(bcx);
							for (int i = 0; i < rs.length; i++) {
								Register r = rs[i];
								Register rr = getReplaceRegister(bcx, r);
								if (rr != null) {
									update = true;
									rs[i] = rr;
								}
							}
						}
					}
				} catch (Exception e) {
					throw new Error(e);
				}
			}
		}
	}
}
