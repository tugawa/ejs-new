package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class ReturnStatement extends Node implements IReturnStatement {

    IExpression argument;

    public ReturnStatement(IExpression argument) {
        type = RETURN_STMT;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ReturnStatement");
        if (argument != null) {
            jb.add(KEY_ARGUMENT, argument.getEsTree());
        } else {
            jb.addNull(KEY_ARGUMENT);
        }
        return jb.build();
    }

    @Override
    public IExpression getArgument() {
        return argument;
    }

	@Override
	public void setArgument(IExpression argument) {
		this.argument = argument;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitReturnStatement(this);
	}

}
