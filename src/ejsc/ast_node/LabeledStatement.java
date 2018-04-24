/*
   LabeledStatement.java

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

public class LabeledStatement extends Node implements ILabeledStatement {

    IIdentifier label;
    IStatement body;

    public LabeledStatement(IIdentifier label, IStatement body) {
        type = LABELED_STMT;
        this.label = label;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "LabeledStatement")
                .add(KEY_LABEL, label.getEsTree())
                .add(KEY_BODY, body.getEsTree());
        return jb.build();
    }

    @Override
    public IIdentifier getLabel() {
        // TODO Auto-generated method stub
        return label;
    }

    @Override
    public IStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitLabeledStatement(this);
    }

}
