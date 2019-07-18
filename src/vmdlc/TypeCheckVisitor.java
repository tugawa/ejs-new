package vmdlc;

import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.util.ConsoleUtils;
import nez.ast.Symbol;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.HashSet;
import java.util.List;

import vmdlc.TypeCheckVisitor.DefaultVisitor;
import type.AstType.*;
import type.AstType;
import type.TypeMap;
import type.VMDataType;
import type.VMDataTypeVecSet;

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
        public TypeMap pop() {
            MatchRecord mr = stack.pop();
            return mr.dict;
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
    
    OperandSpecifications opSpec;
    
    public TypeCheckVisitor() {
        init(TypeCheckVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node, OperandSpecifications opSpec) {
        this.opSpec = opSpec;
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

    private final void save(SyntaxTree node, TypeMap dict) throws Exception {
        find(node.getTag().toString()).saveType(node, dict);
    }

    public class DefaultVisitor {
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
            for (SyntaxTree chunk : node) {
                save(chunk, dict);
            }
        }
    }

    public class FunctionMeta extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree type = node.get(Symbol.unique("type"));
            AstProductType funtype = (AstProductType)AstType.nodeToType((SyntaxTree)type);
            
            SyntaxTree definition = node.get(Symbol.unique("definition"));
            
            SyntaxTree nodeName = node.get(Symbol.unique("name"));
            SyntaxTree nameNode = definition.get(Symbol.unique("name"));
            String name = nameNode.toText();
            dict.add(name, funtype);
            
            Set<String> domain = new HashSet<String>(dict.getKeys());

            TypeMap newDict = dict.clone();

            /* add non-JSValue parameters */
            SyntaxTree paramsNode = definition.get(Symbol.unique("params"));
            if (paramsNode != null && paramsNode.size() != 0) {
                String[] paramNames = new String[paramsNode.size()];
                String[] jsvParamNames = new String[paramsNode.size()];
                AstType paramTypes = funtype.getDomain();
                int nJsvTypes = 0;
                if (paramTypes instanceof AstBaseType) {
                    AstType paramType = paramTypes;
                    String paramName = paramsNode.get(0).toText();
                    paramNames[0] = paramName;
                    if (paramType instanceof JSValueType)
                        jsvParamNames[nJsvTypes++] = paramName;
                    else
                        newDict.add(paramName, paramType);
                } else if (paramTypes instanceof AstPairType) {
                    List<AstType> paramTypeList = ((AstPairType) paramTypes).getTypes();
                    for (int i = 0; i < paramTypeList.size(); i++) {
                        AstType paramType = paramTypeList.get(i);
                        String paramName = paramsNode.get(i).toText();
                        paramNames[i] = paramName;
                        if (paramType instanceof JSValueType)
                            jsvParamNames[nJsvTypes++] = paramName;
                        else
                            newDict.add(paramName, paramType);
                    }
                }

                /* add JSValue parameters (apply operand spec) */
                if (nJsvTypes > 0) {
                    String[] jsvParamNamesPacked = new String[nJsvTypes];
                    System.arraycopy(jsvParamNames, 0, jsvParamNamesPacked, 0, nJsvTypes);
                    VMDataTypeVecSet vtvs = opSpec.getAccept(name, jsvParamNamesPacked);
                    newDict.add(vtvs);                
                }
            }

            SyntaxTree body = (SyntaxTree)definition.get(Symbol.unique("body"));
            dict = visit((SyntaxTree)body, newDict);

            save(nameNode, dict);
            save(nodeName, dict);
            save(paramsNode, dict);
            return dict.select((Set<String>)domain);
        }
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
        }
    }

    public class Parameters extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            for (SyntaxTree chunk : node) {
                // TODO: update dict
                visit(chunk, dict);
                save(chunk, dict);
            }
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            Set<String> domain = new HashSet<String>(dict.getKeys());
            for (SyntaxTree seq : node) {
                dict = visit(seq, dict);
                save(seq, dict);
            }
            TypeMap result = dict.select((Set<String>)domain);
            return result;
        }
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
        }
    }

    public class Match extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            MatchProcessor mp = new MatchProcessor(node);
            SyntaxTree labelNode= node.get(Symbol.unique("label"), null);
            String label = labelNode == null ? null : labelNode.toText();

            TypeMap outDict = dict.getBottomDict();

            TypeMap entryDict;
            TypeMap newEntryDict = dict;

            /*
            List<String> formalParams = new ArrayList<String>();
            for (String p: mp.getFormalParams())
                formalParams.add(p);
            int iterationCount = 0;
            do {
                iterationCount++;
                System.out.println("===ITERATION "+iterationCount+"===");
                entryDict = newEntryDict;
                matchStack.enter(label, mp.getFormalParams(), entryDict);
                System.out.println("entry = "+entryDict.select(formalParams));
                for (int i = 0; i < mp.size(); i++) {
                    TypeMap dictCaseIn = entryDict.enterCase(mp.getFormalParams(), mp.getVMDataTypeVecSet(i));
                    System.out.println("case in = "+dictCaseIn.select(formalParams));
                    if (dictCaseIn.hasBottom())
                        continue;
                    SyntaxTree body = mp.getBodyAst(i);
                    TypeMap dictCaseOut = visit(body, dictCaseIn);
                    System.out.println("case "+" = "+dictCaseOut.select(formalParams));
                    System.out.println(body.toText());
                    outDict = outDict.lub(dictCaseOut);
                }
                newEntryDict = matchStack.pop();
            } while (!entryDict.equals(newEntryDict));
            */

            do {
                entryDict = newEntryDict;
                matchStack.enter(label, mp.getFormalParams(), entryDict);
                for (int i = 0; i < mp.size(); i++) {
                    TypeMap dictCaseIn = entryDict.enterCase(mp.getFormalParams(), mp.getVMDataTypeVecSet(i));
                    if (dictCaseIn.hasBottom())
                        continue;
                    SyntaxTree body = mp.getBodyAst(i);
                    TypeMap dictCaseOut = visit(body, dictCaseIn);
                    outDict = outDict.lub(dictCaseOut);
                }
                newEntryDict = matchStack.pop();
            } while (!entryDict.equals(newEntryDict));
            
            node.setTypeMap(entryDict);

            SyntaxTree paramsNode= node.get(Symbol.unique("params"));
            save(paramsNode, outDict);

            return outDict;
        }
    }

    public class Rematch extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            String label = node.get(Symbol.unique("label")).toText();
            TypeMap matchDict = matchStack.getDict(label);
            if (matchDict == null)
                throw new Error("match label not found: "+label);
            Set<String> domain = matchDict.getKeys();
            String[] matchParams = matchStack.getParams(label);
            
            String[] rematchArgs = new String[matchParams.length];
            for (int i = 1; i < node.size(); i++) {
                rematchArgs[i-1] = node.get(i).toText();
            }
            
            TypeMap matchDict2 = dict.rematch(matchParams, rematchArgs, domain);
            TypeMap result = matchDict2.lub(matchDict);
            matchStack.updateDict(label, result);
            
            return dict.getBottomDict();
        }
    }
    
    public class Return extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = visit(node.get(0), dict).getExprType();
            save(node.get(0), dict);
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
        }
    }

    public class Assignment extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree right = node.get(Symbol.unique("right"));
            AstType rhsType = visit(right, dict).getExprType();
            SyntaxTree left = node.get(Symbol.unique("left"));
            dict.add(left.toText(), rhsType);
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
        }
    }

    public class AssignmentPair extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree right = node.get(Symbol.unique("right"));
            AstType rhsType = visit(right, dict).getExprType();
            if (rhsType instanceof AstPairType) {
                ArrayList<AstType> types = ((AstPairType)rhsType).getTypes();
                SyntaxTree left = node.get(Symbol.unique("left"));
                if (types.size() != left.size()) {
                    throw new Error("AssignmentPair: return type error");
                }
                for (int i = 0; i < types.size(); i++) {
                    dict.add(left.get(i).toText(), types.get(i));
                }

                return dict;
            } else {
                throw new Error("AssignmentPair: type error");
            }
        }
        @Override
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
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
            dict.add(var.toText(), rhsType);

            save(expr, dict);
            save(var, dict);
            return dict;
        }
        @Override
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
        }
    }
    public class If extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            TypeMap copyDict = dict.clone();
            SyntaxTree thenNode = node.get(Symbol.unique("then"));
            
            TypeMap thenDict = visit(thenNode, dict);
            SyntaxTree elseNode = node.get(Symbol.unique("else"));

            TypeMap resultDict;
            if (elseNode == null) {
                resultDict = thenDict;
            } else {
                TypeMap elseDict = visit(elseNode, copyDict);
                resultDict = thenDict.lub(elseDict);
            }
            SyntaxTree condNode = node.get(Symbol.unique("cond"));
            save(condNode, resultDict);

            return resultDict;
        }
        @Override
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
        }
    }

    public class Do extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree initNode = node.get(Symbol.unique("init"));
            SyntaxTree varNode = initNode.get(Symbol.unique("var"));
            SyntaxTree exprNode = initNode.get(Symbol.unique("expr"));
            AstType rhsType = visit(exprNode, dict).getExprType();
            dict.add(varNode.toText(), rhsType);

            TypeMap savedDict;
            do {
                savedDict = dict.clone();
                SyntaxTree blockNode = initNode.get(Symbol.unique("block"));
                dict = visit(blockNode, dict);
            } while (!dict.equals(savedDict));

            return dict;
        }
    }

    public class CTypeDef extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            AstType type = AstType.get(typeNode.toText());
            dict.add(varNode.toText(), type);
            
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
            dict.add(nameNode.toText(), type);
            
            return dict;
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            AstType type = AstType.get(typeNode.toText());
            dict.add(varNode.toText(), type);
            
            return dict;
        }
    }

    public class Trinary extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            //TypeMap copyDict = dict.clone();
            SyntaxTree thenNode = node.get(Symbol.unique("then"));
            AstType thenType = visit(thenNode, dict).getExprType();
            
            SyntaxTree elseNode = node.get(Symbol.unique("else"));
            //AstBaseType elseType = visit(elseNode, copyDict).getExprType();
            AstType elseType = visit(elseNode, dict).getExprType();

            TypeMap resultMap = new TypeMap(thenType.lub(elseType));
            return resultMap;
        }
    }
    public class Or extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }
    public class And extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }
    public class BitwiseOr extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = bitwiseOperator(node, dict);
            return new TypeMap(t);
        }
    }

    public class BitwiseXOr extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = bitwiseOperator(node, dict);
            return new TypeMap(t);
        }
    }

    public class BitwiseAnd extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = bitwiseOperator(node, dict);
            return new TypeMap(t);
        }
    }

    public class Equals extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }

    public class NotEquals extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }

    public class LessThanEquals extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }
    public class GreaterThanEquals extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }
    public class LessThan extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }
    public class GreaterThan extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }
    public class LeftShift extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree leftNode = node.get(Symbol.unique("left"));
            AstType leftType = visit(leftNode, dict).getExprType();
            return new TypeMap(leftType);
        }
    }
    public class RightShift extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree leftNode = node.get(Symbol.unique("left"));
            AstType leftType = visit(leftNode, dict).getExprType();
            return new TypeMap(leftType);
        }
    }
    
    AstType tCint = AstType.get("cint");
    AstType tCdouble = AstType.get("cdouble");
    AstType tCstring = AstType.get("cstring");
    
    private AstType numberOperator(SyntaxTree node, TypeMap dict) throws Exception {
        SyntaxTree leftNode = node.get(Symbol.unique("left"));
        AstType lType = visit(leftNode, dict).getExprType();
        SyntaxTree rightNode = node.get(Symbol.unique("right"));
        AstType rType = visit(rightNode, dict).getExprType();

        if (lType == tCint || rType == tCint) {
            return tCint;
        } else if ((lType == tCint || lType == tCdouble) || (rType == tCint || rType == tCdouble)){
            return tCdouble;
        }
        throw new Error("type error");
    }

    private AstType bitwiseOperator(SyntaxTree node, TypeMap dict) throws Exception {
        SyntaxTree leftNode = node.get(Symbol.unique("left"));
        AstType lType = visit(leftNode, dict).getExprType();
        SyntaxTree rightNode = node.get(Symbol.unique("right"));
        AstType rType = visit(rightNode, dict).getExprType();

        if (lType == tCint && rType == tCint)
            return tCint;
        throw new Error("type error");
    }

    public class Add extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = numberOperator(node, dict);
            return new TypeMap(t);
        }
    }
    public class Sub extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = numberOperator(node, dict);
            return new TypeMap(t);
        }
    }
    public class Mul extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = numberOperator(node, dict);
            return new TypeMap(t);
        }
    }
    public class Div extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = numberOperator(node, dict);
            return new TypeMap(t);
        }
    }
    public class Mod extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            AstType t = numberOperator(node, dict);
            return new TypeMap(t);
        }
    }

    public class Plus extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            AstType exprType = visit(exprNode, dict).getExprType();
            return new TypeMap(exprType);
        }
    }
    public class Minus extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            AstType exprType = visit(exprNode, dict).getExprType();
            return new TypeMap(exprType);
        }
    }
    public class Compl extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            AstType exprType = visit(exprNode, dict).getExprType();
            return new TypeMap(exprType);
        }
    }
    public class Not extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("Bool"));
        }
    }
    public class FunctionCall extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree recv = node.get(Symbol.unique("recv"));
            String funName = recv.toText();
            
            if (!dict.containsKey(funName)) {
                throw new Error("FunctionCall: no such name: "+funName+" :"+node.getSource().getResourceName()+" :"+node.getLineNum());
            }
            AstProductType funType = (AstProductType)dict.get(funName);
            AstBaseType rangeType = (AstBaseType)funType.getRange();
           
            // TODO: type check
            for (SyntaxTree arg : node.get(1)) {
                AstType argType = visit(arg, dict).getExprType();
            }
            
            return new TypeMap(rangeType);
        }
    }

    public class ArrayIndex extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            // TODO
            return dict;
        }
    }
    public class Field extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            // TODO
            return dict;
        }
    }
    public class _Integer extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cint"));
        }
    }

    public class _Float extends DefaultVisitor {
        @Override
        public TypeMap accept(SyntaxTree node, TypeMap dict) throws Exception {
            return new TypeMap(AstType.get("cdouble"));
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
            if (!dict.containsKey(node.toText())) {
                throw new Error("Name: no such name: "+node.getSource().getResourceName()+": "+node.getLineNum());
            }
            AstType type = dict.get(node.toText());

            return new TypeMap(type);
        }
        public void saveType(SyntaxTree node, TypeMap dict) throws Exception {
            if (dict.containsKey(node.toText())) {
                AstType type = dict.get(node.toText());
                node.setType(type);
            }
        }
    }
}
