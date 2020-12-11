package vmdlc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

import nez.ast.Symbol;
import nez.ast.TreeVisitorMap;
import type.CConstantTable;
import type.FunctionAnnotation;
import type.FunctionTable;
import vmdlc.TriggerGCCheckVisitor.DefaultVisitor;
import vmdlc.TriggerGCCheckVisitor.BlockExpansionMap.BlockExpansionRequsets;

public class TriggerGCCheckVisitor extends TreeVisitorMap<DefaultVisitor> {
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

    static class BlockExpansionMap{
        static class BlockExpansionRequest{
            SyntaxTree target;
            SyntaxTree[] stmts;

            public BlockExpansionRequest(SyntaxTree target, SyntaxTree[] stmts){
                this.target = target;
                this.stmts = stmts;
            }

            public int size(){
                return stmts.length;
            }
        }
        static class BlockExpansionRequsets{
            //List<BlockExpansionRequest> requests = new ArrayList<>();
            Map<SyntaxTree, SyntaxTree[]> requests = new HashMap<>();

            public void put(SyntaxTree target, SyntaxTree[] stmts){
                requests.put(target, stmts);
            }
            public SyntaxTree[] get(SyntaxTree target){
                return requests.get(target);
            }
            public int size (){
                return requests.size();
            }
            public int requestSize(){
                int sum = 0;
                for(SyntaxTree[] stmts : requests.values()){
                    sum += stmts.length;
                }
                return sum;
            }
            public Collection<SyntaxTree> keySet(){
                return requests.keySet();
            }
        }
        
        Map<SyntaxTree, BlockExpansionRequsets> map = new HashMap<>();

        public void put(SyntaxTree key, BlockExpansionRequsets value){
            map.put(key, value);
        }
        public BlockExpansionRequsets get(SyntaxTree key){
            return map.get(key);
        }
        public Set<Entry<SyntaxTree, BlockExpansionRequsets>> entrySet(){
            return map.entrySet();
        }
        public static SyntaxTree generateExpandedBlock(SyntaxTree original, BlockExpansionRequsets requests){
            if(original.size() == 0) return original;
            SyntaxTree[] originalStmts = (SyntaxTree[]) original.getSubTree();
            int originalSize = originalStmts.length;
            int additionalSize = requests.requestSize() - requests.size();
            int expandedSize = originalSize + additionalSize;
            SyntaxTree[] expandedStmts = new SyntaxTree[expandedSize];
            int j = 0;
            for(int i=0; i<originalSize; i++){
                SyntaxTree[] stmts = requests.get(originalStmts[i]);
                if(stmts == null){
                    expandedStmts[j++] = originalStmts[i];
                    continue;
                }
                for(SyntaxTree stmt : stmts){
                    expandedStmts[j++] = stmt;
                }
            }
            return ASTHelper.generateBlock(expandedStmts);
        }
    }

    GCFunctionGenerator gcFunctionGenerator = new GCFunctionGenerator();
    BlockExpansionMap blockExpansionMap = new BlockExpansionMap();
    BlockExpansionRequsets currentRequestHandler;

    public TriggerGCCheckVisitor() {
        init(TriggerGCCheckVisitor.class, new DefaultVisitor());
    }

    public void start(ControlFlowGraphNode node) {
        try {
            Queue<ControlFlowGraphNode> queue = new ArrayDeque<>();
            queue.add(node);
            while(!queue.isEmpty()){
                ControlFlowGraphNode target = queue.remove();
                Collection<String> jsTypeVars = target.getJSTypeVars();
                Collection<String> newTailLive = new HashSet<>();
                Collection<ControlFlowGraphNode> nexts = target.getNext();
                for(ControlFlowGraphNode cfgn : nexts){
                    Collection<String> cfgnHeadLive = cfgn.getHeadLive();
                    if(cfgnHeadLive == null) continue;
                    newTailLive.addAll(cfgnHeadLive);
                }
                if(target.hasTailLive() && target.getTailLive().equals(newTailLive)) continue;
                target.setTailLive(new HashSet<>(newTailLive));
                currentRequestHandler = new BlockExpansionRequsets();
                SyntaxTree belongingBlock = target.getBelongingBlock();
                if(belongingBlock != null){
                    blockExpansionMap.put(target.getBelongingBlock(), currentRequestHandler);
                }
                List<SyntaxTree> stmts = target.getStatementList();
                int size = stmts.size();
                for(int i=size-1; i>=0; i--){
                    visit(stmts.get(i), newTailLive, jsTypeVars);
                }
                Collection<ControlFlowGraphNode> prevs = target.getPrev();
                for(ControlFlowGraphNode prev : prevs){
                    if(prev == ControlFlowGraphNode.enter) continue;
                    queue.add(prev);
                }
            }
            for(Entry<SyntaxTree, BlockExpansionRequsets> entry : blockExpansionMap.entrySet()){
                SyntaxTree originalBlock = entry.getKey();
                SyntaxTree expandedBlock = BlockExpansionMap.generateExpandedBlock(originalBlock, entry.getValue());
                if(originalBlock == expandedBlock) continue;
                // Expect originalBlock has no expand candidate
                originalBlock.addExpandedTreeCandidate(expandedBlock);
            }
        }catch(Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
        }
    }

