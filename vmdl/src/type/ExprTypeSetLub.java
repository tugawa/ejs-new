package type;

import java.util.Set;

public class ExprTypeSetLub extends ExprTypeSet{

    public ExprTypeSetLub(){
        super();
    }

    public ExprTypeSetLub(AstType type){
        super(type);
    }

    private AstType getType(){
        if(typeSet.size() > 1){
            throw new Error("InternalError: typeset size is greatar than 1: "+typeSet.toString());
        }
        for(AstType type : typeSet){
            return type;
        }
        return AstType.BOT;
    }

    @Override
    public void add(AstType type){
        if(typeSet.isEmpty()){
            typeSet.add(type);
        }else{
            if(type == AstType.BOT) return;
            if(typeSet.size() != 1){
                throw new Error("InternalError: Illigal exprTypeSet state: "+typeSet.toString());
            }
            AstType t = getType();
            typeSet.clear();
            typeSet.add(t.lub(type));
        }
    }

    @Override
    public ExprTypeSet combine(ExprTypeSet that){
        Set<AstType> thisSet = this.getTypeSet();
        Set<AstType> thatSet = that.getTypeSet();
        if(thisSet.size() > 1 || thatSet.size() > 1){
            throw new Error("InternalError: Illigal exprTypeSet state: "+typeSet.toString());
        }
        AstType t = getType();
        for(AstType type : thisSet){
            t = t.lub(type);
        }
        return new ExprTypeSetLub(t);
    }

    @Override
    public ExprTypeSet clone(){
        return new ExprTypeSetLub(getType());
    }
}