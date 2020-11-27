package vmdlc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import nez.ast.Symbol;
import nez.ast.TreeVisitorMap;
import type.AstType;
import type.TypeMapSet;
import type.VMDataType;
import type.AstType.JSValueType;
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

    public class DefaultVisitor{
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            return from;
        }
    }

    public class FunctionMeta extends DefaultVisitor{
        private SyntaxTree genPhantomDeclaration(SyntaxTree type, SyntaxTree name){
            return new SyntaxTree(Symbol.unique("Declaration"),
                new Symbol[]{Symbol.unique("type"), Symbol.unique("var"), Symbol.unique("expr")},
                new SyntaxTree[]{type, name, null}, null);
        }
        private ControlFlowGraphNode genParamIntro(SyntaxTree node){
            SyntaxTree functionDefinition = node.get(Symbol.unique("definition"));
            SyntaxTree params = functionDefinition.get(Symbol.unique("params"));
            if(params == null || params.size() == 0) return null;
            SyntaxTree type = node.get(Symbol.unique("type"));
            SyntaxTree domain = type.get(0);
            ControlFlowGraphNode intro = new ControlFlowGraphNode(new HashSet<>(params.size()), new HashSet<>(params.size()));
            if(domain.is(Symbol.unique("TypePair"))){
                int size = params.size();
                for(int i=0; i<size; i++){
                    intro.addStatement(genPhantomDeclaration(domain.get(i), params.get(i)));
                }
            }else{
                intro.addStatement(genPhantomDeclaration(domain, params.get(0)));
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
            for(SyntaxTree seq : node){
                from = visit(seq, from);
            }
            return from;
        }
    }

    public class If extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            TypeMapSet dict = node.getTypeMapSet();
            Collection<String> locals = dict.getKeys();
            Collection<String> jsTypeVars = dict.typeOf(JSValueType.class);
            SyntaxTree thenNode = node.get(Symbol.unique("then"));
            ControlFlowGraphNode afterCFGN = new ControlFlowGraphNode(locals, jsTypeVars);
            ControlFlowGraphNode thenCFGN = new ControlFlowGraphNode(locals, jsTypeVars);
            from.makeEdgeTo(thenCFGN);
            ControlFlowGraphNode afterThen = visit(thenNode, thenCFGN);
            if(afterThen != ControlFlowGraphNode.exit){
                afterThen.makeEdgeTo(afterCFGN);
            }
            if (node.has(Symbol.unique("else"))) {
                SyntaxTree elseNode = node.get(Symbol.unique("else"));
                ControlFlowGraphNode elseCFGN = new ControlFlowGraphNode(locals, jsTypeVars);
                from.makeEdgeTo(elseCFGN);
                ControlFlowGraphNode afterElse = visit(elseNode, elseCFGN);
                if(afterElse != ControlFlowGraphNode.exit){
                    afterElse.makeEdgeTo(afterCFGN);
                }
            }else{
                from.makeEdgeTo(afterCFGN);
            }
            return afterCFGN;
        }
    }

    public class Do extends DefaultVisitor{
        private SyntaxTree genDeclaration(SyntaxTree type, SyntaxTree name, SyntaxTree expr){
            return new SyntaxTree(Symbol.unique("Declaration"),
                new Symbol[]{Symbol.unique("type"), Symbol.unique("var"), Symbol.unique("expr")},
                new SyntaxTree[]{type, name, expr}, null);
        }
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            TypeMapSet dict = node.getTypeMapSet();
            Collection<String> locals = dict.getKeys();
            Collection<String> jsTypeVars = dict.typeOf(JSValueType.class);
            SyntaxTree blockNode = node.get(Symbol.unique("block"));
            SyntaxTree init = node.get(Symbol.unique("init"));
            ControlFlowGraphNode intro = new ControlFlowGraphNode(locals, jsTypeVars);
            SyntaxTree typeNode = init.get(Symbol.unique("type"));
            SyntaxTree nameNode = init.get(Symbol.unique("var"));
            intro.addStatement(genDeclaration(typeNode, nameNode, init.get(Symbol.unique("expr"))));
            from.makeEdgeTo(intro);
            Collection<String> afterIntroLocals = new HashSet<>(locals);
            afterIntroLocals.add(nameNode.toText());
            ControlFlowGraphNode body = new ControlFlowGraphNode(afterIntroLocals, jsTypeVars);
            intro.makeEdgeTo(body);
            body.makeEdgeTo(body);
            ControlFlowGraphNode after = visit(blockNode, body);
            return after;
        }
    }

    public class Match extends DefaultVisitor{
        @Override
        public ControlFlowGraphNode accept(SyntaxTree node, ControlFlowGraphNode from) throws Exception{
            MatchProcessor mp = new MatchProcessor(node);
            TypeMapSet dict = node.getTypeMapSet();
            Collection<String> locals = dict.getKeys();
            Collection<String> jsTypeVars = dict.typeOf(JSValueType.class);
            ControlFlowGraphNode branchPoint = new ControlFlowGraphNode(locals, jsTypeVars);
            ControlFlowGraphNode after = new ControlFlowGraphNode(locals, jsTypeVars);
            Set<VMDataType[]> nonMatchConds = dict.filterTypeVecs(mp.getFormalParams(), mp.getNonMatchCondVecSet().getTuples());
            if(!nonMatchConds.isEmpty()){
                branchPoint.makeEdgeTo(after);
            }
            from.makeEdgeTo(branchPoint);
            matchStack.push(mp.getLabel(), branchPoint);
            for(int i=0; i<mp.size(); i++){
                Set<VMDataType[]> filtered = dict.filterTypeVecs(mp.getFormalParams(), mp.getVmtVecCond(i));
                if(filtered.isEmpty()) continue;
                SyntaxTree body = mp.getBodyAst(i);
                ControlFlowGraphNode caseBody = new ControlFlowGraphNode(locals, jsTypeVars);
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
            SyntaxTree type = node.get(Symbol.unique("type"));
            SyntaxTree var = node.get(Symbol.unique("var"));
            String varName = var.toText();
            if(AstType.nodeToType(type) instanceof JSValueType){
                from.addJSTypeLocals(varName);
            }else{
                from.addLocals(varName);
            }
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
