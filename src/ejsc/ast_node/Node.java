/*
   Node.java

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
package ejsc.ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.antlr.v4.runtime.Token;



public abstract class Node {

    public static final int IDENTIFIER = 1;
    public static final int LITERAL = 2;
    public static final int PROGRAM = 3;
    public static final int FUNCTION = 4;
    public static final int EXP_STMT = 5;
    public static final int BLOCK_STMT = 6;
    public static final int EMPTY_STMT = 7;
    public static final int DEBUGGER_STMT = 8;
    public static final int WITH_STMT = 9;
    public static final int RETURN_STMT = 10;
    public static final int LABELED_STMT = 11;
    public static final int BREAK_STMT = 12;
    public static final int CONTINUE_STMT = 13;
    public static final int IF_STMT = 14;
    public static final int SWITCH_STMT = 15;
    public static final int SWITCH_CASE = 16;
    public static final int THROW_STMT = 17;
    public static final int TRY_STMT = 18;
    public static final int CATCH_CLAUSE = 19;
    public static final int FINALIZER = 20;
    public static final int WHILE_STMT = 21;
    public static final int DO_WHILE_STMT = 22;
    public static final int FOR_STMT = 23;
    public static final int FOR_IN_STMT = 24;
    public static final int FUNC_DECLARATION = 25;
    public static final int VAR_DECLARATION = 26;
    public static final int VAR_DECLARATOR = 27;
    public static final int THIS_EXP = 28;
    public static final int ARRAY_EXP = 29;
    public static final int OBJECT_EXP = 30;
    public static final int PROPERTY = 31;
    public static final int FUNC_EXP = 32;
    public static final int UNARY_EXP = 33;
    public static final int UNARY_OP = 34;
    public static final int UPDATE_EXP = 35;
    public static final int UPDATE_OP = 36;
    public static final int BINARY_EXP = 37;
    public static final int BINARY_OP = 38;
    public static final int ASSIGNMENT_EXP = 39;
    public static final int ASSIGNMENT_OP = 40;
    public static final int LOGICAL_EXP = 41;
    public static final int LOGICAL_OP = 42;
    public static final int MEMBER_EXP = 43;
    public static final int CONDITIONAL_EXP = 44;
    public static final int CALL_EXP = 45;
    public static final int NEW_EXP = 46;
    public static final int SEQUENCE_EXP = 47;

    public static final int SOURCE_ELEMENTS = 2;
    public static final int VAR_STMT = 3;
    public static final int VAR_DECL_LIST = 4;
    // public static final int VAR_DECL = 5;

    public static final String[] typeStr = {
            "Program", "VariableDeclaration", "VariableDeclarator", "Identifier",
    };

    public static final String KEY_TYPE = "type";
    public static final String KEY_LOC = "loc";
    public static final String KEY_SOURCE = "source";
    public static final String KEY_START = "start";
    public static final String KEY_END = "end";
    public static final String KEY_LINE = "line";
    public static final String KEY_COLUMN = "column";
    public static final String KEY_NAME = "name";
    public static final String KEY_REGEX = "regex";
    public static final String KEY_PATTERN = "pattern";
    public static final String KEY_FLAGS = "flags";
    public static final String KEY_BODY = "body";
    public static final String KEY_ID = "id";
    public static final String KEY_PARAMS = "params";
    public static final String KEY_EXPRESSION = "expression";
    public static final String KEY_OBJECT = "object";
    public static final String KEY_ARGUMENT = "argument";
    public static final String KEY_LABEL = "label";
    public static final String KEY_TEST = "test";
    public static final String KEY_CONSEQUENT = "consequent";
    public static final String KEY_ALTERNATE = "alternate";
    public static final String KEY_DISCRIMINANT = "discriminant";
    public static final String KEY_CASES = "cases";
    public static final String KEY_BLOCK = "block";
    public static final String KEY_HANDLER = "handler";
    public static final String KEY_FINALIZER = "finalizer";
    public static final String KEY_PARAM = "param";
    public static final String KEY_INIT = "init";
    public static final String KEY_UPDATE = "update";
    public static final String KEY_LEFT = "left";
    public static final String KEY_RIGHT = "right";
    public static final String KEY_DECLARATIONS = "declarations";
    public static final String KEY_KIND = "kind";
    public static final String KEY_ELEMENTS = "elements";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_KEY = "key";
    public static final String KEY_VALUE = "value";
    public static final String KEY_PREFIX = "prefix";
    public static final String KEY_OPERATOR = "operator";
    public static final String KEY_PROPERTY = "property";
    public static final String KEY_COMPUTED = "computed";
    public static final String KEY_CALLEE = "callee";
    public static final String KEY_ARGUMENTS = "arguments";
    public static final String KEY_EXPRESSIONS = "expressions";

    public interface INode {
        public int getTypeId();
        // public SourceLocation getLoc();
        public JsonObject getEsTree();
        public Object accept(ESTreeBaseVisitor visitor);
    }

    public interface IIdentifier extends IExpression, IPattern {
        public String getName();
    }

    public interface ILiteral extends IExpression {
        public enum LiteralType { STRING, BOOLEAN, NULL, NUMBER, REG_EXP };
        public LiteralType getLiteralType();
        public String getStringValue();
        public boolean getBooleanValue();
        public double getNumValue();
        public String getRegExpValue();
    }

    public interface IRegExpLiteral extends ILiteral {
        class Regex {
            String pattern;
            String flags;
        }
        public Regex getRegex();
    }

    public interface IProgram extends INode {
        public List<IStatement> getBody();
        public void setBody(List<IStatement> body);
    }

    public interface IFunction extends INode {
        public IIdentifier getId();
        public List<IPattern> getParams();
        public IBlockStatement getBody();
        public void setId(IIdentifier id);
        public void setParams(List<IPattern> params);
        public void setBody(IBlockStatement body);
    }

    public interface IStatement extends INode {}

    public interface IExpressionStatement extends IStatement {
        public IExpression getExpression();
    }

    public interface IBlockStatement extends IStatement {
        public List<IStatement>  getBody();
        public void setBody(List<IStatement> body);
    }

    public interface IEmptyStatement extends IStatement {}

    public interface IDebuggerStatement extends IStatement {}

    public interface IWithStatement extends IStatement {
        public IExpression getObject();
        public IStatement getBody();
    }

    public interface IReturnStatement extends IStatement {
        public IExpression getArgument();
    }

    public interface ILabeledStatement extends IStatement {
        public IIdentifier getLabel();
        public IStatement getBody();
    }

    public interface IBreakStatement extends IStatement {
        public IIdentifier getLabel();
    }

    public interface IContinueStatement extends IStatement {
        public IIdentifier getLabel();
    }

    public interface IIfStatement extends IStatement {
        public IExpression getTest();
        public IStatement getConsequent();
        public IStatement getAlternate();
    }

    public interface ISwitchStatement extends IStatement {
        public IExpression getDiscriminant();
        public List<ISwitchCase> getCases();
    }

    public interface ISwitchCase extends INode {
        public IExpression getTest();
        public List<IStatement> getConsequent();
        public void setTest(IExpression test);
        public void setConsequent(List<IStatement> consequent);
    }

    public interface IThrowStatement extends IStatement {
        public IExpression getArgument();
    }

    public interface ITryStatement extends IStatement {
        public IBlockStatement getBlock();
        public ICatchClause getHandler();
        public IBlockStatement getFinalizer();
    }

    public interface ICatchClause extends INode {
        public IPattern getParam();
        public IBlockStatement getBody();
    }

    public interface IWhileStatement extends IStatement {
        public IExpression getTest();
        public IStatement getBody();
        public String getLabel();
        public void setLabel(String label);
    }

    public interface IDoWhileStatement extends IStatement {
        public IStatement getBody();
        public IExpression getTest();
        public String getLabel();
        public void setLabel(String label);
    }

    public interface IForStatement extends IStatement {
        public IVariableDeclaration getValDeclInit();
        public IExpression getExpInit();
        public IExpression getTest();
        public IExpression getUpdate();
        public IStatement getBody();
        public String getLabel();
        public void setLabel(String label);
    }

    public interface IForInStatement extends IStatement {
        public IVariableDeclaration getValDeclLeft();
        public IPattern getPatternLeft();
        public IExpression getRight();
        public IStatement getBody();
        public String getLabel();
        public void setLabel(String label);
    }

    public interface IDeclaration extends IStatement {}

    public interface IFunctionDeclaration extends IFunction, IDeclaration {
        public IIdentifier getId();
    }

    public interface IVariableDeclaration extends IDeclaration {
        public List<IVariableDeclarator> getDeclarations();
        public String getKind();
    }

    public interface IVariableDeclarator extends INode {
        public IPattern getId();
        public IExpression getInit();
    }

    public interface IExpression extends INode {}

    public interface IThisExpression extends IExpression {}

    public interface IArrayExpression extends IExpression {
        public List<IExpression> getElements();
    }

    public interface IObjectExpression extends IExpression {
        public List<IProperty> getProperties();
    }

    public interface IProperty extends INode {
        public ILiteral getLiteralKey();
        public IIdentifier getIdentifierKey();
        public IExpression getValue();
        public String getKind();
    }

    public interface IFunctionExpression extends IFunction, IExpression {}

    public interface IUnaryExpression extends IExpression {
        public String getOperator();
        public boolean getPrefix();
        public IExpression getArgument();
    }

    public interface IUpdateExpression extends IExpression {
        public String getOperator();
        public IExpression getArgument();
        public boolean getPrefix();
    }

    public interface IBinaryExpression extends IExpression {
        public BinaryExpression.BinaryOperator getOperator();
        public IExpression getLeft();
        public IExpression getRight();
    }

    public interface IAssignmentExpression extends IExpression {
        public AssignmentExpression.AssignmentOperator getOperator();
        public IPattern getPatternLeft();
        public IExpression getExpressionLeft();
        public IExpression getRight();
    }

    public interface ILogicalExpression extends IExpression {
        public LogicalExpression.LogicalOperator getOperator();
        public IExpression getLeft();
        public IExpression getRight();
    }

    public interface IMemberExpression extends IExpression, IPattern {
        public IExpression getObject();
        public IExpression getProperty();
        public boolean getComputed();
    }

    public interface IConditionalExpression extends IExpression {
        public IExpression getTest();
        public IExpression getAlternate();
        public IExpression getConsequent();
    }

    public interface ICallExpression extends IExpression {
        public IExpression getCallee();
        public List<IExpression> getArguments();
    }

    public interface INewExpression extends ICallExpression {}

    public interface ISequenceExpression extends IExpression {
        public List<IExpression> getExpression();
    }

    public interface IPattern extends INode {}

    protected class Position {
        protected int line;
        protected int column;

        protected JsonObject getAstWithJson() {
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                    .add(KEY_LINE, line)
                    .add(KEY_COLUMN, column);
            return jsonBuilder.build();
        }
    }

    protected class SourceLocation {
        protected SourceLocation() {
            start = new Position();
            end = new Position();
        }

        protected String source;
        protected Position start;
        protected Position end;

        protected JsonObject getAstWithJson() {
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                    .add(KEY_START, this.start.getAstWithJson())
                    .add(KEY_END, this.end.getAstWithJson());
            return jsonBuilder.build();
        }
    }

    protected int type;
    public SourceLocation loc;

    public void setSourceLocation(String source, int startLine,
            int startColumn, int endLine, int endColumn) {
        loc = new SourceLocation();
        loc.source = source;
        loc.start.line = startLine;
        loc.start.column = startColumn;
        loc.end.line = endLine;
        loc.end.column = endColumn;
    }

    public void setSourceLocation(String source, Token start, Token stop) {
        setSourceLocation(source, start.getLine(), start.getCharPositionInLine(),
                stop.getLine(), stop.getCharPositionInLine() + stop.getText().length());
    }

    public int getTypeId() {
        return type;
    }

    public abstract JsonObject getEsTree();
}
