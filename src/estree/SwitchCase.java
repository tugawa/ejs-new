package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class SwitchCase extends Node implements ISwitchCase {

    IExpression test;
    List<IStatement> consequent;

    public SwitchCase(IExpression test, List<IStatement> consequent) {
        type = SWITCH_CASE;
        this.test = test;
        this.consequent = consequent;
    }

    @Override
    public JsonObject getEsTree() {
        JsonArrayBuilder consequentJb = Json.createArrayBuilder();
        for (IStatement c : consequent) {
            consequentJb.add(c.getEsTree());
        }
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "SwitchCase");
        if (test != null) {
            jb.add(KEY_TEST, test.getEsTree());
        } else {
            jb.addNull(KEY_TEST);
        }
        jb.add(KEY_CONSEQUENT, consequentJb);
        return jb.build();
    }

    @Override
    public IExpression getTest() {
        return test;
    }

    @Override
    public List<IStatement> getConsequent() {
        return consequent;
    }

	@Override
	public void setTest(IExpression test) {
		this.test = test;
	}

	@Override
	public void setConsequent(List<IStatement> consequent) {
		this.consequent = consequent;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitSwitchCase(this);
	}

}
