import java.util.LinkedList;
import java.util.List;

import ast_node.*;
import ast_node.Node.*;

public class ESTreeNormalizer {
    
    public void normalize(Node estree) {
        new HoistingFuncDecl().run((INode) estree);
        new EliminatingNamedFunc().run((INode) estree);
    }

    class HoistingFuncDecl extends ESTreeBaseVisitor<Object> {
        
        List<FunctionDeclaration> funcDecls = null;
        
        public void run(INode estree) {
            visit(estree);
        }
        
        protected Object visitProgram(Program node) {
            funcDecls = new LinkedList<FunctionDeclaration>();
            List<IStatement> stmts = new LinkedList<IStatement>();
            for (IStatement stmt : node.getBody()) {
                if (stmt instanceof FunctionDeclaration) {
                    funcDecls.add((FunctionDeclaration) stmt);
                } else {
                    stmts.add(stmt);
                }
                visit(stmt);
            }
            List<IStatement> body = new LinkedList<IStatement>();
            body.addAll(funcDecls);
            body.addAll(stmts);
            node.setBody(body);
            return null;
        }
        
        protected Object visitFunctionDeclaration(FunctionDeclaration node) {
            List<FunctionDeclaration> tmp = funcDecls;
            funcDecls = new LinkedList<FunctionDeclaration>();
            List<IStatement> stmts = new LinkedList<IStatement>();
            for (IStatement stmt : node.getBody().getBody()) {
                if (stmt instanceof FunctionDeclaration) {
                    funcDecls.add((FunctionDeclaration) stmt);
                } else {
                    stmts.add(stmt);
                }
                visit(stmt);
            }
            List<IStatement> body = new LinkedList<IStatement>();
            body.addAll(funcDecls);
            body.addAll(stmts);
            node.getBody().setBody(body);
            funcDecls = tmp;
            return null;
        }
        
        @Override
        protected Object visitFunctionExpression(FunctionExpression node) {
            List<FunctionDeclaration> tmp = funcDecls;
            funcDecls = new LinkedList<FunctionDeclaration>();
            List<IStatement> stmts = new LinkedList<IStatement>();
            for (IStatement stmt : node.getBody().getBody()) {
                if (stmt instanceof FunctionDeclaration) {
                    funcDecls.add((FunctionDeclaration) stmt);
                } else {
                    stmts.add(stmt);
                }
                visit(stmt);
            }
            List<IStatement> body = new LinkedList<IStatement>();
            body.addAll(funcDecls);
            body.addAll(stmts);
            node.getBody().setBody(body);
            funcDecls = tmp;
            return null;
        }
        
        protected Object visitCatchClause(CatchClause node) {
            List<FunctionDeclaration> tmp = funcDecls;
            funcDecls = new LinkedList<FunctionDeclaration>();
            List<IStatement> stmts = new LinkedList<IStatement>();
            for (IStatement stmt : node.getBody().getBody()) {
                if (stmt instanceof FunctionDeclaration) {
                    funcDecls.add((FunctionDeclaration) stmt);
                } else {
                    stmts.add(stmt);
                }
                visit(stmt);
            }
            List<IStatement> body = new LinkedList<IStatement>();
            body.addAll(funcDecls);
            body.addAll(stmts);
            node.getBody().setBody(body);
            funcDecls = tmp;
            return null;
        }
        
        protected Object visitBlockStatement(BlockStatement node) {
            List<IStatement> stmts = new LinkedList<IStatement>();
            for (IStatement stmt : node.getBody()) {
                if (stmt instanceof FunctionDeclaration) {
                    funcDecls.add((FunctionDeclaration) stmt);
                } else {
                    stmts.add(stmt);
                }
                visit(stmt);
            }
            node.setBody(stmts);
            return null;
        }
        
        protected Object visitSwitchCase(SwitchCase node) {
            List<IStatement> stmts = new LinkedList<IStatement>();
            for (IStatement stmt : node.getConsequent()) {
                if (stmt instanceof FunctionDeclaration) {
                    funcDecls.add((FunctionDeclaration) stmt);
                } else {
                    stmts.add(stmt);
                }
                visit(stmt);
            }
            node.setConsequent(stmts);
            return null;
        }
    }

    class EliminatingNamedFunc extends ESTreeBaseVisitor<Object> {
        
        /*
         * For example...
         * 
         * a = function Hoge(a, b) {
         *     // something
         * }
         * 
         *  ->
         * a = function(a, b) {
         *     var Hoge = function(a, b) {
         *         // something
         *     }
         *     Hoge(a, b);
         * }
         * 
         */
        
        public void run(INode estree) {
            visit(estree);
        }
        
        protected Object visitFunctionExpression(FunctionExpression node) {
            super.visitFunctionExpression(node);
            if (node.getId() != null) {
                List<IStatement> outerFuncBody = new LinkedList<IStatement>();
                
                // create inner func
                FunctionExpression inner = new FunctionExpression(null, node.getParams(), node.getBody());
                
                // create var decl
                List<IVariableDeclarator> varDecl = new LinkedList<IVariableDeclarator>();
                varDecl.add(new VariableDeclarator(node.getId(), inner));
                outerFuncBody.add(new VariableDeclaration(varDecl));
                
                // create call exp
                List<IExpression> args = new LinkedList<IExpression>();
                for (IPattern ptn : node.getParams()) {
                    args.add(new Identifier(((Identifier) ptn).getName()));
                }
                CallExpression callExp = new CallExpression(node.getId(), args);
                outerFuncBody.add(new ExpressionStatement(callExp));
                
                node.setId(null);
                node.setBody(new BlockStatement(outerFuncBody));
            }
            return null;
        }
    }

}
