/*
   ForStatement.java

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

public class ForStatement extends Node implements IForStatement {

    public enum InitType {
        VAR_DECL,
        EXPRESSION
    }
    InitType initType = null;

    IVariableDeclaration varDeclInit;
    IExpression expressionInit;
    IExpression test;
    IExpression update;
    IStatement body;

    String label;

    public ForStatement(IVariableDeclaration init, IExpression test, IExpression update, IStatement body) {
        type = FOR_STMT;
        if (init != null) {
            initType = InitType.VAR_DECL;
            this.varDeclInit = init;
        }
        this.test = test;
        this.update = update;
        this.body = body;
    }

    public ForStatement(IExpression init, IExpression test, IExpression update, IStatement body) {
        type = FOR_STMT;
        if (init != null) {
            initType = InitType.EXPRESSION;
            this.expressionInit = init;
        }
        this.test = test;
        this.update = update;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ForStatement");
        if (initType == InitType.VAR_DECL) {
            jb.add(KEY_INIT, varDeclInit.getEsTree());
        } else if (initType == InitType.EXPRESSION) {
            jb.add(KEY_INIT, expressionInit.getEsTree());
        }else {
            jb.addNull(KEY_INIT);
        }

        if (test == null) {
            jb.addNull(KEY_TEST);
        } else {
            jb.add(KEY_TEST, test.getEsTree());
        }

        if (update == null) {
            jb.addNull(KEY_UPDATE);
        } else {
            jb.add(KEY_UPDATE, update.getEsTree());
        }

        if (body == null) {
            jb.addNull(KEY_BODY);
        } else {
            jb.add(KEY_BODY, body.getEsTree());
        }

        return jb.build();
    }

    public InitType getInitType() {
        return initType;
    }

    @Override
    public IVariableDeclaration getValDeclInit() {
        // TODO Auto-generated method stub
        return varDeclInit;
    }

    @Override
    public IExpression getExpInit() {
        // TODO Auto-generated method stub
        return expressionInit;
    }

    @Override
    public IExpression getTest() {
        // TODO Auto-generated method stub
        return test;
    }

    @Override
    public IExpression getUpdate() {
        // TODO Auto-generated method stub
        return update;
    }

    @Override
    public IStatement getBody() {
        // TODO Auto-generated method stub
        return body;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitForStatement(this);
    }

}
