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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

abstract public class IASTNode {
    static class VarDecl {
        static enum Type {
            ARGUMENT,
            LOCAL_VAR,
            EXCEPTION,
            GLOBAL
        }
        static private final GlobalVarLoc GLOBAL_LOC = new GlobalVarLoc();
        static HashMap<String, VarDecl> gvars = new HashMap<String, VarDecl>();
        static VarDecl createGlobalVarDecl(String name) {
            if (!gvars.containsKey(name)) {
                VarDecl decl = new VarDecl(name);
                decl.location = GLOBAL_LOC;
                decl.type = Type.GLOBAL;
                gvars.put(name, decl);
            }
            return gvars.get(name);
        }

        private String name;
        protected VarLoc location;
        protected Type type;
        protected ScopeHolder ownerScope;

        private VarDecl(String name) {
            this.name = name;
        }
        public VarDecl(String name, Type type, ScopeHolder ownerScope) {
            this.name = name;
            this.type = type;
            this.ownerScope = ownerScope;
            this.location = new UnresolvedVarLoc();
        }


        public String getName() {
            return name;
        }
        public VarLoc getLocation() {
            return location;
        }
        public void markMayEscapeUnlessOwnerFunction(IASTFunctionExpression func) {
            if (location instanceof GlobalVarLoc)
                return;
            if (ownerScope instanceof IASTFunctionExpression) {
                if (func != ownerScope)
                    markMayEscape();
            } else {
                if (func != ownerScope.getOwnerFunction())
                    markMayEscape();
            }
        }
        public void markMayEscape() {
            ((UnresolvedVarLoc) location).mayEscape = true;
        }
        public boolean mayEscape() {
            return ((UnresolvedVarLoc) location).mayEscape;
        }
        public void convertToFrame(int index) {
            location = new FrameVarLoc(ownerScope, index);
        }
        public void convertToRegister() {
            location = new RegisterVarLoc();
        }
    }
    static class ParameterVarDecl extends VarDecl {
        private int parameterIndex;
        public ParameterVarDecl(String name, int parameterIndex, ScopeHolder ownerScope) {
            super(name, VarDecl.Type.ARGUMENT, ownerScope);
            this.parameterIndex = parameterIndex;
        }
        public void convertToArguments() {
            location = new ArgumentsVarLoc(ownerScope, parameterIndex);
        }
        public int getParameterIndex() {
            return parameterIndex;
        }
    }

    static class VarLoc {
    }
    static class UnresolvedVarLoc extends VarLoc {
        private boolean mayEscape;
        public UnresolvedVarLoc() {
            mayEscape = false;
        }
        public void markMayEscape() {
            mayEscape = true;
        }
        public boolean mayEscape() {
            return mayEscape;
        }
    }
    static class RegisterVarLoc extends VarLoc {
        private final int NOT_ALLOCATED = -1;
        private int regNo;
        public RegisterVarLoc() {
            regNo = NOT_ALLOCATED;
        }
        public int getRegisterNo() {
            if (regNo == NOT_ALLOCATED)
                throw new Error("register number is not allocated");
            return regNo;
        }
        public void setRegisterNo(int regNo) {
            if (this.regNo != NOT_ALLOCATED)
                throw new Error("register number has already been allocated");
            this.regNo = regNo;
        }
    }
    static class HeapVarLoc extends VarLoc {
        private ScopeHolder ownerScope;
        public HeapVarLoc(ScopeHolder ownerScope) {
            this.ownerScope = ownerScope;
        }
        public int countStaticLink(ScopeHolder node) {
            int linkCount = 0;
            while (node != ownerScope) {
                if (node.frameSize() > 0)
                    linkCount++;
                node = node.getScope();
            }
            return linkCount;
        }
    }
    static class ArgumentsVarLoc extends HeapVarLoc {
        private int index;
        public ArgumentsVarLoc(ScopeHolder ownerScope, int index) {
            super(ownerScope);
            this.index = index;
        }
        public int getIndex() {
            return index;
        }
    }
    static class FrameVarLoc extends HeapVarLoc {
        private int index;
        public FrameVarLoc(ScopeHolder ownerScope, int index) {
            super(ownerScope);
            this.index = index;
        }
        int getIndex() {
            return index;
        }
    }
    static class GlobalVarLoc extends VarLoc {
    }

    static public interface ScopeHolder {
        /**
         * Iterate variable declarations from inner to outer,
         * i.e., local variables -> parameters -> "arguments"
         * for FunctionExpression.
         */
        public Iterable<VarDecl> getVarDecls();
        public IASTFunctionExpression getOwnerFunction();
        public ScopeHolder getScope();
        public int frameSize();
    }

