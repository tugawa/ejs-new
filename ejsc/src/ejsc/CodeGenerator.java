/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ejsc.IASTNode.ArgumentsVarLoc;
import ejsc.IASTNode.ParameterVarDecl;
import ejsc.IASTNode.VarDecl;

public class CodeGenerator extends IASTBaseVisitor {

    static class Continuation {
        Continuation k;
        public void emitBreak(String labelName) {
            k.emitBreak(labelName);
        }
        public void emitContinue(String labelName) {
            k.emitContinue(labelName);
        }
        public void emitReturn(Register r) {
            k.emitReturn(r);
        }
        public void emitThrow(Register r) {
            k.emitThrow(r);
        }
        public void setPrev(Continuation k) {
            this.k = k;
        }
        public Continuation getPrev() {
            return k;
        }
    }

    class LoopContinuation extends Continuation {
        private Label breakLabel, continueLabel;
        private String labelName;
        public LoopContinuation(String labelName, Label breakLabel, Label continueLabel) {
            this.labelName = labelName;
            this.breakLabel = breakLabel;
            this.continueLabel = continueLabel;
        }
        @Override
        public void emitBreak(String name) {
            if (name != null && !labelName.equals(name))
                super.emitBreak(name);
            bcBuilder.push(new IJump(breakLabel));
        }
        @Override
        public void emitContinue(String name) {
            if (name != null && !labelName.equals(name))
                super.emitContinue(name);
            bcBuilder.push(new IJump(continueLabel));
        }
    }

    class SwitchContinuation extends Continuation {
        private Label breakLabel;
        public SwitchContinuation(Label breakLabel) {
            this.breakLabel = breakLabel;
        }
        @Override
        public void emitBreak(String name) {
            if (name != null)
                super.emitBreak(name);
            bcBuilder.push(new IJump(breakLabel));
        }
    }

    class FunctionContinuation extends Continuation {
        FunctionContinuation() {
        }
        @Override
        public void emitBreak(String name) {
            throw new Error("No enclosing loop/switch for `break'");
        }
        @Override
        public void emitContinue(String name) {
            throw new Error("No enclosing loop for `continue'");
        }
        @Override
        public void emitReturn(Register r) {
            if (r != null)
                bcBuilder.push(new ISeta(r));
            bcBuilder.push(new IRet());
        }
        @Override
        public void emitThrow(Register r) {
            bcBuilder.push(new IThrow(r));
        }
    }

    class TryCatchContinuation extends Continuation {
        public TryCatchContinuation() {
        }
        @Override
        public void emitBreak(String name) {
            bcBuilder.push(new IPophandler());
            super.emitBreak(name);
        }
        @Override
        public void emitContinue(String name) {
            bcBuilder.push(new IPophandler());
            super.emitContinue(name);
        }
        @Override
        public void emitReturn(Register r) {
            bcBuilder.push(new IPophandler());
            super.emitReturn(r);
        }
    }

    class TryFinallyContinuation extends Continuation {
        Label finallyLabel;
        public TryFinallyContinuation(Label finallyLabel) {
            this.finallyLabel = finallyLabel;
        }
        private void localCall() {
            bcBuilder.push(new IPophandler());
            bcBuilder.push(new ILocalcall(finallyLabel));
        }
        @Override
        public void emitBreak(String name) {
            localCall();
            super.emitBreak(name);
        }
        @Override
        public void emitContinue(String name) {
            localCall();
            super.emitContinue(name);
        }
        @Override
        public void emitReturn(Register r) {
            localCall();
            super.emitReturn(r);
        }
        @Override
        public void emitThrow(Register r) {
            localCall();
            super.emitThrow(r);
        }
    }

    static class Environment {
        // Frame is made for each IASTFunction.
        static class Frame {
            /* map of register number to interned Register */
            private ArrayList<Register> registers = new ArrayList<Register>();
            private List<Register> argumentRegisters = new ArrayList<Register>();
            private Continuation continuation;

            private Register allocateRegister(int regNo) {
                int index = regNo - 1;  // register number is 1-origin
                if (registers.size() <= index) {
                    registers.ensureCapacity(index + 1);
                    for (int i = registers.size(); i <= index; i++)
                        registers.add(new Register(i + 1));
                }
                return registers.get(index);
            }

            public Register getRegister(int regNo) {
                return registers.get(regNo - 1);
            }

            public Register freshRegister() {
                Register r = new Register(registers.size());
                registers.add(r);
                return r;
            }

            private void allocateRegisterTo(IASTNode.VarLoc locx) {
                if (locx instanceof IASTNode.RegisterVarLoc) {
                    IASTNode.RegisterVarLoc loc = (IASTNode.RegisterVarLoc) locx;
                    loc.setRegisterNo(freshRegister().getRegisterNumber());
                }
            }

