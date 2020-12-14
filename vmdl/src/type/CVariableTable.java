package type;

import java.util.HashMap;
import java.util.Map;

public class CVariableTable {
    private static Map<String, AstType> variableMap = new HashMap<>();

    public static void put(String name, AstType type){
        if(variableMap.get(name) != null){
            System.err.println("InternalWarning: Duplicate CVariable define: "+name);
        }
        variableMap.put(name, type);
    }

    public static boolean contains(String name){
        return variableMap.containsKey(name);
    }

    public static AstType get(String name){
        return variableMap.get(name);
    }
}
