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
import nez.ast.Symbol;

import java.util.HashMap;
import java.lang.Exception;

import vmdlc.DesugarVisitor.DefaultVisitor;
import vmdlc.ReplaceNameVisitor;

public class DesugarVisitor extends TreeVisitorMap<DefaultVisitor> {
    public DesugarVisitor() {
        init(DesugarVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node) {
        try {
            PatternDict dict = new PatternDict();
            for (Tree<?> chunk : node) {
                visit(chunk, dict);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
        }
    }

    private final void visit(Tree<?> node, PatternDict dict) throws Exception {
        find(node.getTag().toString()).accept(node, dict);
    }

    //Never used
    /*
    private void print(Object o) {
        ConsoleUtils.println(o);
    }
    */

    public class DefaultVisitor {
        public void accept(Tree<?> node, PatternDict dict) throws Exception {
            for (Tree<?> seq : node) {
                visit(seq, dict);
            }
        }
    }

    public class PatternDefinition extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, PatternDict dict) throws Exception {
            Tree<?> nameNode = node.get(Symbol.unique("patternname"));
            Tree<?> varNode = node.get(Symbol.unique("var"));
            Tree<?> patternNode = node.get(Symbol.unique("pattern"));
            dict.intern(nameNode.toText(), varNode.toText(), patternNode);
        }
    }

    public class TypePattern extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, PatternDict dict) throws Exception {
            dict.replace(node);
        }
    }
}

class PatternDict {
    HashMap<String, Pattern> map;

    public PatternDict() {
        super();
        map = new HashMap<String, Pattern>();
    }

    public void intern(String name, String varName, Tree<?> pattern) throws Exception {
        if (map.containsKey(name)) {
            throw new Exception("pattern exists");
        } else {
            map.put(name, new Pattern(name, varName, pattern));
        }
    }

    public void replace(Tree<?> node) {
        Tree<?> nameNode = node.get(Symbol.unique("type"));
        String patternName = nameNode.toText();
        if (map.containsKey(patternName)) {
            Pattern pattern = map.get(patternName);
            pattern.replace((SyntaxTree)node);
        }
    }

    private class Pattern {
        //String name;
        String varName;
        Tree<?> pattern;
        public Pattern(String _name, String _varName, Tree<?> _pattern) {
            //name = _name;
            varName = _varName;
            pattern = _pattern;
        }

        public void replace(SyntaxTree node) {
            String v2 = node.get(Symbol.unique("var")).toText();

            SyntaxTree result = (SyntaxTree)pattern.dup();
            new ReplaceNameVisitor().start(result, varName, v2);
            node.setTag(result.getTag());
            node.setValue(result.getValue());
            node.replace(result);
        }
    }
}
