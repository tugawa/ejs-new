package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import java.util.LinkedList;
import java.util.HashMap;
import java.lang.Exception;

import vmdlc.AlphaConvVisitor.DefaultVisitor;

public class AlphaConvVisitor extends TreeVisitorMap<DefaultVisitor> {
    public AlphaConvVisitor() {
        init(AlphaConvVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node, boolean leaveName) {
        try {
            VarDict dict = new VarDict(leaveName);
            for (Tree<?> chunk : node) {
                visit(chunk, dict);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final void visit(Tree<?> node, VarDict dict) throws Exception {
        find(node.getTag().toString()).accept(node, dict);
    }

    private void print(Object o) {
        ConsoleUtils.println(o);
    }

    public class DefaultVisitor {
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            for (Tree<?> seq : node) {
                visit(seq, dict);
            }
        }
    }

    public class FunctionMeta extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            dict.createFrame();

            Tree<?> nameNode = node.get(Symbol.unique("name"));
            dict.internF(nameNode);

            Tree<?> def = node.get(Symbol.unique("definition"));
            visit(def, dict);

            dict.popFrame();
        }
    }
    public class FunctionDefinition extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            dict.createFrame();

            Tree<?> name = node.get(Symbol.unique("name"));
            visit(name, dict);

            Tree<?> params = node.get(Symbol.unique("params"));
            for (Tree<?> param : params) {
                dict.internPreserveName(param);
                visit(param, dict);
            }

            Tree<?> body = node.get(Symbol.unique("body"));

            for (Tree<?> seq : body) {
                visit(seq, dict);
            }

            dict.popFrame();
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            dict.createFrame();

            for (Tree<?> seq : node) {
                visit(seq, dict);
            }

            dict.popFrame();
        }
    }

    public class Declaration extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            Tree<?> name = node.get(Symbol.unique("var"));
            dict.internV(name);

            Tree<?> expr = node.get(Symbol.unique("expr"));
            visit(expr, dict);
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            Tree<?> name = node.get(Symbol.unique("var"));
            System.out.println(name);
            dict.internV(name);
        }
    }

    public class CFunction extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            Tree<?> name = node.get(Symbol.unique("name"));
            dict.internF(name);
        }
    }

    public  class DoInit extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            Tree<?> name = node.get(Symbol.unique("var"));
            dict.internV(name);

            Tree<?> expr = node.get(Symbol.unique("expr"));
            visit(expr, dict);
        }
    }

    public class Name extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, VarDict dict) throws Exception {
            String fname = (String)node.toText();
            node.setValue(dict.search(fname));
        }
    }
}


class VarDict {
    private NameMaker nameMaker;
    LinkedList<HashMap<String,String>> frames;
    HashMap<String, String> varmap;
    boolean leaveName;

    public VarDict(boolean _leaveName) {
        super();
        nameMaker = new NameMaker();
        frames = new LinkedList<HashMap<String,String>>();
        varmap = new HashMap<String, String>();
        leaveName = _leaveName;
    }

    public void createFrame() {
        frames.addFirst(new HashMap<String, String>());
    }
    public void popFrame() {
        frames.removeFirst();
    }

    public void internF(Tree<?> node) throws Exception {
        intern(node, "f");
    }
    public void internV(Tree<?> node) throws Exception {
        intern(node, "v");
    }

    private void intern(Tree<?> node, String prefix) throws Exception {
        String name = node.toText();
        String newName = nameMaker.getName(name, prefix);
        if (varmap.containsKey(newName)) {
            throw new Exception("Var exists");
        } else {
            varmap.put(newName, name);
            frames.getFirst().put(name, newName);
            node.setValue(newName);
        } 
    }

    void internPreserveName(Tree<?> node) throws Exception {
        String name = node.toText();
        if (varmap.containsKey(name)) {
            throw new Exception("Var exists");
        } else {
            varmap.put(name, name);
            frames.getFirst().put(name, name);
        } 
    }

    public String search(String s) {
        for (HashMap h : frames) {
            String v = (String)h.get(s);
            if (v != null) {
                return v;
            }
        }
        return s;
    }

    private class NameMaker {
        private int counter;
        NameMaker() {
            counter = 0;
        }
        public String getName(String name, String prefix) {
            counter++;
            if (leaveName) {
                return name + "_" + counter;
            } else {
                return prefix + counter;
            }
        }
    }
}