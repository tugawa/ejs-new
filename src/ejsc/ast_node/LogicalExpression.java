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

public class LogicalExpression extends Node implements ILogicalExpression {

    public enum LogicalOperator {
        OR,
        AND;

        public String toString() {
            switch (this) {
            case OR:    return "||";
            case AND:   return "&&";
            }
            return null;
        }
    }

    LogicalOperator operator;
    IExpression left;
    IExpression right;

    public LogicalExpression(String operator, IExpression left, IExpression right) {
        type = LOGICAL_EXP;
        this.operator = getLogicalOperator(operator);
        this.left = left;
        this.right = right;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "LogicalExpression")
                .add(KEY_OPERATOR, operator.toString())
                .add(KEY_LEFT, left.getEsTree())
                .add(KEY_RIGHT, right.getEsTree());
        return jb.build();
    }

    @Override
    public LogicalOperator getOperator() {
        return operator;
    }

    @Override
    public IExpression getLeft() {
        return left;
    }

    @Override
    public IExpression getRight() {
        return right;
    }

    public LogicalOperator getLogicalOperator(String op) {
        switch (op) {
        case "||":  return LogicalOperator.OR;
        case "&&":  return LogicalOperator.AND;
        default:    return null;
        }
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitLogicalExpression(this);
    }

}
