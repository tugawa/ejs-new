package ejsc;

public class BCodeEvaluator {
	static abstract class Environment {
		abstract Value lookup(BCode bc, Register r);
	}

	static class Value {
	}

	static class NumberValue extends Value {
		static boolean inFixnumRange(double n) {
			return n == (int) n;
		}

		double n;
		NumberValue(double n) {
			this.n = n;
		}
		double getDoubleValue() {
			return n;
		}
	}
	static class FixnumValue extends NumberValue {
		FixnumValue(int n) {
			super(n);
		}
		int getIntValue() {
			return (int) n;
		}
	}
	static class StringValue extends Value {
		String s;
		StringValue(String s) {
			this.s = s;
		}
		String getStringValue() {
			return s;
		}
	}
	
	public Value eval(Environment env, BCode bc) {
		if (bc instanceof IFixnum)
			return evalIFixnum(env, (IFixnum) bc);
		if (bc instanceof INumber)
			return evalINumber(env, (INumber) bc);
		if (bc instanceof IString)
			return evalIString(env, (IString) bc);
		if (bc instanceof IMove)
			return evalIMove(env, (IMove) bc);
		if (bc instanceof IAdd)
			return evalIAdd(env, (IAdd) bc);
		return null;
	}
	
	protected Value evalIFixnum(Environment env, IFixnum bc) {
		return new FixnumValue(bc.n);
	}

	protected Value evalINumber(Environment env, INumber bc) {
		return new NumberValue(bc.n);
	}
	
	protected Value evalIString(Environment env, IString bc) {
		return new StringValue(bc.str);
	}
	
	protected Value evalIMove(Environment env, IMove bc) {
		return env.lookup(bc, bc.src);
	}

	protected Value evalIAdd(Environment env, IAdd bc) {
		Value v1 = env.lookup(bc, bc.src1);
		if (v1 == null)
			return null;
		Value v2 = env.lookup(bc, bc.src2);
		if (v2 == null)
			return null;

		if (v1 instanceof NumberValue && v2 instanceof NumberValue) {
			double n1 = ((NumberValue) v1).getDoubleValue();
			double n2 = ((NumberValue) v2).getDoubleValue();
			double n = n1 + n2;
			if (NumberValue.inFixnumRange(n))
				return new FixnumValue((int) n);
			else
				return new NumberValue(n);
		} else if (v1 instanceof StringValue && v2 instanceof StringValue) {
			String s1 = ((StringValue) v1).getStringValue();
			String s2 = ((StringValue) v2).getStringValue();
			String s = s1 + s2;
			return new StringValue(s);
		}
		
		return null;
	}
}
