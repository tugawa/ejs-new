/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
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
