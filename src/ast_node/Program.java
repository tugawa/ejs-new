package ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class Program extends Node implements Node.IProgram {

    List<IStatement> body;

    public Program(List<IStatement> body) {
        type = PROGRAM;
        this.body = body;
    }

    public List<IStatement> getBody() {
        return body;
    }
    
    @Override
    public void setBody(List<IStatement> body) {
        // TODO Auto-generated method stub
        this.body = body;
    }

    @Override
    public String toString() {
        String str = "Program(";
        for (int i = 0; i < body.size(); i++) {
            str += body.get(i).toString() + (i+1 < body.size() ? "," : "");
        }
        str += ")";
        return str;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonArrayBuilder bodyJsonBuilder = Json.createArrayBuilder();
        for (IStatement stmt : body) {
            bodyJsonBuilder.add(stmt.getEsTree());
        }
        JsonObject json = Json.createObjectBuilder()
                .add(KEY_TYPE, "Program")
                .add(KEY_BODY, bodyJsonBuilder)
                // .add(KEY_LOC, loc.getAstWithJson())
                .build();
        return json;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitProgram(this);
    }

    
}
