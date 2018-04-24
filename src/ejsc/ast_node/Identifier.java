/*
   Identifier.java

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

public class Identifier extends Node implements Node.IIdentifier {

    String name;

    public Identifier(String name) {
        type = IDENTIFIER;
        this.name = name;
    }

    public String toString() {
        return "Identifier(" + name + ")";
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder()
                .add(KEY_TYPE, "Identifier")
                .add(KEY_NAME, name)
                //.add(KEY_LOC, loc.getAstWithJson())
                ;
        return jsonBuilder.build();
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return name;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitIdentifier(this);
    }
}
