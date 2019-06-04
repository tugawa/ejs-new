/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
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
        return elements;
    }

    @Override
    public <T> T accept(ESTreeBaseVisitor<T> visitor) {
        return visitor.visitArrayExpression(this);
    }

}
