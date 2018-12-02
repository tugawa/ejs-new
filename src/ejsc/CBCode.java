package ejsc;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CBCode extends BCode {
    static enum LoadArgType {
        NONE, REGISTER, LITERAL, SHORTLITERAL,
        GLOBAL, LOCAL, PROP, ARGS, A,
        SPECCONST, NUMBER, STRING, REGEXP;

        public int getArgNum() {
            switch(this) {
            case NONE: case A:
                return 0;
            case SHORTLITERAL: case SPECCONST:
                return 1;
            case REGISTER: case GLOBAL:
            case NUMBER: case STRING: case REGEXP:
                return 2;
            case LITERAL: case LOCAL: case PROP: case ARGS:
                return 4;
            default:
                throw new Error("undefined enum type: " + this.name());
            }
        }
    }
    static enum StoreArgType {
        REGISTER, GLOBAL,
        LOCAL, PROP, A,
        ARRAY, ARGS, NONE;

        public int getArgNum() {
            switch(this) {
            case NONE: case A:
                return 0;
            case REGISTER: case GLOBAL:
                return 2;
            case LOCAL: case PROP: case ARGS: case ARRAY:
                return 4;
            default:
                throw new Error("undefined enum type: " + this.name());
            }
        }
    }
    StoreArgType store;
    LoadArgType  load1, load2;

    CBCode() {}

    CBCode(Register dst) {
        super(dst);
    }

    int getArgsNum() {
        return this.store.getArgNum() + this.load1.getArgNum() + this.load2.getArgNum();
    }

    String toStringArgs() {
        if (store == null)
            return "";
        return " " + store.toString() +
               "_" + load1.toString() +
               "_" + load2.toString();
    }
    
    String toString(String opcode) {
        return opcode + toStringArgs();
    }
    String toString(String opcode, Register op1) {
        return opcode + toStringArgs() + " " + op1;
    }
    String toString(String opcode, Register op1, Register op2) {
        return opcode + toStringArgs() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, Register op2, Register op3) {
        return opcode + toStringArgs() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, Register op1, String op2) {
        return opcode + toStringArgs() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, int op2) {
        return opcode + toStringArgs() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, double op2) {
        return opcode + toStringArgs() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, int op2, int op3) {
        return opcode + toStringArgs() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, Register op1, int op2, Register op3) {
        return opcode + toStringArgs() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, int op1, int op2, Register op3) {
        return opcode + toStringArgs() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, int op1) {
        return opcode + toStringArgs() + " " + op1;
    }
    String toString(String opcode, int op1, int op2) {
        return opcode + toStringArgs() + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, int op2, String op3) {
        return opcode + toStringArgs() + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, Register op1, Register op2, int op3) {
        return opcode + " " + op1 + " " + op2 + " " + op3;
    }
}

class ICBCShortFixnum extends CBCode {
    int n;
    ICBCShortFixnum(IShortFixnum bc) {
        super(bc.dst);
        this.n = bc.n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SHORTLITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("fixnum", dst, n);
    }
}

