package estree;

import javax.json.Json;
import javax.json.JsonObject;

import estree.Node.*;

public class BinaryExpression extends Node implements IBinaryExpression {

    public enum BinaryOperator {
        EQ_EQ("=="),
        NOT_EQ("!="),
        EQ_EQ_EQ("==="),
        NOT_EQ_EQ("!=="),
        LT("<"),
        LE("<="),
        GT(">"),
        GE(">="),
        SLL("<<"),
        SRL(">>"),
        SRA(">>>"),
        ADD("+"),
        SUB("-"),
        MUL("*"),
        DIV("/"),
        MOD("%"),
        BIT_OR("|"),
        BIT_XOR("^"),
        BIT_AND("&"),
        IN("in"),
        INSTANCEOF("instanceof");

        String op;

        private BinaryOperator(String op) {
            this.op = op;
        }

        public String toString() {
            return op;
        }

        public static BinaryOperator getBinaryOperator(String op) {
            switch (op) {
            case "==":
                return EQ_EQ;
            case "!=":
                return NOT_EQ;
            case "===":
                return EQ_EQ_EQ;
            case "!==":
                return NOT_EQ_EQ;
            case "<":
                return LT;
            case "<=":
                return LE;
            case ">":
                return GT;
            case ">=":
                return GE;
            case "<<":
                return SLL;
            case ">>":
                return SRL;
            case ">>>":
                return SRA;
            case "+":
                return ADD;
            case "-":
                return SUB;
            case "*":
                return MUL;
            case "/":
                return DIV;
            case "%":
                return MOD;
            case "|":
                return BIT_OR;
            case "^":
                return BIT_XOR;
            case "&":
                return BIT_AND;
            case "in":
                return IN;
            case "instanceof":
                return INSTANCEOF;
            default:
                return null;
            }
        }
    }

    BinaryOperator operator;
    IExpression left;
    IExpression right;

    public BinaryExpression(String operator, IExpression left, IExpression right) {
        type = BINARY_EXP;
        this.operator = BinaryOperator.getBinaryOperator(operator);
        this.left = left;
        this.right = right;
    }

    public String toString() {
        String str = "BinaryExpression("
                + operator + ","
                + left.toString() + ","
                + right.toString()
                + ")";
        return str;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObject json = Json.createObjectBuilder()
                .add(KEY_TYPE, "BinaryExpression")
                .add(KEY_OPERATOR, operator.toString())
                .add(KEY_LEFT, left.getEsTree())
                .add(KEY_RIGHT, right.getEsTree())
                .build();
        return json;
    }

    @Override
    public BinaryOperator getOperator() {
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
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitBinaryExpression(this);
	}
}
