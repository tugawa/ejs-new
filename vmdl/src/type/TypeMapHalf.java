package type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import type.AstType.AstBaseType;
import type.AstType.JSValueType;
import type.AstType.JSValueVMType;

public class TypeMapHalf extends TypeMapFull {
    Set<String> dispatchSet;

    public TypeMapHalf(){
        dictSet = new HashSet<>();
        dispatchSet = new HashSet<>();
        dictSet.add(new HashMap<String, AstType>());
    }

    public TypeMapHalf(Map<Map<String, AstType>, AstType> _exprTypeMap){
        super(_exprTypeMap);
        dictSet = new HashSet<>();
        dispatchSet = new HashSet<>();
        dictSet.add(new HashMap<String, AstType>());
    }
    public TypeMapHalf(Set<Map<String, AstType>> _dictSet, Set<String> _dispatchSet){
        dictSet = _dictSet;
        dispatchSet = _dispatchSet;
    }
    @Override
    protected boolean needDetailType(String name, AstType type){
        return ((dispatchSet.contains(name)) && (type instanceof JSValueType) && !(type instanceof JSValueVMType));
    }
    private Set<String> cloneDispatchSet(){
        return new HashSet<>(dispatchSet);
    }
    @Override
    public void addDispatch(String name){
        dispatchSet.add(name);
    }
    @Override
    public void clearDispatch(){
        dispatchSet = new HashSet<>();
    }
    @Override
    public Set<String> getDispatchSet(){
        return dispatchSet;
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
        return new TypeMapHalf(selectedSet, cloneDispatchSet());
    }
    @Override
    public TypeMapBase clone(){
        return new TypeMapHalf(cloneDictSet(dictSet), cloneDispatchSet());
    }
    private AstType getLubType(Set<AstType> set){
        AstType result = AstType.BOT;
        for(AstType t : set){
            if(t==AstType.BOT) continue;
            //if(!(t instanceof JSValueType)){
                if(t instanceof AstType.AstProductType){
                    result = t;
                    continue;
                }
                //throw new Error("type error :"+t.toString());
            //}
            result = result.lub(t);
        }
        return result;
    }
    @Override
    public TypeMapBase combine(TypeMapBase that){
        Set<Map<String, AstType>> newSet = new HashSet<>();
        Set<String> thatDispatchSet = that.getDispatchSet();
        Map<String, AstType> lubTypeMap = new HashMap<>();
        for(Map<String, AstType> m : dictSet){
            for(String s : m.keySet()){
                if(dispatchSet.contains(s)||thatDispatchSet.contains(s)) continue;
                Set<AstType> typeSet = this.get(s);
                typeSet.addAll(that.get(s));
                lubTypeMap.put(s, getLubType(typeSet));
            }
        }
        for(Map<String, AstType> m : dictSet){
            Map<String, AstType> newGamma = new HashMap<>();
            for(String s : m.keySet()){
                AstType lubType = lubTypeMap.get(s);
                if(lubType==null){
                    newGamma.put(s, m.get(s));
                }else{
                    newGamma.put(s, lubType);
                }
            }
            newSet.add(newGamma);
        }
        for(Map<String, AstType> m : that.getDictSet()){
            Map<String, AstType> newGamma = new HashMap<>();
            for(String s : m.keySet()){
                AstType lubType = lubTypeMap.get(s);
                if(lubType==null){
                    newGamma.put(s, m.get(s));
                }else{
                    newGamma.put(s, lubType);
                }
            }
            newSet.add(newGamma);
        }
        return new TypeMapHalf(newSet, cloneDispatchSet());
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
        return new TypeMapHalf(newSet, cloneDispatchSet());
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
        return new TypeMapHalf(newSet, cloneDispatchSet());
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
        return new TypeMapHalf(newSet, new HashSet<String>());
    }
    @Override
    public String toString() {
        return dictSet.toString()+", "+globalDict.toString();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj || obj != null && obj instanceof TypeMapHalf) {
            TypeMapHalf tm = (TypeMapHalf)obj;
            Set<Map<String, AstType>> tmDictSet = tm.getDictSet();
            return (dictSet != null && tmDictSet !=null && dictSet.equals(tmDictSet)) &&
                (exprTypeMap != null && tm.exprTypeMap != null && exprTypeMap.equals(tm.exprTypeMap));
        } else {
            return false;
        }
    }
}