package type;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class ExprTypeSet implements Iterable<AstType>{
    protected Set<AstType> typeSet;

    public ExprTypeSet(){
        typeSet = new HashSet<>();
    }

    public ExprTypeSet(AstType type){
        typeSet = new HashSet<>(1);
        if(type == AstType.BOT) return;
        typeSet.add(type);
    }

    protected ExprTypeSet(Set<AstType> set){
        typeSet = set;
    }

    public Set<AstType> getTypeSet(){
        return typeSet;
    }

    public AstType getOne(){
        for(AstType type : typeSet){
            return type;
        }
        return null;
    }

    @Override
    public String toString(){
        return typeSet.toString();
    }

    @Override
    public Iterator<AstType> iterator(){
        return typeSet.iterator();
    }

    @Override
    public abstract ExprTypeSet clone();
    
    public abstract void add(AstType type);
    public abstract ExprTypeSet combine(ExprTypeSet that);
}