package vmdlc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.ast.Symbol;
import nez.ast.TreeVisitorMap;
import type.AstType;
import type.FunctionTable;
import type.AstType.AstBaseType;
import type.AstType.AstProductType;
import vmdlc.NestGCTransformVisitor.DefaultVisitor;
import vmdlc.NestGCTransformVisitor.GCTransformList.GCTransformRecord;

public class NestGCTransformVisitor extends TreeVisitorMap<DefaultVisitor> {
    static class GCFunctionGenerator{
        private Map<String, SyntaxTree> gcPushMap = new HashMap<>();
        private Map<String, SyntaxTree> gcPopMap = new HashMap<>();

        public SyntaxTree genFunctionCall(String functionName, String arg){
            return ASTHelper.generateExpressionStatement(ASTHelper.generateFunctionCall(functionName, new SyntaxTree[]{ASTHelper.generateName(arg)}));
        }
        public SyntaxTree getGCPushExprStmt(String name){
            SyntaxTree exprStmt = gcPushMap.get(name);
            if(exprStmt != null) return exprStmt;
            exprStmt = genFunctionCall("GC_PUSH", name);
            gcPushMap.put(name, exprStmt);
            return exprStmt;
        }
        public SyntaxTree getGCPopExprStmt(String name){
            SyntaxTree exprStmt = gcPopMap.get(name);
            if(exprStmt != null) return exprStmt;
            exprStmt = genFunctionCall("GC_POP", name);
            gcPopMap.put(name, exprStmt);
            return exprStmt;
        }
    }

    static class GCTransformList{
        static class GCTransformRecord{
            String varName;
            SyntaxTree[] declaration;

            private static int count = 0;
            private static String generateVariableName(){
                return "_arg_temp_" + (count++);
            }

            public GCTransformRecord(SyntaxTree expr, AstType type){
                varName = generateVariableName();
                declaration = new NestGCTransformVisitor().start(ASTHelper.generateDeclaration(type, varName, expr));
            }
            public SyntaxTree generateName(){
                return ASTHelper.generateName(varName);
            }
            public SyntaxTree[] generateDeclaration(){
                return declaration;
            }
            public int size(){
                return declaration.length;
            }
            public SyntaxTree generateGCPush(){
                return gcFunctionGenerator.getGCPushExprStmt(varName);
            }
            public SyntaxTree generateGCPop(){
                return gcFunctionGenerator.getGCPopExprStmt(varName);
            }
        }

        private List<GCTransformRecord> list = new ArrayList<>();

        public void add(GCTransformRecord record){
            list.add(record);
        }
        public SyntaxTree[] getDeclaration(int index){
            return list.get(index).generateDeclaration();
        }
        public int size(){
            int sum = 0;
            for(GCTransformRecord record : list){
                sum += record.size();
            }
            return sum;
        }
        public SyntaxTree getGCPush(int index){
            return list.get(index).generateGCPush();
        }
        public SyntaxTree getGCPop(int index){
            return list.get(index).generateGCPop();
        }
        // The stmt is expected that all functioncalls included it have been already replaced
        public SyntaxTree[] generateGCTransformed(SyntaxTree stmt){
            int recordSize = list.size();
            int declarationSize = size();
            SyntaxTree[] stmts = new SyntaxTree[recordSize*2+declarationSize+1];
            int j=0;
            for(int i=0; i<recordSize; i++){
                SyntaxTree[] declaration = getDeclaration(i);
                for(SyntaxTree node : declaration){
                    stmts[j++] = node;
                }
                stmts[j++] = getGCPush(i);
            }
            stmts[2*recordSize] = stmt;
            for(int i=0; i<recordSize; i++){
                stmts[recordSize*2+declarationSize-i] = getGCPop(i);
            }
            return stmts;
        }
    }

    static GCFunctionGenerator gcFunctionGenerator = new GCFunctionGenerator();
    GCTransformList transformList = new GCTransformList();

    public NestGCTransformVisitor() {
        init(NestGCTransformVisitor.class, new DefaultVisitor());
    }

    public SyntaxTree[] start(SyntaxTree node) {
        try {
            SyntaxTree newNode = visit(node);
            return transformList.generateGCTransformed(newNode);
        }catch(Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
        }
    }

    // For statements
    private final SyntaxTree visit(SyntaxTree node) throws Exception{
        return find(node.getTag().toString()).accept(node);
    }

    // For expressions
    private final SyntaxTree visit(SyntaxTree node, boolean triggerGCFlag) throws Exception{
        return find(node.getTag().toString()).accept(node, triggerGCFlag);
    }

    private final SyntaxTree visitByStmt(SyntaxTree node, boolean triggerGCFlag) throws Exception{
        return find(node.getTag().toString()).acceptFromStmt(node, triggerGCFlag);
    }

