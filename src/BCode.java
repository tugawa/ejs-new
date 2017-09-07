import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BCode {
    int number;

    String toString(String opcode) {
        return opcode;
    }

    String toString(String opcode, Register op1) {
        return opcode + " " + op1;
    }
    String toString(String opcode, Register op1, Register op2) {
        return opcode + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, Register op2, Register op3) {
        return opcode + " " + op1 + " " + op2 + " " + op3;
    }

    String toString(String opcode, Register op1, String op2) {
        return opcode + " " + op1 + " " + op2;
    }

    String toString(String opcode, Register op1, int op2) {
        return opcode + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, double op2) {
        return opcode + " " + op1 + " " + op2;
    }
    String toString(String opcode, Register op1, int op2, int op3) {
        return opcode + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, Register op1, int op2, Register op3) {
        return opcode + " " + op1 + " " + op2 + " " + op3;
    }
    String toString(String opcode, int op1, int op2, Register op3) {
        return opcode + " " + op1 + " " + op2 + " " + op3;
    }

    String toString(String opcode, int op1) {
        return opcode + " " + op1;
    }

    String toString(String opcode, Register op1, int op2, String op3) {
        return opcode + " " + op1 + " " + op2 + " " + op3;
    }
}



class Register {
    int n;
    Register() {}
    Register(int n) {
        this.n = n;
    }
    public String toString() {
        return Integer.toString(n);
    }
}
class Fl {
    int n;
    Fl() {}
    Fl(int n) {
        this.n = n;
    }
    public String toString() {
        return Integer.toString(n);
    }
}
class Label {
    BCode bcode;
    Label() {}
    Label(BCode bcode) { this.bcode = bcode; }
    public int dist(int number) {
        return bcode.number - number;
    }
}


class IFixnum extends BCode {
    Register dst;
    int n;
    IFixnum(Register dst, int n) {
        this.dst = dst;
        this.n = n;
    }
    public String toString() {
        return super.toString("fixnum", dst, n);
    }
}
class INumber extends BCode {
    Register dst;
    double n;
    INumber(Register dst, double n) {
        this.dst = dst;
        this.n = n;
    }
    public String toString() {
        return super.toString("number", dst, n);
    }
}
class IString extends BCode {
    Register dst;
    String str;
    IString(Register dst, String str) {
        this.dst = dst;
        Pattern pt = Pattern.compile("\n");
        Matcher match = pt.matcher(str);
        this.str = match.replaceAll("\\\\n");
    }
    public String toString() {
        return super.toString("string", dst, "\"" + str + "\"");
    }
}
class ISpecconst extends BCode {
    Register dst;
    String val;
    ISpecconst(Register dst, boolean val) { this(dst, Boolean.toString(val)); }
    ISpecconst(Register dst, String val) {
        this.dst = dst;
        this.val = val;
    }
    public String toString() {
        return super.toString("specconst", dst, val);
    }
}
class IRegexp extends BCode {
    Register dst;
    int idx;
    String ptn;
    IRegexp(Register dst, int idx, String ptn) {
        this.dst = dst;
        this.idx = idx;
        this.ptn = ptn;
    }
    public String toString() {
        return super.toString("regexp", dst, idx, "\"" + ptn + "\"");
    }
}



class IAdd extends BCode {
    Register dst, src1, src2;
    IAdd(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("add", dst, src1, src2);
    }
}
class ISub extends BCode {
    Register dst, src1, src2;
    ISub(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("sub", dst, src1, src2);
    }
}
class IMul extends BCode {
    Register dst, src1, src2;
    IMul(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("mul", dst, src1, src2);
    }
}
class IDiv extends BCode {
    Register dst, src1, src2;
    IDiv(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("div", dst, src1, src2);
    }
}
class IMod extends BCode {
    Register dst, src1, src2;
    IMod(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("mod", dst, src1, src2);
    }
}
class IBitor extends BCode {
    Register dst, src1, src2;
    IBitor(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("bitor", dst, src1, src2);
    }
}
class IBitand extends BCode {
    Register dst, src1, src2;
    IBitand(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("bitand", dst, src1, src2);
    }
}
class ILeftshift extends BCode {
    Register dst, src1, src2;
    ILeftshift(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("leftshift", dst, src1, src2);
    }
}
class IRightshift extends BCode {
    Register dst, src1, src2;
    IRightshift(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("rightshift", dst, src1, src2);
    }
}
class IUnsignedrightshift extends BCode {
    Register dst, src1, src2;
    IUnsignedrightshift(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("unsignedrightshift", dst, src1, src2);
    }
}

