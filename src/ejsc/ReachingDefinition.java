/*
   ReachingDefinition.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReachingDefinition {
	HashMap<BCode, Set<BCode>> inMap = new HashMap<BCode, Set<BCode>>();
	HashMap<BCode, Set<BCode>> outMap = new HashMap<BCode, Set<BCode>>();
	
	public Set<BCode> getReachingDefinitions(BCode bc) {
		return inMap.get(bc);
	}

	public ReachingDefinition(List<BCode> bcodes) {
		this(new ControlFlowGraph(bcodes));
	}
	
	public ReachingDefinition(ControlFlowGraph cfg) {
		for (ControlFlowGraph.CFGNode n: cfg.getNodes()) {
			BCode bc = n.getBCode();
			inMap.put(bc, new HashSet<BCode>());
			Set<BCode> out = new HashSet<BCode>();
			if (bc.getDestRegister() != null)
				out.add(bc);  // gen
			outMap.put(bc,  out);
		}
		
		boolean fixPoint = false;
		while (!fixPoint) {
			fixPoint = true;
			for (ControlFlowGraph.CFGNode n: cfg.getNodes()) {
				BCode bc = n.getBCode();
				/* in += Union pred(out) */
				for (ControlFlowGraph.CFGNode pred: n.getPreds()) {
					BCode predBC = pred.getBCode();
					if (inMap.get(bc).addAll(outMap.get(predBC)))
						fixPoint = false;
				}
				/* out += in - kill */
				for (BCode inBC: inMap.get(bc)) {
					if (inKillSet(bc, inBC))
						continue;
					if (outMap.get(bc).add(inBC))
						fixPoint = false;
				}
			}
		}
	}
	
	// return target \in Kill_{self}
	private boolean inKillSet(BCode self, BCode target) {
		Register cr = self.getDestRegister();
		if (cr == null)
			return false;
		return cr == target.getDestRegister();
	}
}
