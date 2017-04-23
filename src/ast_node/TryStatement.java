package ast_node;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import ast_node.Node.*;

public class TryStatement extends Node implements ITryStatement {

    IBlockStatement block;
    ICatchClause handler;
    IBlockStatement finalizer;

    public TryStatement(IBlockStatement block, ICatchClause handler, IBlockStatement finalizer) {
        type = TRY_STMT;
        this.block = block;
        this.handler = handler;
        this.finalizer = finalizer;
    }

    @Override
    public JsonObject getEsTree() {
        // TODO Auto-generated method stub
        JsonObjectBuilder jb = Json.createObjectBuilder()
                .add(KEY_TYPE, "TryStatement")
                .add(KEY_BLOCK, block.getEsTree());
        if (handler != null) {
            jb.add(KEY_HANDLER, handler.getEsTree());
        } else {
            jb.addNull(KEY_HANDLER);
        }
        if (finalizer != null) {
            jb.add(KEY_FINALIZER, finalizer.getEsTree());
        } else {
            jb.addNull(KEY_FINALIZER);
        }
        return jb.build();
    }

    @Override
    public IBlockStatement getBlock() {
        // TODO Auto-generated method stub
        return block;
    }

    @Override
    public ICatchClause getHandler() {
        // TODO Auto-generated method stub
        return handler;
    }

    @Override
    public IBlockStatement getFinalizer() {
        // TODO Auto-generated method stub
        return finalizer;
    }

    @Override
    public Object accept(ESTreeBaseVisitor visitor) {
        // TODO Auto-generated method stub
        return visitor.visitTryStatement(this);
    }

}
