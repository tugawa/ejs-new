package ejsc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CBCode {
    static final int MAX_CHAR = 127;
    static final int MIN_CHAR = -128;

    int number;
    protected Argument store, load1, load2;
    BCode originalInsn;

    ArrayList<CBCLabel> labels = new ArrayList<CBCLabel>();

    CBCode(BCode originalInsn) {
        this.originalInsn = originalInsn;
    }

    CBCode(Argument store, Argument load1, Argument load2, BCode originalInsn) {
        this.store = store;
        this.load1 = load1;
        this.load2 = load2;
        this.originalInsn = originalInsn;
    }

    void addLabels(List<CBCLabel> labels) {
        for (CBCLabel l: labels) {
            l.replaceDestCBCode(this);
            this.labels.add(l);
        }
    }

    ArrayList<CBCLabel> getLabels() {
        return labels;
    }

    public boolean isFallThroughInstruction() {
        return true;
    }

    public CBCode getBranchTarget() {
        return null;
    }

    public Register getDestRegister() {
        if (!(store instanceof ARegister))
            return null;
        ARegister s = (ARegister) store;
        return s.r;
    }

    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        if (!(store instanceof ARegister))
            srcs.addAll(store.getSrcRegisters());
        srcs.addAll(load1.getSrcRegisters());
        srcs.addAll(load2.getSrcRegisters());
        return srcs;
    }

    int getArgsNum() {
        return store.byteLength + load1.byteLength + load2.byteLength;
    }

    String toStringArgs() {
        return store.argStr + "_" + load1.argStr + "_" + load2.argStr;
    }

    String toString(String op) {
        String s = op + " " + toStringArgs();
        if (store.byteLength != 0)
            s += " " + store.toString();
        if (load1.byteLength != 0)
            s += " " + load1.toString();
        if (load2.byteLength != 0)
            s += " " + load2.toString();
        return s;
    }

    String toString(String op, int n) {
        return op + " " + n;
    }

    public String getInsnName() {
        return null;
    }
}



class CBCLabel {
    private CBCode bcode;
    CBCLabel() {}
    CBCLabel(CBCode bcode) {
        this.bcode = bcode;
    }
    public int dist(int number) {
        return bcode.number - number;
    }
    public int dist(int number, int argoffset) {
        return bcode.number - number - (argoffset + 1);
    }
    public CBCode getDestCBCode() {
        return bcode;
    }
    public void replaceDestCBCode(CBCode bcode) {
        this.bcode = bcode;
    }
}



class Argument {
    int byteLength;
    String argStr;
    boolean isConstant;

    Argument(int byteLength, String argStr, boolean isConstant) {
        this.byteLength = byteLength;
        this.argStr = argStr;
        this.isConstant = isConstant;
    }

    public HashSet<Register> getSrcRegisters() {
        return new HashSet<Register>();
    }

    public String toString() {
        return "";
    }

