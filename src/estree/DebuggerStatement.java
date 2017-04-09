package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class DebuggerStatement extends Node implements IDebuggerStatement {

    public DebuggerStatement() {
        type = DEBUGGER_STMT;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "DebuggerStatement");
        return jb.build();
    }

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitDebuggerStatement(this);
	}

}
