/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import ejsc.BCodeEvaluator.FixnumValue;
import ejsc.BCodeEvaluator.NumberValue;
import ejsc.BCodeEvaluator.SpecialValue;
import ejsc.BCodeEvaluator.StringValue;
import ejsc.BCodeEvaluator.Value;
import ejsc.Main.Info.SISpecInfo.SISpec;

public class ConstantPropagation {
    static final boolean DEBUG = false;

    static class ConstantEvaluator extends BCodeEvaluator {
        // TODO: make modular
        static final int FIXNUM_OPERNAD_MIN = -0x8000;
        static final int FIXNUM_OPERNAD_MAX = 0x7fff;

        class Environment extends BCodeEvaluator.Environment {
            BCode bc;

            Environment(BCode bc) {
                this.bc = bc;
            }

            @Override
            public BCodeEvaluator.Value lookup(Register r) {
                return findAndEvalDefinition(bc, r);
            }

            private BCode findDefinition(BCode bc, Register src) {
                BCode result = null;
                for (BCode def: rdefa.getReachingDefinitions(bc)) {
                    if (def.getDestRegister() == src) {
                        if (result == null)
                            result = def;
                        else
                            return null;
                    }
                }
                return result;
            }

            private Value findAndEvalDefinition(BCode bc, Register src) {
                BCode def = findDefinition(bc, src);
                if (def == null)
                    return null;
                return eval(new Environment(def), def);
            }
        }

        ReachingDefinition rdefa;

        ConstantEvaluator(ReachingDefinition rdefa) {
            this.rdefa = rdefa;
        }

        public SrcOperand evalSrcOperand(Environment env, SrcOperand src) {
            if (src instanceof RegisterOperand) {
                Value v = operandValue(env, src);
                if (v == null)
                    return src;
                if (v instanceof FixnumValue) {
                    int n = ((FixnumValue) v).getIntValue();
                    if (FIXNUM_OPERNAD_MIN <= n && n <= FIXNUM_OPERNAD_MAX)
                        return new FixnumOperand(n);
                    else
                        return src;
                } else if (v instanceof NumberValue)
                    return new FlonumOperand(((NumberValue) v).getDoubleValue());
                else if (v instanceof StringValue)
                    return new StringOperand(((StringValue) v).getStringValue());
                else if (v instanceof SpecialValue) {
                    SpecialValue s = (SpecialValue) v;
                    switch (s.getSpecialValue()) {
                    case TRUE:
                        return new SpecialOperand(SpecialOperand.V.TRUE);
                    case FALSE:
                        return new SpecialOperand(SpecialOperand.V.FALSE);
                    case NULL:
                        return new SpecialOperand(SpecialOperand.V.NULL);
                    case UNDEFINED:
                        return new SpecialOperand(SpecialOperand.V.UNDEFINED);
                    default:
                        throw new Error("Unknown special value");
                    }
                } else
                    throw new Error("Unknown value type");
            } else
                return src;
        }
    }

    List<BCode> bcodes;
    ReachingDefinition rdefa;
    ConstantEvaluator evaluator;

    ConstantPropagation(List<BCode> bcodes) {
        this.bcodes = bcodes;
        rdefa = new ReachingDefinition(bcodes);
        evaluator = new ConstantEvaluator(rdefa);
    }

    private BCode createConstantInstruction(Register r, Value v) {
        if (v instanceof FixnumValue)
            return new IFixnum(r, ((FixnumValue) v).getIntValue());
        else if (v instanceof NumberValue)
            return new INumber(r, ((NumberValue) v).getDoubleValue());
        else if (v instanceof StringValue)
            return new IString(r, ((StringValue) v).getStringValue());
        else if (v instanceof SpecialValue) {
            SpecialValue s = (SpecialValue) v;
            switch (s.getSpecialValue()) {
            case TRUE:
                return new IBooleanconst(r, true);
            case FALSE:
                return new IBooleanconst(r, false);
            case NULL:
                return new INullconst(r);
            case UNDEFINED:
                return new IUndefinedconst(r);
            default:
                throw new Error("Unknown special value");
            }
        }
        return null;
    }


    boolean isTypeInstance(String type, SrcOperand v) {
        switch(type) {
        case "fixnum":
            return v instanceof FixnumOperand;
        case "string":
            return v instanceof StringOperand;
        case "flonum":
            return v instanceof FlonumOperand;
        case "special":
            return v instanceof SpecialOperand;
        default:
            return false;
        }
    }

