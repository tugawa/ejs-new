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

public class UpdateExpression extends Node implements IUpdateExpression {

    public enum UpdateOperator {
        INC("++"),
        DEC("--");

        String op;

        private UpdateOperator(String op) {
            this.op = op;
        }

        public String toString() {
            return op;
        }

        public static UpdateOperator getUpdateOperator(String op) {
            switch (op) {
            case "++":
                return INC;
            case "--":
                return DEC;
            default:
                return null;
            }
        }
    }

    UpdateOperator operator;
    boolean prefix;
    IExpression argument;

    public UpdateExpression(String operator, boolean prefix, IExpression argument) {
        type = UPDATE_EXP;
        this.operator = UpdateOperator.getUpdateOperator(operator);
        this.prefix = prefix;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "UpdateExpression")
                .add(KEY_OPERATOR, operator.toString())
                .add(KEY_ARGUMENT, argument.getEsTree())
                .add(KEY_PREFIX, prefix);
        return jb.build();
    }

    @Override
    public String getOperator() {
        return operator.toString();
    }

    @Override
    public IExpression getArgument() {
        return argument;
    }

    @Override
    public boolean getPrefix() {
        return prefix;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitUpdateExpression(this);
    }
}
