package estree;

public abstract class EsTreeVisitor<T> {
	
	public abstract T visitArrayExpression(ArrayExpression node);
	public abstract T visitAssignmentExpression(AssignmentExpression node);
	public abstract T visitBinaryExpression(BinaryExpression node);
	public abstract T visitBlockStatement(BlockStatement node);
	public abstract T visitBreakStatement(BreakStatement node);
	public abstract T visitCallExpression(CallExpression node);
	public abstract T visitCatchClause(CatchClause node);
	public abstract T visitConditionalExpression(ConditionalExpression node);
	public abstract T visitContinueStatement(ContinueStatement node);
	public abstract T visitDebuggerStatement(DebuggerStatement node);
	public abstract T visitDoWhileStatement(DoWhileStatement node);
	public abstract T visitEmptyStatement(EmptyStatement node);
	public abstract T visitExpressionStatement(ExpressionStatement node);
	public abstract T visitForInStatement(ForInStatement node);
	public abstract T visitForStatement(ForStatement node);
	public abstract T visitFunction(Function node);
	public abstract T visitFunctionDeclaration(FunctionDeclaration node);
	public abstract T visitFunctionExpression(FunctionExpression node);
	public abstract T visitIdentifier(Identifier node);
	public abstract T visitIfStatement(IfStatement node);
	public abstract T visitLabeledStatement(LabeledStatement node);
	public abstract T visitLiteral(Literal node);
	public abstract T visitLogicalExpression(LogicalExpression node);
	public abstract T visitMemberExpression(MemberExpression node);
	public abstract T visitNewExpression(NewExpression node);
	public abstract T visitNode(Node node);
	public abstract T visitObjectExpression(ObjectExpression node);
	public abstract T visitProgram(Program node);
	public abstract T visitProperty(Property node);
	public abstract T visitReturnStatement(ReturnStatement node);
	public abstract T visitSequenceExpression(SequenceExpression node);
	public abstract T visitSwitchCase(SwitchCase node);
	public abstract T visitSwitchStatement(SwitchStatement node);
	public abstract T visitThisExpression(ThisExpression node);
	public abstract T visitThrowStatement(ThrowStatement node);
	public abstract T visitTryStatement(TryStatement node);
	public abstract T visitUnaryExpression(UnaryExpression node);
	public abstract T visitUpdateExpression(UpdateExpression node);
	public abstract T visitVariableDeclaration(VariableDeclaration node);
	public abstract T visitVariableDeclarator(VariableDeclarator node);
	public abstract T visitWhileStatement(WhileStatement node);
	public abstract T visitWithStatement(WithStatement node);
	
	public T visit(Node node) {
		switch (node.getTypeId()) {
		case Node.IDENTIFIER:
		{
			return visitIdentifier((Identifier) node);
		}
		case Node.ARRAY_EXP:
		{
			return visitArrayExpression((ArrayExpression) node);
		}
		case Node.ASSIGNMENT_EXP:
		{
			return visitAssignmentExpression((AssignmentExpression) node);
		}
		case Node.ASSIGNMENT_OP:
		{
			return null;
		}
		case Node.BINARY_EXP:
		{
			return visitBinaryExpression((BinaryExpression) node);
		}
		case Node.BINARY_OP:
		{
			return null;
		}
		case Node.BLOCK_STMT:
		{
			return visitBlockStatement((BlockStatement) node);
		}
		case Node.BREAK_STMT:
		{
			return visitBreakStatement((BreakStatement) node);
		}
		case Node.CALL_EXP:
		{
			return visitCallExpression((CallExpression) node);
		}
		case Node.CATCH_CLAUSE:
		{
			return visitCatchClause((CatchClause) node);
		}
		case Node.CONDITIONAL_EXP:
		{
			return visitConditionalExpression((ConditionalExpression) node);
		}
		case Node.CONTINUE_STMT:
		{
			return visitContinueStatement((ContinueStatement) node);
		}
		case Node.DEBUGGER_STMT:
		{
			return null;
		}
		case Node.WHILE_STMT:
		{
		    return visitWhileStatement((WhileStatement) node);
		}
		case Node.DO_WHILE_STMT:
		{
			return visitDoWhileStatement((DoWhileStatement) node);
		}
		case Node.EMPTY_STMT:
		{
			return null;
		}
		case Node.EXP_STMT:
		{
			return visitExpressionStatement((ExpressionStatement) node);
		}
		case Node.FINALIZER:
		{
			return null;
		}
		case Node.FOR_IN_STMT:
		{
			return visitForInStatement((ForInStatement) node);
		}
		case Node.FOR_STMT:
		{
			return visitForStatement((ForStatement) node);
		}
		case Node.FUNC_DECLARATION:
		{
			return visitFunctionDeclaration((FunctionDeclaration) node);
		}
		case Node.FUNC_EXP:
		{
			return visitFunctionExpression((FunctionExpression) node);
		}
		case Node.FUNCTION:
		{
			return null;
		}
		case Node.IF_STMT:
		{
			return visitIfStatement((IfStatement) node);
		}
		case Node.LABELED_STMT:
		{
			return visitLabeledStatement((LabeledStatement) node);
		}
		case Node.LITERAL:
		{
			return visitLiteral((Literal) node);
		}
		case Node.LOGICAL_EXP:
		{
			return visitLogicalExpression((LogicalExpression) node);
		}
		case Node.LOGICAL_OP:
		case Node.MEMBER_EXP:
		{
			return visitMemberExpression((MemberExpression) node);
		}
		case Node.NEW_EXP:
		{
			return visitNewExpression((NewExpression) node);
		}
		case Node.OBJECT_EXP:
		{
			return visitObjectExpression((ObjectExpression) node);
		}
		case Node.PROGRAM:
		{
			return visitProgram((Program) node);
		}
		case Node.PROPERTY:
		{
			return visitProperty((Property) node);
		}
		case Node.RETURN_STMT:
		{
			return visitReturnStatement((ReturnStatement) node);
		}
		case Node.SEQUENCE_EXP:
		{
			return visitSequenceExpression((SequenceExpression) node);
		}
		case Node.SWITCH_STMT:
		{
			return visitSwitchStatement((SwitchStatement) node);
		}
		case Node.SWITCH_CASE:
		{
			return visitSwitchCase((SwitchCase) node);
		}
		case Node.THIS_EXP:
		{
			return visitThisExpression((ThisExpression) node);
		}
		case Node.TRY_STMT:
		{
			return visitTryStatement((TryStatement) node);
		}
		case Node.THROW_STMT:
		{
			return visitThrowStatement((ThrowStatement) node);
		}
		case Node.UNARY_EXP:
		{
			return visitUnaryExpression((UnaryExpression) node);
		}
		case Node.UNARY_OP:
		case Node.UPDATE_EXP:
		{
			return visitUpdateExpression((UpdateExpression) node);
		}
		case Node.UPDATE_OP:
		case Node.VAR_DECL_LIST:
		case Node.VAR_DECLARATION:
		{
			return visitVariableDeclaration((VariableDeclaration) node);
		}
		case Node.VAR_DECLARATOR:
		{
			return visitVariableDeclarator((VariableDeclarator) node);
		}
		case Node.VAR_STMT:
		}
		return null;
		
	}
}
