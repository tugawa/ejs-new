/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

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
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add(KEY_TYPE, "VariableDeclarator")
                .add(KEY_ID, id.getEsTree());
        if (init != null) {
            jsonBuilder.add(KEY_INIT, init.getEsTree());
        } else {
            jsonBuilder.addNull(KEY_INIT);
        }
        return jsonBuilder.build();
    }

    @Override
    public IPattern getId() {
        return id;
    }

    @Override
    public IExpression getInit() {
        return init;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitVariableDeclarator(this);
    }
}
