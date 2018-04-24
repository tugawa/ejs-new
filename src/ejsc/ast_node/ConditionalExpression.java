/*
   ConditionalExpression.java

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

public class ConditionalExpression extends Node implements IConditionalExpression {

    IExpression test;
    IExpression alternate;
    IExpression consequent;

    public ConditionalExpression(IExpression test, IExpression consequent, IExpression alternate) {
        type = CONDITIONAL_EXP;
        this.test = test;
        this.consequent = consequent;
        this.alternate = alternate;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ConditionalExpression")
                .add(KEY_TEST, test.getEsTree())
                .add(KEY_CONSEQUENT, consequent.getEsTree())
                .add(KEY_ALTERNATE, alternate.getEsTree());

        return jb.build();
    }

    @Override
    public IExpression getTest() {
        // TODO Auto-generated method stub
        return test;
    }

    @Override
    public IExpression getAlternate() {
        // TODO Auto-generated method stub
        return alternate;
    }

    @Override
    public IExpression getConsequent() {
        // TODO Auto-generated method stub
        return consequent;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitConditionalExpression(this);
    }

}
