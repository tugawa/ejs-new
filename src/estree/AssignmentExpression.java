package estree;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

public class AssignmentExpression extends Node implements IAssignmentExpression {

    /*
     * AssignmentOperator {
     *    "=" | "+=" | "-=" | "*=" | "/=" | "%="
     *        | "<<=" | ">>=" | ">>>="
     *        | "|=" | "^=" | "&="
     * }
     */

    public enum AssignmentOperator {
        EQ_EQ("="),
        ADD_EQ("+="),
        SUB_EQ("-="),
        MUL_EQ("*="),
        DIV_EQ("/="),
        PER_EQ("%="),
        LT_LT_EQ("<<="),
        GT_GT_EQ(">>="),
        GT_GT_GT_EQ(">>>="),
        OR_EQ("|="),
        EXOR_EQ("^="),
        AND_EQ("&=");

        private String op;

        private AssignmentOperator(String op) {
            this.op = op;
        }

        public String toString() {
            return op;
        }

        public static AssignmentOperator getAssignmentOperator(String op) {
            switch (op) {
            case "=":
                return EQ_EQ;
            case "+=":
                return ADD_EQ;
            case "-=":
                return SUB_EQ;
            case "*=":
                return MUL_EQ;
            case "/=":
                return DIV_EQ;
            case "%=":
                return PER_EQ;
            case "<<=":
                return LT_LT_EQ;
            case ">>=":
                return GT_GT_EQ;
            case ">>>=":
                return GT_GT_GT_EQ;
            case "|=":
                return OR_EQ;
            case "^=":
                return EXOR_EQ;
            case "&=":
                return AND_EQ;
            default:
                return null;
            }
        }
    }

    AssignmentOperator operator;
    public enum LeftNodeType {
        EXPRESSION, PATTERN;
    }
    LeftNodeType leftNodeType;
    IExpression expLeft;
    IPattern patternLeft;
    IExpression right;

    private AssignmentExpression() {
        type = ASSIGNMENT_EXP;
    }

    public AssignmentExpression(String op, IExpression expLeft, IExpression right) {
        this();
        operator = AssignmentOperator.getAssignmentOperator(op);
        leftNodeType = LeftNodeType.EXPRESSION;
        this.expLeft = expLeft;
        this.right = right;
    }

    public AssignmentExpression(String op, IPattern patternLeft, IExpression right) {
        this();
        operator = AssignmentOperator.getAssignmentOperator(op);
        leftNodeType = LeftNodeType.PATTERN;
        this.patternLeft = patternLeft;
        this.right = right;
    }

    @Override
    public JsonObject getEsTree() {
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "AssignmentExpression")
                .add(KEY_OPERATOR, operator.toString());
        switch (leftNodeType) {
        case EXPRESSION:
            jb.add(KEY_LEFT, expLeft.getEsTree());
            break;
        case PATTERN:
            jb.add(KEY_LEFT, patternLeft.getEsTree());
            break;
        default:
            // error;
        }
        jb.add(KEY_RIGHT, right.getEsTree());
        return jb.build();
    }

    public LeftNodeType getLeftNodeType() {
        return leftNodeType;
    }

    @Override
    public AssignmentOperator getOperator() {
        return operator;
    }

    @Override
    public IPattern getPatternLeft() {
        return patternLeft;
    }

    @Override
    public IExpression getExpressionLeft() {
        return expLeft;
    }

    @Override
    public IExpression getRight() {
        return right;
    }

	@Override
	public void setRight(IExpression exp) {
		this.right = exp;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitAssignmentExpression(this);
	}

}
