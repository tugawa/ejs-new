package vmdlc;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;
import type.AstType;
import type.FunctionTable;
import type.FunctionAnnotation;
import type.AstType.AstAliasType;
import type.AstType.AstPairType;
import type.AstType.AstProductType;
import type.AstType.JSValueType;

public class ExternDeclarationGenerator {
    private static SyntaxTree getFunctionMeta(SyntaxTree node){
        if(!node.is(Symbol.unique("FunctionMeta"))){
            Tree<SyntaxTree>[] subTree = node.getSubTree();
            if(subTree == null) return null;
            for(Tree<SyntaxTree> tree : subTree){
                SyntaxTree result = getFunctionMeta((SyntaxTree)tree);
                if(result != null) return result;
            }
            return null;
        }
        return node;
    }
    public static String generate(SyntaxTree node){
        node = getFunctionMeta(node);
        if(!node.is(Symbol.unique("FunctionMeta"))){
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Tree<?> nameNode = node.get(Symbol.unique("name"));
        Tree<?> typeNode = node.get(Symbol.unique("type"));
        String name = nameNode.toText();
        AstType type = AstType.nodeToType((SyntaxTree)typeNode);
        if(!(type instanceof AstProductType)){
            throw new Error("Function is not function type");
        }
        AstProductType funType = (AstProductType)type;
        AstType funDomainType = funType.getDomain();
        List<AstType> varTypes;
        if(funDomainType instanceof AstPairType){
            AstPairType d = (AstPairType) funDomainType;
            varTypes = d.getTypes();
        }else{
            varTypes = new ArrayList<>(1);
            varTypes.add(funDomainType);
        }
        builder.append("extern ");
        String typeString;
        if(AstType.get("JSValue").isSuperOrEqual(funType.getRange())){
            typeString = "JSValue";
        }else{
            typeString = funType.getRange().toString();
            if(typeString.equals("cdouble")) typeString = "double";
            else if(typeString.equals("cstring")) typeString = "char*";
        }
        builder.append(typeString+" "+name+"(");
        int size = varTypes.size();
        if(!FunctionTable.contains(name)){
            throw new Error("FunctionTable is broken: not has "+name);
        }
        if(FunctionTable.hasAnnotations(name, FunctionAnnotation.needContext)){
            builder.append("Context*");
            if(size != 0){
                builder.append(", ");
            }
        }
        int i=0;
        while(true){
            AstType varType = varTypes.get(i);
            AstType realVarType;
            String typeName;
            if(varType instanceof AstAliasType){
                realVarType = AstType.get(((AstAliasType)varType).getCTypeName());
            }else{
                realVarType = varType;
            }
            if(AstType.get("JSValue").isSuperOrEqual(realVarType)){
                typeName = "JSValue";
            }else{
                typeName = realVarType.toString();
            }
            builder.append(typeName);
            i++;
            if(i>=size) break;
            builder.append(", ");
        }
        builder.append(");");
        builder.append("\n");
        return builder.toString();
    }
    public static String genereteOperandSpecCRequire(SyntaxTree node){
        node = getFunctionMeta(node);
        if(!node.is(Symbol.unique("FunctionMeta"))){
            return "";
        }
        Tree<?> nameNode = node.get(Symbol.unique("name"));
        Tree<?> typeNode = node.get(Symbol.unique("type"));
        String name = nameNode.toText();
        AstType type = AstType.nodeToType((SyntaxTree)typeNode);
        if(!(type instanceof AstProductType)){
            throw new Error("Function is not function type");
        }
        AstType[] funDomainTypes = ((AstProductType)type).getDomainAsArray();
        int length = funDomainTypes.length;
        StringBuilder builder = new StringBuilder();
        builder.append(name);
        builder.append(" (");
        String[] specifyChars = new String[length];
        for(int i=0; i<length; i++){
            if(funDomainTypes[i] instanceof JSValueType)
                specifyChars[i] = "_";
            else
                specifyChars[i] = "-";
        }
        builder.append(String.join(",", specifyChars));
        builder.append(") ");
        if(FunctionTable.hasAnnotations(name, FunctionAnnotation.calledFromC))
            builder.append("accept\n");
        else
            builder.append("unspecified\n");
        return builder.toString();
    }
}
