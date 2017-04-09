package estree;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import estree.Node.*;

public class VariableDeclaration extends Node implements IVariableDeclaration {
    List<IVariableDeclarator> declarations;
    String kind = "var";

    public VariableDeclaration(List<IVariableDeclarator> declarations) {
        type = VAR_DECLARATION;
        this.declarations = declarations;
    }

    @Override
    public String toString() {
        String str = "VariableDeclaration(";
        for (int i = 0; i < declarations.size(); i++) {
            str += declarations.get(i).toString() + (i+1 < declarations.size() ? "," : "");
        }
        str += ")";
        return str;
    }

    @Override
    public JsonObject getEsTree() {
        JsonArrayBuilder declarationsJsonBuidler = Json.createArrayBuilder();
        for (int i = 0; i < declarations.size(); i++) {
            declarationsJsonBuidler.add(declarations.get(i).getEsTree());
        }
        JsonObject json = Json.createObjectBuilder()
                .add(KEY_TYPE, "VariableDeclaration")
                .add(KEY_DECLARATIONS, declarationsJsonBuidler)
                .add(KEY_KIND, kind)
                .build();
        return json;
    }

    @Override
    public List<IVariableDeclarator> getDeclarations() {
        return declarations;
    }

    @Override
    public String getKind() {
        return kind;
    }

	@Override
	public void setDeclarations(List<IVariableDeclarator> varDecls) {
		this.declarations = varDecls;
	}

	@Override
	public Object accept(ESTreeBaseVisitor visitor) {
		return visitor.visitVariableDeclaration(this);
	}
}
