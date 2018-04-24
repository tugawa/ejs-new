/*
   VariableDeclaration.java

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

import ejsc.ast_node.Node.*;

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
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitVariableDeclaration(this);
    }
}
