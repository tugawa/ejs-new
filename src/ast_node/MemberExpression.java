package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

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
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "MemberExpression")
                .add(KEY_OBJECT, object.getEsTree())
                .add(KEY_PROPERTY, property.getEsTree())
                .add(KEY_COMPUTED, computed);
        return jb.build();
    }

    @Override
    public IExpression getObject() {
        // TODO Auto-generated method stub
        return object;
    }

    @Override
    public IExpression getProperty() {
        // TODO Auto-generated method stub
        return property;
    }

    @Override
    public boolean getComputed() {
        // TODO Auto-generated method stub
        return computed;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitMemberExpression(this);
    }

}
