package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class ContinueStatement extends Node implements IContinueStatement {

    IIdentifier label;

    public ContinueStatement(IIdentifier label) {
        type = CONTINUE_STMT;
        this.label = label;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ContinueStatement");
        if (label != null) {
            jb.add(KEY_LABEL, label.getEsTree());
        } else {
            jb.addNull(KEY_LABEL);
        }
        return jb.build();
    }

    @Override
    public IIdentifier getLabel() {
        // TODO Auto-generated method stub
        return label;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitContinueStatement(this);
    }

}
