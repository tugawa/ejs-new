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

import nez.ast.Symbol;
import nez.ast.TreeVisitorMap;
import type.AstType;
import type.CConstantTable;
import type.FunctionAnnotation;
import type.FunctionTable;
import type.TypeMap;
import type.TypeMapSet;
import type.AstType.AstArrayType;
import type.AstType.AstBaseType;
import vmdlc.Option.CompileMode;
import vmdlc.TriggerGCCheckVisitor.DefaultVisitor;
import vmdlc.TriggerGCCheckVisitor.BlockExpansionMap.BlockExpansionRequsets;
import vmdlc.TriggerGCCheckVisitor.BlockExpansionMap.Entry;

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
        static class Entry{
            private SyntaxTree key;
            private BlockExpansionRequsets value;

            public Entry(SyntaxTree key, BlockExpansionRequsets value){
                this.key = key;
                this.value = value;
            }
            public SyntaxTree getKey(){
                return key;
            }
            public BlockExpansionRequsets getValue(){
                return value;
            }
        }
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

        List<Entry> list = new ArrayList<>();

        public void put(SyntaxTree key, BlockExpansionRequsets value){
            list.add(new Entry(key, value));
        }
        public BlockExpansionRequsets get(SyntaxTree key){
            for(Entry entry : list){
                if(key == entry.getKey())
                    return entry.getValue();
            }
            return null;
        }
        public Collection<Entry> entryCollection(){
            return list;
        }
        public static SyntaxTree generateExpandedBlock(SyntaxTree original, BlockExpansionRequsets requests){
            if(original == null || original.size() == 0) return original;
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
    BlockExpansionRequsets currentRequests;
    CompileMode compileMode;
    Collection<AstType> exceptType;

    public TriggerGCCheckVisitor() {
        init(TriggerGCCheckVisitor.class, new DefaultVisitor());
    }

    public void start(ControlFlowGraphNode node, CompileMode compileMode) {
        this.compileMode = compileMode;
        exceptType = new HashSet<>();
        exceptType.add(AstType.get("Fixnum"));
        exceptType.add(AstType.get("Special"));
        try {
            Queue<ControlFlowGraphNode> queue = new ArrayDeque<>();
            queue.add(node);
            while(!queue.isEmpty()){
                ControlFlowGraphNode target = queue.remove();
                Collection<String> newTailLive = new HashSet<>();
                Collection<ControlFlowGraphNode> nexts = target.getNext();
                for(ControlFlowGraphNode cfgn : nexts){
                    Collection<String> cfgnHeadLive = cfgn.getHeadLive();
                    if(cfgnHeadLive == null) continue;
                    newTailLive.addAll(cfgnHeadLive);
                }
                if(target.hasTailLive() && target.getTailLive().equals(newTailLive)) continue;
                target.setTailLive(new HashSet<>(newTailLive));
                currentRequests = new BlockExpansionRequsets();
                SyntaxTree belongingBlock = target.getBelongingBlock();
                if(belongingBlock != null){
                    BlockExpansionRequsets recorded = blockExpansionMap.get(belongingBlock);
                    if(recorded != null)
                        currentRequests = recorded;
                    blockExpansionMap.put(target.getBelongingBlock(), currentRequests);
                }
                List<SyntaxTree> stmts = target.getStatementList();
                int size = stmts.size();
                for(int i=size-1; i>=0; i--){
                    SyntaxTree stmt = stmts.get(i);
                    if(hasTriggerGC(stmt))
                        visit(stmt, newTailLive);
                    else
                        update(stmt, newTailLive);
                }
                target.setHeadLive(newTailLive);
                Collection<ControlFlowGraphNode> prevs = target.getPrev();
                for(ControlFlowGraphNode prev : prevs){
                    if(prev == ControlFlowGraphNode.enter) continue;
                    queue.add(prev);
                }
            }
            for(Entry entry : blockExpansionMap.entryCollection()){
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

    private final void visit(SyntaxTree node, Collection<String> live) throws Exception{
        find(node.getTag().toString()).accept(node, live);
    }

    private final void visit(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
        find(node.getTag().toString()).accept(node, live, dict);
    }

    private final boolean hasTriggerGC(SyntaxTree node) throws Exception{
        return find(node.getTag().toString()).findTriggerGC(node);
    }

    private final void update(SyntaxTree node, Collection<String> live) throws Exception{
        find(node.getTag().toString()).updateLive(node, live);
    }

    private final void update(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
        find(node.getTag().toString()).updateLive(node, live, dict);
    }

    public class DefaultVisitor{
        public void accept(SyntaxTree node, Collection<String> live) throws Exception{
            for(SyntaxTree chunk : node){
                visit(chunk, live);
            }
        }
        public void accept(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
            for(SyntaxTree chunk : node){
                visit(chunk, live, dict);
            }
        }
        public boolean findTriggerGC(SyntaxTree node) throws Exception{
            for(SyntaxTree chunk : node){
                if(hasTriggerGC(chunk)) return true;
            }
            return false;
        }
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            for(SyntaxTree chunk : node){
                update(chunk, live);
            }
        }
        public void updateLive(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
            for(SyntaxTree chunk : node){
                update(chunk, live, dict);
            }
        }
    }

    public class Statements extends DefaultVisitor{
        public void pushPopGenerate(SyntaxTree node, Collection<String> live){
            if(live == null || live.isEmpty()) return;
            List<String> liveList = new ArrayList<>(live);
            if(compileMode.isBuiltinFunctionMode()){
                liveList.remove("args");
            }
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
        public void updateLive(SyntaxTree expr, Collection<String> live, TypeMapSet dict) throws Exception{
            update(expr, live, dict);
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
            if(live == null || live.isEmpty()) return;
            SyntaxTree[] declarationSeparated = new SyntaxTree[2];
            List<String> liveList = new ArrayList<>(live);
            int size = liveList.size();
            SyntaxTree[] stmts = new SyntaxTree[size*2+1];
            for(int i=0; i<size; i++){
                stmts[i]        = gcFunctionGenerator.getGCPushExprStmt(liveList.get(i));
                stmts[size*2-i] = gcFunctionGenerator.getGCPopExprStmt(liveList.get(i));
            }
            SyntaxTree[] nestSolved = new NestGCTransformVisitor().start(clipAssign(node));
            stmts[size] = (nestSolved.length == 1) ? nestSolved[0] : ASTHelper.generateBlock(nestSolved);
            declarationSeparated[0] = clipDeclaration(node);
            declarationSeparated[1] = ASTHelper.generateBlock(stmts);
            currentRequests.put(node, declarationSeparated);
        }
        private final void updateCollection(SyntaxTree node, Collection<String> live) throws Exception{
            SyntaxTree var = node.get(Symbol.unique("var"));
            String varName = var.toText();
            if(!node.has(Symbol.unique("expr")) && live.contains(varName))
                throw new Error("Illigal live variable analysis status");
            live.remove(varName);
        }
        @Override
        public void accept(SyntaxTree node, Collection<String> live) throws Exception{
            updateCollection(node, live);
            if(!node.has(Symbol.unique("expr")))
                return;
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            if(expr == SyntaxTree.PHANTOM_NODE) return;
            pushPopGenerate(node, live);
            visit(expr, live, node.getTailDict());
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            updateCollection(node, live);
            if(!node.has(Symbol.unique("expr"))) return;
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            updateLive(expr, live, node.getTailDict());
        }
    }

    public class Assignment extends Statements{
        private final void updateCollection(SyntaxTree node, Collection<String> live) throws Exception{
            SyntaxTree var = node.get(Symbol.unique("left"));
            String varName = var.toText();
            live.remove(varName);
        }
        @Override
        public void accept(SyntaxTree node, Collection<String> live) throws Exception{
            updateCollection(node, live);
            SyntaxTree expr = node.get(Symbol.unique("right"));
            pushPopGenerate(node, live);
            visit(expr, live, node.getTailDict());
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            updateCollection(node, live);
            SyntaxTree expr = node.get(Symbol.unique("right"));
            updateLive(expr, live, node.getTailDict());
        }
    }

    public class AssignmentPair extends Statements{
        private final void updateCollection(SyntaxTree node, Collection<String> live) throws Exception{
            SyntaxTree pair = node.get(Symbol.unique("left"));
            for(SyntaxTree var : pair){
                String varName = var.toText();
                live.remove(varName);
            }
        }
        @Override
        public void accept(SyntaxTree node, Collection<String> live) throws Exception{
            updateCollection(node, live);
            SyntaxTree expr = node.get(Symbol.unique("right"));
            pushPopGenerate(node, live);
            visit(expr, live, node.getTailDict());
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            updateCollection(node, live);
            SyntaxTree expr = node.get(Symbol.unique("right"));
            updateLive(expr, live, node.getTailDict());
        }
    }

    public class ExpressionStatement extends Statements{
        @Override
        public void accept(SyntaxTree node, Collection<String> live) throws Exception{
            pushPopGenerate(node, live);
            for(SyntaxTree chunk : node){
                visit(chunk, live, node.getTailDict());
            }
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            SyntaxTree expr = node.get(0);
            updateLive(expr, live, node.getTailDict());
        }
    }

    public class SpecialExpression extends Statements{
        @Override
        public void accept(SyntaxTree node, Collection<String> live) throws Exception{
            ErrorPrinter.error("triggerGC annotated functions cannot be called here.", node.get(0));
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            SyntaxTree expr = node.get(0);
            updateLive(expr, live, node.getTailDict());
        }
    }

    private static class DataLocation{
        private static enum Location{
            VMHeap(true), ExemptedVMHeap(false), NonVMHeap(false);

            private boolean requireGCPushPopFlag;

            private Location(boolean requireGCPushPop){
                requireGCPushPopFlag = requireGCPushPop;
            }
            public boolean isRequiredGCPushPop(){
                return requireGCPushPopFlag;
            }
        }

        private Location location = Location.ExemptedVMHeap;

        public void setLocationToVMHeap(){
            if(location == Location.NonVMHeap)
                throw new Error("Illigal data location specification: cannot set to VMHeap");
            location = Location.VMHeap;
        }
        public void setLocationToNonVMHeap(){
            if(location == Location.VMHeap)
                throw new Error("Illigal data location specification: cannot set to Non-VMHeap");
            location = Location.NonVMHeap;
        }
        public boolean isRequiredGCPushPop(){
            return location.isRequiredGCPushPop();
        }
    }

    public class Return extends DefaultVisitor{
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            if(node.size() == 0) return;
            SyntaxTree expr = node.get(0);
            update(expr, live, node.getHeadDict());
        }
    }

    public class Rematch extends DefaultVisitor{
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live) throws Exception{
            int size = node.size();
            for(int i=1; i<size; i++)
                update(node.get(i), live, node.getHeadDict());
        }
    }

    public class Name extends DefaultVisitor{
        private boolean isCConstant(String name){
            return CConstantTable.contains(name);
        }
        private boolean isRequiredGCPushPop(AstType type){
            if(type instanceof AstArrayType){
                if(type == AstType.ARGS) return false;
                AstType elementType = ((AstArrayType)type).getElementType();
                return isRequiredGCPushPop(elementType);
            }
            if(!(type instanceof AstBaseType)) return false;
            AstBaseType bType = (AstBaseType) type;
            return bType.isRequiredGCPushPop();
        }
        private boolean isRequiredGCPushPop(String name, TypeMapSet dict){
            DataLocation dataLocation = new DataLocation();
            for(TypeMap typeMap : dict){
                AstType type = typeMap.get(name);
                if(exceptType.contains(type)) continue;
                if(isRequiredGCPushPop(type))
                    dataLocation.setLocationToVMHeap();
                else
                    dataLocation.setLocationToNonVMHeap();
            }
            return dataLocation.isRequiredGCPushPop();
        }
        @Override
        public void accept(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
            String name = node.toText();
            if(!isCConstant(name) && isRequiredGCPushPop(name, dict)){
                live.add(name);
            }
        }
        @Override
        public boolean findTriggerGC(SyntaxTree node) throws Exception{
            return false;
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
            accept(node, live, dict);
        }
    }

    public class FieldAccess extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
        }
    }

    public class FunctionCall extends DefaultVisitor{
        @Override
        public void accept(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
            SyntaxTree args = node.get(Symbol.unique("args"));
            visit(args, live, dict);
        }
        @Override
        public boolean findTriggerGC(SyntaxTree node) throws Exception{
            SyntaxTree expandedNode = node.getExpandedTree();
            if(expandedNode != null)
                return findTriggerGC(expandedNode);
            String functionName = node.get(Symbol.unique("recv")).toText();
            return FunctionTable.hasAnnotations(functionName, FunctionAnnotation.triggerGC);
        }
        @Override
        public void updateLive(SyntaxTree node, Collection<String> live, TypeMapSet dict) throws Exception{
            SyntaxTree args = node.get(Symbol.unique("args"));
            visit(args, live, dict);
        }
    }
}
