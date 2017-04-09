package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class SwitchStatement extends Node implements ISwitchStatement {

    IExpression discriminant;
    List<ISwitchCase> cases;

    public SwitchStatement(IExpression discriminant, List<ISwitchCase> cases) {
        type = SWITCH_STMT;
        this.discriminant = discriminant;
        this.cases = cases;
    }

    public IExpression getDiscriminant() {
        return discriminant;
    }

    public List<ISwitchCase> getCases() {
        return cases;
    }

    @Override
    public JsonObject getEsTree() {
        JsonArrayBuilder casesJb = Json.createArrayBuilder();
        for (ISwitchCase sc : cases) {
            casesJb.add(sc.getEsTree());
        }
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "SwitchStatement")
                .add(KEY_DISCRIMINANT, discriminant.getEsTree())
                .add(KEY_CASES, casesJb);
        return jb.build();
    }

	@Override
	public void setDiscriminant(IExpression discriminant) {
		this.discriminant = discriminant;
	}

	@Override
	public void setCases(List<ISwitchCase> cases) {
		this.cases = cases;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitSwitchStatement(this);
	}

}