    SrcOperand[] findMostSpecificOperands(ConstantEvaluator.Environment env, List<SISpec> sis, SrcOperand[] ops) {
        SrcOperand[] vs = new SrcOperand[ops.length];
        for (int i = 0; i < ops.length; i++) {
            if (ops[i] != null)
                vs[i] = evaluator.evalSrcOperand(env, ops[i]);
        }
        if (DEBUG) {
            System.out.print("vs =");
            for (SrcOperand v: vs) {
                System.out.print(" ");
                System.out.print(v);
            }
            System.out.println();
        }

        SrcOperand[] result = new SrcOperand[ops.length];
        System.arraycopy(ops, 0, result, 0, ops.length);
        NEXT_SI: for (SISpec si: sis) {
            String[] siTypes = new String[] {
                    si.op0, si.op1, si.op2
            };
            SrcOperand[] candidate = new SrcOperand[ops.length];

            for (int i = 0; i < ops.length; i++) {
                if (siTypes[i].equals("-")) {
                    assert(ops[i] == null);
                    continue;
                }
                if (isTypeInstance(siTypes[i], vs[i]))
                    candidate[i] = vs[i];
                else if (siTypes[i].equals("_") && result[i] instanceof RegisterOperand)
                    candidate[i] = result[i];
                else
                    continue NEXT_SI;
            }
            if (DEBUG) {
                System.out.print("match");
                for (SrcOperand v: candidate) {
                    System.out.print(" ");
                    System.out.print(v);
                }
                System.out.println();
            }

            result = candidate;
            /* continue iteration to find more specific result */
        }
        return result;
    }

    BCode makeSuperinsn(ConstantEvaluator.Environment env, BCode bcx) {
        List<SISpec> sis = Main.Info.SISpecInfo.getSISpecsByInsnName(bcx.name);
        if (sis.isEmpty())
            return null;

        if (bcx instanceof INot) {
            INot bc = (INot) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {null, bc.src});
            return new INot(bc.dst, ops[1]);
        } else if (bcx instanceof IGetglobal) {
            IGetglobal bc = (IGetglobal) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {null, bc.varName});
            return new IGetglobal(bc.dst, ops[1]);
        } else if (bcx instanceof ISetglobal) {
            ISetglobal bc = (ISetglobal) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {bc.varName, bc.src});
            return new ISetglobal(ops[0], ops[1]);
        } else if (bcx instanceof ISetlocal) {
            ISetlocal bc = (ISetlocal) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {null, null, bc.src});
            return new ISetlocal(bc.link, bc.index, ops[2]);
        } else if (bcx instanceof ISetarg) {
            ISetarg bc = (ISetarg) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {null, null, bc.src});
            return new ISetarg(bc.link, bc.index, ops[2]);
        } else if (bcx instanceof IGetprop) {
            IGetprop bc = (IGetprop) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {null, bc.obj, bc.prop});
            return new IGetprop(bc.dst, ops[1], ops[2]);
        } else if (bcx instanceof ISetprop) {
            ISetprop bc = (ISetprop) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {bc.obj, bc.prop, bc.src});
            return new ISetprop(ops[0], ops[1], ops[2]);
        } else if (bcx instanceof ISetarray) {
            ISetarray bc = (ISetarray) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {bc.ary, null, bc.src});
            return new ISetarray(ops[0], bc.n, ops[2]);
        } else if (bcx instanceof ISeta) {
            ISeta bc = (ISeta) bcx;
            SrcOperand[] ops = findMostSpecificOperands(env, sis, new SrcOperand[] {bc.src});
            return new ISeta(ops[0]);
        } else {
            /* TODO: do not use reflection */
            Class<? extends BCode>[] rxx = new Class[] {
                    IAdd.class, ISub.class, IMul.class, IDiv.class, IMod.class,
                    IBitor.class, IBitand.class, ILeftshift.class, IRightshift.class,
                    IUnsignedrightshift.class, IEqual.class, IEq.class,
                    ILessthan.class, ILessthanequal.class
            };
            for (Class<? extends BCode> cls: rxx) {
                if (cls.isInstance(bcx)) {
                    try {
                        Register dst = (Register) BCode.class.getDeclaredField("dst").get(bcx);
                        SrcOperand[] srcOperands = new SrcOperand[] {
                                null,
                                (SrcOperand) cls.getDeclaredField("src1").get(bcx),
                                (SrcOperand) cls.getDeclaredField("src2").get(bcx)
                        };
                        SrcOperand[] ops = findMostSpecificOperands(env, sis, srcOperands);
                        Constructor<? extends BCode> ctor = cls.getDeclaredConstructor(Register.class, SrcOperand.class, SrcOperand.class);
                        return (BCode) ctor.newInstance(dst, ops[1], ops[2]);
                    } catch (Exception e) {
                        throw new Error(e);
                    }
                }
            }
        }
        return null;
    }

    public List<BCode> exec() {
        List<BCode> newBCodes = new ArrayList<BCode>(bcodes.size());

        for (BCode bc: bcodes) {
            ConstantEvaluator.Environment env = evaluator.new Environment(bc);
            Value v = evaluator.eval(env, bc);
            if (v != null) {
                BCode newBC = createConstantInstruction(bc.getDestRegister(), v);
                newBC.addLabels(bc.getLabels());
                newBCodes.add(newBC);
            } else {
                BCode newBC = makeSuperinsn(env, bc);
                if (newBC != null) {
                    newBC.addLabels(bc.getLabels());
                    newBCodes.add(newBC);
                } else
                    newBCodes.add(bc);
            }
        }

        return newBCodes;
    }
}
