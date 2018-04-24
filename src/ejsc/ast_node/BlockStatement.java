/*
   BlockStatement.java

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

public class BlockStatement extends Node implements IBlockStatement {

    List<IStatement> body;

    public BlockStatement(List<IStatement> body) {
        type = BLOCK_STMT;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonArrayBuilder bodyJb = Json.createArrayBuilder();
        for (IStatement stmt : body) {
            bodyJb.add(stmt.getEsTree());
        }
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "BlockStatement")
                .add(KEY_BODY, bodyJb);
        return jb.build();
    }

    @Override
    public List<IStatement> getBody() {
        // TODO Auto-generated method stub
        return body;
    }
    
    @Override
    public void setBody(List<IStatement> body) {
        // TODO Auto-generated method stub
        this.body = body;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitBlockStatement(this);
    }

    

}
