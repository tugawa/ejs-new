package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class MemberExpression extends Node implements IMemberExpression {

    IExpression object;
    IExpression property;
    boolean computed;

    public MemberExpression(IExpression object, IExpression property, boolean computed) {
        type = MEMBER_EXP;
        this.object = object;
        this.property = property;
        this.computed = computed;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "MemberExpression")
                .add(KEY_OBJECT, object.getEsTree())
                .add(KEY_PROPERTY, property.getEsTree())
                .add(KEY_COMPUTED, computed);
        return jb.build();
    }

    @Override
    public IExpression getObject() {
        return object;
    }

    @Override
    public IExpression getProperty() {
        return property;
    }

    @Override
    public boolean getComputed() {
        return computed;
    }

	@Override
	public void setObject(IExpression object) {
		this.object = object;
	}

	@Override
	public void setProperty(IExpression property) {
		this.property = property;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitMemberExpression(this);
	}

}
