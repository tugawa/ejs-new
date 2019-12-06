package type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeMapSetLub extends TypeMapSetFull {

    public TypeMapSetLub(){
        super();
    }

    public TypeMapSetLub(Set<TypeMap> _typeMapSet){
        super(_typeMapSet);
    }

    @Override
    public void setTypeMapSet(Set<TypeMap> _typeMapSet){
        if(_typeMapSet == null){
            super.setTypeMapSet(null);
            return;
        }
        if(_typeMapSet.isEmpty()){
            super.setTypeMapSet(null);
            return;
        }
        TypeMap newMap = null;
        for(TypeMap map : _typeMapSet){
            newMap = map.lub(newMap);
        }
        Set<TypeMap> newSet = new HashSet<>();
        newSet.add(newMap);
        super.setTypeMapSet(newSet);
    }

    @Override
    public Set<TypeMap> getAddedSet(TypeMap typeMap, String name, AstType type){
        Set<TypeMap> addedSet = new HashSet<>();
        TypeMap temp = typeMap.clone();
        temp.add(name, type);
        addedSet.add(temp);
        return addedSet;
    }
    @Override
    public Set<TypeMap> getAddedSet(TypeMap typeMap, Map<String, AstType> map){
        Set<TypeMap> addedSet = new HashSet<>();
        TypeMap temp = typeMap.clone();
        for(String name : map.keySet()){
            AstType type = map.get(name);
            temp.add(name, type);
        }
        addedSet.add(temp);
        return addedSet;
    }
    @Override
    public Set<TypeMap> getAssignedSet(TypeMap typeMap, String name, AstType type){
        Set<TypeMap> assignedSet = new HashSet<>();
        TypeMap temp = typeMap.clone();
        temp.assign(name, type);
        assignedSet.add(temp);
        return assignedSet;
    }
    @Override
    public TypeMapSet select(Collection<String> domain){
        Set<TypeMap> selectedSet = new HashSet<>();
        for(TypeMap m : typeMapSet){
            TypeMap selectedMap = new TypeMap();
            for(String s : domain){
                AstType type = m.get(s);
                if(type==null){
                    if(containsKey(s)){
                        System.err.println("InternalWarnig: TypeMap has no element: \""+s+"\"");
                        type = AstType.BOT;
                    }else{
                        throw new Error("InternalError: No such element: \""+s+"\"");
                    }
                }
                selectedMap.add(s, type);
            }
            selectedSet.add(selectedMap);
        }
        return new TypeMapSetLub(selectedSet);
    }
    @Override
    public TypeMapSet clone(){
        Set<TypeMap> cloneTypeMapSet = new HashSet<>();
        for(TypeMap typeMap : typeMapSet){
            cloneTypeMapSet.add(typeMap.clone());
        }
        return new TypeMapSetLub(cloneTypeMapSet);
    }
    @Override
    public TypeMapSet combine(TypeMapSet that){
        Set<TypeMap> newSet = new HashSet<>();
        Set<TypeMap> thatTypeMapSet = that.getTypeMapSet();
        TypeMap newMap = new TypeMap();
        for(String name : getKeys()){
            AstType type = AstType.BOT;
            for(TypeMap map : typeMapSet){
                type = type.lub(map.get(name));
            }
            for(TypeMap map : thatTypeMapSet){
                type = type.lub(map.get(name));
            }
            newMap.add(name, type);
        }
        newSet.add(newMap);
        return new TypeMapSetLub(newSet);
    }
    private int indexOf(String[] varNames, String v) {
        for (int i = 0; i < varNames.length; i++) {
            if (varNames[i].equals(v)) {
                return i;
            }
        }
        return -1;
    }
    @Override
    public TypeMapSet enterCase(String[] varNames, VMDataTypeVecSet caseCondition){
        TypeMap typeMap = getOne();
        TypeMap newMap = new TypeMap();
        Set<TypeMap> newSet = new HashSet<>();
        AstType[] paramTypes = new AstType[varNames.length];
        for (int i = 0; i < varNames.length; i++){
            paramTypes[i] = typeMap.get(varNames[i]);
        }
        VMDataTypeVecSet.ByCommonTypes vtvs = new VMDataTypeVecSet.ByCommonTypes(varNames, paramTypes);
        vtvs = vtvs.intersection(caseCondition);
        for (int i = 0; i < varNames.length; i++) {
            AstType t = vtvs.getMostSpecificType(varNames[i]);
            newMap.add(varNames[i], t);
        }
        for(String name : typeMap.keySet()){
            AstType type = typeMap.get(name);
            int index = indexOf(varNames, name);
            if(index == -1){
                newMap.add(name, type);
            }
        }
        newSet.add(newMap);
        return new TypeMapSetLub(newSet);
    }
    @Override
    public TypeMapSet rematch(String[] params, String[] args, Set<String> domain){
        Set<TypeMap> newSet = new HashSet<>();
        for(TypeMap map : typeMapSet){
            TypeMap replacedMap = new TypeMap();
            for(String v : domain){
                int index = indexOf(params, v);
                if(index == -1){
                    replacedMap.add(v, map.get(v));
                }else{
                    replacedMap.add(v, map.get(args[index]));
                }
            }
            newSet.add(replacedMap);
        }
        return new TypeMapSetLub(newSet);
    }
    @Override
    public TypeMapSet getBottomDict(){
        Set<String> domain = getKeys();
        TypeMap newGamma = new TypeMap();
        Set<TypeMap> newSet = new HashSet<>();
        for (String v : domain) {
            newGamma.add(v, AstType.BOT);
        }
        newSet.add(newGamma);
        return new TypeMapSetLub(newSet);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj || obj != null && obj instanceof TypeMapSet) {
            TypeMapSet tm = (TypeMapSet)obj;
            Set<TypeMap> tmTypeMapSet = tm.getTypeMapSet();
            return (typeMapSet != null && tmTypeMapSet !=null && typeMapSet.equals(tmTypeMapSet));
        } else {
            
            return false;
        }
    }
}