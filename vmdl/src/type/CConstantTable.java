package type;

import java.util.HashMap;
import java.util.Map;

public class CConstantTable {
    private static Map<String, String> constantMap = new HashMap<>();

    public static void put(String name, String cValue){
        if(constantMap.get(name) != null){
            throw new Error("Double define:"+name);
        }
        constantMap.put(name, cValue);
    }

    public static boolean contains(String name){
        return constantMap.containsKey(name);
    }

    public static String get(String name){
        return constantMap.get(name);
    }
}
