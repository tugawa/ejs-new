/*
   ArrayExpression.java

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

public class ArrayExpression extends Node implements IArrayExpression  {

    List<IExpression> elements;

    public ArrayExpression(List<IExpression> elements) {
        type = ARRAY_EXP;
        this.elements = elements;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ArrayExpression");
        if (elements == null) {
            jb.addNull(KEY_ELEMENTS);
        } else {
            JsonArrayBuilder elementsJb = Json.createArrayBuilder();
            for (IExpression element : elements) {
                if (element != null)
                    elementsJb.add(element.getEsTree());
                else
                    elementsJb.addNull();
            }
            jb.add(KEY_ELEMENTS, elementsJb);
        }

        return jb.build();
    }

    @Override
    public List<IExpression> getElements() {
        // TODO Auto-generated method stub
        return elements;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitArrayExpression(this);
    }

}
