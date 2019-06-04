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

public class LabeledStatement extends Node implements ILabeledStatement {

    IIdentifier label;
    IStatement body;

    public LabeledStatement(IIdentifier label, IStatement body) {
        type = LABELED_STMT;
        this.label = label;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "LabeledStatement")
                .add(KEY_LABEL, label.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IIdentifier getLabel() {
        return label;
    }

    @Override
    public IStatement getBody() {
        return body;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitLabeledStatement(this);
    }

}
