package type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class TypeMapSet implements Iterable<TypeMap>{
    protected Set<TypeMap> typeMapSet;

    public TypeMapSet(){
        typeMapSet = new HashSet<>();
    }
    public TypeMapSet(Set<TypeMap> _typeMapSet){
        if(_typeMapSet == null){
            typeMapSet = new HashSet<>();
        }else{
            typeMapSet = _typeMapSet;
        }
    }

    public void setTypeMapSet(Set<TypeMap> _typeMapSet){
        if(_typeMapSet == null){
            typeMapSet = new HashSet<>();
        }else{
            typeMapSet = _typeMapSet;
        }
    }
    public Set<TypeMap> getTypeMapSet(){
        if(typeMapSet == null){
            throw new Error("Illigal TypeMapSet state: null");
        }
        return typeMapSet;
    }
    public abstract void setDispatchSet(Set<String> set);
    public abstract Set<String> getDispatchSet();
    public abstract Set<TypeMap> getAddedSet(TypeMap typeMap, String name, AstType type);
    public abstract Set<TypeMap> getAssignedSet(TypeMap typeMap, String name, AstType type);
    public abstract Set<TypeMap> getAssignedSet(TypeMap typeMap, String[] names, AstType[] types);
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

    public Collection<String> typeOf(Class<?> type){
        TypeMapSetLub lubed = new TypeMapSetLub(typeMapSet);
        Collection<String> ret = new HashSet<>();
        TypeMap typeMap = lubed.getOne();
        Set<Entry<String, AstType>> entrySet = typeMap.entrySet();
        for(Entry<String, AstType> entry : entrySet){
            if(type.isInstance(entry.getValue())){
                ret.add(entry.getKey());
            }
        }
        return ret;
    }
}