    /*
     * Try-catch statement introduces a scope of the catch-clause.
     * To compute static link, we hold the inner most surrounding
     * function or catch-clause in ownerScope.  If try-catch
     * statements are absent, ownerScope is the same as ownerFunction.
     */
    private IASTFunctionExpression ownerFunction;
    private ScopeHolder ownerScope;
    IASTNode() {
        this.ownerFunction = null;
        this.ownerScope = null;
    }
    public void setOwner(IASTFunctionExpression ownerFunction, ScopeHolder ownerScope) {
        this.ownerFunction = ownerFunction;
        this.ownerScope = ownerScope;
    }
    public IASTFunctionExpression getOwnerFunction() {
        return ownerFunction;
    }
    public ScopeHolder getScope() {
        return ownerScope;
    }
    abstract Object accept(IASTBaseVisitor visitor);
}

class IASTProgram extends IASTNode {
    List<IASTFunctionExpression> programs;
    IASTProgram() {
        this.programs = new ArrayList<IASTFunctionExpression>();
    }
    void add(IASTFunctionExpression program) {
        programs.add(program);
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitProgram(this);
    }
}

class IASTLiteral extends IASTExpression {
    IASTLiteral() {
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitLiteral(this);
    }
}

class IASTStringLiteral extends IASTLiteral {
    String value;
    IASTStringLiteral(String value) {
        this.value = value;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitStringLiteral(this);
    }
}

class IASTNumericLiteral extends IASTLiteral {
    double value;
    IASTNumericLiteral(double value) {
        this.value = value;
    }
    boolean isInteger() {
        String s = Double.toString(value);
        if (s.charAt(s.length() - 1) == '0' && s.charAt(s.length() - 2) == '.')
            return true;
        else
            return false;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitNumericLiteral(this);
    }
}

class IASTBooleanLiteral extends IASTLiteral {
    boolean value;
    IASTBooleanLiteral(boolean value) {
        this.value = value;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitBooleanLiteral(this);
    }
}

class IASTRegExpLiteral extends IASTLiteral {
    String pattern;
    IASTRegExpLiteral(String pattern) {
        this.pattern = pattern;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitRegExpLiteral(this);
    }
}

class IASTNullLiteral extends IASTLiteral {
    IASTNullLiteral() {
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitNullLiteral(this);
    }
}

class IASTStatement extends IASTNode {
    IASTStatement() {
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitStatement(this);
    }
}

class IASTBlockStatement extends IASTStatement {
    List<IASTStatement> stmts;
    IASTBlockStatement(List<IASTStatement> stmts) {
        this.stmts = stmts;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitBlockStatement(this);
    }
}

class IASTExpressionStatement extends IASTStatement {
    IASTExpression exp;
    IASTExpressionStatement(IASTExpression exp) {
        this.exp = exp;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitExpressionStatement(this);
    }
}

class IASTReturnStatement extends IASTStatement {
    IASTExpression value;  /* can be null */
    IASTReturnStatement(IASTExpression value) {
        this.value = value;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitReturnStatement(this);
    }
}


class IASTWithStatement extends IASTStatement {
    IASTExpression object;
    IASTStatement body;
    IASTWithStatement(IASTExpression object, IASTStatement body) {
        this.object = object;
        this.body = body;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitWithStatement(this);
    }
}


class IASTIfStatement extends IASTStatement {
    IASTExpression test;
    IASTStatement consequent;
    IASTStatement alternate; /* can be null */
    IASTIfStatement(IASTExpression test, IASTStatement consequent, IASTStatement alternate) {
        this.test = test;
        this.consequent = consequent;
        this.alternate = alternate;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitIfStatement(this);
    }
}

class IASTSwitchStatement extends IASTStatement {
    static class CaseClause {
        IASTExpression test;  /* can be null for "default" */
        IASTStatement consequent;
        CaseClause(IASTExpression test, IASTStatement consequent) {
            this.test = test;
            this.consequent = consequent;
        }
    }
    IASTExpression discriminant;
    List<CaseClause> cases;
    String label;
    IASTSwitchStatement(IASTExpression discriminant, List<CaseClause> cases) {
        this.discriminant = discriminant;
        this.cases = cases;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitSwitchStatement(this);
    }
}

class IASTThrowStatement extends IASTStatement {
    IASTExpression value;
    IASTThrowStatement(IASTExpression value) {
        this.value = value;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitThrowStatement(this);
    }
}

class IASTTryCatchStatement extends IASTStatement implements IASTNode.ScopeHolder {
    IASTStatement body;
    VarDecl var;
    IASTStatement handler;

    /* following fields are filled by semantic analysis */
    ScopeHolder prevFrameHolder;