    static Argument fromSrcOperand(SrcOperand opx) {
        if (opx instanceof RegisterOperand) {
            RegisterOperand op = (RegisterOperand) opx;
            return new ARegister(op.get());
        } else if (opx instanceof FixnumOperand) {
            FixnumOperand op = (FixnumOperand) opx;
            return new AFixnum(op.get());
        } else if (opx instanceof FlonumOperand) {
            FlonumOperand op = (FlonumOperand) opx;
            return new ANumber(op.get());
        } else if (opx instanceof StringOperand) {
            StringOperand op = (StringOperand) opx;
            return new AString(op.get());
        } else if (opx instanceof SpecialOperand) {
            SpecialOperand op = (SpecialOperand) opx;
            switch (op.get()) {
            case TRUE:
                return new ASpecial("true");
            case FALSE:
               return new ASpecial("false");
            case NULL:
                return new ASpecial("null");
            case UNDEFINED:
                return new ASpecial("undefined");
            default:
                throw new Error("Unknown special");
            }
        } else
            throw new Error("Unknown src operand");
    }
}
class ANone extends Argument {
    ANone() {
        super(0, "NONE", false);
    }
}
class ARegister extends Argument {
    Register r;
    ARegister(Register r) {
        super(2, "REGISTER", false);
        this.r = r;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> src = new HashSet<Register>();
        src.add(r);
        return src;
    }
    public String toString() {
        return r.toString();
    }
}
class ASpecial extends Argument {
    String s;
    ASpecial(String s) {
        super(1, "SPECCONST", true);
        this.s = s;
    }
    public String toString() {
        return s;
    }
}
class ALiteral extends Argument {
    int n;
    ALiteral(int n) {
        super(4, "LITERAL", true);
        this.n = n;
    }
    public int getN() {
        return n;
    }
    void replaceJumpDist(int n) {
        this.n = n;
    }
    public String toString() {
        return Integer.toString(n);
    }
}
class AFixnum extends Argument {
    int n;
    AFixnum(int n) {
        super(4, "FIXNUM", true);
        this.n = n;
    }
    public int getN() {
        return n;
    }
    public String toString() {
        return Integer.toString(n);
    }
}
class AString extends Argument {
    String s;
    AString(String s) {
        super(2, "STRING", true);
        this.s = s;
    }
    public String toString() {
        return "\"" + s + "\"";
    }
}
class ARegexp extends Argument {
    int idx;
    String ptn;
    ARegexp(int idx, String ptn) {
        super(2, "REGEXP", false);
        this.idx = idx;
        this.ptn = ptn;
    }
    public String toString() {
        return idx + " " + "\"" + ptn + "\"";
    }
}
class ANumber extends Argument {
    double n;
    ANumber(double n) {
        super(2, "NUMBER", true);
        this.n = n;
    }
    public String toString() {
        return Double.toString(n);
    }
}
/*
class AGlobal extends Argument {
    Register r;
    AGlobal(Register r) {
        super(2, "GLOBAL", false);
        this.r = r;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> src = new HashSet<Register>();
        src.add(r);
        return src;
    }
    public String toString() {
        return r.toString();
    }
}
class ALocal extends Argument {
    int link, idx;
    ALocal(int link, int idx) {
        super(4, "LOCAL", false);
        this.link = link;
        this.idx = idx;
    }
    public String toString() {
        return link + " " + idx;
    }
}
class AArgs extends Argument {
    int link, idx;
    AArgs(int link, int idx) {
        super(4, "ARGS", false);
        this.link = link;
        this.idx = idx;
    }
    public String toString() {
        return link + " " + idx;
    }
}
class AProp extends Argument {
    Register obj, prop;
    AProp(Register obj, Register prop) {
        super(4, "PROP", false);
        this.obj = obj;
        this.prop = prop;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        srcs.add(obj);
        srcs.add(prop);
        return srcs;
    }
    public String toString() {
        return obj.toString() + " " + prop.toString();
    }
}
class AAreg extends Argument {
    AAreg() {
        super(0, "A", false);
    }
}
class AGlobalVar extends Argument {
    String s;
    AGlobalVar(String s) {
        super(2, "GLOBALVAR", false);
        this.s = s;
    }
    public String toString() {
        return "\"" + s + "\"";
    }
}
*/



