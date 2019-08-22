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
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitMemberExpression(this);
    }

}
