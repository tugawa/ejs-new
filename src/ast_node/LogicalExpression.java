package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

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
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "LogicalExpression")
                .add(KEY_OPERATOR, operator.toString())
                .add(KEY_LEFT, left.getEsTree())
                .add(KEY_RIGHT, right.getEsTree());
        return jb.build();
    }

    @Override
    public LogicalOperator getOperator() {
        // TODO Auto-generated method stub
        return operator;
    }

    @Override
    public IExpression getLeft() {
        // TODO Auto-generated method stub
        return left;
    }

    @Override
    public IExpression getRight() {
        // TODO Auto-generated method stub
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
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitLogicalExpression(this);
    }

}
