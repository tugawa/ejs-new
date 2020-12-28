package vmdlc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import nez.ast.Symbol;
import nez.ast.TreeVisitorMap;
import type.TypeMapSet;
import type.VMDataType;
import vmdlc.ControlFlowGraphConstructVisitor.DefaultVisitor;

public class ControlFlowGraphConstructVisitor extends TreeVisitorMap<DefaultVisitor> {

    static class MatchStack{
        static class MatchRecord{
            private String label;
            private ControlFlowGraphNode branchPoint;
            public MatchRecord(String label, ControlFlowGraphNode branchPoint){
                this.label = label;
                this.branchPoint = branchPoint;
            }
            public String getLabel(){
                return label;
            }
            public ControlFlowGraphNode getBranchPointNode(){
                return branchPoint;
            }
        }

        Stack<MatchRecord> stack = new Stack<>();

        public MatchRecord lookup(String label){
            for(MatchRecord r : stack){
                if(r.getLabel().equals(label)) return r;
            }
            return null;
        }
        public void push(String label, ControlFlowGraphNode top){
            stack.push(new MatchRecord(label, top));
        }
        public MatchRecord peek(){
            return stack.peek();
        }
        public MatchRecord pop(){
            return stack.pop();
        }
        public boolean isEmpty(){
            return stack.isEmpty();
        }
    }

    private MatchStack matchStack = new MatchStack();

    public ControlFlowGraphConstructVisitor(){
        init(ControlFlowGraphConstructVisitor.class, new DefaultVisitor());
    }

    public ControlFlowGraphNode start(SyntaxTree node){
        try{
            List<ControlFlowGraphNode> afterFunctions = new ArrayList<>(node.size());
            for (SyntaxTree chunk : node){
                if(!chunk.is(Symbol.unique("FunctionMeta"))) continue;
                afterFunctions.add(visit(chunk, ControlFlowGraphNode.enter));
            }
            for(ControlFlowGraphNode cfgn : afterFunctions){
                if(cfgn == ControlFlowGraphNode.exit) continue;
                cfgn.makeEdgeTo(ControlFlowGraphNode.exit);
            }
        }catch(Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
        }
        return ControlFlowGraphNode.enter;
    }

    private final ControlFlowGraphNode visit(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
        return find(node.getTag().toString()).accept(node, from);
    }

    private final SyntaxTree wrapExpressionSyntaxTree(SyntaxTree node, TypeMapSet dict){
        SyntaxTree wrapped = ASTHelper.generateSpecialExpression(node);
        wrapped.setHeadDict(dict);
        wrapped.setTailDict(dict);
        return wrapped;
    }

