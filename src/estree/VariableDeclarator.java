package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class VariableDeclarator extends Node implements IVariableDeclarator {

    IIdentifier id;
    IExpression init;

    public VariableDeclarator(IIdentifier id, IExpression init) {
        type = VAR_DECLARATOR;
        this.id = id;
        this.init = init;
    }

    @Override
    public String toString() {
        String str = "VariableDeclarator(";
        if (init != null) {
            str += init.toString();
        }
        str += ")";
        return str;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add(KEY_TYPE, "VariableDeclarator")
                .add(KEY_ID, id.getEsTree());
        if (init != null) {
            jsonBuilder.add(KEY_INIT, init.getEsTree());
        } else {
            jsonBuilder.addNull(KEY_INIT);
        }
        return jsonBuilder.build();
    }

    @Override
    public IIdentifier getId() {
        return id;
    }

    @Override
    public IExpression getInit() {
        return init;
    }

	@Override
	public void setInit(IExpression init) {
		this.init = init;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitVariableDeclarator(this);
	}
}
