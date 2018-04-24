/*
   SequenceExpression.java

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

public class SequenceExpression extends Node implements ISequenceExpression {

    List<IExpression> expression;

    public SequenceExpression(List<IExpression> expr) {
        type = SEQUENCE_EXP;
        this.expression = expr;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "SequenceExpression");
        if (expression == null) {
            jb.addNull(KEY_EXPRESSIONS);
        } else {
            JsonArrayBuilder propertiesJb = Json.createArrayBuilder();
            for (IExpression property : expression) {
                propertiesJb.add(property.getEsTree());
            }
            jb.add(KEY_EXPRESSIONS, propertiesJb);
        }

        return jb.build();
    }

    @Override
    public List<IExpression> getExpression() {
        // TODO Auto-generated method stub
        return expression;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitSequenceExpression(this);
    }
}