class ICBCSuperInstruction extends CBCode {
    String name;
    ICBCSuperInstruction(Argument store, Argument load1, Argument load2, String name, BCode originalInsn) {
        super(store, load1, load2, originalInsn);
        this.name = name;
    }
    public Register getDestRegister() {
        return null;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        srcs.addAll(store.getSrcRegisters());
        srcs.addAll(load1.getSrcRegisters());
        srcs.addAll(load2.getSrcRegisters());
        return srcs;
    }
    public String getInsnName() {
        return name;
    }
    public String toString() {
        return super.toString(name);
    }
}
class ICBCNop extends CBCode {
    ICBCNop(Argument store, Argument load1) {
        super(null);
        this.store = store;
        this.load1 = load1;
        this.load2 = new ANone();
    }
    public String getInsnName() {
        return "nop";
    }
    public String toString() {
        return super.toString("nop");
    }
}
class ICBCAdd extends CBCode {
    ICBCAdd(IAdd bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "add";
    }
    public String toString() {
        return super.toString("add");
    }
}
class ICBCSub extends CBCode {
    ICBCSub(ISub bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "sub";
    }
    public String toString() {
        return super.toString("sub");
    }
}
class ICBCMul extends CBCode {
    ICBCMul(IMul bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "mul";
    }
    public String toString() {
        return super.toString("mul");
    }
}
class ICBCDiv extends CBCode {
    ICBCDiv(IDiv bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "div";
    }
    public String toString() {
        return super.toString("div");
    }
}
class ICBCMod extends CBCode {
    ICBCMod(IMod bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "mod";
    }
    public String toString() {
        return super.toString("mod");
    }
}
class ICBCBitor extends CBCode {
    ICBCBitor(IBitor bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "bitor";
    }
    public String toString() {
        return super.toString("bitor");
    }
}
class ICBCBitand extends CBCode {
    ICBCBitand(IBitand bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "bitand";
    }
    public String toString() {
        return super.toString("bitand");
    }
}
class ICBCLeftshift extends CBCode {
    ICBCLeftshift(ILeftshift bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "leftshift";
    }
    public String toString() {
        return super.toString("leftshift");
    }
}
class ICBCRightshift extends CBCode {
    ICBCRightshift(IRightshift bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "rightshift";
    }
    public String toString() {
        return super.toString("rightshift");
    }
}
class ICBCUnsignedrightshift extends CBCode {
    ICBCUnsignedrightshift(IUnsignedrightshift bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "unsignedrightshift";
    }
    public String toString() {
        return super.toString("unsignedrightshift");
    }
}


// relation
class ICBCEqual extends CBCode {
    ICBCEqual(IEqual bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "equal";
    }
    public String toString() {
        return super.toString("equal");
    }
}
class ICBCEq extends CBCode {
    ICBCEq(IEq bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "eq";
    }
    public String toString() {
        return super.toString("eq");
    }
}
class ICBCLessthan extends CBCode {
    ICBCLessthan(ILessthan bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "lessthan";
    }
    public String toString() {
        return super.toString("lessthan");
    }
}
class ICBCLessthanequal extends CBCode {
    ICBCLessthanequal(ILessthanequal bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "lessthanequal";
    }
    public String toString() {
        return super.toString("lessthanequal");
    }
}


