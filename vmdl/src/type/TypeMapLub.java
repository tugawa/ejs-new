package type;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import type.AstType.JSValueType;

public class TypeMapLub extends TypeMapBase {
    Map<String, AstType> dict;
    Set<Map<String, AstType>> dictSet;

    public TypeMapLub(){
        dict = new HashMap<String, AstType>();
        dictSet = new HashSet<>();
        dictSet.add(dict);
    }
    public TypeMapLub(Map<String, AstType> _dict) {
        dict = _dict;
        dictSet = new HashSet<>();
        dictSet.add(dict);
    }
    @Override
    public Set<Map<String, AstType>> getDictSet() {
        return dictSet;
    }
    private boolean dictErrorCheck(String name){
        return (dict.containsKey(name) ^ globalDict.containsKey(name));
    }
    @Override
    public Set<AstType> get(String name){
        if(!dictErrorCheck(name)){
            throw new Error("Double declare : "+name);
        }
        Set<AstType> set = new HashSet<>();
        AstType type = dict.get(name);
        if(type == null){
            type = globalDict.get(name);
        }
        if(type==null){
            throw new Error("The variable not found : "+name);
        }
        set.add(type);
        return set;
    }
    @Override
    public void addDispatch(String name){}
    @Override
    public void clearDispatch(){}
    @Override
    public Set<String> getDispatchSet(){
        return new HashSet<String>(0);
    }
    @Override
    public void add(String name, AstType type){
        if(name != null){
            System.err.println("Warning : The variable is already declared : "+name); 
        }
        dict.put(name, type);
    }
    @Override
    public void add(Map<String, AstType> map){
        for(String name : map.keySet()){
            AstType type = dict.get(name);
            if(name != null){
                System.err.println("Warning : The variable is already declared : "+name); 
            }
            dict.put(name, type);
        }
    }
    @Override
    public void add(Set<Map<String, AstType>> set){
        Map<String, AstType> lubMap = new HashMap<>();
        for(Map<String, AstType> m : set){
            for(String name : m.keySet()){
                AstType type = m.get(name);
                AstType lubType = lubMap.get(name);
                if(lubType == null){
                    lubMap.put(name, type);
                }else{
                    lubMap.put(name, lubType.lub(type));
                }
            }
        }
        for(String name : lubMap.keySet()){
            AstType lubType = lubMap.get(name);
            dict.put(name, lubType);
        }
    }
    @Override
    public void add(String name, Map<Map<String, AstType>, AstType> map) {
        AstType type = AstType.BOT;
        for(Map<String,AstType> exprMap : map.keySet()){
            if(contains(dict, exprMap)){
                type = type.lub(map.get(exprMap));
            }
        }
        dict.put(name, type);
    }
    @Override
    public void assign(String name, Map<Map<String, AstType>, AstType> exprTypeMap) {
        Set<Map<String, AstType>> removeMap = new HashSet<>();
        Set<Map<String, AstType>> newSet = new HashSet<>();
        for(Map<String,AstType> exprMap : exprTypeMap.keySet()){
            if(contains(dict, exprMap)){
                Map<String,AstType> replacedDict = new HashMap<>();
                for(String s : dict.keySet()){
                    replacedDict.put(s, dict.get(s));
                }
                replacedDict.replace(name, exprTypeMap.get(exprMap));
                removeMap.add(dict);
                newSet.add(replacedDict);
            }
        }
        for(Map<String, AstType> map : dictSet){
            if(!removeMap.contains(map)){
                newSet.add(map);
            }
        }
        Map<String, AstType> newGamma = new HashMap<>();
        for(String s : dict.keySet()){
            newGamma.put(s, AstType.BOT);
        }
        for(Map<String, AstType> map : newSet){
            for(String s : map.keySet()){
                AstType t1 = newGamma.get(s);
                AstType t2 = map.get(s);
                if (t2 == null) {
                    throw new Error("inconsistent type environment: s = "+s);
                } else {
                    if (t1 == t2) {
                        newGamma.put(s, t1);
                    } else if (t1 == AstType.BOT) {
                        newGamma.put(s, t2);
                    } else if (t2 == AstType.BOT) {
                        newGamma.put(s, t1);
                    } else if (!(t1 instanceof JSValueType && t2 instanceof JSValueType))
                        throw new Error("type error");
                    else {
                        JSValueType jsvt1 = (JSValueType) t1;
                        JSValueType jsvt2 = (JSValueType) t2;
                        newGamma.put(s, jsvt1.lub(jsvt2));
                    }
                }
            }
        }
        dict = newGamma;
    }
    @Override
    public boolean containsKey(String key) {
        return (dict.containsKey(key) || globalDict.containsKey(key));
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
    public TypeMapBase select(Collection<String> domain) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        for (String v : domain) {
            AstType type = dict.get(v);
            if(type == null){
                throw new Error("No such element \""+v+"\"");
            }else{
                newGamma.put(v, type);
            }
        }
        return new TypeMapLub(newGamma);
    }
    @Override
    public TypeMapBase clone() {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        //newGamma = ((HashMap<String, AstType>)this.dict).clone();
        for(String s : dict.keySet()){
            newGamma.put(s, dict.get(s));
        }
        return new TypeMapLub(newGamma);
    }
    private Map<String, AstType> getLubDict(Set<Map<String, AstType>> _dictSet){
        HashMap<String, AstType> lubDict = new HashMap<>();

        for(Map<String, AstType> m : _dictSet){
            for(String k : m.keySet()){
                AstType t = lubDict.get(k);
                if(t==null){
                    lubDict.put(k, m.get(k));
                }else{
                    lubDict.replace(k, t.lub(m.get(k)));
                }
            }
        }
        return lubDict;
    }
    @Override
    public TypeMapBase combine(TypeMapBase that) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        Map<String, AstType> thatDict = getLubDict(that.getDictSet());
        for (String v : dict.keySet()) {
            AstType t1 = dict.get(v);
            AstType t2 = thatDict.get(v);
            if (t2 == null) {
                throw new Error("inconsistent type environment: v = "+v);
            } else {
                if (t1 == t2) {
                    newGamma.put(v, t1);
                } else if (t1 == AstType.BOT) {
                    newGamma.put(v, t2);
                } else if (t2 == AstType.BOT) {
                    newGamma.put(v, t1);
                } else if (!(t1 instanceof JSValueType && t2 instanceof JSValueType))
                    throw new Error("type error: t1="+t1+" t2="+t2);
                else {
                    JSValueType jsvt1 = (JSValueType) t1;
                    JSValueType jsvt2 = (JSValueType) t2;
                    newGamma.put(v, jsvt1.lub(jsvt2));
                }
            }
        }
        return new TypeMapLub(newGamma);
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
    public TypeMapBase enterCase(String[] varNames, VMDataTypeVecSet caseCondition) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();

