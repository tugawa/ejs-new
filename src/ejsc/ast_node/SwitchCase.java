/*
   SwitchCase.java

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

public class SwitchCase extends Node implements ISwitchCase {

    IExpression test;
    List<IStatement> consequent;

    public SwitchCase(IExpression test, List<IStatement> consequent) {
        type = SWITCH_CASE;
        this.test = test;
        this.consequent = consequent;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonArrayBuilder consequentJb = Json.createArrayBuilder();
        for (IStatement c : consequent) {
            consequentJb.add(c.getEsTree());
        }
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "SwitchCase");
        if (test != null) {
            jb.add(KEY_TEST, test.getEsTree());
        } else {
            jb.addNull(KEY_TEST);
        }
        jb.add(KEY_CONSEQUENT, consequentJb);
        return jb.build();
    }

    @Override
    public IExpression getTest() {
        // TODO Auto-generated method stub
        return test;
    }

    @Override
    public List<IStatement> getConsequent() {
        // TODO Auto-generated method stub
        return consequent;
    }

    @Override
    public void setTest(IExpression test) {
        // TODO Auto-generated method stub
        this.test = test;
    }

    @Override
    public void setConsequent(List<IStatement> consequent) {
        // TODO Auto-generated method stub
        this.consequent = consequent;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitSwitchCase(this);
    }


}
