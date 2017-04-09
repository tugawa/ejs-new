package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class LogicalExpression extends Node implements ILogicalExpression {

    public enum LogicalOperator {
        OR("||"),
        AND("&&");

        String op;

        private LogicalOperator(String op) {
            this.op = op;
        }

        public String toString() {
            return op;
        }

        public static LogicalOperator getLogicalOperator(String op) {
            switch (op) {
            case "||":
                return OR;
            case "&&":
                return AND;
            default:
                return null;
            }
        }
    }

    LogicalOperator operator;
    IExpression left;
    IExpression right;

    public LogicalExpression(String operator, IExpression left, IExpression right) {
        type = LOGICAL_EXP;
        this.operator = LogicalOperator.getLogicalOperator(operator);
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

	@Override
	public void setLeft(IExpression left) {
		this.left = left;
	}

	@Override
	public void setRight(IExpression right) {
		this.right = right;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitLogicalExpression(this);
	}

}
