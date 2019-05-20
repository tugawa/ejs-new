/*
   BCode.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Takafumi Kataoka, 2017-18
     Tomoharu Ugawa, 2017-18
     Hideya Iwasaki, 2017-18

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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ejsc.Main.Info;
import ejsc.Main.Info.SISpecInfo;
import ejsc.Main.Info.SISpecInfo.SISpec;

abstract class CodeBuffer {
    enum SpecialValue {
        TRUE,
        FALSE,
        NULL,
        UNDEFINED
    }
    // fixnum
    abstract void addFixnumSmallPrimitive(String opname, Register dst, int n);
    // number
    abstract void addNumberBigPrimitive(String opanme, Register dst, double n);
    // string
    abstract void addStringBigPrimitive(String opname, Register dst, String s);
    // special
    abstract void addSpecialSmallPrimitive(String opname, Register dst, SpecialValue v);
    // regexp
    abstract void addRegexp(String opname, Register dst, int flag, String ptn);
    // threeop
    abstract void addRXXThreeOp(String opname, Register dst, SrcOperand src1, SrcOperand src2);
    // threeop (setprop)
    abstract void addXXXThreeOp(String opname, SrcOperand src1, SrcOperand src2, SrcOperand src3);
    // threeop (setarray)
    abstract void addXIXThreeOp(String opname, SrcOperand src1, int index, SrcOperand src2);
    // twoop
    abstract void addRXTwoOp(String opname, Register dst, SrcOperand src);
    // twoop (setglobal)
    abstract void addXXTwoOp(String opname, SrcOperand src1, SrcOperand src2);
    // twoop (makesimpleiterator, getnextpropnameidx)
    abstract void addXRTwoOp(String opname, SrcOperand src, Register dst);
    // oneop
    abstract void addROneOp(String opname, Register dst);
    // oneop (seta, throw)
    abstract void addXOneOp(String opname, SrcOperand src);
    // oneop (setfl)
    abstract void addIOneOp(String opname, int n);
    // zeroop
    abstract void addZeroOp(String opname);
    // newframe
    abstract void addNewFrameOp(String opname, int len, boolean mkargs);
    // getvar
    abstract void addGetVar(String opname, Register dst, int link, int index);
    // setvar
    abstract void addSetVar(String opname, int link, int inex, SrcOperand src);
    // makeclosure
    abstract void addMakeClosureOp(String opname, Register dst, int index);
    // call
    abstract void addXICall(String opname, SrcOperand fun, int nargs);
    // call (new)
    abstract void addRXCall(String opname, Register dst, SrcOperand fun);
    // uncondjump
    abstract void addUncondJump(String opname, int disp);
    // condjump
    abstract void addCondJump(String opname, SrcOperand test, int disp);
}

public abstract class BCode {
    CodeMaker cm = new CodeMaker();
    String name;
    int number;
    protected Register dst;
    ArrayList<Label> labels = new ArrayList<Label>();
    boolean logging = false;

    BCode(String name) {
        this.name = name;
    }

    BCode(String name, Register dst) {
        this(name);
        this.dst = dst;
    }

    int getArgsNum() {
        return 0;
    }

    void addLabels(List<Label> labels) {
        for (Label l: labels) {
            l.replaceDestBCode(this);
            this.labels.add(l);
        }
    }

    ArrayList<Label> getLabels() {
        return labels;
    }

    public boolean isFallThroughInstruction() {
        return true;
    }

    public BCode getBranchTarget() {
        return null;
    }

    public Register getDestRegister() {
        return dst;
    }

    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        Class<? extends BCode> c = getClass();
        for (Field f: c.getDeclaredFields()) {
            if (f.getType() == SrcOperand.class) {
                try {
                    SrcOperand opx = (SrcOperand) f.get(this);
                    if (opx instanceof RegisterOperand)
                        srcs.add(((RegisterOperand) opx).get());
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        return srcs;
    }

    public void logInsn() {
        this.logging = true;
    }

    String logStr() {
        if (logging) { return "_log"; }
        else { return ""; }
    }

    abstract void emit(CodeBuffer out);

    String toString(String opcode) {
        return opcode + logStr();
    }
    String toString(String opcode, Register op1) {
        return opcode + logStr() + " " + op1;
    }
    String toString(String opcode, SrcOperand op1) {
        return opcode + logStr() + " " + op1;
    }
    String toString(String opcode, Register op1, SrcOperand op2) {
        return opcode + logStr() + " " + op1 + " " + op2;
    }
    String toString(String opcode, SrcOperand op1, Register op2) {
        return opcode + logStr() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, SrcOperand op2, SrcOperand op3) {
        return opcode + logStr() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, SrcOperand op1, SrcOperand op2, SrcOperand op3) {
        return opcode + logStr() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, SrcOperand src1, SrcOperand src2) {
        return opcode + logStr() + " " + src1 + " " + src2;
    }
    String toString(String opcode, Register op1, String op2) {
        return opcode + logStr() + " " + op1 + " " + op2;
    }
    String toString(String opcode, SrcOperand op1, int op2) {
        return opcode + logStr() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, int op2) {
        return opcode + logStr() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, double op2) {
        return opcode + logStr() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, int op2, int op3) {
        return opcode + logStr() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, SrcOperand op1, int op2, SrcOperand op3) {
        return opcode + logStr() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, int op1, int op2, SrcOperand op3) {
        return opcode + logStr() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, int op1) {
        return opcode + logStr() + " " + op1;
    }
    String toString(String opcode, int op1, int op2) {
        return opcode + logStr() + " " + op1 + " " + op2;
    }
    String toString(String opcode, String op1, String op2, String op3) {
        return opcode + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, int op1, Register op2) {
        return opcode + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, int op2, String op3) {
        return opcode + logStr() + " " + op1 + " " + op2 + " " + op3;
    }

    String toByteString() {
        throw new Error("toByteString has no override method");
    }
    String toByteString(String opcode) {
        return cm.makecode(opcode);
    }
    String toByteString(String opcode, Register op1) {
        return cm.makecode(opcode, op1.n);
    }
    String toByteString(String opcode, SrcOperand src) {
        if (src instanceof RegisterOperand) {
            int rs = ((RegisterOperand) src).x.getRegisterNumber();
            return cm.makecode(opcode, rs);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, Register dst, SrcOperand src) {
        if (src instanceof RegisterOperand) {
            int rd = dst.getRegisterNumber();
            int rs = ((RegisterOperand) src).x.getRegisterNumber();
            return cm.makecode(opcode, rd, rs);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, SrcOperand src, Register dst) {
        if (src instanceof RegisterOperand) {
            int rs = ((RegisterOperand) src).x.getRegisterNumber();
            int rd = dst.getRegisterNumber();
            return cm.makecode(opcode, rs, rd);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, SrcOperand src1, SrcOperand src2) {
        if (src1 instanceof RegisterOperand && src2 instanceof RegisterOperand) {
            int rs1 = ((RegisterOperand) src1).x.getRegisterNumber();
            int rs2 = ((RegisterOperand) src2).x.getRegisterNumber();
            return cm.makecode(opcode, rs1, rs2);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, Register dst, SrcOperand src1, SrcOperand src2) {
        if (src1 instanceof RegisterOperand && src2 instanceof RegisterOperand) {
            int rd = dst.getRegisterNumber();
            int rs1 = ((RegisterOperand) src1).x.getRegisterNumber();
            int rs2 = ((RegisterOperand) src2).x.getRegisterNumber();
            return cm.makecode(opcode, rd, rs1, rs2);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, SrcOperand src1, SrcOperand src2, SrcOperand src3) {
        if (src1 instanceof RegisterOperand && src2 instanceof RegisterOperand && src3 instanceof RegisterOperand) {
            int rs1 = ((RegisterOperand) src1).x.getRegisterNumber();
            int rs2 = ((RegisterOperand) src2).x.getRegisterNumber();
            int rs3 = ((RegisterOperand) src3).x.getRegisterNumber();
            return cm.makecode(opcode, rs1, rs2, rs3);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, Register op1, String op2) {
        return cm.makecode(opcode, op1.n, op2);
    }
    String toByteString(String opcode, SrcOperand src, int op2) {
        if (src instanceof RegisterOperand) {
            int rs = ((RegisterOperand) src).x.getRegisterNumber();
            return cm.makecode(opcode, rs, op2);
        } throw new Error("not implemented");
    }
    String toByteString(String opcode, Register op1, double op2) {
        return cm.makecode(opcode, op1.n, op2) ;
    }
    String toByteString(String opcode, Register op1, int op2) {
        return cm.makecode(opcode, op1.n, op2) ;
    }
    String toByteString(String opcode, Register op1, int op2, int op3) {
        return cm.makecode(opcode, op1.n, op2, op3);
    }
    String toByteString(String opcode, SrcOperand src1, int op2, SrcOperand src2) {
        if (src1 instanceof RegisterOperand && src2 instanceof RegisterOperand) {
            int rs1 = ((RegisterOperand) src1).x.getRegisterNumber();
            int rs2 = ((RegisterOperand) src2).x.getRegisterNumber();
            return cm.makecode(opcode, rs1, op2, rs2);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, int op1, int op2, SrcOperand src) {
        if (src instanceof RegisterOperand) {
            int rs = ((RegisterOperand) src).x.getRegisterNumber();
            return cm.makecode(opcode, op1, op2, rs);
        } else
            throw new Error("not implemented");
    }
    String toByteString(String opcode, int op1) {
        return cm.makecode(opcode, op1);
    }
    String toByteString(String opcode, int op1, int op2) {
        return cm.makecode(opcode, op1, op2);
    }
    String toByteString(String opcode, Register op1, int op2, String op3) {
        return cm.makecode(opcode, op1.n, op2);
    }
    String toByteString(String opcode, String op1, String op2, String op3) {
        return cm.makecode(opcode, op1, op2, op3);
    }

    class CodeMaker {
        public CodeMaker() {
        }

        public String makecode(int opcode, int op1, int op2, int op3) {
            return String.format("%04x%04x%04x%04x",
                    opcode, op1&0xffff, op2&0xffff, op3&0xffff);
        }

        public String makecode(String opcode) {
            return makecode(makeopcode(opcode), 0, 0, 0);
        }

        public String makecode(String opcode, int op1) {
            int c = makeopcode(opcode);
            if(c == -1)
                return String.format("%04x", op1);
            return makecode(c, op1, 0, 0);
        }
        public String makecode(String opcode, int op1, int op2) {
            if(opcode.contentEquals("fixnum"))
                return makecode(makeopcode(opcode), op1,
                        (op2 >> 16)&0xffff, (op2 & 0xffff));
            return makecode(makeopcode(opcode), op1, op2, 0);
        }
        public String makecode(String opcode, int op1, int op2, int op3) {
            return makecode(makeopcode(opcode), op1, op2, op3);
        }
        public String makecode(String opcode, String op1, String op2, String op3) {
            Main.Info.SISpecInfo.SISpec sispec = Main.Info.SISpecInfo.getSISpecBySIName(opcode);
            String opstring[] = {op1, op2, op3};
            String optype[] = {sispec.op0, sispec.op1, sispec.op2};
            String BigPrimitiveInfomation="";
            String LiteralBins[] = {"","",""};
            for(int i=0;i<3;i++) {
                switch(optype[i]) {
                case "string" :
                    BigPrimitiveInfomation += "2";
                    char[] name = opstring[i].toCharArray();
                    int[] bins = new int[name.length];
                    char tmp = '\0';
                    // System.out.println(op2);
                    for(int j=1;j<name.length-1;j++) {
                        if(j%2==0) bins[j/2-1] = tmp << 8 | name[j];
                        tmp = name[j];
                        if(j==name.length-2 && j%2==1) bins[(j-1)/2] = tmp << 8;
                    }
                    LiteralBins[i] = names(bins);
                    break;
                case "flonum" :
                    BigPrimitiveInfomation += "1";
                    LiteralBins[i] = nums(Double.parseDouble(opstring[i]));
                    break;
                default :
                    BigPrimitiveInfomation += "0";
                }
            }
            //System.out.println(LiteralBins[0] + ":" + LiteralBins[1] + ":" + LiteralBins[2]);
            return makecode(Main.Info.SISpecInfo.getOpcodeIndex(opcode),
                    makeoperand(op1, optype[0]),
                    makeoperand(op2, optype[1]),
                    makeoperand(op3, optype[2])) 
                    + BigPrimitiveInfomation
                    + LiteralBins[0] + LiteralBins[1] + LiteralBins[2];
        }

        public String makecode(String opcode, int op1, String op2) {
            int value = 0;
            if(op2.contentEquals("true") ||
                    op2.contentEquals("false") ||
                    op2.contentEquals("null") ||
                    op2.contentEquals("undefined")) {
                if(op2.contentEquals("true"))
                    value = 0x1e;
                else if(op2.contentEquals("false"))
                    value = 0x0e;
                else if(op2.contentEquals("null"))
                    value = 0x06;
                else if(op2.contentEquals("undefined"))
                    value = 0x16;
                return makecode(makeopcode(opcode), op1,
                        value >> 16, value & 0xffff);
            }
            int i;
            char[] name = op2.toCharArray();
            int[] bins = new int[name.length];
            char tmp = '\0';
            // System.out.println(op2);
            for(i=1;i<name.length-1;i++) {
                // System.out.println(i + ":" + name[i]);
                if(i%2==0) bins[i/2-1] = tmp << 8 | name[i];
                tmp = name[i];
                value++;
                if(i==name.length-2 && i%2==1) bins[(i-1)/2] = tmp << 8;
            }
            return makecode(makeopcode(opcode), op1,
                    value & 0xffff, 0) + "020" + names(bins);
        }

        String names(int[] bins) {
            int i;
            String str="";
            for(i=0;bins[i]!=0;i++) {
                if((bins[i] & 0xff)==0)
                    str = str + String.format("%02x", bins[i]>>8);
                else
                    str = str + String.format("%04x", bins[i]);
            }
            //System.out.println(str);
            return str;
        }

        String makecode(String opcode, int op1, double op2) {
            return makecode(makeopcode(opcode), op1,
                    8, 0) + "010" + nums(op2);
        }

        String nums(double op) {
            int i;
            double num = op;
            int sign = 0;
            int index = 0;
            double mant = 0;
            int amant[] = new int[52];
            String str="";

            // 指数部
            if(op<0) {
                sign = 1;
                num *= -1.0;
            }

            mant = num;
            if(num == 0.0) {
                return "1000000000000000";
            } else if(num>2.0) {
                while(mant>=2.0) {
                    // System.out.println(mant);
                    mant /= 2.0;
                    index += 1;
                }
            } else if(num<=1.0) {
                while(1.0>mant) {
                    mant *= 2.0;
                    index -= 1;
                }
            }
            // System.out.println(mant);

            double tmp = mant-1.0;
            for(i=0;i<52;i++) {
                // System.out.print(tmp+"/");
                tmp*=2.0;
                if((int)tmp==1) {
                    tmp-=1.0;
                    amant[i] = 1;
                } else {
                    amant[i] = 0;
                }
            }

            str += String.format("%03x", sign*2048 + index + 1023);
            for(i=0;i<13;i++) {
                int a = amant[i*4]*8 + amant[i*4+1]*4 + amant[i*4+2]*2 + amant[i*4+3];
                str += String.format("%x", a);
            }
            return str;
        }

        int makeopcode(String opcode) {
            int code = Info.getOpcodeIndex(opcode);
            return Info.getOpcodeIndex(opcode);
        }

        int makeoperand(String op, String type) {
            switch(type) {
            case "-":
            case "_":
            case "fixnum":
                return Integer.parseInt(op);
            case "special":
                switch(op) {
                case "true": return 0x1e;
                case "false": return 0x0e;
                case "null": return 0x06;
                case "undefined": return 0x16;
                }
            case "string": return (op.length()-2) & 0xffff;
            case "flonum": return 8;
            default:
                throw new Error("undefined type: " + type);
            }
        }
    }
}

class Register {
    int n;
    Register(int n) {
        this.n = n;
    }
    public int getRegisterNumber() {
        return n;
    }
    public String toString() {
        return Integer.toString(n);
    }
}

class Label {
    private BCode bcode;
    Label() {}
    Label(BCode bcode) {
        this.bcode = bcode;
    }
    public int dist(int number) {
        return bcode.number - number;
    }
    public int dist(int number, int argoffset) {
        return bcode.number - number - (argoffset + 1);
    }
    public BCode getDestBCode() {
        return bcode;
    }
    public void replaceDestBCode(BCode bcode) {
        this.bcode = bcode;
    }
}

/*
 * Normal src operand of an instruction.
 */
