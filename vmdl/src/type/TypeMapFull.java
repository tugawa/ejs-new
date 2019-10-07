package type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import type.AstType.AstBaseType;
import type.AstType.JSValueType;

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
    public Set<Map<String, AstType>> getDictSet(){
        return dictSet;
    }
    public Set<AstType> get(String name){
        Set<AstType> typeSet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            AstType t = m.get(name);
            if(t==null) continue;
            typeSet.add(t);
        }
        return typeSet;
    }
    public void addDispatch(String name){}
    public void clearDispatch(){}
    public Set<String> getDispatchSet(){
        return new HashSet<String>(0);
    }
    public void add(String name, AstType type){
        for(Map<String, AstType> m : dictSet){
            if(m.get(name)==null){
                m.put(name, type);
            }else{
                m.replace(name, type);
            }
        }
    }
    public void add(Map<String, AstType> map){
        for(Map<String, AstType> m : dictSet){
            m.putAll(map);
        }
    }
    public void add(Set<Map<String, AstType>> set){
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String, AstType> m : set){
            for(Map<String, AstType> dsm : dictSet){
                Map<String, AstType> newGamma = new HashMap<>();
                newGamma.putAll(dsm);
                newGamma.putAll(m);
                newSet.add(newGamma);
            }
        }
        dictSet = newSet;
    }
    public boolean containsKey(String key){
        for(Map<String, AstType> m : dictSet){
            if(m.containsKey(key)) return true;
        }
        return false;
    }
    public Set<String> getKeys(){
        Set<String> keySet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            keySet.addAll(m.keySet());
        }
        return keySet;
    }
    public TypeMapBase select(Collection<String> domain){
        Set<Map<String, AstType>> selectedSet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            Map<String, AstType> selectedMap = new HashMap<>();
            for(String s : domain){
                AstType type = m.get(s);
                if(type==null){
                    if(containsKey(s)){
                        type = AstType.BOT;
                    }else
                        throw new Error("Failure select : no such element \""+s+"\"");
                }
                selectedMap.put(s, type);
            }
            selectedSet.add(selectedMap);
        }
        return new TypeMapFull(selectedSet, null);
    }
    @Override
    public TypeMapBase clone(){
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String, AstType> m : dictSet){
            Map<String, AstType> newGamma = new HashMap<>();
            for(String s : m.keySet()){
                newGamma.put(s, m.get(s));
            }
            newSet.add(newGamma);
        }
        return new TypeMapFull(newSet, null);
    }
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
                    }else
                        throw new Error("internal error :"+xt.toString());
                }
                filtered.add(dts);
                break;
            }
        }
        return filtered;
    }
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
        return dictSet.toString();
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
    public void assign(String name, Map<Map<String, AstType>, AstType> exprTypeMap) {
        Set<Map<String, AstType>> removeMap = new HashSet<>();
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String,AstType> exprMap : exprTypeMap.keySet()){
            for(Map<String,AstType> dictMap : dictSet){
                if(contains(dictMap, exprMap)){
                    Map<String,AstType> replacedMap = new HashMap<>();
                    for(String s : dictMap.keySet()){
                        replacedMap.put(s, dictMap.get(s));
                    }
                    replacedMap.replace(name, exprTypeMap.get(exprMap));
                    removeMap.add(dictMap);
                    newSet.add(replacedMap);
                }
            }
        }
        for(Map<String, AstType> map : dictSet){
            if(!removeMap.contains(map)){
                newSet.add(map);
            }
        }
        dictSet = newSet;
    }

    public void add(String name, Map<Map<String, AstType>, AstType> map) {
        Set<Map<String,AstType>> newDictSet = new HashSet<>();
        for(Map<String,AstType> exprMap : map.keySet()){
            for(Map<String,AstType> dictMap : dictSet){
                if(contains(dictMap, exprMap)){
                    Map<String,AstType> newMap = new HashMap<>();
                    for(String s : dictMap.keySet()){
                        newMap.put(s, dictMap.get(s));
                    }
                    newMap.put(name, map.get(exprMap));
                    newDictSet.add(newMap);
                }else{
                    newDictSet.add(dictMap);
                }
            }
        }
        dictSet = newDictSet;
    }

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
}