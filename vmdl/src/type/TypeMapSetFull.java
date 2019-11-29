package type;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TypeMapSetFull extends TypeMapSet {

    public TypeMapSetFull(){
        super();
    }

    public TypeMapSetFull(Set<TypeMap> _typeMapSet){
        super(_typeMapSet);
    }

    @Override
    public void setDispatchSet(Set<String> set){}
    @Override
    public Set<String> getDispatchSet(){
        return Collections.emptySet();
    }
    protected Set<AstType> getTypeSet(String name, AstType type){
        Set<AstType> set = AstType.getChildren(type);
        if(set == null){
            set = new HashSet<>();
        }
        if(set.isEmpty()){
            set.add(type);
        }
        return set;
    }
    @Override
    public Set<TypeMap> getAddedSet(TypeMap typeMap, String name, AstType type){
        Set<TypeMap> addedSet = new HashSet<>();
        Set<AstType> addTypes = getTypeSet(name, type);
        for(AstType t : addTypes){
            TypeMap temp = typeMap.clone();
            temp.add(name, t);
            addedSet.add(temp);
        }
        return addedSet;
    }
    @Override
    public Set<TypeMap> getAddedSet(TypeMap typeMap, Map<String, AstType> map){
        Set<TypeMap> tempSet = new HashSet<>();
        tempSet.add(typeMap);
        for(String name : map.keySet()){
            AstType type = map.get(name);
            Set<TypeMap> newTempSet = new HashSet<>();
            for(TypeMap map2 : tempSet){
                newTempSet.addAll(getAddedSet(map2, name, type));
            }
            tempSet = newTempSet;
        }
        return tempSet;
    }
    @Override
    public Set<TypeMap> getAssignedSet(TypeMap typeMap, String name, AstType type){
        Set<TypeMap> assignedSet = new HashSet<>();
        Set<AstType> assignTypes = getTypeSet(name, type);
        for(AstType t : assignTypes){
            TypeMap temp = typeMap.clone();
            temp.assign(name, t);
            assignedSet.add(temp);
        }
        return assignedSet;
    }
    @Override
    public boolean containsKey(String key){
        if(typeMapSet.isEmpty()) return false;
        TypeMap typeMap = getOne();
        return typeMap.containsKey(key);
    }
    @Override
    public Set<String> getKeys(){
        if(typeMapSet.isEmpty()) return Collections.emptySet();
        TypeMap typeMap = getOne();
        if(typeMap==null){
            System.err.println(typeMapSet.toString());
        }
        return typeMap.keySet();
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
        return new TypeMapSetFull(selectedSet);
    }
    @Override
    public TypeMapSet clone(){
        Set<TypeMap> cloneTypeMapSet = new HashSet<>();
        for(TypeMap typeMap : typeMapSet){
            cloneTypeMapSet.add(typeMap.clone());
        }
        return new TypeMapSetFull(cloneTypeMapSet);
    }
    @Override
    public TypeMapSet combine(TypeMapSet that){
        Set<TypeMap> newTypeMapSet = new HashSet<>();
        Set<TypeMap> thatTypeMapSet = that.getTypeMapSet();
        for(TypeMap m : typeMapSet){
            newTypeMapSet.add(m.clone());
        }
        for(TypeMap m : thatTypeMapSet){
            newTypeMapSet.add(m.clone());
        }
        return new TypeMapSetFull(newTypeMapSet);
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
        return new TypeMapSetFull(newSet);
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
        return new TypeMapSetFull(newSet);
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
        return new TypeMapSetFull(newSet);
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
    public boolean noInformationAbout(String[] formalParams){
        NEXT_MAP: for(TypeMap map : typeMapSet){
            for(String name : formalParams){
                AstType type = map.get(name);
                if(type==AstType.BOT) continue NEXT_MAP;
            }
            return false;
        }
        return true;
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