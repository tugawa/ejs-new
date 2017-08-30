package ast_node;

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
        // TODO Auto-generated method stub
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add(KEY_TYPE, "Identifier")
                .add(KEY_NAME, name)
                //.add(KEY_LOC, loc.getAstWithJson())
                ;
        return jsonBuilder.build();
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return name;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitIdentifier(this);
    }
}
