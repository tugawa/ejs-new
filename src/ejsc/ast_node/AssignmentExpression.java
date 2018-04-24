/*
   AssignmentExpression.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Takafumi Kataoka, 2017-18
     Tomoharu Ugawa, 2017-18
     Hideya Iwasaki, 2017-18

   The eJS Project is the successor of the SSJS Project at the University of
   Electro-communications, which was contributed by the following members.

     Sho Takada, 2012-13
     Akira Tanimura, 2012-13
     Akihiro Urushihara, 2013-14
     Ryota Fujii, 2013-14
     Tomoharu Ugawa, 2012-14
     Hideya Iwasaki, 2012-14
*/
package ejsc.ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

public class AssignmentExpression extends Node implements IAssignmentExpression {

    /*
     * AssignmentOperator {
     *    "=" | "+=" | "-=" | "*=" | "/=" | "%="
     *        | "<<=" | ">>=" | ">>>="
     *        | "|=" | "^=" | "&="
     * }
     */

    public enum AssignmentOperator {
        EQ_EQ,
        ADD_EQ,
        SUB_EQ,
        MUL_EQ,
        DIV_EQ,
        PER_EQ,
        LT_LT_EQ,
        GT_GT_EQ,
        GT_GT_GT_EQ,
        OR_EQ,
        EXOR_EQ,
        AND_EQ;
        
        public String toString() {
            switch (this) {
            case EQ_EQ:         return "=";
            case ADD_EQ:        return "+=";
            case SUB_EQ:        return "-=";
            case MUL_EQ:        return "*=";
            case DIV_EQ:        return "/=";
            case PER_EQ:        return "%=";
            case LT_LT_EQ:      return "<<=";
            case GT_GT_EQ:      return ">>=";
            case GT_GT_GT_EQ:   return ">>>=";
            case OR_EQ:         return "|=";
            case EXOR_EQ:       return "^=";
            case AND_EQ:        return "&=";
            }
            return null;
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
    
    public AssignmentOperator getAssignmentOperator(String op) {
        switch (op) {
        case "=":   return AssignmentOperator.EQ_EQ;
        case "+=":  return AssignmentOperator.ADD_EQ;
        case "-=":  return AssignmentOperator.SUB_EQ;
        case "*=":  return AssignmentOperator.MUL_EQ;
        case "/=":  return AssignmentOperator.DIV_EQ;
        case "%=":  return AssignmentOperator.PER_EQ;
        case "<<=": return AssignmentOperator.LT_LT_EQ;
        case ">>=": return AssignmentOperator.GT_GT_EQ;
        case ">>>=":return AssignmentOperator.GT_GT_GT_EQ;
        case "|=":  return AssignmentOperator.OR_EQ;
        case "^=":  return AssignmentOperator.EXOR_EQ;
        case "&=":  return AssignmentOperator.AND_EQ;
        default:    return null;
        }
    }

    public AssignmentExpression(String op, IExpression expLeft, IExpression right) {
        this();
        operator = getAssignmentOperator(op);
        leftNodeType = LeftNodeType.EXPRESSION;
        this.expLeft = expLeft;
        this.right = right;
    }

    public AssignmentExpression(String op, IPattern patternLeft, IExpression right) {
        this();
        operator = getAssignmentOperator(op);
        leftNodeType = LeftNodeType.PATTERN;
        this.patternLeft = patternLeft;
        this.right = right;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return operator;
    }

    @Override
    public IPattern getPatternLeft() {
        // TODO Auto-generated method stub
        return patternLeft;
    }

    @Override
    public IExpression getExpressionLeft() {
        // TODO Auto-generated method stub
        return expLeft;
    }

    @Override
    public IExpression getRight() {
        // TODO Auto-generated method stub
        return right;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitAssignmentExpression(this);
    }

}
