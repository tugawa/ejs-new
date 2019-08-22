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

public class ReturnStatement extends Node implements IReturnStatement {

    IExpression argument;

    public ReturnStatement(IExpression argument) {
        type = RETURN_STMT;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ReturnStatement");
        if (argument != null) {
            jb.add(KEY_ARGUMENT, argument.getEsTree());
        } else {
            jb.addNull(KEY_ARGUMENT);
        }
        return jb.build();
    }

    @Override
    public IExpression getArgument() {
        return argument;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitReturnStatement(this);
    }
}
