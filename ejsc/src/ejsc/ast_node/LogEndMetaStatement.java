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

import ejsc.ast_node.Node.*;

public class LogEndMetaStatement extends Node implements IStatement {

    public LogEndMetaStatement() {
        type = LOG_END_META_STMT;
    }

    @Override
    public JsonObject getEsTree() {
        return Json.createObjectBuilder().build();
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitLogEndMetaStatement(this);
    }

}
