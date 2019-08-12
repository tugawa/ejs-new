/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import specfile.SpecFile;


public class SBCFileComposer extends OutputFileComposer {
    static class SBCInstruction {
        String insnName;
        String[] ops;

        SBCInstruction(String insnName, String... ops) {
            this.insnName = insnName;
            this.ops = ops;
        }
    }

    class SBCFunction implements CodeBuffer {
        int functionNumberOffset;

        /* function header */
        int callEntry;
        int sendEntry;
        int numberOfLocals;

        ConstantTable constants;
        List<SBCInstruction> instructions;

        SBCFunction(BCBuilder.FunctionBCBuilder fb, int functionNumberOffset) {
            this.functionNumberOffset = functionNumberOffset;

            List<BCode> bcodes = fb.getInstructions();
            this.callEntry = fb.callEntry.dist(0);
            this.sendEntry = fb.sendEntry.dist(0);
            this.numberOfLocals = fb.numberOfLocals;

            constants = new ConstantTable();
            instructions = new ArrayList<SBCInstruction>(bcodes.size());
            for (BCode bc: bcodes)
                bc.emit(this);
        }

        String decorateInsnName(String insnName, boolean log, SrcOperand... srcs) {
            String decorated = SBCFileComposer.decorateInsnName(insnName, srcs);
            if (decorated != null)
                insnName = decorated;
            if (log)
                insnName += "_log";
            return insnName;
        }

        String escapeString(String s) {
            return "\""+s+"\""; // TODO: do escape
        }

        String formatConstant(int index, String constStr) {
            return "#"+index+"="+constStr;
        }

        String flonumConst(double n) {
            int index = constants.lookup(n);
            String constStr = Double.toString(n);
            return formatConstant(index, constStr);
        }

        String stringConst(String s) {
            int index = constants.lookup(s);
            String constStr = escapeString(s);
            return formatConstant(index, constStr);
        }

        String srcOperandField(SrcOperand src) {
            if (src instanceof RegisterOperand) {
                Register r = ((RegisterOperand) src).get();
                int n = r.getRegisterNumber();
                return Integer.toString(n);
            } else if (src instanceof FixnumOperand) {
                int n = ((FixnumOperand) src).get();
                return Integer.toString(n);
            } else if (src instanceof FlonumOperand) {
                double n = ((FlonumOperand) src).get();
                return flonumConst(n);
            } else if (src instanceof StringOperand) {
                String s = ((StringOperand) src).get();
                return stringConst(s);
            } else if (src instanceof SpecialOperand) {
                SpecialOperand.V v = ((SpecialOperand) src).get();
                switch (v) {
                case TRUE:
                    return "true";
                case FALSE:
                    return "false";
                case NULL:
                    return "null";
                case UNDEFINED:
                    return "undefined";
                default:
                    throw new Error("Unknown special");
                }
            } else
                throw new Error("Unknown source operand");
        }

