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

public class EmptyStatement extends Node implements IEmptyStatement {

    public EmptyStatement() {
        type = EMPTY_STMT;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "EmptyStatement");
        return jb.build();
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitEmptyStatement(this);
    }

}
