package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import vmdlc.NodeVisitor.DefaultVisitor;

public class NodeVisitor extends TreeVisitorMap<DefaultVisitor> {
    public NodeVisitor() {
        init(NodeVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node) {
        ConsoleUtils.println(node.getTag().toString());
        for (Tree<?> chunk : node) {
            visit(chunk);
        }
    }

    private final void visit(Tree<?> node) {
        find(node.getTag().toString()).accept(node);
    }

    public class DefaultVisitor {
        public void accept(Tree<?> node) {
            /*
            for (Tree<?> seq : node) {
                find(node.getTag().toString()).accept(node);
            }
            */
            // System.out.println("ERROR");
            // return null;
        }
    }

    public class Source extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node) {
            for (Tree<?> seq : node) {
                visit(seq);
            }
        }
    }
    public class FunctionDefinition extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node) {
            Tree<?> annotations = node.get(Symbol.unique("annotations"));
            String fname = node.get(Symbol.unique("name")).toText();
            ConsoleUtils.println("L_" + fname + ":");

            // for (Tree<?> seq : node) {
            //     visit(seq);
            // }
        }
    }
    public class Match extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node) {
            // ConsoleUtils.println(node.getTag().toString());
            for (Tree<?> seq : node) {
                visit(seq);
            }
        }
    }
}
