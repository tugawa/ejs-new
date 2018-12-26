package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import java.util.ArrayList;

import vmdlc.TypeCheckVisitor.DefaultVisitor;

public class TypeCheckVisitor extends TreeVisitorMap<DefaultVisitor> {
    public TypeCheckVisitor() {
        init(TypeCheckVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node) {
        ArrayList dict = new ArrayList();
        for (Tree<?> chunk : node) {
            visit(chunk, dict);
        }
    }

    private final void visit(Tree<?> node, ArrayList dict) {
        find(node.getTag().toString()).accept(node, dict);
    }

    public class DefaultVisitor {
        public void accept(Tree<?> node, ArrayList dict) {
            for (Tree<?> seq : node) {
                find(node.getTag().toString()).accept(node, dict);
            }
            // System.out.println("ERROR");
            // return null;
        }
    }

    public class Source extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, ArrayList dict) {
            for (Tree<?> seq : node) {
                visit(seq, dict);
            }
        }
    }
    public class FunctionDefinition extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, ArrayList dict) {
            Tree<?> annotations = node.get(Symbol.unique("annotations"));
            Tree<?> type = node.get(Symbol.unique("type"));
            ConsoleUtils.println(type.getTag());
            String fname = node.get(Symbol.unique("name")).toText();
            ConsoleUtils.println("L_" + fname + ":");

            // for (Tree<?> seq : node) {
            //     visit(seq);
            // }
        }
    }
    public class Match extends DefaultVisitor {
        public void accept(Tree<?> node, ArrayList dict) {
            // ConsoleUtils.println(node.getTag().toString());
            for (Tree<?> seq : node) {
                visit(seq, dict);
            }
        }
    }
}
