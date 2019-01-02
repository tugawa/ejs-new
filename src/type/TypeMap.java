package type;

import java.util.HashMap;
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
                throw new Error("inconsistent type environment");
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

    public TypeMap enterCase(String[] varNames, Set<VMDataType[]> caseCondition) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();
        for (String v : dict.keySet()) {
            AstType t1 = dict.get(v);
            int index = indexOf(varNames, v);
            if (index == -1) {
                newGamma.put(v, t1);
            } else {
                AstType t2 = AstType.BOT;
                for (VMDataType[] dts : caseCondition) {
                    AstType glb = t1.glb(AstType.get(dts[index]));
                    t2 = t2.lub(glb);
                }
                newGamma.put(v, t2);
            }
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
    public String toString() {
        return dict.toString();
    }
    public TypeMap getBottomDict() {
        Set<String> domain = getKeys();
        TypeMap result = new TypeMap();
        for (String v : domain) {
            result.add(v, AstType.BOT);
        }
        return result;
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