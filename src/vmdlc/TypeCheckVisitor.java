package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.HashSet;

import vmdlc.AstToCVisitor.MatchRecord;
import vmdlc.TypeCheckVisitor.DefaultVisitor;
import type.AstType.*;
import type.AstType;
import type.TypeMap;
import type.VMDataType;

public class TypeCheckVisitor extends TreeVisitorMap<DefaultVisitor> {
    static class MatchStack {
        static class MatchRecord {
            String name;
            String[] formalParams;
            TypeMap dict;
            MatchRecord(String name, String[] formalParams, TypeMap dict) {
                this.name = name;
                this.formalParams = formalParams;
                this.dict = dict;
            }
            public void setDict(TypeMap _dict) {
                dict = _dict;
            }
            public String toString() {
                return name;
            }
        }
        
        Stack<MatchRecord> stack;
        public MatchStack() {
            stack = new Stack<MatchRecord>();
        }
        MatchRecord lookup(String name) {
            for (int i = stack.size() - 1; i >= 0; i--) {
                MatchRecord mr = stack.get(i);
                if (mr.name != null && mr.name.equals(name))
                    return mr;
            }
            return null;
        }
        
        public void enter(String name, String[] formalParams, TypeMap dict) {
            MatchRecord mr = new MatchRecord(name, formalParams, dict);
            stack.push(mr);
        }
        public String[] getParams(String name) {
            MatchRecord mr = lookup(name);
            if (mr == null)
                return null;
            return mr.formalParams;
        }
        public TypeMap getDict(String name) {
            MatchRecord mr = lookup(name);
            if (mr == null)
                return null;
            return mr.dict;
        }
        public void pop() {
            stack.pop();
        }
        public boolean isEmpty() {
            return stack.isEmpty();
        }
        public void updateDict(String name, TypeMap dict) {
            MatchRecord mr = lookup(name);
            if (mr != null) {
                mr.setDict(dict);
            }
        }
        public String toString() {
            return stack.toString();
        }
    }
    MatchStack matchStack;
    
    public TypeCheckVisitor() {
        init(TypeCheckVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node) {
        try {
            TypeMap dict = new TypeMap();
            matchStack = new MatchStack();
            for (Tree<?> chunk : node) {
                dict = visit((SyntaxTree)chunk, dict);
            }
            if (!matchStack.isEmpty())
                throw new Error("match stack is not empty after typing process");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final TypeMap visit(SyntaxTree node, TypeMap dict) throws Exception {
        return find(node.getTag().toString()).accept(node, dict);
    }

    public class DefaultVisitor {
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return dict;
        }
    }

    public class FunctionMeta extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            Tree<?> type = node.get(Symbol.unique("type"));
            AstProductType funtype = (AstProductType)AstType.nodeToType((SyntaxTree)type);
            
            // node.setType(funtype);
            Tree<?> definition = node.get(Symbol.unique("definition"));
            
            String name = definition.get(Symbol.unique("name")).toText();
            dict.add(name, funtype);
            
            Set<String> domain = new HashSet<String>(dict.getKeys());

            Tree<?> params = definition.get(Symbol.unique("params"));

            AstType types = funtype.getDomain();
            if (types instanceof AstBaseType) {
                // ((SyntaxTree)params).setType(types);
                dict.add(params.toText(), types);
            } else if (types instanceof AstPairType) {
                ArrayList<AstType> lst = ((AstPairType)types).getTypes();
                for (int i = 0; i < params.size(); i++) {
                    SyntaxTree param = (SyntaxTree)params.get(i);
                    param.setType(lst.get(i));
                    dict.add(param.toText(), lst.get(i));
                }
            }
            
            SyntaxTree body = (SyntaxTree)definition.get(Symbol.unique("body"));
            body.setType(funtype.getRange());
            
            dict = visit((SyntaxTree)body, dict);

            return dict.select((Set<String>)domain);
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            Set<String> domain = new HashSet<String>(dict.getKeys());
            for (Tree<?> seq : node) {
                dict = visit((SyntaxTree)seq, dict);
            }
            TypeMap result = dict.select((Set<String>)domain);
            return result;
        }
    }

    public class Match extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            MatchProcessor mp = new MatchProcessor(node);
            // TODO: no label match
            SyntaxTree labelNode= node.get(Symbol.unique("label"));
            String label = labelNode.toText();

            SyntaxTree cases = node.get(Symbol.unique("cases"));

            TypeMap outDict = dict.getBottomDict();

            TypeMap entryDict;
            TypeMap newEntryDict = dict;

