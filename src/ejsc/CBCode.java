package ejsc;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ejsc.Argument.ArgType;

public class CBCode extends BCode {
    protected Argument store, load1, load2;

    CBCode() {}

    int getArgsNum() {
        return this.store.getArgNum() + this.load1.getArgNum() + this.load2.getArgNum();
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
    void replaceJampDist(int n) {
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
}

class ICBCFixnum extends CBCode {
    ICBCFixnum(IFixnum bc) {
        store = new ARegister(bc.dst);
        load1 = new AFixnum(bc.n);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("fixnum");
    }
}
class ICBCNumber extends CBCode {
    ICBCNumber(INumber bc) {
        store = new ARegister(bc.dst);
        load1 = new ANumber(bc.n);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("number");
    }
}
class ICBCString extends CBCode {
    ICBCString(IString bc) {
        Pattern pt = Pattern.compile("\n");
        Matcher match = pt.matcher(bc.str);
        String str = match.replaceAll("\\\\n");
        store = new ARegister(bc.dst);
        load1 = new AString(str);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("string");
    }
}
class ICBCBooleanconst extends CBCode {
    ICBCBooleanconst(IBooleanconst bc) {
        store = new ARegister(bc.dst);
        load1 = new ASpecial(bc.b ? "true" : "false");
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("specconst");
    }
}
class ICBCNullconst extends CBCode {
    ICBCNullconst(INullconst bc) {
        store = new ARegister(bc.dst);
        load1 = new ASpecial("null");
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("specconst");
    }
}
class ICBCUndefinedconst extends CBCode {
    ICBCUndefinedconst(IUndefinedconst bc) {
        store = new ARegister(bc.dst);
        load1 = new ASpecial("undefined");
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("specconst");
    }
}
class ICBCRegexp extends CBCode {
    int idx;
    String ptn;
    ICBCRegexp(IRegexp bc) {
        this.idx = bc.idx;
        this.ptn = bc.ptn;
        store = new ARegister(bc.dst);
        load1 = new ARegexp(bc.idx, bc.ptn);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("regexp");
    }
}
class ICBCAdd extends CBCode {
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
    ICBCNewframe(INewframe bc) {
        store = new Argument();
        load1 = new ALiteral(bc.len);
        load2 = new ALiteral(bc.status);
    }
    public String toString() {
        return super.toString("newframe");
    }
}
class ICBCGetglobal extends CBCode {
    ICBCGetglobal(IGetglobal bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.lit, ArgType.GLOBAL);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("getglobal");
    }
}
class ICBCSetglobal extends CBCode {
    ICBCSetglobal(ISetglobal bc) {
        store = new ARegister(bc.lit, ArgType.GLOBAL);
        load1 = new ARegister(bc.src);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("setglobal");
    }
}
class ICBCGetlocal extends CBCode {
    ICBCGetlocal(IGetlocal bc) {
        store = new ARegister(bc.dst);
        load1 = new ALitPair(bc.depth, bc.n, ArgType.LOCAL);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("getlocal");
    }
}
class ICBCSetlocal extends CBCode {
    ICBCSetlocal(ISetlocal bc) {
        store = new ALitPair(bc.depth, bc.n, ArgType.LOCAL);
        load1 = new ARegister(bc.src);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("setlocal");
    }
}
class ICBCGetarg extends CBCode {
    ICBCGetarg(IGetarg bc) {
        store = new ARegister(bc.dst);
        load1 = new ALitPair(bc.depth, bc.n, ArgType.ARGS);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("getarg");
    }
}
class ICBCSetarg extends CBCode {
    ICBCSetarg(ISetarg bc) {
        store = new ALitPair(bc.depth, bc.n, ArgType.ARGS);
        load1 = new ARegister(bc.src);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("setarg");
    }
}
class ICBCGetprop extends CBCode {
    ICBCGetprop(IGetprop bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegPair(bc.obj, bc.prop, ArgType.PROP);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("getprop");
    }
}
class ICBCSetprop extends CBCode {
    ICBCSetprop(ISetprop bc) {
        store = new ARegPair(bc.obj, bc.prop, ArgType.PROP);
        load1 = new ARegister(bc.src);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("setprop");
    }
}
class ICBCSetarray extends CBCode {
    ICBCSetarray(ISetarray bc) {
        store = new ARegLitPair(bc.ary, bc.n, ArgType.ARRAY);
        load1 = new ARegister(bc.src);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("setarray");
    }
}
class ICBCMakeclosure extends CBCode {
    ICBCMakeclosure(IMakeclosure bc) {
        store = new ARegister(bc.dst);
        load1 = new ALiteral(bc.idx);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("makeclosure");
    }
}
class ICBCGeta extends CBCode {
    ICBCGeta(IGeta bc) {
        store = new ARegister(bc.dst);
        load1 = new Argument(ArgType.A);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("geta");
    }
}
class ICBCSeta extends CBCode {
    ICBCSeta(ISeta bc) {
        store = new Argument(ArgType.A);
        load1 = new ARegister(bc.src);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("seta");
    }
}
class ICBCRet extends CBCode {
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
class ICBCMove extends CBCode {
    ICBCMove(IMove bc) {
        store = new ARegister(bc.dst);
        load1 = new ARegister(bc.src);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("move");
    }
}
class ICBCIsundef extends CBCode {
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
    ICBCMakesimpleiterator(IMakesimpleiterator bc) {
        store = new ARegister(bc.obj);
        load1 = new ARegister(bc.dst);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("makesimpleiterator");
    }
}
class ICBCNextpropnameidx extends CBCode {
    ICBCNextpropnameidx(INextpropnameidx bc) {
        store = new ARegister(bc.ite);
        load1 = new ARegister(bc.dst);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("nextpropnameidx");
    }
}


// Jump instructions
class ICBCJump extends CBCode {
    Label label;
    ICBCJump(IJump bc) {
        store = new Argument();
        load1 = new ALiteral();
        load2 = new Argument();
        this.label = bc.label;
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJampDist(label.dist(number, this.getArgsNum()));
    }
    public String toString() {
        return super.toString("jump");
    }
}
class ICBCJumptrue extends CBCode {
    Label label;
    ICBCJumptrue(IJumptrue bc) {
        store = new Argument();
        load1 = new ARegister(bc.test);
        load2 = new ALiteral();
        this.label = bc.label;
    }
    void resolveJumpDist() {
        ((ALiteral) load2).replaceJampDist(label.dist(number, this.getArgsNum()));
    }
    public String toString() {
        return super.toString("jumptrue");
    }
}
class ICBCJumpfalse extends CBCode {
    Label label;
    ICBCJumpfalse(IJumpfalse bc) {
        store = new Argument();
        load1 = new ARegister(bc.test);
        load2 = new ALiteral();
        this.label = bc.label;
    }
    void resolveJumpDist() {
        ((ALiteral) load2).replaceJampDist(label.dist(number, this.getArgsNum()));
    }
    public String toString() {
        return super.toString("jumpfalse");
    }
}


class ICBCThrow extends CBCode {
    ICBCThrow(IThrow bc) {
        store = new Argument();
        load1 = new ARegister(bc.reg);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("throw");
    }
}
class ICBCPushhandler extends CBCode {
    Label label;
    ICBCPushhandler(IPushhandler bc) {
        store = new Argument();
        load1 = new ALiteral();
        load2 = new Argument();
        this.label = bc.label;
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJampDist(label.dist(number, this.getArgsNum()));
    }
    public String toString() {
        return super.toString("pushhandler");
    }
}
class ICBCPophandler extends CBCode {
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
    Label label;
    ICBCLocalcall(ILocalcall bc) {
        store = new Argument();
        load1 = new ALiteral();
        load2 = new Argument();
        this.label = bc.label;
    }
    void resolveJumpDist() {
        ((ALiteral) load1).replaceJampDist(label.dist(number, this.getArgsNum()));
    }
    public String toString() {
        return super.toString("localcall");
    }
}
class ICBCLocalret extends CBCode {
    ICBCLocalret() {
        store = new Argument();
        load1 = new Argument();
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("localret");
    }
}
class ICBCPoplocal extends CBCode {
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
    ICBCSetfl(ISetfl bc) {
        store = new Argument();
        load1 = new ALiteral(bc.fl);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("setfl");
    }
}
class ICBCError extends CBCode {
    ICBCError(IError bc) {
        store = new ARegister(bc.dst);
        load1 = new AString(bc.str);
        load2 = new Argument();
    }
    public String toString() {
        return super.toString("error");
    }
}