class ICBCNot extends CBCode {
    ICBCNot(INot bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "not";
    }
    public String toString() {
        return super.toString("not");
    }
}
class ICBCGetglobalobj extends CBCode {
    ICBCGetglobalobj(IGetglobalobj bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = new ANone();
        load2 = new ANone();
    }
    public String getInsnName() {
        return "getglobalobj";
    }
    public String toString() {
        return super.toString("getglobalobj");
    }
}
class ICBCNewargs extends CBCode {
    ICBCNewargs(INewargs bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
    }
    public String getInsnName() {
        return "newargs";
    }
    public String toString() {
        return super.toString("newargs");
    }
}
class ICBCNewframe extends CBCode {
    ICBCNewframe(INewframe bc) {
        super(bc);
        store = new ANone();
        load1 = new ALiteral(bc.len);
        load2 = new ALiteral(bc.makeArguments ? 1 : 0);
    }
    public String getInsnName() {
        return "newframe";
    }
    public String toString() {
        return super.toString("newframe");
    }
}
class ICBCMakeclosure extends CBCode {
    ICBCMakeclosure(IMakeclosure bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = new ALiteral(bc.function.getIndex());
        load2 = new ANone();
    }
    public String getInsnName() {
        return "makeclosure";
    }
    public String toString() {
        return super.toString("makeclosure");
    }
}
class ICBCRet extends CBCode {
    ICBCRet(IRet bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    public String getInsnName() {
        return "ret";
    }
    public String toString() {
        return super.toString("ret");
    }
}
class ICBCIsundef extends CBCode {
    ICBCIsundef(IIsundef bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "isundef";
    }
    public String toString() {
        return super.toString("isundef");
    }
}
class ICBCIsobject extends CBCode {
    ICBCIsobject(IIsobject bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "isobject";
    }
    public String toString() {
        return super.toString("isobject");
    }
}
class ICBCInstanceof extends CBCode {
    ICBCInstanceof(IInstanceof bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.src1);
        load2 = Argument.fromSrcOperand(bc.src2);
    }
    public String getInsnName() {
        return "instanceof";
    }
    public String toString() {
        return super.toString("instanceof");
    }
}
class ICBCCall extends CBCode {
    ICBCCall(ICall bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.function);
        load2 = new ALiteral(bc.numOfArgs);
    }
    public String getInsnName() {
        return "call";
    }
    public String toString() {
        return super.toString("call");
    }
}
class ICBCSend extends CBCode {
    ICBCSend(ISend bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.function);
        load2 = new ALiteral(bc.numOfArgs);
    }
    public String getInsnName() {
        return "send";
    }
    public String toString() {
        return super.toString("send");
    }
}
class ICBCNew extends CBCode {
    ICBCNew(INew bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.constructor);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "new";
    }
    public String toString() {
        return super.toString("new");
    }
}
class ICBCNewsend extends CBCode {
    ICBCNewsend(INewsend bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.constructor);
        load2 = new ALiteral(bc.numOfArgs);
    }
    public String getInsnName() {
        return "newsend";
    }
    public String toString() {
        return super.toString("newsend");
    }
}
class ICBCMakesimpleiterator extends CBCode {
    ICBCMakesimpleiterator(IMakesimpleiterator bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.obj);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "makesimpleiterator";
    }
    public String toString() {
        return super.toString("makesimpleiterator");
    }
}
class ICBCNextpropnameidx extends CBCode {
    ICBCNextpropnameidx(INextpropnameidx bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.ite);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "nextpropnameidx";
    }
    public String toString() {
        return super.toString("nextpropnameidx");
    }
}
class ICBCGetglobal extends CBCode {
    ICBCGetglobal(IGetglobal bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.varName);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "getglobal";
    }
    public String toString() {
        return super.toString("getglobal");
    }
}
class ICBCSetglobal extends CBCode {
    ICBCSetglobal(ISetglobal bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.varName);
        load2 = Argument.fromSrcOperand(bc.src);
    }
    public String getInsnName() {
        return "setglobal";
    }
    public String toString() {
        return super.toString("setglobal");
    }
}
class ICBCGetlocal extends CBCode {
    ICBCGetlocal(IGetlocal bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = new ALiteral(bc.link);
        load2 = new ALiteral(bc.index);
    }
    public String getInsnName() {
        return "getlocal";
    }
    public String toString() {
        return super.toString("getlocal");
    }
}
class ICBCSetlocal extends CBCode {
    ICBCSetlocal(ISetlocal bc) {
        super(bc);
        store = new ALiteral(bc.link);
        load1 = new ALiteral(bc.index);
        load2 = Argument.fromSrcOperand(bc.src);
    }
    public Register getDestRegister() {
        return null;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        srcs.addAll(store.getSrcRegisters());
        srcs.addAll(load1.getSrcRegisters());
        srcs.addAll(load2.getSrcRegisters());
        return srcs;
    }
    public String getInsnName() {
        return "setlocal";
    }
    public String toString() {
        return super.toString("setlocal");
    }
}
class ICBCGetarg extends CBCode {
    ICBCGetarg(IGetarg bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = new ALiteral(bc.link);
        load2 = new ALiteral(bc.index);
    }
    public String getInsnName() {
        return "getarg";
    }
    public String toString() {
        return super.toString("getarg");
    }
}
class ICBCSetarg extends CBCode {
    ICBCSetarg(ISetarg bc) {
        super(bc);
        store = new ALiteral(bc.link);
        load1 = new ALiteral(bc.index);
        load2 = Argument.fromSrcOperand(bc.src);
    }
    public Register getDestRegister() {
        return null;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        srcs.addAll(store.getSrcRegisters());
        srcs.addAll(load1.getSrcRegisters());
        srcs.addAll(load2.getSrcRegisters());
        return srcs;
    }
    public String getInsnName() {
        return "setarg";
    }
    public String toString() {
        return super.toString("setarg");
    }
}
class ICBCGetprop extends CBCode {
    ICBCGetprop(IGetprop bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = Argument.fromSrcOperand(bc.obj);
        load2 = Argument.fromSrcOperand(bc.prop);
    }
    public String getInsnName() {
        return "getprop";
    }
    public String toString() {
        return super.toString("getprop");
    }
}
class ICBCSetprop extends CBCode {
    ICBCSetprop(ISetprop bc) {
        super(bc);
        store = Argument.fromSrcOperand(bc.obj);
        load1 = Argument.fromSrcOperand(bc.prop);
        load2 = Argument.fromSrcOperand(bc.src);
    }
    public Register getDestRegister() {
        return null;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        srcs.addAll(store.getSrcRegisters());
        srcs.addAll(load1.getSrcRegisters());
        srcs.addAll(load2.getSrcRegisters());
        return srcs;
    }
    public String getInsnName() {
        return "setprop";
    }
    public String toString() {
        return super.toString("setprop");
    }
}
class ICBCGeta extends CBCode {
    ICBCGeta(IGeta bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = new ANone();
        load2 = new ANone();
    }
    public String getInsnName() {
        return "geta";
    }
    public String toString() {
        return super.toString("geta");
    }
}
class ICBCSeta extends CBCode {
    ICBCSeta(ISeta bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.src);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "seta";
    }
    public String toString() {
        return super.toString("seta");
    }
}