            do {
                entryDict = newEntryDict;
                matchStack.enter(label, mp.getFormalParams(), entryDict);
                
                NEXT_CASE:
                for (SyntaxTree cas : cases) {
                    TypeMap lDict = dict.enterCase(mp.getFormalParams(), mp.getVmtVecCond(cas));

                    for (String v : mp.getFormalParams()) {
                        if (lDict.get(v) == AstType.JSValueType.BOT) {
                            continue NEXT_CASE;
                        }
                    }
                    SyntaxTree body = cas.get(Symbol.unique("body"));
                    TypeMap lDict2 = visit(body, lDict);
                    
                    outDict = outDict.lub(lDict2);
                }
                newEntryDict = matchStack.getDict(label);
                matchStack.pop();
            } while (!entryDict.equals(newEntryDict));

            System.out.println(outDict);
            return outDict;
        }
    }

    public class Rematch extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            String label = node.get(Symbol.unique("label")).toText();
            TypeMap matchDict = matchStack.getDict(label);
            Set<String> domain = matchDict.getKeys();
            String[] matchParams = matchStack.getParams(label);
            
            String[] rematchArgs = new String[matchParams.length];
            for (int i = 1; i < node.size(); i++) {
                rematchArgs[i-1] = node.get(i).toText();
            }
            
            TypeMap matchDict2 = dict.rematch(matchParams, rematchArgs, domain);
            TypeMap result = matchDict2.lub(matchDict);
            matchStack.updateDict(label, result);
            
            return matchDict.getBottomDict();
        }
    }
    
    public class Return extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            visit(node.get(0), dict);
            return dict;
        }
    }

    public class Assignment extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree right = node.get(Symbol.unique("right"));
            AstType rhsType = visit(right, dict).getExprType();
            SyntaxTree left = node.get(Symbol.unique("left"));
            left.setType(rhsType);
            dict.add(left.toText(), rhsType);
            return dict;
        }
    }

    public class Declaration extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            // SyntaxTree type = node.get(Symbol.unique("type"));
            // AstBaseType varType = new AstBaseType(type.toText());

            SyntaxTree var = node.get(Symbol.unique("var"));
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            AstType rhsType = visit(expr, dict).getExprType();
            var.setType(rhsType);
            dict.add(var.toText(), rhsType);

            return dict;
        }
    }
    public class If extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            // SyntaxTree condNode = node.get(Symbol.unique("cond"));
            // visit(condNode, dict);
            SyntaxTree thenNode = node.get(Symbol.unique("then"));
            TypeMap thenDict = visit(thenNode, dict);
            SyntaxTree elseNode = node.get(Symbol.unique("else"));
            TypeMap elseDict = visit(elseNode, dict);

            return thenDict.lub(elseDict);
        }
    }

    public class Do extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            Set<String> domain = new HashSet<String>(dict.getKeys());

            SyntaxTree initNode = node.get(Symbol.unique("init"));
            SyntaxTree varNode = initNode.get(Symbol.unique("var"));
            SyntaxTree exprNode = initNode.get(Symbol.unique("expr"));
            AstType rhsType = visit(exprNode, dict).getExprType();
            varNode.setType(rhsType);
            dict.add(varNode.toText(), rhsType);
            /*
            do (cint i = 0 step i+1) {

            }
            */
            SyntaxTree blockNode = initNode.get(Symbol.unique("block"));
            dict = visit(blockNode, dict);

            TypeMap result = dict.select((Set<String>)domain);

            return dict;
        }
    }

    public class CTypeDef extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            AstType type = AstType.get(typeNode.toText());
            varNode.setType(type);
            // dict.put(varNode.toText(), type);
            
            return dict;
        }
    }

    public class CFunction extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            AstType domain = AstType.nodeToType(typeNode.get(0));
            AstType range = AstType.nodeToType(typeNode.get(1));
            SyntaxTree nameNode = node.get(Symbol.unique("name"));
            AstType type = new AstProductType(domain, range);
            nameNode.setType(type);
            // dict.put(nameNode.toText(), type);
            
            return dict;
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            AstType type = AstType.get(typeNode.toText());
            varNode.setType(type);
            // dict.put(varNode.toText(), type);
            
            return dict;
        }
    }

    public class FunctionCall extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree recv = node.get(Symbol.unique("recv"));
            String funName = recv.toText();
            
            AstProductType funType = (AstProductType)dict.get(funName);
            AstBaseType rangeType = (AstBaseType)funType.getRange();

            // TODO type check of arguments
            for (SyntaxTree arg : node.get(1)) {
            }
            return new TypeMap(rangeType);
        }
    }
    public class _Integer extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("Fixnum"));
        }
    }

    public class _Float extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("Flonum"));
        }
    }

    public class _String extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("String"));
        }
    }

    public class _True extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("Bool"));
        }
    }
    public class _False extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("Bool"));
        }
    }

    public class Name extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(dict.get(node.toText()));
        }
    }
}
