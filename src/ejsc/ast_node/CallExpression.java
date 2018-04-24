/*
   CallExpression.java

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

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ejsc.ast_node.Node.*;

public class CallExpression extends Node implements ICallExpression {

    IExpression callee;
    List<IExpression> arguments;

    boolean isTailCall = false;

    public CallExpression(IExpression callee, List<IExpression> arguments) {
        type = CALL_EXP;
        this.callee = callee;
        this.arguments = arguments;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "CallExpression")
                .add(KEY_CALLEE, callee.getEsTree());
        if (arguments == null || arguments.size() == 0) {
            JsonArrayBuilder argumentsJb = Json.createArrayBuilder();
            jb.add(KEY_ARGUMENTS, argumentsJb);
        } else {
            JsonArrayBuilder argumentsJb = Json.createArrayBuilder();
            for (IExpression argument : arguments) {
                argumentsJb.add(argument.getEsTree());
            }
            jb.add(KEY_ARGUMENTS, argumentsJb);
        }

        return jb.build();
    }

    public void setTailCall() {
        this.isTailCall = true;
    }

    public boolean getTailCall() {
        return this.isTailCall;
    }

    @Override
    public IExpression getCallee() {
        // TODO Auto-generated method stub
        return callee;
    }

    @Override
    public List<IExpression> getArguments() {
        // TODO Auto-generated method stub
        return arguments;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitCallExpression(this);
    }

}
