package ejsc;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CBCLiveRegisterAnalyser {
    HashMap<CBCode, Set<Register>> inMap = new HashMap<CBCode, Set<Register>>();
    HashMap<CBCode, Set<Register>> outMap = new HashMap<CBCode, Set<Register>>();

    public Set<Register> getLiveRegisters(CBCode bc) {
        return inMap.get(bc);
    }

    public CBCLiveRegisterAnalyser(List<CBCode> bcodes) {
        CBCControlFlowGraph cfg = new CBCControlFlowGraph(bcodes);
        for (CBCode bc: bcodes) {
            inMap.put(bc,  new HashSet<Register>());
            outMap.put(bc, new HashSet<Register>(bc.getSrcRegisters()));
        }

        boolean fixPoint = false;
        while (!fixPoint) {
            fixPoint = true;
            for (CBCControlFlowGraph.CFGNode n: cfg.getNodes()) {
                CBCode bc = n.getBCode();
                Set<Register> in = inMap.get(bc);
                Set<Register> out = outMap.get(bc);
                /* in += Union succ(out) */
                for (CBCControlFlowGraph.CFGNode succ: n.getSuccs()) {
                    CBCode succBC = succ.getBCode();
                    if (in.addAll(outMap.get(succBC)))
                        fixPoint = false;
                }
                /* out += in - kill */
                for (Register r: in) {
                    Register storeReg = bc.getDestRegister();
                    if (r == storeReg) // kill
                        continue;
                    if (out.add(r))
                        fixPoint = false;
                }
            }
        }
    }

    public void print(List<CBCode> bcodes) {
        System.out.println("----- CBC Live Register Analyser begin -----");
        for (CBCode bc: bcodes)
            System.out.println(bc.number + ": "+bc+" "+showRegs(getLiveRegisters(bc)));
        System.out.println("----- CBC Live Register Analyser end -----");
    }

    static public String showRegs(Set<Register> regs) {
        String s = "{";
        for (Register r: regs)
            s += "r" + r + " ";
        return s + "}";
    }
}
