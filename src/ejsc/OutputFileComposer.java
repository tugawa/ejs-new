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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import specfile.SpecFile;

public class OutputFileComposer {
    class ConstantTable {
        int count = 0;
        Map<Object, Integer> table = new HashMap<Object, Integer>();
        List<Object> array = new ArrayList<Object>();

        private int doLookup(Object x) {
            if (table.containsKey(x))
                return table.get(x);
            table.put(x, count);
            array.add(x);
            return count++;
        }

        int lookup(double n) {
            return doLookup(n);
        }

        int lookup(String n) {
            return doLookup(n);
        }

        List<Object> getConstants() {
            return array;
        }

        int size() {
            return array.size();
        }
    }

    /**
     * Decorate instruction name with operand type names.  For example, add => addregfix.
     * If all operands are register operands, this function returns null,
     * so that the caller can determine if this instruction has at least one constant operand or not.
     * 
     * @param insnName  base instruction name
     * @param srcs      source operands
     * @return decorated instruction name. If all operands are register operands, null is returend.
     */
    static String decorateInsnName(String insnName, SrcOperand[] srcs) {
        String modifier = "";
        boolean hasConstantOperand = false;
        for (SrcOperand src: srcs) {
            if (src instanceof RegisterOperand)
                modifier += "reg";
            else {
                if (src instanceof FixnumOperand)
                    modifier += "fix";
                else if (src instanceof FlonumOperand)
                    modifier += "flo";
                else if (src instanceof StringOperand)
                    modifier += "str";
                else if (src instanceof SpecialOperand)
                    modifier += "spec";
                else
                    throw new Error("Unknown source operand");
                hasConstantOperand = true;
            }
        }
        if (hasConstantOperand)
            return insnName + modifier;
        return null;
    }

    protected SpecFile spec;
    OutputFileComposer(SpecFile spec) {
        this.spec = spec;
    }
}
