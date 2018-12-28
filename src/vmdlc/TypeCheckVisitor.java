package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import java.util.ArrayList;

import vmdlc.TypeCheckVisitor.DefaultVisitor;
import type.AstType.*;
import type.AstType;
import type.TypeMap;

public class TypeCheckVisitor extends TreeVisitorMap<DefaultVisitor> {
    public TypeCheckVisitor() {
        init(TypeCheckVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node) {
        try {
            TypeMap dict = new TypeMap();
            for (Tree<?> chunk : node) {
                dict = visit((SyntaxTree)chunk, dict);
            }
            System.out.println(dict);
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
            node.setType(funtype);
            Tree<?> definition = node.get(Symbol.unique("definition"));
            String name = definition.get(Symbol.unique("name")).toText();
            // dict.put(name, funtype);

            Tree<?> params = definition.get(Symbol.unique("params"));
            AstType types = funtype.getDomain();
            if (types instanceof AstBaseType) {
                ((SyntaxTree)params).setType(types);
                // dict.put(params.toText(), types);
            } else if (types instanceof AstPairType) {
                ArrayList<AstType> lst = ((AstPairType)types).getTypes();
                for (int i = 0; i < params.size(); i++) {
                    SyntaxTree param = (SyntaxTree)params.get(i);
                    param.setType(lst.get(i));
                    // dict.put(param.toText(), lst.get(i));
                }
            }
            
            SyntaxTree body = (SyntaxTree)definition.get(Symbol.unique("body"));
            body.setType(funtype.getRange());

            return visit((SyntaxTree)body, dict);
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            for (Tree<?> seq : node) {
                dict = visit((SyntaxTree)seq, dict);
            }
            return dict;
        }
    }

    public class Match extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree params = node.get(Symbol.unique("params"));
            String[] varName = new String[params.size()];
            for (int i = 0; i < params.size(); i++) {
                varName[i] = params.get(i).toText();
            }

            SyntaxTree cases = node.get(Symbol.unique("cases"));
            for (SyntaxTree cas : cases) {
                SyntaxTree pattern = cas.get(Symbol.unique("pattern"));
            }
        }
    }
    public class Return extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return visit(node.get(0), dict);
        }
    }

    public class Assignment extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree right = node.get(Symbol.unique("right"));
            AstType rhsType = visit(right, dict).getExprType();
            SyntaxTree left = node.get(Symbol.unique("left"));
            left.setType(rhsType);
            // dict.put(left.toText(), rhsType);
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
            // dict.put(var.toText(), rhsType);

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
            SyntaxTree initNode = node.get(Symbol.unique("init"));
            SyntaxTree varNode = initNode.get(Symbol.unique("var"));
            SyntaxTree exprNode = initNode.get(Symbol.unique("expr"));
            AstType rhsType = visit(exprNode, dict).getExprType();
            varNode.setType(rhsType);
            // dict.put(varNode.toText(), rhsType);

            SyntaxTree blockNode = initNode.get(Symbol.unique("block"));
            dict = visit(blockNode, dict);

            return dict;
        }
    }

    public class CTypeDefinition extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            AstType type = new AstBaseType("HeapObject");
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
            AstType type = new AstBaseType(typeNode.toText());
            varNode.setType(type);
            // dict.put(varNode.toText(), type);
            
            return dict;
        }
    }

    public class _Integer extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(new AstBaseType("Fixnum"));
        }
    }

    public class _Float extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(new AstBaseType("Flonum"));
        }
    }

    public class _String extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(new AstBaseType("String"));
        }
    }

    public class _True extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(new AstBaseType("Bool"));
        }
    }
    public class _False extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(new AstBaseType("Bool"));
        }
    }

    public class Name extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(dict.get(node.get(0).toText()));
        }
    }
}
