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

import ejsc.CodeBuffer.SpecialValue;
import ejsc.OutputFileComposer.InstructionBinaryFormat;
import specfile.SpecFile;

public class OutputFileComposer {
    static abstract class InstructionBinaryFormat {
        static final int FIELD_VALUE_TRUE = 0x3;
        static final int FIELD_VALUE_FALSE = 0x1;
        static final int FIELD_VALUE_NULL = 0x0;
        static final int FIELD_VALUE_UNDEFINED = 0x2;

        static class Bit32 extends InstructionBinaryFormat {
            public Bit32(int ptagBits, int specPTag) {
                super(ptagBits, specPTag);
            }
            @Override public int instructionBytes() { return 4; }
            @Override public int opcodeBits()       { return 8; }
            @Override public int aBits()            { return 8; }
            @Override public int bBits()            { return 8; }
            @Override public int cBits()            { return 8; }
        }


        static class Bit64 extends InstructionBinaryFormat {
            public Bit64(int ptagBits, int specPTag) {
                super(ptagBits, specPTag);
            }
            @Override public int instructionBytes() { return 8; }
            @Override public int opcodeBits()       { return 16; }
            @Override public int aBits()            { return 16; }
            @Override public int bBits()            { return 16; }
            @Override public int cBits()            { return 16; }
        }

        int ptagBits;
        int specPTag;

        InstructionBinaryFormat(int ptagBits, int specPTag) {
            this.ptagBits = ptagBits;
            this.specPTag = specPTag;
        }

        abstract public int instructionBytes();
        abstract public int opcodeBits();
        abstract public int aBits();
        abstract public int bBits();
        abstract public int cBits();
        public int bbBits() {
            return bBits() + cBits();
        }
        public int opcodeOffset() {
            return aOffset() + aBits();
        }
        public int aOffset() {
            return bOffset() + bBits();
        }
        public int bOffset() {
            return cOffset() + cBits();
        }
        public int bbOffset() {
            return 0;
        }
        public int cOffset() {
            return 0;
        }
        public long opcodeMask() {
            return ((1L << opcodeBits()) - 1) << opcodeOffset();
        }
        public long aMask() {
            return ((1L << aBits()) - 1) << aOffset();
        }
        public long bMask() {
            return ((1L << bBits()) - 1) << bOffset();
        }
        public long bbMask() {
            return ((1L << bbBits()) - 1) << bbOffset();
        }
        public long cMask() {
            return ((1L << cBits()) - 1) << cOffset();
        }
        public int specialFieldValue(SpecialValue v) {
            switch (v) {
            case TRUE:
                return (FIELD_VALUE_TRUE << ptagBits) | specPTag;
            case FALSE:
                return (FIELD_VALUE_FALSE << ptagBits) | specPTag;
            case NULL:
                return (FIELD_VALUE_NULL << ptagBits) | specPTag;
            case UNDEFINED:
                return (FIELD_VALUE_UNDEFINED << ptagBits) | specPTag;
            default:
                throw new Error("Unknown special");
            }
        }
        protected boolean inFixnumRange(double x) {
            if (x == (double)(long) x) {
                long fixnumMax = (1L << (bbBits() - 1)) - 1;
                long fixnumMin = -fixnumMax - 1;
                return fixnumMin <= x && x <= fixnumMax;
            }
            return false;
        }
    }

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
    protected InstructionBinaryFormat format;
    OutputFileComposer(SpecFile spec, boolean insn32, boolean align32) {
        this.spec = spec;
        int ptagBits, specPTag;

        if (align32) {
            ptagBits = 2;
            specPTag = 1;
        } else {
            ptagBits = 3;
            specPTag = 6;
        }
        if (insn32)
            format = new InstructionBinaryFormat.Bit32(ptagBits, specPTag);
        else
            format = new InstructionBinaryFormat.Bit64(ptagBits, specPTag);
    }
}
