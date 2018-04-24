/*
   ForInStatement.java

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

public class ForInStatement extends Node implements IForInStatement {

    public enum InitType {
        VAR_DECL,
        PATTERN
    }
    InitType initType;

    IVariableDeclaration varDeclLeft;
    IPattern patternLeft;
    IExpression right;
    IStatement body;

    String label = null;

    public ForInStatement(IVariableDeclaration left, IExpression right, IStatement body) {
        type = FOR_IN_STMT;
        initType = InitType.VAR_DECL;
        this.varDeclLeft = left;
        this.right = right;
        this.body = body;
    }

    public ForInStatement(IPattern left, IExpression right, IStatement body) {
        type = FOR_IN_STMT;
        initType = InitType.PATTERN;
        this.patternLeft = left;
        this.right = right;
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "ForInStatement");
        if (initType == InitType.VAR_DECL) {
            jb.add(KEY_LEFT, varDeclLeft.getEsTree());
        } else if (initType == InitType.PATTERN) {
            jb.add(KEY_LEFT, patternLeft.getEsTree());
        }else {
            jb.addNull(KEY_LEFT);
        }
        jb.add(KEY_RIGHT, right.getEsTree());
        jb.add(KEY_BODY, body.getEsTree());

        return jb.build();
    }
    
    public InitType getInitType() {
        return this.initType;
    }

    @Override
    public IVariableDeclaration getValDeclLeft() {
        // TODO Auto-generated method stub
        return varDeclLeft;
    }

    @Override
    public IPattern getPatternLeft() {
        // TODO Auto-generated method stub
        return patternLeft;
    }

    @Override
    public IExpression getRight() {
        // TODO Auto-generated method stub
        return right;
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
        return visitor.visitForInStatement(this);
    }

}
