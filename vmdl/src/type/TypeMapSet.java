package type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class TypeMapSet implements Iterable<TypeMap>{
    protected Set<TypeMap> typeMapSet;

    public TypeMapSet(){
        typeMapSet = new HashSet<>();
    }
    public TypeMapSet(Set<TypeMap> _typeMapSet){
        typeMapSet = _typeMapSet;
    }

    public void setTypeMapSet(Set<TypeMap> _typeMapSet){
        typeMapSet = _typeMapSet;
    }
    public Set<TypeMap> getTypeMapSet(){
        return typeMapSet;
    }
    public abstract void setDispatchSet(Set<String> set);
    public abstract Set<String> getDispatchSet();
    public abstract Set<TypeMap> getAddedSet(TypeMap typeMap, String name, AstType type);
    public abstract Set<TypeMap> getAssignedSet(TypeMap typeMap, String name, AstType type);
    public abstract boolean containsKey(String key);
    public abstract Set<String> getKeys();
    public abstract TypeMapSet select(Collection<String> domain);
    public abstract TypeMapSet combine(TypeMapSet that);
    public abstract TypeMapSet enterCase(String[] varNames, VMDataTypeVecSet caseCondition);
    public abstract TypeMapSet rematch(String[] params, String[] args, Set<String> domain);
    public abstract TypeMapSet getBottomDict();
    public abstract Set<VMDataType[]> filterTypeVecs(String[] formalParams, Set<VMDataType[]> vmtVecs);
    public abstract boolean noInformationAbout(String[] formalParams);

    public abstract Set<TypeMap> getAddedSet(TypeMap typeMap, Map<String, AstType> map);

    @Override
    public abstract TypeMapSet clone();
    
    @Override
    public abstract boolean equals(Object obj);

    @Override
    public int hashCode(){
        return typeMapSet.hashCode();
    }

    @Override
    public String toString(){
        return typeMapSet.toString();
    }

    @Override
    public Iterator<TypeMap> iterator(){
        return typeMapSet.iterator();
    }

    public TypeMap getOne(){
        for(TypeMap map : typeMapSet){
            return map;
        }
        return null;
    }
}
