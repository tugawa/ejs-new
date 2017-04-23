package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

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
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "LabeledStatement")
                .add(KEY_LABEL, label.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IIdentifier getLabel() {
        // TODO Auto-generated method stub
        return label;
    }

    @Override
    public IStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitLabeledStatement(this);
    }

}
