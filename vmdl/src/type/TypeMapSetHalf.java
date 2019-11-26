package type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeMapSetHalf extends TypeMapSetFull {
    private Set<String> dispatchSet;
    public TypeMapSetHalf(){
        super();
        dispatchSet = new HashSet<>();
    }

    public TypeMapSetHalf(Set<TypeMap> _typeMapSet, Set<String> _dispatchSet){
        super(_typeMapSet);
        dispatchSet = _dispatchSet;
    }

    @Override
    public void setDispatchSet(Set<String> set){
        dispatchSet = set;
    }
    @Override
    public Set<String> getDispatchSet(){
        return dispatchSet;
    }
    @Override
    protected Set<AstType> getTypeSet(String name, AstType type){
        Set<AstType> set;
        if(dispatchSet.contains(name)){
            set = super.getTypeSet(name, type);
        }else{
            set = new HashSet<>();
            set.add(type);
        }
        return set;
    }
    private Set<String> cloneDispatchSet(){
        Set<String> newSet = new HashSet<>();
        for(String name : dispatchSet){
            newSet.add(name);
        }
        return newSet;
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
        return new TypeMapSetHalf(selectedSet, cloneDispatchSet());
    }
    @Override
    public TypeMapSet clone(){
        Set<TypeMap> cloneTypeMapSet = new HashSet<>();
        for(TypeMap typeMap : typeMapSet){
            cloneTypeMapSet.add(typeMap.clone());
        }
        return new TypeMapSetHalf(cloneTypeMapSet, cloneDispatchSet());
    }
    private AstType lub(Set<TypeMap> maps, String name){
        AstType result = AstType.BOT;
        for(TypeMap map : maps){
            result = result.lub(map.get(name));
        }
        return result;
    }
    @Override
    public TypeMapSet combine(TypeMapSet that){
        Set<TypeMap> newSet = new HashSet<>();
        Map<String, AstType> thisLubMap = new HashMap<>();
        Map<String, AstType> thatLubMap = new HashMap<>();
        for(String name : getKeys()){
            if(dispatchSet.contains(name)) continue;
            thisLubMap.put(name, lub(typeMapSet, name));
        }
        for(String name : that.getKeys()){
            if(dispatchSet.contains(name)) continue;
            thatLubMap.put(name, lub(typeMapSet, name));
        }
        for(TypeMap map : this){
            TypeMap newMap = new TypeMap();
            for(String name : map.keySet()){
                AstType type;
                if(dispatchSet.contains(name)){
                    type = map.get(name);
                }else{
                    type = map.get(name).lub(thatLubMap.get(name));
                }
                newMap.add(name, type);
            }
            newSet.add(newMap);
        }
        for(TypeMap map : that){
            TypeMap newMap = new TypeMap();
            for(String name : map.keySet()){
                AstType type;
                if(dispatchSet.contains(name)){
                    type = map.get(name);
                }else{
                    type = map.get(name).lub(thisLubMap.get(name));
                }
                newMap.add(name, type);
            }
            newSet.add(newMap);
        }
        return new TypeMapSetHalf(newSet, cloneDispatchSet());
    }
    private Set<TypeMap> getBottedSet(String[] varNames){
        Set<TypeMap> newSet = new HashSet<>();
        for(TypeMap map : typeMapSet){
            TypeMap bottedMap = map.clone();
            for(String varName : varNames){
                bottedMap.assign(varName, AstType.BOT);
            }
            newSet.add(bottedMap);
        }
        return newSet;
    }
    @Override
    public TypeMapSet enterCase(String[] varNames, VMDataTypeVecSet caseCondition){
        Set<VMDataType[]> conditionSet = caseCondition.getTuples();
        Set<TypeMap> newSet = new HashSet<>();
        for(VMDataType[] v : conditionSet){
            int length = varNames.length;
            NEXT_MAP: for(TypeMap map : typeMapSet){
                TypeMap matchedMap = map.clone();
                for(int i=0; i<length; i++){
                    AstType varType = map.get(varNames[i]);
                    AstType conditionType = AstType.get(v[i]);
                    if(!(conditionType.isSuperOrEqual(varType))){
                        continue NEXT_MAP;
                    }
                }
                newSet.add(matchedMap);
            }
        }
        if(newSet.isEmpty()){
            newSet = getBottedSet(varNames);
        }
        return new TypeMapSetHalf(newSet, cloneDispatchSet());
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
        return new TypeMapSetHalf(newSet, cloneDispatchSet());
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
        return new TypeMapSetHalf(newSet, cloneDispatchSet());
    }
    @Override
    public Set<VMDataType[]> filterTypeVecs(String[] formalParams, Set<VMDataType[]> vmtVecs){
        Set<VMDataType[]> filtered = new HashSet<VMDataType[]>();
        int length = formalParams.length;
        for(VMDataType[] conditionVector : vmtVecs){
            NEXT_MAP: for(TypeMap map : typeMapSet){
                for(int i=0; i<length; i++){
                    VMDataType conditionType = conditionVector[i];
                    AstType realType = map.get(formalParams[i]);
                    if(realType == null) continue NEXT_MAP;
                    if(realType == AstType.BOT) continue NEXT_MAP;
                    if(!(realType.isSuperOrEqual(AstType.get(conditionType)))) continue NEXT_MAP;
                }
                filtered.add(conditionVector);
                break;
            }
        }
        return filtered;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj || obj != null && obj instanceof TypeMapSetHalf) {
            TypeMapSetFull tm = (TypeMapSetHalf)obj;
            Set<TypeMap> tmTypeMapSet = tm.getTypeMapSet();
            return (typeMapSet != null && tmTypeMapSet !=null && typeMapSet.equals(tmTypeMapSet));
        } else {
            return false;
        }
    }
}