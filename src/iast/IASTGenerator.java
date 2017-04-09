package iast;
import java.util.ArrayList;
import java.util.List;

import estree.*;
import estree.Node.*;
import iast.IASTNode.*;

public class IASTGenerator extends ESTreeBaseVisitor {
	
	public IASTGenerator() {};
	
	public IASTProgram gen(Node estree) {
		return (IASTProgram) visitProgram((IProgram) estree);
	}
	
	List<String> hoistDeclarations(List<IStatement> nodes) {
		List<String> localNames = new ArrayList<String>();
		for (INode nd : nodes) {
			switch (nd.getTypeId()) {
			case Node.FUNC_DECLARATION: {
				FunctionDeclaration fn = (FunctionDeclaration) nd;
				localNames.add(fn.getId().getName());
			} break;
			case Node.VAR_DECLARATION: {
				for (IVariableDeclarator vd : ((VariableDeclaration) nd).getDeclarations()) {
					localNames.add(((IIdentifier) vd.getId()).getName());
				}
			} break;
			}
		}
		return localNames;
	}
	
	IASTBlockStatement listOfIStatement2IASTBlock(List<IStatement> src) {
		List<IASTStatement> stmts = new ArrayList<IASTStatement>();
		for (IStatement s : src) {
			stmts.add((IASTStatement) s.accept(this));
		}
		return new IASTBlockStatement(stmts);
	}