class SrcOperand {}

class RegisterOperand extends SrcOperand {
    Register x;
    RegisterOperand(Register x) {
        if (x == null)
            throw new Error("x == null");
        this.x = x;
    }
    Register get() {
        return x;
    }
    void set(Register x) {
        this.x = x;
    }
    @Override
    public String toString() {
        return x.toString();
    }
}

class FixnumOperand extends SrcOperand {
    int x;
    int get() {
        return x;
    }
}

class FlonumOperand extends SrcOperand {
    double x;
    double get() {
        return x;
    }
}

class StringOperand extends SrcOperand {
    String x;
    String get() {
        return x;
    }
}

class SpecialOperand extends SrcOperand {
    enum V {
        TRUE,
        FALSE,
        NULL,
        UNDEFINED
    }
    V x;
    V get() {
        return x;
    }
}

/*
 * BCode
 */

class ISuperInstruction extends BCode {
    String store, load1, load2;
    ISuperInstruction(String name, String store, String load1, String load2) {
        super(name);
        this.store = store;
        this.load1 = load1;
        this.load2 = load2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("to be removed");
    }
    public String toString() {
        return super.toString(name, store, load1, load2);
    }
    public String toByteString() {
        return super.toByteString(name, store, load1, load2);
    }
}

/* SMALLPRIMITIVE */
class IFixnum extends BCode {
    int n;
    IFixnum(Register dst, int n) {
        super("fixnum", dst);
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addFixnumSmallPrimitive(name, dst, n);
    }
    public String toString() {
        return super.toString(name, dst, n);
    }
    public String toByteString() {
        return super.toByteString(name, dst, n);
    }
}
/* BIGPRIMITIVE */
class INumber extends BCode {
    double n;
    INumber(Register dst, double n) {
        super("number", dst);
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        // TODO: check range of n
        buf.addNumberBigPrimitive(name, dst, n);
    }
    public String toString() {
        return super.toString(name, dst, n);
    }
    public String toByteString() {
        return super.toByteString(name, dst, n);
    }
}
/* BIGPRIMITIVE */
class IString extends BCode {
    String str;
    IString(Register dst, String str) {
        super("string", dst);
        // TODO: check string format.  Too many backslashes?
        this.str = str;
        this.str = this.str.replaceAll("\n", "\\\\n");
        this.str = this.str.replaceAll(" ", "\\\\s");
        this.str = this.str.replaceAll("\"", "\\\\\"");
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addStringBigPrimitive(name, dst, str);
    }
    public String toString() {
        return super.toString(name, dst, "\"" + str + "\"");
    }
    public String toByteString() {
        return super.toByteString(name, dst, "\"" + str + "\"");
    }
}
/* SMALLPRIMITIVE */
class IBooleanconst extends BCode {
    boolean b;
    IBooleanconst(Register dst, boolean b) {
        super("specconst", dst);
        this.b = b;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSpecialSmallPrimitive(name, dst, b ? CodeBuffer.SpecialValue.TRUE : CodeBuffer.SpecialValue.FALSE);
    }
    public String toString() {
        return super.toString(name, dst, b ? "true" : "false");
    }
    public String toByteString() {
        return super.toByteString(name, dst, b ? "true" : "false");
    }
}
/* SMALLPRIMITIVE */
class INullconst extends BCode {
    INullconst(Register dst) {
        super("specconst", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSpecialSmallPrimitive(name, dst, CodeBuffer.SpecialValue.NULL);
    }
    public String toString() {
        return super.toString(name, dst, "null");
    }
    public String toByteString() {
        return super.toByteString(name, dst, "null");
    }
}
/* SMALLPRIMITIVE */
class IUndefinedconst extends BCode {
    IUndefinedconst(Register dst) {
        super("specconst", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSpecialSmallPrimitive(name, dst, CodeBuffer.SpecialValue.UNDEFINED);
    }
    public String toString() {
        return super.toString(name, dst, "undefined");
    }
    public String toByteString() {
        return super.toByteString(name, dst, "undefined");
    }
}
/* REGEXPOP */
class IRegexp extends BCode {
    int idx;
    String ptn;
    IRegexp(Register dst, int idx, String ptn) {
        super("regexp", dst);
        this.idx = idx;
        this.ptn = ptn;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRegexp(name, dst, idx, ptn);
    }
    public String toString() {
        return super.toString(name, dst, idx, "\"" + ptn + "\"");
    }
    public String toByteString() {
        return super.toByteString(name, dst, idx, "\"" + ptn + "\"");
    }
}
/* THREEOP */
class IAdd extends BCode {
    SrcOperand src1, src2;
    IAdd(Register dst, Register src1, Register src2) {
        super("add", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ISub extends BCode {
    SrcOperand src1, src2;
    ISub(Register dst, Register src1, Register src2) {
        super("sub", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IMul extends BCode {
    SrcOperand src1, src2;
    IMul(Register dst, Register src1, Register src2) {
        super("mul", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IDiv extends BCode {
    SrcOperand src1, src2;
    IDiv(Register dst, Register src1, Register src2) {
        super("div", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IMod extends BCode {
    SrcOperand src1, src2;
    IMod(Register dst, Register src1, Register src2) {
        super("mod", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IBitor extends BCode {
    SrcOperand src1, src2;
    IBitor(Register dst, Register src1, Register src2) {
        super("bitor", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IBitand extends BCode {
    SrcOperand src1, src2;
    IBitand(Register dst, Register src1, Register src2) {
        super("bitand", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ILeftshift extends BCode {
    SrcOperand src1, src2;
    ILeftshift(Register dst, Register src1, Register src2) {
        super("leftshift", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IRightshift extends BCode {
    SrcOperand src1, src2;
    IRightshift(Register dst, Register src1, Register src2) {
        super("rightshift", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IUnsignedrightshift extends BCode {
    SrcOperand src1, src2;
    IUnsignedrightshift(Register dst, Register src1, Register src2) {
        super("unsignedrightshift", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IEqual extends BCode {
    SrcOperand src1, src2;
    IEqual(Register dst, Register src1, Register src2) {
        super("equal", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IEq extends BCode {
    SrcOperand src1, src2;
    IEq(Register dst, Register src1, Register src2) {
        super("eq", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ILessthan extends BCode {
    SrcOperand src1, src2;
    ILessthan(Register dst, Register src1, Register src2) {
        super("lessthan", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ILessthanequal extends BCode {
    SrcOperand src1, src2;
    ILessthanequal(Register dst, Register src1, Register src2) {
        super("lessthanequal", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* TWOOP */
class INot extends BCode {
    SrcOperand src;
    INot(Register dst, Register src) {
        super("not", dst);
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src);
    }
}
/* ONEOP */
class IGetglobalobj extends BCode {
    IGetglobalobj(Register dst) {
        super("getglobalobj", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addROneOp(name, dst);
    }
    public String toString() {
        return super.toString(name, dst);
    }
    public String toByteString() {
        return super.toByteString(name, dst);
    }
}
/* ZEROOP */
class INewargs extends BCode {
    INewargs() {
        super("newargs");
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name);
    }
    public String toString() {
        return super.toString(name);
    }
    public String toByteString() {
        return super.toByteString(name);
    }
}
/* NEWFRAMEOP */
class INewframe extends BCode {
    int len;
    boolean makeArguments;
    INewframe(int len, boolean makeArguments) {
        super("newframe");
        this.len = len;
        this.makeArguments = makeArguments;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addNewFrameOp(name, len, makeArguments);
    }
    public String toString() {
        return super.toString(name, len, makeArguments ? 1 : 0);
    }
    public String toByteString() {
        return super.toByteString(name, len, makeArguments ? 1 : 0);
    }
}
/* TWOOP */
class IGetglobal extends BCode {
    SrcOperand varName;
    IGetglobal(Register dst, Register name) {
        super("getglobal", dst);
        this.varName = new RegisterOperand(name);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, dst, varName);
    }
    public String toString() {
        return super.toString(name, dst, varName);
    }
    public String toByteString() {
        return super.toByteString(name, dst, varName);
    }
}
/* TWOOP */
class ISetglobal extends BCode {
    SrcOperand varName, src;
    ISetglobal(Register name, Register src) {
        super("setglobal");
        this.varName = new RegisterOperand(name);
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXXTwoOp(name, varName, src);
    }
    public String toString() {
        return super.toString("setglobal", varName, src);
    }
    public String toByteString() {
        return super.toByteString("setglobal", varName, src);
    }
}
/* GETVAR */
class IGetlocal extends BCode {
    int link, index;
    IGetlocal(Register dst, int depth, int index) {
        super("getlocal", dst);
        this.link = depth;
        this.index = index;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addGetVar(name, dst, link, index);
    }
    public String toString() {
        return super.toString("getlocal", dst, link, index);
    }
    public String toByteString() {
        return super.toByteString("getlocal", dst, link, index);
    }
}
/* SETVAR */
class ISetlocal extends BCode {
    int link, index;
    SrcOperand src;
    ISetlocal(int link, int index, Register src) {
        super("setlocal");
        this.link = link;
        this.index = index;
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSetVar(name, link, index, src);
    }
    public String toString() {
        return super.toString(name, link, index, src);
    }
    public String toByteString() {
        return super.toByteString(name, link, index, src);
    }
}
/* GETVAR */
class IGetarg extends BCode {
    int link, index;
    IGetarg(Register dst, int link, int index) {
        super("getarg", dst);
        this.link = link;
        this.index = index;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addGetVar(name, dst,  link, index);
    }
    public String toString() {
        return super.toString(name, dst, link, index);
    }
    public String toByteString() {
        return super.toByteString(name, dst, link, index);
    }
}
/* SETVAR */
class ISetarg extends BCode {
    int link, index;
    SrcOperand src;
    ISetarg(int link, int index, Register src) {
        super("setarg");
        this.link = link;
        this.index = index;
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSetVar(name, link, index, src);
    }
    public String toString() {
        return super.toString(name, link, index, src);
    }
    public String toByteString() {
        return super.toByteString(name, link, index, src);
    }
}
/* THREEOP */
class IGetprop extends BCode {
    SrcOperand obj, prop;
    IGetprop(Register dst, Register obj, Register prop) {
        super("getprop", dst);
        this.obj = new RegisterOperand(obj);
        this.prop = new RegisterOperand(prop);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, obj, prop);
    }
    public String toString() {
        return super.toString("getprop", dst, obj, prop);
    }
    public String toByteString() {
        return super.toByteString("getprop", dst, obj, prop);
    }
}
/* SETPROP */
class ISetprop extends BCode {
    SrcOperand obj, prop, src;
    ISetprop(Register obj, Register prop, Register src) {
        super("setprop");
        this.obj = new RegisterOperand(obj);
        this.prop = new RegisterOperand(prop);
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXXXThreeOp(name, obj, prop, src);
    }
    public String toString() {
        return super.toString("setprop", obj, prop, src);
    }
    public String toByteString() {
        return super.toByteString("setprop", obj, prop, src);
    }
}
/* THREEOP */
class ISetarray extends BCode {
    SrcOperand ary;
    int n;
    SrcOperand src;
    ISetarray(Register ary, int n, Register src) {
        super("setarray");
        this.ary = new RegisterOperand(ary);
        this.n = n;
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXIXThreeOp(name, ary, n, src);
    }
    public String toString() {
        return super.toString(name, ary, n, src);
    }
    public String toByteString() {
        return super.toByteString(name, ary, n, src);
    }
}
/* MAKECLOSUREOP */
class IMakeclosure extends BCode {
    BCBuilder.FunctionBCBuilder function;
    IMakeclosure(Register dst, BCBuilder.FunctionBCBuilder function) {
        super("makeclosure", dst);
        this.function = function;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addMakeClosureOp(name, dst, function.getIndex());
    }
    public String toString() {
        return super.toString(name, dst, function.getIndex());
    }
    public String toByteString() {
        return super.toByteString(name, dst, function.getIndex());
    }
}
/* ONEOP */
class IGeta extends BCode {
    IGeta(Register dst) {
        super("geta", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addROneOp(name, dst);
    }
    public String toString() {
        return super.toString(name, dst);
    }
    public String toByteString() {
        return super.toByteString(name, dst);
    }
}
/* ONEOP */
class ISeta extends BCode {
    SrcOperand src;
    ISeta(Register src) {
        super("seta");
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXOneOp(name, src);
    }
    public String toString() {
        return super.toString("seta", src);
    }
    public String toByteString() {
        return super.toByteString("seta", src);
    }
}
/* RET */
class IRet extends BCode {
    IRet() {
        super("ret");
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name);
    }
    public String toString() {
        return super.toString("ret");
    }
    public String toByteString() {
        return super.toByteString("ret");
    }
}
/* TWOOP */
class IMove extends BCode {
    SrcOperand src;
    IMove(Register dst, Register src) {
        super("move", dst);
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src);
    }
}
/* TWOOP */
class IIsundef extends BCode {
    SrcOperand src;
    IIsundef(Register dst, Register src) {
        super("isundef", dst);
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src);
    }
}
/* TWOOP */
class IIsobject extends BCode {
    SrcOperand src;
    IIsobject(Register dst, Register src) {
        super("isobject", dst);
        this.src = new RegisterOperand(src);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src);
    }
}
/* THREEOP */
class IInstanceof extends BCode {
    SrcOperand src1, src2;
    IInstanceof(Register dst, Register src1, Register src2) {
        super("instanceof", dst);
        this.src1 = new RegisterOperand(src1);
        this.src2 = new RegisterOperand(src2);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
    public String toByteString() {
        return super.toByteString(name, dst, src1, src2);
    }
}
/* CALLOP */
class ICall extends BCode {
    SrcOperand function;
    int numOfArgs;
    ICall(Register function, int numOfArgs) {
        super("call");
        this.function = new RegisterOperand(function);
        this.numOfArgs = numOfArgs;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXICall(name, function, numOfArgs);
    }
    public String toString() {
        return super.toString(name, function, numOfArgs);
    }
    public String toByteString() {
        return super.toByteString(name, function, numOfArgs);
    }
}
/* CALL */
class ISend extends BCode {
    SrcOperand function;
    int numOfArgs;
    ISend(Register function, int numOfArgs) {
        super("send");
        this.function = new RegisterOperand(function);
        this.numOfArgs = numOfArgs;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXICall(name, function, numOfArgs);
    }
    public String toString() {
        return super.toString(name, function, numOfArgs);
    }
    public String toByteString() {
        return super.toByteString(name, function, numOfArgs);
    }
}
/* CALL */
class INew extends BCode {
    SrcOperand constructor;
    INew(Register dst, Register constructor) {
        super("new", dst);
        this.constructor = new RegisterOperand(constructor);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXCall(name, dst, constructor);
    }
    public String toString() {
        return super.toString(name, dst, constructor);
    }
    public String toByteString() {
        return super.toByteString(name, dst, constructor);
    }
}
/* CALL */
class INewsend extends BCode {
    SrcOperand constructor;
    int numOfArgs;
    INewsend(Register constructor, int numOfArgs) {
        super("newsend");
        this.constructor = new RegisterOperand(constructor);
        this.numOfArgs = numOfArgs;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXICall(name, constructor, numOfArgs);
    }
    public String toString() {
        return super.toString(name, constructor, numOfArgs);
    }
    public String toByteString() {
        return super.toByteString(name, constructor, numOfArgs);
    }
}
/* TWOOP */
class IMakesimpleiterator extends BCode {
    SrcOperand obj;
    IMakesimpleiterator(Register obj, Register dst) {
        super("makesimpleiterator", dst);
        this.obj = new RegisterOperand(obj);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXRTwoOp(name, obj, dst);
    }
    public String toString() {
        return super.toString(name, obj, dst);
    }
    public String toByteString() {
        return super.toByteString(name, obj, dst);
    }
}
class INextpropnameidx extends BCode {
    SrcOperand ite;
    INextpropnameidx(Register ite, Register dst) {
        super("nextpropnameidx", dst);
        this.ite = new RegisterOperand(ite);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXRTwoOp(name, ite, dst);
    }
    public String toString() {
        return super.toString(name, ite, dst);
    }
    public String toByteString() {
        return super.toByteString(name, ite, dst);
    }
}
/* UNCONDJUMP */
class IJump extends BCode {
    Label label;
    IJump(Label label) {
        super("jump");
        this.label = label;
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    @Override
    public BCode getBranchTarget() {
        return label.getDestBCode();
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addUncondJump(name, label.dist(number));
    }
    public String toString() {
        return super.toString(name, label.dist(number));
    }
    public String toByteString() {
        return super.toByteString(name, label.dist(number));
    }
}
/* CONDJUMP */
class IJumptrue extends BCode {
    SrcOperand test;
    Label label;
    IJumptrue(Label label, Register test) {
        super("jumptrue");
        this.label = label;
        this.test = new RegisterOperand(test);
    }
    @Override
    public BCode getBranchTarget() {
        return label.getDestBCode();
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addCondJump(name, test, label.dist(number));
    }
    public String toString() {
        return super.toString(name, test, label.dist(number));
    }
    public String toByteString() {
        return super.toByteString(name, test, label.dist(number));
    }
}
/* CONDJUMP */
class IJumpfalse extends BCode {
    SrcOperand test;
    Label label;
    IJumpfalse(Label label, Register test) {
        super("jumpfalse");
        this.label = label;
        this.test = new RegisterOperand(test);
    }
    @Override
    public BCode getBranchTarget() {
        return label.getDestBCode();
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addCondJump(name, test, label.dist(number));
    }
    public String toString() {
        return super.toString(name, test, label.dist(number));
    }
    public String toByteString() {
        return super.toByteString(name, test, label.dist(number));
    }
}
/* ONEOP */
class IThrow extends BCode {
    SrcOperand reg;
    IThrow(Register reg) {
        super("throw");
        this.reg = new RegisterOperand(reg);
    }
    @Override
    public boolean isFallThroughInstruction()  {
        return false;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXOneOp(name, reg);
    }
    public String toString() {
        return super.toString(name, reg);
    }
    public String toByteString() {
        return super.toByteString(name, reg);
    }
}
/* UNCONDJUMP */
class IPushhandler extends BCode {
    Label label;
    IPushhandler(Label label) {
        super("pushhandler");
        this.label = label;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addUncondJump(name, label.dist(number));
    }
    public String toString() {
        return super.toString(name, label.dist(number));
    }
    public String toByteString() {
        return super.toByteString(name, label.dist(number));
    }
}
/* ZEROOP */
class IPophandler extends BCode {
    IPophandler() {
        super("pophandler");
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name);
    }
    public String toString() {
        return super.toString(name);
    }
    public String toByteString() {
        return super.toByteString(name);
    }
}
/* UNCONDJUMP */
class ILocalcall extends BCode {
    Label label;
    ILocalcall(Label label) {
        super("localcall");
        this.label = label;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addUncondJump(name, label.dist(number));
    }
    public String toString() {
        return super.toString("localcall", label.dist(number));
    }
    public String toByteString() {
        return super.toByteString("localcall", label.dist(number));
    }
}
/* ZEROOP */
class ILocalret extends BCode {
    ILocalret() {
        super("localret");
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name);
    }
    public String toString() {
        return super.toString(name);
    }
    public String toByteString() {
        return super.toByteString(name);
    }
}
/* ZEROOP */
class IPoplocal extends BCode {
    IPoplocal() {
        super("poplocal");
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name);
    }
    public String toString() {
        return super.toString("poplocal");
    }
    public String toByteString() {
        return super.toByteString("poplocal");
    }
}
/* ONEOP */
class ISetfl extends BCode {
    int fl;
    ISetfl(int fl) {
        super("setfl");
        this.fl = fl;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addIOneOp(name, fl);
    }
    public String toString() {
        return super.toString(name, fl);
    }
    public String toByteString() {
        return super.toByteString(name, fl);
    }
}


class IFuncLength extends BCode {
    int n;
    IFuncLength(int n) {
        super("funcLength");
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("to be removed");
    }
    public String toString() {
        return super.toString(name, n);
    }
    public String toByteString() {
        return super.toByteString(name, n);
    }
}
class ICallentry extends BCode {
    int n;
    ICallentry(int n) {
        super("callentry");
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("to be removed");
    }
    public String toString() {
        return super.toString(name, n);
    }
    public String toByteString() {
        return super.toByteString(name, n);
    }
}
class ISendentry extends BCode {
    int n;
    ISendentry(int n) {
        super("sendentry");
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("to be removed");
    }
    public String toString() {
        return super.toString(name, n);
    }
    public String toByteString() {
        return super.toByteString(name, n);
    }
}
class INumberOfLocals extends BCode {
    int n;
    INumberOfLocals(int n) {
        super("numberOfLocals");
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("to be removed");
    }
    public String toString() {
        return super.toString(name, n);
    }
    public String toByteString() {
        return super.toByteString(name, n);
    }
}
class INumberOfInstruction extends BCode {
    int n;
    INumberOfInstruction(int n) {
        super("numberOfInstruction");
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("to be removed");
    }
    public String toString() {
        return super.toString(name, n);
    }
    public String toByteString() {
        return super.toByteString(name, n);
    }
}
/* BIGPRIMITIVE */
class IError extends BCode {
    String str;
    IError(Register dst, String str) {
        super("error", dst);
        this.str = str;
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addStringBigPrimitive(name, dst, str);
    }
    public String toString() {
        return super.toString(name, dst, str);
    }
    public String toByteString() {
        return super.toByteString(name, dst, str);
    }
}
/* macro instruction */
class MSetfl extends BCode {
    MSetfl() {
        super("@Msetfl");
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("attempt to emit a macro instruction MSetfl");
    }
    @Override
    public String toString() {
        return "@MACRO setfl";
    }
}

class MCall extends BCode {
    SrcOperand receiver;
    SrcOperand function;
    SrcOperand[] args;
    boolean isNew;
    boolean isTail;
    MCall(Register receiver, Register function, Register[] args, boolean isNew, boolean isTail) {
        super("@Mcall");
        this.receiver = receiver == null ? null : new RegisterOperand(receiver);
        this.function = new RegisterOperand(function);
        this.args = new SrcOperand[args.length];
        for (int i = 0; i < args.length; i++)
            this.args[i] = new RegisterOperand(args[i]);
        this.isNew = isNew;
        this.isTail = isTail;
    }
    @Override
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        if (receiver != null && receiver instanceof RegisterOperand)
            srcs.add(((RegisterOperand) receiver).get());
        if (function instanceof RegisterOperand)
            srcs.add(((RegisterOperand) function).get());
        for (SrcOperand opx: args) {
            if (opx instanceof RegisterOperand)
                srcs.add(((RegisterOperand) opx).get());
        }
        return srcs;
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("attempt to emit a macro instruction MSetfl");
    }
    @Override
    public String toString() {
        String s ="@MACRO ";

        if (isTail)
            s += "tail";
        if (isNew)
            s += "new " + receiver + " " + function;
        else if (receiver == null)
            s += "call " + function;
        else
            s += "send " + receiver + " " + function;
        for (SrcOperand opx: args)
            s += " " + opx;
        return s;
    }
}

class MParameter extends BCode {
    MParameter(Register dst) {
        super("@Mparameter", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        throw new Error("attempt to emit a macro instruction MSetfl");
    }
    @Override
    public String toString() {
        return "@MACRO param "+dst;
    }
}
