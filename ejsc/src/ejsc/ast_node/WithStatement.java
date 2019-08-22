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

public class WithStatement extends Node implements IWithStatement {

    IExpression object;
    IStatement body;

    public WithStatement(IExpression object, IStatement body) {
        type = WITH_STMT;
        this.object = object;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "WithStatement")
                .add(KEY_OBJECT, object.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IExpression getObject() {
        return object;
    }

    @Override
    public IStatement getBody() {
        return body;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitWithStatement(this);
    }
}
