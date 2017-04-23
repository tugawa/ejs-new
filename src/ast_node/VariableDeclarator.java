package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class VariableDeclarator extends Node implements IVariableDeclarator {

    IPattern id;
    IExpression init;

    public VariableDeclarator(IPattern id, IExpression init) {
        type = VAR_DECLARATOR;
        this.id = id;
        this.init = init;
    }

    @Override
    public String toString() {
        String str = "VariableDeclarator(";
        if (init != null) {
            str += init.toString();
        }
        str += ")";
        return str;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add(KEY_TYPE, "VariableDeclarator")
                .add(KEY_ID, id.getEsTree());
        // .add(KEY_LOC, loc.getAstWithJson());
        if (init != null) {
            jsonBuilder.add(KEY_INIT, init.getEsTree());
        } else {
            jsonBuilder.addNull(KEY_INIT);
        }
        return jsonBuilder.build();
    }

    @Override
    public IPattern getId() {
        // TODO Auto-generated method stub
        return id;
    }

    @Override
    public IExpression getInit() {
        // TODO Auto-generated method stub
        return init;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitVariableDeclarator(this);
    }
}
