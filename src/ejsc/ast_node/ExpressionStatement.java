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

public class ExpressionStatement extends Node implements IExpressionStatement {

    IExpression expression;

    public ExpressionStatement(IExpression expression) {
        type = EXP_STMT;
        this.expression = expression;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ExpressionStatement")
                .add(KEY_EXPRESSION, expression.getEsTree());
        return jb.build();
    }

    @Override
    public IExpression getExpression() {
        return expression;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitExpressionStatement(this);
    }

}
