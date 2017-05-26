package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class EmptyStatement extends Node implements IEmptyStatement {

    public EmptyStatement() {
        type = EMPTY_STMT;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "EmptyStatement");
        return jb.build();
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        return visitor.visitEmptyStatement(this);
    }

}