    public class DefaultVisitor{
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            return from;
        }
    }

    public class FunctionMeta extends DefaultVisitor{
        private SyntaxTree genPhantomDeclaration(SyntaxTree type, SyntaxTree name){
            return ASTHelper.generateDeclaration(type, name, SyntaxTree.PHANTOM_NODE);
        }
        private ControlFlowGraphNode genParamIntro(SyntaxTree node) throws Exception{
            SyntaxTree functionDefinition = node.get(Symbol.unique("definition"));
            SyntaxTree params = functionDefinition.get(Symbol.unique("params"));
            if(params == null || params.size() == 0) return null;
            SyntaxTree type = node.get(Symbol.unique("type"));
            SyntaxTree domain = type.get(0);
            ControlFlowGraphNode intro = new ControlFlowGraphNode(new HashSet<>(params.size()));
            if(domain.is(Symbol.unique("TypePair"))){
                int size = params.size();
                for(int i=0; i<size; i++){
                    SyntaxTree phantomDeclaration = genPhantomDeclaration(domain.get(i), params.get(i));
                    visit(phantomDeclaration, intro);
                    intro.addStatement(phantomDeclaration);
                }
            }else{
                SyntaxTree phantomDeclaration = genPhantomDeclaration(domain, params.get(0));
                visit(phantomDeclaration, intro);
                intro.addStatement(phantomDeclaration);
            }
            return intro;
        }
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            SyntaxTree functionDefinition = node.get(Symbol.unique("definition"));
            SyntaxTree body = functionDefinition.get(Symbol.unique("body"));
            ControlFlowGraphNode intro = genParamIntro(node);
            if(intro != null){
                from.makeEdgeTo(intro);
            }else{
                intro = from;
            }
            ControlFlowGraphNode after = visit(body, intro);
            return after;
        }
    }

    public class Block extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            if(!from.isSetBelongingBlock()){
                from.setBelongingBlock(node);
            }
            for(SyntaxTree seq : node){
                from = visit(seq, from);
            }
            return from;
        }
    }

    public class If extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            TypeMapSet dict = node.getHeadDict();
            Collection<String> locals = dict.getKeys();
            SyntaxTree condNode = node.get(Symbol.unique("cond"));
            SyntaxTree wrappedCondNode = wrapExpressionSyntaxTree(condNode, dict);
            ControlFlowGraphNode condCFGN = new ControlFlowGraphNode(locals);
            condCFGN.addStatement(wrappedCondNode);
            from.makeEdgeTo(condCFGN);
            SyntaxTree thenNode = node.get(Symbol.unique("then"));
            ControlFlowGraphNode afterCFGN = new ControlFlowGraphNode(locals);
            ControlFlowGraphNode thenCFGN = new ControlFlowGraphNode(locals);
            condCFGN.makeEdgeTo(thenCFGN);
            ControlFlowGraphNode afterThen = visit(thenNode, thenCFGN);
            if(afterThen != ControlFlowGraphNode.exit){
                afterThen.makeEdgeTo(afterCFGN);
            }
            if (node.has(Symbol.unique("else"))) {
                SyntaxTree elseNode = node.get(Symbol.unique("else"));
                ControlFlowGraphNode elseCFGN = new ControlFlowGraphNode(locals);
                condCFGN.makeEdgeTo(elseCFGN);
                ControlFlowGraphNode afterElse = visit(elseNode, elseCFGN);
                if(afterElse != ControlFlowGraphNode.exit){
                    afterElse.makeEdgeTo(afterCFGN);
                }
            }else{
                condCFGN.makeEdgeTo(afterCFGN);
            }
            return afterCFGN;
        }
    }

    public class Do extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            TypeMapSet dict = node.getHeadDict();
            Collection<String> locals = dict.getKeys();
            SyntaxTree blockNode = node.get(Symbol.unique("block"));
            SyntaxTree init = node.get(Symbol.unique("init"));
            TypeMapSet afterInitDict = init.getTailDict();
            ControlFlowGraphNode intro = new ControlFlowGraphNode(locals);
            SyntaxTree typeNode = init.get(Symbol.unique("type"));
            SyntaxTree nameNode = init.get(Symbol.unique("var"));
            SyntaxTree introNode = ASTHelper.generateDeclaration(typeNode, nameNode, init.get(Symbol.unique("expr")));
            introNode.setHeadDict(dict);
            introNode.setTailDict(afterInitDict);
            intro.addStatement(introNode);
            SyntaxTree limitNode = node.get(Symbol.unique("limit"));
            SyntaxTree wrappedLimitNode = wrapExpressionSyntaxTree(limitNode, afterInitDict);
            SyntaxTree stepNode = node.get(Symbol.unique("step"));
            SyntaxTree wrappedStepNode = wrapExpressionSyntaxTree(stepNode, afterInitDict);
            Collection<String> afterIntroLocals = new HashSet<>(locals);
            afterIntroLocals.add(nameNode.toText());
            ControlFlowGraphNode wrappedCFGN = new ControlFlowGraphNode(afterIntroLocals);
            wrappedCFGN.addStatement(wrappedLimitNode);
            wrappedCFGN.addStatement(wrappedStepNode);
            from.makeEdgeTo(intro);
            intro.makeEdgeTo(wrappedCFGN);
            ControlFlowGraphNode body = new ControlFlowGraphNode(afterIntroLocals);
            wrappedCFGN.makeEdgeTo(body);
            ControlFlowGraphNode after = visit(blockNode, body);
            after.makeEdgeTo(body);
            return after;
        }
    }

    public class While extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            TypeMapSet dict = node.getHeadDict();
            Collection<String> locals = dict.getKeys();
            SyntaxTree condNode = node.get(Symbol.unique("cond"));
            SyntaxTree wrappedCondNode = wrapExpressionSyntaxTree(condNode, dict);
            ControlFlowGraphNode condCFGN = new ControlFlowGraphNode(locals);
            condCFGN.addStatement(wrappedCondNode);
            from.makeEdgeTo(condCFGN);
            SyntaxTree blockNode = node.get(Symbol.unique("block"));
            ControlFlowGraphNode body = new ControlFlowGraphNode(locals);
            condCFGN.makeEdgeTo(body);
            ControlFlowGraphNode after = visit(blockNode, body);
            after.makeEdgeTo(body);
            return after;
        }
    }

    public class Match extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            MatchProcessor mp = new MatchProcessor(node);
            TypeMapSet dict = node.getHeadDict();
            Collection<String> locals = dict.getKeys();
            ControlFlowGraphNode branchPoint = new ControlFlowGraphNode(locals);
            SyntaxTree paramNode = node.get(Symbol.unique("params"));
            for(SyntaxTree parameter : paramNode)
                branchPoint.addStatement(wrapExpressionSyntaxTree(parameter, dict));
            ControlFlowGraphNode after = new ControlFlowGraphNode(locals);
            Set<VMDataType[]> nonMatchConds = dict.filterTypeVecs(mp.getFormalParams(), mp.getNonMatchCondVecSet().getTuples());
            if(!nonMatchConds.isEmpty())
                branchPoint.makeEdgeTo(after);
            from.makeEdgeTo(branchPoint);
            matchStack.push(mp.getLabel(), branchPoint);
            for(int i=0; i<mp.size(); i++){
                Set<VMDataType[]> filtered = dict.filterTypeVecs(mp.getFormalParams(), mp.getVmtVecCond(i));
                if(filtered.isEmpty()) continue;
                SyntaxTree body = mp.getBodyAst(i);
                ControlFlowGraphNode caseBody = new ControlFlowGraphNode(locals);
                branchPoint.makeEdgeTo(caseBody);
                ControlFlowGraphNode ret = visit(body, caseBody);
                if(ret == branchPoint || ret == ControlFlowGraphNode.exit) continue;
                ret.makeEdgeTo(after);
            }
            return after;
        }
    }

    public class Rematch extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            if(matchStack.isEmpty()){
                throw new Error("matchStack is empty");
            }
            ControlFlowGraphNode branchPoint = matchStack.peek().getBranchPointNode();
            from.addStatement(node);
            from.makeEdgeTo(branchPoint);
            return branchPoint;
        }
    }

    public class Return extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            from.addStatement(node);
            from.makeEdgeTo(ControlFlowGraphNode.exit);
            return ControlFlowGraphNode.exit;
        }
    }

    public class Declaration extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            if(from == ControlFlowGraphNode.exit){
                ErrorPrinter.error("Dead code.", node);
            }
            from.addStatement(node);
            SyntaxTree var = node.get(Symbol.unique("var"));
            String varName = var.toText();
            from.addLocals(varName);
            return from;
        }
    }

    public class Statements extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            if(from == ControlFlowGraphNode.exit){
                ErrorPrinter.error("Dead code.", node);
            }
            from.addStatement(node);
            return from;
        }
    }

    public class Assignment extends Statements{}

    public class AssignmentPair extends Statements{}

    public class ExpressionStatement extends Statements{}
}