            Frame(IASTFunctionExpression func) {
                /* create input registers */
                allocateRegister(THIS_OBJECT_REGISTER);
                for (ParameterVarDecl decl: func.params)
                    allocateRegister(THIS_OBJECT_REGISTER + 1 + decl.getParameterIndex());

                /* allocate registers to not escaping variables */
                for (VarDecl decl: func.params)
                    allocateRegisterTo(decl.getLocation());
                for (VarDecl decl: func.locals)
                    allocateRegisterTo(decl.getLocation());
            }

            public void close() {
                int top = registers.size() + argumentRegisters.size();
                for (int i = 0; i < argumentRegisters.size(); i++)
                    argumentRegisters.get(i).setRegisterNumber(top - i);
                assert(continuation == null);
            }

            public void pushContinuation(Continuation k) {
                k.setPrev(continuation);
                continuation = k;
            }

            public void popContinuation() {
                continuation = continuation.getPrev();
            }

            public Continuation getContinuation() {
                return continuation;
            }

            int getNumberOfGPRegisters() {
                return registers.size();
            }
        }

        private LinkedList<Frame> frameList = new LinkedList<Frame>();

        Environment() {
        }

        private Frame getCurrentFrame() {
            return frameList.getFirst();
        }

        public void openFrame(IASTFunctionExpression func) {
            frameList.addFirst(new Frame(func));
        }

        public void closeFrame() {
            getCurrentFrame().close();
            this.frameList.removeFirst();
        }

        public int getNumberOfGPRegisters() {
            return getCurrentFrame().getNumberOfGPRegisters();
        }

        public Register getRegister(int regNo) {
            return getCurrentFrame().getRegister(regNo);
        }

        public Register freshRegister() {
            return getCurrentFrame().freshRegister();
        }

        public void pushContinuation(Continuation k) {
            getCurrentFrame().pushContinuation(k);
        }

        public void popContinuation() {
            getCurrentFrame().popContinuation();
        }

        public Continuation getContinuation() {
            return getCurrentFrame().getContinuation();
        }

    }

    public CodeGenerator(Main.Info info) {
        this.info = info;
        optLocals = info.optLocals;
    }

    BCBuilder bcBuilder;
    Environment env;
    Register reg;
    Main.Info info;
    Main.Info.OptLocals optLocals;
    static final int THIS_OBJECT_REGISTER = 1;

    void printByteCode(List<BCode> bcodes) {
        for (BCode bcode : bcodes) {
            System.out.println(bcode);
        }
    }