// relation
class IEqual extends BCode {
    Register dst, src1, src2;
    IEqual(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("equal", dst, src1, src2);
    }
}
class IEq extends BCode {
    Register dst, src1, src2;
    IEq(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("eq", dst, src1, src2);
    }
}
class ILessthan extends BCode {
    Register dst, src1, src2;
    ILessthan(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("lessthan", dst, src1, src2);
    }
}
class ILessthanequal extends BCode {
    Register dst, src1, src2;
    ILessthanequal(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("lessthanequal", dst, src1, src2);
    }
}
class INot extends BCode {
    Register dst, src;
    INot(Register dst, Register src) {
        this.dst = dst;
        this.src = src;
    }
    public String toString() {
        return super.toString("not", dst, src);
    }
}




class IGetglobalobj extends BCode {
    Register dst;
    IGetglobalobj(Register dst) { this.dst = dst; }
    public String toString() {
        return super.toString("getglobalobj", dst);
    }
}
class INewargs extends BCode {
    INewargs() {}
    public String toString() {
        return super.toString("newargs");
    }
}
class IGetglobal extends BCode {
    Register dst, lit;
    IGetglobal(Register dst, Register lit) {
        this.dst = dst;
        this.lit = lit;
    }
    public String toString() {
        return super.toString("getglobal", dst, lit);
    }
}
class ISetglobal extends BCode {
    Register lit, src;
    ISetglobal(Register lit, Register src) {
        this.lit = lit;
        this.src = src;
    }
    public String toString() {
        return super.toString("setglobal", lit, src);
    }
}
class IGetlocal extends BCode {
    Register dst;
    int depth, n;
    IGetlocal(Register dst, int depth, int n) {
        this.dst = dst;
        this.depth = depth;
        this.n = n;
    }
    public String toString() {
        return super.toString("getlocal", dst, depth, n);
    }
}
class ISetlocal extends BCode {
    int depth, n;
    Register src;
    ISetlocal(int depth, int n, Register src) {
        this.depth = depth;
        this.n = n;
        this.src = src;
    }
    public String toString() {
        return super.toString("setlocal", depth, n, src);
    }
}
class IGetarg extends BCode {
    Register dst;
    int depth, n;
    IGetarg(Register dst, int depth, int n) {
        this.dst = dst;
        this.depth = depth;
        this.n = n;
    }
    public String toString() {
        return super.toString("getarg", dst, depth, n);
    }
}
class ISetarg extends BCode {
    int depth, n;
    Register src;
    ISetarg(int depth, int n, Register src) {
        this.depth = depth;
        this.n = n;
        this.src = src;
    }
    public String toString() {
        return super.toString("setarg", depth, n, src);
    }
}
class IGetprop extends BCode {
    Register dst, obj, prop;
    IGetprop(Register dst, Register obj, Register prop) {
        this.dst = dst;
        this.obj = obj;
        this.prop = prop;
    }
    public String toString() {
        return super.toString("getprop", dst, obj, prop);
    }
}
class ISetprop extends BCode {
    Register obj, prop, src;
    ISetprop(Register obj, Register prop, Register src) {
        this.obj = obj;
        this.prop = prop;
        this.src = src;
    }
    public String toString() {
        return super.toString("setprop", obj, prop, src);
    }
}
class ISetarray extends BCode {
    Register ary;
    int n;
    Register src;
    ISetarray(Register ary, int n, Register src) {
        this.ary = ary;
        this.n = n;
        this.src = src;
    }
    public String toString() {
        return super.toString("setarray", ary, n, src);
    }
}


class IMakeclosure extends BCode {
    Register dst;
    int idx;
    IMakeclosure(Register dst, int idx) {
        this.dst = dst;
        this.idx = idx;
    }
    public String toString() {
        return super.toString("makeclosure", dst, idx);
    }
}


class IGeta extends BCode {
    Register dst;
    IGeta(Register dst) { this.dst = dst; }
    public String toString() {
        return super.toString("geta", dst);
    }
}
class ISeta extends BCode {
    Register src;
    ISeta(Register src) { this.src = src; }
    public String toString() {
        return super.toString("seta", src);
    }
}

class IRet extends BCode {
    IRet() {}
    public String toString() {
        return super.toString("ret");
    }
}

class IMove extends BCode {
    Register dst, src;
    IMove(Register dst, Register src) {
        this.dst = dst;
        this.src = src;
    }
    public String toString() {
        return super.toString("move", dst, src);
    }
}

class IIsundef extends BCode {
    Register dst, src;
    IIsundef(Register dst, Register src) {
        this.dst = dst;
        this.src = src;
    }
    public String toString() {
        return super.toString("isundef", dst, src);
    }
}
class IIsobject extends BCode {
    Register dst, src;
    IIsobject(Register dst, Register src) {
        this.dst = dst;
        this.src = src;
    }
    public String toString() {
        return super.toString("isobject", dst, src);
    }
}
class IInstanceof extends BCode {
    Register dst, src1, src2;
    IInstanceof(Register dst, Register src1, Register src2) {
        this.dst = dst;
        this.src1 = src1;
        this.src2 = src2;
    }
    public String toString() {
        return super.toString("instanceof", dst, src1, src2);
    }
}


