/*
   IASTNode.java

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
import java.util.HashSet;
import java.util.List;

abstract public class IASTNode {
	abstract Object accept(IASTBaseVisitor visitor);
	// abstract void compile(CodeGenerator.BCBuilder bcb, CodeGenerator.Environment env, Register reg)
}

class IASTProgram extends IASTNode {
	IASTFunctionExpression program;
	IASTProgram(IASTFunctionExpression program) {
		this.program = program;
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

class IASTTryCatchStatement extends IASTStatement {
	IASTStatement body;
	String param;
	IASTStatement handler;
	IASTTryCatchStatement(IASTStatement body, String param, IASTStatement handler) {
		this.body = body;
		this.param = param;
		this.handler = handler;
	}
	@Override
	Object accept(IASTBaseVisitor visitor) {
		return visitor.visitTryCatchStatement(this);
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
	String var;
	IASTExpression object;
	IASTStatement body;
	String label;
	IASTForInStatement(String var, IASTExpression object, IASTStatement body) {
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

class IASTFunctionExpression extends IASTExpression {
	List<String> params;
	List<String> locals;
	HashSet<String> innerUsedLocals;
	IASTStatement body;
	public boolean needArguments;
	public boolean needFrame;
	IASTFunctionExpression(List<String> params, List<String> locals, IASTStatement body) {
		this.params = params;
		this.locals = locals;
		this.body = body;
		this.needArguments = true;
		this.needFrame = true;
	}
	@Override
	Object accept(IASTBaseVisitor visitor) {
		return visitor.visitFunctionExpression(this);
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
	IASTIdentifier(String id) {
		this.id = id;
	}
	@Override
	Object accept(IASTBaseVisitor visitor) {
		return visitor.visitIdentifier(this);
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

