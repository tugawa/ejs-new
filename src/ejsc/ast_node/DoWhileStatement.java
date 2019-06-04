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

public class DoWhileStatement extends Node implements IDoWhileStatement {

    IStatement body;
    IExpression test;

    String label = null;

    public DoWhileStatement(IStatement body, IExpression test) {
        type = DO_WHILE_STMT;
        this.test = test;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "DoWhileStatement")
                .add(KEY_BODY, body.getEsTree())
                .add(KEY_TEST, test.getEsTree());
        return jb.build();
    }

    @Override
    public IStatement getBody() {
        return body;
    }

    @Override
    public IExpression getTest() {
        return test;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitDoWhileStatement(this);
    }

}
