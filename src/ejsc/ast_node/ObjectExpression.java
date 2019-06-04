/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

public class ObjectExpression extends Node implements IObjectExpression {

    List<IProperty> properties;

    public ObjectExpression(List<IProperty> properties) {
        type = OBJECT_EXP;
        this.properties = properties;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ObjectExpression");
        if (properties == null) {
            jb.addNull(KEY_ELEMENTS);
        } else {
            JsonArrayBuilder propertiesJb = Json.createArrayBuilder();
            for (IProperty property : properties) {
                propertiesJb.add(property.getEsTree());
            }
            jb.add(KEY_PROPERTIES, propertiesJb);
        }

        return jb.build();
    }

    @Override
    public List<IProperty> getProperties() {
        return properties;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitObjectExpression(this);
    }

}
