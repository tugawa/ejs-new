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

public class ContinueStatement extends Node implements IContinueStatement {

    IIdentifier label;

    public ContinueStatement(IIdentifier label) {
        type = CONTINUE_STMT;
        this.label = label;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ContinueStatement");
        if (label != null) {
            jb.add(KEY_LABEL, label.getEsTree());
        } else {
            jb.addNull(KEY_LABEL);
        }
        return jb.build();
    }

    @Override
    public IIdentifier getLabel() {
        return label;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitContinueStatement(this);
    }

}