    private final void visit(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
        find(node.getTag().toString()).accept(node, live, jsTypeVars);
    }

    private final boolean hasTriggerGC(SyntaxTree node) throws Exception{
        return find(node.getTag().toString()).findTriggerGC(node);
    }

    public class DefaultVisitor{
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
            for(SyntaxTree chunk : node){
                visit(chunk, live, jsTypeVars);
            }
        }
        public boolean findTriggerGC(SyntaxTree node) throws Exception{
            for(SyntaxTree chunk : node){
                if(hasTriggerGC(chunk)) return true;
            }
            return false;
        }
    }

    public class Statements extends DefaultVisitor{
        public void pushPopGenerate(SyntaxTree node, Collection<String> live){
            List<String> liveList = new ArrayList<>(live);
            int size = liveList.size();
            SyntaxTree[] stmts = new SyntaxTree[size*2+1];
            for(int i=0; i<size; i++){
                stmts[i]        = gcFunctionGenerator.getGCPushExprStmt(liveList.get(i));
                stmts[size*2-i] = gcFunctionGenerator.getGCPopExprStmt(liveList.get(i));
            }
            SyntaxTree nestSolved = ASTHelper.generateBlock(new NestGCTransformVisitor().start(node.dup()));
            stmts[size] = nestSolved;
            node.clearExpandedTreeCandidate();
            node.addExpandedTreeCandidate(ASTHelper.generateBlock(stmts));
        }
    }

    public class Declaration extends Statements{
        private SyntaxTree clipDeclaration(SyntaxTree node){
            SyntaxTree type = node.get(Symbol.unique("type"));
            SyntaxTree var = node.get(Symbol.unique("var"));
            return ASTHelper.generateDeclaration(type, var);
        }
        private SyntaxTree clipAssign(SyntaxTree node){
            SyntaxTree var = node.get(Symbol.unique("var"));
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            return ASTHelper.generateAssignment(var, expr);
        }
        @Override
        public void pushPopGenerate(SyntaxTree node, Collection<String> live){
            SyntaxTree[] declarationSeparated = new SyntaxTree[2];
            List<String> liveList = new ArrayList<>(live);
            int size = liveList.size();
            SyntaxTree[] stmts = new SyntaxTree[size*2+1];
            for(int i=0; i<size; i++){
                stmts[i]        = gcFunctionGenerator.getGCPushExprStmt(liveList.get(i));
                stmts[size*2-i] = gcFunctionGenerator.getGCPopExprStmt(liveList.get(i));
            }
            SyntaxTree nestSolved = ASTHelper.generateBlock(new NestGCTransformVisitor().start(clipAssign(node)));
            stmts[size] = nestSolved;
            declarationSeparated[0] = clipDeclaration(node);
            declarationSeparated[1] = ASTHelper.generateBlock(stmts);
            currentRequestHandler.put(node, declarationSeparated);
        }
        @Override
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
            SyntaxTree var = node.get(Symbol.unique("var"));
            String varName = var.toText();
            if(!node.has(Symbol.unique("expr"))){
                if(live.contains(varName)){
                    throw new Error("Illigal live variable analysis status");
                }
                return;
            }
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            live.remove(varName);
            if(expr == SyntaxTree.PHANTOM_NODE) return;
            visit(expr, live, jsTypeVars);
            pushPopGenerate(node, live);
        }
    }

    public class Assignment extends Statements{
        @Override
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
            SyntaxTree var = node.get(Symbol.unique("left"));
            SyntaxTree expr = node.get(Symbol.unique("right"));
            visit(expr, live, jsTypeVars);
            String varName = var.toText();
            live.remove(varName);
            pushPopGenerate(node, live);
        }
    }

    public class AssignmentPair extends Statements{
        @Override
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
            SyntaxTree pair = node.get(Symbol.unique("left"));
            SyntaxTree expr = node.get(Symbol.unique("right"));
            visit(expr, live, jsTypeVars);
            for(SyntaxTree var : pair){
                String varName = var.toText();
                live.remove(varName);
            }
            pushPopGenerate(node, live);
        }
    }

    public class ExpressionStatement extends Statements{
        @Override
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
            for(SyntaxTree chunk : node){
                visit(chunk, live, jsTypeVars);
            }
            pushPopGenerate(node, live);
        }
    }

    public class Name extends DefaultVisitor{
        private boolean isCConstant(String name){
            return CConstantTable.contains(name);
        }
        @Override
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
            String name = node.toText();
            // TODO: add condition that is variable externC when externC variable is implemented.
            if(!isCConstant(name) && jsTypeVars.contains(name)){
                live.add(name);
            }
        }
    }

    public class FieldAccess extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
        }
    }
    
    public class FunctionCall extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> live, Collection<String> jsTypeVars) throws Exception{
            SyntaxTree args = node.get(Symbol.unique("args"));
            visit(args, live, jsTypeVars);
        }
        @Override
        public boolean findTriggerGC(SyntaxTree node) throws Exception{
            String functionName = node.get(Symbol.unique("recv")).toText();
            return FunctionTable.hasAnnotations(functionName, FunctionAnnotation.triggerGC);
        }
    }
}