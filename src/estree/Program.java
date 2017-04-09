package estree;

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
                // .add(KEY_LOC, loc.getAstWithJson())
                .build();
        return json;
    }

	@Override
	public void setBody(List<IStatement> body) {
		this.body = body;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitProgram(this);
	}
}
