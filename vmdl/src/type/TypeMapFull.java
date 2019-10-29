package type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import type.AstType.AstBaseType;
import type.AstType.JSValueType;
import type.AstType.JSValueVMType;

public class TypeMapFull extends TypeMapBase {
    Set<Map<String, AstType>> dictSet;

    public TypeMapFull(){
        dictSet = new HashSet<>();
        dictSet.add(new HashMap<String, AstType>());
    }

    public TypeMapFull(Map<Map<String, AstType>, AstType> _exprTypeMap){
        super(_exprTypeMap);
        dictSet = new HashSet<>();
        dictSet.add(new HashMap<String, AstType>());
    }
    public TypeMapFull(Set<Map<String, AstType>> _dictSet, Set<String> _dispatchSet){
        dictSet = _dictSet;
    }
    @Override
    public Set<Map<String, AstType>> getDictSet(){
        return dictSet;
    }
    private boolean isDoubleDeclare(String name){
        boolean flag = false;
        for(Map<String, AstType> m : dictSet){
            if(m.containsKey(name)){
                flag = true;
                break;
            }
        }
        return (flag && globalDict.containsKey(name));
    }
    @Override
    public Set<AstType> get(String name){
        if(isDoubleDeclare(name)){
            throw new Error("Internal Error : The element is declare in local and global dict \""+name+"\"");
        }
        Set<AstType> typeSet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            AstType t = m.get(name);
            if(t == null) continue;
            typeSet.add(t);
        }
        AstType t = globalDict.get(name);
        if(t != null){
            typeSet.add(t);
        }
        if(typeSet.isEmpty()){
            throw new Error("The variable not found : "+name);
        }
        return typeSet;
    }
    @Override
    public void addDispatch(String name){}
    @Override
    public void clearDispatch(){}
    @Override
    public Set<String> getDispatchSet(){
        return new HashSet<String>(0);
    }
    protected boolean needDetailType(String name, AstType type){
        return ((type instanceof JSValueType) && !(type instanceof JSValueVMType));
    }
    private void detailPut(Set<Map<String, AstType>> set, Map<String, AstType> original, String name, AstType type){
        if(needDetailType(name, type)){
            for(JSValueVMType t : JSValueType.getChildren((JSValueType)type)){
                Map<String, AstType> tempMap = cloneDict(original);
                tempMap.put(name, t);
                set.add(tempMap);
            }
            set.remove(original);
        }else{
            original.put(name, type);
        }
    }
    private void detailAdd(Set<Map<String, AstType>> set, Map<String, AstType> original, String name, AstType type){
        if(original.get(name)!=null){
            System.err.println("Warning : The variable is already declared : "+name); 
        }
        detailPut(set, original, name, type);
    }
    private void detailAssign(Set<Map<String, AstType>> set, Map<String, AstType> original, String name, AstType type){
        if(original.get(name)==null){
            throw new Error("The variable is not declared : "+name); 
        }
        detailPut(set, original, name, type);
    }
    @Override
    public void add(String name, AstType type){
        for(Map<String, AstType> m : dictSet){
            detailAdd(dictSet, m, name, type);
        }
    }
    @Override
    public void add(Map<String, AstType> map){
        for(String name : map.keySet()){
            AstType type = map.get(name);
            add(name, type);
        }
    }
    private Set<Map<String,AstType>> getPutSet(Set<Map<String, AstType>> set, Map<String, AstType> map){
        Set<Map<String,AstType>> newSet = cloneDictSet(set);
        for(Map<String, AstType> originalMap : newSet){
            for(String name : map.keySet()){
                AstType type = map.get(name);
                detailAdd(newSet, originalMap, name, type);
            }
        }
        return newSet;
    }
    @Override
    public void add(Set<Map<String, AstType>> set){
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String, AstType> m : set){
            newSet.addAll(getPutSet(dictSet, m));
        }
        dictSet = newSet;
    }
    @Override
    public void add(String name, Map<Map<String, AstType>, AstType> map) {
        Set<Map<String,AstType>> newDictSet = new HashSet<>();
        for(Map<String,AstType> exprMap : map.keySet()){
            for(Map<String,AstType> dictMap : dictSet){
                if(contains(dictMap, exprMap)){
                    Map<String,AstType> newMap = cloneDict(dictMap);
                    AstType type = map.get(exprMap);
                    newDictSet.add(newMap);
                    detailAdd(newDictSet, newMap, name, type);
                }
            }
        }
        dictSet = newDictSet;
    }
    @Override
    public void assign(String name, Map<Map<String, AstType>, AstType> exprTypeMap) {
        Set<Map<String, AstType>> removeMaps = new HashSet<>();
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String,AstType> exprMap : exprTypeMap.keySet()){
            for(Map<String,AstType> dictMap : dictSet){
                if(contains(dictMap, exprMap)){
                    AstType replacedType = exprTypeMap.get(exprMap);
                    detailAssign(newSet, dictMap, name, replacedType);
                    removeMaps.add(dictMap);
                }
            }
        }
        for(Map<String, AstType> map : dictSet){
            if(!removeMaps.contains(map)){
                newSet.add(map);
            }
        }
        dictSet = newSet;
    }
    @Override
    public boolean containsKey(String key){
        for(Map<String, AstType> m : dictSet){
            if(m.containsKey(key)) return true;
        }
        if(globalDict.containsKey(key)) return true;
        return false;
    }
    @Override
    public Set<String> getKeys(){
        Set<String> keySet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            keySet.addAll(m.keySet());
        }
        return keySet;
    }
    @Override
    public TypeMapBase select(Collection<String> domain){
        Set<Map<String, AstType>> selectedSet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            Map<String, AstType> selectedMap = new HashMap<>();
            for(String s : domain){
                AstType type = m.get(s);
                if(type==null){
                    if(containsKey(s)){
                        type = AstType.BOT;
                    }else{
                        throw new Error("No such element \""+s+"\"");
                    }
                }
                selectedMap.put(s, type);
            }
            selectedSet.add(selectedMap);
        }
        return new TypeMapFull(selectedSet, null);
    }
    @Override
    public TypeMapBase clone(){
        return new TypeMapFull(cloneDictSet(dictSet), null);
    }
    @Override
    public TypeMapBase combine(TypeMapBase that){
        Set<Map<String, AstType>> newSet = new HashSet<>();
        Set<Map<String, AstType>> thatDictSet = that.getDictSet();
        for(Map<String, AstType> m : dictSet){
            Map<String, AstType> newGamma = new HashMap<>();
            newGamma.putAll(m);
            newSet.add(newGamma);
        }
        for(Map<String, AstType> m : thatDictSet){
            Map<String, AstType> newGamma = new HashMap<>();
            newGamma.putAll(m);
            newSet.add(newGamma);
        }
        return new TypeMapFull(newSet, null);
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
    public TypeMapBase enterCase(String[] varNames, VMDataTypeVecSet caseCondition){
        Set<VMDataType[]> conditionSet = caseCondition.getTuples();
        Set<Map<String, AstType>> newSet = new HashSet<>();
        if(conditionSet.isEmpty()){
            Map<String, AstType> newGamma = new HashMap<>();
            Set<String> keys = getKeys();
            for(String s : keys){
                newGamma.put(s, AstBaseType.BOT);
            }
            newSet.add(newGamma);
        }else{
            for(VMDataType[] v : conditionSet){
                int length = varNames.length;
                NEXT_MAP: for(Map<String, AstType> m : dictSet){
                    for(int i=0; i<length; i++){
                        if(!(m.get(varNames[i]) instanceof JSValueType)) continue NEXT_MAP;
                        if(!((JSValueType)AstType.get(v[i])).isSuperOrEqual((JSValueType)m.get(varNames[i]))){
                            continue NEXT_MAP;
                        }
                    }
                    Map<String, AstType> newGamma = new HashMap<>();
                    newGamma.putAll(m);
                    newSet.add(newGamma);
                }
            }
            if(newSet.isEmpty()){
                Map<String, AstType> newGamma = new HashMap<>();
                Set<String> keys = getKeys();
                for(String s : keys){
                    newGamma.put(s, AstBaseType.BOT);
                }
                newSet.add(newGamma);
            }
        }
        return new TypeMapFull(newSet, null);
    }
    @Override
    public TypeMapBase rematch(String[] params, String[] args, Set<String> domain){
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            Map<String, AstType> newGamma = new HashMap<>();
            for (String v : domain) {
                int index = indexOf(params, v);
                if (index == -1) {
                    newGamma.put(v, m.get(v));
                } else {
                    newGamma.put(v, m.get(args[index]));
                }
            }
            newSet.add(newGamma);
        }
        return new TypeMapFull(newSet, null);
    }
    @Override
    public TypeMapBase getBottomDict(){
        Set<String> domain = getKeys();
        Map<String, AstType> newGamma = new HashMap<>();
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for (String v : domain) {
            newGamma.put(v, AstType.BOT);
        }
        newSet.add(newGamma);
        return new TypeMapFull(newSet, null);
    }
    @Override
    public Set<VMDataType[]> filterTypeVecs(String[] formalParams, Set<VMDataType[]> vmtVecs){
        Set<VMDataType[]> filtered = new HashSet<VMDataType[]>();
        int length = formalParams.length;
        for(VMDataType[] dts : vmtVecs){
            NEXT_MAP: for(Map<String, AstType> m : dictSet){
                for(int i=0; i<length; i++){
                    VMDataType dt = dts[i];
                    AstType xt = m.get(formalParams[i]);
                    if(xt==null) continue NEXT_MAP;
                    if(xt instanceof JSValueType){
                        JSValueType t = (JSValueType)xt;
                        if(!t.isSuperOrEqual(JSValueType.get(dt))) continue NEXT_MAP;
                    } else if (xt == AstType.BOT){
                        continue NEXT_MAP;
                    } else
                        throw new Error("internal error: "+formalParams[i]+"="+xt.toString());
                    }
                filtered.add(dts);
                break;
            }
        }
        return filtered;
    }
    @Override
    public boolean hasBottom(){
        for(Map<String, AstType> m : dictSet){
            for(AstType t : m.values()){
                if(t==AstType.BOT) return true;
            }
        }
        return false;
    }
    @Override
    public String toString() {
        return dictSet.toString()+", "+globalDict.toString();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj || obj != null && obj instanceof TypeMapFull) {
            TypeMapFull tm = (TypeMapFull)obj;
            Set<Map<String, AstType>> tmDictSet = tm.getDictSet();
            return (dictSet != null && tmDictSet !=null && dictSet.equals(tmDictSet)) &&
                (exprTypeMap != null && tm.exprTypeMap != null && exprTypeMap.equals(tm.exprTypeMap));
        } else {
            return false;
        }
    }
    @Override
    public Map<Map<String, AstType>, AstType> combineExprTypeMap(Map<Map<String, AstType>, AstType> exprTypeMap1, Map<Map<String, AstType>, AstType> exprTypeMap2) {
        Map<Map<String, AstType>, AstType> newExprTypeMap = new HashMap<>();
        for(Map<String,AstType> map : exprTypeMap1.keySet()){
            newExprTypeMap.put(map, exprTypeMap1.get(map));
        }
        for(Map<String,AstType> map : exprTypeMap2.keySet()){
            newExprTypeMap.put(map, exprTypeMap2.get(map));
        }
        return newExprTypeMap;
    }
    @Override
    public void putExprTypeElement(Map<String, AstType> key, AstType type) {
        if((type instanceof JSValueType) && !(type instanceof JSValueVMType)){
            for(JSValueVMType t : AstType.getChildren((JSValueType)type)){
                exprTypeMap.put(key, t);
            }
        }else{
            exprTypeMap.put(key, type);
        }
    }
}