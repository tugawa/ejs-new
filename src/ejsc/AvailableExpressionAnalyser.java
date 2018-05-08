/*
AvailableExpressionAnalyser.java

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

import ejsc.ControlFlowGraph.CFGNode;

public class AvailableExpressionAnalyser {
    static class Value {}
    static class NumberValue extends Value {
        double n;
        NumberValue(double n) {
            this.n = n;
        }
        @Override
        public boolean equals(Object v) {
            if (v instanceof NumberValue)
                if (((NumberValue) v).n == n)
                    return true;
            return false;
        }
        @Override
        public int hashCode() {
            return (int) n;
        }
        @Override
        public String toString() {
            return Double.toString(n);
        }
    }
    static class FixnumValue extends Value {
        int n;
        FixnumValue(int n) {
            this.n = n;
        }
        @Override
        public boolean equals(Object v) {
            if (v instanceof FixnumValue)
                if (((FixnumValue) v).n == n)
                    return true;
            return false;
        }
        @Override
        public int hashCode() {
            return n;
        }
        @Override
        public String toString() {
            return Integer.toString(n);
        }
    }
    static class StringValue extends Value {
        String s;
        StringValue(String s) {
            this.s = s;
        }
        @Override
        public boolean equals(Object v) {
            if (v instanceof StringValue)
                if (((StringValue) v).s.equals(s))
                    return true;
            return false;
        }
        @Override
        public int hashCode() {
            return s.hashCode();
        }
        @Override
        public String toString() {
            return s;
        }
    }
    static class BooleanValue extends Value {
        boolean b;
        BooleanValue(Boolean b) {
            this.b = b;
        }
        @Override
        public boolean equals(Object v) {
            if (v instanceof BooleanValue)
                if (((BooleanValue) v).b == b)
                    return true;
            return false;
        }
        @Override
        public int hashCode() {
            return b ? 1 : 0;
        }
        @Override
        public String toString() {
            return b ? "true" : "false";
        }
    }
    static class NullValue extends Value {
        @Override
        public boolean equals(Object v) {
            return v instanceof NullValue;
        }
        @Override
        public int hashCode() {
            return 0;
        }
        @Override
        public String toString() {
            return "null";
        }
    }
    static class UndefinedValue extends Value {
        @Override
        public boolean equals(Object v) {
            return v instanceof UndefinedValue;
        }
        @Override
        public int hashCode() {
            return 0;
        }
        @Override
        public String toString() {
            return "undefined";
        }
    }
    
    static class AvailableVals extends HashMap<Value, Set<Register>> {
        public AvailableVals() {}
        public AvailableVals(AvailableVals src) {
            for (Value v: src.keySet())
                put(v, new HashSet<Register>(src.get(v)));
        }
        public boolean put(Value v, Register r) {
            Set<Register> rs = get(v);
            if (rs == null) {
                rs = new HashSet<Register>();
                put(v, rs);
            }
            return rs.add(r);
        }
        public boolean addAll(AvailableVals other) {
            boolean update = false;
            for (Value v: other.keySet()) {
                Set<Register> rs = get(v);
                Set<Register> otherRs = other.get(v);
                if (rs == null) {
                    put(v, new HashSet<Register>(otherRs));
                    update = true;
                } else {
                    update |= rs.addAll(otherRs);
                }
            }
            return update;
        }
        @Override
        public String toString() {
            String s = "[";
            for (Value v: keySet()) {
                s += v + " => {";
                for (Register r: get(v))
                    s += "r" + r + " ";
                s += "} ";
            }
            return s + "]";            
        }
    }
    
    ControlFlowGraph cfg;
    HashMap<BCode, AvailableVals> ins = new HashMap<BCode, AvailableVals>();
    HashMap<BCode, AvailableVals> outs = new HashMap<BCode, AvailableVals>();
    
    public AvailableExpressionAnalyser(List<BCode> bcodes) {
        this(new ControlFlowGraph(bcodes));
    }
    
    public AvailableExpressionAnalyser(ControlFlowGraph cfg) {
        this.cfg = cfg;
        
        for (CFGNode node: cfg.getNodes()) {
            BCode bc = node.getBCode();
            ins.put(bc, new AvailableVals());
            AvailableVals out = new AvailableVals();
            Value genVal = computeGenValue(bc);
            if (genVal != null)
                out.put(genVal, bc.getDestRegister());
            outs.put(bc, out);
        }
        
        boolean update = true;
        while (update) {
            update = false;
            
            for (CFGNode node: cfg.getNodes()) {
                BCode bc = node.getBCode();
                AvailableVals out = outs.get(bc);
                // in = ¥cap pred.out
                AvailableVals in = new AvailableVals();
                boolean first = true;
                for (CFGNode pred: node.getPreds()) {
                    AvailableVals predOut = outs.get(pred.getBCode());
                    if (first) {
                        for (Value v: predOut.keySet())
                            in.put(v, new HashSet<Register>(predOut.get(v)));
                        first = false;
                        continue;
                    }
                    HashSet<Value> toRemove = new HashSet<Value>();
                    for (Value v: in.keySet()) {
                        Set<Register> rs = predOut.get(v);
                        if (rs == null)
                            toRemove.add(v);
                        else {
                            Set<Register> iRs = in.get(v);
                            iRs.retainAll(rs);
                            if (iRs.isEmpty())
                                toRemove.add(v);
                        }
                    }
                    for (Value v: toRemove)
                        in.remove(v);
                }
                ins.put(bc, new AvailableVals(in));
                
                // out += in - <?, destReg>
                Register dst = bc.getDestRegister();
                Value toRemove = null;
                if (dst != null)
                    for (Value v: in.keySet()) {
                        Set<Register> rs = in.get(v);
                        rs.remove(dst);
                        if (rs.isEmpty())
                            toRemove = v;
                    }
                if (toRemove != null)
                    in.remove(toRemove);
                if (out.addAll(in))
                    update = true;
            }
        }
    }
    
    public static Value computeGenValue(BCode bcx) {
        if (bcx instanceof IFixnum)
            return new FixnumValue(((IFixnum) bcx).n);
        else if (bcx instanceof INumber)
            return new NumberValue(((INumber) bcx).n);
        else if (bcx instanceof IString)
            return new StringValue(((IString) bcx).str);
        else if (bcx instanceof IBooleanconst)
            return new BooleanValue(((IBooleanconst) bcx).b);
        else if (bcx instanceof INullconst)
            return new NullValue();
        else if (bcx instanceof IUndefinedconst)
            return new UndefinedValue();
        else
            return null;
    }
    
    public Set<Register> getRegisterForValue(BCode bc, Value v) {
        AvailableVals out = ins.get(bc);
        return out.get(v);
    }
}
