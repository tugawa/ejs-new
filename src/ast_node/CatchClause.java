package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

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
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "CatchClause")
                .add(KEY_PARAM, param.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IPattern getParam() {
        // TODO Auto-generated method stub
        return param;
    }

    @Override
    public IBlockStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitCatchClause(this);
    }

}
