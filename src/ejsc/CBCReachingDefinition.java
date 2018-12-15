package ejsc;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CBCReachingDefinition {
    HashMap<CBCode, Set<CBCode>> inMap = new HashMap<CBCode, Set<CBCode>>();
    HashMap<CBCode, Set<CBCode>> outMap = new HashMap<CBCode, Set<CBCode>>();

    public Set<CBCode> getReachingDefinitions(CBCode bc) {
        return inMap.get(bc);
    }

    public CBCReachingDefinition(List<CBCode> bcodes) {
        this(new CBCControlFlowGraph(bcodes));
    }

    public CBCReachingDefinition(CBCControlFlowGraph cfg) {
        for (CBCControlFlowGraph.CFGNode n: cfg.getNodes()) {
            CBCode bc = n.getBCode();
            inMap.put(bc, new HashSet<CBCode>());
            Set<CBCode> out = new HashSet<CBCode>();
            if (bc.getStoreRegister() != null)
                out.add(bc); // gen
            outMap.put(bc,  out);
        }

        boolean fixPoint = false;
        while (!fixPoint) {
            fixPoint = true;
            for (CBCControlFlowGraph.CFGNode n: cfg.getNodes()) {
                CBCode bc = n.getBCode();
                /* in += Union pred(out) */
                for (CBCControlFlowGraph.CFGNode pred: n.getPreds()) {
                    CBCode predBC = pred.getBCode();
                    if (inMap.get(bc).addAll(outMap.get(predBC)))
                        fixPoint = false;
                }
                /* out += in - kill */
                for (CBCode inBC: inMap.get(bc)) {
                    if (inKillSet(bc, inBC))
                        continue;
                    if (outMap.get(bc).add(inBC))
                        fixPoint = false;
                }
            }
        }
    }
    
    // return target \in Kill_{self}
    private boolean inKillSet(CBCode self, CBCode target) {
        Register selfArg = self.getStoreRegister();
        Register targetArg = target.getStoreRegister();
        if (selfArg == null || targetArg == null)
            return false;
        return selfArg == targetArg;
    }
}
