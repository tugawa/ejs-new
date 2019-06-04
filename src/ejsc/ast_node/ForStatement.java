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

public class ForStatement extends Node implements IForStatement {

    public enum InitType {
        VAR_DECL,
        EXPRESSION
    }
    InitType initType = null;

    IVariableDeclaration varDeclInit;
    IExpression expressionInit;
    IExpression test;
    IExpression update;
    IStatement body;

    String label;

    public ForStatement(IVariableDeclaration init, IExpression test, IExpression update, IStatement body) {
        type = FOR_STMT;
        if (init != null) {
            initType = InitType.VAR_DECL;
            this.varDeclInit = init;
        }
        this.test = test;
        this.update = update;
        this.body = body;
    }

    public ForStatement(IExpression init, IExpression test, IExpression update, IStatement body) {
        type = FOR_STMT;
        if (init != null) {
            initType = InitType.EXPRESSION;
            this.expressionInit = init;
        }
        this.test = test;
        this.update = update;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ForStatement");
        if (initType == InitType.VAR_DECL) {
            jb.add(KEY_INIT, varDeclInit.getEsTree());
        } else if (initType == InitType.EXPRESSION) {
            jb.add(KEY_INIT, expressionInit.getEsTree());
        }else {
            jb.addNull(KEY_INIT);
        }

        if (test == null) {
            jb.addNull(KEY_TEST);
        } else {
            jb.add(KEY_TEST, test.getEsTree());
        }

        if (update == null) {
            jb.addNull(KEY_UPDATE);
        } else {
            jb.add(KEY_UPDATE, update.getEsTree());
        }

        if (body == null) {
            jb.addNull(KEY_BODY);
        } else {
            jb.add(KEY_BODY, body.getEsTree());
        }

        return jb.build();
    }

    public InitType getInitType() {
        return initType;
    }

    @Override
    public IVariableDeclaration getValDeclInit() {
        return varDeclInit;
    }

    @Override
    public IExpression getExpInit() {
        return expressionInit;
    }

    @Override
    public IExpression getTest() {
        return test;
    }

    @Override
    public IExpression getUpdate() {
        return update;
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
        return visitor.visitForStatement(this);
    }

}
