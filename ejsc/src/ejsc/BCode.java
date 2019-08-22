/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

interface CodeBuffer {
    enum SpecialValue {
        TRUE,
        FALSE,
        NULL,
        UNDEFINED
    }
    // fixnum
    void addFixnumSmallPrimitive(String insnName, boolean log, Register dst, int n);
    // number
    void addNumberBigPrimitive(String insnName, boolean log, Register dst, double n);
    // string
    void addStringBigPrimitive(String insnName, boolean log, Register dst, String s);
    // special
    void addSpecialSmallPrimitive(String insnName, boolean log, Register dst, SpecialValue v);
    // regexp
    void addRegexp(String insnName, boolean log, Register dst, int flag, String ptn);
    // threeop
    void addRXXThreeOp(String insnName, boolean log, Register dst, SrcOperand src1, SrcOperand src2);
    // threeop (setprop)
    void addXXXThreeOp(String insnName, boolean log, SrcOperand src1, SrcOperand src2, SrcOperand src3);
    // threeop (setarray)
    void addXIXThreeOp(String insnName, boolean log, SrcOperand src1, int index, SrcOperand src2);
    // twoop
    void addRXTwoOp(String insnName, boolean log, Register dst, SrcOperand src);
    // twoop (setglobal)
    void addXXTwoOp(String insnName, boolean log, SrcOperand src1, SrcOperand src2);
    // oneop
    void addROneOp(String insnName, boolean log, Register dst);
    // oneop (seta, throw)
    void addXOneOp(String insnName, boolean log, SrcOperand src);
    // oneop (setfl)
    void addIOneOp(String insnName, boolean log, int n);
    // zeroop
    void addZeroOp(String insnName, boolean log);
    // newframe
    void addNewFrameOp(String insnName, boolean log, int len, boolean mkargs);
    // getvar
    void addGetVar(String insnName, boolean log, Register dst, int link, int index);
    // setvar
    void addSetVar(String insnName, boolean log, int link, int inex, SrcOperand src);
    // makeclosure
    void addMakeClosureOp(String insnName, boolean log, Register dst, int index);
    // call
    void addXICall(String insnName, boolean log, SrcOperand fun, int nargs);
    // call (new)
    void addRXCall(String insnName, boolean log, Register dst, SrcOperand fun);
    // uncondjump
    void addUncondJump(String insnName, boolean log, int disp);
    // condjump
    void addCondJump(String insnName, boolean log, SrcOperand test, int disp);
}

public abstract class BCode {
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
    String toString(String opcode, Register op1, int op2, String op3) {
        return opcode + logStr() + " " + op1 + " " + op2 + " " + op3;
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
        return "[reg "+x.toString()+"]";
    }
}

class FixnumOperand extends SrcOperand {
    int x;
    FixnumOperand(int x) {
        this.x = x;
    }
    int get() {
        return x;
    }
    @Override
    public String toString() {
        return "[fixnum "+String.valueOf(x)+"]";
    }
}

class FlonumOperand extends SrcOperand {
    double x;
    FlonumOperand(double x) {
        this.x = x;
    }
    double get() {
        return x;
    }
    @Override
    public String toString() {
        return "[flonum "+String.valueOf(x)+"]";
    }
}

