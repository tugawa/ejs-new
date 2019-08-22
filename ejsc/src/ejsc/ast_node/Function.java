/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class Function extends Node implements Node.IFunction {

    protected IIdentifier id;
    protected List<IPattern> params;
    protected IBlockStatement body;
    protected boolean logging;

    public Function(IIdentifier id, List<IPattern> params, IBlockStatement body) {
        type = FUNCTION;
        this.id = id;
        this.params = params;
        this.body = body;
        this.logging = false;
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
    public boolean getLogging() {
        return logging;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitFunction(this);
    }

    @Override
    public void setId(IIdentifier id) {
        this.id = id;
    }

    @Override
    public void setParams(List<IPattern> params) {
        this.params = params;
    }

    @Override
    public void setBody(IBlockStatement body) {
        this.body = body;
    }

}
