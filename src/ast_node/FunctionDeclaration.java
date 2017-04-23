package ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

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
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return params;
    }

    @Override
    public IBlockStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    @Override
    public IIdentifier getId() {
        // TODO Auto-generated method stub
        return id;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitFunctionDeclaration(this);
    }

    @Override
    public void setId(IIdentifier id) {
        // TODO Auto-generated method stub
        this.id = id;
    }

    @Override
    public void setParams(List<IPattern> params) {
        // TODO Auto-generated method stub
        this.params = params;
    }

    @Override
    public void setBody(IBlockStatement body) {
        // TODO Auto-generated method stub
        this.body = body;
    }

}
