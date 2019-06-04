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
import java.util.List;

public class RedundantInstructionElimination {
    private static final boolean DEBUG = false;
    List<BCode> bcodes;
    LiveRegisterAnalyser lra;

    RedundantInstructionElimination(List<BCode> bcodes) {
        this.bcodes = bcodes;
        lra = new LiveRegisterAnalyser(bcodes);
        if (DEBUG)
            lra.print(bcodes);
    }

    private boolean isRedundant(BCode bc) {
        Register dst = bc.getDestRegister();
        if (dst == null)
            return false;
        if (lra.getLiveRegisters(bc).contains(dst))
            return false;
        if (bc instanceof IError)
            return false;
        // TODO: check side effect

        if (!bc.isFallThroughInstruction() || bc.getBranchTarget() != null)
            throw new Error("control instruction: "+bc);

        if (DEBUG) {
            System.out.print("Eliminate: "+bc+" LiveRegs = {");
            for (Register r: lra.getLiveRegisters(bc))
                System.out.print("r"+r.n+" ");
            System.out.println("}");
        }

        return true;
    }

    public List<BCode> exec() {
        List<BCode> newBCodes = new ArrayList<BCode>(bcodes.size());

        ArrayList<Label> labels = new ArrayList<Label>();
        for (BCode bc: bcodes) {
            if (isRedundant(bc)) {
                labels.addAll(bc.getLabels());
                continue;
            }
            bc.addLabels(labels);
            labels.clear();
            newBCodes.add(bc);
        }

        return newBCodes;
    }
}
