package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Function extends Node implements Node.IFunction {

    protected IIdentifier id;
    protected List<IPattern> params;
    protected IBlockStatement body;

    public Function(IIdentifier id, List<IPattern> params, IBlockStatement body) {
        type = FUNCTION;
        this.id = id;
        this.params = params;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonArrayBuilder paramsJb = Json.createArrayBuilder();
        for (IPattern param : params) {
            paramsJb.add(param.getEsTree());
        }
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "Function")
                .add(KEY_PARAMS, paramsJb)
                .add(KEY_BODY, body.getEsTree());
        if (id != null) {
            jb.add(KEY_ID, id.getEsTree());
        } else {
            jb.addNull(KEY_ID);
        }
        return jb.build();
    }

    @Override
    public IIdentifier getId() {
        return id;
    }

    @Override
    public List<IPattern> getParams() {
        return params;
    }

    @Override
    public IBlockStatement getBody() {
        return body;
    }

	@Override
	public void setBody(IBlockStatement body) {
		this.body = body;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitFunction(this);
	}

}
