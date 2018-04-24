/*
   UpdateExpression.java

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
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "UpdateExpression")
                .add(KEY_OPERATOR, operator.toString())
                .add(KEY_ARGUMENT, argument.getEsTree())
                .add(KEY_PREFIX, prefix);
        return jb.build();
    }

    @Override
    public String getOperator() {
        // TODO Auto-generated method stub
        return operator.toString();
    }

    @Override
    public IExpression getArgument() {
        // TODO Auto-generated method stub
        return argument;
    }

    @Override
    public boolean getPrefix() {
        // TODO Auto-generated method stub
        return prefix;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitUpdateExpression(this);
    }

}
