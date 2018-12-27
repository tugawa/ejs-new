package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import java.util.HashMap;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.lang.Exception;

import vmdlc.AstToCVisitor.DefaultVisitor;

import dispatch.DispatchProcessor;
import dispatch.DispatchPlan;
import dispatch.RuleSet;
import type.VMDataType;

public class AstToCVisitor extends TreeVisitorMap<DefaultVisitor> {
    Stack<StringBuffer> outStack;

    public AstToCVisitor() {
        init(AstToCVisitor.class, new DefaultVisitor());
        outStack = new Stack<StringBuffer>();
    }

    public void start(Tree<?> node) {
        try {
            for (Tree<?> chunk : node) {
                visit(chunk, 0);
            }
        } catch (Exception e) {
        }
    }

    private final void visit(Tree<?> node, int indent) throws Exception {
        find(node.getTag().toString()).accept(node, indent);
    }

    private void print(Object o) {
        outStack.peek().append(o);
    }

    private void println(Object o) {
        outStack.peek().append(o + "\n");
    }

    private void printOperator(Tree<?> node, String s) throws Exception {
        Tree<?> leftNode = node.get(Symbol.unique("left"));
        Tree<?> rightNode = node.get(Symbol.unique("right"));
        print("(");
        visit(leftNode, 0);
        print(s);
        visit(rightNode, 0);
        print(")");
    }
    private void printIndent(int indent, String s) {
        for (int i = 0; i < indent; i++) {
            print("  ");
        }
        print(s);
    }
    private void printIndentln(int indent, String s) {
        printIndent(indent, s);
        println("");
    }