class ICBCFixnum extends CBCode {
    int n;
    ICBCFixnum(IFixnum bc) {
        super(bc.dst);
        this.n = bc.n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("fixnum", dst, n);
    }
}
class ICBCNumber extends CBCode {
    double n;
    ICBCNumber(INumber bc) {
        super(bc.dst);
        this.n = bc.n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.NUMBER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("number", dst, n);
    }
}
class ICBCString extends CBCode {
    String str;
    ICBCString(IString bc) {
        super(bc.dst);
        Pattern pt = Pattern.compile("\n");
        Matcher match = pt.matcher(bc.str);
        this.str = match.replaceAll("\\\\n");
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.STRING;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("string", dst, "\"" + str + "\"");
    }
}
class ICBCBooleanconst extends CBCode {
    boolean b;
    ICBCBooleanconst(IBooleanconst bc) {
        super(bc.dst);
        this.b = bc.b;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SPECCONST;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("specconst", dst, b ? "true" : "false");
    }
}
class ICBCNullconst extends CBCode {
    ICBCNullconst(INullconst bc) {
        super(bc.dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SPECCONST;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("specconst", dst, "null");
    }
}
class ICBCUndefinedconst extends CBCode {
    ICBCUndefinedconst(IUndefinedconst bc) {
        super(bc.dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SPECCONST;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("specconst", dst, "undefined");
    }
}
class ICBCRegexp extends CBCode {
    int idx;
    String ptn;
    ICBCRegexp(IRegexp bc) {
        super(bc.dst);
        this.idx = bc.idx;
        this.ptn = bc.ptn;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGEXP;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("regexp", dst, idx, "\"" + ptn + "\"");
    }
}
class ICBCAdd extends CBCode {
    Register src1, src2;
    ICBCAdd(IAdd bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("add", dst, src1, src2);
    }
}
class ICBCSub extends CBCode {
    Register src1, src2;
    ICBCSub(ISub bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("sub", dst, src1, src2);
    }
}
class ICBCMul extends CBCode {
    Register src1, src2;
    ICBCMul(IMul bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("mul", dst, src1, src2);
    }
}
class ICBCDiv extends CBCode {
    Register src1, src2;
    ICBCDiv(IDiv bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("div", dst, src1, src2);
    }
}
class ICBCMod extends CBCode {
    Register src1, src2;
    ICBCMod(IMod bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("mod", dst, src1, src2);
    }
}
class ICBCBitor extends CBCode {
    Register src1, src2;
    ICBCBitor(IBitor bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("bitor", dst, src1, src2);
    }
}
class ICBCBitand extends CBCode {
    Register src1, src2;
    ICBCBitand(IBitand bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("bitand", dst, src1, src2);
    }
}
class ICBCLeftshift extends CBCode {
    Register src1, src2;
    ICBCLeftshift(ILeftshift bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("leftshift", dst, src1, src2);
    }
}
class ICBCRightshift extends CBCode {
    Register src1, src2;
    ICBCRightshift(IRightshift bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("rightshift", dst, src1, src2);
    }
}
class ICBCUnsignedrightshift extends CBCode {
    Register src1, src2;
    ICBCUnsignedrightshift(IUnsignedrightshift bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("unsignedrightshift", dst, src1, src2);
    }
}


// relation
class ICBCEqual extends CBCode {
    Register src1, src2;
    ICBCEqual(IEqual bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("equal", dst, src1, src2);
    }
}
class ICBCEq extends CBCode {
    Register src1, src2;
    ICBCEq(IEq bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("eq", dst, src1, src2);
    }
}
class ICBCLessthan extends CBCode {
    Register src1, src2;
    ICBCLessthan(ILessthan bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("lessthan", dst, src1, src2);
    }
}
class ICBCLessthanequal extends CBCode {
    Register src1, src2;
    ICBCLessthanequal(ILessthanequal bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("lessthanequal", dst, src1, src2);
    }
}


class ICBCNot extends CBCode {
    Register src;
    ICBCNot(INot bc) {
        super(bc.dst);
        this.src = bc.src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("not", dst, src);
    }
}
class ICBCGetglobalobj extends CBCode {
    ICBCGetglobalobj(IGetglobalobj bc) {
        super(bc.dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("getglobalobj", dst);
    }
}
class ICBCNewargs extends CBCode {
    ICBCNewargs() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("newargs");
    }
}
class ICBCNewframe extends CBCode {
    int len;
    int status;
    ICBCNewframe(INewframe bc) {
        this.len = bc.len;
        this.status = bc.status;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("newframe", len, status);
    }
}
class ICBCGetglobal extends CBCode {
    Register lit;
    ICBCGetglobal(IGetglobal bc) {
        super(bc.dst);
        this.lit = bc.lit;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.GLOBAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("getglobal", dst, lit);
    }
}
class ICBCSetglobal extends CBCode {
    Register lit, src;
    ICBCSetglobal(ISetglobal bc) {
        this.lit = bc.lit;
        this.src = bc.src;
        this.store = StoreArgType.GLOBAL;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setglobal", lit, src);
    }
}
class ICBCGetlocal extends CBCode {
    int depth, n;
    ICBCGetlocal(IGetlocal bc) {
        super(bc.dst);
        this.depth = bc.depth;
        this.n = bc.n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.LOCAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("getlocal", dst, depth, n);
    }
}
class ICBCSetlocal extends CBCode {
    int depth, n;
    Register src;
    ICBCSetlocal(ISetlocal bc) {
        this.depth = bc.depth;
        this.n = bc.n;
        this.src = bc.src;
        this.store = StoreArgType.LOCAL;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setlocal", depth, n, src);
    }
}
class ICBCGetarg extends CBCode {
    int depth, n;
    ICBCGetarg(IGetarg bc) {
        super(bc.dst);
        this.depth = bc.depth;
        this.n = bc.n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.ARGS;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("getarg", dst, depth, n);
    }
}
class ICBCSetarg extends CBCode {
    int depth, n;
    Register src;
    ICBCSetarg(ISetarg bc) {
        this.depth = bc.depth;
        this.n = bc.n;
        this.src = bc.src;
        this.store = StoreArgType.ARGS;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setarg", depth, n, src);
    }
}
class ICBCGetprop extends CBCode {
    Register obj, prop;
    ICBCGetprop(IGetprop bc) {
        super(bc.dst);
        this.obj = bc.obj;
        this.prop = bc.prop;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.PROP;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("getprop", dst, obj, prop);
    }
}
class ICBCSetprop extends CBCode {
    Register obj, prop, src;
    ICBCSetprop(ISetprop bc) {
        this.obj = bc.obj;
        this.prop = bc.prop;
        this.src = bc.src;
        this.store = StoreArgType.PROP;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setprop", obj, prop, src);
    }
}
class ICBCSetarray extends CBCode {
    Register ary;
    int n;
    Register src;
    ICBCSetarray(ISetarray bc) {
        this.ary = bc.ary;
        this.n = bc.n;
        this.src = bc.src;
        this.store = StoreArgType.ARRAY;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setarray", ary, n, src);
    }
}
class ICBCMakeclosure extends CBCode {
    int idx;
    ICBCMakeclosure(IMakeclosure bc) {
        super(bc.dst);
        this.idx = bc.idx;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("makeclosure", dst, idx);
    }
}
class ICBCGeta extends CBCode {
    ICBCGeta(IGeta bc) {
        super(bc.dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.A;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("geta", dst);
    }
}
class ICBCSeta extends CBCode {
    Register src;
    ICBCSeta(ISeta bc) {
        this.src = bc.src;
        this.store = StoreArgType.A;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("seta", src);
    }
}
class ICBCRet extends CBCode {
    ICBCRet() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
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
    Register src;
    ICBCMove(IMove bc) {
        super(bc.dst);
        this.src = bc.src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("move", dst, src);
    }
}
class ICBCIsundef extends CBCode {
    Register src;
    ICBCIsundef(IIsundef bc) {
        super(bc.dst);
        this.src = bc.src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("isundef", dst, src);
    }
}
class ICBCIsobject extends CBCode {
    Register src;
    ICBCIsobject(IIsobject bc) {
        super(bc.dst);
        this.src = bc.src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("isobject", dst, src);
    }
}
class ICBCInstanceof extends CBCode {
    Register src1, src2;
    ICBCInstanceof(IInstanceof bc) {
        super(bc.dst);
        this.src1 = bc.src1;
        this.src2 = bc.src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("instanceof", dst, src1, src2);
    }
}
class ICBCCall extends CBCode {
    Register callee;
    int numOfArgs;
    ICBCCall(ICall bc) {
        this.callee = bc.callee;
        this.numOfArgs = bc.numOfArgs;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("call", callee, numOfArgs);
    }
}
class ICBCSend extends CBCode {
    Register callee;
    int numOfArgs;
    ICBCSend(ISend bc) {
        this.callee = bc.callee;
        this.numOfArgs = bc.numOfArgs;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("send", callee, numOfArgs);
    }
}
class ICBCNew extends CBCode {
    Register constructor;
    ICBCNew(INew bc) {
        super(bc.dst);
        this.constructor = bc.constructor;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("new", dst, constructor);
    }
}
class ICBCNewsend extends CBCode {
    Register constructor;
    int numOfArgs;
    ICBCNewsend(INewsend bc) {
        this.constructor = bc.constructor;
        this.numOfArgs = bc.numOfArgs;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("newsend", constructor, numOfArgs);
    }
}
class ICBCMakesimpleiterator extends CBCode {
    Register obj;
    ICBCMakesimpleiterator(IMakesimpleiterator bc) {
        super(bc.obj);
        this.obj = bc.dst;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("makesimpleiterator", dst, obj);
    }
}
class ICBCNextpropnameidx extends CBCode {
    Register ite;
    ICBCNextpropnameidx(INextpropnameidx bc) {
        super(bc.ite);
        this.ite = bc.dst;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("nextpropnameidx", dst, ite);
    }
}


// Jump instructions
class ICBCJump extends CBCode {
    Label label;
    ICBCJump(IJump bc) {
        this.label = bc.label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("jump", label.dist(number, this.getArgsNum()));
    }
}
class ICBCJumptrue extends CBCode {
    Register test;
    Label label;
    ICBCJumptrue(IJumptrue bc) {
        this.test = bc.test;
        this.label = bc.label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("jumptrue", test, label.dist(number, this.getArgsNum()));
    }
}
class ICBCJumpfalse extends CBCode {
    Register test;
    Label label;
    ICBCJumpfalse(IJumpfalse bc) {
        this.test = bc.test;
        this.label = bc.label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("jumpfalse", test, label.dist(number, this.getArgsNum()));
    }
}


class ICBCThrow extends CBCode {
    Register reg;
    ICBCThrow(IThrow bc) {
        this.reg = bc.reg;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("throw", reg);
    }
}
class ICBCPushhandler extends CBCode {
    Label label;
    ICBCPushhandler(IPushhandler bc) {
        this.label = bc.label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("pushhandler", label.dist(number, this.getArgsNum()));
    }
}
class ICBCPophandler extends CBCode {
    ICBCPophandler() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("pophandler");
    }
}
class ICBCLocalcall extends CBCode {
    Label label;
    ICBCLocalcall(ILocalcall bc) {
        this.label = bc.label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("localcall", label.dist(number, this.getArgsNum()));
    }
}
class ICBCLocalret extends CBCode {
    ICBCLocalret() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("localret");
    }
}
class ICBCPoplocal extends CBCode {
    ICBCPoplocal() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("poplocal");
    }
}
class ICBCSetfl extends CBCode {
    int fl;
    ICBCSetfl(ISetfl bc) {
        this.fl = bc.fl;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setfl", fl);
    }
}
class ICBCError extends CBCode {
    String str;
    ICBCError(IError bc) {
        super(bc.dst);
        this.str = bc.str;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.STRING;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("error", dst, str);
    }
}
