/*
CommonConstantElimination.java

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
