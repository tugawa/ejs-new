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

import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;

import vmdlc.TypeCheckVisitor.DefaultVisitor;
import type.AstType.*;
import type.AstType;
import type.TypeMapBase;
import type.VMDataType;
import type.VMDataTypeVecSet;

public class TypeCheckVisitor extends TreeVisitorMap<DefaultVisitor> {
    static class MatchStack {
        static class MatchRecord {
            String name;
            String[] formalParams;
            TypeMapBase dict;
            MatchRecord(String name, String[] formalParams, TypeMapBase dict) {
                this.name = name;
                this.formalParams = formalParams;
                this.dict = dict;
            }
            public void setDict(TypeMapBase _dict) {
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

        public void enter(String name, String[] formalParams, TypeMapBase dict) {
            MatchRecord mr = new MatchRecord(name, formalParams, dict);
            stack.push(mr);
        }
        public String[] getParams(String name) {
            MatchRecord mr = lookup(name);
            if (mr == null)
                return null;
            return mr.formalParams;
        }
        public TypeMapBase getDict(String name) {
            MatchRecord mr = lookup(name);
            if (mr == null)
                return null;
            return mr.dict;
        }
        public TypeMapBase pop() {
            MatchRecord mr = stack.pop();
            return mr.dict;
        }
        public boolean isEmpty() {
            return stack.isEmpty();
        }
        public void updateDict(String name, TypeMapBase dict) {
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

    public static TypeMapBase TYPE_MAP;

    public TypeCheckVisitor() {
        init(TypeCheckVisitor.class, new DefaultVisitor());
        numberOperatorInitialize();
    }

    public void start(Tree<?> node, OperandSpecifications opSpec, TypeMapBase typeMap) {
        this.opSpec = opSpec;
        try {
            TYPE_MAP = typeMap;
            TypeMapBase dict = TYPE_MAP.clone();
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

    private final TypeMapBase visit(SyntaxTree node, TypeMapBase dict) throws Exception {
        /*
        System.err.println("==================");
        System.err.println(node.getTag().toString());
        System.err.println(node.toString());
        System.err.println("----");
        System.err.println("dict:"+dict.toString());
        System.err.println("----");
        System.err.println("exprMap:"+dict.getExprTypeMap().toString());
        */
        return find(node.getTag().toString()).accept(node, dict);
    }

    private final void save(SyntaxTree node, TypeMapBase dict) throws Exception {
        find(node.getTag().toString()).saveType(node, dict);
    }

    public class DefaultVisitor {
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
            for (SyntaxTree chunk : node) {
                save(chunk, dict);
            }
        }
    }

    public class FunctionMeta extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree type = node.get(Symbol.unique("type"));
            AstProductType funtype = (AstProductType)AstType.nodeToType((SyntaxTree)type);

            SyntaxTree definition = node.get(Symbol.unique("definition"));

            SyntaxTree nodeName = node.get(Symbol.unique("name"));
            SyntaxTree nameNode = definition.get(Symbol.unique("name"));
            String name = nameNode.toText();
            dict.addGlobal(name, funtype);

            Set<String> domain = new HashSet<String>(dict.getKeys());

            TypeMapBase newDict = dict.clone();

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
                    Set<VMDataType[]> tupleSet = vtvs.getTuples();
                    String[] variableStrings = vtvs.getVarNames();
                    int length = variableStrings.length;
                    Set<Map<String, AstType>> newDictSet = new HashSet<>();
                    if(tupleSet.isEmpty()){
                        Map<String, AstType> tempMap = new HashMap<>();
                        for(int i=0; i<length; i++){
                            tempMap.put(variableStrings[i], AstType.BOT);
                        }
                        newDictSet.add(tempMap);
                    }else{
                        for(VMDataType[] vec : tupleSet){
                            Map<String, AstType> tempMap = new HashMap<>();
                            for(int i=0; i<length; i++){
                                tempMap.put(variableStrings[i], AstType.get(vec[i]));
                            }
                            newDictSet.add(tempMap);
                        }
                    }
                    newDict.add(newDictSet);
                }
            }
            /* add diaptched variables information */
            Set<String> rematchVarSet = node.getRematchVarSet();
            for(String s : rematchVarSet){
                newDict.addDispatch(s);
            }
            SyntaxTree body = (SyntaxTree)definition.get(Symbol.unique("body"));
            dict = visit((SyntaxTree)body, newDict);

            save(nameNode, dict);
            save(nodeName, dict);
            save(paramsNode, dict);
            return dict.select(domain);
        }
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }

