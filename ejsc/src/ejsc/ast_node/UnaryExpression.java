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

import ejsc.ast_node.Node.*;

public class UnaryExpression extends Node implements IUnaryExpression {

    public enum UnaryOperator {
        MINUS("-"),
        PLUS("+"),
        NOT("!"),
        BIT_INV("~"),
        TYPEOF("typeof"),
        VOID("void"),
        DELETE("delete");

        String op;

        private UnaryOperator(String op) {
            this.op = op;
        }

        public String toString() {
            return op;
        }

        public static UnaryOperator getUnaryOperator(String op) {
            switch (op) {
            case "-":
                return MINUS;
            case "+":
                return PLUS;
            case "!":
                return NOT;
            case "~":
                return BIT_INV;
            case "typeof":
                return TYPEOF;
            case "void":
                return VOID;
            case "delete":
                return DELETE;
            default:
                return null;
            }
        }
    }

    UnaryOperator operator;
    boolean prefix;
    IExpression argument;

    public UnaryExpression(String operator, boolean prefix, IExpression argument) {
        type = UNARY_EXP;
        this.operator = UnaryOperator.getUnaryOperator(operator);
        this.prefix = prefix;
        this.argument = argument;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObject json = Json.createObjectBuilder()
                .add(KEY_TYPE, "UnaryExpression")
                .add(KEY_OPERATOR, operator.toString())
                .add(KEY_ARGUMENT, argument.getEsTree())
                .add(KEY_PREFIX, prefix)
                .build();
        return json;
    }

    @Override
    public String getOperator() {
        return operator.toString();
    }

    @Override
    public boolean getPrefix() {
        return prefix;
    }

    @Override
    public IExpression getArgument() {
        return argument;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitUnaryExpression(this);
    }
}
