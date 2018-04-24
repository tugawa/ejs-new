/*
   FunctionExpression.java

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

public class FunctionExpression extends Node implements IFunctionExpression {
    IIdentifier id;
    List<IPattern> params;
    IBlockStatement body;

    public FunctionExpression(IIdentifier id, List<IPattern> params, IBlockStatement body) {
        type = FUNC_EXP;
        this.id = id;
        this.params = params;
        this.body = body;
    }

    @Override
    public IIdentifier getId() {
        return id;
    }

    @Override
    public List<IPattern> getParams() {
        return params;
    }

    @Override
    public IBlockStatement getBody() {
        return body;
    }

    @Override
    public void setId(IIdentifier id) {
        // TODO Auto-generated method stub
        this.id = id;
    }

    @Override
    public void setParams(List<IPattern> params) {
        // TODO Auto-generated method stub
        this.params = params;
    }

    @Override
    public void setBody(IBlockStatement body) {
        // TODO Auto-generated method stub
        this.body = body;
    }

    @Override
    public JsonObject getEsTree() {
        JsonArrayBuilder paramsJb = Json.createArrayBuilder();
        for (IPattern param : params)
            paramsJb.add(param.getEsTree());
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "FunctionExpression")
                .add(KEY_PARAMS, paramsJb);
        if (id != null) {
            jb.add(KEY_ID, id.getEsTree());
        } else {
            jb.addNull(KEY_ID);
        }
        if (body != null) {
            jb.add(KEY_BODY, body.getEsTree());
        } else {
            jb.addNull(KEY_BODY);
        }
        return jb.build();
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitFunctionExpression(this);
    }
}
