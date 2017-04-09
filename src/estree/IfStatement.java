package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

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
        return test;
    }

    @Override
    public IStatement getConsequent() {
        return consequent;
    }

    @Override
    public IStatement getAlternate() {
        return alternate;
    }

	@Override
	public void setTest(IExpression test) {
		this.test = test;
	}

	@Override
	public void setConsequent(IStatement consequent) {
		this.consequent = consequent;
	}

	@Override
	public void setAlternate(IStatement alternate) {
		this.alternate = alternate;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitIfStatement(this);
	}

}
