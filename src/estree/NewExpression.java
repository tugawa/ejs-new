package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class NewExpression extends Node implements INewExpression {

    IExpression callee;
    List<IExpression> arguments;

    public NewExpression(IExpression callee, List<IExpression> arguments) {
        type = NEW_EXP;
        this.callee = callee;
        this.arguments = arguments;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "NewExpression")
                .add(KEY_CALLEE, callee.getEsTree());

        if (arguments == null || arguments.size() == 0) {
            JsonArrayBuilder ja = Json.createArrayBuilder();
            jb.add(KEY_ARGUMENTS, ja);
        } else {
            JsonArrayBuilder ja = Json.createArrayBuilder();
            for (IExpression arg : arguments) {
                ja.add(arg.getEsTree());
            }
            jb.add(KEY_ARGUMENTS, ja);
        }

        return jb.build();
    }

    @Override
    public IExpression getCallee() {
        return callee;
    }

    @Override
    public List<IExpression> getArguments() {
        return arguments;
    }

	@Override
	public void setCallee(IExpression callee) {
		this.callee = callee;
	}

	@Override
	public void setArguments(List<IExpression> arguments) {
		this.arguments = arguments;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitNewExpression(this);
	}

}
