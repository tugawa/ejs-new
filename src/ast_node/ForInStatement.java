package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class ForInStatement extends Node implements IForInStatement {

    public enum InitType {
        VAR_DECL,
        PATTERN
    }
    InitType initType;

    IVariableDeclaration varDeclLeft;
    IPattern patternLeft;
    IExpression right;
    IStatement body;

    String label = null;

    public ForInStatement(IVariableDeclaration left, IExpression right, IStatement body) {
        type = FOR_IN_STMT;
        initType = InitType.VAR_DECL;
        this.varDeclLeft = left;
        this.right = right;
        this.body = body;
    }

    public ForInStatement(IPattern left, IExpression right, IStatement body) {
        type = FOR_IN_STMT;
        initType = InitType.PATTERN;
        this.patternLeft = left;
        this.right = right;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ForInStatement");
        if (initType == InitType.VAR_DECL) {
            jb.add(KEY_LEFT, varDeclLeft.getEsTree());
        } else if (initType == InitType.PATTERN) {
            jb.add(KEY_LEFT, patternLeft.getEsTree());
        }else {
            jb.addNull(KEY_LEFT);
        }
        jb.add(KEY_RIGHT, right.getEsTree());
        jb.add(KEY_BODY, body.getEsTree());

        return jb.build();
    }
    
    public InitType getInitType() {
        return this.initType;
    }

    @Override
    public IVariableDeclaration getValDeclLeft() {
        // TODO Auto-generated method stub
        return varDeclLeft;
    }

    @Override
    public IPattern getPatternLeft() {
        // TODO Auto-generated method stub
        return patternLeft;
    }

    @Override
    public IExpression getRight() {
        // TODO Auto-generated method stub
        return right;
    }

    @Override
    public IStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitForInStatement(this);
    }

}