    IASTTryCatchStatement(IASTStatement body, String param, IASTStatement handler) {
        this.body = body;
        this.var = new VarDecl(param, VarDecl.Type.EXCEPTION, this);
        this.handler = handler;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitTryCatchStatement(this);
    }

    /* FrameHolder */
    @Override
    public Iterable<VarDecl> getVarDecls() {
        return new Iterable<VarDecl> () {
            @Override
            public Iterator<VarDecl> iterator() {
                return new Iterator<VarDecl>() {
                    private boolean first = true;
                    @Override
                    public boolean hasNext() {
                        return first;
                    }
                    @Override
                    public VarDecl next() {
                        if (first) {
                            first = false;
                            return var;
                        }
                        return null;
                    }
                };
            }
        };
    }
    @Override
    public int frameSize() {
        if (var.getLocation() instanceof HeapVarLoc)
            return 1;
        if (var.getLocation() instanceof UnresolvedVarLoc)
            throw new Error("location has not been resolved");
        return 0;
    }
}

class IASTTryFinallyStatement extends IASTStatement {
    IASTStatement body;
    IASTStatement finaliser;
    IASTTryFinallyStatement(IASTStatement body, IASTStatement finaliser) {
        this.body = body;
        this.finaliser = finaliser;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitTryFinallyStatement(this);
    }
}

class IASTForStatement extends IASTStatement {
    IASTExpression init;
    IASTExpression test;
    IASTExpression update;
    IASTStatement body;
    String label;
    IASTForStatement(IASTExpression init, IASTExpression test, IASTExpression update, IASTStatement body) {
        this.init = init;
        this.test = test;
        this.update = update;
        this.body = body;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitForStatement(this);
    }
}

class IASTWhileStatement extends IASTStatement {
    IASTExpression test;
    IASTStatement body;
    String label;
    IASTWhileStatement(IASTExpression test, IASTStatement body) {
        this.test = test;
        this.body = body;

    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitWhileStatement(this);
    }
}


class IASTDoWhileStatement extends IASTStatement {
    IASTExpression test;
    IASTStatement body;
    String label;
    IASTDoWhileStatement(IASTExpression test, IASTStatement body) {
        this.test = test;
        this.body = body;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitDoWhileStatement(this);
    }
}

class IASTForInStatement extends IASTStatement {
    IASTIdentifier var;
    IASTExpression object;
    IASTStatement body;
    String label;
    IASTForInStatement(IASTIdentifier var, IASTExpression object, IASTStatement body) {
        this.var = var;
        this.object = object;
        this.body = body;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitForInStatement(this);
    }
}

class IASTBreakStatement extends IASTStatement {
    String label; // can be null
    IASTBreakStatement(String label) {
        this.label = label;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitBreakStatement(this);
    }
}

class IASTContinueStatement extends IASTStatement {
    String label; // can be null
    IASTContinueStatement(String label) {
        this.label = label;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitContinueStatement(this);
    }
}

class IASTExpression extends IASTNode {
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitExpression(this);
    }
}

class IASTThisExpression extends IASTExpression {
    IASTThisExpression() {}
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitThisExpression(this);
    }
}

class IASTArrayExpression extends IASTExpression {
    List<IASTExpression> elements;
    IASTArrayExpression(List<IASTExpression> elements) {
        this.elements = elements;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitArrayExpression(this);
    }
}

class IASTObjectExpression extends IASTExpression {
    static class Property {
        static enum Kind {
            INIT, GET, SET
        };
        IASTLiteral key;  /* identifier is a string literal */
        IASTExpression value;
        Kind kind;
        Property(IASTLiteral key, IASTExpression value, Kind kind) {
            this.key = key;
            this.value = value;
            this.kind = kind;
        }
    }
    List<Property> properties;
    IASTObjectExpression(List<Property> properties) {
        this.properties = properties;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitObjectExpression(this);
    }
}

class IASTFunctionExpression extends IASTExpression implements IASTNode.ScopeHolder {
    ParameterVarDecl[] params;
    VarDecl[] locals;
    IASTStatement body;
    boolean topLevel;
    boolean logging;

    /* following members are filled by semantic analysis */
    public boolean needArguments;
    public int frameSize;
    public VarDecl argumentsArrayDecl;

    IASTFunctionExpression(List<String> params, List<String> locals, IASTStatement body, boolean topLevel, boolean logging) {
        this.params = new ParameterVarDecl[params.size()];
        for (int i = 0; i < params.size(); i++)
            this.params[i] = new ParameterVarDecl(params.get(i), i, this);
        this.locals = locals.stream().map(name -> new VarDecl(name, VarDecl.Type.LOCAL_VAR, this)).toArray(VarDecl[]::new);
        this.body = body;
        this.topLevel = topLevel;
        this.logging = logging;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitFunctionExpression(this);
    }

