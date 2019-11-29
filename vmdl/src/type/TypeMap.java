/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package type;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.Map.Entry;
import java.lang.Error;

public class TypeMap{
    public static Map<String, AstType> global = new HashMap<>();
    private Map<String, AstType> dict;

    public TypeMap() {
        dict = new HashMap<String, AstType>();
    }

    private TypeMap(Map<String, AstType> _dict) {
        dict = _dict;
    }

    public AstType get(String key) {
        AstType type = dict.get(key);
        if(type == null){
            type = global.get(key);
        }
        return type;
    }

    public void add(String key, AstType value) {
        if(global.get(key) != null){
            throw new Error("InternalError: the element is already exist in global: "+key);
        }
        if(dict.get(key) != null){
            throw new Error("InternalError: the element is already exist: "+key);
        }
        dict.put(key, value);
    }

    public void assign(String key, AstType value) {
        if(global.get(key) != null){
            throw new Error("InternalError: the element is defined in global: "+key);
        }
        if(dict.get(key) == null){
            throw new Error("InternalError: the element is not exist: "+key);
        }
        dict.replace(key, value);
    }

    public TypeMap lub(TypeMap that){
        if(that == null) return clone();
        Map<String, AstType> newMap = new HashMap<>();
        for(Entry<String, AstType> entry : dict.entrySet()){
            String name = entry.getKey();
            AstType type = that.get(name).lub(entry.getValue());
            newMap.put(name, type);
        }
        return new TypeMap(newMap);
    }

    public boolean containsKey(String key) {
        boolean contain = dict.containsKey(key);
        if(contain) return true;
        return global.containsKey(key);
    }

    public Set<String> keySet() {
        return dict.keySet();
    }

    public TypeMap select(Collection<String> domain) {
        HashMap<String, AstType> newGamma = new HashMap<String, AstType>();

        for (String v : domain) {
            newGamma.put(v, dict.get(v));
        }
        return new TypeMap(newGamma);
    }

    public void update(String key, AstType value) {
        dict.replace(key, value);
    }

    public TypeMap getBottomDict() {
        Set<String> domain = dict.keySet();
        TypeMap result = new TypeMap();
        for (String v : domain) {
            result.add(v, AstType.BOT);
        }
        return result;
    }

    public boolean hasBottom() {
        for (AstType t: dict.values())
            if (t == AstType.BOT)
                return true;
        return false;
    }

    public static boolean addGlobal(String key, AstType value) {
        if(global.get(key) != null){
            return false;
        }
        global.put(key, value);
        return true;
    }

    @Override
    public TypeMap clone() {
        Map<String, AstType> newGamma = new HashMap<String, AstType>();
        for(String key : dict.keySet()){
            newGamma.put(key, dict.get(key));
        }
        return new TypeMap(newGamma);
    }

    @Override
    public String toString() {
        return dict.toString();
    }

    @Override
    public int hashCode(){
        return dict.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj || obj != null && obj instanceof TypeMap) {
            TypeMap tm = (TypeMap)obj;
            return (dict != null && tm.dict !=null && dict.equals(tm.dict));
        } else {
            return false;
        }
    }
}