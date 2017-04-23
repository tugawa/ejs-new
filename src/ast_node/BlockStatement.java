package ast_node;

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import ast_node.Node.*;

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