        @Override
        public void addFixnumSmallPrimitive(String insnName, boolean log, Register dst, int n) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = Integer.toString(n);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addNumberBigPrimitive(String insnName, boolean log, Register dst, double n) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = flonumConst(n);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);

        }
        @Override
        public void addStringBigPrimitive(String insnName, boolean log, Register dst, String s) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = stringConst(s);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addSpecialSmallPrimitive(String insnName, boolean log, Register dst, SpecialValue v) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            String b;
            switch (v) {
            case TRUE:
                b = "true"; break;
            case FALSE:
                b = "false"; break;
            case NULL:
                b = "null"; break;
            case UNDEFINED:
                b = "undefined"; break;
            default:
                throw new Error("Unknown special");
            }
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addRegexp(String insnName, boolean log, Register dst, int flag, String ptn) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = Integer.toString(flag);
            String c = stringConst(ptn);
            SBCInstruction insn = new SBCInstruction(insnName, a, b, c);
            instructions.add(insn);
        }
        @Override
        public void addRXXThreeOp(String insnName, boolean log, Register dst, SrcOperand src1, SrcOperand src2) {
            insnName = decorateInsnName(insnName, log, src1, src2);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = srcOperandField(src1);
            String c = srcOperandField(src2);
            SBCInstruction insn = new SBCInstruction(insnName, a, b, c);
            instructions.add(insn);
        }
        @Override
        public void addXXXThreeOp(String insnName, boolean log, SrcOperand src1, SrcOperand src2, SrcOperand src3) {
            insnName = decorateInsnName(insnName, log, src1, src2, src3);
            String a = srcOperandField(src1);
            String b = srcOperandField(src2);
            String c = srcOperandField(src3);
            SBCInstruction insn = new SBCInstruction(insnName, a, b, c);
            instructions.add(insn);
        }
        @Override
        public void addXIXThreeOp(String insnName, boolean log, SrcOperand src1, int index, SrcOperand src2) {
            insnName = decorateInsnName(insnName, log, src1, src2);
            String a = srcOperandField(src1);
            String b = Integer.toString(index);
            String c = srcOperandField(src2);
            SBCInstruction insn = new SBCInstruction(insnName, a, b, c);
            instructions.add(insn);
        }
        @Override
        public void addRXTwoOp(String insnName, boolean log, Register dst, SrcOperand src) {
            insnName = decorateInsnName(insnName, log, src);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = srcOperandField(src);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addXXTwoOp(String insnName, boolean log, SrcOperand src1, SrcOperand src2) {
            insnName = decorateInsnName(insnName, log, src1, src2);
            String a = srcOperandField(src1);
            String b = srcOperandField(src2);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addROneOp(String insnName, boolean log, Register dst) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            SBCInstruction insn = new SBCInstruction(insnName, a);
            instructions.add(insn);
        }
        @Override
        public void addXOneOp(String insnName, boolean log, SrcOperand src) {
            insnName = decorateInsnName(insnName, log, src);
            String a = srcOperandField(src);
            SBCInstruction insn = new SBCInstruction(insnName, a);
            instructions.add(insn);
        }
        @Override
        public void addIOneOp(String insnName, boolean log, int n) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(n);
            SBCInstruction insn = new SBCInstruction(insnName, a);
            instructions.add(insn);
        }
        @Override
        public void addZeroOp(String insnName, boolean log) {
            insnName = decorateInsnName(insnName, log);
            SBCInstruction insn = new SBCInstruction(insnName);
            instructions.add(insn);
        }
        @Override
        public void addNewFrameOp(String insnName, boolean log, int len, boolean mkargs) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(len);
            String b = mkargs ? "1" : "0";
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addGetVar(String insnName, boolean log, Register dst, int link, int index) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = Integer.toString(link);
            String c = Integer.toString(index);
            SBCInstruction insn = new SBCInstruction(insnName, a, b, c);
            instructions.add(insn);
        }
        @Override
        public void addSetVar(String insnName, boolean log, int link, int index, SrcOperand src) {
            insnName = decorateInsnName(insnName, log, src);
            String a = Integer.toString(link);
            String b = Integer.toString(index);
            String c = srcOperandField(src);
            SBCInstruction insn = new SBCInstruction(insnName, a, b, c);
            instructions.add(insn);
        }
        @Override
        public void addMakeClosureOp(String insnName, boolean log, Register dst, int index) {
            insnName = decorateInsnName(insnName, log);
            String a = Integer.toString(dst.getRegisterNumber());
            // String b = Integer.toString(index + functionNumberOffset);
            String b = Integer.toString(index);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addXICall(String insnName, boolean log, SrcOperand fun, int nargs) {
            insnName = decorateInsnName(insnName, log, fun);
            String a = srcOperandField(fun);
            String b = Integer.toString(nargs);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);                    
        }
        @Override
        public void addRXCall(String insnName, boolean log, Register dst, SrcOperand fun) {
            insnName = decorateInsnName(insnName, log, fun);
            String a = Integer.toString(dst.getRegisterNumber());
            String b = srcOperandField(fun);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
        @Override
        public void addUncondJump(String insnName, boolean log, int disp) {
            insnName = decorateInsnName(insnName, log);
            String b = Integer.toString(disp);
            SBCInstruction insn = new SBCInstruction(insnName, b);
            instructions.add(insn);
        }
        @Override
        public void addCondJump(String insnName, boolean log, SrcOperand test, int disp) {
            insnName = decorateInsnName(insnName, log, test);
            String a = srcOperandField(test);
            String b = Integer.toString(disp);
            SBCInstruction insn = new SBCInstruction(insnName, a, b);
            instructions.add(insn);
        }
    }

    List<SBCFunction> obcFunctions;

    SBCFileComposer(BCBuilder compiledFunctions, int functionNumberOffset, SpecFile spec) {
        super(spec);
        List<BCBuilder.FunctionBCBuilder> fbs = compiledFunctions.getFunctionBCBuilders();
        obcFunctions = new ArrayList<SBCFunction>(fbs.size());
        for (BCBuilder.FunctionBCBuilder fb: fbs) {
            SBCFunction out = new SBCFunction(fb, functionNumberOffset);
            obcFunctions.add(out);
        }
    }

    /**
     * Output instruction to the file.
     * @param fileName file name to be output to.
     */
    void output(String fileName) {
        try {
            FileOutputStream outs = new FileOutputStream(fileName);
            PrintWriter out = new PrintWriter(outs);

            /* Specfile fingerprint */
            out.println("fingerprint "+spec.getFingerprint());

            /* File header */
            out.println("funcLength "+obcFunctions.size());

            /* Function */
            for (SBCFunction fun: obcFunctions) {
                /* Function header */
                out.println("callentry "+fun.callEntry);
                out.println("sendentry "+fun.sendEntry);
                out.println("numberOfLocals "+fun.numberOfLocals);
                out.println("numberOfInstructions "+fun.instructions.size());
                out.println("numberOfConstants "+fun.constants.size());

                /* Instructions */
                for (SBCInstruction insn: fun.instructions) {
                    out.print(insn.insnName);
                    for (String op: insn.ops)
                        out.print(" "+op);
                    out.println();
                }
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
