package estree;

import estree.Node.*;

public class ESTreeBaseVisitor {

	public Object visitNode(INode node) {
		return null;
	}
	public Object visitIdentifier(IIdentifier node) {
		return visitExpression(node);
	}
	public Object visitLiteral(ILiteral node) {
		return visitExpression(node);
	}
	public Object visitRegExpLiteral(IRegExpLiteral node) {
		return visitLiteral(node);
	}
	public Object visitProgram(IProgram node) {
		for (IStatement stmt : node.getBody()) {
			stmt.accept(this);
		}
		return visitNode(node);
	}
	public Object visitFunction(IFunction node) {
		node.getId().accept(this);
		for (IPattern param : node.getParams()) {
			param.accept(this);
		}
		node.getBody().accept(this);
		return visitNode(node);
	}
	public Object visitStatement(IStatement node) {
		return visitNode(node);
	}
	public Object visitExpressionStatement(IExpressionStatement node) {
		node.getExpression().accept(this);
		return visitStatement(node);
	}
	public Object visitBlockStatement(IBlockStatement node) {
		for (IStatement stmt : node.getBody()) {
			stmt.accept(this);
		}
		return visitStatement(node);
	}
	public Object visitEmptyStatement(IEmptyStatement node) {
		return visitStatement(node);
	}
	public Object visitDebuggerStatement(IDebuggerStatement node) {
		return visitStatement(node);
	}
	public Object visitWithStatement(IWithStatement node) {
		node.getBody().accept(this);
		return visitStatement(node);
	}
	public Object visitReturnStatement(IReturnStatement node) {
		if (node.getArgument() != null) {
			node.getArgument().accept(this);
		}
		return visitStatement(node);
	}
	public Object visitLabeledStatement(ILabeledStatement node) {
		node.getLabel().accept(this);
		return visitStatement(node);
	}
	public Object visitBreakStatement(IBreakStatement node) {
		node.getLabel().accept(this);
		return visitStatement(node);
	}
	public Object visitContinueStatement(IContinueStatement node) {
		node.getLabel().accept(this);
		return visitStatement(node);
	}
	public Object visitIfStatement(IIfStatement node) {
		node.getTest().accept(this);
		node.getConsequent().accept(this);
		if (node.getAlternate() != null) {
			node.getAlternate().accept(this);
		}
		return visitStatement(node);
	}
	public Object visitSwitchStatement(ISwitchStatement node) {
		node.getDiscriminant().accept(this);
		for (ISwitchCase c : node.getCases()) {
			c.accept(this);
		}
		return visitStatement(node);
	}
	public Object visitSwitchCase(ISwitchCase node) {
		node.getTest().accept(this);
		for (IStatement s : node.getConsequent()) {
			s.accept(this);
		}
		return visitNode(node);
	}
	public Object visitThrowStatement(IThrowStatement node) {
		node.getArgument().accept(this);
		return visitStatement(node);
	}
	public Object visitTryStatement(ITryStatement node) {
		node.getBlock().accept(this);
		if (node.getHandler() != null) {
			node.getHandler().accept(this);
		}
		if (node.getFinalizer() != null) {
			node.getFinalizer().accept(this);
		}
		return visitStatement(node);
	}
	public Object visitCatchClause(ICatchClause node) {
		node.getParam().accept(this);
		node.getBody().accept(this);
		return visitNode(node);
	}
	public Object visitWhileStatement(IWhileStatement node) {
		node.getTest().accept(this);
		node.getBody().accept(this);
		return visitStatement(node);
	}
	public Object visitDoWhileStatement(IDoWhileStatement node) {
		node.getTest().accept(this);
		node.getBody().accept(this);
		return visitStatement(node);
	}
	public Object visitForStatement(IForStatement node) {
		if (node.getValDeclInit() == null) {
			node.getExpInit().accept(this);
		} else {
			node.getValDeclInit().accept(this);
		}
		node.getTest().accept(this);
		node.getUpdate().accept(this);
		node.getBody().accept(this);
		return visitStatement(node);
	}
	public Object visitForInStatement(IForInStatement node) {
		if (node.getPatternLeft() == null) {
			node.getValDeclLeft().accept(this);
		} else {
			node.getPatternLeft().accept(this);
		}
		node.getRight().accept(this);
		node.getBody().accept(this);
		return visitStatement(node);
	}
	public Object visitDeclaration(IDeclaration node) {
		return visitStatement(node);
	}
	public Object visitFunctionDeclaration(IFunctionDeclaration node) {
		node.getId().accept(this);
		for (IPattern param : node.getParams()) {
			param.accept(this);
		}
		node.getBody().accept(this);
		return visitDeclaration(node);
	}
	public Object visitVariableDeclaration(IVariableDeclaration node) {
		for (IVariableDeclarator vd : node.getDeclarations()) {
			vd.accept(this);
		}
		return visitDeclaration(node);
	}
	public Object visitVariableDeclarator(IVariableDeclarator node) {
		node.getId().accept(this);
		if (node.getInit() != null) {
			node.getInit().accept(this);
		}
		return visitNode(node);
	}
	public Object visitExpression(IExpression node) {
		return visitNode(node);
	}
	public Object visitThisExpression(IThisExpression node) {
		return visitExpression(node);
	}
	public Object visitArrayExpression(IArrayExpression node) {
		for (IExpression e : node.getElements()) {
			e.accept(this);
		}
		return visitExpression(node);
	}
	public Object visitObjectExpression(IObjectExpression node) {
		for (IProperty p : node.getProperties()) {
			p.accept(this);
		}
		return visitExpression(node);
	}
	public Object visitProperty(IProperty node) {
		if (node.getIdentifierKey() == null) {
			node.getLiteralKey().accept(this);
		} else {
			node.getIdentifierKey().accept(this);
		}
		node.getValue().accept(this);
		return visitNode(node);
	}
	public Object visitFunctionExpression(IFunctionExpression node) {
		node.getId().accept(this);
		for (IPattern param : node.getParams()) {
			param.accept(this);
		}
		node.getBody().accept(this);
		return visitExpression(node);
	}
	public Object visitUnaryExpression(IUnaryExpression node) {
		node.getArgument().accept(this);
		return visitExpression(node);
	}
	public Object visitUpdateExpression(IUpdateExpression node) {
		node.getArgument().accept(this);
		return visitExpression(node);
	}
	public Object visitBinaryExpression(IBinaryExpression node) {
		node.getLeft().accept(this);
		node.getRight().accept(this);
		return visitExpression(node);
	}
	public Object visitAssignmentExpression(IAssignmentExpression node) {
		if (node.getExpressionLeft() == null) {
			node.getPatternLeft().accept(this);
		} else {
			node.getExpressionLeft().accept(this);
		}
		node.getRight().accept(this);
		return visitExpression(node);
	}
	public Object visitLogicalExpression(ILogicalExpression node) {
		node.getLeft().accept(this);
		node.getRight().accept(this);
		return visitExpression(node);
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
