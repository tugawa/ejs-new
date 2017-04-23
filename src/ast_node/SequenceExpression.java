package ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class SequenceExpression extends Node implements ISequenceExpression {

    List<IExpression> expression;

    public SequenceExpression(List<IExpression> expr) {
        type = SEQUENCE_EXP;
        this.expression = expr;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "SequenceExpression");
        if (expression == null) {
            jb.addNull(KEY_EXPRESSIONS);
        } else {
            JsonArrayBuilder propertiesJb = Json.createArrayBuilder();
            for (IExpression property : expression) {
                propertiesJb.add(property.getEsTree());
            }
            jb.add(KEY_EXPRESSIONS, propertiesJb);
        }

        return jb.build();
    }

    @Override
    public List<IExpression> getExpression() {
        // TODO Auto-generated method stub
        return expression;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitSequenceExpression(this);
    }
}
