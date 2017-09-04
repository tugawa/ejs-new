package ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

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
        // TODO Auto-generated method stub
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
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitSwitchStatement(this);
    }

}
