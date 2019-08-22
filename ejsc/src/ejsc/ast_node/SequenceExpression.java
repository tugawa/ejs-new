/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package ejsc.ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

public class SequenceExpression extends Node implements ISequenceExpression {

    List<IExpression> expression;

    public SequenceExpression(List<IExpression> expr) {
        type = SEQUENCE_EXP;
        this.expression = expr;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "SequenceExpression");
        if (expression == null) {
            jb.addNull(KEY_EXPRESSIONS);
        } else {
            JsonArrayBuilder propertiesJb = Json.createArrayBuilder();
            for (IExpression property : expression) {
                propertiesJb.add(property.getEsTree());
            }
            jb.add(KEY_EXPRESSIONS, propertiesJb);
        }

        return jb.build();
    }

    @Override
    public List<IExpression> getExpression() {
        return expression;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitSequenceExpression(this);
    }
}
