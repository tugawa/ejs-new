package type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import type.AstType.AstProductType;

public class FunctionTable{
    private static Map<String, FunctionInfo> functionMap = new HashMap<>();

    static{
        AstProductType topToVoidType = new AstProductType(AstType.get("Top"), AstType.get("void"));
        AstProductType voidToVoidType = new AstProductType(AstType.get("Top"), AstType.get("void"));
        put("GC_PUSH", topToVoidType, Collections.emptySet());
        put("GC_POP", topToVoidType, Collections.emptySet());
        put("builtin_prologue", voidToVoidType, Collections.emptySet());
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

    public static boolean equalsAnnotation(String name, Set<FunctionAnnotation> annotations){
        if(!contains(name)) return false;
        Set<FunctionAnnotation> targetsAnnotations = functionMap.get(name).annotations;
        return targetsAnnotations.equals(annotations);
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