// Jump instructions
class ICBCJump extends CBCode {
    CBCLabel label;
    ICBCJump(IJump bc) {
        super(bc);
        store = new ANone();
        load1 = new ALiteral(0);
        load2 = new ANone();
        label = new CBCLabel();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    @Override
    public CBCode getBranchTarget() {
        return label.getDestCBCode();
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
    }
    public String getInsnName() {
        return "jump";
    }
    public String toString() {
        return super.toString("jump");
    }
}
class ICBCJumptrue extends CBCode {
    CBCLabel label;
    ICBCJumptrue(IJumptrue bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.test);
        load2 = new ALiteral(0);
        label = new CBCLabel();
    }
    @Override
    public CBCode getBranchTarget() {
        return label.getDestCBCode();
    }
    void resolveJumpDist() {
        ((ALiteral) load2).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
    }
    public String getInsnName() {
        return "jumptrue";
    }
    public String toString() {
        return super.toString("jumptrue");
    }
}
class ICBCJumpfalse extends CBCode {
    CBCLabel label;
    ICBCJumpfalse(IJumpfalse bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.test);
        load2 = new ALiteral(0);
        label = new CBCLabel();
    }
    @Override
    public CBCode getBranchTarget() {
        return label.getDestCBCode();
    }
    void resolveJumpDist() {
        ((ALiteral) load2).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
    }
    public String getInsnName() {
        return "jumpfalse";
    }
    public String toString() {
        return super.toString("jumpfalse");
    }
}


