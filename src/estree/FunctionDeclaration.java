package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class FunctionDeclaration extends Node implements IFunctionDeclaration {

    IIdentifier id;
    List<IPattern> params;
    IBlockStatement body;

    public FunctionDeclaration(IIdentifier id, List<IPattern> params, IBlockStatement body) {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        type = FUNC_DECLARATION;
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
                .add(KEY_TYPE, "FunctionDeclaration")
                .add(KEY_ID, id.getEsTree())
                .add(KEY_PARAMS, paramsJb)
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
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
    public IIdentifier getId() {
        return id;
    }

	@Override
	public void setBody(IBlockStatement body) {
		this.body = body;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitFunctionDeclaration(this);
	}

}