class ICall extends BCode {
    Register callee;
    int numOfArgs;
    ICall(Register callee, int numOfArgs) {
        this.callee = callee;
        this.numOfArgs = numOfArgs;
    }
    public String toString() {
        return super.toString("call", callee, numOfArgs);
    }
}
class ISend extends BCode {
    Register callee;
    int numOfArgs;
    ISend(Register callee, int numOfArgs) {
        this.callee = callee;
        this.numOfArgs = numOfArgs;
    }
    public String toString() {
        return super.toString("send", callee, numOfArgs);
    }
}
class INew extends BCode {
    Register dst, constructor;
    INew(Register dst, Register constructor) {
        this.dst = dst;
        this.constructor = constructor;
    }
    public String toString() {
        return super.toString("new", dst, constructor);
    }
}
class INewsend extends BCode {
    Register constructor;
    int numOfArgs;
    INewsend(Register constructor, int numOfArgs) {
        this.constructor = constructor;
        this.numOfArgs = numOfArgs;
    }
    public String toString() {
        return super.toString("newsend", constructor, numOfArgs);
    }
}
class IMakeiterator extends BCode {
    Register obj, dst;
    IMakeiterator(Register obj, Register dst) {
        this.obj = obj;
        this.dst = dst;
    }
    public String toString() {
        return super.toString("makeiterator", obj, dst);
    }
}
class INextpropname extends BCode {
    Register obj, ite, dst;
    INextpropname(Register obj, Register ite, Register dst) {
        this.obj = obj;
        this.ite = ite;
        this.dst = dst;
    }
    public String toString() {
        return super.toString("nextpropname", obj, ite, dst);
    }
}


// Jump instructions
class IJump extends BCode {
    Label label;
    IJump(Label label) { this.label = label; }
    public String toString() {
        return super.toString("jump", label.dist(number));
    }
}
class IJumptrue extends BCode {
    Register test;
    Label label;
    IJumptrue(Register test, Label label) {
        this.test = test;
        this.label = label;
    }
    public String toString() {
        return super.toString("jumptrue", test, label.dist(number));
    }
}
class IJumpfalse extends BCode {
    Register test;
    Label label;
    IJumpfalse(Register test, Label label) {
        this.test = test;
        this.label = label;
    }
    public String toString() {
        return super.toString("jumpfalse", test, label.dist(number));
    }
}


class IThrow extends BCode {
    Register reg;
    IThrow(Register reg) {
        this.reg = reg;
    }
    public String toString() {
        return super.toString("throw", reg);
    }
}
class IPushhandler extends BCode {
    Label label;
    IPushhandler(Label label) {
        this.label = label;
    }
    public String toString() {
        return super.toString("pushhandler", label.dist(number));
    }
}
class IPophandler extends BCode {
    public String toString() {
        return super.toString("pophandler");
    }
}
class ILocalcall extends BCode {
    Label label;
    ILocalcall(Label label) {
        this.label = label;
    }
    public String toString() {
        return super.toString("localcall", label.dist(number));
    }
}
class ILocalret extends BCode {
    public String toString() {
        return super.toString("localret");
    }
}
class IPoplocal extends BCode {
    public String toString() {
        return super.toString("poplocal");
    }
}



class ISetfl extends BCode {
    Fl fl;
    ISetfl(Fl fl) { this.fl = fl; }
    public String toString() {
        return super.toString("setfl", fl.n);
    }
}


class IFuncLength extends BCode {
    int n;
    IFuncLength(int n) { this.n = n; }
    public String toString() {
        return super.toString("funcLength", n);
    }
}
class ICallentry extends BCode {
    int n;
    ICallentry(int n) { this.n = n; }
    public String toString() {
        return super.toString("callentry", n);
    }
}
class ISendentry extends BCode {
    int n;
    ISendentry(int n) { this.n = n; }
    public String toString() {
        return super.toString("sendentry", n);
    }
}
class INumberOfLocals extends BCode {
    int n;
    INumberOfLocals(int n) { this.n = n; }
    public String toString() {
        return super.toString("numberOfLocals", n);
    }
}
class INumberOfInstruction extends BCode {
    int n;
    INumberOfInstruction(int n) { this.n = n; }
    public String toString() {
        return super.toString("numberOfInstruction", n);
    }
}


class IError extends BCode {
    Register dst;
    String str;
    IError(Register dst, String str) {
        this.dst = dst;
        this.str = str;
    }
    public String toString() {
        return super.toString("error", dst, str);
    }
}