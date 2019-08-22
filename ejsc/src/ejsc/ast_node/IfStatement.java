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
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitIfStatement(this);
    }

}
