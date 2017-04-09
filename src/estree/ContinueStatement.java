package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class ContinueStatement extends Node implements IContinueStatement {

    IIdentifier label;

    public ContinueStatement(IIdentifier label) {
        type = CONTINUE_STMT;
        this.label = label;
    }

    @Override
    public JsonObject getEsTree() {
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
        return label;
    }

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitContinueStatement(this);
	}

}
