/*
   IASTGenerator.java

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

import ejsc.ast_node.*;
import ejsc.ast_node.Node.*;

public class IASTGenerator extends ESTreeBaseVisitor<IASTNode> {

	public IASTGenerator() {};

	public IASTProgram gen(Node estree) {
		return (IASTProgram) visitProgram((Program) estree);
	}

	List<String> hoistDeclarations(IStatement nd) {
	    List<String> declIds = new ArrayList<String>();
	    List<String> lexicalIds = new ArrayList<String>();
	    hoistDeclarations_(declIds, lexicalIds, nd);
	    // delete duplicate string
	    List<String> uniqDeclIds = new ArrayList<String>();
	    for (String s : declIds) {
	        boolean isUniq = true;
	        for (String t : uniqDeclIds) {
	            if (s.equals(t)) {
	                isUniq = false;
	                break;
	            }
	        }
	        if (isUniq) {
	            uniqDeclIds.add(s);
	        }
	    }
	    return uniqDeclIds;
	}

	void hoistDeclarations_(List<String> declIds, List<String> lexicalIds, IStatement nd) {
        // List<String> localNames = new ArrayList<String>();
        switch (nd.getTypeId()) {
        case Node.FUNC_DECLARATION: {
            FunctionDeclaration fn = (FunctionDeclaration) nd;
            // localNames.add(fn.getId().getName());
            declIds.add(fn.getId().getName());
        } break;
        case Node.VAR_DECLARATION: {
            for (IVariableDeclarator vd : ((VariableDeclaration) nd).getDeclarations()) {
                // localNames.add(((IIdentifier) vd.getId()).getName());
                declIds.add(((Identifier) vd.getId()).getName());
            }
        } break;
        case Node.BLOCK_STMT: {
            for (IStatement stmt: ((IBlockStatement) nd).getBody()) {
                // declIds.addAll(hoistDeclarations_(stmt));
                hoistDeclarations_(declIds, lexicalIds, stmt);
            }
        } break;
        case Node.IF_STMT: {
            IIfStatement ifstmt = (IIfStatement) nd;
            // localNames.addAll(hoistDeclarations_(ifstmt.getConsequent()));
            // localNames.addAll(hoistDeclarations_(ifstmt.getAlternate()));
            hoistDeclarations_(declIds, lexicalIds, ifstmt.getConsequent());
            if (ifstmt.getAlternate() != null)
                hoistDeclarations_(declIds, lexicalIds, ifstmt.getAlternate());
        } break;
        case Node.DO_WHILE_STMT: {
            IDoWhileStatement dowhile = (IDoWhileStatement) nd;
            // localNames.addAll(hoistDeclarations_(dowhile.getBody()));
            hoistDeclarations_(declIds, lexicalIds, dowhile.getBody());
        } break;
        case Node.WHILE_STMT: {
            IWhileStatement whileStmt = (IWhileStatement) nd;
            // localNames.addAll(hoistDeclarations_(whileStmt.getBody()));
            hoistDeclarations_(declIds, lexicalIds, whileStmt.getBody());
        } break;
        case Node.FOR_STMT: {
            IForStatement forStmt = (IForStatement) nd;
            if (forStmt.getValDeclInit() != null) {
                // localNames.addAll(hoistDeclarations_(forStmt.getValDeclInit()));
                hoistDeclarations_(declIds, lexicalIds, forStmt.getValDeclInit());
            }
            // localNames.addAll(hoistDeclarations_(forStmt.getBody()));
            hoistDeclarations_(declIds, lexicalIds, forStmt.getBody());
        } break;
        case Node.FOR_IN_STMT: {
            IForInStatement forinStmt = (IForInStatement) nd;
            if (forinStmt.getValDeclLeft() != null) {
                // localNames.addAll(hoistDeclarations_(forinStmt.getValDeclLeft()));
                hoistDeclarations_(declIds, lexicalIds, forinStmt.getValDeclLeft());
            }
            // localNames.addAll(hoistDeclarations_(forinStmt.getBody()));
            hoistDeclarations_(declIds, lexicalIds, forinStmt.getBody());
        } break;
        case Node.TRY_STMT: {
            ITryStatement tryStmt = (ITryStatement) nd;
            // localNames.addAll(hoistDeclarations_(tryStmt.getBlock()));
            hoistDeclarations_(declIds, lexicalIds, tryStmt.getBlock());
            if (tryStmt.getHandler() != null) {
                ICatchClause handler = tryStmt.getHandler();
                if (handler.getParam() != null) {
                    lexicalIds.add(((Identifier) handler.getParam()).getName());
                }
                hoistDeclarations_(declIds, lexicalIds, tryStmt.getBlock());
            }
            if (tryStmt.getFinalizer() != null) {
                // localNames.addAll(hoistDeclarations_(tryStmt.getFinalizer()));
                hoistDeclarations_(declIds, lexicalIds, tryStmt.getFinalizer());
            }
        } break;
        case Node.LABELED_STMT: {
            ILabeledStatement labeled = (ILabeledStatement) nd;
            // localNames.addAll(hoistDeclarations_(labeled.getBody()));
            hoistDeclarations_(declIds, lexicalIds, labeled.getBody());
        } break;
        case Node.SWITCH_STMT: {
            ISwitchStatement switchStmt = (ISwitchStatement) nd;
            for (ISwitchCase c : switchStmt.getCases()) {
                for (IStatement s : c.getConsequent()) {
                    // localNames.addAll(hoistDeclarations_(s));
                    hoistDeclarations_(declIds, lexicalIds, s);
                }
            }
        } break;
        }
    }

	IASTBlockStatement listOfIStatement2IASTBlock(List<IStatement> src) {
		List<IASTStatement> stmts = new ArrayList<IASTStatement>();
		for (IStatement s : src) {
			stmts.add((IASTStatement) s.accept(this));
		}
		return new IASTBlockStatement(stmts);
	}

	public IASTNode visitIdentifier(Identifier node) {
		return new IASTIdentifier(node.getName());
	}
	protected IASTNode visitLiteral(Literal node) {
		switch (node.getLiteralType()) {
		case STRING:
			return new IASTStringLiteral(node.getStringValue());
		case BOOLEAN:
			return new IASTBooleanLiteral(node.getBooleanValue());
		case NULL:
			return new IASTNullLiteral();
		case NUMBER:
			return new IASTNumericLiteral(node.getNumValue());
		case REG_EXP:
			return new IASTRegExpLiteral(node.getRegExpValue());
		}
		return null; // ERROR
	}
	/*protected IASTNode visitRegExpLiteral(RegExpLiteral node) {
		return new IASTRegExpLiteral(node.getRegExpValue());
	}*/
	protected IASTNode visitProgram(Program node) {
		List<IASTStatement> stmts = new ArrayList<IASTStatement>();
		for (IStatement stmt : node.getBody()) {
		    // System.out.println(stmt.accept(this));
	        stmts.add((IASTStatement) stmt.accept(this));
	    }

		List<String> params = new ArrayList<String>();
		List<String> locals = new ArrayList<String>();
		IASTBlockStatement block = new IASTBlockStatement(stmts);
		IASTFunctionExpression func = new IASTFunctionExpression(params, locals, block);
		return new IASTProgram(func);
	}
	/*protected IASTNode visitFunction(IFunction node) {
		node.getId().accept(this);
		for (IPattern param : node.getParams()) {
			param.accept(this);
		}
		node.getBody().accept(this);
		return visitNode(node);
	}*/
	/*protected IASTNode visitStatement(IStatement node) {
		return visitNode(node);
	}*/
	protected IASTNode visitExpressionStatement(ExpressionStatement node) {
		return new IASTExpressionStatement((IASTExpression) node.getExpression().accept(this));
	}
	protected IASTNode visitBlockStatement(BlockStatement node) {
		List<IASTStatement> stmts = new ArrayList<IASTStatement>();
		for (IStatement stmt : node.getBody()) {
			stmts.add((IASTStatement) stmt.accept(this));
		}
		return new IASTBlockStatement(stmts);
	}
	protected IASTNode visitEmptyStatement(EmptyStatement node) {
	    return new IASTEmptyStatement();
	}/*
	protected IASTNode visitDebuggerStatement(IDebuggerStatement node) {
		return visitStatement(node);
	}*/
	protected IASTNode visitWithStatement(WithStatement node) {
	    IASTExpression object = (IASTExpression) node.getObject().accept(this);
	    IASTStatement body = (IASTStatement) node.getBody().accept(this);
		return new IASTWithStatement(object, body);
	}
	protected IASTNode visitReturnStatement(ReturnStatement node) {
		IASTExpression arg = null;
		if (node.getArgument() != null) {
			arg = (IASTExpression) node.getArgument().accept(this);
		}
		return new IASTReturnStatement(arg);
	}
	protected IASTNode visitLabeledStatement(LabeledStatement node) {
		IASTStatement stmt = (IASTStatement) node.getBody().accept(this);
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
	protected IASTNode visitBreakStatement(BreakStatement node) {
	    String labelName = null;
	    if (node.getLabel() != null) labelName = node.getLabel().getName();
		return new IASTBreakStatement(labelName);
	}
	protected IASTNode visitContinueStatement(ContinueStatement node) {
	    String labelName = null;
        if (node.getLabel() != null) labelName = node.getLabel().getName();
		return new IASTContinueStatement(labelName);
	}
	protected IASTNode visitIfStatement(IfStatement node) {
		IASTExpression test = (IASTExpression) node.getTest().accept(this);
		IASTStatement conseq = (IASTStatement) node.getConsequent().accept(this);
		IASTStatement alter = null;
		if (node.getAlternate() != null) {
			alter = (IASTStatement) node.getAlternate().accept(this);
		}
		return new IASTIfStatement(test, conseq, alter);
	}
	protected IASTNode visitSwitchStatement(SwitchStatement node) {
		IASTExpression discriminant = (IASTExpression) node.getDiscriminant().accept(this);
		List<IASTSwitchStatement.CaseClause> cases = new ArrayList<IASTSwitchStatement.CaseClause>();
		for (ISwitchCase c : node.getCases()) {
			List<IASTStatement> blockBody = new ArrayList<IASTStatement>();
			for (IStatement s : c.getConsequent()) {
				blockBody.add((IASTStatement) s.accept(this));
			}
			IASTExpression test = (IASTExpression) (c.getTest() == null ? null : c.getTest().accept(this));
			cases.add(new IASTSwitchStatement.CaseClause(
					test,
					(IASTStatement) new IASTBlockStatement(blockBody)));
		}
		return new IASTSwitchStatement(discriminant, cases);
	}
	protected IASTNode visitThrowStatement(ThrowStatement node) {
		return new IASTThrowStatement((IASTExpression) node.getArgument().accept(this));
	}
	protected IASTNode visitTryStatement(TryStatement node) {
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
	/*protected IASTNode visitCatchClause(ICatchClause node) {
		node.getParam().accept(this);
		node.getBody().accept(this);
		return visitNode(node);
	}*/
	protected IASTNode visitWhileStatement(WhileStatement node) {
		IASTExpression test = (IASTExpression) node.getTest().accept(this);
		IASTStatement body = (IASTStatement) node.getBody().accept(this);
		return new IASTWhileStatement(test, body);
	}
	protected IASTNode visitDoWhileStatement(DoWhileStatement node) {
		IASTExpression test = (IASTExpression) node.getTest().accept(this);
		IASTStatement body = (IASTStatement) node.getBody().accept(this);
		return new IASTDoWhileStatement(test, body);
	}
	protected IASTNode visitForStatement(ForStatement node) {
	    IASTExpression init = null, test = null, update = null;
	    IASTStatement body = null;
		if (node.getExpInit() != null) {
			init = (IASTExpression) node.getExpInit().accept(this);
		} else if (node.getValDeclInit() != null) {
			init = ((IASTExpressionStatement) node.getValDeclInit().accept(this)).exp;
		}
		if (node.getTest() != null) {
		    test = (IASTExpression) node.getTest().accept(this);
		}
		if (node.getUpdate() != null) {
		    update = (IASTExpression) node.getUpdate().accept(this);
		}
		if (node.getBody() != null) {
		    body = (IASTStatement) node.getBody().accept(this);
		}
		return new IASTForStatement(init, test, update, body);
	}
	protected IASTNode visitForInStatement(ForInStatement node) {
	    String v = null;
	    IASTExpression obj = null;
	    IASTStatement body = null;
		if (node.getPatternLeft() != null) {
		    v = ((IIdentifier) node.getPatternLeft()).getName();
			// v = ((IIdentifier) node.getValDeclLeft()).getName();
		} else if (node.getValDeclLeft() != null) {
		    List<IVariableDeclarator> varDecls = node.getValDeclLeft().getDeclarations();
		    if (varDecls.size() == 1) {
		        v = ((IIdentifier) varDecls.get(0).getId()).getName();
		    } else {
		        // what behavior???
		    }
		}
		obj = (IASTExpression) node.getRight().accept(this);
		body = (IASTStatement) node.getBody().accept(this);
		return new IASTForInStatement(v, obj, body);
	}
	/*protected IASTNode visitDeclaration(Declaration node) {
		return visitStatement(node);
	}*/
	protected IASTNode visitFunctionDeclaration(FunctionDeclaration node) {
		IASTIdentifier id = (IASTIdentifier) node.getId().accept(this);
		List<String> params = new ArrayList<String>();
		for (IPattern param : node.getParams()) {
			params.add(((IIdentifier) param).getName());
		}
		List<String> locals = hoistDeclarations(node.getBody());
		IASTStatement body = (IASTStatement) node.getBody().accept(this);
		IASTFunctionExpression func = new IASTFunctionExpression(params, locals, body);
		IASTBinaryExpression assign = new IASTBinaryExpression(
				IASTBinaryExpression.Operator.ASSIGN, id, func);
		return new IASTExpressionStatement(assign);
	}
	protected IASTNode visitVariableDeclaration(VariableDeclaration node) {
		List<IASTExpression> exps = new ArrayList<IASTExpression>();
		for (IVariableDeclarator vd : node.getDeclarations()) {
			Object r = vd.accept(this);
			if (r != null) {
				exps.add((IASTExpression) r);
			}
		}
		return new IASTExpressionStatement(new IASTSequenceExpression(exps));
	}
	protected IASTNode visitVariableDeclarator(VariableDeclarator node) {
		if (node.getInit() == null) {
			return null;
		}
		IASTIdentifier id = (IASTIdentifier) node.getId().accept(this);
		IASTExpression exp = (IASTExpression) node.getInit().accept(this);
		IASTBinaryExpression assign = new IASTBinaryExpression(
				IASTBinaryExpression.Operator.ASSIGN, id, exp);
		return assign;
	}
	/*protected IASTNode visitExpression(IExpression node) {
		return visitNode(node);
	}*/
	protected IASTNode visitThisExpression(ThisExpression node) {
		return new IASTThisExpression();
	}
	protected IASTNode visitArrayExpression(ArrayExpression node) {
		List<IASTExpression> elements = new ArrayList<IASTExpression>();
		for (IExpression e : node.getElements()) {
			elements.add((IASTExpression) e.accept(this));
		}
		return new IASTArrayExpression(elements);
	}
	protected IASTNode visitObjectExpression(ObjectExpression node) {
	    List<IASTObjectExpression.Property> props = new ArrayList<IASTObjectExpression.Property>();
		for (IProperty p : node.getProperties()) {
			// props.add((IASTObjectExpression.Property) p.accept(this));
		    IASTLiteral key = null;
		    if (p.getLiteralKey() != null)
		        key = (IASTLiteral) p.getLiteralKey().accept(this);
		    else if (p.getIdentifierKey() != null)
		        key = new IASTStringLiteral(p.getIdentifierKey().getName());
		    IASTExpression value = (IASTExpression) p.getValue().accept(this);
		    props.add(new IASTObjectExpression.Property(key, value, IASTObjectExpression.Property.Kind.INIT));
		}
		return new IASTObjectExpression(props);
	}
	/*protected IASTNode visitProperty(Property node) {
		IASTLiteral key = null;
		if (node.getLiteralKey() != null) {
			key = (IASTLiteral) node.getLiteralKey().accept(this);
		} else if (node.getIdentifierKey() != null) {
			key = new IASTStringLiteral(node.getIdentifierKey().getName());
		} // else ERROR
		IASTExpression value = (IASTExpression) node.getValue().accept(this);
		return new IASTObjectExpression.Property(key, value, IASTObjectExpression.Property.Kind.INIT);
	}*/
	protected IASTNode visitFunctionExpression(FunctionExpression node) {
		List<String> params = new ArrayList<String>();
		for (IPattern param : node.getParams()) {
			params.add(((IIdentifier) param).getName());
		}
		List<String> locals = hoistDeclarations(node.getBody());
		IASTBlockStatement body = (IASTBlockStatement) node.getBody().accept(this);
		return new IASTFunctionExpression(params, locals, body);
	}
	protected IASTNode visitUnaryExpression(UnaryExpression node) {
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
			operator = IASTUnaryExpression.Operator.BNOT;
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
	protected IASTNode visitUpdateExpression(UpdateExpression node) {
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
	protected IASTNode visitBinaryExpression(BinaryExpression node) {
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
	protected IASTNode visitAssignmentExpression(AssignmentExpression node) {
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
	protected IASTNode visitLogicalExpression(LogicalExpression node) {
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
	protected IASTNode visitMemberExpression(MemberExpression node) {
		IASTExpression object = (IASTExpression) node.getObject().accept(this);
		IASTExpression property = (IASTExpression) node.getProperty().accept(this);
		if (!node.getComputed()) {
		    if (property instanceof IASTIdentifier) {
		        property = new IASTStringLiteral(((IASTIdentifier) property).id);
		    }
		}
		return new IASTMemberExpression(object, property);
	}
	protected IASTNode visitConditionalExpression(ConditionalExpression node) {
		IASTExpression exp1 = (IASTExpression) node.getTest().accept(this);
		IASTExpression exp2 = (IASTExpression) node.getConsequent().accept(this);
		IASTExpression exp3 = (IASTExpression) node.getAlternate().accept(this);
		return new IASTTernaryExpression(IASTTernaryExpression.Operator.COND, exp1, exp2, exp3);
	}
	protected IASTNode visitCallExpression(CallExpression node) {
		IASTExpression callee = (IASTExpression) node.getCallee().accept(this);
		List<IASTExpression> arguments = new ArrayList<IASTExpression>();
		for (IExpression e : node.getArguments()) {
			arguments.add((IASTExpression) e.accept(this));
		}
		return new IASTCallExpression(callee, arguments);
	}
	protected IASTNode visitNewExpression(NewExpression node) {
		IASTExpression constructor = (IASTExpression) node.getCallee().accept(this);
		List<IASTExpression> arguments = new ArrayList<IASTExpression>();
		for (IExpression e : node.getArguments()) {
			arguments.add((IASTExpression) e.accept(this));
		}
		return new IASTNewExpression(constructor, arguments);
	}
	protected IASTNode visitSequenceExpression(SequenceExpression node) {
		for (IExpression e : node.getExpression()) {
			e.accept(this);
		}
		return visitExpression(node);
	}
}
