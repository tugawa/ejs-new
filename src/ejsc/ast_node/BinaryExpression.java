/*
   BinaryExpression.java

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

import ejsc.ast_node.Node.*;

public class BinaryExpression extends Node implements IBinaryExpression {

    public enum BinaryOperator {
        EQ_EQ, NOT_EQ, EQ_EQ_EQ, NOT_EQ_EQ,
        LT, LE, GT, GE,
        SLL, SRL, SRA,
        ADD, SUB, MUL, DIV, MOD,
        BIT_OR, BIT_AND, BIT_XOR,
        IN, INSTANCEOF;
        
        public String toString() {
            switch (this) {
            case EQ_EQ:         return "==";
            case NOT_EQ:        return "!=";
            case EQ_EQ_EQ:      return "===";
            case NOT_EQ_EQ:     return "!==";
            case LT:            return "<";
            case LE:            return "<=";
            case GT:            return ">";
            case GE:            return ">=";
            case SLL:           return "<<";
            case SRL:           return ">>";
            case SRA:           return ">>>";
            case ADD:           return "+";
            case SUB:           return "-";
            case MUL:           return "*";
            case DIV:           return "/";
            case MOD:           return "%";
            case BIT_OR:        return "|";
            case BIT_AND:       return "&";
            case BIT_XOR:       return "^";
            case IN:            return "in";
            case INSTANCEOF:    return "instanceof";
            }
            return null;
        }
    }

    BinaryOperator operator;
    IExpression left;
    IExpression right;

    public BinaryExpression(String operator, IExpression left, IExpression right) {
        type = BINARY_EXP;
        this.operator = this.stringToBinaryOperator(operator);
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
    
    public String stringOperator() {
        switch (operator) {
        case EQ_EQ:         return "==";
        case NOT_EQ:        return "!=";
        case EQ_EQ_EQ:      return "===";
        case NOT_EQ_EQ:     return "!==";
        case LT:            return "<";
        case LE:            return "<=";
        case GT:            return ">";
        case GE:            return ">=";
        case SLL:           return "<<";
        case SRL:           return ">>";
        case SRA:           return ">>>";
        case ADD:           return "+";
        case SUB:           return "-";
        case MUL:           return "*";
        case DIV:           return "/";
        case MOD:           return "%";
        case BIT_OR:        return "|";
        case BIT_AND:       return "&";
        case BIT_XOR:       return "^";
        case IN:            return "in";
        case INSTANCEOF:    return "instanceof";
        }
        return null;
    }
    
    public BinaryOperator stringToBinaryOperator(String str) {
        switch (str) {
        case "==":
            return BinaryOperator.EQ_EQ;
        case "!=":
            return BinaryOperator.NOT_EQ;
        case "===":
            return BinaryOperator.EQ_EQ_EQ;
        case "!==":
            return BinaryOperator.NOT_EQ_EQ;
        case "<":
            return BinaryOperator.LT;
        case "<=":
            return BinaryOperator.LE;
        case ">":
            return BinaryOperator.GT;
        case ">=":
            return BinaryOperator.GE;
        case "<<":
            return BinaryOperator.SLL;
        case ">>":
            return BinaryOperator.SRL;
        case ">>>":
            return BinaryOperator.SRA;
        case "+":
            return BinaryOperator.ADD;
        case "-":
            return BinaryOperator.SUB;
        case "*":
            return BinaryOperator.MUL;
        case "/":
            return BinaryOperator.DIV;
        case "%":
            return BinaryOperator.MOD;
        case "|":
            return BinaryOperator.BIT_OR;
        case "^":
            return BinaryOperator.BIT_XOR;
        case "&":
            return BinaryOperator.BIT_AND;
        case "in":
            return BinaryOperator.IN;
        case "instanceof":
            return BinaryOperator.INSTANCEOF;
        default:
            return null;
        }
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitBinaryExpression(this);
    }
}
