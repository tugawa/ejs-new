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

public class ThrowStatement extends Node implements IThrowStatement {

    IExpression argument;

    public ThrowStatement(IExpression argument) {
        type = THROW_STMT;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ThrowStatement")
                .add(KEY_ARGUMENT, argument.getEsTree());
        return jb.build();
    }

    @Override
    public IExpression getArgument() {
        return argument;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitThrowStatement(this);
    }

}
