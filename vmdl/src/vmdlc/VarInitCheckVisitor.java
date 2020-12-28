package vmdlc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import type.AstType;
import type.CConstantTable;
import type.CVariableTable;
import type.TypeMap;
import type.TypeMapSet;
import type.AstType.JSValueType;
import vmdlc.VarInitCheckVisitor.DefaultVisitor;
import vmdlc.VarInitCheckVisitor.CFGNStack.CFGNRecord;

public class VarInitCheckVisitor extends TreeVisitorMap<DefaultVisitor> {

    static class CFGNStack{
        static class CFGNRecord{
            private ControlFlowGraphNode node;
            private Collection<String> initialized;

            public CFGNRecord(ControlFlowGraphNode node, Collection<String> initialized){
                this.node = node;
                this.initialized = initialized;
            }
            public ControlFlowGraphNode getNode(){
                return node;
            }
            public Collection<String> getInitialized(){
                return initialized;
            }
        }

        Stack<CFGNRecord> stack = new Stack<>();

        public void push(ControlFlowGraphNode node, Collection<String> initialized){
            stack.push(new CFGNRecord(node, initialized));
        }
        public CFGNRecord pop(){
            return stack.pop();
        }
        public boolean isEmpty(){
            return stack.isEmpty();
        }
        public int size(){
            return stack.size();
        }
    }
    public VarInitCheckVisitor() {
        init(VarInitCheckVisitor.class, new DefaultVisitor());
    }

    public void start(ControlFlowGraphNode node) {
        try {
            CFGNStack stack = new CFGNStack();
            stack.push(node, new HashSet<String>(0));
            while(!stack.isEmpty()){
                CFGNRecord record = stack.pop();
                ControlFlowGraphNode target = record.getNode();
                Collection<String> recordedInitialized = target.selectValidAtHead(record.getInitialized());
                if(target.isSetInitialized()){
                    Collection<String> expectedInitialized = target.getInitialized();
                    if(recordedInitialized.equals(expectedInitialized)) continue;
                    SyntaxTree targetHead = target.getHeadStatement();
                    if(targetHead == null)
                        ErrorPrinter.error("Illigal initialize state");
                    else
                        ErrorPrinter.error("Illigal initialize state", targetHead);
                }else{
                    target.setInitialized(new HashSet<>(recordedInitialized));
                }
                Collection<String> newInitialized = new HashSet<>(recordedInitialized);
                for(SyntaxTree stmt : target){
                    visit(stmt, newInitialized);
                }
                Collection<ControlFlowGraphNode> nexts = target.getNext();
                for(ControlFlowGraphNode next : nexts){
                    if(next == ControlFlowGraphNode.exit) continue;
                    stack.push(next, newInitialized);
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
        }
    }

    private final void visit(SyntaxTree node, Collection<String> initialized) throws Exception{
        find(node.getTag().toString()).accept(node, initialized);
    }

    public class DefaultVisitor{
        protected boolean isJSValueType(String name, TypeMapSet dict){
            for(TypeMap typeMap : dict){
                AstType type = typeMap.get(name);
                if(type instanceof JSValueType) return true;
            }
            return false;
        }
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            for(SyntaxTree chunk : node){
                visit(chunk, initialized);
            }
        }
    }

    public class Declaration extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            SyntaxTree var = node.get(Symbol.unique("var"));
            String varName = var.toText();
            if(!node.has(Symbol.unique("expr"))) return;
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            /* expr is null when the declaration is introduce part of FunctionMeta */
            if(expr != SyntaxTree.PHANTOM_NODE){
                visit(expr, initialized);
            }
            initialized.add(varName);
        }
    }

    public class Assignment extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            SyntaxTree var = node.get(Symbol.unique("left"));
            SyntaxTree expr = node.get(Symbol.unique("right"));
            visit(expr, initialized);
            String varName = var.toText();
            if(initialized.contains(varName) && isJSValueType(varName, node.getHeadDict())){
                ErrorPrinter.error("Duplicate variable initalization", node);
            }
            initialized.add(varName);
        }
    }

    public class AssignmentPair extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            SyntaxTree pair = node.get(Symbol.unique("left"));
            SyntaxTree expr = node.get(Symbol.unique("right"));
            visit(expr, initialized);
            for(SyntaxTree var : pair){
                String varName = var.toText();
                if(initialized.contains(varName) && isJSValueType(varName, node.getHeadDict())){
                    ErrorPrinter.error("Duplicate variable initalization", node);
                }
                initialized.add(varName);
            }
        }
    }

    public class SpecialExpression extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            SyntaxTree expr = node.get(0);
            visit(expr, initialized);
        }
    }

    public class Rematch extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            Tree<SyntaxTree>[] subTree = node.getSubTree();
            int size = subTree.length;
            for(int i=1; i<size; i++){ // skip subTree[0] (label node)
                visit((SyntaxTree)subTree[i], initialized);
            }
        }
    }

    public class Name extends DefaultVisitor{
        private boolean isExternC(String name){
            return CConstantTable.contains(name) || CVariableTable.contains(name);
        }
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            /* Check if is the variable initialized */
            String name = node.toText();
            if(initialized.contains(name) || isExternC(name)) return;
            ErrorPrinter.error("Variable must be initialized before use.", node);
        }
    }

    public class FieldAccess extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
        }
    }
    
    public class FunctionCall extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> initialized) throws Exception{
            SyntaxTree args = node.get(Symbol.unique("args"));
            visit(args, initialized);
        }
    }
}