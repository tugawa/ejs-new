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
import java.util.Stack;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.lang.Exception;
import java.lang.Integer;

import vmdlc.AstToCVisitor.DefaultVisitor;

import dispatch.DispatchProcessor;
import dispatch.DispatchPlan;
import dispatch.RuleSet;
import type.AstType;
import type.ExprTypeSet;
import type.FunctionAnnotation;
import type.FunctionTable;
import type.TypeMapSet;
import type.VMDataType;
import type.AstType.AstAliasType;
import type.AstType.AstMappingType;
import type.AstType.AstPairType;
import type.AstType.AstProductType;

public class AstToCVisitor extends TreeVisitorMap<DefaultVisitor> {
    static final boolean OUTPUT_DEBUG_INFO = false;
    static final boolean VM_INSTRUCTION = true;
    static class MatchRecord {
        static int next = 1;
        String name;
        String functionName;
        String matchLabel;
        String[] opNames;
        MatchRecord(String functionName, String matchLabel, int lineNum, String[] opNames) {
            this.matchLabel = matchLabel;
            this.functionName = functionName;
            if (matchLabel != null)
                name = matchLabel +"AT"+lineNum;
            else
                name = (next++)+"AT"+lineNum;
            this.opNames = opNames;
        }
        String getHeadLabel() {
            String labelPrefix = Main.option.getOption(Option.AvailableOptions.GEN_LABEL_PREFIX, functionName);
            return "MATCH_HEAD_"+labelPrefix+"_"+name;
        }
        String getTailLabel() {
            String labelPrefix = Main.option.getOption(Option.AvailableOptions.GEN_LABEL_PREFIX, functionName);
            return "MATCH_TAIL_"+labelPrefix+"_"+name;
        }
        boolean hasMatchLabelOf(String label) {
            return matchLabel != null && matchLabel.equals(label);
        }
    }
    private static Map<String, String> cConstTable = new HashMap<>();
    Stack<StringBuffer> outStack;
    Stack<MatchRecord> matchStack;
    String currentFunctionName;
    OperandSpecifications opSpec;
    Main.OutputMode outputMode;
    
    public static void addCConstant(String name, String cValue){
        cConstTable.put(name, cValue);
    }

    public AstToCVisitor() {
        init(AstToCVisitor.class, new DefaultVisitor());
        outStack = new Stack<StringBuffer>();
        matchStack = new Stack<MatchRecord>();
    }

