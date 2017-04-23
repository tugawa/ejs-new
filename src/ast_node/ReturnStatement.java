package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class ReturnStatement extends Node implements IReturnStatement {

    IExpression argument;

    public ReturnStatement(IExpression argument) {
        type = RETURN_STMT;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return argument;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitReturnStatement(this);
    }

}
