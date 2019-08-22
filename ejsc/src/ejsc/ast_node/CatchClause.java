/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

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
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitCatchClause(this);
    }

}
