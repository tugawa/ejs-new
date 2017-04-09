package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class WhileStatement extends Node implements IWhileStatement {

    IExpression test;
    IStatement body;

    String label = null;

    public WhileStatement(IExpression test, IStatement body) {
        type = WHILE_STMT;
        this.test = test;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "WhileStatement")
                .add(KEY_TEST, test.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IExpression getTest() {
        return test;
    }

    @Override
    public IStatement getBody() {
        return body;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

	@Override
	public void setBody(IStatement body) {
		this.body = body;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitWhileStatement(this);
	}

}
