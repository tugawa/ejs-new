/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

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
        return varDeclLeft;
    }

    @Override
    public IPattern getPatternLeft() {
        return patternLeft;
    }

    @Override
    public IExpression getRight() {
        return right;
    }

    @Override
    public IStatement getBody() {
        return body;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitForInStatement(this);
    }

}