	public Object visitIdentifier(IIdentifier node) {
		return new IASTIdentifier(node.getName());
	}
	public Object visitLiteral(ILiteral node) {
		System.out.println("Literal");
		switch (node.getLiteralType()) {
		case STRING:
			return new IASTStringLiteral(node.getStringValue());
		case BOOLEAN:
			return new IASTBooleanLiteral(node.getBooleanValue());
		case NULL:
			return new IASTNullLiteral();
		case NUMBER:
			System.out.println("NumericLiteral");
			return new IASTNumericLiteral(node.getNumValue());
		case REG_EXP:
			return new IASTRegExpLiteral(node.getRegExpValue());
		}
		return null; // ERROR
	}
	public Object visitRegExpLiteral(IRegExpLiteral node) {
		return new IASTRegExpLiteral(node.getRegExpValue());
	}
	public Object visitProgram(IProgram node) {
		System.out.println("visitProgram");
		List<IASTStatement> stmts = new ArrayList<IASTStatement>();
		for (IStatement stmt : node.getBody()) {
			stmts.add((IASTStatement) stmt.accept(this));
		}
		List<String> params = new ArrayList<String>();
		List<String> locals = new ArrayList<String>();
		IASTBlockStatement block = new IASTBlockStatement(stmts);
		IASTFunctionExpression func = new IASTFunctionExpression(params, locals, block);
		return new IASTProgram(func);
	}
	/*public Object visitFunction(IFunction node) {
		node.getId().accept(this);
		for (IPattern param : node.getParams()) {
			param.accept(this);
		}
		node.getBody().accept(this);
		return visitNode(node);
	}*/
	/*public Object visitStatement(IStatement node) {
		return visitNode(node);
	}*/
	public Object visitExpressionStatement(IExpressionStatement node) {
		System.out.println("ExpressionStatement");
		return new IASTExpressionStatement((IASTExpression) node.getExpression().accept(this));
	}
	public Object visitBlockStatement(IBlockStatement node) {
		List<IASTStatement> stmts = new ArrayList<IASTStatement>();
		for (IStatement stmt : node.getBody()) {
			stmts.add((IASTStatement) stmt.accept(this));
		}
		return new IASTBlockStatement(stmts);
	}
	/*public Object visitEmptyStatement(IEmptyStatement node) {
		return visitStatement(node);
	}
	public Object visitDebuggerStatement(IDebuggerStatement node) {
		return visitStatement(node);
	}
	public Object visitWithStatement(IWithStatement node) {
		node.getBody().accept(this);
		return visitStatement(node);
	}*/
	public Object visitReturnStatement(IReturnStatement node) {
		IASTExpression arg = null;
		if (node.getArgument() != null) {
			arg = (IASTExpression) node.getArgument().accept(this);
		}
		return new IASTReturnStatement(arg);
	}
	public Object visitLabeledStatement(ILabeledStatement node) {
		IASTStatement stmt = (IASTStatement) node.getLabel().accept(this);
		String label = node.getLabel().getName();
		if (stmt instanceof IASTForStatement) {
			((IASTForStatement) stmt).label = label;
		} else if (stmt instanceof IASTForInStatement) {
			((IASTForInStatement) stmt).label = label;
		} else if (stmt instanceof IASTWhileStatement) {
			((IASTWhileStatement) stmt).label = label;
		} else if (stmt instanceof IASTDoWhileStatement) {
			((IASTDoWhileStatement) stmt).label = label;
		} else if (stmt instanceof IASTSwitchStatement) {
			((IASTSwitchStatement) stmt).label = label;
		}
		return stmt;
	}
	public Object visitBreakStatement(IBreakStatement node) {
		return new IASTBreakStatement(node.getLabel().getName());
	}
	public Object visitContinueStatement(IContinueStatement node) {
		return new IASTContinueStatement(node.getLabel().getName());
	}
	public Object visitIfStatement(IIfStatement node) {
		IASTExpression test = (IASTExpression) node.getTest().accept(this);
		IASTStatement conseq = (IASTStatement) node.getConsequent().accept(this);
		IASTStatement alter = null;
		if (node.getAlternate() != null) {
			alter = (IASTStatement) node.getAlternate().accept(this);
		}
		return new IASTIfStatement(test, conseq, alter);
	}
	public Object visitSwitchStatement(ISwitchStatement node) {
		IASTExpression discriminant = (IASTExpression) node.getDiscriminant().accept(this);
		List<IASTSwitchStatement.CaseClause> cases = new ArrayList<IASTSwitchStatement.CaseClause>();
		for (ISwitchCase c : node.getCases()) {
			List<IASTStatement> blockBody = new ArrayList<IASTStatement>();
			for (IStatement s : c.getConsequent()) {
				blockBody.add((IASTStatement) s.accept(this));
			}
			cases.add(new IASTSwitchStatement.CaseClause(
					(IASTExpression) c.getTest().accept(this),
					(IASTStatement) new IASTBlockStatement(blockBody)));
		}
		return new IASTSwitchStatement(discriminant, cases);
	}
	/*public Object visitSwitchCase(ISwitchCase node) {
		IASTExpression test = (IASTExpression) node.getTest().accept(this);
		List<IASTStatement> blockBody = new ArrayList<IASTStatement>();
		for (IStatement s : node.getConsequent()) {
			blockBody.add((IASTStatement) s.accept(this));
		}
		return new IASTSwitchStatement.CaseClause(test, (IASTStatement) new IASTBlockStatement(blockBody));
	}*/
	public Object visitThrowStatement(IThrowStatement node) {
		return new IASTThrowStatement((IASTExpression) node.getArgument().accept(this));
	}
	public Object visitTryStatement(ITryStatement node) {
		IASTStatement block = (IASTStatement) node.getBlock().accept(this);
		if (node.getHandler() != null) {
			String param = ((IIdentifier) node.getHandler().getParam()).getName();
			IASTStatement catchBody = (IASTStatement) node.getHandler().getBody().accept(this);
			IASTTryCatchStatement tryCatchStmt = new IASTTryCatchStatement(block, param, catchBody);
			if (node.getFinalizer() != null) {
				IASTStatement finallyStmt = listOfIStatement2IASTBlock(node.getFinalizer().getBody());
				return new IASTTryFinallyStatement(tryCatchStmt, finallyStmt);
			} else {
				return tryCatchStmt;
			}
		} else if (node.getFinalizer() != null) {
			IASTStatement finallyStmt = listOfIStatement2IASTBlock(node.getFinalizer().getBody());
			return new IASTTryFinallyStatement(block, finallyStmt);
		} // else ERROR
		return null;
	}
	/*public Object visitCatchClause(ICatchClause node) {
		node.getParam().accept(this);
		node.getBody().accept(this);
		return visitNode(node);
	}*/
	public Object visitWhileStatement(IWhileStatement node) {
		IASTExpression test = (IASTExpression) node.getTest().accept(this);
		IASTStatement body = (IASTStatement) node.getBody().accept(this);
		return new IASTWhileStatement(test, body);
	}
	public Object visitDoWhileStatement(IDoWhileStatement node) {
		IASTExpression test = (IASTExpression) node.getTest().accept(this);
		IASTStatement body = (IASTStatement) node.getBody().accept(this);
		return new IASTDoWhileStatement(test, body);
	}
	public Object visitForStatement(IForStatement node) {
		/*
		if (node.getExpInit() != null) {
			node.getExpInit().accept(this);
		} else if (node.getValDeclInit() != null) {
			node.getValDeclInit().accept(this);
			node.getValDeclInit().getDeclarations()
		}
		node.getTest().accept(this);
		node.getUpdate().accept(this);
		node.getBody().accept(this);*/
		return visitStatement(node);
	}
	public Object visitForInStatement(IForInStatement node) {
		/*
		if (node.getPatternLeft() == null) {
			node.getValDeclLeft().accept(this);
		} else {
			node.getPatternLeft().accept(this);
		}
		node.getRight().accept(this);
		node.getBody().accept(this);*/
		return visitStatement(node);
	}
	public Object visitDeclaration(IDeclaration node) {
		return visitStatement(node);
	}
	public Object visitFunctionDeclaration(IFunctionDeclaration node) {
		IASTIdentifier id = (IASTIdentifier) node.getId().accept(this);
		List<String> params = new ArrayList<String>();
		for (IPattern param : node.getParams()) {
			params.add(((IIdentifier) param).getName());
		}
		List<String> locals = hoistDeclarations(node.getBody().getBody());
		IASTStatement body = (IASTStatement) node.getBody().accept(this);
		IASTFunctionExpression func = new IASTFunctionExpression(params, locals, body);
		IASTBinaryExpression assign = new IASTBinaryExpression(
				IASTBinaryExpression.Operator.ASSIGN, id, func);
		return new IASTExpressionStatement(assign);
	}
	public Object visitVariableDeclaration(IVariableDeclaration node) {
		List<IASTStatement> stmts = new ArrayList<IASTStatement>();
		for (IVariableDeclarator vd : node.getDeclarations()) {
			Object r = vd.accept(this);
			if (r != null) {
				stmts.add((IASTStatement) r);
			}
		}
		return new IASTBlockStatement(stmts);
	}
	public Object visitVariableDeclarator(IVariableDeclarator node) {
		if (node.getInit() == null) {
			return null;
		}
		IASTIdentifier id = (IASTIdentifier) node.getId().accept(this);
		IASTExpression exp = (IASTExpression) node.getId().accept(this);
		IASTBinaryExpression assign = new IASTBinaryExpression(
				IASTBinaryExpression.Operator.ASSIGN, id, exp);
		return new IASTExpressionStatement(assign);
	}
	/*public Object visitExpression(IExpression node) {
		return visitNode(node);
	}*/
	public Object visitThisExpression(IThisExpression node) {
		return new IASTThisExpression();
	}
	public Object visitArrayExpression(IArrayExpression node) {
		List<IASTExpression> elements = new ArrayList<IASTExpression>();
		for (IExpression e : node.getElements()) {
			elements.add((IASTExpression) e.accept(this));
		}
		return new IASTArrayExpression(elements);
	}
	public Object visitObjectExpression(IObjectExpression node) {
	    List<IASTObjectExpression.Property> props = new ArrayList<IASTObjectExpression.Property>();
		for (IProperty p : node.getProperties()) {
			props.add((IASTObjectExpression.Property) p.accept(this));
		}
		return new IASTObjectExpression(props);
	}
	public Object visitProperty(IProperty node) {
		IASTLiteral key = null;
		if (node.getLiteralKey() != null) {
			key = (IASTLiteral) node.getLiteralKey().accept(this);
		} else if (node.getIdentifierKey() != null) {
			key = new IASTStringLiteral(node.getIdentifierKey().getName());
		} // else ERROR
		IASTExpression value = (IASTExpression) node.getValue().accept(this);
		return new IASTObjectExpression.Property(key, value, IASTObjectExpression.Property.Kind.INIT);
	}
	public Object visitFunctionExpression(IFunctionExpression node) {
		// node.getId().accept(this);
		System.out.println("FunctionExpression");
		List<String> params = new ArrayList<String>();
		for (IPattern param : node.getParams()) {
			params.add(((IIdentifier) param).getName());
		}
		List<String> locals = hoistDeclarations(node.getBody().getBody());
		IASTBlockStatement body = (IASTBlockStatement) node.getBody().accept(this);
		return new IASTFunctionExpression(params, locals, body);
	}
	public Object visitUnaryExpression(IUnaryExpression node) {
		IASTUnaryExpression.Operator operator = null;
		switch (node.getOperator()) {
		case "+": {
			operator = IASTUnaryExpression.Operator.PLUS;
		} break;
		case "-": {
			operator = IASTUnaryExpression.Operator.MINUS;
		} break;
		case "!": {
			operator = IASTUnaryExpression.Operator.NOT;
		} break;
		case "void": {
			operator = IASTUnaryExpression.Operator.VOID;
		} break;
		case "~": {
			operator = IASTUnaryExpression.Operator.REVERSE;
		} break;
		case "typeof": {
			operator = IASTUnaryExpression.Operator.TYPEOF;
		} break;
		case "delete": {
			operator = IASTUnaryExpression.Operator.DELETE;
		} break;
		}
		IASTExpression argument = (IASTExpression) node.getArgument().accept(this);
		return new IASTUnaryExpression(operator, argument, true);
	}
	public Object visitUpdateExpression(IUpdateExpression node) {
		IASTUnaryExpression.Operator operator = null;
		switch (node.getOperator()) {
		case "++": {
			operator = IASTUnaryExpression.Operator.INC;
		} break;
		case "--": {
			operator = IASTUnaryExpression.Operator.DEC;
		} break;
		}
		IASTExpression argument = (IASTExpression) node.getArgument().accept(this);
		return new IASTUnaryExpression(operator, argument, node.getPrefix());
	}
	public Object visitBinaryExpression(IBinaryExpression node) {
		System.out.println("BinaryExpression");
		IASTBinaryExpression.Operator operator = null;
		switch (node.getOperator()) {
		case EQ_EQ: {
			operator = IASTBinaryExpression.Operator.EQUAL;
		} break;
		case NOT_EQ: {
			operator = IASTBinaryExpression.Operator.NOT_EQUAL;
		} break;
		case EQ_EQ_EQ: {
			operator = IASTBinaryExpression.Operator.EQ;
		} break;
		case NOT_EQ_EQ: {
			operator = IASTBinaryExpression.Operator.NOT_EQ;
		} break;
		case LT: {
			operator = IASTBinaryExpression.Operator.LT;
		} break;
		case LE: {
			operator = IASTBinaryExpression.Operator.LTE;
		} break;
		case GT: {
			operator = IASTBinaryExpression.Operator.GT;
		} break;
		case GE: {
			operator = IASTBinaryExpression.Operator.GTE;
		} break;
		case SLL: {
			operator = IASTBinaryExpression.Operator.SHL;
		} break;
		case SRL: {
			operator = IASTBinaryExpression.Operator.SHR;
		} break;
		case SRA: {
			operator = IASTBinaryExpression.Operator.UNSIGNED_SHR;
		} break;
		case ADD: {
			operator = IASTBinaryExpression.Operator.ADD;
		} break;
		case SUB: {
			operator = IASTBinaryExpression.Operator.SUB;
		} break;
		case MUL: {
			operator = IASTBinaryExpression.Operator.MUL;
		} break;
		case DIV: {
			operator = IASTBinaryExpression.Operator.DIV;
		} break;
		case MOD: {
			operator = IASTBinaryExpression.Operator.MOD;
		} break;
		case BIT_OR: {
			operator = IASTBinaryExpression.Operator.BOR;
		} break;
		case BIT_AND: {
			operator = IASTBinaryExpression.Operator.BAND;
		} break;
		case BIT_XOR: {
			operator = IASTBinaryExpression.Operator.BXOR;
		} break;
		case IN: {
			operator = IASTBinaryExpression.Operator.IN;
		} break;
		case INSTANCEOF: {
			operator = IASTBinaryExpression.Operator.INSTANCE_OF;
		} break;
		}
		IASTExpression left = (IASTExpression) node.getLeft().accept(this);
		IASTExpression right = (IASTExpression) node.getRight().accept(this);
		return new IASTBinaryExpression(operator, left, right);
	}
	public Object visitAssignmentExpression(IAssignmentExpression node) {
		IASTExpression left = null;
		if (node.getExpressionLeft() == null) {
			left = new IASTStringLiteral(((IIdentifier) node.getPatternLeft()).getName());
		} else {
			left = (IASTExpression) node.getExpressionLeft().accept(this);
		}
		IASTExpression right = (IASTExpression) node.getRight().accept(this);
		IASTBinaryExpression.Operator operator = null;
		switch (node.getOperator()) {
		case EQ_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN;
		} break;
		case ADD_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_ADD;
		} break;
		case SUB_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_SUB;
		} break;
		case MUL_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_MUL;
		} break;
		case DIV_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_DIV;
		} break;
		case PER_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_MOD;
		} break;
		case LT_LT_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_SHL;
		} break;
		case GT_GT_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_SHR;
		} break;
		case GT_GT_GT_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_UNSIGNED_SHR;
		} break;
		case OR_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_BOR;
		} break;
		case EXOR_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_BXOR;
		} break;
		case AND_EQ: {
			operator = IASTBinaryExpression.Operator.ASSIGN_BAND;
		} break;
		}
		return new IASTBinaryExpression(operator, left, right);
	}
	public Object visitLogicalExpression(ILogicalExpression node) {
		IASTBinaryExpression.Operator operator = null;
		switch (node.getOperator()) {
		case OR: {
			operator = IASTBinaryExpression.Operator.OR;
		} break;
		case AND: {
			operator = IASTBinaryExpression.Operator.AND;
		} break;
		}
		IASTExpression left = (IASTExpression) node.getLeft().accept(this);
		IASTExpression right = (IASTExpression) node.getRight().accept(this);
		return new IASTBinaryExpression(operator, left, right);
	}
	public Object visitMemberExpression(IMemberExpression node) {
		node.getObject().accept(this);
		node.getProperty().accept(this);
		return visitExpression(node);
	}
	public Object visitConditionalExpression(IConditionalExpression node) {
		node.getTest().accept(this);
		node.getConsequent().accept(this);
		node.getAlternate().accept(this);
		return visitExpression(node);
	}
	public Object visitCallExpression(ICallExpression node) {
		node.getCallee().accept(this);
		for (IExpression e : node.getArguments()) {
			e.accept(this);
		}
		return visitExpression(node);
	}
	public Object visitNewExpression(INewExpression node) {
		return visitCallExpression(node);
	}
	public Object visitSequenceExpression(ISequenceExpression node) {
		for (IExpression e : node.getExpression()) {
			e.accept(this);
		}
		return visitExpression(node);
	}
	public Object visitPattern(IPattern node) {
		return visitNode(node);
	}
}
