package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class ConditionalExpression extends Node implements IConditionalExpression {

    IExpression test;
    IExpression alternate;
    IExpression consequent;

    public ConditionalExpression(IExpression test, IExpression alternate, IExpression consequent) {
        type = CONDITIONAL_EXP;
        this.test = test;
        this.alternate = alternate;
        this.consequent = consequent;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ConditionalExpression")
                .add(KEY_TEST, test.getEsTree())
                .add(KEY_CONSEQUENT, consequent.getEsTree())
                .add(KEY_ALTERNATE, alternate.getEsTree());

        return jb.build();
    }

    @Override
    public IExpression getTest() {
        return test;
    }

    @Override
    public IExpression getAlternate() {
        return alternate;
    }

    @Override
    public IExpression getConsequent() {
        return consequent;
    }

	@Override
	public void setTest(IExpression test) {
		this.test = test;
	}

	@Override
	public void setAlternate(IExpression alternate) {
		this.alternate = alternate;
	}

	@Override
	public void setConsequent(IExpression consequent) {
		this.consequent = consequent;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitConditionalExpression(this);
	}

}
