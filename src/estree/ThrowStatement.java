package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class ThrowStatement extends Node implements IThrowStatement {

    IExpression argument;

    public ThrowStatement(IExpression argument) {
        type = THROW_STMT;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ThrowStatement")
                .add(KEY_ARGUMENT, argument.getEsTree());
        return jb.build();
    }

    @Override
    public IExpression getArgument() {
        return argument;
    }

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitThrowStatement(this);
	}

}
