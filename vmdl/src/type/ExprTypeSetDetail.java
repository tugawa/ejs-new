package type;

import java.util.Set;

public class ExprTypeSetDetail extends ExprTypeSet{

    public ExprTypeSetDetail(){
        super();
    }

    public ExprTypeSetDetail(AstType type){
        super(type);
    }

    @Override
    public void add(AstType type){
        if(type == AstType.BOT) return;
        typeSet.add(type);
    }

    @Override
    public ExprTypeSet combine(ExprTypeSet that){
        Set<AstType> thisSet = this.getTypeSet();
        Set<AstType> thatSet = that.getTypeSet();
        ExprTypeSetDetail newSet = new ExprTypeSetDetail();
        for(AstType type : thisSet){
            newSet.add(type);
        }
        for(AstType type : thatSet){
            newSet.add(type);
        }
        return newSet;
    }

    @Override
    public ExprTypeSet clone(){
        ExprTypeSetDetail newSet = new ExprTypeSetDetail();
        for(AstType type : typeSet){
            newSet.add(type);
        }
        return newSet;
    }
}