        /* parameters */
        AstType[] paramTypes = new AstType[varNames.length];
        for (int i = 0; i < varNames.length; i++)
            paramTypes[i] = dict.get(varNames[i]);
        VMDataTypeVecSet.ByCommonTypes vtvs = new VMDataTypeVecSet.ByCommonTypes(varNames, paramTypes);
        vtvs = vtvs.intersection(caseCondition);
        for (int i = 0; i < varNames.length; i++) {
            AstType t = vtvs.getMostSpecificType(varNames[i]);
            newGamma.put(varNames[i], t);
        }
        
        /* add other variables */
        for (String v : dict.keySet()) {
            AstType t = dict.get(v);
            int index = indexOf(varNames, v);
            if (index == -1)
                newGamma.put(v, t);
        }
        
        return new TypeMapLub(newGamma);
    }
    @Override
    public TypeMapBase rematch(String[] params, String[] args, Set<String> domain) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        for (String v : domain) {
            int index = indexOf(params, v);
            if (index == -1) {
                newGamma.put(v, dict.get(v));
            } else {
                newGamma.put(v, dict.get(args[index]));
            }
        }
        return new TypeMapLub(newGamma);
    }
    @Override
    public TypeMapBase getBottomDict() {
        return new TypeMapLub(cloneDict(dict));
    }
    @Override
    public Set<VMDataType[]> filterTypeVecs(String[] formalParams, Set<VMDataType[]> vmtVecs) {
        Set<VMDataType[]> filtered = new HashSet<VMDataType[]>();
        NEXT_DTS: for (VMDataType[] dts: vmtVecs) {
            for (int i = 0; i < formalParams.length; i++) {
                VMDataType dt = dts[i];
                AstType xt = dict.get(formalParams[i]);
                if (xt instanceof JSValueType) {
                    JSValueType t = (JSValueType) xt;
                    if (!t.isSuperOrEqual(dt))
                        continue NEXT_DTS;
                } else if (xt == AstType.BOT){
                    continue NEXT_DTS;
                } else
                    throw new Error("internal error: "+formalParams[i]+"="+xt.toString());
            }
            filtered.add(dts);
        }
        return filtered;
    }
    @Override
    public boolean hasBottom() {
        for (AstType t: dict.values())
            if (t == AstType.BOT)
                return true;
        return false;
    }
    @Override
    public String toString() {
        return dict.toString()+", "+globalDict.toString();
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj ||
            obj != null && obj instanceof TypeMapLub) {
                TypeMapLub tm = (TypeMapLub)obj;
            return (dict != null && tm.dict !=null && dict.equals(tm.dict)) &&
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
}