    public String start(Tree<?> node, OperandSpecifications opSpec, Main.OutputMode outputMode) {
        this.opSpec = opSpec;
        this.outputMode = outputMode;
        try {
            outStack.push(new StringBuffer());
            for (Tree<?> chunk : node) {
                visit(chunk, 0);
            }
            StringBuffer sb = outStack.pop();
            if(!outputMode.isFunctionMode()) sb.append(getEpilogueLabel() + ": ;\n");
            String program = sb.toString();
            return program;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("visitor thrown an exception");
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

    private String getEpilogueLabel() {
        String labelPrefix = Main.option.getOption(Option.AvailableOptions.GEN_LABEL_PREFIX, currentFunctionName);
        return "L"+labelPrefix+"_EPILOGUE";
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
            Tree<?> nameNode = node.get(Symbol.unique("name"));
            String name = nameNode.toText();
            currentFunctionName = name;

            Tree<?> bodyNode = node.get(Symbol.unique("definition"));
            if(outputMode.isFunctionMode()){
                SyntaxTree typeNode = (SyntaxTree)node.get(Symbol.unique("type"));
                AstType type = AstType.nodeToType(typeNode);
                if(!(type instanceof AstProductType)){
                    throw new Error("Function is not function type");
                }
                AstProductType funType = (AstProductType)type;
                AstType funDomainType = funType.getDomain();
                List<AstType> varTypes;
                if(funDomainType instanceof AstPairType){
                    AstPairType d = (AstPairType) funDomainType;
                    varTypes = d.getTypes();
                }else{
                    varTypes = new ArrayList<>(1);
                    varTypes.add(funDomainType);
                }
                String typeString;
                if(AstType.get("JSValue").isSuperOrEqual(funType.getRange())){
                    typeString = "JSValue";
                }else{
                    typeString = funType.getRange().toString();
                    if(typeString.equals("cdouble")) typeString = "double";
                    else if(typeString.equals("cstring")) typeString = "char*";
                }
                print(typeString+" "+name+" (");
                SyntaxTree paramsNode = ((SyntaxTree)bodyNode).get(Symbol.unique("params"));
                int size = paramsNode.size();
                if(!FunctionTable.contains(name)){
                    throw new Error("FunctionTable is broken: not has "+name);
                }
                if(FunctionTable.hasAnnotations(name, FunctionAnnotation.needContext)){
                    print("Context* context");
                    if(size != 0){
                        print(", ");
                    }
                }
                int i=0;
                while(true){
                    AstType varType = varTypes.get(i);
                    String varName = paramsNode.get(i).toText();
                    AstType realVarType;
                    String typeName;
                    if(varType instanceof AstAliasType){
                        realVarType = AstType.get(((AstAliasType)varType).getCTypeName());
                    }else{
                        realVarType = varType;
                    }
                    if(AstType.get("JSValue").isSuperOrEqual(realVarType)){
                        typeName = "JSValue";
                    }else{
                        typeName = realVarType.toString();
                    }
                    print(typeName+" "+varName);
                    i++;
                    if(i>=size) break;
                    print(", ");
                }
                println(") {");
                indent++;
            }
            visit(bodyNode, indent);
            if(outputMode.isFunctionMode()){
                print("}");
            }
        }
    }
    public class FunctionDefinition extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
            if(!outputMode.isFunctionMode()){
                Tree<?> funNameNode = node.get(Symbol.unique("name"));
                Tree<?> paramsNode = node.get(Symbol.unique("params"));
                String[] jsvParams = new String[paramsNode.size()];
                int jsvNum = 0;

                for (int i = 0; i < paramsNode.size(); i++) {
                    String paramName = paramsNode.get(i).toText();
                    // JSValue parameter's name starts with v as defined in InstructionDefinitions.java
                    if (paramName.startsWith("v")) {
                        jsvParams[jsvNum++] = paramName;
                    }
                }
                println("DEFLABEL(HEAD):");
                print("INSN_COUNT" + jsvNum + "(" + funNameNode.toText());
                for (int i = 0; i < jsvNum; i++) {
                    print("," + jsvParams[i]);
                }
                println(");");
            }

            Tree<?> bodyNode = node.get(Symbol.unique("body"));
            visit(bodyNode, indent);
        }
    }

    public class CFunction extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
        }
    }

    public class CConstantDef extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
        }
    }

    public class CTypeDef extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
        }
    }

    public class CObjectmapping extends DefaultVisitor {
        public void accept(Tree<?> node, int indent) throws Exception {
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

    private static Map<String, Integer> matchLabelGeneratedSizeMap = new HashMap<>();

    public class Match extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            MatchProcessor mp = new MatchProcessor((SyntaxTree) node);
            String[] formalParams = mp.getFormalParams();
            String rawLabel = mp.getLabel();
            String label;
            if(outputMode.isFunctionMode()){
                Integer labelCount = matchLabelGeneratedSizeMap.get(rawLabel);
                if(labelCount == null){
                    labelCount = 0;
                }
                matchLabelGeneratedSizeMap.put(rawLabel, labelCount + 1);
                label = "_" + rawLabel + labelCount;
            }else{
                label = "";
            }

            TypeMapSet dict = ((SyntaxTree) node).getTypeMapSet();

            println("/* "+dict.toString()+" */");

            matchStack.add(new MatchRecord(currentFunctionName, rawLabel, node.getLineNum(), formalParams));
            print(matchStack.peek().getHeadLabel()+":"+"\n");

            Set<RuleSet.Rule> rules = new HashSet<RuleSet.Rule>();
            Set<VMDataType[]> acceptInput = new HashSet<>();

            for (int i = 0; i < mp.size(); i++) {
                Set<VMDataType[]> vmtVecs = mp.getVmtVecCond(i);
                if (!Main.option.disableMatchOptimisation())
                    vmtVecs = dict.filterTypeVecs(formalParams, vmtVecs);
                if (vmtVecs.size() == 0)
                    continue;

                acceptInput.addAll(vmtVecs);
                
                /* action */
                outStack.push(new StringBuffer());
                Tree<?> stmt = mp.getBodyAst(i);
                visit(stmt, 0);
                String action = outStack.pop().toString();

                /* OperandDataTypes set */
                Set<RuleSet.OperandDataTypes> odts = new HashSet<RuleSet.OperandDataTypes>();
                for (VMDataType[] vmtVec: vmtVecs) {
                    RuleSet.OperandDataTypes odt = new RuleSet.OperandDataTypes(vmtVec);
                    odts.add(odt);
                }

                /* debug */
                if (OUTPUT_DEBUG_INFO) {
                    StringBuffer sb = new StringBuffer();
                    for (VMDataType[] vmts: vmtVecs) {
                        sb.append("/*");
                        for(VMDataType vmt: vmts)
                            sb.append(" "+vmt);
                        sb.append(" */\n");
                    }
                    action = sb.toString() + action;
                }

                RuleSet.Rule r = new RuleSet.Rule(action, odts);
                rules.add(r);
            }

            /* print error types (NOT in accept types) */
            Set<VMDataType[]> errorInput = opSpec.getErrorOperands(currentFunctionName);
            Set<RuleSet.OperandDataTypes> errorConditions = new HashSet<RuleSet.OperandDataTypes>();
            NEXT_DTS: for (VMDataType[] dts: errorInput) {
                int length = dts.length;
                NEXT_ARRAY: for (VMDataType[] accept: acceptInput) {
                    for(int i=0; i<length; i++){
                        if(!dts[i].equals(accept[i])) continue NEXT_ARRAY;
                    }
                    continue NEXT_DTS;
                }
                errorConditions.add(new RuleSet.OperandDataTypes(dts));
            }
            String errorAction = new String("LOG_EXIT(\"unexpected operand type\\n\");\n");
            if (errorConditions.size() > 0) {
                rules.add(new RuleSet.Rule(errorAction, errorConditions));
            }

            RuleSet rs = new RuleSet(formalParams, rules);

            DispatchPlan dp = new DispatchPlan(Main.option);
            DispatchProcessor dispatchProcessor = new DispatchProcessor();
            String labelPrefix = Main.option.getOption(Option.AvailableOptions.GEN_LABEL_PREFIX, currentFunctionName);
            dispatchProcessor.setLabelPrefix(labelPrefix + "_"+ matchStack.peek().name + "_");
            String s = dispatchProcessor.translate(rs, dp, Main.option, currentFunctionName, label);
            println(s);

            println(matchStack.pop().getTailLabel()+": ;");
        }
    }

    public class Return extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            if (outputMode.isFunctionMode()) {
                printIndent(indent, "return ");
                for (Tree<?> expr : node) {
                    visit(expr, 0);
                }
                println(";");
            } else {
                printIndent(indent, "regbase[r0] = ");
                for (Tree<?> expr : node) {
                    visit(expr, 0);
                }
                println(";");
                println("goto "+getEpilogueLabel()+";");
            }
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
            Tree<?> limitNode = node.get(Symbol.unique("limit"));
            Tree<?> stepNode = node.get(Symbol.unique("step"));
            Tree<?> blockNode = node.get(Symbol.unique("block"));
            printIndent(indent, "for (");
            visit(initNode, 0);
            Tree<?> varNode = initNode.get(Symbol.unique("var"));
            print("; ");
            visit(varNode, 0);
            print("<=");
            visit(limitNode, 0);
            print("; ");
            visit(varNode, 0);
            print("+=");
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
            print("=");
            visit(exprNode, 0);
        }
    }
    public class Rematch extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            Tree<?> targetNode = node.get(Symbol.unique("label"));
            String target = targetNode.toText();

            println("{");
            for (int i = matchStack.size() - 1; i >= 0; i--) {
                MatchRecord mr = matchStack.elementAt(i);
                if (mr.hasMatchLabelOf(target)) {
                    for (int j = 0; j < mr.opNames.length; j++) {
                        Tree<?> argNode = node.get(j + 1);
                        print("JSValue tmp"+j+" = ");
                        visit(argNode, 0);
                        println(";");
                    }
                    for (int j = 0; j < mr.opNames.length; j++)
                        println(mr.opNames[j]+" = "+"tmp"+j+";");
                    println("goto "+mr.getHeadLabel()+";");
                    println("}");
                    return;
                }
            }
            throw new Error("no rematch target:"+ target);
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
    public class Mod extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            printOperator(node, "%");
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
    public class FunctionCall extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            SyntaxTree expandedNode = ((SyntaxTree)node).getExpanndedTree();
            if(expandedNode != null){
                // TEST PRINT ******************************************
                //System.err.println("Original:"+node.toString());
                //System.err.println("Expanded:"+expandedNode.toString());
                // *****************************************************
                visit(expandedNode, indent);
                return;
            }
            String functionName = node.get(0).toText();
            visit(node.get(0), 0);
            print("(");
            if(!FunctionTable.contains(functionName)){
                throw new Error("FunctionTable is broken: not has "+functionName);
            }
            Tree<?> argsNode = node.get(1);
            if(FunctionTable.hasAnnotations(functionName, FunctionAnnotation.needContext)){
                print("context");
                if(argsNode.size() != 0){
                    print(", ");
                }
            }
            visit(argsNode, 0);
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

    public class FieldAccess extends DefaultVisitor {
        private void pointerPrint(Tree<?> recvNode, Tree<?> fieldNode) throws Exception {
            print("*(");
            embeddedPrint(recvNode, fieldNode);
            print(")");
        }
        private void embeddedPrint(Tree<?> recvNode, Tree<?> fieldNode) throws Exception {
            visit(recvNode, 0);
            print(".");
            visit(fieldNode, 0);
        }
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            SyntaxTree recvNode = (SyntaxTree) node.get(Symbol.unique("recv"));
            SyntaxTree fieldNode = (SyntaxTree) node.get(Symbol.unique("field"));
            ExprTypeSet exprTypeSet = recvNode.getExprTypeSet();
            if(exprTypeSet.getTypeSet().size() != 1){
                throw new Error("Illigal field access");
            }
            AstType type = exprTypeSet.getOne();
            if(!(type instanceof AstMappingType)){
                throw new Error("Illigal field access");
            }
            AstMappingType mtype = (AstMappingType)type;
            String fieldName = fieldNode.toText();
            Set<String> annotaions = mtype.getFieldAnnotations(fieldName);
            if(annotaions != null){
                if(annotaions.contains("embedded")){
                    embeddedPrint(recvNode, fieldNode);
                    return;
                }
            }
            pointerPrint(recvNode, fieldNode);
        }
    }

    public class LeftHandField extends FieldAccess{
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            super.accept(node, indent);
        }
    }

    public class _Float extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print(node.toText());
        }
    }
    public class _Integer extends DefaultVisitor {
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
            String name = node.toText();
            if(cConstTable.containsKey(name)){
                print(cConstTable.get(name));
            }else{
                print(name);
            }
        }
    }
    public class JSValueTypeName extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            print("JSValue");
        }
    }
    public class UserTypeName extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            AstType type = AstType.nodeToType((SyntaxTree)node);
            String typeName;
            if(type instanceof AstAliasType){
                typeName = ((AstAliasType)type).getCTypeName();
            }else{
                typeName = type.toString();
            }
            print(typeName);
        }
    }
    public class Ctype extends DefaultVisitor {
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            HashMap<String, String> varmap = new HashMap<String, String>();
            varmap.put("cint", "int");
            varmap.put("cdouble", "double");
            varmap.put("cstring", "char*");
            varmap.put("Displacement", "Displacement");
            varmap.put("Subscript", "Subscript");
            //NOTE: HeapObject cannnot print
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
    public class TypeArray extends DefaultVisitor{
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            visit(node.get(Symbol.unique("type")), indent);
            print("[]");
        }
    }
    public class ArrayIndex extends DefaultVisitor{
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            visit(node.get(Symbol.unique("recv")), indent);
            print("[");
            visit(node.get(Symbol.unique("index")), indent);
            print("]");
        }
    }

    public class LeftHandIndex extends ArrayIndex{
        @Override
        public void accept(Tree<?> node, int indent) throws Exception {
            super.accept(node, indent);
        }
    }
}
