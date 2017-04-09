package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class BreakStatement extends Node implements IBreakStatement {

    IIdentifier label;

    public BreakStatement(IIdentifier label) {
        type = BREAK_STMT;
        this.label = label;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "BreakStatement");
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
		return visitor.visitBreakStatement(this);
	}

}
