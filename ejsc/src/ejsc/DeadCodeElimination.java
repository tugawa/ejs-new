/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ejsc.ControlFlowGraph.CFGNode;

public class DeadCodeElimination {
    List<BCode> bcodes;
    ControlFlowGraph cfg;
    HashSet<BCode> live;

    public DeadCodeElimination(List<BCode> bcodes, Main.Info info) {
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
            if (live.contains(bc)) {
                newBCodes.add(bc);

                if (bc instanceof IPushhandler) {
                    /*
                     * If there is no edge to the handler, the handler is eliminated.
                     * Pretend the pushhandler instruction itself is the handler, which
                     * is never used but popped by pophandler.
                     */
                    IPushhandler pushHandler = (IPushhandler) bc;
                    BCode handlerBc = pushHandler.label.getDestBCode();
                    if (!live.contains(handlerBc)) {
                        Label dummyHandler = new Label();
                        pushHandler.addLabel(dummyHandler);
                        pushHandler.label = dummyHandler;
                    }
                } 
            } else if (bc.getLabels().size() > 0) {
                for (BCode b: bcodes) {
                    if (b.getBranchTarget() == bc && live.contains(b))
                        throw new Error("internal error");                            
                }
            }
        }
        return newBCodes;
    }
}
