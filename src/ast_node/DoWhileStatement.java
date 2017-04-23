package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class DoWhileStatement extends Node implements IDoWhileStatement {

    IStatement body;
    IExpression test;

    String label = null;

    public DoWhileStatement(IStatement body, IExpression test) {
        type = DO_WHILE_STMT;
        this.test = test;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "DoWhileStatement")
                .add(KEY_BODY, body.getEsTree())
                .add(KEY_TEST, test.getEsTree());
        return jb.build();
    }

    @Override
    public IStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    @Override
    public IExpression getTest() {
        // TODO Auto-generated method stub
        return test;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitDoWhileStatement(this);
    }

}
