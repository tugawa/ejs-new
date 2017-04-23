package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ast_node.Node.*;

public class ExpressionStatement extends Node implements IExpressionStatement {

    IExpression expression;

    public ExpressionStatement(IExpression expression) {
        type = EXP_STMT;
        this.expression = expression;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ExpressionStatement")
                .add(KEY_EXPRESSION, expression.getEsTree());
        return jb.build();
    }

    @Override
    public IExpression getExpression() {
        // TODO Auto-generated method stub
        return expression;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitExpressionStatement(this);
    }

}
