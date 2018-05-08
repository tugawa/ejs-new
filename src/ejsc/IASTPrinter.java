/*
   IASTPrinter.java

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
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class IASTPrinter extends IASTBaseVisitor {
	public static final String KEY_NAME              = "name";
	public static final String KEY_PROGRAM           = "program";
	public static final String KEY_PARAMS            = "params";
	public static final String KEY_LOCALS            = "locals";
	public static final String KEY_INNER_USED_LOCALS = "innerUsedLocals";
	public static final String KEY_BODY              = "body";
	public static final String KEY_NEED_ARGUMENTS    = "needArguments";
	public static final String KEY_NEED_FRAME        = "needFrame";
	public static final String KEY_STMTS             = "stmts";
	public static final String KEY_VALUE             = "value";
	public static final String KEY_TEST              = "test";
	public static final String KEY_CONSEQUENT        = "consequent";
	public static final String KEY_ALTERNATE         = "alternate";
	public static final String KEY_DISCRIMINANT      = "discriminant";
	public static final String KEY_CASES             = "cases";
	public static final String KEY_HANDLER           = "handler";
	public static final String KEY_FINALISER         = "finaliser";
	public static final String KEY_INIT              = "init";
	public static final String KEY_UPDATE            = "update";
	public static final String KEY_EXP               = "exp";
	public static final String KEY_OBJECT            = "object";
	public static final String KEY_ELEMENTS          = "elements";
	public static final String KEY_KEY               = "key";
	public static final String KEY_PROPERTIES        = "properties";
	public static final String KEY_OPERANDS          = "operands";
	public static final String KEY_OPERATOR          = "operator";
	public static final String KEY_PREFIX            = "prefix";
	public static final String KEY_CALLEE            = "callee";
	public static final String KEY_ARGUMENTS         = "arguments";
	public static final String KEY_CONSTRUCTOR       = "constructor";
	public static final String KEY_PROPERTY          = "property";
	public static final String KEY_ID                = "id";
	public static final String KEY_LABEL             = "label";
	public static final String KEY_EXPRESSIONS       = "expressions";
	public static final String KEY_VAR               = "var";

	public void print(IASTNode iast) {
		JsonObject json = (JsonObject) iast.accept(this);
		System.out.println(json.toString());
	}

	public Object visitProgram(IASTProgram node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "Program");
		jb.add(KEY_PROGRAM, (JsonObject) node.program.accept(this));
		return jb.build();
	}
	public Object visitLiteral(IASTLiteral node) {
		return Json.createObjectBuilder().add(KEY_NAME, "Literal").build();
	}
	public Object visitStringLiteral(IASTStringLiteral node) {
		JsonObjectBuilder jb = Json.createObjectBuilder().add(KEY_NAME, "StringLiteral");
		jb.add(KEY_VALUE, node.value);
		return jb.build();
	}
	public Object visitNumericLiteral(IASTNumericLiteral node) {
		JsonObjectBuilder jb = Json.createObjectBuilder().add(KEY_NAME, "NumericLiteral");
		jb.add(KEY_VALUE, node.value);
		return jb.build();
	}
	public Object visitBooleanLiteral(IASTBooleanLiteral node) {
		JsonObjectBuilder jb = Json.createObjectBuilder().add(KEY_NAME, "BooleanLiteral");
		jb.add(KEY_VALUE, node.value);
		return jb.build();
	}
	public Object visitRegExpLiteral(IASTRegExpLiteral node) {
		JsonObjectBuilder jb = Json.createObjectBuilder().add(KEY_NAME, "RegExpLiteral");
		jb.add(KEY_VALUE, node.pattern);
		return jb.build();
	}
	public Object visitNullLiteral(IASTNullLiteral node) {
		JsonObjectBuilder jb = Json.createObjectBuilder().add(KEY_NAME, "NullLiteral");
		return jb.build();
	}
	public Object visitStatement(IASTStatement node) {
		return Json.createObjectBuilder().add(KEY_NAME, "Statement").build();
	}
	public Object visitBlockStatement(IASTBlockStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "BlockStatement");
		// stmts
		JsonArrayBuilder jaStmts = Json.createArrayBuilder();
		for (IASTStatement s: node.stmts) {
			jaStmts.add((JsonObject) s.accept(this));
		}
		jb.add(KEY_STMTS, jaStmts);
		return jb.build();
	}
	public Object visitExpressionStatement(IASTExpressionStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "ExpressionStatement");
		// exp
		jb.add(KEY_EXP, (JsonObject) node.exp.accept(this));
		return jb.build();
	}
	public Object visitReturnStatement(IASTReturnStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "ReturnStatement");
		// value
		if (node.value != null) {
			jb.add(KEY_VALUE, (JsonObject) node.value.accept(this));
		} else {
			jb.add(KEY_VALUE, JsonObject.NULL);
		}
		return jb.build();
	}
	public Object visitEmptyStatement(IASTEmptyStatement node) {
	    JsonObjectBuilder jb = Json.createObjectBuilder();
	    jb.add(KEY_NAME, "EmptyStatement");
	    return jb.build();
	}
	public Object visitWithStatement(IASTWithStatement node) {
	    JsonObjectBuilder jb = Json.createObjectBuilder();
	    jb.add(KEY_NAME, "WithStatement");
	    // object
	    jb.add(KEY_OBJECT, (JsonObject) node.object.accept(this));
	    // body
	    jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
	    return jb.build();
	}
	public Object visitIfStatement(IASTIfStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "IfStatement");
		jb.add(KEY_TEST, (JsonObject) node.test.accept(this));
		jb.add(KEY_CONSEQUENT, (JsonObject) node.consequent.accept(this));
		if (node.alternate != null) {
			jb.add(KEY_ALTERNATE, (JsonObject) node.alternate.accept(this));
		} else {
			jb.add(KEY_ALTERNATE,  JsonObject.NULL);
		}
		return jb.build();
	}
	public Object visitSwitchStatement(IASTSwitchStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "SwitchStatement");
		// discriminant
		jb.add(KEY_DISCRIMINANT, (JsonObject) node.discriminant.accept(this));
		// cases
		JsonArrayBuilder jaCases = Json.createArrayBuilder();
		for (IASTSwitchStatement.CaseClause c: node.cases) {
			JsonObjectBuilder jbCase = Json.createObjectBuilder();
			if (c.test != null) {
				jbCase.add(KEY_TEST, (JsonObject) c.test.accept(this));
			} else {
				jbCase.add(KEY_TEST,  JsonObject.NULL);
			}
			jbCase.add(KEY_CONSEQUENT, (JsonObject) c.consequent.accept(this));
			jaCases.add(jbCase);
		}
		jb.add(KEY_CASES, jaCases);
		return jb.build();
	}
	public Object visitThrowStatement(IASTThrowStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "ThrowStatement");
		// value
		jb.add(KEY_VALUE, (JsonObject) node.value.accept(this));
		return jb.build();
	}
	public Object visitTryCatchStatement(IASTTryCatchStatement node){
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "TryCatchStatement");
		// body
		jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
		// handler
		jb.add(KEY_HANDLER, (JsonObject) node.handler.accept(this));
		return jb.build();
	}
	public Object visitTryFinallyStatement(IASTTryFinallyStatement node){
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "TryFinally");
		// body
		jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
		// finaliser
		jb.add(KEY_FINALISER, (JsonObject) node.finaliser.accept(this));
		return jb.build();
	}
	public Object visitForStatement(IASTForStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "ForStatement");
		// init
		if (node.init != null) {
			jb.add(KEY_INIT, (JsonObject) node.init.accept(this));
		} else {
			jb.add(KEY_INIT, JsonObject.NULL);
		}
		// test
		if (node.test != null) {
			jb.add(KEY_TEST, (JsonObject) node.test.accept(this));
		} else {
			jb.add(KEY_TEST, JsonObject.NULL);
		}
		// update
		if (node.update != null) {
			jb.add(KEY_UPDATE, (JsonObject) node.update.accept(this));
		} else {
			jb.add(KEY_UPDATE, JsonObject.NULL);
		}
		// body
		jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
		return jb.build();
	}
	public Object visitWhileStatement(IASTWhileStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "WhileStatement");
		// test
		jb.add(KEY_TEST, (JsonObject) node.test.accept(this));
		// body
		jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
		return jb.build();
	}
	public Object visitDoWhileStatement(IASTDoWhileStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "DoWhileStatement");
		// test
		jb.add(KEY_TEST, (JsonObject) node.test.accept(this));
		// body
		jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
		return jb.build();
	}
	public Object visitForInStatement(IASTForInStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "ForInStatement");
		// var
		jb.add(KEY_VAR, node.var);
		// object
		jb.add(KEY_OBJECT, (JsonObject) node.object.accept(this));
		// body
		jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
		return jb.build();
	}
	public Object visitBreakStatement(IASTBreakStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "BreakStatement");
		// label
		if (node.label != null) {
			jb.add(KEY_LABEL, node.label);
		} else {
			jb.add(KEY_LABEL, JsonObject.NULL);
		}
		return jb.build();
	}
	public Object visitContinueStatement(IASTContinueStatement node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		// label
		jb.add(KEY_NAME, "BreakStatement");
		if (node.label != null) {
			jb.add(KEY_LABEL, node.label);
		} else {
			jb.add(KEY_LABEL, JsonObject.NULL);
		}
		return jb.build();
	}
	public Object visitExpression(IASTExpression node) {
		return Json.createObjectBuilder().add(KEY_NAME, "Expression").build();
	}
	public Object visitThisExpression(IASTThisExpression node) {
		return Json.createObjectBuilder().add(KEY_NAME, "ThisExpression").build();
	}
	public Object visitArrayExpression(IASTArrayExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "ArrayExpression");
		// elements
		JsonArrayBuilder jaElements = Json.createArrayBuilder();
		for (IASTExpression e: node.elements) {
			jaElements.add((JsonObject) e.accept(this));
		}
		jb.add(KEY_ELEMENTS, jaElements);
		return jb.build();
	}
	public Object visitObjectExpression(IASTObjectExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "ObjectExpression");
		// properties
		JsonArrayBuilder jaProperties = Json.createArrayBuilder();
		for (IASTObjectExpression.Property p: node.properties) {
			JsonObjectBuilder jbp = Json.createObjectBuilder();
			jbp.add(KEY_KEY, (JsonObject) p.key.accept(this));
			jbp.add(KEY_VALUE, (JsonObject) p.value.accept(this));
			jaProperties.add(jbp);
		}
		jb.add(KEY_PROPERTIES, jaProperties);
		return jb.build();
	}
	public Object visitFunctionExpression(IASTFunctionExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "FunctionExpression");
		// params
		JsonArrayBuilder jaParams = Json.createArrayBuilder();
		for (String param : node.params) {
			jaParams.add(param);
		}
		jb.add(KEY_PARAMS, jaParams);
		// locals
		JsonArrayBuilder jaLocals = Json.createArrayBuilder();
		for (String local : node.locals) {
			jaLocals.add(local);
		}
		jb.add(KEY_LOCALS, jaLocals);
		// innerUsedLocals
		JsonArrayBuilder jaInnerUseLocals = Json.createArrayBuilder();
		for (String local : node.innerUsedLocals) {
			jaInnerUseLocals.add(local);
		}
		jb.add(KEY_INNER_USED_LOCALS, jaInnerUseLocals);
		// body
		jb.add(KEY_BODY, (JsonObject) node.body.accept(this));
		// needArguments and needFrame
		jb.add(KEY_NEED_ARGUMENTS, node.needArguments);
		jb.add(KEY_NEED_FRAME, node.needFrame);
		return jb.build();
	}
	public Object visitOperatorExpression(IASTOperatorExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		// operands
		JsonArrayBuilder jaOperands = Json.createArrayBuilder();
		for(IASTExpression e: node.operands) {
			jaOperands.add((JsonObject) e.accept(this));
		}
		jb.add(KEY_OPERANDS, jaOperands);
		return jb.build();
	}
	public Object visitUnaryExpression(IASTUnaryExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "UnaryExpression");
		// operator
		switch (node.operator) {
		case PLUS: {
			jb.add(KEY_OPERATOR, "PLUS (+)");
		} break;
		case MINUS: {
			jb.add(KEY_OPERATOR, "MINUS (-)");
		} break;
		case NOT: {
			jb.add(KEY_OPERATOR, "NOT (!)");
		} break;
		case BNOT: {
			jb.add(KEY_OPERATOR, "BNOT (~)");
		} break;
		case TYPEOF: {
			jb.add(KEY_OPERATOR, "TYPEOF (typeof)");
		} break;
		case VOID: {
			jb.add(KEY_OPERATOR, "VOID (void)");
		} break;
		case DELETE: {
			jb.add(KEY_OPERATOR, "DELETE (delete)");
		} break;
		case INC: {
			jb.add(KEY_OPERATOR, "INC (++)");
		} break;
		case DEC: {
			jb.add(KEY_OPERATOR, "DEC (--)");
		} break;
		}
		// prefix
		jb.add(KEY_PREFIX, node.prefix);
		// operands
		JsonArrayBuilder jaOperands = Json.createArrayBuilder();
		for(IASTExpression e: node.operands) {
			jaOperands.add((JsonObject) e.accept(this));
		}
		jb.add(KEY_OPERANDS, jaOperands);
		return jb.build();
	}

	public Object visitBinaryExpression(IASTBinaryExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "BinaryExpression");
		// operator
		switch (node.operator) {
		case EQUAL: {
			jb.add(KEY_OPERATOR, "EQUAL (==)");
		} break;
		case NOT_EQUAL: {
			jb.add(KEY_OPERATOR, "NOT_EQUAL (!=)");
		} break;
		case EQ: {
			jb.add(KEY_OPERATOR, "EQ (===)");
		} break;
		case NOT_EQ:{
			jb.add(KEY_OPERATOR, "NOT_EQ (!==)");
		} break;
		case LT: {
			jb.add(KEY_OPERATOR, "LT (<)");
		} break;
		case LTE: {
			jb.add(KEY_OPERATOR, "LTE (<=)");
		} break;
		case GT: {
			jb.add(KEY_OPERATOR, "GT (>)");
		} break;
		case GTE: {
			jb.add(KEY_OPERATOR, "GTE (>=)");
		} break;
		case IN: {
			jb.add(KEY_OPERATOR, "IN (in)");
		} break;
		case INSTANCE_OF: {
			jb.add(KEY_OPERATOR, "INSTANCE_OF (instanceof)");
		} break;
		case ADD: {
			jb.add(KEY_OPERATOR, "ADD (+)");
		} break;
		case SUB: {
			jb.add(KEY_OPERATOR, "SUB (-)");
		} break;
		case MUL: {
			jb.add(KEY_OPERATOR, "MUL (*)");
		} break;
		case DIV: {
			jb.add(KEY_OPERATOR, "DIV (/)");
		} break;
		case MOD: {
			jb.add(KEY_OPERATOR, "MOD (%)");
		} break;
		case SHL: {
			jb.add(KEY_OPERATOR, "SHL (<<)");
		} break;
		case SHR: {
			jb.add(KEY_OPERATOR, "SHR (>>)");
		} break;
		case UNSIGNED_SHR: {
			jb.add(KEY_OPERATOR, "UNSIGNED_SHR (>>>)");
		} break;
		case BAND: {
			jb.add(KEY_OPERATOR, "BAND (&)");
		} break;
		case BOR: {
			jb.add(KEY_OPERATOR, "BOR (|)");
		} break;
		case BXOR: {
			jb.add(KEY_OPERATOR, "BXOR (^)");
		} break;
		case ASSIGN: {
			jb.add(KEY_OPERATOR, "ASSIGN (=)");
		} break;
		case ASSIGN_ADD: {
			jb.add(KEY_OPERATOR, "ASSIGN_ADD (+=)");
		} break;
		case ASSIGN_SUB: {
			jb.add(KEY_OPERATOR, "ASSIGN_SUB (-=)");
		} break;
		case ASSIGN_MUL: {
			jb.add(KEY_OPERATOR, "ASSIGN_MUL (*=)");
		} break;
		case ASSIGN_DIV: {
			jb.add(KEY_OPERATOR, "ASSIGN_DIV (/=)");
		} break;
		case ASSIGN_MOD: {
			jb.add(KEY_OPERATOR, "ASSIGN_MOD (%=)");
		} break;
		case ASSIGN_SHL: {
			jb.add(KEY_OPERATOR, "ASSIGN_SHL (<<=)");
		} break;
		case ASSIGN_SHR: {
			jb.add(KEY_OPERATOR, "ASSIGN_SHR (>>=)");
		} break;
		case ASSIGN_BAND: {
			jb.add(KEY_OPERATOR, "ASSIGN_MUL (&=)");
		} break;
		case ASSIGN_BOR: {
			jb.add(KEY_OPERATOR, "ASSIGN_BOR (|=)");
		} break;
		case ASSIGN_BXOR: {
			jb.add(KEY_OPERATOR, "ASSIGN_BXOR (^=)");
		} break;
		case AND: {
			jb.add(KEY_OPERATOR, "AND (&&)");
		} break;
		case OR: {
			jb.add(KEY_OPERATOR, "OR (||)");
		} break;
		}
		// operands
		JsonArrayBuilder jaOperands = Json.createArrayBuilder();
		for(IASTExpression e: node.operands) {
			jaOperands.add((JsonObject) e.accept(this));
		}
		jb.add(KEY_OPERANDS, jaOperands);
		return jb.build();
	}
	public Object visitTernayExpression(IASTTernaryExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "TernayExpression");
		// operator
		jb.add(KEY_OPERATOR, "COND");
		// operands
		JsonArrayBuilder jaOperands = Json.createArrayBuilder();
		for(IASTExpression e: node.operands) {
			jaOperands.add((JsonObject) e.accept(this));
		}
		jb.add(KEY_OPERANDS, jaOperands);
		return jb.build();
	}
	public Object visitCallExpression(IASTCallExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "CallExpression");
		// operator
		jb.add(KEY_CALLEE, (JsonObject) node.callee.accept(this));
		// operands
		JsonArrayBuilder jaOperands = Json.createArrayBuilder();
		for(IASTExpression e: node.arguments) {
			jaOperands.add((JsonObject) e.accept(this));
		}
		jb.add(KEY_ARGUMENTS, jaOperands);
		return jb.build();
	}
	public Object visitNewExpression(IASTNewExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "NewExpression");
		// operator
		jb.add(KEY_CONSTRUCTOR, (JsonObject) node.constructor.accept(this));
		// operands
		JsonArrayBuilder jaOperands = Json.createArrayBuilder();
		for(IASTExpression e: node.arguments) {
			jaOperands.add((JsonObject) e.accept(this));
		}
		jb.add(KEY_ARGUMENTS, jaOperands);
		return jb.build();
	}
	public Object visitMemberExpression(IASTMemberExpression node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "MemberExpression");
		jb.add(KEY_OBJECT, (JsonObject) node.object.accept(this));
		jb.add(KEY_PROPERTY, (JsonObject) node.property.accept(this));
		return jb.build();
	}
	public Object visitIdentifier(IASTIdentifier node) {
		JsonObjectBuilder jb = Json.createObjectBuilder();
		jb.add(KEY_NAME, "Identifier");
		jb.add(KEY_ID, node.id);
		return jb.build();
	}
	public Object visitSequenceExpression(IASTSequenceExpression node) {
	    JsonObjectBuilder jb = Json.createObjectBuilder();
	    jb.add(KEY_NAME, "SequenceExpression");
	    JsonArrayBuilder ja = Json.createArrayBuilder();
	    for (IASTExpression e : node.expressions) {
	        ja.add((JsonObject) e.accept(this));
	    }
	    jb.add(KEY_EXPRESSIONS, ja);
	    return jb.build();
	}
}
