package estree;
import java.util.ArrayList;
import java.util.List;
import estree.Node;
//import estree_nodes.*;
//import estree_nodes.Node;
//import estree_nodes.Node.IStatement;
//import estree_nodes.Node.ISwitchCase;

public class EsTreeNormalizer {
	
	Node est;
	
	public EsTreeNormalizer(estree.Node est) {
		this.est = est;
		new HoistingFuncDecl().visitProgram((estree.Program) est);
		new EliminatingNamedFunc().visit(est);
	}
	
	class HoistingFuncDecl extends EsTreeVisitor<List<estree.FunctionDeclaration>> {

		@Override
		public List<FunctionDeclaration> visitArrayExpression(ArrayExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitAssignmentExpression(AssignmentExpression node) {
			// TODO Auto-generated method stub
			if (node.getLeftNodeType() == AssignmentExpression.LeftNodeType.EXPRESSION) {
				visit((estree.Node) node.getExpressionLeft());
			} else {
				visit((estree.Node) node.getPatternLeft());
			}
			visit((estree.Node) node.getRight());
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitBinaryExpression(BinaryExpression node) {
			// TODO Auto-generated method stub
			visit((estree.Node) node.getLeft());
			visit((estree.Node) node.getRight());
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitBlockStatement(BlockStatement node) {
			// TODO Auto-generated method stub
			List<estree.Node.IStatement> body = node.getBody();
			if (body != null) {
				for (estree.Node.IStatement stmt : body) {
					visit((estree.Node) stmt);
				}
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitBreakStatement(BreakStatement node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitCallExpression(CallExpression node) {
			// TODO Auto-generated method stub
			visit((estree.Node) node.getCallee());
			if (node.getArguments() != null) {
				for (estree.Node.IExpression exp : node.getArguments()) {
					visit((estree.Node) exp);
				}
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitCatchClause(CatchClause node) {
			// TODO Auto-generated method stub
			estree.Node.IBlockStatement block = node.getBody();
			if (block != null) {
				List<estree.FunctionDeclaration> funcDecls = new ArrayList<estree.FunctionDeclaration>();
				List<estree.Node.IStatement> stmts = new ArrayList<estree.Node.IStatement>();
				for (estree.Node.IStatement stmt : block.getBody()) {
					if (stmt.getTypeId() == estree.Node.FUNC_DECLARATION) {
						funcDecls.add((estree.FunctionDeclaration) stmt);
					} else {
						stmts.add(stmt);
						List<estree.FunctionDeclaration> _funcDecls = visit((Node) stmt);
						if (_funcDecls != null) {
							funcDecls.addAll(_funcDecls);
						}
					}
				}
				List<estree.Node.IStatement> ret = new ArrayList<estree.Node.IStatement>();
				ret.addAll(funcDecls);
				ret.addAll(stmts);
				node.getBody().setBody(ret);
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitConditionalExpression(ConditionalExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitDebuggerStatement(DebuggerStatement node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitDoWhileStatement(DoWhileStatement node) {
			// TODO Auto-generated method stub
			estree.Node.IStatement body = node.getBody();
			if (body != null) {
				visit((estree.Node) body);
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitEmptyStatement(EmptyStatement node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitExpressionStatement(ExpressionStatement node) {
			// TODO Auto-generated method stub
			visit((Node) node.getExpression());
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitForInStatement(ForInStatement node) {
			// TODO Auto-generated method stub
			estree.Node.IStatement body = node.getBody();
			if (body != null) {
				visit((estree.Node) body);
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitForStatement(ForStatement node) {
			// TODO Auto-generated method stub
			estree.Node.IStatement body = node.getBody();
			if (body != null) {
				visit((estree.Node) body);
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitFunction(Function node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitFunctionDeclaration(FunctionDeclaration node) {
			// TODO Auto-generated method stub
			estree.FunctionDeclaration f = (estree.FunctionDeclaration) node;
			List<estree.FunctionDeclaration> funcDecls = new ArrayList<estree.FunctionDeclaration>();
			List<estree.Node.IStatement> stmts = new ArrayList<estree.Node.IStatement>();
			for (estree.Node.IStatement stmt : f.getBody().getBody()) {
				if (stmt.getTypeId() == estree.Node.FUNC_DECLARATION) {
					funcDecls.add((estree.FunctionDeclaration) stmt);
					visit((estree.FunctionDeclaration) stmt);
				} else {
					stmts.add(stmt);
					List<estree.FunctionDeclaration> decls = visit((estree.Node) stmt);
					if (decls != null) {
						funcDecls.addAll(visit((estree.Node) stmt));
					}
				}
			}
			List<estree.Node.IStatement> ret = new ArrayList<estree.Node.IStatement>();
			ret.addAll(funcDecls);
			ret.addAll(stmts);
			f.getBody().setBody(ret);
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitFunctionExpression(FunctionExpression node) {
			// TODO Auto-generated method stub
			estree.FunctionExpression f = (estree.FunctionExpression) node;
			List<estree.FunctionDeclaration> funcDecls = new ArrayList<estree.FunctionDeclaration>();
			List<estree.Node.IStatement> stmts = new ArrayList<estree.Node.IStatement>();
			for (estree.Node.IStatement stmt : f.getBody().getBody()) {
				if (stmt.getTypeId() == estree.Node.FUNC_DECLARATION) {
					funcDecls.add((estree.FunctionDeclaration) stmt);
					visit((estree.FunctionDeclaration) stmt);
				} else {
					stmts.add(stmt);
					List<estree.FunctionDeclaration> decls = visit((estree.Node) stmt);
					if (decls != null) {
						funcDecls.addAll(visit((estree.Node) stmt));
					}
				}
			}
			List<estree.Node.IStatement> ret = new ArrayList<estree.Node.IStatement>();
			ret.addAll(funcDecls);
			ret.addAll(stmts);
			f.getBody().setBody(ret);
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitIdentifier(Identifier node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitIfStatement(IfStatement node) {
			// TODO Auto-generated method stub
			visit((estree.Node) node.getTest());
			visit((estree.Node) node.getConsequent());
			if (node.getAlternate() != null) {
				visit((estree.Node) node.getAlternate());
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitLabeledStatement(LabeledStatement node) {
			// TODO Auto-generated method stub
		    visit((estree.Node) node.body);
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitLiteral(Literal node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitLogicalExpression(LogicalExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitMemberExpression(MemberExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitNewExpression(NewExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitNode(Node node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitObjectExpression(ObjectExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitProgram(Program node) {
			// TODO Auto-generated method stub
			estree.Program program = (estree.Program) node;
			List<estree.FunctionDeclaration> funcDecls = new ArrayList<estree.FunctionDeclaration>();
			List<estree.Node.IStatement> stmts = new ArrayList<estree.Node.IStatement>();
			for (estree.Node.IStatement stmt : program.getBody()) {
				if (stmt.getTypeId() == estree.Node.FUNC_DECLARATION) {
					funcDecls.add((estree.FunctionDeclaration) stmt);
					visit((estree.FunctionDeclaration) stmt);
				} else {
					stmts.add(stmt);
					List<estree.FunctionDeclaration> decls = visit((estree.Node) stmt);
					if (decls != null) {
						funcDecls.addAll(visit((estree.Node) stmt));
					}
				}
			}
			List<estree.Node.IStatement> ret = new ArrayList<estree.Node.IStatement>();
			ret.addAll(funcDecls);
			ret.addAll(stmts);
			program.setBody(ret);
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitProperty(Property node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitReturnStatement(ReturnStatement node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitSequenceExpression(SequenceExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitSwitchStatement(SwitchStatement node) {
			// TODO Auto-generated method stub
			estree.SwitchStatement s = (estree.SwitchStatement) node; 
			List<estree.Node.ISwitchCase> cases = s.getCases();
			for (estree.Node.ISwitchCase c : cases) {
				visitSwitchCase((estree.SwitchCase) c);
			}
			return null;
		}
		
		@Override
		public List<FunctionDeclaration> visitSwitchCase(SwitchCase node) {
			// TODO Auto-generated method stub
			// List<estree.FunctionDeclaration> funcDecls = new ArrayList<estree.FunctionDeclaration>();
			// List<estree.Node.IStatement> stmts = new ArrayList<estree.Node.IStatement>();
			for (estree.Node.IStatement stmt : node.getConsequent()) {
				visit((estree.Node) stmt);
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitTryStatement(TryStatement node) {
			// TODO Auto-generated method stub
			visit((estree.Node) node.getBlock());
			if (node.getHandler() != null) {
				visit((estree.Node) node.getHandler());
			}
			if (node.getFinalizer() != null) {
				visit((estree.Node)node.getFinalizer());
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitUnaryExpression(UnaryExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitUpdateExpression(UpdateExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitVariableDeclaration(VariableDeclaration node) {
			// TODO Auto-generated method stub
			List<estree.Node.IVariableDeclarator> varDecls = node.getDeclarations();
			if (varDecls != null) {
				for (estree.Node.IVariableDeclarator varDecl : varDecls) {
					visitVariableDeclarator((VariableDeclarator) varDecl);
				}
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitVariableDeclarator(VariableDeclarator node) {
			// TODO Auto-generated method stub
			if (node.getInit() != null) {
				visit((estree.Node) node.getInit());
			}
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitWhileStatement(WhileStatement node) {
			// TODO Auto-generated method stub
		    if (node.body != null)
		        visit((estree.Node) node.body);
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitWithStatement(WithStatement node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitContinueStatement(ContinueStatement node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitThisExpression(ThisExpression node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<FunctionDeclaration> visitThrowStatement(ThrowStatement node) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

	class EliminatingNamedFunc extends EsTreeVisitor<Node> {

		@Override
		public Node visitArrayExpression(ArrayExpression node) {
			// TODO Auto-generated method stub
			List<estree.Node.IExpression> newElements = new ArrayList<estree.Node.IExpression>();
			for (estree.Node.IExpression el : node.getElements()) {
				newElements.add((estree.Node.IExpression) visit((Node) el));
			}
			node.setElements(newElements);
			return node;
		}

		@Override
		public Node visitAssignmentExpression(AssignmentExpression node) {
			// TODO Auto-generated method stub
			node.setRight((estree.Node.IExpression) visit((estree.Node) node.getRight()));
			return node;
		}

		@Override
		public Node visitBinaryExpression(BinaryExpression node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitBlockStatement(BlockStatement node) {
			// TODO Auto-generated method stub
			List<estree.Node.IStatement> stmts = node.getBody();
			List<estree.Node.IStatement> newstmts = new ArrayList<estree.Node.IStatement>(); 
			if (stmts != null) {
				for (estree.Node.IStatement stmt : stmts) {
					newstmts.add((estree.Node.IStatement) visit((estree.Node) stmt));
				}
			}
			node.setBody(newstmts);
			return node;
		}

		@Override
		public Node visitBreakStatement(BreakStatement node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitCallExpression(CallExpression node) {
			// TODO Auto-generated method stub
			node.setCallee((estree.Node.IExpression) visit((estree.Node) node.getCallee()));
			List<estree.Node.IExpression> newArgs = new ArrayList<estree.Node.IExpression>();
			for (estree.Node.IExpression arg : node.getArguments()) {
				newArgs.add((estree.Node.IExpression) visit((estree.Node)arg));
			}
			node.setArguments(newArgs);
			return node;
		}

		@Override
		public Node visitCatchClause(CatchClause node) {
			// TODO Auto-generated method stub
			node.setBody((estree.Node.IBlockStatement) visit((estree.Node) node.getBody()));
			return node;
		}

		@Override
		public Node visitConditionalExpression(ConditionalExpression node) {
			// TODO Auto-generated method stub
			node.setTest((estree.Node.IExpression) visit((estree.Node) node.getTest()));
			node.setAlternate((estree.Node.IExpression) visit((estree.Node) node.getAlternate()));
			node.setConsequent((estree.Node.IExpression) visit((estree.Node) node.getConsequent()));
			return node;
		}

		@Override
		public Node visitDebuggerStatement(DebuggerStatement node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitDoWhileStatement(DoWhileStatement node) {
			// TODO Auto-generated method stub
			node.setTest((estree.Node.IExpression) visit((estree.Node) node.getTest()));
			node.setBody((estree.Node.IStatement) visit((estree.Node) node.getBody()));
			return node;
		}

		@Override
		public Node visitEmptyStatement(EmptyStatement node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitExpressionStatement(ExpressionStatement node) {
			// TODO Auto-generated method stub
			node.setExpression((estree.Node.IExpression) visit((estree.Node) node.getExpression()));
			return node;
		}

		@Override
		public Node visitForInStatement(ForInStatement node) {
			// TODO Auto-generated method stub
			if (node.getInitType() == ForInStatement.InitType.PATTERN) {
				node.setPatternLeft((estree.Node.IPattern) visit((estree.Node) node.getPatternLeft()));
			} else {
				node.setVarDeclLeft((estree.Node.IVariableDeclaration) visit((estree.Node) node.getValDeclLeft()));
			}
			node.setRight((estree.Node.IExpression) visit((estree.Node) node.getRight()));
			node.setBody((estree.Node.IStatement) visit((estree.Node) node.getBody()));
			return node;
		}

		@Override
		public Node visitForStatement(ForStatement node) {
			// TODO Auto-generated method stub
			if (node.getInitType() == ForStatement.InitType.EXPRESSION) {
				node.setExpInit((estree.Node.IExpression) visit((estree.Node) node.getExpInit()));
			} else if (node.getInitType() == ForStatement.InitType.VAR_DECL) {
				node.setVarDeclInit((estree.Node.IVariableDeclaration) visit((estree.Node) node.getValDeclInit()));
			}
			if (node.getTest() != null) {
				node.setTest((estree.Node.IExpression) visit((estree.Node) node.getTest()));
			}
			if (node.getUpdate() != null) {
				node.setUpdate((estree.Node.IExpression) visit((estree.Node) node.getUpdate()));
			}
			node.setBody((estree.Node.IStatement) visit((estree.Node) node.getBody()));
			return node;
		}

		@Override
		public Node visitFunction(Function node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitFunctionDeclaration(FunctionDeclaration node) {
			// TODO Auto-generated method stub
			/*
			FunctionExpression anonymousFuncExp = new FunctionExpression(null, node.getParams(), node.getBody());
			VariableDeclarator varDecl = new VariableDeclarator(node.getId(), anonymousFuncExp);
			List<estree.Node.IVariableDeclarator> varDecls = new ArrayList<estree.Node.IVariableDeclarator>();
			varDecls.add(varDecl);
			return new VariableDeclaration(varDecls);*/
			
			node.setBody((estree.Node.IBlockStatement) visit((estree.Node) node.getBody()));
			return node;
		}

		@Override
		public Node visitFunctionExpression(FunctionExpression node) {
			// TODO Auto-generated method stub
			if (node.getId() != null) {
				List<estree.Node.IStatement> stmts = new ArrayList<estree.Node.IStatement>();
				FunctionExpression inner = new FunctionExpression(null, node.getParams(), node.getBody());
				List<estree.Node.IVariableDeclarator> varDecl = new ArrayList<estree.Node.IVariableDeclarator>();
				varDecl.add(new VariableDeclarator(node.getId(), inner));
				VariableDeclaration varDecls = new VariableDeclaration(varDecl);
				stmts.add(varDecls);
				// CallExpression call = new CallExpression(node.getId(), (List<Node.IExpression>) new ArrayList<estree.Node.IExpression>());
				List<Node.IExpression> args = new ArrayList<Node.IExpression>();
				for (Node.IPattern ptn : node.getParams()) {
					args.add(new Identifier(((Identifier) ptn).getName()));  // TODO: FIX
				}
				CallExpression call = new CallExpression(node.getId(), args);
				stmts.add(new ExpressionStatement(call));
				BlockStatement block = new BlockStatement(stmts);
				FunctionExpression outer = new FunctionExpression(null, node.getParams(), block);
				return outer;
			} else {
				return node;
			}
		}

		@Override
		public Node visitIdentifier(Identifier node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitIfStatement(IfStatement node) {
			// TODO Auto-generated method stub
			node.setTest((estree.Node.IExpression) visit((estree.Node) node.getTest()));
			node.setConsequent((estree.Node.IStatement) visit((estree.Node) node.getConsequent()));
			if (node.getAlternate() != null) {
				node.setAlternate((estree.Node.IStatement) visit((estree.Node) node.getAlternate()));
			}
			return node;
		}

		@Override
		public Node visitLabeledStatement(LabeledStatement node) {
		    node.setBody((estree.Node.IStatement) visit((estree.Node) node.getBody()));
			return node;
		}

		@Override
		public Node visitLiteral(Literal node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitLogicalExpression(LogicalExpression node) {
			// TODO Auto-generated method stub
			node.setLeft((estree.Node.IExpression) visit((estree.Node) node.getLeft()));
			node.setRight((estree.Node.IExpression) visit((estree.Node) node.getRight()));
			return node;
		}

		@Override
		public Node visitMemberExpression(MemberExpression node) {
			// TODO Auto-generated method stub
			node.setObject((estree.Node.IExpression) visit((estree.Node) node.getObject()));
			node.setProperty((estree.Node.IExpression) visit((estree.Node) node.getProperty()));
			return node;
		}

		@Override
		public Node visitNewExpression(NewExpression node) {
			// TODO Auto-generated method stub
			node.setCallee((estree.Node.IExpression) visit((estree.Node) node.getCallee()));
			List<estree.Node.IExpression> newArguments = new ArrayList<estree.Node.IExpression>();
			for (estree.Node.IExpression arg : node.getArguments()) {
				newArguments.add((estree.Node.IExpression) visit((estree.Node) arg));
			}
			node.setArguments(newArguments);
			return node;
		}

		@Override
		public Node visitNode(Node node) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Node visitObjectExpression(ObjectExpression node) {
			// TODO Auto-generated method stub
			List<estree.Node.IProperty> props = new ArrayList<estree.Node.IProperty>();
			for (estree.Node.IProperty p : node.getProperties()) {
				props.add((estree.Node.IProperty) visit((estree.Node) p));
			}
			node.setProperties(props);
			return node;
		}

		@Override
		public Node visitProgram(Program node) {
			// TODO Auto-generated method stub
			List<estree.Node.IStatement> newBody = new ArrayList<estree.Node.IStatement>();
			for (estree.Node.IStatement stmt : node.getBody()) {
				newBody.add((estree.Node.IStatement) visit((estree.Node) stmt));
			}
			node.setBody(newBody);
			return node;
		}

		@Override
		public Node visitProperty(Property node) {
			// TODO Auto-generated method stub
			node.setValue((estree.Node.IExpression) visit((estree.Node) node.getValue()));
			return node;
		}

		@Override
		public Node visitReturnStatement(ReturnStatement node) {
			// TODO Auto-generated method stub
		    if (node.getArgument() != null) {
		        node.setArgument((estree.Node.IExpression) visit((estree.Node) node.getArgument()));
		    }
			return node;
		}

		@Override
		public Node visitSequenceExpression(SequenceExpression node) {
			// TODO Auto-generated method stub
			List<estree.Node.IExpression> newExp = new ArrayList<estree.Node.IExpression>();
			for (estree.Node.IExpression exp : node.getExpression()) {
				newExp.add((estree.Node.IExpression) visit((estree.Node) exp));
			}
			node.setExpression(newExp);
			return node;
		}

		@Override
		public Node visitSwitchCase(SwitchCase node) {
			// TODO Auto-generated method stub
		    if (node.getTest() != null) {
		        node.setTest((estree.Node.IExpression) visit((estree.Node) node.getTest()));
		    }
			List<estree.Node.IStatement> newStmts = new ArrayList<estree.Node.IStatement>();
			for (estree.Node.IStatement stmt : node.getConsequent()) {
				newStmts.add((estree.Node.IStatement) visit((estree.Node) stmt));
			}
			node.setConsequent(newStmts);
			return node;
		}

		@Override
		public Node visitSwitchStatement(SwitchStatement node) {
			// TODO Auto-generated method stub
			node.setDiscriminant((Node.IExpression) visit((Node) node.getDiscriminant()));
			List<Node.ISwitchCase> newCases = new ArrayList<Node.ISwitchCase>();
			for (Node.ISwitchCase c : node.getCases()) {
				newCases.add((Node.ISwitchCase) visit((Node) c));
			}
			node.setCases(newCases);
			return node;
		}

		@Override
		public Node visitTryStatement(TryStatement node) {
			// TODO Auto-generated method stub
			node.setBlock((Node.IBlockStatement) visit((Node) node.getBlock()));
			node.setHandler((Node.ICatchClause) visit((Node) node.getHandler()));
			node.setFinalizer((Node.IBlockStatement) visit((Node) node.getFinalizer()));
			return node;
		}

		@Override
		public Node visitUnaryExpression(UnaryExpression node) {
			// TODO Auto-generated method stub
			node.setArgument((Node.IExpression) visit((estree.Node) node.getArgument()));
			return node;
		}

		@Override
		public Node visitUpdateExpression(UpdateExpression node) {
			// TODO Auto-generated method stub
			node.setArgument((Node.IExpression) visit((Node) node.getArgument()));
			return node;
		}

		@Override
		public Node visitVariableDeclaration(VariableDeclaration node) {
			// TODO Auto-generated method stub
			List<Node.IVariableDeclarator> newVarDecls = new ArrayList<Node.IVariableDeclarator>();
			for (Node.IVariableDeclarator varDecl : node.getDeclarations()) {
				newVarDecls.add((Node.IVariableDeclarator) visit((Node) varDecl));
			}
			node.setDeclarations(newVarDecls);
			return node;
		}

		@Override
		public Node visitVariableDeclarator(VariableDeclarator node) {
			// TODO Auto-generated method stub
		    if (node.getInit() != null) {
		        node.setInit((Node.IExpression) visit((Node) node.getInit()));
		    }
			return node;
		}

		@Override
		public Node visitWhileStatement(WhileStatement node) {
			// TODO Auto-generated method stub
			node.setBody((Node.IStatement) visit((Node) node.getBody()));
			return node;
		}

		@Override
		public Node visitWithStatement(WithStatement node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitContinueStatement(ContinueStatement node) {
			// TODO Auto-generated method stub
			return node;
		}

		@Override
		public Node visitThisExpression(ThisExpression node) {
			return node;
		}

		@Override
		public Node visitThrowStatement(ThrowStatement node) {
			// TODO Auto-generated method stub
			return node;
		}
		
	}
}