class StringOperand extends SrcOperand {
    String x;
    StringOperand(String x) {
        this.x = x;
    }
    String get() {
        return x;
    }
    @Override
    public String toString() {
        return "[string "+x+"]";
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
    SpecialOperand(V x) {
        this.x = x;
    }
    V get() {
        return x;
    }
    @Override
    public String toString() {
        switch (x) {
        case TRUE:
            return "[special true]";
        case FALSE:
            return "[special false]";
        case NULL:
            return "[special null]";
        case UNDEFINED:
            return "[special undefined]";
        default:
            throw new Error("unknown special value");
        }
    }
}

/*
 * BCode
 */

/* SMALLPRIMITIVE */
class IFixnum extends BCode {
    int n;
    IFixnum(Register dst, int n) {
        super("fixnum", dst);
        this.n = n;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addFixnumSmallPrimitive(name, logging, dst, n);
    }
    public String toString() {
        return super.toString(name, dst, n);
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
        buf.addNumberBigPrimitive(name, logging, dst, n);
    }
    public String toString() {
        return super.toString(name, dst, n);
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
        buf.addStringBigPrimitive(name, logging, dst, str);
    }
    public String toString() {
        return super.toString(name, dst, "\"" + str + "\"");
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
        buf.addSpecialSmallPrimitive(name, logging, dst, b ? CodeBuffer.SpecialValue.TRUE : CodeBuffer.SpecialValue.FALSE);
    }
    public String toString() {
        return super.toString(name, dst, b ? "true" : "false");
    }
}
/* SMALLPRIMITIVE */
class INullconst extends BCode {
    INullconst(Register dst) {
        super("specconst", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSpecialSmallPrimitive(name, logging, dst, CodeBuffer.SpecialValue.NULL);
    }
    public String toString() {
        return super.toString(name, dst, "null");
    }
}
/* SMALLPRIMITIVE */
class IUndefinedconst extends BCode {
    IUndefinedconst(Register dst) {
        super("specconst", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSpecialSmallPrimitive(name, logging, dst, CodeBuffer.SpecialValue.UNDEFINED);
    }
    public String toString() {
        return super.toString(name, dst, "undefined");
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
        buf.addRegexp(name, logging, dst, idx, ptn);
    }
    public String toString() {
        return super.toString(name, dst, idx, "\"" + ptn + "\"");
    }
}
/* THREEOP */
class IAdd extends BCode {
    SrcOperand src1, src2;
    IAdd(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IAdd(Register dst, SrcOperand src1, SrcOperand src2) {
        super("add", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ISub extends BCode {
    SrcOperand src1, src2;
    ISub(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    ISub(Register dst, SrcOperand src1, SrcOperand src2) {
        super("sub", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IMul extends BCode {
    SrcOperand src1, src2;
    IMul(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IMul(Register dst, SrcOperand src1, SrcOperand src2) {
        super("mul", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IDiv extends BCode {
    SrcOperand src1, src2;
    IDiv(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IDiv(Register dst, SrcOperand src1, SrcOperand src2) {
        super("div", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IMod extends BCode {
    SrcOperand src1, src2;
    IMod(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IMod(Register dst, SrcOperand src1, SrcOperand src2) {
        super("mod", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IBitor extends BCode {
    SrcOperand src1, src2;
    IBitor(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IBitor(Register dst, SrcOperand src1, SrcOperand src2) {
        super("bitor", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IBitand extends BCode {
    SrcOperand src1, src2;
    IBitand(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IBitand(Register dst, SrcOperand src1, SrcOperand src2) {
        super("bitand", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ILeftshift extends BCode {
    SrcOperand src1, src2;
    ILeftshift(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    ILeftshift(Register dst, SrcOperand src1, SrcOperand src2) {
        super("leftshift", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IRightshift extends BCode {
    SrcOperand src1, src2;
    IRightshift(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IRightshift(Register dst, SrcOperand src1, SrcOperand src2) {
        super("rightshift", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IUnsignedrightshift extends BCode {
    SrcOperand src1, src2;
    IUnsignedrightshift(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IUnsignedrightshift(Register dst, SrcOperand src1, SrcOperand src2) {
        super("unsignedrightshift", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IEqual extends BCode {
    SrcOperand src1, src2;
    IEqual(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IEqual(Register dst, SrcOperand src1, SrcOperand src2) {
        super("equal", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class IEq extends BCode {
    SrcOperand src1, src2;
    IEq(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    IEq(Register dst, SrcOperand src1, SrcOperand src2) {
        super("eq", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ILessthan extends BCode {
    SrcOperand src1, src2;
    ILessthan(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    ILessthan(Register dst, SrcOperand src1, SrcOperand src2) {
        super("lessthan", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* THREEOP */
class ILessthanequal extends BCode {
    SrcOperand src1, src2;
    ILessthanequal(Register dst, Register src1, Register src2) {
        this(dst, new RegisterOperand(src1), new RegisterOperand(src2));
    }
    ILessthanequal(Register dst, SrcOperand src1, SrcOperand src2) {
        super("lessthanequal", dst);
        this.src1 = src1;
        this.src2 = src2;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
    }
}
/* TWOOP */
class INot extends BCode {
    SrcOperand src;
    INot(Register dst, Register src) {
        this(dst, new RegisterOperand(src));
    }
    INot(Register dst, SrcOperand src) {
        super("not", dst);
        this.src = src;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, logging, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
    }
}
/* ONEOP */
class IGetglobalobj extends BCode {
    IGetglobalobj(Register dst) {
        super("getglobalobj", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addROneOp(name, logging, dst);
    }
    public String toString() {
        return super.toString(name, dst);
    }
}
/* ZEROOP */
class INewargs extends BCode {
    INewargs() {
        super("newargs");
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name, logging);
    }
    public String toString() {
        return super.toString(name);
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
        buf.addNewFrameOp(name, logging, len, makeArguments);
    }
    public String toString() {
        return super.toString(name, len, makeArguments ? 1 : 0);
    }
}
/* TWOOP */
class IGetglobal extends BCode {
    SrcOperand varName;
    IGetglobal(Register dst, Register name) {
        this(dst, new RegisterOperand(name));
    }
    IGetglobal(Register dst, SrcOperand name) {
        super("getglobal", dst);
        this.varName = name;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, logging, dst, varName);
    }
    public String toString() {
        return super.toString(name, dst, varName);
    }
}
/* TWOOP */
class ISetglobal extends BCode {
    SrcOperand varName, src;
    ISetglobal(Register name, Register src) {
        this(new RegisterOperand(name), new RegisterOperand(src));
    }
    ISetglobal(SrcOperand name, SrcOperand src) {
        super("setglobal");
        this.varName = name;
        this.src = src;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXXTwoOp(name, logging, varName, src);
    }
    public String toString() {
        return super.toString(name, varName, src);
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
        buf.addGetVar(name, logging, dst, link, index);
    }
    public String toString() {
        return super.toString(name, dst, link, index);
    }
}
/* SETVAR */
class ISetlocal extends BCode {
    int link, index;
    SrcOperand src;
    ISetlocal(int link, int index, Register src) {
        this(link, index, new RegisterOperand(src));
    }
    ISetlocal(int link, int index, SrcOperand src) {
        super("setlocal");
        this.link = link;
        this.index = index;
        this.src = src;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSetVar(name, logging, link, index, src);
    }
    public String toString() {
        return super.toString(name, link, index, src);
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
        buf.addGetVar(name, logging, dst, link, index);
    }
    public String toString() {
        return super.toString(name, dst, link, index);
    }
}
/* SETVAR */
class ISetarg extends BCode {
    int link, index;
    SrcOperand src;
    ISetarg(int link, int index, Register src) {
        this(link, index, new RegisterOperand(src));
    }
    ISetarg(int link, int index, SrcOperand src) {
        super("setarg");
        this.link = link;
        this.index = index;
        this.src = src;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addSetVar(name, logging, link, index, src);
    }
    public String toString() {
        return super.toString(name, link, index, src);
    }
}
/* THREEOP */
class IGetprop extends BCode {
    SrcOperand obj, prop;
    IGetprop(Register dst, Register obj, Register prop) {
        this(dst, new RegisterOperand(obj), new RegisterOperand(prop));
    }
    IGetprop(Register dst, SrcOperand obj, SrcOperand prop) {
        super("getprop", dst);
        this.obj = obj;
        this.prop = prop;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXXThreeOp(name, logging, dst, obj, prop);
    }
    public String toString() {
        return super.toString("getprop", dst, obj, prop);
    }
}
/* SETPROP */
class ISetprop extends BCode {
    SrcOperand obj, prop, src;
    ISetprop(Register obj, Register prop, Register src) {
        this(new RegisterOperand(obj), new RegisterOperand(prop), new RegisterOperand(src));
    }
    ISetprop(SrcOperand obj, SrcOperand prop, SrcOperand src) {
        super("setprop");
        this.obj = obj;
        this.prop = prop;
        this.src = src;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXXXThreeOp(name, logging, obj, prop, src);
    }
    public String toString() {
        return super.toString("setprop", obj, prop, src);
    }
}
/* THREEOP */
class ISetarray extends BCode {
    SrcOperand ary;
    int n;
    SrcOperand src;
    ISetarray(Register ary, int n, Register src) {
        this(new RegisterOperand(ary), n, new RegisterOperand(src));
    }
    ISetarray(SrcOperand ary, int n, SrcOperand src) {
        super("setarray");
        this.ary = ary;
        this.n = n;
        this.src = src;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXIXThreeOp(name, logging, ary, n, src);
    }
    public String toString() {
        return super.toString(name, ary, n, src);
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
        buf.addMakeClosureOp(name, logging, dst, function.getIndex());
    }
    public String toString() {
        return super.toString(name, dst, function.getIndex());
    }
}
/* ONEOP */
class IGeta extends BCode {
    IGeta(Register dst) {
        super("geta", dst);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addROneOp(name, logging, dst);
    }
    public String toString() {
        return super.toString(name, dst);
    }
}
/* ONEOP */
class ISeta extends BCode {
    SrcOperand src;
    ISeta(Register src) {
        this(new RegisterOperand(src));
    }
    ISeta(SrcOperand src) {
        super("seta");
        this.src = src;
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addXOneOp(name, logging, src);
    }
    public String toString() {
        return super.toString(name, src);
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
        buf.addZeroOp(name, logging);
    }
    public String toString() {
        return super.toString(name);
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
        buf.addRXTwoOp(name, logging, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
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
        buf.addRXTwoOp(name, logging, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
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
        buf.addRXTwoOp(name, logging, dst, src);
    }
    public String toString() {
        return super.toString(name, dst, src);
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
        buf.addRXXThreeOp(name, logging, dst, src1, src2);
    }
    public String toString() {
        return super.toString(name, dst, src1, src2);
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
        buf.addXICall(name, logging, function, numOfArgs);
    }
    public String toString() {
        return super.toString(name, function, numOfArgs);
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
        buf.addXICall(name, logging, function, numOfArgs);
    }
    public String toString() {
        return super.toString(name, function, numOfArgs);
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
        buf.addRXCall(name, logging, dst, constructor);
    }
    public String toString() {
        return super.toString(name, dst, constructor);
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
        buf.addXICall(name, logging, constructor, numOfArgs);
    }
    public String toString() {
        return super.toString(name, constructor, numOfArgs);
    }
}
/* TWOOP */
class IMakeiterator extends BCode {
    SrcOperand obj;
    IMakeiterator(Register obj, Register dst) {
        super("makeiterator", dst);
        this.obj = new RegisterOperand(obj);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, logging, dst, obj);
    }
    public String toString() {
        return super.toString(name, dst, obj);
    }
}
/* TWOOP */
class INextpropnameidx extends BCode {
    SrcOperand ite;
    INextpropnameidx(Register ite, Register dst) {
        super("nextpropnameidx", dst);
        this.ite = new RegisterOperand(ite);
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addRXTwoOp(name, logging, dst, ite);
    }
    public String toString() {
        return super.toString(name, dst, ite);
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
        buf.addUncondJump(name, logging, label.dist(number));
    }
    public String toString() {
        return super.toString(name, label.dist(number));
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
        buf.addCondJump(name, logging, test, label.dist(number));
    }
    public String toString() {
        return super.toString(name, test, label.dist(number));
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
        buf.addCondJump(name, logging, test, label.dist(number));
    }
    public String toString() {
        return super.toString(name, test, label.dist(number));
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
        buf.addXOneOp(name, logging, reg);
    }
    public String toString() {
        return super.toString(name, reg);
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
        buf.addUncondJump(name, logging, label.dist(number));
    }
    public String toString() {
        return super.toString(name, label.dist(number));
    }
}
/* ZEROOP */
class IPophandler extends BCode {
    IPophandler() {
        super("pophandler");
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name, logging);
    }
    public String toString() {
        return super.toString(name);
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
        buf.addUncondJump(name, logging, label.dist(number));
    }
    public String toString() {
        return super.toString("localcall", label.dist(number));
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
        buf.addZeroOp(name, logging);
    }
    public String toString() {
        return super.toString(name);
    }
}
/* ZEROOP */
class IPoplocal extends BCode {
    IPoplocal() {
        super("poplocal");
    }
    @Override
    public void emit(CodeBuffer buf) {
        buf.addZeroOp(name, logging);
    }
    public String toString() {
        return super.toString("poplocal");
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
        buf.addIOneOp(name, logging, fl);
    }
    public String toString() {
        return super.toString(name, fl);
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
        buf.addStringBigPrimitive(name, logging, dst, str);
    }
    public String toString() {
        return super.toString(name, dst, str);
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
