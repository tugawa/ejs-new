/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;

import vmdlc.ReplaceNameVisitor.DefaultVisitor;

public class ReplaceNameVisitor extends TreeVisitorMap<DefaultVisitor> {
    public ReplaceNameVisitor() {
        init(ReplaceNameVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node, String v1, String v2) {
        try {
            for (Tree<?> chunk : node) {
                visit(chunk, v1, v2);
            }
        } catch (Exception e) {
        }
    }

    private final void visit(Tree<?> node, String v1, String v2) throws Exception {
        find(node.getTag().toString()).accept(node, v1, v2);
    }

    private void print(Object o) {
        ConsoleUtils.println(o);
    }

    public class DefaultVisitor {
        public void accept(Tree<?> node, String v1, String v2) throws Exception {
            for (Tree<?> seq : node) {
                visit(seq, v1, v2);
            }
        }
    }

    public class Name extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, String v1, String v2) throws Exception {
            if (node.toText().equals(v1)) {
                node.setValue(v2);
            }
        }
    }
}