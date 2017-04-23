package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class ThrowStatement extends Node implements IThrowStatement {

    IExpression argument;

    public ThrowStatement(IExpression argument) {
        type = THROW_STMT;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ThrowStatement")
                .add(KEY_ARGUMENT, argument.getEsTree());
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
        return visitor.visitThrowStatement(this);
    }

}
