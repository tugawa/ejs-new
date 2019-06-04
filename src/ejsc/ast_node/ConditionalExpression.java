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

public class ConditionalExpression extends Node implements IConditionalExpression {

    IExpression test;
    IExpression alternate;
    IExpression consequent;

    public ConditionalExpression(IExpression test, IExpression consequent, IExpression alternate) {
        type = CONDITIONAL_EXP;
        this.test = test;
        this.consequent = consequent;
        this.alternate = alternate;
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
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitConditionalExpression(this);
    }

}
