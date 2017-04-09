package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class ThisExpression extends Node implements IThisExpression {

    public ThisExpression() {
        type = THIS_EXP;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ThisExpression");
        return jb.build();
    }

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitThisExpression(this);
	}

}
