package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import java.util.HashMap;
import java.util.ArrayList;

import vmdlc.TypeCheckVisitor.DefaultVisitor;
import type.AstType.*;
import type.AstType;

public class TypeCheckVisitor extends TreeVisitorMap<DefaultVisitor> {
    public TypeCheckVisitor() {
        init(TypeCheckVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node) {
        try {
            HashMap<String, AstType> dict = new HashMap<String, AstType>();
            for (Tree<?> chunk : node) {
                visit((SyntaxTree)chunk, dict);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final void visit(SyntaxTree node, HashMap<String, AstType> dict) throws Exception {
        find(node.getTag().toString()).accept(node, dict);
    }

    public class DefaultVisitor {
        public void accept(SyntaxTree node, HashMap<String, AstType> dict) throws Exception {
            for (Tree<?> seq : node) {
                visit((SyntaxTree)seq, dict);
            }
        }
    }

    public class FunctionMeta extends DefaultVisitor {
        @Override
        public void accept(SyntaxTree node, HashMap<String, AstType> dict) throws Exception {
            Tree<?> annotations = node.get(Symbol.unique("annotations"));
            Tree<?> type = node.get(Symbol.unique("type"));
            AstProductType funtype = (AstProductType)AstType.nodeToType((SyntaxTree)type);
            node.addType(funtype);

            Tree<?> definition = node.get(Symbol.unique("definition"));
            String name = definition.get(Symbol.unique("name")).toText();
            dict.put(name, funtype);
            
            Tree<?> params = definition.get(Symbol.unique("params"));
            AstType types = funtype.getDomain();
            if (types instanceof AstBaseType) {
                ((SyntaxTree)params).addType(types);
                dict.put(params.toText(), types);
            } else if (types instanceof AstPairType) {
                ArrayList<AstType> lst = ((AstPairType)types).getTypes();
                for (int i = 0; i < params.size(); i++) {
                    SyntaxTree param = (SyntaxTree)params.get(i);
                    param.addType(lst.get(i));
                    dict.put(param.toText(), lst.get(i));
                }
            }
            
            SyntaxTree body = (SyntaxTree)definition.get(Symbol.unique("body"));
            body.addType(funtype.getRange());
            
            visit((SyntaxTree)body, dict);
        }
    }

    public class Declaration extends DefaultVisitor {
        @Override
        public void accept(SyntaxTree node, HashMap<String, AstType> dict) throws Exception {
            SyntaxTree type = node.get(Symbol.unique("type"));
            SyntaxTree var = node.get(Symbol.unique("var"));
            AstBaseType varType = new AstBaseType(type.toText());
            var.addType(varType);
            dict.put(var.toText(), varType);
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            visit(expr, dict);
        }
    }
    
    public class MatchParameters extends DefaultVisitor {
        @Override
        public void accept(SyntaxTree node, HashMap<String, AstType> dict) throws Exception {
            for (SyntaxTree v : node) {
                v.addType(dict.get(v.toText()));
            }
        }
    }

    public class Name extends DefaultVisitor {
        @Override
        public void accept(SyntaxTree node, HashMap<String, AstType> dict) throws Exception {
            node.addType(dict.get(node.toText()));
        }
    }
}
