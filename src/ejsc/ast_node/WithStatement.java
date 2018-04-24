/*
   WithStatement.java

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

public class WithStatement extends Node implements IWithStatement {

    IExpression object;
    IStatement body;

    public WithStatement(IExpression object, IStatement body) {
        type = WITH_STMT;
        this.object = object;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "WithStatement")
                .add(KEY_OBJECT, object.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IExpression getObject() {
        // TODO Auto-generated method stub
        return object;
    }

    @Override
    public IStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitWithStatement(this);
    }

}