    public BCBuilder compile(IASTProgram node) {
        this.bcBuilder = new BCBuilder();
        this.env = new Environment();
        try {
            compileNode(node, null);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return bcBuilder;
    }

    void compileNode(IASTNode node, Register reg) {
        Register tmp = this.reg;
        this.reg = reg;
        node.accept(this);
        this.reg = tmp;
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        for (IASTFunctionExpression program: node.programs) {
            bcBuilder.openFunctionBCBuilder();
            env.openFrame(program);
            env.pushContinuation(new FunctionContinuation());
            bcBuilder.setTopLevel();
            bcBuilder.setLogging(program.logging);
            Label callEntry = new Label();
            Label sendEntry = new Label();
            bcBuilder.setEntry(callEntry, sendEntry);
            bcBuilder.push(callEntry);
            bcBuilder.push(sendEntry);
            bcBuilder.push(new IGetglobalobj(env.getRegister(THIS_OBJECT_REGISTER)));
            bcBuilder.pushMsetfl();
            Register retReg = env.freshRegister();
            bcBuilder.push(new IUndefinedconst(retReg)); // default value
            compileNode(program.body, retReg);
            bcBuilder.push(new ISeta(retReg));
            /* do not put iret for top-level program */
            bcBuilder.setNumberOfLocals(program.frameSize);
            bcBuilder.setNumberOfGPRegisters(env.getNumberOfGPRegisters());
            env.popContinuation();
            env.closeFrame();
            bcBuilder.closeFuncBCBuilder();
        }        

        return null;
    }

    @Override
    public Object visitStringLiteral(IASTStringLiteral node) {
        bcBuilder.push(new IString(reg, node.value));
        return null;
    }

    @Override
    public Object visitNumericLiteral(IASTNumericLiteral node) {
        if (node.isInteger()) {
            bcBuilder.push(new IFixnum(reg, (int) node.value));
        } else {
            bcBuilder.push(new INumber(reg, node.value));
        }
        return null;
    }

    @Override
    public Object visitBooleanLiteral(IASTBooleanLiteral node) {
        bcBuilder.push(new IBooleanconst(reg, node.value));
        return null;
    }

    @Override
    public Object visitNullLiteral(IASTNullLiteral node) {
        bcBuilder.push(new INullconst(reg));
        return null;
    }

    @Override
    public Object visitRegExpLiteral(IASTRegExpLiteral node) {
        bcBuilder.push(new IRegexp(reg, 0, node.pattern));
        return null;
    }

    @Override
    public Object visitBlockStatement(IASTBlockStatement node) {
        bcBuilder.push(new IUndefinedconst(reg));
        for (IASTStatement stmt : node.stmts)
            compileNode(stmt, reg);
        return null;
    }

    @Override
    public Object visitReturnStatement(IASTReturnStatement node) {
        if (node.value == null) {
            bcBuilder.push(new IUndefinedconst(reg));
        } else {
            compileNode(node.value, reg);
        }
        env.getCurrentFrame().getContinuation().emitReturn(reg);
        return null;
    }

    @Override
    public Object visitWithStatement(IASTWithStatement node) {
        throw new UnsupportedOperationException("WithStatement has not been implemented yet.");

    }

    @Override
    public Object visitEmptyStatement(IASTEmptyStatement node) {
        return null;
    }

    @Override
    public Object visitIfStatement(IASTIfStatement node) {
        Label l1 = new Label();
        compileNode(node.test, reg);
        bcBuilder.push(new IJumpfalse(l1, reg));
        compileNode(node.consequent, reg);
        if (node.alternate == null) {
            bcBuilder.push(l1);
        } else {
            Label l2 = new Label();
            bcBuilder.push(new IJump(l2));
            bcBuilder.push(l1);
            compileNode(node.alternate, reg);
            bcBuilder.push(l2);
        }
        return null;
    }

    @Override
    public Object visitSwitchStatement(IASTSwitchStatement node) {
        Register discReg = env.getCurrentFrame().freshRegister();
        compileNode(node.discriminant, discReg);
        LinkedList<Label> caseLabels = new LinkedList<Label>();
        Register testReg = env.getCurrentFrame().freshRegister();
        for (IASTSwitchStatement.CaseClause caseClause : node.cases) {
            if (caseClause.test != null) {
                Label caseLabel = new Label();
                compileNode(caseClause.test, testReg);
                bcBuilder.push(new IEq(reg, discReg, testReg));
                bcBuilder.push(new IJumptrue(caseLabel, reg));
                caseLabels.add(caseLabel);
            } else {
                Label caseLabel = new Label();
                bcBuilder.push(new IJump(caseLabel));
                caseLabels.add(caseLabel);
                break;
            }
        }
        Label breakLabel = new Label();
        env.getCurrentFrame().pushContinuation(new SwitchContinuation(breakLabel));
        for (IASTSwitchStatement.CaseClause caseClause : node.cases) {
            Label caseLabel = caseLabels.pollFirst();
            if (caseLabel != null) {
                bcBuilder.push(caseLabel);
            }
            compileNode(caseClause.consequent, testReg);
        }
        env.getCurrentFrame().popContinuation();
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitExpressionStatement(IASTExpressionStatement node) {
        compileNode(node.exp, reg);
        return null;
    }

    @Override
    public Object visitThrowStatement(IASTThrowStatement node) {
        Register r = env.freshRegister();
        compileNode(node.value, r);
        env.getContinuation().emitThrow(r);
        return null;
    }

    @Override
    public Object visitTryCatchStatement(IASTTryCatchStatement node) {
        Label l1 = new Label();
        Label l2 = new Label();
        bcBuilder.push(new IPushhandler(l1));
        env.pushContinuation(new TryCatchContinuation());
        compileNode(node.body, reg);
        env.popContinuation();
        bcBuilder.push(new IJump(l2));

        bcBuilder.push(l1);
        bcBuilder.pushMsetfl();
        IASTNode.VarLoc locx = node.var.getLocation();
        if (locx instanceof IASTNode.FrameVarLoc) {
            IASTNode.FrameVarLoc loc = (IASTNode.FrameVarLoc) locx;
            Register r = env.freshRegister();
            bcBuilder.push(new INewframe(1, false));
            bcBuilder.push(new IGeta(r));
            bcBuilder.push(new ISetlocal(0, loc.getIndex(), r));
            compileNode(node.handler, reg);
            bcBuilder.push(l2);
            bcBuilder.push(new IExitframe());
        } else if (locx instanceof IASTNode.RegisterVarLoc) {
            IASTNode.RegisterVarLoc loc = (IASTNode.RegisterVarLoc) locx;
            Register r = env.freshRegister();
            loc.setRegisterNo(r.getRegisterNumber());
            bcBuilder.push(new IGeta(r));
            compileNode(node.handler, reg);
            bcBuilder.push(l2);
        } else
            throw new Error("Unexpected VarLoc");
        return null;
    }

    @Override
    public Object visitTryFinallyStatement(IASTTryFinallyStatement node) {
        throw new UnsupportedOperationException("TryFinallyStatement has not been implemented yet.");
    }

    @Override
    public Object visitForStatement(IASTForStatement node) {
        if (node.init != null)
            compileNode(node.init, env.getCurrentFrame().freshRegister());
        Label l1 = new Label();
        Label l2 = new Label();
        Label continueLabel = new Label();
        Label breakLabel = new Label();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(l2);
        if (node.body != null) {
            env.getCurrentFrame().pushContinuation(new LoopContinuation(node.label, breakLabel, continueLabel));
            compileNode(node.body, reg);
            env.getCurrentFrame().popContinuation();
        }
        bcBuilder.push(continueLabel);
        if (node.update != null)
            compileNode(node.update, env.getCurrentFrame().freshRegister());
        bcBuilder.push(l1);
        Register testReg = env.getCurrentFrame().freshRegister();
        if (node.test != null) {
            compileNode(node.test, testReg);
            bcBuilder.push(new IJumptrue(l2, testReg));
        } else {
            bcBuilder.push(new IJump(l2));
        }
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitWhileStatement(IASTWhileStatement node) {
        Label l1 = new Label();
        Label l2 = new Label();
        Label breakLabel = new Label();
        Label continueLabel = new Label();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(l2);
        if (node.body != null) {
            env.getCurrentFrame().pushContinuation(new LoopContinuation(node.label, breakLabel, continueLabel));
            compileNode(node.body, reg);
            env.getCurrentFrame().popContinuation();
        }
        bcBuilder.push(continueLabel);
        bcBuilder.push(l1);
        Register testReg = env.getCurrentFrame().freshRegister();
        compileNode(node.test, testReg);
        bcBuilder.push(new IJumptrue(l2, testReg));
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitDoWhileStatement(IASTDoWhileStatement node) {
        Label l1 = new Label();
        Label breakLabel = new Label();
        Label continueLabel = new Label();
        bcBuilder.push(continueLabel);
        bcBuilder.push(l1);
        if (node.body != null) {
            env.getCurrentFrame().pushContinuation(new LoopContinuation(node.label, breakLabel, continueLabel));
            compileNode(node.body, reg);
            env.getCurrentFrame().popContinuation();
        }
        Register testReg = env.getCurrentFrame().freshRegister();
        compileNode(node.test, testReg);
        bcBuilder.push(new IJumptrue(l1, testReg));
        bcBuilder.push(breakLabel);
        return null;
    }

    @Override
    public Object visitForInStatement(IASTForInStatement node) {
        Register objReg = env.getCurrentFrame().freshRegister();
        Register iteReg = env.getCurrentFrame().freshRegister();
        Register propReg = env.getCurrentFrame().freshRegister();
        Register testReg = env.getCurrentFrame().freshRegister();
        Label breakLabel = new Label();
        Label continueLabel = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        compileNode(node.object, objReg);
        bcBuilder.push(new IMakeiterator(objReg, iteReg));
        bcBuilder.push(l1);
        bcBuilder.push(new INextpropnameidx(iteReg, propReg));
        compileSetVariable(node.var, propReg);
        bcBuilder.push(new IIsundef(testReg, propReg));
        bcBuilder.push(new IJumptrue(l2, testReg));
        bcBuilder.push(continueLabel);
        env.getCurrentFrame().pushContinuation(new LoopContinuation(node.label, breakLabel, continueLabel));
        compileNode(node.body, reg);
        env.getCurrentFrame().popContinuation();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(breakLabel);
        bcBuilder.push(l2);
        return null;
    }

    @Override
    public Object visitBreakStatement(IASTBreakStatement node) {
        env.getCurrentFrame().getContinuation().emitBreak(node.label);
        return null;
    }

    @Override
    public Object visitContinueStatement(IASTContinueStatement node) {
        env.getCurrentFrame().getContinuation().emitContinue(node.label);
        return null;
    }

    // precondition: node.params and node.locals are disjoint
    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        BCBuilder.FunctionBCBuilder compiledFunction = bcBuilder.openFunctionBCBuilder();
        env.openFrame(node);
        env.getCurrentFrame().pushContinuation(new FunctionContinuation());

        /*
         * Pseudo instruction to mark input registers
         */
        bcBuilder.push(new MParameter(env.getRegister(THIS_OBJECT_REGISTER)));
        for (ParameterVarDecl decl: node.params)
            bcBuilder.push(new MParameter(env.getRegister(THIS_OBJECT_REGISTER + 1 + decl.getParameterIndex())));

        /*
         * Create entry point
         */
        bcBuilder.setLogging(node.logging);
        Label callEntry = new Label();
        Label sendEntry = new Label();
        bcBuilder.setEntry(callEntry, sendEntry);
        bcBuilder.push(callEntry);
        bcBuilder.push(new IGetglobalobj(env.getRegister(THIS_OBJECT_REGISTER)));
        bcBuilder.push(sendEntry);
        if (node.needFrame())
            bcBuilder.push(new INewframe(node.frameSize, node.needArguments));
        bcBuilder.pushMsetfl();

        /*
         * move argument on stack to frame
         */
        for (ParameterVarDecl decl: node.params) {
            IASTNode.VarLoc locx = decl.getLocation();
            if (locx instanceof IASTNode.FrameVarLoc) {
                IASTNode.FrameVarLoc loc = (IASTNode.FrameVarLoc) locx;
                Register inputReg = env.getRegister(THIS_OBJECT_REGISTER + 1 + decl.getParameterIndex());
                bcBuilder.push(new ISetlocal(0, loc.getIndex(), inputReg));
            }
        } 

        /*
         * Compile body
         */
        Register retReg = env.freshRegister();
        compileNode(node.body, retReg);

        /*
         * return undefined
         */
        bcBuilder.push(new IUndefinedconst(reg));
        env.getCurrentFrame().getContinuation().emitReturn(reg);

        bcBuilder.setNumberOfLocals(node.frameSize);
        bcBuilder.setNumberOfGPRegisters(env.getNumberOfGPRegisters());

        // Don't change the order.
        env.getCurrentFrame().popContinuation();
        env.closeFrame();
        bcBuilder.closeFuncBCBuilder();

        bcBuilder.push(new IMakeclosure(reg, compiledFunction));
        return null;
    }

    @Override
    public Object visitThisExpression(IASTThisExpression node) {
        bcBuilder.push(new IMove(reg, env.getRegister(THIS_OBJECT_REGISTER)));
        return null;
    }

    @Override
    public Object visitArrayExpression(IASTArrayExpression node) {
        Register r1 = env.getCurrentFrame().freshRegister();
        Register constructorReg = env.getCurrentFrame().freshRegister();
        Register[] tmpRegs = new Register[1];
        tmpRegs[0] = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IString(r1, "Array"));
        bcBuilder.push(new IGetglobal(constructorReg, r1));
        bcBuilder.push(new IFixnum(tmpRegs[0], node.elements.size()));
        bcBuilder.push(new INew(reg, constructorReg));
        bcBuilder.pushMCall(reg, constructorReg, tmpRegs, true, false);
        bcBuilder.pushMsetfl();
        bcBuilder.push(new IGeta(reg));
        int i = 0;
        for (IASTExpression element : node.elements) {
            compileNode(element, r1);
            bcBuilder.push(new IFixnum(constructorReg, i++));
            bcBuilder.push(new ISetprop(reg, constructorReg, r1));
            /*
            if (info.compactByteCode) {
                bcBuilder.push(new IFixnum(constructorReg, i++));
                bcBuilder.push(new ISetprop(reg, constructorReg, r1));
            } else {
                bcBuilder.push(new ISetarray(reg, i++, r1));
            }
             */
        }
        return null;
    }

    @Override
    public Object visitObjectExpression(IASTObjectExpression node) {
        Register r1 = env.getCurrentFrame().freshRegister();
        Register constructorReg = env.getCurrentFrame().freshRegister();
        Register[] tmpRegs = new Register[0];
        bcBuilder.push(new IString(r1, "Object"));
        bcBuilder.push(new IGetglobal(constructorReg, r1));
        bcBuilder.push(new INew(reg, constructorReg));
        bcBuilder.pushMCall(reg, constructorReg, tmpRegs, true, false);
        bcBuilder.pushMsetfl();
        bcBuilder.push(new IGeta(reg));
        for (IASTObjectExpression.Property prop : node.properties) {
            compileNode(prop.value, r1);
            compileNode(prop.key, constructorReg);
            bcBuilder.push(new ISetprop(reg, constructorReg, r1));
        }
        return null;
    }

    @Override
    public Object visitUnaryExpression(IASTUnaryExpression node) {
        switch (node.operator) {
        case PLUS: {
            compileNode(node.operands[0], reg);
        }
        break;
        case MINUS: {
            Register r1 = env.getCurrentFrame().freshRegister();
            Register r2 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], r1);
            bcBuilder.push(new IFixnum(r2, -1));
            bcBuilder.push(new IMul(reg, r1, r2));
        }
        break;
        case NOT: {
            Register r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], r1);
            bcBuilder.push(new INot(reg, r1));
        }
        break;
        case INC:
        case DEC: {
            if (node.prefix) {
                Register r1 = env.getCurrentFrame().freshRegister();
                Register r2 = env.getCurrentFrame().freshRegister();
                compileNode(node.operands[0], r1);
                bcBuilder.push(new IFixnum(r2, 1));
                if (node.operator == IASTUnaryExpression.Operator.INC) {
                    bcBuilder.push(new IAdd(reg, r1, r2));
                } else if (node.operator == IASTUnaryExpression.Operator.DEC) {
                    bcBuilder.push(new ISub(reg, r1, r2));
                }
                compileAssignment(node.operands[0], reg);
            } else {
                Register r1 = env.getCurrentFrame().freshRegister();
                Register r2 = env.getCurrentFrame().freshRegister();
                compileNode(node.operands[0], reg);
                bcBuilder.push(new IFixnum(r1, 1));
                if (node.operator == IASTUnaryExpression.Operator.INC) {
                    bcBuilder.push(new IAdd(r2, reg, r1));
                } else if (node.operator == IASTUnaryExpression.Operator.DEC) {
                    bcBuilder.push(new ISub(r2, reg, r1));
                }
                compileAssignment(node.operands[0], r2);
            }
        }
        break;
        case BNOT: {
            Register r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IFixnum(r1, 0));
            bcBuilder.push(new ISub(reg, r1, reg));
            bcBuilder.push(new IFixnum(r1, 1));
            bcBuilder.push(new ISub(reg, reg, r1));
        }
        break;
        case TYPEOF:
            throw new UnsupportedOperationException("Unary operator not implemented : typeof");
        case VOID: {
            Register r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IString(r1, "\"undefined\""));
            bcBuilder.push(new IGetglobal(reg, r1));
        }
        break;
        case DELETE:
            throw new UnsupportedOperationException("Unary operator not implemented : delete");
        default:
            throw new UnsupportedOperationException("Unary operator not implemented : unknown");
        }
        return null;
    }
    @Override
    public Object visitBinaryExpression(IASTBinaryExpression node) {
        // TODO: refactoring
        Register r1 = null, r2 = null;
        switch (node.operator) {
        case ADD: case SUB: case MUL: case DIV: case MOD:
        case SHL: case SHR: case UNSIGNED_SHR:
        case BAND: case BOR: case BXOR:
        case EQUAL: case NOT_EQUAL: case EQ: case NOT_EQ: case LT: case LTE: case GT: case GTE:
        case ASSIGN_ADD: case ASSIGN_SUB: case ASSIGN_MUL: case ASSIGN_DIV: case ASSIGN_MOD:
        case ASSIGN_SHL: case ASSIGN_SHR: case ASSIGN_UNSIGNED_SHR:
        case ASSIGN_BAND: case ASSIGN_BOR: case ASSIGN_BXOR:
            r1 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], r1);
            r2 = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[1], r2);
            break;
        default:
            /* do nothing */
            break;
        }


        switch (node.operator) {
        // arithmetic
        case ADD: case ASSIGN_ADD: {
            bcBuilder.push(new IAdd(reg, r1, r2));
        } break;
        case SUB: case ASSIGN_SUB: {
            bcBuilder.push(new ISub(reg, r1, r2));
        } break;
        case MUL: case ASSIGN_MUL: {
            bcBuilder.push(new IMul(reg, r1, r2));
        } break;
        case DIV: case ASSIGN_DIV: {
            bcBuilder.push(new IDiv(reg, r1, r2));
        } break;
        case MOD: case ASSIGN_MOD: {
            bcBuilder.push(new IMod(reg, r1, r2));
        } break;

        // shift
        case SHL: case ASSIGN_SHL: {
            bcBuilder.push(new ILeftshift(reg, r1, r2));
        } break;
        case SHR: case ASSIGN_SHR: {
            bcBuilder.push(new IRightshift(reg, r1, r2));
        } break;
        case UNSIGNED_SHR: case ASSIGN_UNSIGNED_SHR: {
            bcBuilder.push(new IUnsignedrightshift(reg, r1, r2));
        } break;

        // bit
        case BOR: case ASSIGN_BOR: {
            bcBuilder.push(new IBitor(reg, r1, r2));
        } break;
        case BAND: case ASSIGN_BAND: {
            bcBuilder.push(new IBitand(reg, r1, r2));
        } break;
        case BXOR: case ASSIGN_BXOR: {
            Register r3 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IBitor(r3, r1, r2));  // r3 = a | b
            bcBuilder.push(new IBitand(r1, r1, r2));  // r1 = a & b
            bcBuilder.push(new IFixnum(r2, 0));
            bcBuilder.push(new ISub(r1, r2, r1));    // r1 = -(a & b)
            bcBuilder.push(new IFixnum(r2, 1));
            bcBuilder.push(new ISub(r1, r1, r2));    // r1 = ~(a & b)
            bcBuilder.push(new IBitand(reg, r1, r3)); // reg = ~(a & b) & (a | b)
        } break;

        // logical
        case OR: {
            Label l1 = new Label();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IJumptrue(l1, reg));
            compileNode(node.operands[1], reg);
            bcBuilder.push(l1);
        }
        break;
        case AND: {
            Label l1 = new Label();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IJumpfalse(l1, reg));
            compileNode(node.operands[1], reg);
            bcBuilder.push(l1);
        }
        break;

        // relational
        case EQUAL: case NOT_EQUAL: {
            Register r3 = (node.operator == IASTBinaryExpression.Operator.EQUAL) ? reg : env.getCurrentFrame().freshRegister();
            Register r4 = env.getCurrentFrame().freshRegister();
            Register r5 = env.getCurrentFrame().freshRegister();
            Register r6 = env.getCurrentFrame().freshRegister();
            Register r7 = env.getCurrentFrame().freshRegister();
            Register r8 = env.getCurrentFrame().freshRegister();
            Register r9 = env.getCurrentFrame().freshRegister();
            // Register r10 = node.operator == IASTBinaryExpression.Operator.EQUAL ? reg : env.freshRegister();
            Label l1 = new Label();
            Label l2 = new Label();
            Label l3 = new Label();
            Label l4 = new Label();
            bcBuilder.push(new IEqual(r3, r1, r2));
            bcBuilder.push(new IIsundef(r4, r3));
            bcBuilder.push(new IJumpfalse(l1, r4));
            bcBuilder.push(new IString(r5, "valueOf"));
            bcBuilder.push(new IIsobject(r6, r1));
            bcBuilder.push(new IJumpfalse(l2, r6));

            bcBuilder.push(new IGetprop(r7, r1, r5));
            bcBuilder.push(new MCall(r1, r7, new Register[] {}, false, false));
            bcBuilder.push(new IGeta(r8));
            bcBuilder.push(new IMove(r9, r2));

            bcBuilder.push(new IJump(l3));

            bcBuilder.push(l2);
            bcBuilder.push(new IGetprop(r7, r2, r5));
            bcBuilder.push(new MCall(r2, r7, new Register[] {}, false, false));
            bcBuilder.push(new IGeta(r9));
            bcBuilder.push(new IMove(r8, r1));

            bcBuilder.push(l3);
            bcBuilder.push(new IEqual(r3, r8, r9));
            bcBuilder.push(new IIsundef(r5, r3));
            bcBuilder.push(new IJumpfalse(l4, r5));
            bcBuilder.push(new IError(r3, "EQUAL_GETTOPRIMITIVE"));
            bcBuilder.push(l1);
            bcBuilder.push(l4);
            if (node.operator == IASTBinaryExpression.Operator.NOT_EQUAL)
                bcBuilder.push(new INot(reg, r3));
        } break;
        case EQ: {
            bcBuilder.push(new IEq(reg, r1, r2));
        } break;
        case NOT_EQ: {
            Register r3 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IEq(r3, r1, r2));
            bcBuilder.push(new INot(reg, r3));
        } break;
        case LT: {
            bcBuilder.push(new ILessthan(reg, r1, r2));
        } break;
        case LTE: {
            bcBuilder.push(new ILessthanequal(reg, r1, r2));
        } break;
        case GT: {
            bcBuilder.push(new ILessthan(reg, r2, r1));
        } break;
        case GTE: {
            bcBuilder.push(new ILessthanequal(reg, r2, r1));
        } break;
        case IN:
            throw new UnsupportedOperationException("Binary operator not implemented : in");
        case INSTANCE_OF: {
            bcBuilder.push(new IInstanceof(reg, r2, r1));
        } break;

        // assignment
        case ASSIGN: {
            compileNode(node.operands[1], reg);
        } break;
        }


        switch (node.operator) {
        case ASSIGN_ADD: case ASSIGN_SUB: case ASSIGN_MUL: case ASSIGN_DIV: case ASSIGN_MOD:
        case ASSIGN_SHL: case ASSIGN_SHR: case ASSIGN_UNSIGNED_SHR:
        case ASSIGN_BAND: case ASSIGN_BOR: case ASSIGN_BXOR:
        case ASSIGN:
            compileAssignment(node.operands[0], reg);
            break;
        default:
            break;
        }
        return null;
    }
    void compileAssignment(IASTExpression dst, Register srcReg) {
        if (dst instanceof IASTIdentifier) {
            IASTIdentifier dstVar = (IASTIdentifier) dst;
            compileSetVariable(dstVar, srcReg);
        } else if (dst instanceof IASTMemberExpression) {
            IASTMemberExpression memExp = (IASTMemberExpression) dst;
            Register objReg = env.getCurrentFrame().freshRegister();
            compileNode(memExp.object, objReg);
            Register propReg = env.getCurrentFrame().freshRegister();
            compileNode(memExp.property, propReg);
            bcBuilder.push(new ISetprop(objReg, propReg, srcReg));
        }
    }

    void compileSetVariable(IASTIdentifier node, Register srcReg) {
        IASTNode.VarLoc locx = node.getDeclaration().getLocation();
        if (locx instanceof IASTNode.GlobalVarLoc) {
            Register r1 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IString(r1, node.id));
            bcBuilder.push(new ISetglobal(r1, srcReg));
        } else if (locx instanceof IASTNode.RegisterVarLoc) {
            IASTNode.RegisterVarLoc loc = (IASTNode.RegisterVarLoc) locx;
            Register varReg = env.getRegister(loc.getRegisterNo());
            bcBuilder.push(new IMove(varReg, srcReg));
        } else if (locx instanceof IASTNode.FrameVarLoc) {
            IASTNode.FrameVarLoc loc = (IASTNode.FrameVarLoc) locx;
            int slink = loc.countStaticLink(node.getOwnerFrame());
            bcBuilder.push(new ISetlocal(slink, loc.getIndex(), srcReg));
        } else if (locx instanceof IASTNode.ArgumentsVarLoc) {
            IASTNode.ArgumentsVarLoc loc = (IASTNode.ArgumentsVarLoc) locx;
            int slink = loc.countStaticLink(node.getOwnerFrame());
            bcBuilder.push(new ISetarg(slink, loc.getIndex(), srcReg));
        } else
            throw new Error("unknown VarLoc");
    }

    void compileGetVariable(IASTIdentifier node, Register dstReg) {
        IASTNode.VarLoc locx = node.getDeclaration().getLocation();
        if (locx instanceof IASTNode.GlobalVarLoc) {
            Register r1 = env.getCurrentFrame().freshRegister();
            bcBuilder.push(new IString(r1, node.id));
            bcBuilder.push(new IGetglobal(dstReg, r1));
        } else if (locx instanceof IASTNode.RegisterVarLoc) {
            IASTNode.RegisterVarLoc loc = (IASTNode.RegisterVarLoc) locx;
            Register varReg = env.getRegister(loc.getRegisterNo());
            bcBuilder.push(new IMove(dstReg, varReg));
        } else if (locx instanceof IASTNode.FrameVarLoc) {
            IASTNode.FrameVarLoc loc = (IASTNode.FrameVarLoc) locx;
            int slink = loc.countStaticLink(node.getOwnerFrame());
            bcBuilder.push(new IGetlocal(dstReg, slink, loc.getIndex()));
        } else if (locx instanceof IASTNode.ArgumentsVarLoc) {
            IASTNode.ArgumentsVarLoc loc = (IASTNode.ArgumentsVarLoc) locx;
            int slink = loc.countStaticLink(node.getOwnerFrame());
            bcBuilder.push(new IGetarg(dstReg, slink, loc.getIndex()));
        } else
            throw new Error("unknown VarLoc");
    }

    @Override
    public Object visitTernaryExpression(IASTTernaryExpression node) {
        switch (node.operator) {
        case COND: {
            Register testReg = env.getCurrentFrame().freshRegister();
            compileNode(node.operands[0], testReg);
            Label l1 = new Label();
            Label l2 = new Label();
            bcBuilder.push(new IJumpfalse(l1, testReg));
            compileNode(node.operands[1], reg);
            bcBuilder.push(new IJump(l2));
            bcBuilder.push(l1);
            compileNode(node.operands[2], reg);
            bcBuilder.push(l2);
        }
        break;
        default:
            throw new Error("Unreachable code.");
        }
        return null;
    }

    @Override
    public Object visitCallExpression(IASTCallExpression node) {
        Register[] tmpRegs = new Register[node.arguments.size()];
        for (int i = 0; i < tmpRegs.length; i++)
            tmpRegs[i] = env.getCurrentFrame().freshRegister();

        if (node.callee instanceof IASTMemberExpression) {
            IASTNode object = ((IASTMemberExpression) node.callee).object;
            IASTNode property = ((IASTMemberExpression) node.callee).property;
            Register objReg      = env.getCurrentFrame().freshRegister();
            Register propNameReg = env.getCurrentFrame().freshRegister();
            Register propValReg   = env.getCurrentFrame().freshRegister();

            compileNode(object,   objReg);
            compileNode(property, propNameReg);
            bcBuilder.push(new IGetprop(propValReg, objReg, propNameReg));
            for (int i = 0; i < node.arguments.size(); i++) {
                IASTNode argument = node.arguments.get(i);
                compileNode(argument, tmpRegs[i]);
            }
            bcBuilder.pushMCall(objReg, propValReg, tmpRegs, false, false);
        } else {
            Register calleeReg = env.getCurrentFrame().freshRegister();
            compileNode(node.callee, calleeReg);
            for (int i = 0; i < node.arguments.size(); i++) {
                IASTNode argument = node.arguments.get(i);
                compileNode(argument, tmpRegs[i]);
            }            
            bcBuilder.pushMCall(null, calleeReg, tmpRegs, false, false);
        }
        bcBuilder.pushMsetfl();
        bcBuilder.push(new IGeta(reg));
        return null;
    }

    @Override
    public Object visitNewExpression(IASTNewExpression node) {
        Register[] tmpRegs = new Register[node.arguments.size()];
        for (int i = 0; i < tmpRegs.length; i++)
            tmpRegs[i] = env.getCurrentFrame().freshRegister();
        Register constructorReg = env.getCurrentFrame().freshRegister();
        compileNode(node.constructor, constructorReg);
        for (int i = 0; i < node.arguments.size(); i++) {
            IASTNode argument = node.arguments.get(i);
            compileNode(argument, tmpRegs[i]);
        }
        bcBuilder.push(new INew(reg, constructorReg));
        bcBuilder.pushMCall(reg, constructorReg, tmpRegs, true, false);

        bcBuilder.pushMsetfl();
        Register strReg = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IString(strReg, "Object"));
        Register objReg = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IGetglobal(objReg, strReg));
        Register resultOfNewSendReg = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IGeta(resultOfNewSendReg));
        Register result = env.getCurrentFrame().freshRegister();
        bcBuilder.push(new IInstanceof(result, resultOfNewSendReg, objReg));
        Label l1 = new Label();
        bcBuilder.push(new IJumpfalse(l1, result));
        bcBuilder.push(new IGeta(reg));
        bcBuilder.push(l1);
        return null;
    }

    @Override
    public Object visitMemberExpression(IASTMemberExpression node) {
        Register objReg = env.getCurrentFrame().freshRegister();
        compileNode(node.object, objReg);
        Register expReg = env.getCurrentFrame().freshRegister();
        compileNode(node.property, expReg);
        bcBuilder.push(new IGetprop(reg, objReg, expReg));
        return null;
    }

    @Override
    public Object visitIdentifier(IASTIdentifier node) {
        compileGetVariable(node, reg);
        return null;
    }
}
