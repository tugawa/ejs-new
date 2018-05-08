/*
   ASTGenerator.java

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
import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import ejsc.antlr.*;
import ejsc.antlr.ECMAScriptParser.*;
import ejsc.ast_node.*;
import ejsc.ast_node.Node.*;


public class ASTGenerator extends ECMAScriptBaseVisitor<Node> {

	@Override
	public Node visitProgram(ECMAScriptParser.ProgramContext ctx) {
		List<IStatement> decls = new ArrayList<IStatement>();
		for (SourceElementContext el : ctx.sourceElements().sourceElement()) {
			IStatement decl = (IStatement) visit(el);
			decls.add(decl);
		}
		return new Program(decls);
	}

	@Override
	public Node visitSourceElements(ECMAScriptParser.SourceElementsContext ctx) {
		return visitChildren(ctx);
	}

	/*
	// old
	@Override
	public Node visitProgram(ECMAScriptParser.ProgramContext ctx) {
		Node sourceElements = visit(ctx.sourceElements());
		return new Program(sourceElements);
	}

	@Override
	public Node visitSourceElements(ECMAScriptParser.SourceElementsContext ctx) {
		List<Node> sourceElementList = new ArrayList<Node>();
		for (SourceElementContext el : ctx.sourceElement()) {
			Node sourceElement = visit(el);
			sourceElementList.add(sourceElement);
		}
		return new SourceElements(sourceElementList);
	}
	*/
	/*
	@Override
	public Node visitSourceElement(ECMAScriptParser.SourceElementContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Node visitStatement(ECMAScriptParser.StatementContext ctx) {
		return visitChildren(ctx);
	}*/

	@Override
	public BlockStatement visitBlock(ECMAScriptParser.BlockContext ctx) {
		List<IStatement> stmts = new ArrayList<IStatement>();
		if (ctx.statementList() != null) {
			for (StatementContext stmtCtx : ctx.statementList().statement()) {
				stmts.add((IStatement) visit(stmtCtx));
			}
		}
		return new BlockStatement(stmts);
	}
	/*
	@Override
	public Node visitStatementList(ECMAScriptParser.StatementListContext ctx) {
		return visitChildren(ctx);
	}
	*/
	@Override
	public Node visitVariableStatement(ECMAScriptParser.VariableStatementContext ctx) {
		Node varDecl = visit(ctx.variableDeclarationList());
		return varDecl;
	}

	@Override
	public Node visitVariableDeclarationList(ECMAScriptParser.VariableDeclarationListContext ctx) {
		List<IVariableDeclarator> varDeclList = new ArrayList<IVariableDeclarator>();
		for (VariableDeclarationContext c : ctx.variableDeclaration()) {
			IVariableDeclarator varDecl = (IVariableDeclarator) visit(c);
			varDeclList.add(varDecl);
		}
		VariableDeclaration node =  new VariableDeclaration(varDeclList);
		node.setSourceLocation(ctx.getText(), ctx.start, ctx.stop);
		return node;
	}

	@Override
	public Node visitVariableDeclaration(ECMAScriptParser.VariableDeclarationContext ctx) {
		String name = ctx.Identifier().getText();
		IExpression initaliser;
		ECMAScriptParser.InitialiserContext initctx = ctx.initialiser();
		if (initctx == null) {
			initaliser = null;
		} else {
			initaliser = (IExpression) visit(ctx.initialiser());
		}
		Identifier id = new Identifier(name);
		id.setSourceLocation(name, ctx.Identifier().getSymbol(), ctx.Identifier().getSymbol());
		VariableDeclarator node =
				new VariableDeclarator(id, initaliser);
		node.setSourceLocation(ctx.getText(), ctx.start, ctx.stop);
		return node;
	}

	@Override
	public Node visitInitialiser(ECMAScriptParser.InitialiserContext ctx) {
		// System.out.println("" + ctx.singleExpression().getChildCount());
		Node node = visit(ctx.singleExpression());
		if (node == null) {
			node = new Identifier(ctx.singleExpression().getText());
		}
		return node;
	}

	@Override
	public Node visitEmptyStatement(ECMAScriptParser.EmptyStatementContext ctx) {
		return new EmptyStatement();
	}

	@Override
	public Node visitExpressionStatement(ECMAScriptParser.ExpressionStatementContext ctx) {
		IExpression expression = (IExpression) visit(ctx.expressionSequence());
		return new ExpressionStatement(expression);
	}

	@Override
	public Node visitIfStatement(ECMAScriptParser.IfStatementContext ctx) {
		IExpression test = (IExpression) visit(ctx.expressionSequence());
		IStatement consequent = (IStatement) visit(ctx.statement(0));
		IStatement alternate;
		if (ctx.Else() == null) {
			alternate = null;
		} else {
			alternate = (IStatement) visit(ctx.statement(1));
		}
		IfStatement ifstmt = new IfStatement(test, consequent, alternate);
		ifstmt.setSourceLocation(ctx.getText(), ctx.start, ctx.stop);
		return ifstmt;
	}

	@Override
	public Node visitDoStatement(ECMAScriptParser.DoStatementContext ctx) {
		IStatement body = (IStatement) visit(ctx.statement());
		IExpression test = (IExpression) visit(ctx.expressionSequence());
		return new DoWhileStatement(body, test);
	}

	@Override
	public Node visitWhileStatement(ECMAScriptParser.WhileStatementContext ctx) {
		IStatement stmt = (IStatement) visit(ctx.statement());
		WhileStatement whileStatement = new WhileStatement(
				(IExpression) visit(ctx.expressionSequence()),
				stmt);
		return whileStatement;
	}

	@Override
	public Node visitForStatement(ECMAScriptParser.ForStatementContext ctx) {
    /*
		IExpression exp = null;
		IExpression test = null;
		IExpression update = null;
		if (ctx.expressionSequence(0) != null) {
			exp = (IExpression) visit(ctx.expressionSequence(0));
		}
		if (ctx.expressionSequence(1) != null) {
			test = (IExpression) visit(ctx.expressionSequence(1));
		}
		if (ctx.expressionSequence(2) != null) {
			update = (IExpression) visit(ctx.expressionSequence(2));
		}
		IStatement body = (IStatement) visit(ctx.statement());
		return new ForStatement(exp, (IExpression) test, update, body);
    */
    IExpression exp = null;
		IExpression test = null;
		IExpression update = null;
		if (ctx.expressionSequenceOpt(0).expressionSequence() != null) {
			exp = (IExpression) visit(ctx.expressionSequenceOpt(0).expressionSequence());
		}
		if (ctx.expressionSequenceOpt(1).expressionSequence() != null) {
			test = (IExpression) visit(ctx.expressionSequenceOpt(1).expressionSequence());
		}
		if (ctx.expressionSequenceOpt(2).expressionSequence() != null) {
			update = (IExpression) visit(ctx.expressionSequenceOpt(2).expressionSequence());
		}
		IStatement body = (IStatement) visit(ctx.statement());

    return new ForStatement(exp, (IExpression) test, update, body);
	}

	@Override
	public Node visitForVarStatement(ECMAScriptParser.ForVarStatementContext ctx) {
		VariableDeclaration varDecl = (VariableDeclaration) visit(ctx.variableDeclarationList());
		IExpression test = null;
		IExpression update = null;
		if (ctx.expressionSequenceOpt(0).expressionSequence() != null) {
			test = (IExpression) visit(ctx.expressionSequenceOpt(0).expressionSequence());
		}
		if (ctx.expressionSequenceOpt(1).expressionSequence() != null) {
			update = (IExpression) visit(ctx.expressionSequenceOpt(1).expressionSequence());
		}
		IStatement body = (IStatement) visit(ctx.statement());
		return new ForStatement(varDecl, test, update, body);
	}

	@Override
	public Node visitForInStatement(ECMAScriptParser.ForInStatementContext ctx) {
		IPattern left = (IPattern) visit(ctx.singleExpression());
		IExpression right = (IExpression) visit(ctx.expressionSequence());
		IStatement body = (IStatement) visit(ctx.statement());
		return new ForInStatement(left, right, body);
	}

	@Override
	public Node visitForVarInStatement(ECMAScriptParser.ForVarInStatementContext ctx) {
		List<IVariableDeclarator> decls = new ArrayList<IVariableDeclarator>();
		decls.add((IVariableDeclarator) visit(ctx.variableDeclaration()));
		IVariableDeclaration left = new VariableDeclaration(decls);
		IExpression right = (IExpression) visit(ctx.expressionSequence());
		IStatement body = (IStatement) visit(ctx.statement());
		return new ForInStatement(left, right, body);
	}

	@Override
	public Node visitContinueStatement(ECMAScriptParser.ContinueStatementContext ctx) {
		Identifier id = null;
		if (ctx.Identifier() != null) {
			id = new Identifier(ctx.Identifier().getText());
		}
		return new ContinueStatement(id);
	}

	@Override public Node visitBreakStatement(ECMAScriptParser.BreakStatementContext ctx) {
		Identifier id = null;
		if (ctx.Identifier() != null) {
			id = new Identifier(ctx.Identifier().getText());
		}
		return new BreakStatement(id);
	}

	@Override public Node visitReturnStatement(ECMAScriptParser.ReturnStatementContext ctx) {
		IExpression argument = null;
		if (ctx.expressionSequence() != null) {
			argument = (IExpression) visit(ctx.expressionSequence());
		}
		return new ReturnStatement(argument);
	}

	@Override public Node visitWithStatement(ECMAScriptParser.WithStatementContext ctx) {
		IExpression object = (IExpression) visit(ctx.expressionSequence());
		IStatement body = (IStatement) visit(ctx.statement());
		return new WithStatement(object, body);
	}

	@Override public Node visitSwitchStatement(ECMAScriptParser.SwitchStatementContext ctx) {
	    IExpression discriminant = (IExpression) visit(ctx.expressionSequence());
	    List<ISwitchCase> switchCases = new ArrayList<ISwitchCase>();
	    if (ctx.caseBlock().caseClauses(0) != null) {
	        for (CaseClauseContext switchList : ctx.caseBlock().caseClauses(0).caseClause()) {
	            switchCases.add((SwitchCase) visit(switchList));
	        }
	    }
	    if (ctx.caseBlock().defaultClause() != null) {
	        switchCases.add((SwitchCase) visit(ctx.caseBlock().defaultClause()));
	    }
	    if (ctx.caseBlock().caseClauses(1) != null) {
	        for (CaseClauseContext switchList : ctx.caseBlock().caseClauses(1).caseClause()) {
	            switchCases.add((SwitchCase) visit(switchList));
	        }
	    }
	    return new SwitchStatement(discriminant, switchCases);
		/*IExpression discriminant = (IExpression) visit(ctx.expressionSequence());
		SwitchStatement switchStmt = (SwitchStatement) visit(ctx.caseBlock());
		switchStmt.setDiscriminant(discriminant);
		return switchStmt;*/
	}

	/*
	@Override public Node visitCaseBlock(ECMAScriptParser.CaseBlockContext ctx) {
		SwitchStatement switchStmt = (SwitchStatement) visit(ctx.caseClauses(0));
		if (ctx.defaultClause() != null) {
			switchStmt.setCase((SwitchCase) visit(ctx.defaultClause()));
			if (ctx.caseClauses(1) != null) {
				SwitchStatement switchStmt2 = (SwitchStatement) visit(ctx.caseClauses(1));
				for (ISwitchCase sc : switchStmt2.getCases()) {
					switchStmt.setCase(sc);
				}
			}
		}
		return switchStmt;
	}*/

	/*
	@Override public Node visitCaseClauses(ECMAScriptParser.CaseClausesContext ctx) {
		List<ISwitchCase> switchCases = new ArrayList<ISwitchCase>();
		for (CaseClauseContext caseClauseCtx : ctx.caseClause()) {
			switchCases.add((SwitchCase) visit(caseClauseCtx));
		}
		return new SwitchStatement(switchCases);
	}*/

	@Override public Node visitCaseClause(ECMAScriptParser.CaseClauseContext ctx) {
		IExpression test = (IExpression) visit(ctx.expressionSequence());
		List<IStatement> consequent = new ArrayList<IStatement>();
		if (ctx.statementList() != null) {
		    for (StatementContext stmtCtx : ctx.statementList().statement()) {
	            consequent.add((IStatement) visit(stmtCtx));
	        }
		}
		return new SwitchCase(test, consequent);
	}

	@Override public Node visitDefaultClause(ECMAScriptParser.DefaultClauseContext ctx) {
		List<IStatement> consequent = new ArrayList<IStatement>();
		for (StatementContext stmtCtx : ctx.statementList().statement()) {
			consequent.add((IStatement) visit(stmtCtx));
		}
		return new SwitchCase(null, consequent);
	}

	@Override public Node visitLabelledStatement(ECMAScriptParser.LabelledStatementContext ctx) {
		Identifier label = new Identifier(ctx.Identifier().getText());
		IStatement statement = (IStatement) visit(ctx.statement());
		if (statement.getTypeId() == Node.WHILE_STMT) {
			((WhileStatement) statement).setLabel(label.getName());
		} else if (statement.getTypeId() == Node.DO_WHILE_STMT) {
			((DoWhileStatement) statement).setLabel(label.getName());
		} else if (statement.getTypeId() == Node.FOR_STMT) {
			((ForStatement) statement).setLabel(label.getName());
    } else if (statement.getTypeId() == Node.FOR_IN_STMT) {
			((ForInStatement) statement).setLabel(label.getName());
		}
		return new LabeledStatement(label, statement);
	}

	@Override public Node visitThrowStatement(ECMAScriptParser.ThrowStatementContext ctx) {
		IExpression argument = (IExpression) visit(ctx.expressionSequence());
		return new ThrowStatement(argument);
	}

	@Override public Node visitTryStatement(ECMAScriptParser.TryStatementContext ctx) {
		BlockStatement block = (BlockStatement) visit(ctx.block());
		CatchClause handler = null;
		if (ctx.catchProduction() != null) {
			handler = (CatchClause) visit(ctx.catchProduction());
		}
		BlockStatement finalizer = null;
		if (ctx.finallyProduction() != null) {
			finalizer = (BlockStatement) visit(ctx.finallyProduction());
		}
		return new TryStatement(block, handler, finalizer);
	}

	@Override public Node visitCatchProduction(ECMAScriptParser.CatchProductionContext ctx) {
		Identifier id = new Identifier(ctx.Identifier().getText());
		BlockStatement body = (BlockStatement) visit(ctx.block());
		return new CatchClause(id, body);
	}

	@Override public Node visitFinallyProduction(ECMAScriptParser.FinallyProductionContext ctx) {
		return visit(ctx.block());
	}

	@Override public Node visitDebuggerStatement(ECMAScriptParser.DebuggerStatementContext ctx) {
		return new DebuggerStatement();
	}

	@Override public Node visitFunctionDeclaration(ECMAScriptParser.FunctionDeclarationContext ctx) {
		Identifier id = new Identifier(ctx.Identifier().getText());
		List<IPattern> params = new ArrayList<IPattern>();
		FormalParameterListContext fplist = ctx.formalParameterList();
		if (fplist != null) {
			for (TerminalNode t : fplist.Identifier()) {
				params.add(new Identifier(t.getText()));
			}
		}
		BlockStatement body = (BlockStatement) visit(ctx.functionBody());
		return new FunctionDeclaration(id, params, body);
	}

	@Override public Node visitFormalParameterList(ECMAScriptParser.FormalParameterListContext ctx) {
		System.out.println("FormalParameterList");
		return visitChildren(ctx); }

	@Override public Node visitFunctionBody(ECMAScriptParser.FunctionBodyContext ctx) {
		List<IStatement> decls = new ArrayList<IStatement>();
		if (ctx.sourceElements() != null) {
			for (SourceElementContext el : ctx.sourceElements().sourceElement()) {
				IStatement decl = (IStatement) visit(el);
				decls.add(decl);
			}
		}
		return new BlockStatement(decls);
	}

	@Override public Node visitArrayLiteral(ECMAScriptParser.ArrayLiteralContext ctx) {
		List<IExpression> elements = new ArrayList<IExpression>();
		if (ctx.elementList() != null) {
			List<SingleExpressionContext> singleExprCtxList = ctx.elementList().singleExpression();

			for (SingleExpressionContext singleExprCtx : singleExprCtxList) {
				elements.add((IExpression) visit(singleExprCtx));
			}
		}
		return new ArrayExpression(elements);
	}

	@Override public Node visitElementList(ECMAScriptParser.ElementListContext ctx) { return visitChildren(ctx); }

	@Override public Node visitElision(ECMAScriptParser.ElisionContext ctx) { return visitChildren(ctx); }
	// @Override public Node visitElision_opt(ECMAScriptParser.Elision_optContext ctx) { return visitChildren(ctx); }

	@Override public Node visitObjectLiteral(ECMAScriptParser.ObjectLiteralContext ctx) {
		// System.out.println("ObjectLiteral");
		List<IProperty> props = new ArrayList<IProperty>();
		if (ctx.propertyNameAndValueList() != null) {
			List<PropertyAssignmentContext> propAssignCtxList = ctx.propertyNameAndValueList().propertyAssignment();
			for (PropertyAssignmentContext propAssignCtx : propAssignCtxList) {
				props.add((Property) visit(propAssignCtx));
			}
		}
		return new ObjectExpression(props);
	}

	@Override public Node visitPropertyNameAndValueList(ECMAScriptParser.PropertyNameAndValueListContext ctx) { return visitChildren(ctx); }

	@Override public Node visitPropertyExpressionAssignment(ECMAScriptParser.PropertyExpressionAssignmentContext ctx) {
		// System.out.println(ctx.propertyName().getText());
		Node node = visit(ctx.propertyName());
		IExpression exp = (IExpression) visit(ctx.singleExpression());
		Property prop = null;
		if (node == null) {
			prop = new Property(new Literal(ctx.propertyName().getText()), exp, "init");
		} else if (node.getTypeId() == Node.IDENTIFIER) {
			prop = new Property((Identifier) node, exp, "init");
		} else if (node.getTypeId() == Node.LITERAL) {
			prop = new Property((Literal) node, exp, "init");
		} else {
			System.out.println("else");
		}
		return prop;
	}

	@Override public Node visitPropertyGetter(ECMAScriptParser.PropertyGetterContext ctx) {
		IBlockStatement block = (IBlockStatement) visit(ctx.functionBody());
		List<IPattern> params = new ArrayList<IPattern>();
		IFunctionExpression fun = new FunctionExpression(null, params, block);
		Node node = visit(ctx.getter().propertyName());
		Property prop = null;
		if (node == null) {
			prop = new Property(new Literal(ctx.getter().propertyName().getText()), fun, "get");
		} else if (node.getTypeId() == Node.IDENTIFIER) {
			prop = new Property((Identifier) node, fun, "get");
		} else if (node.getTypeId() == Node.LITERAL) {
			prop = new Property((Literal) node, fun, "get");
		} else {
			System.out.println("else");
		}
		return prop;
	}

	@Override public Node visitPropertySetter(ECMAScriptParser.PropertySetterContext ctx) {
		IBlockStatement block = (IBlockStatement) visit(ctx.functionBody());
		List<IPattern> params = new ArrayList<IPattern>();
		String paramName = ctx.propertySetParameterList().Identifier().getText();
		params.add(new Identifier(paramName));
		IFunctionExpression fun = new FunctionExpression(null, params, block);
		Node node = visit(ctx.setter().propertyName());
		Property prop = null;
		if (node == null) {
			prop = new Property(new Literal(ctx.setter().propertyName().getText()), fun, "set");
		} else if (node.getTypeId() == Node.IDENTIFIER) {
			prop = new Property((Identifier) node, fun, "set");
		} else if (node.getTypeId() == Node.LITERAL) {
			prop = new Property((Literal) node, fun, "set");
		} else {
			System.out.println("else");
		}
		return prop;
	}

	@Override public Node visitPropertyName(ECMAScriptParser.PropertyNameContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Node visitPropertySetParameterList(ECMAScriptParser.PropertySetParameterListContext ctx) { return visitChildren(ctx); }

	@Override public Node visitArguments(ECMAScriptParser.ArgumentsContext ctx) {
		System.out.println("Arguments");
		return visitChildren(ctx); }

	@Override public Node visitArgumentList(ECMAScriptParser.ArgumentListContext ctx) {
		System.out.println("ArgumentList");
		return visitChildren(ctx); }

	@Override public Node visitExpressionSequence(ECMAScriptParser.ExpressionSequenceContext ctx) { return visitChildren(ctx); }

	@Override public Node visitTernaryExpression(ECMAScriptParser.TernaryExpressionContext ctx) {
		IExpression test = (IExpression) visit(ctx.singleExpression(0));
		IExpression alternate = (IExpression) visit(ctx.singleExpression(1));
		IExpression consequent = (IExpression) visit(ctx.singleExpression(2));
		return new ConditionalExpression(test, alternate, consequent);
	}

	@Override public Node visitBitOrExpression(ECMAScriptParser.BitOrExpressionContext ctx) {
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression("|", left, right);
	}

	@Override public Node visitAssignmentExpression(ECMAScriptParser.AssignmentExpressionContext ctx) {
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new AssignmentExpression("=", left, right);
	}

	@Override public Node visitLogicalAndExpression(ECMAScriptParser.LogicalAndExpressionContext ctx) {
		IExpression lhs = (IExpression) visit(ctx.singleExpression(0));
		IExpression rhs = (IExpression) visit(ctx.singleExpression(1));
		return new LogicalExpression(ctx.getChild(1).getText(), lhs, rhs);
	}

	@Override public Node visitInstanceofExpression(ECMAScriptParser.InstanceofExpressionContext ctx) {
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.Instanceof().getText(), left, right);
	}

	@Override public Node visitObjectLiteralExpression(ECMAScriptParser.ObjectLiteralExpressionContext ctx) { return visitChildren(ctx); }

	@Override public Node visitPreDecreaseExpression(ECMAScriptParser.PreDecreaseExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UpdateExpression("--", true, argument);
	}

	@Override public Node visitArrayLiteralExpression(ECMAScriptParser.ArrayLiteralExpressionContext ctx) {
		return visitChildren(ctx); }

	@Override public Node visitInExpression(ECMAScriptParser.InExpressionContext ctx) {
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.In().getText(), left, right);
	}

	@Override public Node visitArgumentsExpression(ECMAScriptParser.ArgumentsExpressionContext ctx) {
		IExpression callee = (IExpression) visit(ctx.singleExpression());
		List<IExpression> arguments = new ArrayList<IExpression>();
		ArgumentListContext argumentsContext = ctx.arguments().argumentList();
		if (argumentsContext != null) {
			for (SingleExpressionContext sectx : argumentsContext.singleExpression()) {
				arguments.add((IExpression) visit(sectx));
			}
		}
		return new CallExpression(callee, arguments);
	}

	@Override public Node visitMemberDotExpression(ECMAScriptParser.MemberDotExpressionContext ctx) {
		IExpression object = (IExpression) visit(ctx.singleExpression());
		IExpression property = (IExpression) visit(ctx.identifierName());
		return new MemberExpression(object, property, false);
	}

	@Override public Node visitNotExpression(ECMAScriptParser.NotExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UnaryExpression("!", true, argument);
	}

	@Override public Node visitDeleteExpression(ECMAScriptParser.DeleteExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UnaryExpression(ctx.Delete().getText(), true, argument);
	}

	@Override public Node visitIdentifierExpression(ECMAScriptParser.IdentifierExpressionContext ctx) {
		// System.out.println("IdentifierExpr");
		Node node;
		String name = ctx.Identifier().getText();
		if (name.equals("null")) {
			node = new Literal(name);
		} else {
			node = new Identifier(name);
		}
		// node.setSourceLocation(name, ctx.Identifier().getSymbol(), ctx.Identifier().getSymbol());
		return node;
	}

	@Override public Node visitBitAndExpression(ECMAScriptParser.BitAndExpressionContext ctx) {
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression("&", left, right);
	}

	@Override public Node visitUnaryMinusExpression(ECMAScriptParser.UnaryMinusExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UnaryExpression("-", true, argument);
	}

	@Override public Node visitPreIncrementExpression(ECMAScriptParser.PreIncrementExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UpdateExpression("++", true, argument);
	}

	@Override public Node visitFunctionExpression(ECMAScriptParser.FunctionExpressionContext ctx) {
		List<IPattern> params = new ArrayList<IPattern>();
		Identifier id = ctx.Identifier() != null ? new Identifier(ctx.Identifier().getText()) : null;
		FormalParameterListContext fplist = ctx.formalParameterList();
		if (fplist != null) {
			for (TerminalNode t : fplist.Identifier()) {
				params.add(new Identifier(t.getText()));
			}
		}
		BlockStatement body = (BlockStatement) visit(ctx.functionBody());
		return new FunctionExpression(id, params, body);
	}

	@Override public Node visitBitShiftExpression(ECMAScriptParser.BitShiftExpressionContext ctx) {
		IExpression lhs = (IExpression) visit(ctx.singleExpression(0));
		IExpression rhs = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.getChild(1).getText(), lhs, rhs);
	}

	@Override public Node visitLogicalOrExpression(ECMAScriptParser.LogicalOrExpressionContext ctx) {
		IExpression lhs = (IExpression) visit(ctx.singleExpression(0));
		IExpression rhs = (IExpression) visit(ctx.singleExpression(1));
		return new LogicalExpression(ctx.getChild(1).getText(), lhs, rhs);
	}

	@Override public Node visitVoidExpression(ECMAScriptParser.VoidExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UnaryExpression(ctx.Void().getText(), true, argument);
	}

	@Override public Node visitParenthesizedExpression(ECMAScriptParser.ParenthesizedExpressionContext ctx) {
		return visit(ctx.expressionSequence());
	}

	@Override public Node visitUnaryPlusExpression(ECMAScriptParser.UnaryPlusExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UnaryExpression("+", true, argument);
	}

	@Override public Node visitLiteralExpression(ECMAScriptParser.LiteralExpressionContext ctx) {
		// System.out.println(ctx.getText());
		return visitChildren(ctx);
	}

	@Override public Node visitBitNotExpression(ECMAScriptParser.BitNotExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UnaryExpression("~", true, argument);
	}

	@Override public Node visitPostIncrementExpression(ECMAScriptParser.PostIncrementExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UpdateExpression("++", false, argument);
	}

	@Override public Node visitTypeofExpression(ECMAScriptParser.TypeofExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UnaryExpression(ctx.Typeof().getText(), true, argument);
	}

	@Override public Node visitAssignmentOperatorExpression(ECMAScriptParser.AssignmentOperatorExpressionContext ctx) {
		String op = ctx.assignmentOperator().getText();
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new AssignmentExpression(op, left, right);
	}

	@Override public Node visitNewExpression(ECMAScriptParser.NewExpressionContext ctx) {
		IExpression exp = (IExpression) visit(ctx.singleExpression());
    return new NewExpression(
        ((CallExpression) exp).getCallee(),
        ((CallExpression) exp).getArguments());

/*
    IExpression callee;
    IMemberExpression memExp = null, tmp = null;
    int i = 0;
    if (exp.getTypeId() == Node.CALL_EXP) {
      callee = ((CallExpression) exp).getCallee();
    } else {
      callee = exp;
    }
    while (callee.getTypeId() == Node.MEMBER_EXP) {
      if (memExp == null) {
        memExp = (MemberExpression) callee;
        callee = memExp.getObject();
      } else {
        tmp = (MemberExpression) callee;
        callee = tmp.getObject();
      }
    }

    if (memExp == null) {
		  return new NewExpression(
        callee,
        ((CallExpression) exp).getArguments());
    } else {
      if (callee.getTypeId() == Node.CALL_EXP) {
        INewExpression newExp =  new NewExpression(
            ((CallExpression) callee).getCallee(),
            ((CallExpression) callee).getArguments());
        return new MemberExpression(newExp, memExp.getProperty(), memExp.getComputed());
      } else {
        System.out.println("ERROR: AST: NewExpression");
        return null;
      }
    }
    */
  }

	@Override public Node visitPostDecreaseExpression(ECMAScriptParser.PostDecreaseExpressionContext ctx) {
		IExpression argument = (IExpression) visit(ctx.singleExpression());
		return new UpdateExpression("--", false, argument);
	}

	@Override public Node visitRelationalExpression(ECMAScriptParser.RelationalExpressionContext ctx) {
		IExpression lhs = (IExpression) visit(ctx.singleExpression(0));
		IExpression rhs = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.getChild(1).getText(), lhs, rhs);
	}

	@Override public Node visitEqualityExpression(ECMAScriptParser.EqualityExpressionContext ctx) {
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.getChild(1).getText(), left, right);
	}

	@Override public Node visitBitXOrExpression(ECMAScriptParser.BitXOrExpressionContext ctx) {
		IExpression left = (IExpression) visit(ctx.singleExpression(0));
		IExpression right = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.getChild(1).getText(), left, right);
	}

	@Override public Node visitAdditiveExpression(ECMAScriptParser.AdditiveExpressionContext ctx) {
		IExpression lhs = (IExpression) visit(ctx.singleExpression(0));
		IExpression rhs = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.getChild(1).getText(), lhs, rhs);
	}

	@Override public Node visitMemberIndexExpression(ECMAScriptParser.MemberIndexExpressionContext ctx) {
		IExpression object = (IExpression) visit(ctx.singleExpression());
		IExpression property = (IExpression) visit(ctx.expressionSequence());
		return new MemberExpression(object, property, true);
	}

	@Override public Node visitThisExpression(ECMAScriptParser.ThisExpressionContext ctx) {
		return new ThisExpression();
	}

	@Override public Node visitMultiplicativeExpression(ECMAScriptParser.MultiplicativeExpressionContext ctx) {
		IExpression lhs = (IExpression) visit(ctx.singleExpression(0));
		IExpression rhs = (IExpression) visit(ctx.singleExpression(1));
		return new BinaryExpression(ctx.getChild(1).getText(), lhs, rhs);
	}

	@Override public Node visitAssignmentOperator(ECMAScriptParser.AssignmentOperatorContext ctx) { return visitChildren(ctx); }

	@Override public Node visitLiteral(ECMAScriptParser.LiteralContext ctx) {
		Literal literal = (Literal) visitChildren(ctx);
		if (literal == null) {
			literal = new Literal(ctx.getText());
			literal.setSourceLocation(ctx.getText(), ctx.start, ctx.stop);
		}
		return literal;
	}

	@Override public Node visitNumericLiteral(ECMAScriptParser.NumericLiteralContext ctx) {
		Literal literal = null;
		if (ctx.DecimalLiteral() != null) {
			String numStr = ctx.DecimalLiteral().getText();
			boolean isDouble = false;
			int i;
			for (i = 0; i < numStr.length(); i++) {
				char c = numStr.charAt(i);
				if (c == '.') {
					break;
				}
			}
			for (i += 1; i < numStr.length(); i++) {
				char c = numStr.charAt(i);
				if (c != '0') {
					isDouble = true;
				}
			}
			literal = new Literal(Double.valueOf(numStr).doubleValue(), isDouble);
		} else if (ctx.OctalIntegerLiteral() != null) {
			String numStr = ctx.OctalIntegerLiteral().getText().substring(1);
			// literal = new Literal((double) Integer.parseInt(numStr, 8), false);
			literal = new Literal(stringToDouble(numStr, 8), false);
		} else if (ctx.HexIntegerLiteral() != null) {
			String numStr = ctx.HexIntegerLiteral().getText().substring(2);
			// literal = new Literal((double) Integer.parseInt(numStr, 16), false);
			literal = new Literal(stringToDouble(numStr, 16), false);
		}
		literal.setSourceLocation(ctx.getText(), ctx.start, ctx.stop);
		return literal;
	}

	double stringToDouble(String s, int d) {
	    char[] ca = s.toCharArray();
	    double ret = 0;
	    for (int i = 0; i < ca.length; i++) {
	        ret *= d;
	        switch (ca[i]) {
	        case '0': ret += 0; break;
	        case '1': ret += 1; break;
	        case '2': ret += 2; break;
	        case '3': ret += 3; break;
	        case '4': ret += 4; break;
	        case '5': ret += 5; break;
	        case '6': ret += 6; break;
	        case '7': ret += 7; break;
	        case '8': ret += 8; break;
	        case '9': ret += 9; break;
	        case 'a': case 'A': ret += 10; break;
	        case 'b': case 'B': ret += 11; break;
	        case 'c': case 'C': ret += 12; break;
	        case 'd': case 'D': ret += 13; break;
	        case 'e': case 'E': ret += 14; break;
	        case 'f': case 'F': ret += 15; break;
	        }
	    }
	    return ret;
	}

	@Override public Node visitIdentifierName(ECMAScriptParser.IdentifierNameContext ctx) {
		String name = ctx.getText();
		Identifier id = new Identifier(name);
		id.setSourceLocation(name, ctx.start, ctx.stop);
		return id;
	}

	@Override public Node visitReservedWord(ECMAScriptParser.ReservedWordContext ctx) { return visitChildren(ctx); }

	@Override public Node visitKeyword(ECMAScriptParser.KeywordContext ctx) {
		return visitChildren(ctx);
	}

	@Override public Node visitFutureReservedWord(ECMAScriptParser.FutureReservedWordContext ctx) { return visitChildren(ctx); }

	@Override public Node visitGetter(ECMAScriptParser.GetterContext ctx) { return visitChildren(ctx); }

	@Override public Node visitSetter(ECMAScriptParser.SetterContext ctx) { return visitChildren(ctx); }

	@Override public Node visitEos(ECMAScriptParser.EosContext ctx) { return visitChildren(ctx); }

	@Override public Node visitEof(ECMAScriptParser.EofContext ctx) { return visitChildren(ctx); }
}
