package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class SequenceExpression extends Node implements ISequenceExpression {

    List<IExpression> expression;

    public SequenceExpression(List<IExpression> expr) {
        type = SEQUENCE_EXP;
        this.expression = expr;
    }

    @Override
    public JsonObject getEsTree() {
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
        return expression;
    }

	@Override
	public void setExpression(List<IExpression> expressions) {
		this.expression = expressions;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitSequenceExpression(this);
	}
}
