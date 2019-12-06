package type;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import type.AstType.AstProductType;

public class FunctionTable{
    private static Map<String, FunctionInfo> functionMap = new HashMap<>();

    public static void add(String name, AstProductType type, Set<FunctionAnnotation> annotations){
        if(functionMap.get(name) != null){
            throw new Error("Double define");
        }
        functionMap.put(name, new FunctionInfo(type, annotations));
    }

    public static boolean contains(String name){
        return functionMap.containsKey(name);
    }

    public static AstProductType getType(String name){
        FunctionInfo info = functionMap.get(name);
        if(info == null){
            throw new Error("No such name:"+name);
        }
        return info.type;
    }

    /*
    public static Set<FunctionAnnotation> getAnnotations(String name){
        return functionMap.get(name).annotations;
    }
    */

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