    public VarDecl createArgumentsArray() {
        assert(!topLevel);
        if (argumentsArrayDecl == null)
            argumentsArrayDecl = new VarDecl("arguments", VarDecl.Type.LOCAL_VAR, this);
        return argumentsArrayDecl;
    }

    public VarDecl getArgumentsArray() {
        return argumentsArrayDecl;
    }

    public boolean needArguments() {
        return argumentsArrayDecl != null;
    }

    /* FrameHolder */
    @Override
    public Iterable<VarDecl> getVarDecls() {
        ArrayList<VarDecl> list = new ArrayList<VarDecl>(locals.length + params.length);
        list.addAll(Arrays.asList(locals));
        list.addAll(Arrays.asList(params));
        return list;
    }

    @Override
    public int frameSize() {
        return frameSize;
    }
}

class IASTOperatorExpression extends IASTExpression {
    IASTExpression[] operands;
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitOperatorExpression(this);
    }
}

class IASTUnaryExpression extends IASTOperatorExpression {
    static enum Operator {
        PLUS, MINUS, NOT, BNOT, TYPEOF, VOID, DELETE, INC, DEC
    }
    Operator operator;
    boolean prefix;
    IASTUnaryExpression(Operator operator, IASTExpression operand, boolean prefix) {
        this.operator = operator;
        this.operands = new IASTExpression[1];
        operands[0] = operand;
        this.prefix = prefix;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitUnaryExpression(this);
    }
}

class IASTBinaryExpression extends IASTOperatorExpression {
    static enum Operator {
        /* relational */
        EQUAL, NOT_EQUAL, EQ, NOT_EQ, LT, LTE, GT, GTE,
        IN, INSTANCE_OF,
        /* arithmetic + shift */
        ADD, SUB, MUL, DIV, MOD, SHL, SHR, UNSIGNED_SHR,
        /* bitwise logical */
        BAND, BOR, BXOR,
        /* compound */
        ASSIGN,
        ASSIGN_ADD, ASSIGN_SUB, ASSIGN_MUL, ASSIGN_DIV, ASSIGN_MOD,
        ASSIGN_SHL, ASSIGN_SHR, ASSIGN_UNSIGNED_SHR,
        ASSIGN_BAND, ASSIGN_BOR, ASSIGN_BXOR,
        /* logical */
        AND, OR
    }
    Operator operator;
    IASTBinaryExpression(Operator operator, IASTExpression op1, IASTExpression op2) {
        this.operator = operator;
        this.operands = new IASTExpression[2];
        this.operands[0] = op1;
        this.operands[1] = op2;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitBinaryExpression(this);
    }
}

class IASTTernaryExpression extends IASTOperatorExpression {
    static enum Operator {
        COND
    }
    Operator operator;
    IASTTernaryExpression(Operator operator, IASTExpression op1, IASTExpression op2, IASTExpression op3) {
        this.operator = operator;
        this.operands = new IASTExpression[3];
        this.operands[0] = op1;
        this.operands[1] = op2;
        this.operands[2] = op3;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitTernaryExpression(this);
    }
}

class IASTCallExpression extends IASTExpression {
    IASTExpression callee;
    List<IASTExpression> arguments;
    IASTCallExpression(IASTExpression callee, List<IASTExpression> arguments) {
        this.callee = callee;
        this.arguments = arguments;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitCallExpression(this);
    }
}

class IASTNewExpression extends IASTExpression {
    IASTExpression constructor;
    List<IASTExpression> arguments;
    IASTNewExpression(IASTExpression constructor, List<IASTExpression> arguments) {
        this.constructor = constructor;
        this.arguments = arguments;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitNewExpression(this);
    }
}

class IASTMemberExpression extends IASTExpression {
    IASTExpression object;
    IASTExpression property;
    IASTMemberExpression(IASTExpression object, IASTExpression property) {
        this.object = object;
        this.property = property;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitMemberExpression(this);
    }
}

class IASTIdentifier extends IASTExpression {
    String id;
    VarDecl decl;
    IASTIdentifier(String id) {
        this.id = id;
        this.decl = null;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitIdentifier(this);
    }
    public void setDeclaration(VarDecl decl) {
        this.decl = decl;
    }
    public VarDecl getDeclaration() {
        return decl;
    }
}

class IASTSequenceExpression extends IASTExpression {
    List<IASTExpression> expressions;
    IASTSequenceExpression(List<IASTExpression> expressions) {
        this.expressions = expressions;
    }
    @Override
    Object accept(IASTBaseVisitor visitor) {
        return visitor.visitSequenceExpression(this);
    }
}

class IASTEmptyStatement extends IASTStatement {}

