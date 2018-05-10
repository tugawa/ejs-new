package ejsc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ejsc.ControlFlowGraph.CFGNode;

public class DeadCodeElimination {
    List<BCode> bcodes;
    ControlFlowGraph cfg;
    HashSet<BCode> live;
    
    public DeadCodeElimination(List<BCode> bcodes) {
        this.bcodes = bcodes;
        cfg = new ControlFlowGraph(bcodes);
        live = new HashSet<BCode>();
        dfs(cfg.get(bcodes.get(0)));
    }
    
    private void dfs(CFGNode node) {
        if (live.contains(node.getBCode()))
            return;
        live.add(node.getBCode());
        for (CFGNode next: node.getSuccs())
            dfs(next);
    }
    
    public List<BCode> exec() {
        List<BCode> newBCodes = new ArrayList<BCode>(live.size());
        for (BCode bc: bcodes) {
            if (live.contains(bc))
                newBCodes.add(bc);
            else if (bc.getLabels().size() > 0) {
                for (BCode b: bcodes) {
                    if (b.getBranchTarget() == bc && live.contains(b))
                        throw new Error("internal error");                            
                }
            }
        }
        return newBCodes;
    }
}
