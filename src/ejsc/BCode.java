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
	CodeMaker cm = new CodeMaker();
    int number;
    protected Register dst;
    ArrayList<Label> labels = new ArrayList<Label>();
    
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
    
    String toString(String opcode) {
        return cm.makecode(opcode);
    }

    String toString(String opcode, Register op1) {
    	return cm.makecode(opcode, op1.n);
    }
    String toString(String opcode, Register op1, Register op2) {
    	return cm.makecode(opcode, op1.n, op2.n);
    }
    String toString(String opcode, Register op1, Register op2, Register op3) {
    	return cm.makecode(opcode, op1.n, op2.n, op3.n);
    }

    String toString(String opcode, Register op1, String op2) {
    	return cm.makecode(opcode, op1.n, op2);
    }

    String toString(String opcode, Register op1, int op2) {
    	return cm.makecode(opcode, op1.n, op2);
    }
    String toString(String opcode, Register op1, double op2) {
    	return cm.makecode(opcode, op1.n, op2) ;
    }
    String toString(String opcode, Register op1, int op2, int op3) {
    	return cm.makecode(opcode, op1.n, op2, op3);
    }
    String toString(String opcode, Register op1, int op2, Register op3) {
    	return cm.makecode(opcode, op1.n, op2, op3.n);
    }
    String toString(String opcode, int op1, int op2, Register op3) {
    	return cm.makecode(opcode, op1, op2, op3.n);
    }

    String toString(String opcode, int op1) {
    	return cm.makecode(opcode, op1);
    }

    String toString(String opcode, int op1, int op2) {
    	return cm.makecode(opcode, op1, op2);
    }

    String toString(String opcode, Register op1, int op2, String op3) {
    	return cm.makecode(opcode, op1.n, op2);
    }
    class CodeMaker {
    	// 読み込む
		String[] insn_table = {
				"fixnum",
				"specconst",
				"string",
				"regexp",
				"number",
				"add",
				"sub",
				"mul",
				"div",
				"mod",
				"bitand",
				"bitor",
				"leftshift",
				"rightshift",
				"unsignedrightshift",
				"lessthan",
				"lessthanequal",
				"eq",
				"equal",
				"getarg",
				"setarg",
				"getprop",
				"setprop",
				"setarray",
				"getglobal",
				"setglobal",
				"instanceof",
				"move",
				"typeof",
				"not",
				"new",
				"getidx",
				"isundef",
				"isobject",
				"setfl",
				"seta",
				"geta",
				"geterr",
				"getglobalobj",
				"newframe",
				"ret",
				"nop",
				"jump",
				"jumptrue",
				"jumpfalse",
				"getlocal",
				"setlocal",
				"makeclosure",
				"makeiterator",
				"nextpropname",
				"send",
				"newsend",
				"call",
				"tailsend",
				"tailcall",
				"pushhandler",
				"pophandler",
				"throw",
				"localcall",
				"localret",
				"poplocal",
				"error",
				"unknown",
				"end",
				"hoge"
		};
		
		
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
					value & 0xffff, 0) + names(bins);
		}
		
		String names(int[] bins) {
			int i;
			String str="";
			for(i=0;bins[i]!=0;i++) {
				if((bins[i] & 0xffff)==0)
					str = str + String.format("02x", bins[i]>>8);
				str = str + String.format("%04x", bins[i]);
			}
			//System.out.println(str);
			return str;
		}
		
		String makecode(String opcode, int op1, double op2) {
			return makecode(makeopcode(opcode), op1,
					8, 0) + nums(op2);
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
			int i;
			for(i=0;i<insn_table.length;i++) {
				if(opcode.equals(insn_table[i]))
					return i;
			}
			return -1;
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
    public BCode getDestBCode() {
    		return bcode;
    }
    public void replaceDestBCode(BCode bcode) {
    		this.bcode = bcode;
    }
}


class IFixnum extends BCode {
    int n;
    IFixnum(Register dst, int n) {
    		super(dst);
        this.n = n;
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
    }
    public String toString() {
        return super.toString("specconst", dst, b ? "true" : "false");
    }
}
class INullconst extends BCode {
    INullconst(Register dst) {
        super(dst);
    }
    public String toString() {
        return super.toString("specconst", dst, "null");
    }
}
class IUndefinedconst extends BCode {
    IUndefinedconst(Register dst) {
        super(dst);
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
    }
    public String toString() {
        return super.toString("not", dst, src);
    }
}




class IGetglobalobj extends BCode {
    IGetglobalobj(Register dst) {
		super(dst);
    }
    public String toString() {
        return super.toString("getglobalobj", dst);
    }
}
class INewargs extends BCode {
    INewargs() {
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
    int depth, n;
    IGetlocal(Register dst, int depth, int n) {
		super(dst);
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
    int depth, n;
    IGetarg(Register dst, int depth, int n) {
		super(dst);
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
    Register obj, prop;
    IGetprop(Register dst, Register obj, Register prop) {
		super(dst);
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
    int idx;
    IMakeclosure(Register dst, int idx) {
		super(dst);
        this.idx = idx;
    }
    public String toString() {
        return super.toString("makeclosure", dst, idx);
    }
}


class IGeta extends BCode {
    IGeta(Register dst) {
		super(dst);
    }
    public String toString() {
        return super.toString("geta", dst);
    }
}
class ISeta extends BCode {
    Register src;
    ISeta(Register src) {
    		this.src = src;
    	}
    public String toString() {
        return super.toString("seta", src);
    }
}

class IRet extends BCode {
    IRet() {
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
    Register constructor;
    INew(Register dst, Register constructor) {
		super(dst);
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
class IMakesimpleiterator extends BCode {
    Register obj;
    IMakesimpleiterator(Register obj, Register dst) {
        super(dst);
        this.obj = obj;
    }
    public String toString() {
        return super.toString("makesimpleiterator", obj, dst);
    }
}
class INextpropnameidx extends BCode {
    Register ite;
    INextpropnameidx(Register ite, Register dst) {
        super(dst);
        this.ite = ite;
    }
    public String toString() {
        return super.toString("nextpropnameidx", ite, dst);
    }
}
// Jump instructions
class IJump extends BCode {
    Label label;
    IJump(Label label) {
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
    @Override
    public BCode getBranchTarget() {
    		return label.getDestBCode();
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
    @Override
    public BCode getBranchTarget() {
    		return label.getDestBCode();
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
    }
    public String toString() {
        return super.toString("pushhandler", label.dist(number));
    }
}
class IPophandler extends BCode {
	IPophandler() {
	}
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
	ILocalret() {
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
	}
    public String toString() {
        return super.toString("poplocal");
    }
}



class ISetfl extends BCode {
    int fl;
    ISetfl(int fl) {
    		this.fl = fl;
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


class IError extends BCode {
    String str;
    IError(Register dst, String str) {
		super(dst);
        this.str = str;
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
