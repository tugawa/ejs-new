package ejsc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CBCode {
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

    int getArgsNum() {
        return this.store.getArgNum() + this.load1.getArgNum() + this.load2.getArgNum();
    }

    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        srcs.addAll(store.getSrcRegisters());
        srcs.addAll(load1.getSrcRegisters());
        srcs.addAll(load2.getSrcRegisters());
        return srcs;
    }

    String toStringArgs() {
        return this.store.type.name() + "_" + this.load1.type.name() + "_" + this.load2.type.name();
    }

    String toString(String op) {
        String s = op + " " + toStringArgs();
        if (store.getArgNum() != 0)
            s += " " + store.toString();
        if (load1.getArgNum() != 0)
            s += " " + load1.toString();
        if (load2.getArgNum() != 0)
            s += " " + load2.toString();
        return s;
    }

    String toString(String op, int n) {
        return op + " " + n;
    }

    public Register getStoreRegister() {
        if (store.type == Argument.ArgType.REGISTER)
            return ((ARegister) store).reg;
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
    static enum ArgType {
        NONE, REGISTER, LITERAL, SHORTLITERAL,
        GLOBAL, LOCAL, PROP, ARGS, ARRAY, A,
        SPECCONST, NUMBER, STRING, REGEXP,
        SHORTFIXNUM, FIXNUM;
        public int getArgNum() {
            switch(this) {
            case NONE: case A:
                return 0;
            case SHORTLITERAL: case SPECCONST:
            case SHORTFIXNUM:
                return 1;
            case REGISTER: case GLOBAL:
            case NUMBER: case STRING: case REGEXP:
                return 2;
            case LITERAL: case FIXNUM: case LOCAL:
            case PROP: case ARGS: case ARRAY:
                return 4;
            default:
                throw new Error("undefined enum type: " + this.name());
            }
        }
    }

    static final int CHAR_MAX = 127;
    static final int CHAR_MIN = -128;

    ArgType type;

    Argument() {
        this.type = ArgType.NONE;
    }

    Argument(ArgType type) {
        this.type = type;
    }

    int getArgNum() {
        return type.getArgNum();
    }

    boolean isShortRange(int n) {
        return (n >= CHAR_MIN && n <= CHAR_MAX);
    }

    public String toString() {
        return "";
    }
    public HashSet<Register> getSrcRegisters() {
        return new HashSet<Register>();
    }

    public boolean isConstant() {
        switch(type) {
        case SPECCONST: case SHORTFIXNUM:
        case NUMBER: case FIXNUM:
            return true;
        default:
            return false;
        }
    }
}

class ALiteral extends Argument {
    int n;
    ALiteral(int n) {
        super(ArgType.LITERAL);
        if (isShortRange(n))
            type = ArgType.SHORTLITERAL;
        this.n = n;
    }

    // Use to jump label
    ALiteral() {
        super(ArgType.LITERAL);
        this.n = 0;
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
        super(ArgType.FIXNUM);
        if (isShortRange(n))
            type = ArgType.SHORTFIXNUM;
        this.n = n;
    }

    public String toString() {
        return Integer.toString(n);
    }
}
class AString extends Argument {
    String s;
    AString(String s) {
        super(ArgType.STRING);
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
        super(ArgType.REGEXP);
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
        super(ArgType.NUMBER);
        this.n = n;
    }
    public String toString() {
        return Double.toString(n);
    }
}
class ASpecial extends Argument {
    String s;
    ASpecial(String s) {
        super(ArgType.SPECCONST);
        this.s = s;
    }
    public String toString() {
        return s;
    }
}
class ARegister extends Argument {
    Register reg;
    ARegister(Register reg) {
        super(ArgType.REGISTER);
        this.reg = reg;
    }
    ARegister(Register reg, ArgType type) {
        super(type);
        this.reg = reg;
    }
    public String toString() {
        return reg.toString();
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> src = new HashSet<Register>();
        src.add(reg);
        return src;
    }
}
class ARegPair extends Argument {
    Register reg1, reg2;
    ARegPair(Register reg1, Register reg2, ArgType type) {
        super(type);
        this.reg1 = reg1;
        this.reg2 = reg2;
    }
    public String toString() {
        return reg1 + " " + reg2;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> srcs = new HashSet<Register>();
        srcs.add(reg1);
        srcs.add(reg2);
        return srcs;
    }
}
class ALitPair extends Argument {
    int lit1, lit2;
    ALitPair(int lit1, int lit2, ArgType type) {
        super(type);
        this.lit1 = lit1;
        this.lit2 = lit2;
    }
    public String toString() {
        return lit1 + " " + lit2;
    }
}
class ARegLitPair extends Argument {
    Register reg;
    int lit;
    ARegLitPair(Register reg, int lit, ArgType type) {
        super(type);
        this.reg = reg;
        this.lit = lit;
    }
    public String toString() {
        return reg + " " + lit;
    }
    public HashSet<Register> getSrcRegisters() {
        HashSet<Register> src = new HashSet<Register>();
        src.add(reg);
        return src;
    }
}

