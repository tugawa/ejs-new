/*
   ESTreeBaseVisitor.java

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

import ejsc.ast_node.Node.*;

public class ESTreeBaseVisitor<T> {
    
    protected T visitNode(Node node) {
        return null;
    }
    
    protected T visitProgram(Program node) {
        for (IStatement stmt : node.getBody()) {
            stmt.accept(this);
        }
        return visitNode(node);
    }
    
    protected T visitStatement(IStatement node) {
        return visitNode((Node) node);
    }
    
    protected T visitBlockStatement(BlockStatement node) {
        for (IStatement stmt : node.getBody()) {
            stmt.accept(this);
        }
        return visitStatement(node);
    }
    
    protected T visitFunction(Function node) {
        return visitNode((Node) node);
    }
    
    protected T visitFunctionDeclaration(FunctionDeclaration node) {
        for (IPattern ptn : node.getParams()) {
            ptn.accept(this);
        }
        return visitStatement(node);
    }
    
    protected T visitVariableDeclaration(VariableDeclaration node) {
        for (IVariableDeclarator vd : node.getDeclarations()) {
            vd.accept(this);
        }
        return visitStatement(node);
    }
    
    protected T visitVariableDeclarator(VariableDeclarator node) {
        node.getId().accept(this);
        if (node.getInit() != null) {
            node.getInit().accept(this);
        }
        return visitNode((Node) node);
    }

    protected T visitIfStatement(IfStatement node) {
        node.getTest().accept(this);
        node.getConsequent().accept(this);
        if (node.getAlternate() != null) {
            node.getAlternate().accept(this);
        }
        return visitStatement(node);
    }
    
    protected T visitForStatement(ForStatement node) {
        ForStatement.InitType initType = node.getInitType();
        if (initType != null) {
            if (initType == ForStatement.InitType.VAR_DECL) {
                node.getValDeclInit().accept(this);
            } else if (initType == ForStatement.InitType.EXPRESSION) {
                node.getExpInit().accept(this);
            }
        }
        if (node.getLabel() != null)
            node.getLabel();
        if (node.getTest() != null)
            node.getTest().accept(this);
        if (node.getUpdate() != null)
            node.getUpdate().accept(this);
        if (node.getBody() != null)
            node.getBody().accept(this);
        return visitStatement(node);
    }
    
    protected T visitWhileStatement(WhileStatement node) {
        node.getTest().accept(this);
        if (node.getLabel() != null)
            node.getLabel();
        if (node.getBody() != null)
            node.getBody().accept(this);
        return visitStatement(node);
    }
    
    protected T visitDoWhileStatement(DoWhileStatement node) {
        node.getTest().accept(this);
        if (node.getLabel() != null)
            node.getLabel();
        if (node.getBody() != null)
            node.getBody().accept(this);
        return visitStatement(node);
    }
    
    protected T visitForInStatement(ForInStatement node) {
        if (node.getLabel() != null)
            node.getLabel();
        ForInStatement.InitType initType = node.getInitType();
        if (initType == ForInStatement.InitType.PATTERN)
            node.getPatternLeft().accept(this);
        else if (initType == ForInStatement.InitType.VAR_DECL)
            node.getValDeclLeft().accept(this);
        node.getRight().accept(this);
        node.getBody().accept(this);
        return visitStatement(node);
    }
    
    protected T visitSwitchStatement(SwitchStatement node) {
        node.getDiscriminant().accept(this);
        for (ISwitchCase switchCase : node.getCases()) {
            switchCase.accept(this);
        }
        return visitStatement(node);
    }
    
    protected T visitSwitchCase(SwitchCase node) {
        if (node.getTest() != null)
            node.getTest().accept(this);
        for (IStatement stmt : node.getConsequent())
            stmt.accept(this);
        return visitNode((Node) node);
    }
    
    protected T visitBreakStatement(BreakStatement node) {
        if (node.getLabel() != null)
            node.getLabel().accept(this);
        return visitStatement(node);
    }
    
    protected T visitContinueStatement(ContinueStatement node) {
        if (node.getLabel() != null)
            node.getLabel().accept(this);
        return visitStatement(node);
    }
    
    protected T visitReturnStatement(ReturnStatement node) {
        if (node.getArgument() != null)
            node.getArgument().accept(this);
        return visitStatement(node);
    }
    
    protected T visitTryStatement(TryStatement node) {
        node.getBlock().accept(this);
        if (node.getHandler() != null)
            node.getHandler().accept(this);
        if (node.getFinalizer() != null)
            node.getFinalizer().accept(this);
        return visitStatement(node);
    }
    
    protected T visitCatchClause(CatchClause node) {
        node.getParam().accept(this);
        node.getBody().accept(this);
        return visitNode((Node) node);
    }
    
    protected T visitThrowStatement(ThrowStatement node) {
        node.getArgument().accept(this);
        return visitStatement(node);
    }
    
    protected T visitLabeledStatement(LabeledStatement node) {
        node.getLabel().accept(this);
        node.getBody().accept(this);
        return visitStatement(node);
    }
    
    protected T visitWithStatement(WithStatement node) {
        node.getObject().accept(this);
        node.getBody().accept(this);
        return visitStatement(node);
    }
    
    protected T visitExpressionStatement(ExpressionStatement node) {
        node.getExpression().accept(this);
        return visitStatement(node);
    }
    
    protected T visitEmptyStatement(EmptyStatement node) {
        return visitStatement(node);
    }
    
    protected T visitDebuggerStatement(DebuggerStatement node) {
        return visitStatement(node);
    }
    
    protected T visitExpression(IExpression node) {
        return visitNode((Node) node);
    }
    
    protected T visitFunctionExpression(FunctionExpression node) {
        if (node.getId() != null)
            node.getId().accept(this);
        if (node.getParams() != null) {
            for (IPattern ptn : node.getParams())
                ptn.accept(this);
        }
        node.getBody().accept(this);
        return visitExpression(node);
    }
    
    protected T visitArrayExpression(ArrayExpression node) {
        for (IExpression exp : node.getElements())
            exp.accept(this);
        return visitExpression(node);
    }
    
    protected T visitObjectExpression(ObjectExpression node) {
        for (IProperty prop : node.getProperties())
            prop.accept(this);
        return visitExpression(node);
    }
    
    protected T visitProperty(Property node) {
        Property.KeyType keyType = node.getKeyType();
        if (keyType == Property.KeyType.IDENTIFIER)
            node.getIdentifierKey().accept(this);
        else if (keyType == Property.KeyType.LITERAL)
            node.getLiteralKey().accept(this);
        node.getKind();
        node.getValue().accept(this);
        return visitNode((Node) node);
    }
    
    protected T visitSequenceExpression(SequenceExpression node) {
        for (IExpression exp : node.getExpression())
            exp.accept(this);
        return visitExpression(node);
    }
    
    protected T visitThisExpression(ThisExpression node) {
        return visitExpression(node);
    }
    
    protected T visitAssignmentExpression(AssignmentExpression node) {
        node.getOperator();
        AssignmentExpression.LeftNodeType leftType = node.getLeftNodeType();
        if (leftType == AssignmentExpression.LeftNodeType.EXPRESSION) 
            node.getExpressionLeft().accept(this);
        else if (leftType == AssignmentExpression.LeftNodeType.PATTERN)
            node.getPatternLeft().accept(this);
        node.getExpressionLeft().accept(this);
        node.getRight().accept(this);
        return visitExpression(node);
    }
    
    protected T visitUnaryExpression(UnaryExpression node) {
        node.getOperator();
        node.getArgument().accept(this);
        return visitExpression(node);
    }
    
    protected T visitBinaryExpression(BinaryExpression node) {
        node.getOperator();
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return visitExpression(node);
    }
    
    protected T visitConditionalExpression(ConditionalExpression node) {
        node.getTest().accept(this);
        node.getConsequent().accept(this);
        node.getAlternate().accept(this);
        return visitExpression(node);
    }
    
    protected T visitUpdateExpression(UpdateExpression node) {
        node.getOperator();
        node.getPrefix();
        node.getArgument().accept(this);
        return visitExpression(node);
    }
    
    protected T visitLogicalExpression(LogicalExpression node) {
        node.getOperator();
        node.getLeft().accept(this);
        node.getRight().accept(this);
        return visitExpression(node);
    }
    
    protected T visitCallExpression(CallExpression node) {
        node.getCallee().accept(this);
        if (node.getArguments() != null) {
            for (IExpression exp : node.getArguments())
                exp.accept(this);
        }
        return visitExpression(node);
    }
    
    protected T visitNewExpression(NewExpression node) {
        node.getCallee().accept(this);
        if (node.getArguments() != null) {
            for (IExpression exp : node.getArguments())
                exp.accept(this);
        }
        return visitExpression(node);
    }
    
    protected T visitMemberExpression(MemberExpression node) {
        node.getObject().accept(this);
        node.getProperty().accept(this);
        return visitExpression(node);
    }
    
    protected T visitLiteral(Literal node) {
        return visitExpression(node);
    }
    
    protected T visitIdentifier(Identifier node) {
        return visitExpression(node);
    }
    
    protected T visit(INode node) {
        return (T) node.accept(this);
    }
}
