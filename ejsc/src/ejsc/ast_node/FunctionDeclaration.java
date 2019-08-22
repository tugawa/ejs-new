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

import ejsc.ast_node.Node.*;

public class FunctionDeclaration extends Node implements IFunctionDeclaration {

    IIdentifier id;
    List<IPattern> params;
    IBlockStatement body;
    boolean logging;

    public FunctionDeclaration(IIdentifier id, List<IPattern> params, IBlockStatement body, boolean logging) {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        type = FUNC_DECLARATION;
        this.id = id;
        this.params = params;
        this.body = body;
        this.logging = logging;
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
    public boolean getLogging() {
        return logging;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitFunctionDeclaration(this);
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
