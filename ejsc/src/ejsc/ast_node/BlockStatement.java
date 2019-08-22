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

public class BlockStatement extends Node implements IBlockStatement {

    List<IStatement> body;

    public BlockStatement(List<IStatement> body) {
        type = BLOCK_STMT;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonArrayBuilder bodyJb = Json.createArrayBuilder();
        for (IStatement stmt : body) {
            bodyJb.add(stmt.getEsTree());
        }
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "BlockStatement")
                .add(KEY_BODY, bodyJb);
        return jb.build();
    }

    @Override
    public List<IStatement> getBody() {
        return body;
    }

    @Override
    public void setBody(List<IStatement> body) {
        this.body = body;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitBlockStatement(this);
    }



}
