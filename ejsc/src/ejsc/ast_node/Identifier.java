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

public class Identifier extends Node implements Node.IIdentifier {

    String name;

    public Identifier(String name) {
        type = IDENTIFIER;
        this.name = name;
    }

    public String toString() {
        return "Identifier(" + name + ")";
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add(KEY_TYPE, "Identifier")
                .add(KEY_NAME, name)
                ;
        return jsonBuilder.build();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitIdentifier(this);
    }
}
