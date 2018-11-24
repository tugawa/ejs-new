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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BCode {
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
    int number;
    protected Register dst;
    ArrayList<Label> labels = new ArrayList<Label>();
    StoreArgType store;
    LoadArgType  load1, load2;
    
    BCode() {}
    
    BCode(Register dst) {
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
	    		if (f.getType() == Register.class) {
	    			try {
	    				srcs.add((Register) f.get(this));
	    			} catch (Exception e) {
	    				throw new Error(e);
	    			}
	    		}
	    	}
	    	
	    	return srcs;
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

class IShortFixnum extends BCode {
    int n;
    IShortFixnum(Register dst, int n) {
        super(dst);
        this.n = n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SHORTLITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("fixnum", dst, n);
    }
}

class IFixnum extends BCode {
    int n;
    IFixnum(Register dst, int n) {
    		super(dst);
        this.n = n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("fixnum", dst, n);
    }
}
class INumber extends BCode {
    double n;
    INumber(Register dst, double n) {
    		super(dst);
        this.n = n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.NUMBER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("number", dst, n);
    }
}
class IString extends BCode {
    String str;
    IString(Register dst, String str) {
    		super(dst);
        Pattern pt = Pattern.compile("\n");
        Matcher match = pt.matcher(str);
        this.str = match.replaceAll("\\\\n");
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.STRING;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("string", dst, "\"" + str + "\"");
    }
}
class IBooleanconst extends BCode {
    boolean b;
    IBooleanconst(Register dst, boolean b) {
        super(dst);
        this.b = b;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SPECCONST;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("specconst", dst, b ? "true" : "false");
    }
}
class INullconst extends BCode {
    INullconst(Register dst) {
        super(dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SPECCONST;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("specconst", dst, "null");
    }
}
class IUndefinedconst extends BCode {
    IUndefinedconst(Register dst) {
        super(dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.SPECCONST;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("specconst", dst, "undefined");
    }
}
class IRegexp extends BCode {
    int idx;
    String ptn;
    IRegexp(Register dst, int idx, String ptn) {
    		super(dst);
        this.idx = idx;
        this.ptn = ptn;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGEXP;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("regexp", dst, idx, "\"" + ptn + "\"");
    }
}



class IAdd extends BCode {
    Register src1, src2;
    IAdd(Register dst, Register src1, Register src2) {
    		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("add", dst, src1, src2);
    }
}
class ISub extends BCode {
    Register src1, src2;
    ISub(Register dst, Register src1, Register src2) {
    		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("sub", dst, src1, src2);
    }
}
class IMul extends BCode {
    Register src1, src2;
    IMul(Register dst, Register src1, Register src2) {
    		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("mul", dst, src1, src2);
    }
}
class IDiv extends BCode {
    Register src1, src2;
    IDiv(Register dst, Register src1, Register src2) {
    		super(dst);
    		this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("div", dst, src1, src2);
    }
}
class IMod extends BCode {
    Register src1, src2;
    IMod(Register dst, Register src1, Register src2) {
        super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("mod", dst, src1, src2);
    }
}
class IBitor extends BCode {
    Register src1, src2;
    IBitor(Register dst, Register src1, Register src2) {
    		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("bitor", dst, src1, src2);
    }
}
class IBitand extends BCode {
    Register src1, src2;
    IBitand(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("bitand", dst, src1, src2);
    }
}
class ILeftshift extends BCode {
    Register src1, src2;
    ILeftshift(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("leftshift", dst, src1, src2);
    }
}
class IRightshift extends BCode {
    Register src1, src2;
    IRightshift(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("rightshift", dst, src1, src2);
    }
}
class IUnsignedrightshift extends BCode {
    Register src1, src2;
    IUnsignedrightshift(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("unsignedrightshift", dst, src1, src2);
    }
}

// relation
class IEqual extends BCode {
    Register src1, src2;
    IEqual(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("equal", dst, src1, src2);
    }
}
class IEq extends BCode {
    Register src1, src2;
    IEq(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("eq", dst, src1, src2);
    }
}
class ILessthan extends BCode {
    Register src1, src2;
    ILessthan(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("lessthan", dst, src1, src2);
    }
}
class ILessthanequal extends BCode {
    Register src1, src2;
    ILessthanequal(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
    }
    public String toString() {
        return super.toString("lessthanequal", dst, src1, src2);
    }
}
class INot extends BCode {
    Register src;
    INot(Register dst, Register src) {
		super(dst);
        this.src = src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("not", dst, src);
    }
}




class IGetglobalobj extends BCode {
    IGetglobalobj(Register dst) {
		super(dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("getglobalobj", dst);
    }
}
class INewargs extends BCode {
    INewargs() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("newargs");
    }
}
class INewframe extends BCode {
    int len;
    int status;
    INewframe(int len, int status) {
		this.len = len;
        this.status = status;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("newframe", len, status);
    }
}
class IGetglobal extends BCode {
    Register lit;
    IGetglobal(Register dst, Register lit) {
		super(dst);
        this.lit = lit;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.GLOBAL;
        this.load2 = LoadArgType.NONE;
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
        this.store = StoreArgType.GLOBAL;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setglobal", lit, src);
    }
}
class IGetlocal extends BCode {
    int depth, n;
    IGetlocal(Register dst, int depth, int n) {
		super(dst);
        this.depth = depth;
        this.n = n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.LOCAL;
        this.load2 = LoadArgType.NONE;
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
        this.store = StoreArgType.LOCAL;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setlocal", depth, n, src);
    }
}
class IGetarg extends BCode {
    int depth, n;
    IGetarg(Register dst, int depth, int n) {
		super(dst);
        this.depth = depth;
        this.n = n;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.ARGS;
        this.load2 = LoadArgType.NONE;
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
        this.store = StoreArgType.ARGS;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setarg", depth, n, src);
    }
}
class IGetprop extends BCode {
    Register obj, prop;
    IGetprop(Register dst, Register obj, Register prop) {
		super(dst);
        this.obj = obj;
        this.prop = prop;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.PROP;
        this.load2 = LoadArgType.NONE;
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
        this.store = StoreArgType.PROP;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
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
        this.store = StoreArgType.ARRAY;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("setarray", ary, n, src);
    }
}


class IMakeclosure extends BCode {
    int idx;
    IMakeclosure(Register dst, int idx) {
		super(dst);
        this.idx = idx;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("makeclosure", dst, idx);
    }
}


class IGeta extends BCode {
    IGeta(Register dst) {
		super(dst);
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.A;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("geta", dst);
    }
}
class ISeta extends BCode {
    Register src;
    ISeta(Register src) {
    		this.src = src;
        this.store = StoreArgType.A;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    	}
    public String toString() {
        return super.toString("seta", src);
    }
}

class IRet extends BCode {
    IRet() {
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

class IMove extends BCode {
    Register src;
    IMove(Register dst, Register src) {
		super(dst);
        this.src = src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("move", dst, src);
    }
}

class IIsundef extends BCode {
    Register src;
    IIsundef(Register dst, Register src) {
		super(dst);
        this.src = src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("isundef", dst, src);
    }
}
class IIsobject extends BCode {
    Register src;
    IIsobject(Register dst, Register src) {
		super(dst);
        this.src = src;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("isobject", dst, src);
    }
}
class IInstanceof extends BCode {
    Register src1, src2;
    IInstanceof(Register dst, Register src1, Register src2) {
		super(dst);
        this.src1 = src1;
        this.src2 = src2;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.REGISTER;
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
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
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
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("send", callee, numOfArgs);
    }
}
class INew extends BCode {
    Register constructor;
    INew(Register dst, Register constructor) {
		super(dst);
        this.constructor = constructor;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
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
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    public String toString() {
        return super.toString("newsend", constructor, numOfArgs);
    }
}
class IMakesimpleiterator extends BCode {
    Register obj;
    IMakesimpleiterator(Register obj, Register dst) {
        super(dst);
        this.obj = obj;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("makesimpleiterator", dst, obj);
    }
}
class INextpropnameidx extends BCode {
    Register ite;
    INextpropnameidx(Register ite, Register dst) {
        super(dst);
        this.ite = ite;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("nextpropnameidx", dst, ite);
    }
}
// Jump instructions
class IJump extends BCode {
    Label label;
    IJump(Label label) {
    		this.label = label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    	}
    @Override
    public boolean isFallThroughInstruction() {
    		return false;
    }
    @Override
    public BCode getBranchTarget() {
    		return label.getDestBCode();
    }
    public String toString() {
        return super.toString("jump", label.dist(number, this.getArgsNum()));
    }
}
class IJumptrue extends BCode {
    Register test;
    Label label;
    IJumptrue(Register test, Label label) {
        this.test = test;
        this.label = label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    @Override
    public BCode getBranchTarget() {
    		return label.getDestBCode();
    }
    public String toString() {
        return super.toString("jumptrue", test, label.dist(number, this.getArgsNum()));
    }
}
class IJumpfalse extends BCode {
    Register test;
    Label label;
    IJumpfalse(Register test, Label label) {
        this.test = test;
        this.label = label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.LITERAL;
    }
    @Override
    public BCode getBranchTarget() {
    		return label.getDestBCode();
    }
    public String toString() {
        return super.toString("jumpfalse", test, label.dist(number, this.getArgsNum()));
    }
}


class IThrow extends BCode {
    Register reg;
    IThrow(Register reg) {
        this.reg = reg;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.REGISTER;
        this.load2 = LoadArgType.NONE;
    }
    @Override
    public boolean isFallThroughInstruction()  {
    		return false;
    }
    public String toString() {
        return super.toString("throw", reg);
    }
}
class IPushhandler extends BCode {
    Label label;
    IPushhandler(Label label) {
        this.label = label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("pushhandler", label.dist(number, this.getArgsNum()));
    }
}
class IPophandler extends BCode {
	IPophandler() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
	}
    public String toString() {
        return super.toString("pophandler");
    }
}
class ILocalcall extends BCode {
    Label label;
    ILocalcall(Label label) {
        this.label = label;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    }
    public String toString() {
        return super.toString("localcall", label.dist(number, this.getArgsNum()));
    }
}
class ILocalret extends BCode {
	ILocalret() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
	}
	@Override
	public boolean isFallThroughInstruction() {
		return false;
	}
    public String toString() {
        return super.toString("localret");
    }
}
class IPoplocal extends BCode {
	IPoplocal() {
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.NONE;
        this.load2 = LoadArgType.NONE;
	}
    public String toString() {
        return super.toString("poplocal");
    }
}



class ISetfl extends BCode {
    int fl;
    ISetfl(int fl) {
    		this.fl = fl;
        this.store = StoreArgType.NONE;
        this.load1 = LoadArgType.LITERAL;
        this.load2 = LoadArgType.NONE;
    	}
    public String toString() {
        return super.toString("setfl", fl);
    }
}


class IFuncLength extends BCode {
    int n;
    IFuncLength(int n) {
    		this.n = n;
    	}
    public String toString() {
        return super.toString("funcLength", n);
    }
}
class ICallentry extends BCode {
    int n;
    ICallentry(int n) {
    		this.n = n;
    	}
    public String toString() {
        return super.toString("callentry", n);
    }
}
class ISendentry extends BCode {
    int n;
    ISendentry(int n) {
    		this.n = n;
    	}
    public String toString() {
        return super.toString("sendentry", n);
    }
}
class INumberOfLocals extends BCode {
    int n;
    INumberOfLocals(int n) {
    		this.n = n;
    }
    public String toString() {
        return super.toString("numberOfLocals", n);
    }
}
class INumberOfInstruction extends BCode {
    int n;
    INumberOfInstruction(int n) {
    		this.n = n;
    }
    public String toString() {
        return super.toString("numberOfInstruction", n);
    }
}
class INumberOfArgument extends BCode {
    int n;
    INumberOfArgument(int n) {
        this.n = n;
    }
    public String toString() {
        return super.toString("numberOfArgument", n);
    }
}


class IError extends BCode {
    String str;
    IError(Register dst, String str) {
		super(dst);
        this.str = str;
        this.store = StoreArgType.REGISTER;
        this.load1 = LoadArgType.STRING;
        this.load2 = LoadArgType.NONE;
    }
    @Override
    public boolean isFallThroughInstruction() {
    		return false;
    }
    public String toString() {
        return super.toString("error", dst, str);
    }
}

class MSetfl extends BCode {
	MSetfl() {}
	@Override
	public String toString() {
		return "@MACRO setfl";
	}
}

class MCall extends BCode {
	Register receiver;
	Register function;
	Register[] args;
	boolean isNew;
	boolean isTail;
	MCall(Register receiver, Register function, Register[] args, boolean isNew, boolean isTail) {
		this.receiver = receiver;
		this.function = function;
		this.args = args;
		this.isNew = isNew;
		this.isTail = isTail;
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
		String s ="@MACRO ";

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

class MParameter extends BCode {
	MParameter(Register dst) {
		super(dst);
	}
	@Override
	public String toString() {
		return "@MACRO param "+dst;
	}
}
