package type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import type.AstType.JSValueType;

public abstract class TypeMapBase {
    Map<Map<String, AstType>, AstType> exprTypeMap;

    public TypeMapBase(){
        exprTypeMap = new HashMap<>();
    }

    public TypeMapBase(Map<Map<String, AstType>, AstType> _exprTypeMap){
        exprTypeMap = _exprTypeMap;
    }

    public abstract Set<Map<String, AstType>> getDictSet();
    public abstract Set<AstType> get(String name);
    public abstract void addDispatch(String name);
    public abstract void clearDispatch();
    public abstract Set<String> getDispatchSet();
    public abstract void assign(String name, Map<Map<String, AstType>, AstType> exprTypeMap);
    public abstract void add(String name, AstType type);
    public abstract void add(String name, Map<Map<String,AstType>,AstType> map);
    public abstract void add(Map<String, AstType> map);
    public abstract void add(Set<Map<String, AstType>> set);
    public abstract boolean containsKey(String key);
    public abstract Set<String> getKeys();
    public abstract TypeMapBase select(Collection<String> domain);
    public abstract TypeMapBase clone();
    public Set<AstType> getExprType(Map<String, AstType> key) {
        Set<AstType> tempSet = new HashSet<>();
        for(Map<String, AstType> m : exprTypeMap.keySet()){
            boolean isInclude = true;
            for(String s : key.keySet()){
                if(m.containsKey(s)){
                    if(key.get(s).equals(m.get(s))){
                        continue;
                    }
                }
                isInclude = false;
                break;
            }
            if(isInclude){
                tempSet.add(exprTypeMap.get(m));
            }
        }
        return tempSet;
    }
    public Map<Map<String, AstType>, AstType> getExprTypeMap(){
        return exprTypeMap;
    }
    public Set<Map<String, AstType>> getKeySetValueOf(AstType t){
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String,AstType> m : exprTypeMap.keySet()){
            if(exprTypeMap.get(m).equals(t))newSet.add(m);
        }
        return newSet;
    }
    public void putExprTypeElement(Map<String, AstType> key, AstType type) {
        exprTypeMap.put(key, type);
    }
    public void setExprTypeMap(Map<Map<String, AstType>, AstType> map){
        exprTypeMap = map;
    }
    public static Map<Map<String,AstType>,AstType> getSimpleExprTypeMap(AstType type){
        Map<Map<String,AstType>,AstType> newMap = new HashMap<>();
        newMap.put(new HashMap<>(), type);
        return newMap;
    }
    public static Map<Map<String,AstType>,AstType> cloneExprTypeMap(Map<Map<String,AstType>,AstType> map){
        Map<Map<String,AstType>,AstType> newMap = new HashMap<>();
        for(Map<String,AstType> km : map.keySet()){
            Map<String,AstType> clonedKeyMap = new HashMap<>(km);
            newMap.put(clonedKeyMap, map.get(km));
        }
        return newMap;
    }
    public static boolean contains(Map<String,AstType> target, Map<String,AstType> cond){
        for(String s : cond.keySet()){
            if(!target.containsKey(s)) return false;
            AstType t = target.get(s);
            if(t instanceof JSValueType){
                if(!((JSValueType)t).isSuperOrEqual((JSValueType)cond.get(s))) return false;
            }else{
                if(t != cond.get(s)) return false;
            }
        }
        return true;
    }
    public abstract Map<Map<String, AstType>, AstType> 
    combineExprTypeMap(Map<Map<String, AstType>, AstType> exprTypeMap1, Map<Map<String, AstType>, AstType> exprTypeMap2);
    public abstract TypeMapBase combine(TypeMapBase that);
    public abstract TypeMapBase enterCase(String[] varNames, VMDataTypeVecSet caseCondition);
    public abstract TypeMapBase rematch(String[] params, String[] args, Set<String> domain);
    public abstract TypeMapBase getBottomDict();
    public abstract Set<VMDataType[]> filterTypeVecs(String[] formalParams, Set<VMDataType[]> vmtVecs);
    public abstract boolean hasBottom();
}
