/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;

public class BCodeEvaluator {
    static abstract class Environment {
        abstract Value lookup(Register r);
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
    static class SpecialValue extends Value {
        enum V {
            TRUE,
            FALSE,
            NULL,
            UNDEFINED
        }
        V v;
        private SpecialValue(V v) {
            this.v = v;
        }
        static SpecialValue TRUE_VALUE = new SpecialValue(V.TRUE);
        static SpecialValue FALSE_VALUE = new SpecialValue(V.FALSE);
        static SpecialValue NULL_VALUE = new SpecialValue(V.NULL);
        static SpecialValue UNDEFINED_VALUE = new SpecialValue(V.UNDEFINED);

        V getSpecialValue() {
            return v;
        }
    }

    public Value operandValue(Environment env, SrcOperand src) {
        if (src instanceof RegisterOperand) {
            Register rs = ((RegisterOperand) src).get();
            return env.lookup(rs);
        } else if (src instanceof FixnumOperand) {
            int n = ((FixnumOperand) src).get();
            return new FixnumValue(n);
        } else if (src instanceof FlonumOperand) {
            double n = ((FlonumOperand) src).get();
            return new NumberValue(n);
        } else if (src instanceof StringOperand) {
            String s = ((StringOperand) src).get();
            return new StringValue(s);
        } else if (src instanceof SpecialOperand) {
            SpecialOperand s = (SpecialOperand) src;
            switch(s.get()) {
            case TRUE:
                return SpecialValue.TRUE_VALUE;
            case FALSE:
                return SpecialValue.FALSE_VALUE;
            case NULL:
                return SpecialValue.NULL_VALUE;
            case UNDEFINED:
                return SpecialValue.UNDEFINED_VALUE;
            default:
                throw new Error("Unknown special");
            }
        } else
            throw new Error("Unknown operand type");
    }

    public Value eval(Environment env, BCode bc) {
        /* constnat */
        if (bc instanceof IFixnum)
            return evalIFixnum(env, (IFixnum) bc);
        if (bc instanceof INumber)
            return evalINumber(env, (INumber) bc);
        if (bc instanceof IString)
            return evalIString(env, (IString) bc);
        if (bc instanceof IBooleanconst)
            return evalIBooleanconst(env, (IBooleanconst) bc);
        if (bc instanceof INullconst)
            return evalINullconst(env, (INullconst) bc);
        if (bc instanceof IUndefinedconst)
            return evalIUndefinedconst(env, (IUndefinedconst) bc);
        if (bc instanceof IMove)
            return evalIMove(env, (IMove) bc);
        if (bc instanceof IAdd)
            return evalIAdd(env, (IAdd) bc);
        if (bc instanceof ISub)
            return evalISub(env, (ISub) bc);
        if (bc instanceof IMul)
            return evalIMul(env, (IMul) bc);
        if (bc instanceof IDiv)
            return evalIDiv(env, (IDiv) bc);
        if (bc instanceof IMod)
            return evalIMod(env, (IMod) bc);
        if (bc instanceof IBitor)
            return evalIBitor(env, (IBitor) bc);
        if (bc instanceof IBitand)
            return evalIBitand(env, (IBitand) bc);
        if (bc instanceof ILeftshift)
            return evalILeftshift(env, (ILeftshift) bc);
        if (bc instanceof IRightshift)
            return evalIRightshift(env, (IRightshift) bc);
        if (bc instanceof IUnsignedrightshift)
            return evalIUnsignedrightshift(env, (IUnsignedrightshift) bc);
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

    protected Value evalIBooleanconst(Environment env, IBooleanconst bc) {
        return bc.b ? SpecialValue.TRUE_VALUE : SpecialValue.FALSE_VALUE;
    }

    protected Value evalINullconst(Environment env, INullconst bc) {
        return SpecialValue.NULL_VALUE;
    }

    protected Value evalIUndefinedconst(Environment env, IUndefinedconst bc) {
        return SpecialValue.UNDEFINED_VALUE;
    }

    protected Value evalIMove(Environment env, IMove bc) {
        return operandValue(env, bc.src);
    }

    protected Value evalIAdd(Environment env, IAdd bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
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

    protected Value evalISub(Environment env, ISub bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof NumberValue && v2 instanceof NumberValue) {
            double n1 = ((NumberValue) v1).getDoubleValue();
            double n2 = ((NumberValue) v2).getDoubleValue();
            double n = n1 - n2;
            if (NumberValue.inFixnumRange(n))
                return new FixnumValue((int) n);
            else
                return new NumberValue(n);
        }
        return null;
    }

    protected Value evalIMul(Environment env, IMul bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof NumberValue && v2 instanceof NumberValue) {
            double n1 = ((NumberValue) v1).getDoubleValue();
            double n2 = ((NumberValue) v2).getDoubleValue();
            double n = n1 * n2;
            if (NumberValue.inFixnumRange(n))
                return new FixnumValue((int) n);
            else
                return new NumberValue(n);
        }
        return null;
    }