    private final boolean checkTriggerGC(SyntaxTree node){
        return find(node.getTag().toString()).isTriggerGC(node);
    }

    public class DefaultVisitor{
        // For statements
        public SyntaxTree accept(SyntaxTree node) throws Exception{
            for(SyntaxTree chunk : node){
                visit(chunk);
            }
            return node;
        }
        // For expressions
        public SyntaxTree accept(SyntaxTree node, boolean triggerGCFlag) throws Exception{
            if(!triggerGCFlag) return node;
            int size = node.size();
            SyntaxTree[] subTree = (SyntaxTree[]) node.getSubTree();
            SyntaxTree ret = node.dup();
            for(int i=0; i<size; i++){
                SyntaxTree replaced = visit(subTree[i], triggerGCFlag);
                if(replaced == subTree[i]) continue;
                ret.set(i, replaced);
            }
            return ret;
        }
        public SyntaxTree acceptFromStmt(SyntaxTree node, boolean triggerGCFlag) throws Exception{
            return accept(node, triggerGCFlag);
        }
        public boolean isTriggerGC(SyntaxTree node){
            for(SyntaxTree chunk : node){
                if(checkTriggerGC(chunk)) return true;
            }
            return false;
        }
    }
    
    //*****************************
    // Statements
    //*****************************

    public class Statements extends DefaultVisitor{
        public SyntaxTree visit_(SyntaxTree expr) throws Exception{
            boolean triggerGCFlag = checkTriggerGC(expr);
            return visitByStmt(expr, triggerGCFlag);

        }
    }

    public class Assignment extends Statements{
        @Override
        public SyntaxTree accept(SyntaxTree node) throws Exception{
            SyntaxTree expr = node.get(Symbol.unique("right"));
            SyntaxTree newExpr = visit_(expr);
            SyntaxTree ret = node.dup();
            ret.set(Symbol.unique("right"), newExpr);
            return ret;
        }
    }
    
    public class AssignmentPair extends Statements{
        @Override
        public SyntaxTree accept(SyntaxTree node) throws Exception{
            SyntaxTree expr = node.get(Symbol.unique("right"));
            SyntaxTree newExpr = visit_(expr);
            SyntaxTree ret = node.dup();
            ret.set(Symbol.unique("right"), newExpr);
            return ret;
        }
    }
    
    public class ExpressionStatement extends Statements{
        @Override
        public SyntaxTree accept(SyntaxTree node) throws Exception{
            SyntaxTree expr = node.get(0);
            SyntaxTree newExpr = visit_(expr);
            SyntaxTree ret = node.dup();
            ret.set(0, newExpr);
            return ret;
        }
    }
    
    public class Declaration extends Statements{
        @Override
        public SyntaxTree accept(SyntaxTree node) throws Exception{
            if(!node.has(Symbol.unique("expr"))) return node;
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            if(expr == SyntaxTree.PHANTOM_NODE) return node;
            SyntaxTree newExpr = visit_(expr);
            SyntaxTree ret = node.dup();
            ret.set(Symbol.unique("expr"), newExpr);
            return ret;
        }
    }

    //*****************************
    // Expressions
    //*****************************

    public class Name extends DefaultVisitor{
        @Override
        public SyntaxTree accept(SyntaxTree node, boolean triggerGCFlag) throws Exception{
            return node;
        }
        @Override
        public boolean isTriggerGC(SyntaxTree node){
            return false;
        }
    }

    public class FunctionCall extends DefaultVisitor{
        @Override
        public SyntaxTree accept(SyntaxTree node, boolean triggerGCFlag) throws Exception{
            if(!triggerGCFlag) return node;
            AstProductType type = FunctionTable.getType(node.get(Symbol.unique("recv")).toText());
            AstType rangeType = type.getRange();
            if(!(rangeType instanceof AstBaseType)){
                throw new Error("The range type of FunctionCall "+node.get(Symbol.unique("recv")).toText()+" is "+rangeType.toString()+", AstBaseType is expected");
            }
            if(!((AstBaseType)rangeType).isRequiredGCPushPop()){
                //return node;
                return super.accept(node, triggerGCFlag);
            }
            GCTransformRecord record = new GCTransformRecord(node, rangeType);
            transformList.add(record);
            return record.generateName();
        }
        @Override
        public SyntaxTree acceptFromStmt(SyntaxTree node, boolean triggerGCFlag) throws Exception{
            return super.accept(node, triggerGCFlag);
        }
        @Override
        public boolean isTriggerGC(SyntaxTree node){
            return FunctionTable.hasAnnotations(node.get(Symbol.unique("recv")).toText(), type.FunctionAnnotation.triggerGC);
        }
    }
}