class ICBCThrow extends CBCode {
    ICBCThrow(IThrow bc) {
        super(bc);
        store = new ANone();
        load1 = Argument.fromSrcOperand(bc.reg);
        load2 = new ANone();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    public String getInsnName() {
        return "throw";
    }
    public String toString() {
        return super.toString("throw");
    }
}
class ICBCPushhandler extends CBCode {
    CBCLabel label;
    ICBCPushhandler(IPushhandler bc) {
        super(bc);
        store = new ANone();
        load1 = new ALiteral(0);
        load2 = new ANone();
        label = new CBCLabel();
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
    }
    public String getInsnName() {
        return "pushhandler";
    }
    public String toString() {
        return super.toString("pushhandler");
    }
}
class ICBCPophandler extends CBCode {
    ICBCPophandler(IPophandler bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
    }
    public String getInsnName() {
        return "pophandler";
    }
    public String toString() {
        return super.toString("pophandler");
    }
}
class ICBCLocalcall extends CBCode {
    CBCLabel label;
    ICBCLocalcall(ILocalcall bc) {
        super(bc);
        store = new ANone();
        load1 = new ALiteral(0);
        load2 = new ANone();
        label = new CBCLabel();
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
    }
    public String getInsnName() {
        return "localcall";
    }
    public String toString() {
        return super.toString("localcall");
    }
}
class ICBCLocalret extends CBCode {
    ICBCLocalret(ILocalret bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    public String getInsnName() {
        return "localret";
    }
    public String toString() {
        return super.toString("localret");
    }
}
class ICBCPoplocal extends CBCode {
    ICBCPoplocal(IPoplocal bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
    }
    public String getInsnName() {
        return "poplocal";
    }
    public String toString() {
        return super.toString("poplocal");
    }
}
class ICBCSetfl extends CBCode {
    ICBCSetfl(ISetfl bc) {
        super(bc);
        store = new ANone();
        load1 = new ALiteral(bc.fl);
        load2 = new ANone();
    }
    public String getInsnName() {
        return "setfl";
    }
    public String toString() {
        return super.toString("setfl");
    }
}


class ICBCFuncLength extends CBCode {
    int n;
    ICBCFuncLength(int n) {
        super(null);
        this.n = n;
    }
    public String toString() {
        return super.toString("funcLength", n);
    }
}
class ICBCCallentry extends CBCode {
    int n;
    ICBCCallentry(int n) {
        super(null);
        this.n = n;
    }
    public String toString() {
        return super.toString("callentry", n);
    }
}
class ICBCSendentry extends CBCode {
    int n;
    ICBCSendentry(int n) {
        super(null);
        this.n = n;
    }
    public String toString() {
        return super.toString("sendentry", n);
    }
}
class ICBCNumberOfLocals extends CBCode {
    int n;
    ICBCNumberOfLocals(int n) {
        super(null);
        this.n = n;
    }
    public String toString() {
        return super.toString("numberOfLocals", n);
    }
}
class ICBCNumberOfInstruction extends CBCode {
    int n;
    ICBCNumberOfInstruction(int n) {
        super(null);
        this.n = n;
    }
    public String toString() {
        return super.toString("numberOfInstruction", n);
    }
}
class ICBCNumberOfArgument extends CBCode {
    int n;
    ICBCNumberOfArgument(int n) {
        super(null);
        this.n = n;
    }
    public String toString() {
        return super.toString("numberOfArgument", n);
    }
}


class ICBCError extends CBCode {
    ICBCError(IError bc) {
        super(bc);
        store = new ARegister(bc.dst);
        load1 = new AString(bc.str);
        load2 = new ANone();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    public String getInsnName() {
        return "error";
    }
    public String toString() {
        return super.toString("error");
    }
}

class MCBCSetfl extends CBCode {
    MCBCSetfl(MSetfl bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
    }
    @Override
    public String toString() {
        return "@MACRO cbc setfl";
    }
}

class MCBCCall extends CBCode {
    Register receiver;
    Register function;
    Register[] args;
    boolean isNew;
    boolean isTail;
    MCBCCall(MCall bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
        if (bc.receiver != null) {
            if (bc.receiver instanceof RegisterOperand)
                this.receiver = ((RegisterOperand) bc.receiver).get();
            else
                throw new Error("internal error");
        }
        if (bc.function instanceof RegisterOperand)
            this.function = ((RegisterOperand) bc.function).get();
        else
            throw new Error("internal error");
        this.args = new Register[bc.args.length];
        for (int i = 0; i < bc.args.length; i++) {
            SrcOperand opx = bc.args[i];
            if (opx instanceof RegisterOperand)
                this.args[i] = ((RegisterOperand) opx).get();
        }
        this.isNew = bc.isNew;
        this.isTail = bc.isTail;
    }
    @Override
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        if (receiver != null)
            srcs.add(receiver);
        srcs.add(function);
        for (Register r: args)
            srcs.add(r);
        return srcs;
    }
    @Override
    public String toString() {
        String s ="@MACRO cbc ";

        if (isTail)
            s += "tail";
        if (isNew)
            s += "new " + receiver + " " + function;
        else if (receiver == null)
            s += "call " + function;
        else
            s += "send " + receiver + " " + function;
        for (Register r: args)
            s += " " + r;
        return s;
    }
}

class MCBCParameter extends CBCode {
    Register dst;
    MCBCParameter(MParameter bc) {
        super(bc);
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
        this.dst = bc.dst;
    }
    @Override
    public Register getDestRegister() {
        return dst;
    }
    @Override
    public String toString() {
        return "@MACRO cbc param";
    }
}
