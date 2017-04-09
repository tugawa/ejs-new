package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class BlockStatement extends Node implements IBlockStatement {

    List<IStatement> body;

    public BlockStatement(List<IStatement> body) {
        type = BLOCK_STMT;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonArrayBuilder bodyJb = Json.createArrayBuilder();
        for (IStatement stmt : body) {
            bodyJb.add(stmt.getEsTree());
        }
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "BlockStatement")
                .add(KEY_BODY, bodyJb);
        return jb.build();
    }

    @Override
    public List<IStatement> getBody() {
        return body;
    }

	@Override
	public void setBody(List<IStatement> body) {
		this.body = body;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitBlockStatement(this);
	}

}