class ICBCNop extends CBCode {
    ICBCNop(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCNop(Argument store, Argument load1) {
        this.store = store;
        this.load1 = load1;
        this.load2 = new Argument();
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
        load2 = new Argument();
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
        load1 = new Argument();
        load2 = new Argument();
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
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
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
        store = new Argument();
        load1 = new ALiteral(bc.len);
        load2 = new ALiteral(bc.status);
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
        load1 = new ALiteral(bc.idx);
        load2 = new Argument();
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
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
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
        load2 = new Argument();
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
        load2 = new Argument();
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
    public String toString() {
        return super.toString("instanceof");
    }
}
class ICBCCall extends CBCode {
    ICBCCall(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    ICBCCall(ICall bc) {
        store = new Argument();
        load1 = new ARegister(bc.callee);
        load2 = new ALiteral(bc.numOfArgs);
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
        store = new Argument();
        load1 = new ARegister(bc.callee);
        load2 = new ALiteral(bc.numOfArgs);
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
        load2 = new Argument();
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
        store = new Argument();
        load1 = new ARegister(bc.constructor);
        load2 = new ALiteral(bc.numOfArgs);
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
        load2 = new Argument();
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
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("nextpropnameidx");
    }
}


// Jump instructions
class ICBCJump extends CBCode {
    ICBCJump(Argument store, Argument load1, Argument load2) {
        super(store, load1, load2);
    }
    CBCLabel label;
    ICBCJump(IJump bc) {
        store = new Argument();
        load1 = new ALiteral();
        load2 = new Argument();
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
        store = new Argument();
        load1 = new ARegister(bc.test);
        load2 = new ALiteral();
        label = new CBCLabel();
    }
    @Override
    public CBCode getBranchTarget() {
        return label.getDestCBCode();
    }
    void resolveJumpDist() {
        ((ALiteral) load2).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
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
        store = new Argument();
        load1 = new ARegister(bc.test);
        load2 = new ALiteral();
        label = new CBCLabel();
    }
    @Override
    public CBCode getBranchTarget() {
        return label.getDestCBCode();
    }
    void resolveJumpDist() {
        ((ALiteral) load2).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
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
        store = new Argument();
        load1 = new ARegister(bc.reg);
        load2 = new Argument();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
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
        store = new Argument();
        load1 = new ALiteral();
        load2 = new Argument();
        label = new CBCLabel();
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
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
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
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
        store = new Argument();
        load1 = new ALiteral();
        load2 = new Argument();
        label = new CBCLabel();
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJumpDist(label.dist(this.number, this.getArgsNum()));
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
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
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
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
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
        store = new Argument();
        load1 = new ALiteral(bc.fl);
        load2 = new Argument();
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
        load2 = new Argument();
    }
    @Override
    public boolean isFallThroughInstruction() {
        return false;
    }
    public String toString() {
        return super.toString("error");
    }
}

class MCBCSetfl extends CBCode {
    MCBCSetfl() {
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
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
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
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
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
        this.dst = bc.dst;
    }
    @Override
    public Register getStoreRegister() {
        return dst;
    }
    @Override
    public String toString() {
        return "@MACRO cbc param";
    }
}
