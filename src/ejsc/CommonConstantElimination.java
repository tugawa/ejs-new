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
import java.util.Set;

public class CommonConstantElimination {
    List<BCode> bcodes;
    AvailableExpressionAnalyser aea;

    CommonConstantElimination(List<BCode> bcodes) {
        this.bcodes = bcodes;
        aea = new AvailableExpressionAnalyser(bcodes); 
    }

    public List<BCode> exec() {
        ArrayList<BCode> newBCodes = new ArrayList<BCode>(bcodes.size());
        for (BCode bc: bcodes) {
            AvailableExpressionAnalyser.Value v = AvailableExpressionAnalyser.computeGenValue(bc);
            if (v != null) {
                Set<Register> rs = aea.getRegisterForValue(bc, v);
                if (rs != null && !rs.isEmpty()) {
                    Register src = rs.iterator().next();
                    Register dst = bc.getDestRegister();
                    BCode newBC = new IMove(dst, src);
                    newBC.addLabels(bc.getLabels());
                    newBCodes.add(newBC);
                    continue;
                }
            }
            newBCodes.add(bc);
        }
        return newBCodes;
    }
}
