package ejsc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CBCode {
    static final int MAX_CHAR = 127;
    static final int MIN_CHAR = -128;

    int number;
    protected Argument store, load1, load2;

    ArrayList<CBCLabel> labels = new ArrayList<CBCLabel>();

    CBCode() {}

    CBCode(Argument store, Argument load1, Argument load2) {
        this.store = store;
        this.load1 = load1;
        this.load2 = load2;
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
    ICBCSuperInstruction(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSuperInstruction(Argument store, Argument load1, Argument load2, String name) {
        super(store, load1, load2);
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
    ICBCNop(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNop(Argument store, Argument load1) {
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
    ICBCAdd(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCAdd(IAdd bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "add";
    }
    public String toString() {
        return super.toString("add");
    }
}
class ICBCSub extends CBCode {
    ICBCSub(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSub(ISub bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "sub";
    }
    public String toString() {
        return super.toString("sub");
    }
}
class ICBCMul extends CBCode {
    ICBCMul(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCMul(IMul bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "mul";
    }
    public String toString() {
        return super.toString("mul");
    }
}
class ICBCDiv extends CBCode {
    ICBCDiv(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCDiv(IDiv bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "div";
    }
    public String toString() {
        return super.toString("div");
    }
}
class ICBCMod extends CBCode {
    ICBCMod(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCMod(IMod bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "mod";
    }
    public String toString() {
        return super.toString("mod");
    }
}
class ICBCBitor extends CBCode {
    ICBCBitor(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCBitor(IBitor bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "bitor";
    }
    public String toString() {
        return super.toString("bitor");
    }
}
class ICBCBitand extends CBCode {
    ICBCBitand(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCBitand(IBitand bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "bitand";
    }
    public String toString() {
        return super.toString("bitand");
    }
}
class ICBCLeftshift extends CBCode {
    ICBCLeftshift(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCLeftshift(ILeftshift bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "leftshift";
    }
    public String toString() {
        return super.toString("leftshift");
    }
}
class ICBCRightshift extends CBCode {
    ICBCRightshift(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCRightshift(IRightshift bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "rightshift";
    }
    public String toString() {
        return super.toString("rightshift");
    }
}
class ICBCUnsignedrightshift extends CBCode {
    ICBCUnsignedrightshift(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCUnsignedrightshift(IUnsignedrightshift bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
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
    ICBCEqual(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCEqual(IEqual bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "equal";
    }
    public String toString() {
        return super.toString("equal");
    }
}
class ICBCEq extends CBCode {
    ICBCEq(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCEq(IEq bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "eq";
    }
    public String toString() {
        return super.toString("eq");
    }
}
class ICBCLessthan extends CBCode {
    ICBCLessthan(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCLessthan(ILessthan bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "lessthan";
    }
    public String toString() {
        return super.toString("lessthan");
    }
}
class ICBCLessthanequal extends CBCode {
    ICBCLessthanequal(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCLessthanequal(ILessthanequal bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "lessthanequal";
    }
    public String toString() {
        return super.toString("lessthanequal");
    }
}


class ICBCNot extends CBCode {
    ICBCNot(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNot(INot bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src);
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
    ICBCGetglobalobj(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCGetglobalobj(IGetglobalobj bc) {
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
    ICBCNewargs(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNewargs() {
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
    ICBCNewframe(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNewframe(INewframe bc) {
        store = new ANone();
        load1 = new ALiteral(bc.len);
        load2 = new ALiteral(bc.status);
    }
    public String getInsnName() {
        return "newframe";
    }
    public String toString() {
        return super.toString("newframe");
    }
}
class ICBCMakeclosure extends CBCode {
    ICBCMakeclosure(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCMakeclosure(IMakeclosure bc) {
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
    ICBCRet(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCRet() {
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
    ICBCIsundef(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCIsundef(IIsundef bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src);
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
    ICBCIsobject(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCIsobject(IIsobject bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src);
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
    ICBCInstanceof(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCInstanceof(IInstanceof bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src1);
        load2 = new ARegister(bc.src2);
    }
    public String getInsnName() {
        return "instanceof";
    }
    public String toString() {
        return super.toString("instanceof");
    }
}
class ICBCCall extends CBCode {
    ICBCCall(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCCall(ICall bc) {
        store = new ANone();
        load1 = new ARegister(bc.callee);
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
    ICBCSend(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSend(ISend bc) {
        store = new ANone();
        load1 = new ARegister(bc.callee);
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
    ICBCNew(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNew(INew bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.constructor);
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
    ICBCNewsend(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNewsend(INewsend bc) {
        store = new ANone();
        load1 = new ARegister(bc.constructor);
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
    ICBCMakesimpleiterator(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCMakesimpleiterator(IMakesimpleiterator bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.obj);
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
    ICBCNextpropnameidx(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNextpropnameidx(INextpropnameidx bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.ite);
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
    ICBCGetglobal(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCGetglobal(IGetglobal bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.lit);
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
    ICBCSetglobal(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSetglobal(ISetglobal bc) {
        store = new ANone();
        load1 = new ARegister(bc.lit);
        load2 = new ARegister(bc.src);
    }
    public String getInsnName() {
        return "setglobal";
    }
    public String toString() {
        return super.toString("setglobal");
    }
}
class ICBCGetlocal extends CBCode {
    ICBCGetlocal(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCGetlocal(IGetlocal bc) {
        store = new ARegister(bc.dst);
        load1 = new ALiteral(bc.depth);
        load2 = new ALiteral(bc.n);
    }
    public String getInsnName() {
        return "getlocal";
    }
    public String toString() {
        return super.toString("getlocal");
    }
}
class ICBCSetlocal extends CBCode {
    ICBCSetlocal(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSetlocal(ISetlocal bc) {
        store = new ALiteral(bc.depth);
        load1 = new ALiteral(bc.n);
        load2 = new ARegister(bc.src);
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
    ICBCGetarg(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCGetarg(IGetarg bc) {
        store = new ARegister(bc.dst);
        load1 = new ALiteral(bc.depth);
        load2 = new ALiteral(bc.n);
    }
    public String getInsnName() {
        return "getarg";
    }
    public String toString() {
        return super.toString("getarg");
    }
}
class ICBCSetarg extends CBCode {
    ICBCSetarg(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSetarg(ISetarg bc) {
        store = new ALiteral(bc.depth);
        load1 = new ALiteral(bc.n);
        load2 = new ARegister(bc.src);
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
    ICBCGetprop(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCGetprop(IGetprop bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.obj);
        load2 = new ARegister(bc.prop);
    }
    public String getInsnName() {
        return "getprop";
    }
    public String toString() {
        return super.toString("getprop");
    }
}
class ICBCSetprop extends CBCode {
    ICBCSetprop(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSetprop(ISetprop bc) {
        store = new ARegister(bc.obj);
        load1 = new ARegister(bc.prop);
        load2 = new ARegister(bc.src);
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
    ICBCGeta(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCGeta(IGeta bc) {
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
    ICBCSeta(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSeta(ISeta bc) {
        store = new ANone();
        load1 = new ARegister(bc.src);
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
    ICBCJump(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    CBCLabel label;
    ICBCJump(IJump bc) {
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
    ICBCJumptrue(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    CBCLabel label;
    ICBCJumptrue(IJumptrue bc) {
        store = new ANone();
        load1 = new ARegister(bc.test);
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
    ICBCJumpfalse(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    CBCLabel label;
    ICBCJumpfalse(IJumpfalse bc) {
        store = new ANone();
        load1 = new ARegister(bc.test);
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
    ICBCThrow(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCThrow(IThrow bc) {
        store = new ANone();
        load1 = new ARegister(bc.reg);
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
    ICBCPushhandler(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    CBCLabel label;
    ICBCPushhandler(IPushhandler bc) {
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
    ICBCPophandler(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCPophandler() {
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
    ICBCLocalcall(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    CBCLabel label;
    ICBCLocalcall(ILocalcall bc) {
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
    ICBCLocalret(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCLocalret() {
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
    ICBCPoplocal(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCPoplocal() {
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
    ICBCSetfl(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCSetfl(ISetfl bc) {
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
        this.n = n;
    }
    public String toString() {
        return super.toString("funcLength", n);
    }
}
class ICBCCallentry extends CBCode {
    int n;
    ICBCCallentry(int n) {
            this.n = n;
    }
    public String toString() {
        return super.toString("callentry", n);
    }
}
class ICBCSendentry extends CBCode {
    int n;
    ICBCSendentry(int n) {
        this.n = n;
    }
    public String toString() {
        return super.toString("sendentry", n);
    }
}
class ICBCNumberOfLocals extends CBCode {
    int n;
    ICBCNumberOfLocals(int n) {
        this.n = n;
    }
    public String toString() {
        return super.toString("numberOfLocals", n);
    }
}
class ICBCNumberOfInstruction extends CBCode {
    int n;
    ICBCNumberOfInstruction(int n) {
        this.n = n;
    }
    public String toString() {
        return super.toString("numberOfInstruction", n);
    }
}
class ICBCNumberOfArgument extends CBCode {
    int n;
    ICBCNumberOfArgument(int n) {
        this.n = n;
    }
    public String toString() {
        return super.toString("numberOfArgument", n);
    }
}


class ICBCError extends CBCode {
    ICBCError(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCError(IError bc) {
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
    MCBCSetfl() {
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
        store = new ANone();
        load1 = new ANone();
        load2 = new ANone();
        this.receiver = bc.receiver;
        this.function = bc.function;
        this.args = bc.args;
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
