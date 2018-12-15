package ejsc;
import java.util.ArrayList;
import java.util.List;

public class CBCRedundantInstructionElimination {
    private static final boolean DEBUG = false;
    List<CBCode> bcodes;
    CBCLiveRegisterAnalyser lra;

    CBCRedundantInstructionElimination(List<CBCode> bcodes) {
        this.bcodes = bcodes;
        lra = new CBCLiveRegisterAnalyser(bcodes);
        if (DEBUG)
            lra.print(bcodes);
    }

    private boolean isRedundant(CBCode bc) {
        Register reg = bc.getStoreRegister();
        if (reg == null)
            return false;
        if (lra.getLiveRegisters(bc).contains(reg))
            return false;
        if (bc instanceof ICBCError)
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

    public List<CBCode> exec() {
        List<CBCode> newBCodes = new ArrayList<CBCode>(bcodes.size());

        ArrayList<CBCLabel> labels = new ArrayList<CBCLabel>();
        for (CBCode bc: bcodes) {
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
