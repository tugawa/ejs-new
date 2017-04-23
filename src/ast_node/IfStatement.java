package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class IfStatement extends Node implements IIfStatement {

    private IExpression test;
    private IStatement consequent;
    private IStatement alternate;

    public IfStatement(IExpression test, IStatement consequent, IStatement alternate) {
        type = IF_STMT;
        this.test = test;
        this.consequent = consequent;
        this.alternate = alternate;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add(KEY_TYPE, "IfStatement")
                .add(KEY_TEST, test.getEsTree())
                .add(KEY_CONSEQUENT, consequent.getEsTree());
        if (alternate != null) {
            jsonBuilder.add(KEY_ALTERNATE, alternate.getEsTree());
        } else {
            jsonBuilder.addNull(KEY_ALTERNATE);
        }
        return jsonBuilder.build();
    }

    @Override
    public IExpression getTest() {
        // TODO Auto-generated method stub
        return test;
    }

    @Override
    public IStatement getConsequent() {
        // TODO Auto-generated method stub
        return consequent;
    }

    @Override
    public IStatement getAlternate() {
        // TODO Auto-generated method stub
        return alternate;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitIfStatement(this);
    }

}
