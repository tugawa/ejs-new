package type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import type.AstType.AstProductType;

public class FunctionTable{
    private static Map<String, FunctionInfo> functionMap = new HashMap<>();

    static{
        AstProductType type = new AstProductType(AstType.get("Top"), AstType.get("void"));
        put("GC_PUSH", type, Collections.emptySet());
        put("GC_POP", type, Collections.emptySet());
    }

    public static void put(String name, AstProductType type, Set<FunctionAnnotation> annotations){
        if(functionMap.get(name) != null){
            System.err.println("InternalWarning: Duplicate Function define: "+name);
        }
        functionMap.put(name, new FunctionInfo(type, annotations));
    }

    public static boolean contains(String name){
        return functionMap.containsKey(name);
    }

    public static AstProductType getType(String name){
        FunctionInfo info = functionMap.get(name);
        if(info == null){
            throw new Error("Cannot find function: "+name);
        }
        return info.type;
    }

    public static boolean hasAnnotations(String name, FunctionAnnotation annotation){
        return functionMap.get(name).annotations.contains(annotation);
    }

    private static class FunctionInfo{
        AstProductType type;
        Set<FunctionAnnotation> annotations;

        private FunctionInfo(AstProductType _type, Set<FunctionAnnotation> _annotations){
            type = _type;
            annotations = _annotations;
        }


    }
}