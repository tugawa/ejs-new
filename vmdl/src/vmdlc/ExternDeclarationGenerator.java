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

public class ExternDeclarationGenerator {
    public String generate(SyntaxTree node){
        if(!node.is(Symbol.unique("FunctionMeta"))){
            Tree<SyntaxTree>[] subTree = node.getSubTree();
            if(subTree == null) return "";
            for(Tree<SyntaxTree> tree : subTree){
                String result = generate((SyntaxTree)tree);
                if(!result.equals("")) return result;
            }
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
}
