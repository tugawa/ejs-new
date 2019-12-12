package vmdlc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import type.AstType;
import type.FunctionAnnotation;
import type.FunctionTable;
import type.TypeMap;
import type.AstType.AstBaseType;
import type.AstType.AstMappingType;
import type.AstType.AstProductType;
import vmdlc.ExternProcessVisitor.DefaultVisitor;

public class ExternProcessVisitor extends TreeVisitorMap<DefaultVisitor>{
    String currentFunctionName;
    public ExternProcessVisitor(){
        init(ExternProcessVisitor.class, new DefaultVisitor());
    }

    public String start(Tree<?> node){
        try{
            for (Tree<?> chunk : node){
                visit(chunk);
            }
            return currentFunctionName;
        }catch(Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
        }
    }

    private final void visit(Tree<?> node) throws Exception{
        find(node.getTag().toString()).accept(node);
    }

    public class DefaultVisitor{
        public void accept(Tree<?> node) throws Exception{
            for(Tree<?> seq : node){
                visit(seq);
            }
        }
    }

    public class FunctionMeta extends DefaultVisitor{
        private Set<FunctionAnnotation> nodeToAnnotations(Tree<?> annotationsNode){
            if(annotationsNode.countSubNodes() == 0){
                return Collections.emptySet();
            }
            Set<FunctionAnnotation> annotations = new HashSet<>();
            for(Tree<?> annotation : annotationsNode){
                FunctionAnnotation annotationEnum = FunctionAnnotation.valueOf(annotation.toText());
                annotations.add(annotationEnum);
            }
            return annotations;
        }
        @Override
        public void accept(Tree<?> node) throws Exception{
            Tree<?> nameNode = node.get(Symbol.unique("name"));
            Tree<?> typeNode = node.get(Symbol.unique("type"));
            Tree<?> annotationsNode = node.get(Symbol.unique("annotations"));
            Set<FunctionAnnotation> annotations = nodeToAnnotations(annotationsNode);
            if(annotations.contains(FunctionAnnotation.vmInstruction) && annotations.contains(FunctionAnnotation.makeInline)){
                ErrorPrinter.error("Function has annotations of \"vmInstruction\" and \"makeInline\"", (SyntaxTree)node);
            }
            String name = nameNode.toText();
            currentFunctionName = name;
            AstType type = AstType.nodeToType((SyntaxTree)typeNode);
            if(!(type instanceof AstProductType)){
                ErrorPrinter.error("Function is not function type", (SyntaxTree)typeNode);
            }
            if(!FunctionTable.contains(name)){
                FunctionTable.add(name, (AstProductType)type, annotations);
            }
        }
    }

    public class CFunction extends DefaultVisitor{
        private Set<FunctionAnnotation> nodeToAnnotations(Tree<?> annotationsNode){
            if(annotationsNode.countSubNodes() == 0){
                return Collections.emptySet();
            }
            Set<FunctionAnnotation> annotations = new HashSet<>();
            for(Tree<?> annotation : annotationsNode){
                FunctionAnnotation annotationEnum = FunctionAnnotation.valueOf(annotation.toText());
                annotations.add(annotationEnum);
            }
            return annotations;
        }
        @Override
        public void accept(Tree<?> node) throws Exception{
            Tree<?> nameNode = node.get(Symbol.unique("name"));
            Tree<?> typeNode = node.get(Symbol.unique("type"));
            Tree<?> annotationsNode = node.get(Symbol.unique("annotations"));
            Set<FunctionAnnotation> annotations = nodeToAnnotations(annotationsNode);
            if(annotations.contains(FunctionAnnotation.vmInstruction) && annotations.contains(FunctionAnnotation.makeInline)){
                ErrorPrinter.error("Function has annotations of \"vmInstruction\" and \"makeInline\"", (SyntaxTree)node);
            }
            String name = nameNode.toText();
            AstType type = AstType.nodeToType((SyntaxTree)typeNode);
            if(!(type instanceof AstProductType)){
                ErrorPrinter.error("Function is not function type", (SyntaxTree)typeNode);
            }
            if(FunctionTable.contains(name)){
                ErrorPrinter.error("Double define: "+name, (SyntaxTree)node);
            }
            FunctionTable.add(name, (AstProductType)type, annotations);
        }
    }

    public class CTypeDef extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node) throws Exception {
            Tree<?> typeNode = node.get(Symbol.unique("type"));
            Tree<?> varNode = node.get(Symbol.unique("var"));
            Tree<?> valueNode = node.get(Symbol.unique("value"));
            AstType type = AstType.nodeToType((SyntaxTree)typeNode);
            String typeName = varNode.toText();
            String cValue = valueNode.toText().replace("\\\"", "\"").replace("\\\\", "\\");
            if(!(type instanceof AstBaseType)){
                ErrorPrinter.error("Extern type must be basic type: "+type.toString(), (SyntaxTree)typeNode);
            }
            AstType.addAlias(typeName, (AstBaseType)type, cValue);
        }
    }

    public class CObjectmapping extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node) throws Exception {
            Tree<?> mappingTypeNode = node.get(Symbol.unique("type"));
            Tree<?> membersNode = node.get(Symbol.unique("members"));
            String mappingTypeName = mappingTypeNode.toText();
            AstMappingType mappingType = AstType.defineMappingType(mappingTypeName);
            for(Tree<?> memberNode : membersNode){
                Tree<?> typeNode = memberNode.get(Symbol.unique("type"));
                Tree<?> varNode = memberNode.get(Symbol.unique("var"));
                Set<String> annotations = Collections.emptySet();
                if(memberNode.has(Symbol.unique("annotations"))){
                    Tree<?> anotationsNode = memberNode.get(Symbol.unique("annotations"));
                    annotations = new HashSet<>();
                    annotations.add(anotationsNode.toText());
                }
                AstType type = AstType.nodeToType((SyntaxTree)typeNode);
                String name = varNode.toText();
                mappingType.addField(annotations, name, type);
            }
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node) throws Exception {
            Tree<?> typeNode = node.get(Symbol.unique("type"));
            Tree<?> varNode = node.get(Symbol.unique("var"));
            Tree<?> valueNode = node.get(Symbol.unique("value"));
            AstType type = AstType.nodeToType((SyntaxTree)typeNode);
            String varName = varNode.toText();
            String cValue = valueNode.toText().replace("\\\"", "\"").replace("\\\\", "\\");
            boolean isAdded = TypeMap.addGlobal(varName, type);
            if(!isAdded){
                ErrorPrinter.error("Double define: "+varName, (SyntaxTree)node);
            }
            AstToCVisitor.addCConstant(varName, cValue);
        }
    }
}