    public class DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
            for (Tree<?> seq : node) {
                visit(seq, indent);
            }
        }
    }

    public class PatternDefinition extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
        }
    }
    public class FunctionMeta extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> bodyNode = node.get(Symbol.unique("definition"));
            visit(bodyNode, indent);
        }
    }
    public class FunctionDefinition extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> bodyNode = node.get(Symbol.unique("body"));
            visit(bodyNode, indent);
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            for (Tree<?> seq : node) {
                visit(seq, indent + 1);
            }
        }
    }

    public class Match extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> params = node.get(Symbol.unique("params"));
            String[] formalParams = new String[params.size()];
            for (int i = 0; i < params.size(); i++) {
                formalParams[i] = params.get(i).toText();
            }

            Set<RuleSet.Rule> rules = new HashSet<RuleSet.Rule>();
            Tree<?> cases = node.get(Symbol.unique("cases"));
            for (Tree<?> cas: cases) {
                Tree<?> pat = cas.get(Symbol.unique("pattern"));
                ArrayList<RuleSet.OperandDataTypes> odts = makeOdts(pat);
                outStack.push(new StringBuffer());
                Tree<?> statement = cas.get(Symbol.unique("body"));
                visit(statement, 0);
                String action = outStack.pop().toString();
                RuleSet.Rule rule = new RuleSet.Rule(action, odts);
                rules.add(rule);
            }
            RuleSet ruleSet = new RuleSet(formalParams, rules);

            DispatchPlan dp = new DispatchPlan(params.size(), false);
            DispatchProcessor dispatchProcessor = new DispatchProcessor();
            String s = dispatchProcessor.translate(ruleSet, dp);
            println(s);
        }

        private ArrayList<RuleSet.OperandDataTypes> makeOdts(Tree<?> pat) {
            ArrayList<RuleSet.OperandDataTypes> result = new ArrayList<RuleSet.OperandDataTypes>();
            _makeOdts(pat, result);
            return result;
        }
        private void _makeOdts(Tree<?> pat, ArrayList<RuleSet.OperandDataTypes> result) {
            if (pat.is(Symbol.unique("OrPattern"))) {
                for (Tree<?> child : pat) {
                    _makeOdts(child, result);
                }
            } else if (pat.is(Symbol.unique("AndPattern"))) {
                VMDataType[] dt = new VMDataType[pat.size()];
                
                for (int i = 0; i < pat.size(); i++) {
                    String s = pat.get(i).get(Symbol.unique("type")).toText();
                    dt[i] = VMDataType.get(s.toLowerCase());
                }
                RuleSet.OperandDataTypes odt = new RuleSet.OperandDataTypes(dt);
                result.add(odt);
            }
        }
    }

    public class Return extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printIndent(indent, "return ");
            for (Tree<?> expr : node) {
                visit(expr, 0);
            }
            println(";");
        }
    }

    public class Assignment extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printIndent(indent, "");
            Tree<?> leftNode = node.get(Symbol.unique("left"));
            Tree<?> rightNode = node.get(Symbol.unique("right"));
            visit(leftNode, 0);
            print(" = ");
            visit(rightNode, 0);
            println(";");
        }
    }
    public class AssignmentPair extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printIndent(indent, "");
            Tree<?> leftNode = node.get(Symbol.unique("left"));
            Tree<?> rightNode = node.get(Symbol.unique("right"));
            Tree<?> fname = rightNode.get(Symbol.unique("recv"));
            print(fname.toText());
            print("(");
            
            for (Tree<?> child : rightNode) {
                if (child.is(Symbol.unique("ArgList"))) {
                    int i = 0;
                    for (i = 0; i < child.size(); i++) {
                        visit(child.get(i), 0);
                        print(", ");
                    }
                    int j = 0;
                    for (j = 0; j < leftNode.size() - 1; j++) {
                        print("&");
                        visit(leftNode.get(j), 0);
                        print(", ");
                    }
                    print("&");
                    visit(leftNode.get(j), 0);
                    
                    break;
                }
            }
            println(");");
        }
    }

    public class ExpressionStatement extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printIndent(indent, "");
            visit(node.get(0), indent);
            println(";");
        }
    }
    public class Declaration extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> typeNode = node.get(Symbol.unique("type"));
            Tree<?> varNode = node.get(Symbol.unique("var"));
            Tree<?> exprNode = node.get(Symbol.unique("expr"));
            visit(typeNode, 0);
            print(" ");
            visit(varNode, 0);
            print(" = ");
            visit(exprNode, 0);
            println(";");
        }
    }
    public class If extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> condNode = node.get(Symbol.unique("cond"));
            Tree<?> thenNode = node.get(Symbol.unique("then"));
            printIndent(indent, "if (");
            visit(condNode, indent + 1);
            println(") {");
            visit(thenNode, indent + 1);
            printIndentln(indent, "}");
            if (node.has(Symbol.unique("else"))) {
                Tree<?> elseNode = node.get(Symbol.unique("else"));
                printIndentln(indent, "else {");
                visit(elseNode, indent + 1);
                printIndentln(indent, "}");
            }
        }
    }
    public class Do extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> initNode = node.get(Symbol.unique("init"));
            Tree<?> stepNode = node.get(Symbol.unique("step"));
            Tree<?> blockNode = node.get(Symbol.unique("block"));
            printIndent(indent, "for (");
            visit(initNode, 0);
            Tree<?> varNode = initNode.get(Symbol.unique("var"));
            print(";;");
            visit(varNode, 0);
            print("=");
            visit(stepNode, 0);
            println(") {");
            visit(blockNode, indent + 1);
            printIndentln(indent, "}");
        }
    }
    public class DoInit extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> typeNode = node.get(Symbol.unique("type"));
            Tree<?> varNode = node.get(Symbol.unique("var"));
            Tree<?> exprNode = node.get(Symbol.unique("expr"));
            visit(typeNode, 0);
            print(" ");
            visit(varNode, 0);
            print(" = ");
            visit(exprNode, 0);
        }
    }

    public class Trinary extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> condNode = node.get(Symbol.unique("cond"));
            Tree<?> thenNode = node.get(Symbol.unique("then"));
            Tree<?> elseNode = node.get(Symbol.unique("else"));
            visit(condNode, 0);
            print(" ? ");
            visit(thenNode, 0);
            print(" : ");
            visit(elseNode, 0);
        }
    }
    public class Or extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "||");
        }
    }
    public class And extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "&&");
        }
    }
    public class BitwiseOr extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "|");
        }
    }
    public class BitwiseXor extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "^");
        }
    }
    public class BitwiseAnd extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "&");
        }
    }
    public class Equals extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "==");
        }
    }
    public class NotEquals extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "!=");
        }
    }
    public class LessThanEquals extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "<=");
        }
    }
    public class GreaterThanEquals extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, ">=");
        }
    }
    public class LessThan extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "<");
        }
    }
    public class GreaterThan extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, ">");
        }
    }
    public class LeftShift extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "<<");
        }
    }
    public class RightShift extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, ">>");
        }
    }
    public class Add extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "+");
        }
    }
    public class Sub extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "-");
        }
    }
    public class Mul extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "*");
        }
    }
    public class Div extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "/");
        }
    }
    public class Plus extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "+");
        }
    }
    public class Minus extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "-");
        }
    }
    public class Compl extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "~");
        }
    }
    public class Not extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "!");
        }
    }
    public class Apply extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            visit(node.get(0), 0);
            print("(");
            for (int i = 1; i < node.size(); i++) {
                visit(node.get(i), 0);
            }
            print(")");
        }
    }
    public class ArgList extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            int i;
            for (i = 0; i < node.size() - 1; i++) {
                visit(node.get(i), 0);
                print(", ");
            }
            if (node.size() != 0) {
                visit(node.get(node.size() - 1), 0);
            }
        }
    }

    public class Index extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            visit(node.get(0), 0);
            print("[");
            visit(node.get(1), 0);
            print("]");

        }
    }
    public class Field extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            visit(node.get(0), 0);
            print(".");
            visit(node.get(1), 0);
        }
    }
    public class Float extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print(node.toText());
        }
    }
    public class Integer extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print(node.toText());
        }
    }
    public class _String extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print("\"");
            print(node.toText());
            print("\"");
        }
    }
    public class _Character extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print("\'");
            print(node.toText());
            print("\'");
        }
    }
    public class _True extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print("1");
        }
    }
    public class _False extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print("0");
        }
    }

    public class Name extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print(node.toText());
        }
    }
    public class TypeName extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print(node.toText());
        }
    }
    public class Ctype extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            HashMap<String, String> varmap = new HashMap<String, String>();
            varmap.put("cint", "int");
            varmap.put("cdouble", "double");
            print(varmap.get(node.toText()));
        }
    }
    public class CValue extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print("\"");
            print(node.toText());
            print("\"");
        }
    }
    
    /*
    public class Trinary extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
        }
    }
    */
}
