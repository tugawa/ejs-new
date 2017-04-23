package ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ast_node.Node.*;

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
