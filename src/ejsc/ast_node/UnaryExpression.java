/*
   UnaryExpression.java

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
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return operator.toString();
    }

    @Override
    public boolean getPrefix() {
        // TODO Auto-generated method stub
        return prefix;
    }

    @Override
    public IExpression getArgument() {
        // TODO Auto-generated method stub
        return argument;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitUnaryExpression(this);
    }
}
