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

public class Program extends Node implements Node.IProgram {

    List<IStatement> body;
    boolean logging = false;

    public Program(List<IStatement> body, boolean logging) {
        type = PROGRAM;
        this.body = body;
        this.logging = logging;
    }

    public List<IStatement> getBody() {
        return body;
    }

    public boolean getLogging() {
        return logging;
    }

    @Override
    public void setBody(List<IStatement> body) {
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
        JsonArrayBuilder bodyJsonBuilder = Json.createArrayBuilder();
        for (IStatement stmt : body) {
            bodyJsonBuilder.add(stmt.getEsTree());
        }
        JsonObject json = Json.createObjectBuilder()
                .add(KEY_TYPE, "Program")
                .add(KEY_BODY, bodyJsonBuilder)
                .build();
        return json;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitProgram(this);
    }
}