    protected Value evalIDiv(Environment env, IDiv bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof NumberValue && v2 instanceof NumberValue) {
            double n1 = ((NumberValue) v1).getDoubleValue();
            double n2 = ((NumberValue) v2).getDoubleValue();
            double n = n1 / n2;
            if (NumberValue.inFixnumRange(n))
                return new FixnumValue((int) n);
            else
                return new NumberValue(n);
        }
        return null;
    }

    protected Value evalIMod(Environment env, IMod bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof NumberValue && v2 instanceof NumberValue) {
            double n1 = ((NumberValue) v1).getDoubleValue();
            double n2 = ((NumberValue) v2).getDoubleValue();
            double n = n1 % n2;
            if (NumberValue.inFixnumRange(n))
                return new FixnumValue((int) n);
            else
                return new NumberValue(n);
        }
        return null;
    }

    protected Value evalIBitor(Environment env, IBitor bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof FixnumValue && v2 instanceof FixnumValue) {
            int n1 = ((FixnumValue) v1).getIntValue();
            int n2 = ((FixnumValue) v2).getIntValue();
            int n = n1 | n2;
            return new FixnumValue((int) n);
        }
        return null;
    }

    protected Value evalIBitand(Environment env, IBitand bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof FixnumValue && v2 instanceof FixnumValue) {
            int n1 = ((FixnumValue) v1).getIntValue();
            int n2 = ((FixnumValue) v2).getIntValue();
            int n = n1 & n2;
            return new FixnumValue((int) n);
        }
        return null;
    }

    protected Value evalILeftshift(Environment env, ILeftshift bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof FixnumValue && v2 instanceof FixnumValue) {
            int n1 = ((FixnumValue) v1).getIntValue();
            int n2 = ((FixnumValue) v2).getIntValue();
            int n = n1 << n2;
            return new FixnumValue((int) n);
        }
        return null;
    }

    protected Value evalIRightshift(Environment env, IRightshift bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof FixnumValue && v2 instanceof FixnumValue) {
            int n1 = ((FixnumValue) v1).getIntValue();
            int n2 = ((FixnumValue) v2).getIntValue();
            int n = n1 >> n2;
            return new FixnumValue((int) n);
        }
        return null;
    }

    protected Value evalIUnsignedrightshift(Environment env, IUnsignedrightshift bc) {
        Value v1 = operandValue(env, bc.src1);
        if (v1 == null)
            return null;
        Value v2 = operandValue(env, bc.src2);
        if (v2 == null)
            return null;
        if (v1 instanceof FixnumValue && v2 instanceof FixnumValue) {
            int n1 = ((FixnumValue) v1).getIntValue();
            int n2 = ((FixnumValue) v2).getIntValue();
            int n = n1 >>> n2;
            return new FixnumValue((int) n);
        }
        return null;
    }
}
