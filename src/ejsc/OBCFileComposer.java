package ejsc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ejsc.Main.Info;

public class OBCFileComposer {
    static final boolean BIG_ENDIAN        = true;
    
    static final int FIELD_VALUE_TRUE      = 0x1e;
    static final int FIELD_VALUE_FALSE     = 0x0e;
    static final int FIELD_VALUE_NULL      = 0x06;
    static final int FIELD_VALUE_UNDEFINED = 0x16;

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
    }

    static class OBCInstruction {
        static final int INSTRUCTION_BYTES = 8;
        static final int OPCODE_OFFSET = 16 * 3;
        static final int OPCODE_BITS   = 16;
        static final long OPCODE_MASK  = ((1L << OPCODE_BITS) - 1) << OPCODE_OFFSET;
        static final int A_OFFSET      = 16 * 2;
        static final int A_BITS        = 16;
        static final long A_MASK       = ((1L << A_BITS) - 1) << A_OFFSET;
        static final int B_OFFSET      = 16 * 1;
        static final int B_BITS        = 16;
        static final long B_MASK       = ((1L << B_BITS) - 1) << B_OFFSET;
        static final int BB_OFFSET     = 0;
        static final int BB_BITS       = 32;
        static final long BB_MASK      = ((1L << BB_BITS) - 1) << BB_OFFSET;
        static final int C_OFFSET      = 0;
        static final int C_BITS        = 16;
        static final long C_MASK       = ((1L << C_BITS) - 1) << C_OFFSET;
        
        enum Format {
            ABC,
            AB
        }

        static OBCInstruction createAB(String insnName, int opcode, int a, int b) {
            return new OBCInstruction(insnName, opcode, Format.AB, a, b, 0);
        }

        static OBCInstruction createABC(String insnName, int opcode, int a, int b, int c) {
            return new OBCInstruction(insnName, opcode, Format.ABC, a, b, c);
        }

        String insnName;  /* debug */
        int opcode;
        Format format;
        int a, b, c;

        OBCInstruction(String insnName, int opcode, Format format, int a, int b, int c) {
            this.insnName = insnName;
            this.opcode = opcode;
            this.format = format;
            this.a = a;
            this.b = b;
            this.c = c;
        }

        /**
         * Returns binary representation of the instruction.
         * @return binary representation of this instruction.
         */
        byte[] getBytes() {
            long insn = ((long) opcode) << OPCODE_OFFSET;
            switch (format) {
            case ABC:
                insn |= (((long) a) << A_OFFSET) & A_MASK;
                insn |= (((long) b) << B_OFFSET) & B_MASK;
                insn |= (((long) c) << C_OFFSET) & C_MASK;
                break;
            case AB:
                insn |= (((long) a) << A_OFFSET) & A_MASK;
                insn |= (((long) b) << BB_OFFSET) & BB_MASK;
                break;
            default:
                throw new Error("Unknown instruction format");    
            }
            if (BIG_ENDIAN)
                insn = Long.reverseBytes(insn);                
            byte[] bytes = new byte[INSTRUCTION_BYTES];
            for (int i = 0; i < INSTRUCTION_BYTES; i++)
                bytes[i] = (byte) (insn >> (8 * i));
            return bytes;
        }
    }
    
    class OBCFunction extends CodeBuffer {
        int functionNumberOffset;
        
        /* function header */
        int callEntry;
        int sendEntry;
        int numberOfLocals;
        
        ConstantTable constants;
        List<OBCInstruction> instructions;
        
        OBCFunction(BCBuilder.FunctionBCBuilder fb, int functionNumberOffset) {
            this.functionNumberOffset = functionNumberOffset;
            
            List<BCode> bcodes = fb.getInstructions();
            this.callEntry = fb.callEntry.dist(0);
            this.sendEntry = fb.sendEntry.dist(0);
            this.numberOfLocals = fb.numberOfLocals;
            
            constants = new ConstantTable();
            instructions = new ArrayList<OBCInstruction>(bcodes.size());
            for (BCode bc: bcodes)
               bc.emit(this);
        }

        int getOpcode(String insnName, SrcOperand... srcs) {
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
                        modifier += "spe";
                    else
                        throw new Error("Unknown source operand");
                    hasConstantOperand = true;
                }
            }
            if (hasConstantOperand)
                return Main.Info.SISpecInfo.getOpcodeIndex(insnName + modifier);
            else
                return Info.getOpcodeIndex(insnName);
        }
        
        int fieldBitsOf(SrcOperand src) {
            if (src instanceof RegisterOperand) {
                Register r = ((RegisterOperand) src).get();
                int n = r.getRegisterNumber();
                return n;
            } else if (src instanceof FixnumOperand) {
                int n = ((FixnumOperand) src).get();
                return n;
            } else if (src instanceof FlonumOperand) {
                double n = ((FlonumOperand) src).get();
                int index = constants.lookup(n);
                return index;
            } else if (src instanceof StringOperand) {
                String s = ((StringOperand) src).get();
                int index = constants.lookup(s);
                return index;
            } else if (src instanceof SpecialOperand) {
                SpecialOperand.V v = ((SpecialOperand) src).get();
                switch (v) {
                case TRUE:
                    return FIELD_VALUE_TRUE;
                case FALSE:
                    return FIELD_VALUE_FALSE;
                case NULL:
                    return FIELD_VALUE_NULL;
                case UNDEFINED:
                    return FIELD_VALUE_UNDEFINED;
                default:
                    throw new Error("Unknown special");
                }
            } else
                throw new Error("Unknown source operand");
        }
        
        @Override
        void addFixnumSmallPrimitive(String insnName, Register dst, int n) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            int b = n;
            OBCInstruction insn = OBCInstruction.createAB(insnName, opcode, a, b);
            instructions.add(insn);
        }
        @Override
        void addNumberBigPrimitive(String insnName, Register dst, double n) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            int b = constants.lookup(n);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, 0);
            instructions.add(insn);
            
        }
        @Override
        void addStringBigPrimitive(String insnName, Register dst, String s) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            int b = constants.lookup(s);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, 0);
            instructions.add(insn);
        }
        @Override
        void addSpecialSmallPrimitive(String insnName, Register dst, SpecialValue v) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            int b;
            switch (v) {
            case TRUE:
                b = FIELD_VALUE_TRUE; break;
            case FALSE:
                b = FIELD_VALUE_FALSE; break;
            case NULL:
                b = FIELD_VALUE_NULL; break;
            case UNDEFINED:
                b = FIELD_VALUE_UNDEFINED; break;
            default:
                throw new Error("Unknown special");
            }
            OBCInstruction insn = OBCInstruction.createAB(insnName, opcode, a, b);
            instructions.add(insn);
        }
        @Override
        void addRegexp(String insnName, Register dst, int flag, String ptn) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            int c = constants.lookup(ptn);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, flag, c);
            instructions.add(insn);
        }
        @Override
        void addRXXThreeOp(String insnName, Register dst, SrcOperand src1, SrcOperand src2) {
            int opcode = getOpcode(insnName, src1, src2);
            int a = dst.getRegisterNumber();
            int b = fieldBitsOf(src1);
            int c = fieldBitsOf(src2);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, c);
            instructions.add(insn);
        }
        @Override
        void addXXXThreeOp(String insnName, SrcOperand src1, SrcOperand src2, SrcOperand src3) {
            int opcode = getOpcode(insnName, src1, src2, src3);
            int a = fieldBitsOf(src1);
            int b = fieldBitsOf(src2);
            int c = fieldBitsOf(src3);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, c);
            instructions.add(insn);
        }
        @Override
        void addXIXThreeOp(String insnName, SrcOperand src1, int index, SrcOperand src2) {
            int opcode = getOpcode(insnName, src1, src2);
            int a = fieldBitsOf(src1);
            int c = fieldBitsOf(src2);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, index, c);
            instructions.add(insn);
        }
        @Override
        void addRXTwoOp(String insnName, Register dst, SrcOperand src) {
            int opcode = getOpcode(insnName, src);
            int a = dst.getRegisterNumber();
            int b = fieldBitsOf(src);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, 0);
            instructions.add(insn);
        }
        @Override
        void addXXTwoOp(String insnName, SrcOperand src1, SrcOperand src2) {
            int opcode = getOpcode(insnName, src1, src2);
            int a = fieldBitsOf(src1);
            int b = fieldBitsOf(src2);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, 0);
            instructions.add(insn);
        }
        @Override
        void addXRTwoOp(String insnName, SrcOperand src, Register dst) {
            int opcode = getOpcode(insnName, src);
            int a = fieldBitsOf(src);
            int b = dst.getRegisterNumber();
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, 0);
            instructions.add(insn);
        }
        @Override
        void addROneOp(String insnName, Register dst) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, 0, 0);
            instructions.add(insn);
        }
        @Override
        void addXOneOp(String insnName, SrcOperand src) {
            int opcode = getOpcode(insnName, src);
            int a = fieldBitsOf(src);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, 0, 0);
            instructions.add(insn);
        }
        @Override
        void addIOneOp(String insnName, int n) {
            int opcode = getOpcode(insnName);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, n, 0, 0);
            instructions.add(insn);
        }
        @Override
        void addZeroOp(String insnName) {
            int opcode = getOpcode(insnName);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, 0, 0, 0);
            instructions.add(insn);
        }
        @Override
        void addNewFrameOp(String insnName, int len, boolean mkargs) {
            int opcode = getOpcode(insnName);
            int b = mkargs ? 1 : 0;
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, len, b, 0);
            instructions.add(insn);
        }
        @Override
        void addGetVar(String insnName, Register dst, int link, int index) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, link, index);
            instructions.add(insn);
        }
        @Override
        void addSetVar(String insnName, int link, int index, SrcOperand src) {
            int opcode = getOpcode(insnName, src);
            int c = fieldBitsOf(src);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, link, index, c);
            instructions.add(insn);
        }
        @Override
        void addMakeClosureOp(String insnName, Register dst, int index) {
            int opcode = getOpcode(insnName);
            int a = dst.getRegisterNumber();
            int b = index + functionNumberOffset;
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, 0);
            instructions.add(insn);
        }
        @Override
        void addXICall(String insnName, SrcOperand fun, int nargs) {
            int opcode = getOpcode(insnName, fun);
            int a = fieldBitsOf(fun);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, nargs, 0);
            instructions.add(insn);                    
        }
        @Override
        void addRXCall(String insnName, Register dst, SrcOperand fun) {
            int opcode = getOpcode(insnName, fun);
            int a = dst.getRegisterNumber();
            int b = fieldBitsOf(fun);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, b, 0);
            instructions.add(insn);
        }
        @Override
        void addUncondJump(String insnName, int disp) {
            int opcode = getOpcode(insnName);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, disp, 0, 0);
            instructions.add(insn);
        }
        @Override
        void addCondJump(String insnName, SrcOperand test, int disp) {
            int opcode = getOpcode(insnName, test);
            int a = fieldBitsOf(test);
            OBCInstruction insn = OBCInstruction.createABC(insnName, opcode, a, disp,  0);
            instructions.add(insn);
        }
    }

    List<OBCFunction> obcFunctions;
    
    OBCFileComposer(BCBuilder compiledFunctions, int functionNumberOffset) {
        compiledFunctions.mergeTopLevel();
        compiledFunctions.assignFunctionIndex(true);
        List<BCBuilder.FunctionBCBuilder> fbs = compiledFunctions.getFunctionBCBuilders();
        obcFunctions = new ArrayList<OBCFunction>(fbs.size());
        for (BCBuilder.FunctionBCBuilder fb: fbs) {
            OBCFunction out = new OBCFunction(fb, functionNumberOffset);
            obcFunctions.add(out);
        }
    }
    
    private void outputShort(OutputStream out, int v) throws IOException {
        if (BIG_ENDIAN)
            v = Integer.reverseBytes(v << 16);
        out.write((byte)(v & 0xff));
        out.write((byte)((v >> 8) & 0xff));
    }
    
    private void outputLong(OutputStream out, long v) throws IOException {
        if (BIG_ENDIAN)
            v = Long.reverseBytes(v);
        for (int i = 0; i < 8; i++)
            out.write((byte) ((v >> (8 * i)) & 0xff));
    }
    
    /**
     * Output instruction to the file.
     * @param fileName file name to be output to.
     */
    void output(String fileName) {
        try {
            FileOutputStream out = new FileOutputStream(fileName);            
            
            /* File header */
            outputShort(out, obcFunctions.size());

            /* Function */
            for (OBCFunction fun: obcFunctions) {
                /* Function header */
                outputShort(out, fun.callEntry);
                outputShort(out, fun.sendEntry);
                outputShort(out, fun.numberOfLocals);
                outputShort(out, fun.instructions.size());
                
                /* Instructions */
                for (OBCInstruction insn: fun.instructions)
                    out.write(insn.getBytes());               
                
                /* Constant pool */
                for (Object v: fun.constants.getConstants()) {
                    if (v instanceof Double) {
                        long bits = Double.doubleToLongBits((Double) v);
                        outputShort(out, 8);  // size
                        outputLong(out, bits);
                    } else if (v instanceof String) {
                        String s = (String) v;
                        outputShort(out, s.length() + 1); // size
                        out.write(s.getBytes());
                        out.write('\0');
                    } else
                        throw new Error("Unknown constant");
                }
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
