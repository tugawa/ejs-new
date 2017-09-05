import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class CodeGenerator extends IASTBaseVisitor {

    class JSLabel {
        String name;
        Label label;
        JSLabel(String name, Label label) {
            this.name = name;
            this.label = label;
        }
    }

    class Environment {

        // Frame is made for each IASTFunction.
        class Frame {
            List<String> args;
            List<String> staticLocals;
            LinkedList<String> dynamicLocals;

            private int maxNumOfDynamicLocals = 0;
            private int numOfUsingRegisters = 0;
            private int numOfRequiredRegisters = numOfUsingRegisters;
            private final static int MAX_OF_STACK_OF_USING_REGISTERS = 256;
            private int[] stackOfUsingRegisters = new int[MAX_OF_STACK_OF_USING_REGISTERS];
            private int stackPointerOfUsingRegisters = 0;

            private int numOfUsingArgumentRegisters = 0;
            private List<Register> argumentRegisters = new LinkedList<Register>();

            private Fl fl = new Fl();
            LinkedList<JSLabel> breakLabels = new LinkedList<JSLabel>();
            LinkedList<JSLabel> continueLabels = new LinkedList<JSLabel>();

            Register registerOfGlobalObj = null;

            Frame(List<String> args, List<String> locals) {
                this.args = args; // created by FunctionDeclaration
                this.staticLocals = locals; // created by VariableDeclaration and FunctionDeclaration
                this.dynamicLocals = new LinkedList<String>(); // created by CatchClause and WithStatement
            }
            void addLocal(String s) {
                dynamicLocals.addFirst(s);
                if (dynamicLocals.size() >  maxNumOfDynamicLocals) {
                    maxNumOfDynamicLocals = dynamicLocals.size();
                }
            }
            void pushCurNumOfUsingRegister() {
                stackOfUsingRegisters[stackPointerOfUsingRegisters++] = numOfUsingRegisters;
            }
            void popCurNumOfUsingRegister() {
                numOfUsingRegisters = stackOfUsingRegisters[--stackPointerOfUsingRegisters];
            }
            Register newRegister() {
                numOfUsingRegisters++;
                if (numOfRequiredRegisters <= numOfUsingRegisters)
                    numOfRequiredRegisters = numOfUsingRegisters;
                return new Register(numOfUsingRegisters);
            }
            Register[] newArgumentRegister(int n) {
                for (; numOfUsingArgumentRegisters < n; numOfUsingArgumentRegisters++) {
                    argumentRegisters.add(new Register());
                }
                Register[] ret = new Register[n];
                for (int i = 0, j = n - 1; i < n; i++, j--) {
                    ret[j] = argumentRegisters.get(i);
                }
                return ret;
            }
            Fl getFl() {
                return fl;
            }
            void close() {
                fl.n = numOfRequiredRegisters + numOfUsingArgumentRegisters;
                for (int i = 0, n = fl.n; i < numOfUsingArgumentRegisters; i++, n--) {
                    argumentRegisters.get(i).n = n;
                }
            }

            int getNumberOfLocals() {
                return this.staticLocals.size() + this.maxNumOfDynamicLocals;
            }
        }


        LinkedList<Frame> frameList = new LinkedList<Frame>();


        Environment() {}

        // It's necessary to call the method before
        // compiling IASTProgram and IASTFunction.
        void openFrame(List<String> args, List<String> locals) {
            this.frameList.addFirst(new Frame(args, locals));
        }

        // It's necessary to call the method after
        // compiling IASTProgram and IASTFunction.
        void closeFrame() {
            this.frameList.getFirst().close();
            this.frameList.removeFirst();
        }

        // If you need a new LocalVar while compiling IASTFunction's body,
        // call the method at the beginning scope of the LocalVar.
        void addLocal(String local) {
            this.frameList.getFirst().addLocal(local);
        }

        // You must call this method
        // at the ending scope of a added LocalVar by "addLocal" method.
        void removeLocal() {
            this.frameList.getFirst().dynamicLocals.removeFirst();
        }

        // "getVar" method fetch arg/local's storage location from given identifier.
        // For example: if Result is { isLocal:true, depth:1, n:2 },
        //              you can push a bytecode "setlocal 1 2 x" or "getlocal y 1 2".
        class Result {
            boolean isLocal; // ARG -> false, LOCAL -> true
            int depth;
            int n;
            Result(boolean isLocal, int depth, int n) {
                this.isLocal = isLocal; this.depth = depth; this.n = n;
            }
        }
        Result getVar(String id) {
            int depth = 0;
            for (Frame ve : frameList) {
                int cnt = 0;
                for (String lo : ve.dynamicLocals) {
                    if (id.equals(lo)) {
                        int n = ve.dynamicLocals.size() - cnt - 1 + ve.staticLocals.size();
                        return new Result(true, depth, n);
                    }
                    cnt++;
                }
                cnt = 0;
                for (String lo : ve.staticLocals) {
                    if (id.equals(lo)) {
                        return new Result(true, depth, cnt);
                    }
                    cnt++;
                }
                cnt = 0;
                for (String arg : ve.args) {
                    if (id.equals(arg)) {
                        return new Result(false, depth, cnt);
                    }
                    cnt++;
                }
                depth++;
            }
            return null;
        }

        // You must call these methods before/after compiling each IASTNode.
        void before() {
            this.frameList.getFirst().pushCurNumOfUsingRegister();
        }
        void after() {
            this.frameList.getFirst().popCurNumOfUsingRegister();
        }

        void pushBreakLabel(String name, Label label) {
            frameList.getFirst().breakLabels.push(new JSLabel(name, label));
        }
        void popBreakLabel() {
            frameList.getFirst().breakLabels.pop();
        }
        void pushContinueLabel(String name, Label label) {
            frameList.getFirst().continueLabels.push(new JSLabel(name, label));
        }
        void popContinueLabel() {
            frameList.getFirst().continueLabels.pop();
        }

        Label getBreakLabel(String name) {
            Frame f = frameList.getFirst();
            if (name == null) {
                return f.breakLabels.getFirst().label;
            } else {
                for (JSLabel jsl : f.breakLabels) {
                    if (name.equals(jsl.name)) return jsl.label;
                }
            }
            return null;
        }
        Label getContinueLabel(String name) {
            Frame f = frameList.getFirst();
            if (name == null) {
                return f.continueLabels.getFirst().label;
            } else {
                for (JSLabel jsl : f.continueLabels) {
                    if (name.equals(jsl.name)) return jsl.label;
                }
            }
            return null;
        }

        // If you need a fresh Register, you have to call this.
        Register freshRegister() {
            return this.frameList.getFirst().newRegister();
        }
        Register[] freshArgumentRegister(int n) {
            return this.frameList.getFirst().newArgumentRegister(n);
        }

        Fl getFl() {
            return this.frameList.getFirst().getFl();
        }

        int getNumberOfLocals() {
            return this.frameList.getFirst().getNumberOfLocals();
        }

        void setRegOfGlobalObj(Register reg) {
            this.frameList.getFirst().registerOfGlobalObj = reg;
        }

        Register getRegOfGlobalObj() {
            return this.frameList.getFirst().registerOfGlobalObj;
        }
    }

    class BCBuilder {

        class FunctionBCBuilder {
            int callentry = 0;
            int sendentry = 0;
            int numberOfLocals = 0;
            LinkedList<BCode> bcodes = new LinkedList<BCode>();

            LinkedList<JSLabel> jslabelsContinueDest = new LinkedList<JSLabel>();
            LinkedList<JSLabel> jslabelsBreakDest = new LinkedList<JSLabel>();
            LinkedList<Label>   labelsSetJumpDest   = new LinkedList<Label>();

            List<BCode> build() {
                int numberOfInstruction = bcodes.size();
                List<BCode> result = new LinkedList<BCode>();
                int number = 0;
                for (BCode bcode : bcodes) {
                    bcode.number = number;
                    number++;
                }
                result.add(new ICallentry(callentry));
                result.add(new ISendentry(sendentry));
                result.add(new INumberOfLocals(numberOfLocals));
                result.add(new INumberOfInstruction(numberOfInstruction));
                result.addAll(bcodes);
                return result;
            }
        }

        private LinkedList<FunctionBCBuilder> fbStack = new LinkedList<FunctionBCBuilder>();
        private LinkedList<FunctionBCBuilder> fBuilders = new LinkedList<FunctionBCBuilder>();

        int maxNumOfArgsOfCallingFunction = 0;

        void openFunctionBCBuilder() {
            FunctionBCBuilder bcb = new FunctionBCBuilder();
            fbStack.push(bcb);
            fBuilders.add(bcb);
        }

        void closeFuncBCBuilder() {
            fbStack.pop();
        }

        List<BCode> build() {
            // build fBuilders.
            List<BCode> result = new LinkedList<BCode>();
            result.add(new IFuncLength(fBuilders.size()));
            for (FunctionBCBuilder fb : fBuilders) {
                result.addAll(fb.build());
            }
            return result;
        }

        void pushContinueDest(JSLabel label) {
            fbStack.getFirst().jslabelsContinueDest.add(label);
        }
        void pushBreakDest(JSLabel label) {
            fbStack.getFirst().jslabelsBreakDest.add(label);
        }
        void push(Label label) {
            fbStack.getFirst().labelsSetJumpDest.push(label);
        }
        void push(BCode bcode) {
            fbStack.getFirst().jslabelsBreakDest.clear();
            for (Label l : fbStack.getFirst().labelsSetJumpDest) {
                l.bcode = bcode;
            }
            fbStack.getFirst().labelsSetJumpDest.clear();
            fbStack.getFirst().bcodes.add(bcode);
        }

        BCode getLastBCode() {
            return this.fbStack.getFirst().bcodes.getLast();
        }

        int getFBIdx() {
            return fBuilders.size() - 2;
        }

        void setSendentry(int n) {
            this.fbStack.getFirst().sendentry = n;
        }

        void setNumberOfLocals(int n) {
            this.fbStack.getFirst().numberOfLocals = n;
        }

    }

    public CodeGenerator() {}

    BCBuilder bcBuilder;
    Environment env;
    Register reg;

    void printByteCode(List<BCode> bcodes) {
        for (BCode bcode : bcodes) {
            System.out.println(bcode);
        }
    }

    public List<BCode> compile(IASTProgram node) {
        this.bcBuilder = new BCBuilder();
        this.env = new Environment();
        try {
            bcBuilder.openFunctionBCBuilder();
            env.openFrame(new ArrayList<String>(), new ArrayList<String>());
            compileNode(node, null);
            env.closeFrame();
            bcBuilder.closeFuncBCBuilder();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        // printByteCode(bcBuilder.build());
        return bcBuilder.build();
    }

    void compileNode(IASTNode node, Register reg) {
        Register tmp = this.reg;
        this.reg = reg;
        node.accept(this);
        this.reg = tmp;
    }

    @Override
    public Object visitProgram(IASTProgram node) {
        Register globalObjReg = env.freshRegister();
        env.setRegOfGlobalObj(globalObjReg);
        bcBuilder.push(new IGetglobalobj(globalObjReg));
        bcBuilder.push(new ISetfl(env.getFl()));
        Register retReg = env.freshRegister();
        compileNode(node.program.body, retReg);
        bcBuilder.push(new ISeta(retReg));
        bcBuilder.push(new IRet());
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
        bcBuilder.push(new ISpecconst(reg, node.value));
        return null;
    }
    @Override
    public Object visitNullLiteral(IASTNullLiteral node) {
        bcBuilder.push(new ISpecconst(reg, "null"));
        return null;
    }
    @Override
    public Object visitRegExpLiteral(IASTRegExpLiteral node) {
        bcBuilder.push(new IRegexp(reg, 0, node.pattern));
        return null;
    }

    @Override
    public Object visitBlockStatement(IASTBlockStatement node) {
        for (IASTStatement stmt : node.stmts) {
            compileNode(stmt, reg);
        }
        return null;
    }
    @Override
    public Object visitReturnStatement(IASTReturnStatement node) {
        if (node.value == null) {
            bcBuilder.push(new ISpecconst(reg, "undefined"));
        } else {
          compileNode(node.value, reg);
        }
        bcBuilder.push(new ISeta(reg));
        bcBuilder.push(new IRet());
        return null;
    }
    @Override
    public Object visitWithStatement(IASTWithStatement node) {
        throw new UnsupportedOperationException("WithStatement has not been implemented yet.");

    }
    @Override
    public Object visitEmptyStatement(IASTEmptyStatement node) { return null; }
    @Override
    public Object visitIfStatement(IASTIfStatement node) {
        Label l1 = new Label();
        compileNode(node.test, reg);
        bcBuilder.push(new IJumpfalse(reg, l1));
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
        Register discReg = env.freshRegister();
        compileNode(node.discriminant, discReg);
        LinkedList<Label> caseLabels = new LinkedList<Label>();
        Register testReg = env.freshRegister();
        for (IASTSwitchStatement.CaseClause caseClause : node.cases) {
            if (caseClause.test != null) {
                Label caseLabel = new Label();
                compileNode(caseClause.test, testReg);
                bcBuilder.push(new IEq(reg, discReg, testReg));
                bcBuilder.push(new IJumptrue(reg, caseLabel));
                caseLabels.add(caseLabel);
            } else {
                Label caseLabel = new Label();
                bcBuilder.push(new IJump(caseLabel));
                caseLabels.add(caseLabel);
                break;
            }
        }
        Label breakLabel = new Label();
        env.pushBreakLabel(node.label, breakLabel);
        for (IASTSwitchStatement.CaseClause caseClause : node.cases) {
            Label caseLabel = caseLabels.pollFirst();
            if (caseLabel != null) {
                bcBuilder.push(caseLabel);
            }
            compileNode(caseClause.consequent, testReg);
        }
        env.popBreakLabel();
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
        throw new UnsupportedOperationException("ThrowStatement has not been implemented yet.");
    }
    @Override
    public Object visitTryCatchStatement(IASTTryCatchStatement node) {
        throw new UnsupportedOperationException("TryCatchStatement has not been implemented yet.");
    }
    @Override
    public Object visitTryFinallyStatement(IASTTryFinallyStatement node) {
        throw new UnsupportedOperationException("TryFinallyStatement has not been implemented yet.");
    }
    @Override
    public Object visitForStatement(IASTForStatement node) {
        if (node.init != null)
            compileNode(node.init, env.freshRegister());
        Label l1 = new Label();
        Label l2 = new Label();
        Label continueLabel = new Label();
        Label breakLabel = new Label();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(l2);
        if (node.body != null) {
            env.pushBreakLabel(node.label, breakLabel);
            env.pushContinueLabel(node.label, continueLabel);
            compileNode(node.body, reg);
            env.popBreakLabel();
            env.popContinueLabel();
        }
        bcBuilder.push(continueLabel);
        if (node.update != null)
            compileNode(node.update, env.freshRegister());
        bcBuilder.push(l1);
        Register testReg = env.freshRegister();
        if (node.test != null) {
            compileNode(node.test, testReg);
            bcBuilder.push(new IJumptrue(testReg, l2));
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
            env.pushBreakLabel(node.label, breakLabel);
            env.pushContinueLabel(node.label, continueLabel);
            compileNode(node.body, reg);
            env.popBreakLabel();
            env.popContinueLabel();
        }
        bcBuilder.push(continueLabel);
        bcBuilder.push(l1);
        Register testReg = env.freshRegister();
        compileNode(node.test, testReg);
        bcBuilder.push(new IJumptrue(testReg, l2));
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
            env.pushBreakLabel(node.label, breakLabel);
            env.pushContinueLabel(node.label, continueLabel);
            compileNode(node.body, reg);
            env.popBreakLabel();
            env.popContinueLabel();
        }
        Register testReg = env.freshRegister();
        compileNode(node.test, testReg);
        bcBuilder.push(new IJumptrue(testReg, l1));
        bcBuilder.push(breakLabel);
        return null;
    }
    @Override
    public Object visitForInStatement(IASTForInStatement node) {
        Register objReg = env.freshRegister();
        Register iteReg = env.freshRegister();
        Register propReg = env.freshRegister();
        Register testReg = env.freshRegister();
        Label breakLabel = new Label();
        Label continueLabel = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        compileNode(node.object, objReg);
        bcBuilder.push(new IMakeiterator(objReg, iteReg));
        bcBuilder.push(l1);
        bcBuilder.push(new INextpropname(objReg, iteReg, propReg));
        Environment.Result id = env.getVar(node.var);
        if (id == null) {
            Register r1 = env.freshRegister();
            bcBuilder.push(new IString(r1, node.var));
            bcBuilder.push(new ISetglobal(r1, propReg));
        } else {
            if (id.isLocal) {
                bcBuilder.push(new ISetlocal(id.depth, id.n, propReg));
            } else {
                bcBuilder.push(new ISetarg(id.depth, id.n, propReg));
            }
        }
        bcBuilder.push(new IIsundef(testReg, propReg));
        bcBuilder.push(new IJumptrue(testReg, l2));
        bcBuilder.push(continueLabel);
        env.pushBreakLabel(node.label, breakLabel);
        env.pushContinueLabel(node.label, continueLabel);
        compileNode(node.body, reg);
        env.popBreakLabel();
        env.popContinueLabel();
        bcBuilder.push(new IJump(l1));
        bcBuilder.push(breakLabel);
        bcBuilder.push(l2);
        return null;
    }
    @Override
    public Object visitBreakStatement(IASTBreakStatement node) {
        Label label = env.getBreakLabel(node.label);
        if (label != null) {
            bcBuilder.push(new IJump(label));
        } else {
            throw new Error("Compile Error ... BreakStatement");
        }
        return null;
    }
    @Override
    public Object visitContinueStatement(IASTContinueStatement node) {
        Label label = env.getContinueLabel(node.label);
        if (label != null) {
            bcBuilder.push(new IJump(label));
        } else {
            throw new Error("Compile Error ... ContinueStatement");
        }
        return null;
    }
    @Override
    public Object visitFunctionExpression(IASTFunctionExpression node) {
        bcBuilder.openFunctionBCBuilder();
        int functionIdx = bcBuilder.getFBIdx();
        env.openFrame(node.params, node.locals);
        Register globalObjReg = env.freshRegister();
        env.setRegOfGlobalObj(globalObjReg);
        bcBuilder.push(new IGetglobalobj(globalObjReg));
        bcBuilder.push(new ISetfl(env.getFl()));
        bcBuilder.push(new INewargs());
        Register retReg = env.freshRegister();
        compileNode(node.body, retReg);
        bcBuilder.push(new ISpecconst(reg, "undefined"));
        bcBuilder.push(new ISeta(reg));
        bcBuilder.push(new IRet());

        bcBuilder.setSendentry(1);
        bcBuilder.setNumberOfLocals(env.getNumberOfLocals());

        // Don't change the order.
        env.closeFrame();
        bcBuilder.closeFuncBCBuilder();

        bcBuilder.push(new IMakeclosure(reg, functionIdx));
        return null;
    }
    @Override
    public Object visitThisExpression(IASTThisExpression node) {
        bcBuilder.push(new IMove(reg, env.getRegOfGlobalObj()));
        return null;
    }
    @Override
    public Object visitArrayExpression(IASTArrayExpression node) {
        Register r1 = env.freshRegister();
        Register r2 = env.freshRegister();
        Register r3 = env.freshRegister();
        Register[] argRegs = env.freshArgumentRegister(2);
        bcBuilder.push(new IFixnum(r1, node.elements.size()));
        bcBuilder.push(new IString(r2, "Array"));
        bcBuilder.push(new IGetglobal(r3, r2));
        bcBuilder.push(new INew(reg, r3));
        bcBuilder.push(new IMove(argRegs[1], r1));
        bcBuilder.push(new IMove(argRegs[0], reg));
        bcBuilder.push(new INewsend(r3, 1));
        bcBuilder.push(new ISetfl(env.getFl()));
        bcBuilder.push(new IGeta(reg));
        int i = 0;
        for (IASTExpression element : node.elements) {
            compileNode(element, r1);
            bcBuilder.push(new ISetarray(reg, i++, r1));
        }
        return null;
    }
    @Override
    public Object visitObjectExpression(IASTObjectExpression node) {
        Register r1 = env.freshRegister();
        Register r2 = env.freshRegister();
        Register[] argRegs = env.freshArgumentRegister(1);
        bcBuilder.push(new IString(r1, "Object"));
        bcBuilder.push(new IGetglobal(r2, r1));
        bcBuilder.push(new INew(reg, r2));
        bcBuilder.push(new IMove(argRegs[0], reg));
        bcBuilder.push(new INewsend(r2, 0));
        bcBuilder.push(new ISetfl(env.getFl()));
        bcBuilder.push(new IGeta(reg));
        for (IASTObjectExpression.Property prop : node.properties) {
            compileNode(prop.value, r1);
            compileNode(prop.key, r2);
            bcBuilder.push(new ISetprop(reg, r2, r1));
        }
        return null;
    }
    @Override
    public Object visitUnaryExpression(IASTUnaryExpression node) {
        switch (node.operator) {
        case PLUS: {
            compileNode(node.operands[0], reg);
        } break;
        case MINUS: {
            Register r1 = env.freshRegister();
            Register r2 = env.freshRegister();
            compileNode(node.operands[0], r1);
            bcBuilder.push(new IFixnum(r2, -1));
            bcBuilder.push(new IMul(reg, r1, r2));
        } break;
        case NOT: {
            Register r1 = env.freshRegister();
            compileNode(node.operands[0], r1);
            bcBuilder.push(new INot(reg, r1));
        } break;
        case INC:
        case DEC: {
            if (node.prefix) {
                Register r1 = env.freshRegister();
                Register r2 = env.freshRegister();
                compileNode(node.operands[0], r1);
                bcBuilder.push(new IFixnum(r2, 1));
                if (node.operator == IASTUnaryExpression.Operator.INC) {
                    bcBuilder.push(new IAdd(reg, r1, r2));
                } else if (node.operator == IASTUnaryExpression.Operator.DEC) {
                    bcBuilder.push(new ISub(reg, r1, r2));
                }
                compileAssignment(node.operands[0], reg);
            } else {
                Register r1 = env.freshRegister();
                Register r2 = env.freshRegister();
                compileNode(node.operands[0], reg);
                bcBuilder.push(new IFixnum(r1, 1));
                if (node.operator == IASTUnaryExpression.Operator.INC) {
                    bcBuilder.push(new IAdd(r2, reg, r1));
                } else if (node.operator == IASTUnaryExpression.Operator.DEC) {
                    bcBuilder.push(new ISub(r2, reg, r1));
                }
                compileAssignment(node.operands[0], r2);
            }
        } break;
        case BNOT: {
            Register r1 = env.freshRegister();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IFixnum(r1, 0));
            bcBuilder.push(new ISub(reg, r1, reg));
            bcBuilder.push(new IFixnum(r1, 1));
            bcBuilder.push(new ISub(reg, reg, r1));
        } break;
        case TYPEOF:
            throw new UnsupportedOperationException("Unary operator not implemented : typeof");
        case VOID: {
            Register r1 = env.freshRegister();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IString(r1, "\"undefined\""));
            bcBuilder.push(new IGetglobal(reg, r1));
        } break;
        case DELETE:
            throw new UnsupportedOperationException("Unary operator not implemented : delete");
        default:
            throw new UnsupportedOperationException("Unary operator not implemented : unknown");
        }
        return null;
    }
    @Override
    public Object visitBinaryExpression(IASTBinaryExpression node) {
        Register r1 = null, r2 = null;
        switch (node.operator) {
        case ADD: case SUB: case MUL: case DIV: case MOD:
        case SHL: case SHR: case UNSIGNED_SHR:
        case BAND: case BOR: case BXOR:
        case EQUAL: case NOT_EQUAL: case EQ: case NOT_EQ: case LT: case LTE: case GT: case GTE:
        case ASSIGN_ADD: case ASSIGN_SUB: case ASSIGN_MUL: case ASSIGN_DIV: case ASSIGN_MOD:
        case ASSIGN_SHL: case ASSIGN_SHR: case ASSIGN_UNSIGNED_SHR:
        case ASSIGN_BAND: case ASSIGN_BOR: case ASSIGN_BXOR:
            r1 = env.freshRegister();
            compileNode(node.operands[0], r1);
            r2 = env.freshRegister();
            compileNode(node.operands[1], r2);
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
            Register r3 = env.freshRegister();
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
            bcBuilder.push(new IJumptrue(reg, l1));
            compileNode(node.operands[1], reg);
            bcBuilder.push(l1);
        } break;
        case AND: {
            Label l1 = new Label();
            compileNode(node.operands[0], reg);
            bcBuilder.push(new IJumpfalse(reg, l1));
            compileNode(node.operands[1], reg);
            bcBuilder.push(l1);
        }

        // relational
        case EQUAL: case NOT_EQUAL: {
            Register r3 = (node.operator == IASTBinaryExpression.Operator.EQUAL) ? reg : env.freshRegister();
            Register r4 = env.freshRegister();
            Register r5 = env.freshRegister();
            Register r6 = env.freshRegister();
            Register r7 = env.freshRegister();
            Register r8 = env.freshRegister();
            Register r9 = env.freshRegister();
            // Register r10 = node.operator == IASTBinaryExpression.Operator.EQUAL ? reg : env.freshRegister();
            Register ar1 = env.freshArgumentRegister(1)[0];
            Label l1 = new Label();
            Label l2 = new Label();
            Label l3 = new Label();
            Label l4 = new Label();
            bcBuilder.push(new IEqual(r3, r1, r2));
            bcBuilder.push(new IIsundef(r4, r3));
            bcBuilder.push(new IJumpfalse(r4, l1));
            bcBuilder.push(new IString(r5, "valueOf"));
            bcBuilder.push(new IIsobject(r6, r1));
            bcBuilder.push(new IJumpfalse(r6, l2));

            bcBuilder.push(new IGetprop(r7, r1, r5));
            bcBuilder.push(new IMove(ar1, r1));
            bcBuilder.push(new ISend(r7, 0));
            bcBuilder.push(new IGeta(r8));
            bcBuilder.push(new IMove(r9, r2));

            bcBuilder.push(new IJump(l3));

            bcBuilder.push(l2);
            bcBuilder.push(new IGetprop(r7, r2, r5));
            bcBuilder.push(new IMove(ar1, r2));
            bcBuilder.push(new ISend(r7, 0));
            bcBuilder.push(new IGeta(r9));
            bcBuilder.push(new IMove(r8, r1));

            bcBuilder.push(l3);
            bcBuilder.push(new IEqual(r3, r8, r9));
            bcBuilder.push(new IIsundef(r5, r3));
            bcBuilder.push(new IJumpfalse(r5, l4));
            bcBuilder.push(new IError(r3, "\"EQUAL_GETTOPRIMITIVE\""));
            bcBuilder.push(l1);
            bcBuilder.push(l4);
            if (node.operator == IASTBinaryExpression.Operator.NOT_EQUAL)
                bcBuilder.push(new INot(reg, r3));
        } break;
        case EQ: {
            bcBuilder.push(new IEq(reg, r1, r2));
        } break;
        case NOT_EQ: {
            Register r3 = env.freshRegister();
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
        case ASSIGN: {
            compileAssignment(node.operands[0], reg);
        }
        }
        return null;
    }
    void compileAssignment(IASTExpression dst, Register srcReg) {
        if (dst instanceof IASTIdentifier) {
            String id = ((IASTIdentifier) dst).id;
            Environment.Result varLoc = env.getVar(id);
            if (varLoc == null) {
                Register r1 = env.freshRegister();
                bcBuilder.push(new IString(r1, id));
                bcBuilder.push(new ISetglobal(r1, srcReg));
            } else {
                if (varLoc.isLocal) {
                    bcBuilder.push(new ISetlocal(varLoc.depth, varLoc.n, srcReg));
                } else {
                    bcBuilder.push(new ISetarg(varLoc.depth, varLoc.n, srcReg));
                }
            }
        } else if (dst instanceof IASTMemberExpression) {
            IASTMemberExpression memExp = (IASTMemberExpression) dst;
            Register objReg = env.freshRegister();
            compileNode(memExp.object, objReg);
            Register propReg = env.freshRegister();
            compileNode(memExp.property, propReg);
            bcBuilder.push(new ISetprop(objReg, propReg, srcReg));
        }
    }
    @Override
    public Object visitTernaryExpression(IASTTernaryExpression node) {
        switch (node.operator) {
        case COND: {
            Register testReg = env.freshRegister();
            compileNode(node.operands[0], testReg);
            Label l1 = new Label();
            Label l2 = new Label();
            bcBuilder.push(new IJumpfalse(testReg, l1));
            compileNode(node.operands[1], reg);
            bcBuilder.push(new IJump(l2));
            bcBuilder.push(l1);
            compileNode(node.operands[2], reg);
            bcBuilder.push(l2);
        } break;
        default:
            throw new Error("Unreachable code.");
        }
        return null;
    }
    @Override
    public Object visitCallExpression(IASTCallExpression node) {
        if (node.callee instanceof IASTMemberExpression) {
            Register[] argRegs = env.freshArgumentRegister(node.arguments.size() + 1);
            Register[] tmpRegs = new Register[node.arguments.size()];
            Register objReg = env.freshRegister();
            compileNode(((IASTMemberExpression) node.callee).object, objReg);
            Register propReg = env.freshRegister();
            compileNode(((IASTMemberExpression) node.callee).property, propReg);
            Register expReg = env.freshRegister();
            bcBuilder.push(new IGetprop(expReg, objReg, propReg));
            for (int i = 0; i < tmpRegs.length; i++) {
                tmpRegs[i] = env.freshRegister();
                compileNode(node.arguments.get(i), tmpRegs[i]);
            }
            bcBuilder.push(new IMove(argRegs[0], objReg));
            for (int i = 0; i < tmpRegs.length; i++) {
                bcBuilder.push(new IMove(argRegs[i + 1], tmpRegs[i]));
            }
            bcBuilder.push(new ISend(expReg, node.arguments.size()));
        } else {
            Register[] argRegs = env.freshArgumentRegister(node.arguments.size());
            Register[] tmpRegs = new Register[argRegs.length];
            for (int i = 0; i < argRegs.length; i++) {
                tmpRegs[i] = env.freshRegister();
                compileNode(node.arguments.get(i), tmpRegs[i]);
            }
            for (int i = 0; i < argRegs.length; i++) {
                bcBuilder.push(new IMove(argRegs[i], tmpRegs[i]));
            }
            Register calleeReg = env.freshRegister();
            compileNode(node.callee, calleeReg);
            bcBuilder.push(new ICall(calleeReg, argRegs.length));
        }
        bcBuilder.push(new ISetfl(env.getFl()));
        bcBuilder.push(new IGeta(reg));
        return null;
    }
    @Override
    public Object visitNewExpression(IASTNewExpression node) {
        Register constructorReg = env.freshRegister();
        compileNode(node.constructor, constructorReg);
        Register[] argRegs = env.freshArgumentRegister(node.arguments.size() + 1);
        {
            List<Register> tmpRegs = new LinkedList<Register>();
            for (IASTExpression arg : node.arguments) {
                Register tmpReg = env.freshRegister();
                tmpRegs.add(tmpReg);
                compileNode(arg, tmpReg);
            }
            int i = 1;
            for (Register r : tmpRegs) {
                bcBuilder.push(new IMove(argRegs[i++], r));
            }
        }
        bcBuilder.push(new INew(reg, constructorReg));
        bcBuilder.push(new IMove(argRegs[0], reg));
        bcBuilder.push(new INewsend(constructorReg, node.arguments.size()));
        bcBuilder.push(new ISetfl(env.getFl()));
        Register strReg = env.freshRegister();
        bcBuilder.push(new IString(strReg, "Object"));
        Register objReg = env.freshRegister();
        bcBuilder.push(new IGetglobal(objReg, strReg));
        Register resultOfNewSendReg = env.freshRegister();
        bcBuilder.push(new IGeta(resultOfNewSendReg));
        Register result = env.freshRegister();
        bcBuilder.push(new IInstanceof(result, resultOfNewSendReg, objReg));
        Label l1 = new Label();
        bcBuilder.push(new IJumpfalse(result, l1));
        bcBuilder.push(new IGeta(reg));
        bcBuilder.push(l1);
        return null;
    }
    @Override
    public Object visitMemberExpression(IASTMemberExpression node) {
        Register objReg = env.freshRegister();
        compileNode(node.object, objReg);
        Register expReg = env.freshRegister();
        compileNode(node.property, expReg);
        bcBuilder.push(new IGetprop(reg, objReg, expReg));
        return null;
    }
    @Override
    public Object visitIdentifier(IASTIdentifier node) {
        Environment.Result id = env.getVar(node.id);
        if (id == null) {
            Register r1 = env.freshRegister();
            bcBuilder.push(new IString(r1, node.id));
            bcBuilder.push(new IGetglobal(reg, r1));
        } else {
            if (id.isLocal) {
                bcBuilder.push(new IGetlocal(reg, id.depth, id.n));
            } else {
                bcBuilder.push(new IGetarg(reg, id.depth, id.n));
            }
        }
        return null;
    }
}