    public class Parameters extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            for (SyntaxTree chunk : node) {
                // TODO: update dict
                visit(chunk, dict);
                save(chunk, dict);
            }
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Set<String> domain = new HashSet<String>(dict.getKeys());
            for (SyntaxTree seq : node) {
                dict = visit(seq, dict);
                save(seq, dict);
            }
            TypeMapBase result = dict.select(domain);
            return result;
        }
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }

    public class Match extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            MatchProcessor mp = new MatchProcessor(node);
            SyntaxTree labelNode= node.get(Symbol.unique("label"), null);
            String label = labelNode == null ? null : labelNode.toText();

            TypeMapBase outDict = dict.getBottomDict();

            TypeMapBase entryDict;
            TypeMapBase newEntryDict = dict;
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
                    TypeMapBase dictCaseIn = entryDict.enterCase(mp.getFormalParams(), mp.getVMDataTypeVecSet(i));
                    if (dictCaseIn.hasBottom())
                        continue;
                    SyntaxTree body = mp.getBodyAst(i);
                    TypeMapBase dictCaseOut = visit(body, dictCaseIn);
                    outDict = outDict.combine(dictCaseOut);
                }
                newEntryDict = matchStack.pop();
            } while (!entryDict.equals(newEntryDict));
            node.setTypeMap(entryDict);

            SyntaxTree paramsNode = node.get(Symbol.unique("params"));
            save(paramsNode, outDict);
            outDict.clearDispatch();
            return outDict;
        }
    }

    public class Rematch extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            String label = node.get(Symbol.unique("label")).toText();
            TypeMapBase matchDict = matchStack.getDict(label);
            if (matchDict == null)
                throw new Error("match label not found: "+label);
            Set<String> domain = matchDict.getKeys();
            String[] matchParams = matchStack.getParams(label);

            String[] rematchArgs = new String[matchParams.length];
            for (int i = 1; i < node.size(); i++) {
                rematchArgs[i-1] = node.get(i).toText();
            }

            TypeMapBase matchDict2 = dict.rematch(matchParams, rematchArgs, domain);
            TypeMapBase result = matchDict2.combine(matchDict);
            matchStack.updateDict(label, result);

            return dict.getBottomDict();
        }
    }

    public class Return extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            save(node.get(0), dict);
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }

    public class Assignment extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree right = node.get(Symbol.unique("right"));
            Map<Map<String, AstType>, AstType> rhsTypeMap = visit(right, dict).getExprTypeMap();
            SyntaxTree left = node.get(Symbol.unique("left"));
            dict.assign(left.toText(), rhsTypeMap);
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }

    public class AssignmentPair extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree right = node.get(Symbol.unique("right"));
            Map<Map<String, AstType>, AstType> rhsTypeMap = visit(right, dict).getExprTypeMap();
            for(Map<String, AstType> cond : rhsTypeMap.keySet()){
                AstType t = rhsTypeMap.get(cond);
                if (!(t instanceof AstPairType)) {
                    throw new Error("AssignmentPair: type error");
                }
                Set<Map<String, AstType>> dictSet = dict.getDictSet();
                for(Map<String, AstType> m : dictSet){
                    if(!TypeMapBase.contains(m, cond)) continue;
                    ArrayList<AstType> types = ((AstPairType)t).getTypes();
                    SyntaxTree left = node.get(Symbol.unique("left"));
                    if (types.size() != left.size()) {
                        throw new Error("AssignmentPair: return type error");
                    }
                    for (int i = 0; i < types.size(); i++) {
                        m.replace(left.get(i).toText(), types.get(i));
                    }
                }
            }
            return dict;
        }
        @Override
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }

    public class Declaration extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            // SyntaxTree type = node.get(Symbol.unique("type"));
            // AstBaseType varType = new AstBaseType(type.toText());

            SyntaxTree var = node.get(Symbol.unique("var"));
            SyntaxTree expr = node.get(Symbol.unique("expr"));
            Map<Map<String, AstType>, AstType> rhsTypeMap = visit(expr, dict).getExprTypeMap();
            dict.add(var.toText(), rhsTypeMap);
            save(expr, dict);
            save(var, dict);
            return dict;
        }
        @Override
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }
    public class If extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase copyDict = dict.clone();
            SyntaxTree thenNode = node.get(Symbol.unique("then"));

            TypeMapBase thenDict = visit(thenNode, dict);
            SyntaxTree elseNode = node.get(Symbol.unique("else"));
            TypeMapBase resultDict;
            if (elseNode == null) {
                resultDict = thenDict;
            } else {
                TypeMapBase elseDict = visit(elseNode, copyDict);
                resultDict = thenDict.combine(elseDict);
            }
            SyntaxTree condNode = node.get(Symbol.unique("cond"));
            save(condNode, resultDict);

            return resultDict;
        }
        @Override
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
        }
    }

    public class Do extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree initNode = node.get(Symbol.unique("init"));
            SyntaxTree varNode = initNode.get(Symbol.unique("var"));
            SyntaxTree exprNode = initNode.get(Symbol.unique("expr"));
            Map<Map<String, AstType>, AstType> rhsTypeMap = visit(exprNode, dict).getExprTypeMap();
            dict.add(varNode.toText(), rhsTypeMap);

            TypeMapBase savedDict;
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
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            AstType type = AstType.get(typeNode.toText());
            dict.addGlobal(varNode.toText(), type);

            return dict;
        }
    }

    public class CFunction extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            AstType domain = AstType.nodeToType(typeNode.get(0));
            AstType range = AstType.nodeToType(typeNode.get(1));
            SyntaxTree nameNode = node.get(Symbol.unique("name"));
            AstType type = new AstProductType(domain, range);
            dict.addGlobal(nameNode.toText(), type);

            return dict;
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            AstType type = AstType.get(typeNode.toText());
            dict.addGlobal(varNode.toText(), type);

            return dict;
        }
    }

    public class Trinary extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree thenNode = node.get(Symbol.unique("then"));
            Map<Map<String, AstType>, AstType> thenExprTypeMap = visit(thenNode, dict).getExprTypeMap();
            SyntaxTree elseNode = node.get(Symbol.unique("else"));
            Map<Map<String, AstType>, AstType> elseExprTypeMap = visit(elseNode, dict).getExprTypeMap();

            TypeMapBase resultMap = TYPE_MAP.clone();
            resultMap.setExprTypeMap(resultMap.combineExprTypeMap(thenExprTypeMap, elseExprTypeMap));
            return resultMap;
        }
    }
    public class Or extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
            tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
            return tempMap;
        }
    }
    public class And extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }
    public class BitwiseOr extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = bitwiseOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }

    public class BitwiseXOr extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = bitwiseOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }

    public class BitwiseAnd extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = bitwiseOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }

    public class Equals extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }

    public class NotEquals extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }

    public class LessThanEquals extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
            tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }
    public class GreaterThanEquals extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }
    public class LessThan extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }
    public class GreaterThan extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }
    public class LeftShift extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree leftNode = node.get(Symbol.unique("left"));
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(visit(leftNode, dict).getExprTypeMap());
			return tempMap;
        }
    }
    public class RightShift extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree leftNode = node.get(Symbol.unique("left"));
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(visit(leftNode, dict).getExprTypeMap());
			return tempMap;
        }
    }

    Map<String, Integer> operatorNameToIndex = new HashMap<>();
    private void numberOperatorInitialize(){
        operatorNameToIndex.put("Add", 0);
        operatorNameToIndex.put("Sub", 1);
        operatorNameToIndex.put("Mul", 2);
        operatorNameToIndex.put("Div", 3);
        operatorNameToIndex.put("Mod", 4);
    }

    private Set<Map<String,AstType>> getKeySet(Set<Map<String,AstType>> lKeySet, Set<Map<String,AstType>> rKeySet){
        Set<Map<String,AstType>> newKeySet = new HashSet<>();
        for(Map<String,AstType> m1 : lKeySet){
            for(Map<String,AstType> m2 : rKeySet){
                Map<String,AstType> tempMap = new HashMap<>(m1);
                for(String name : m2.keySet()){
                    if(!m1.containsKey(name)){
                        tempMap.put(name, m2.get(name));
                        continue;
                    }
                    if(m1.get(name)!=m2.get(name)){
                        tempMap = null;
                        break;
                    }
                }
                if(tempMap!=null){
                    newKeySet.add(tempMap);
                }
            }
        }
        return newKeySet;
    }

    AstType tCint = AstType.get("cint");
    AstType tCdouble = AstType.get("cdouble");
    AstType tCstring = AstType.get("cstring");
    AstType tDisplacement = AstType.get("Displacement");
    AstType tSubscript = AstType.get("Subscript");
    private Map<Map<String,AstType>,AstType> numberOperator(SyntaxTree node, TypeMapBase dict) throws Exception {
        final int CINT         = 0;
        final int CDOUBLE      = 1;
        final int DISPLACEMENT = 2;
        final int SUBSCRIPT    = 3;
        final AstType typeArray[] = {tCint, tCdouble, tDisplacement, tSubscript};
        /*  rule ---> {<LeftOperandType>, <RightOperandType>, <ResultType>}, ... */
        final int typeRule[][][] = {
            //Add rule
            {{CINT, CINT, CINT}, {CINT, CDOUBLE, CDOUBLE}, {CDOUBLE, CINT, CDOUBLE}, {CDOUBLE, CDOUBLE, CDOUBLE},
             {CINT, SUBSCRIPT, SUBSCRIPT}, {SUBSCRIPT, CINT, SUBSCRIPT}, {CINT, DISPLACEMENT, SUBSCRIPT}, {DISPLACEMENT, CINT, SUBSCRIPT}},
            //Sub rule
            {{CINT, CINT, CINT}, {CINT, CDOUBLE, CDOUBLE}, {CDOUBLE, CINT, CDOUBLE}, {CDOUBLE, CDOUBLE, CDOUBLE},
             {CINT, SUBSCRIPT, SUBSCRIPT}, {SUBSCRIPT, CINT, SUBSCRIPT}, {CINT, DISPLACEMENT, SUBSCRIPT}, {DISPLACEMENT, CINT, SUBSCRIPT},
             {SUBSCRIPT, SUBSCRIPT, SUBSCRIPT}},
            //Mul rule
            {{CINT, CINT, CINT}, {CINT, CDOUBLE, CDOUBLE}, {CDOUBLE, CINT, CDOUBLE}, {CDOUBLE, CDOUBLE, CDOUBLE}},
            //Div rule
            {{CINT, CINT, CINT}, {CINT, CDOUBLE, CDOUBLE}, {CDOUBLE, CINT, CDOUBLE}, {CDOUBLE, CDOUBLE, CDOUBLE}},
            //Mod rule
            {{CINT, CINT, CINT}, {CINT, CDOUBLE, CDOUBLE}, {CDOUBLE, CINT, CDOUBLE}, {CDOUBLE, CDOUBLE, CDOUBLE}},
        };
        SyntaxTree leftNode = node.get(Symbol.unique("left"));
        SyntaxTree rightNode = node.get(Symbol.unique("right"));
        TypeMapBase lTypeMap = visit(leftNode, dict);
        TypeMapBase rTypeMap = visit(rightNode, dict);
        List<Set<Map<String,AstType>>> lKeySetSetList = new ArrayList<>();
        List<Set<Map<String,AstType>>> rKeySetSetList = new ArrayList<>();
        for(int i=0; i<typeArray.length; i++){
            lKeySetSetList.add(lTypeMap.getKeySetValueOf(typeArray[i]));
            rKeySetSetList.add(rTypeMap.getKeySetValueOf(typeArray[i]));
        }
        Map<Map<String,AstType>,AstType> newExprTypeMap = new HashMap<>();
        Integer index = operatorNameToIndex.get(node.getTag().toString());
        if(index==null){
            throw new Error("Unknown operator name : "+node.getTag().toString());
        }
        for(int[] rule : typeRule[index]){
            Set<Map<String,AstType>> lOperandExprTypeMapSet = lKeySetSetList.get(rule[0]);
            Set<Map<String,AstType>> rOperandExprTypeMapSet = rKeySetSetList.get(rule[1]);
            AstType resultType   = typeArray[rule[2]];
            Set<Map<String,AstType>> keySet = getKeySet(lOperandExprTypeMapSet, rOperandExprTypeMapSet);
            for(Map<String, AstType> key : keySet){
                newExprTypeMap.put(key, resultType);
            }
        }
        if(newExprTypeMap.isEmpty()){
            throw new Error("type error: number Operate has no result type");
        }
        return newExprTypeMap;
    }

    private Map<Map<String,AstType>,AstType> bitwiseOperator(SyntaxTree node, TypeMapBase dict) throws Exception {
        SyntaxTree leftNode = node.get(Symbol.unique("left"));
        Map<Map<String,AstType>,AstType> lExprTypeMap = visit(leftNode, dict).getExprTypeMap();
        SyntaxTree rightNode = node.get(Symbol.unique("right"));
        Map<Map<String,AstType>,AstType> rExprTypeMap = visit(rightNode, dict).getExprTypeMap();

        for(Map<String,AstType> m : lExprTypeMap.keySet()){
            if(lExprTypeMap.get(m)!=tCint){
                throw new Error("type error: exprType has not Cint pattern");
            }
        }
        for(Map<String,AstType> m : rExprTypeMap.keySet()){
            if(rExprTypeMap.get(m)!=tCint){
                throw new Error("type error: exprType has not Cint pattern");
            }
        }
        return TypeMapBase.getSimpleExprTypeMap(tCint);
    }

    public class Add extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = numberOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }
    public class Sub extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = numberOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }
    public class Mul extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = numberOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }
    public class Div extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = numberOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }
    public class Mod extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            Map<Map<String,AstType>,AstType> exprTypeMap = numberOperator(node, dict);
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(exprTypeMap);
			return tempMap;
        }
    }

    public class Plus extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(TypeMapBase.cloneExprTypeMap(visit(exprNode, dict).getExprTypeMap()));
			return tempMap;
        }
    }
    public class Minus extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(TypeMapBase.cloneExprTypeMap(visit(exprNode, dict).getExprTypeMap()));
			return tempMap;
        }
    }
    public class Compl extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(TypeMapBase.cloneExprTypeMap(visit(exprNode, dict).getExprTypeMap()));
			return tempMap;
        }
    }
    public class Not extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("Bool"));
			return tempMap;
        }
    }
    public class FunctionCall extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            SyntaxTree recv = node.get(Symbol.unique("recv"));
            String funName = recv.toText();
            
            if (!dict.containsKey(funName)) {
                throw new Error("FunctionCall: no such name: "+funName+" :"+node.getSource().getResourceName()+" :"+node.getLineNum());
            }
            Set<AstType> funTypeSet = dict.get(funName);
            if(funTypeSet.size() != 1){
                throw new Error("FunctionCall: function \""+funName+"\" is defined multiple types");
            }
            AstProductType funType = (AstProductType)funTypeSet.toArray()[0];
            AstBaseType rangeType = (AstBaseType)funType.getRange();
           
            // TODO: type check
            /*
            for (SyntaxTree arg : node.get(1)) {
                Map<Map<String,AstType>,AstType> exprTypeMap = visit(arg, dict).getExprTypeMap();
            }
            */
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.setExprTypeMap(TypeMapBase.getSimpleExprTypeMap(rangeType));
			return tempMap;
        }
    }

    public class ArrayIndex extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            // TODO
            return dict;
        }
    }
    public class Field extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            // TODO
            return dict;
        }
    }
    public class _Integer extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cint"));
			return tempMap;
        }
    }

    public class _Float extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("cdouble"));
			return tempMap;
        }
    }

    public class _String extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("String"));
			return tempMap;
        }
    }

    public class _True extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
			tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("Bool"));
			return tempMap;
        }
    }
    public class _False extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            TypeMapBase tempMap = TYPE_MAP.clone();
            tempMap.putExprTypeElement(new HashMap<String, AstType>(), AstType.get("Bool"));
			return tempMap;
        }
    }

    public class Name extends DefaultVisitor {
        @Override
        public TypeMapBase accept(SyntaxTree node, TypeMapBase dict) throws Exception {
            String name = node.toText();
            if (!dict.containsKey(name)) {
                throw new Error("Name: no such name: "+"\""+name+"\""+node.getSource().getResourceName()+": "+node.getLineNum());
            }
            Set<AstType> type = dict.get(name);
            TypeMapBase tempMap = TYPE_MAP.clone();
            for(AstType t : type){
                Map<String, AstType> keyMap = new HashMap<>();
                keyMap.put(name, t);
                tempMap.putExprTypeElement(keyMap, t);
            }
			return tempMap;
        }
        public void saveType(SyntaxTree node, TypeMapBase dict) throws Exception {
            String name = node.toText();
            if (dict.containsKey(name)) {
                Set<AstType> type = dict.get(name);
                Map<Map<String, AstType>, AstType> tempExprTypeMap = new HashMap<>();
                for(AstType t : type){
                    Map<String, AstType> keyMap = new HashMap<>();
                    keyMap.put(name, t);
                    tempExprTypeMap.put(keyMap, t);
                }
                node.setExprTypeMap(tempExprTypeMap);
            }
        }
    }
}
