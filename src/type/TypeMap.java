package type;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Set;

import type.AstType.JSValueType;
import type.AstType.AstBaseType;

import java.lang.Error;

public class TypeMap {
    HashMap<String, AstType> dict;
    AstType exprType;

    public TypeMap() {
        dict = new HashMap<String, AstType>();
    }
    public TypeMap(HashMap<String, AstType> _dict) {
        dict = _dict;
    }
    public TypeMap(AstType _exprType) {
        exprType = _exprType;
    }

    public AstType get(String key) {
        return dict.get(key);
    }
    public void add(String key, AstType value) {
        dict.put(key, value);
    }
    public void add(VMDataTypeVecSet vtvs) {
        for (String vn: vtvs.getVarNames()) {
            AstType t = vtvs.getMostSpecificType(vn);
            add(vn, t);
        }
    }
    public Boolean containsKey(String key) {
        return dict.containsKey(key);
    }
    public Set<String> getKeys() {
        return dict.keySet();
    }
    public TypeMap select(Collection<String> domain) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        
        for (String v : domain) {
            newGamma.put(v, dict.get(v));
        }
        return new TypeMap(newGamma);
    }
    public TypeMap clone() {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        newGamma = (HashMap<String, AstType>)this.dict.clone();
        return new TypeMap(newGamma);
    }
    public void update(String key, AstType value) {
        dict.replace(key, value);
    }
    public AstType getExprType() {
        return exprType;
    }
    public void setExprType(AstType _exprType) {
        exprType = _exprType;
    }

    public TypeMap lub(TypeMap that) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        HashMap<String, AstType> thatDict = that.dict;
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
                    throw new Error("type error");
                else {
                    JSValueType jsvt1 = (JSValueType) t1;
                    JSValueType jsvt2 = (JSValueType) t2;
                    newGamma.put(v, jsvt1.lub(jsvt2));
                }
            }
        }
        return new TypeMap(newGamma);
    }
    private int indexOf(String[] varNames, String v) {
        for (int i = 0; i < varNames.length; i++) {
            if (varNames[i].equals(v)) {
                return i;
            }
        }
        return -1;
    }

    public TypeMap enterCase(String[] varNames, VMDataTypeVecSet caseCondition) {
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
        
        return new TypeMap(newGamma);
    }

    public TypeMap rematch(String[] params, String[] args, Set<String> domain) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        for (String v : domain) {
            int index = indexOf(params, v);
            if (index == -1) {
                newGamma.put(v, dict.get(v));
            } else {
                newGamma.put(v, dict.get(args[index]));
            }
        }
        return new TypeMap(newGamma);
    }

    public TypeMap getBottomDict() {
        Set<String> domain = getKeys();
        TypeMap result = new TypeMap();
        for (String v : domain) {
            result.add(v, AstType.BOT);
        }
        return result;
    }

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
                } else
                    throw new Error("internal error");
            }
            filtered.add(dts);
        }
        return filtered;
    }
    
    public boolean hasBottom() {
        for (AstType t: dict.values())
            if (t == AstType.BOT)
                return true;
        return false;
    }
    
    @Override
    public String toString() {
        return dict.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj ||
            obj != null && obj instanceof TypeMap) {
                TypeMap tm = (TypeMap)obj;
            return (dict != null && tm.dict !=null && dict.equals(tm.dict)) ||
                (exprType != null && tm.exprType != null && exprType.equals(tm.exprType));
        } else {
            return false;
        }
    }
}