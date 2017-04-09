package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class CatchClause extends Node implements ICatchClause {

    IPattern param;
    IBlockStatement body;

    public CatchClause(IPattern param, IBlockStatement body) {
        type = CATCH_CLAUSE;
        this.param = param;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "CatchClause")
                .add(KEY_PARAM, param.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IPattern getParam() {
        return param;
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
		return visitor.visitCatchClause(this);
	}

}
