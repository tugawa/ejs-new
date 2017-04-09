package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import estree.Node.*;

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
	public void setElements(List<IExpression> elements) {
		this.elements = elements;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitArrayExpression(this);
	}

}
