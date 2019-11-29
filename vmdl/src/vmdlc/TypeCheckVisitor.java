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

import java.util.Set;
import java.util.Map;
import java.util.Stack;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import vmdlc.TypeCheckVisitor.DefaultVisitor;
import type.AstType.*;
import type.AstType;
import type.ExprTypeSet;
import type.ExprTypeSetLub;
import type.ExprTypeSetDetail;
import type.OperatorTypeChecker;
import type.TypeMap;
import type.TypeMapSet;
import type.TypeMapSetLub;
import type.TypeMapSetHalf;
import type.TypeMapSetFull;
import type.VMDataType;
import type.VMDataTypeVecSet;

public class TypeCheckVisitor extends TreeVisitorMap<DefaultVisitor> {
    static class MatchStack {
        static class MatchRecord {
            String name;
            String[] formalParams;
            TypeMapSet dict;

            MatchRecord(String name, String[] formalParams, TypeMapSet dict) {
                this.name = name;
                this.formalParams = formalParams;
                this.dict = dict;
            }

            public void setDict(TypeMapSet _dict) {
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

        public void enter(String name, String[] formalParams, TypeMapSet dict) {
            MatchRecord mr = new MatchRecord(name, formalParams, dict);
            stack.push(mr);
        }

        public String[] getParams(String name) {
            MatchRecord mr = lookup(name);
            if (mr == null)
                return null;
            return mr.formalParams;
        }

        public TypeMapSet getDict(String name) {
            MatchRecord mr = lookup(name);
            if (mr == null)
                return null;
            return mr.dict;
        }

        public TypeMapSet pop() {
            MatchRecord mr = stack.pop();
            return mr.dict;
        }

        public boolean isEmpty() {
            return stack.isEmpty();
        }

        public void updateDict(String name, TypeMapSet dict) {
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

    public static enum CheckTypePlicy{
        Lub(new TypeMapSetLub(), new ExprTypeSetLub()),
        Half(new TypeMapSetHalf(), new ExprTypeSetDetail()),
        Full(new TypeMapSetFull(), new ExprTypeSetDetail());

        private TypeMapSet typeMap;
        private ExprTypeSet exprTypeSet;
        private CheckTypePlicy(TypeMapSet typeMap, ExprTypeSet exprTypeSet){
            this.typeMap = typeMap;
            this.exprTypeSet = exprTypeSet;
        }
        public TypeMapSet getTypeMap(){
            return typeMap;
        }
        public ExprTypeSet getExprTypeSet(){
            return exprTypeSet;
        }
    };
    public static TypeMapSet TYPE_MAP;
    public static ExprTypeSet EXPR_TYPE;

    public TypeCheckVisitor() {
        init(TypeCheckVisitor.class, new DefaultVisitor());
    }

    public void start(Tree<?> node, OperandSpecifications opSpec, CheckTypePlicy policy) {
        this.opSpec = opSpec;
        try {
            TYPE_MAP  = policy.getTypeMap();
            EXPR_TYPE = policy.getExprTypeSet();
            TypeMapSet dict = TYPE_MAP.clone();
            matchStack = new MatchStack();
            for (Tree<?> chunk : node) {
                dict = visit((SyntaxTree) chunk, dict);
            }
            if (!matchStack.isEmpty())
                throw new Error("match stack is not empty after typing process");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Visit for statement
    private final TypeMapSet visit(SyntaxTree node, TypeMapSet dict) throws Exception {
        /*
        System.err.println("==================");
        System.err.println(node.getTag().toString());
        System.err.println(node.toString());
        System.err.println("----");
        System.err.println("dict:"+dict.toString());
        System.err.println("----");
        */
        return find(node.getTag().toString()).accept(node, dict);
    }

    // Visit for expression
    private final ExprTypeSet visit(SyntaxTree node, TypeMap dict) throws Exception {
        /*
        System.err.println("==================");
        System.err.println(node.getTag().toString());
        System.err.println(node.toString());
        System.err.println("----");
        System.err.println("dict:"+dict.toString());
        System.err.println("----");
        */
        return find(node.getTag().toString()).accept(node, dict);
    }

    private final void save(SyntaxTree node, TypeMapSet dict) throws Exception {
        find(node.getTag().toString()).saveType(node, dict);
    }

    private final void saveFunction(SyntaxTree node, AstProductType type) throws Exception{
        find(node.getTag().toString()).saveFunctionType(node, type);
    }

    public class DefaultVisitor {
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            return dict;
        }

        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            return null;
        }

        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
            for (SyntaxTree chunk : node) {
                save(chunk, dict);
            }
        }

        public void saveFunctionType(SyntaxTree node, AstProductType type) throws Exception {
            for (SyntaxTree chunk : node) {
                saveFunction(chunk, type);
            }
        }
    }

    //**********************************
    // Statements
    //**********************************

    public class FunctionMeta extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree type = node.get(Symbol.unique("type"));
            AstProductType funtype = (AstProductType) AstType.nodeToType((SyntaxTree) type);

            SyntaxTree definition = node.get(Symbol.unique("definition"));

            SyntaxTree nodeName = node.get(Symbol.unique("name"));
            SyntaxTree nameNode = definition.get(Symbol.unique("name"));
            String name = nameNode.toText();
            boolean isAdded = TypeMap.addGlobal(name, funtype);
            if(!isAdded){
                ErrorPrinter.error("Double define: "+name, node);
            }

            Set<String> domain = new HashSet<String>(dict.getKeys());

            Set<TypeMap> newSet = dict.clone().getTypeMapSet();
            Set<TypeMap> newSet2 = new HashSet<>();
            /* add non-JSValue parameters */
            SyntaxTree paramsNode = definition.get(Symbol.unique("params"));
            if (paramsNode != null && paramsNode.size() != 0) {
                String[] paramNames = new String[paramsNode.size()];
                String[] jsvParamNames = new String[paramsNode.size()];
                AstType paramTypes = funtype.getDomain();
                int nJsvTypes = 0;
                
                Map<String, AstType> nJsvMap = new HashMap<>();
                if (paramTypes instanceof AstBaseType) {
                    AstType paramType = paramTypes;
                    String paramName = paramsNode.get(0).toText();
                    paramNames[0] = paramName;
                    if (paramType instanceof JSValueType)
                        jsvParamNames[nJsvTypes++] = paramName;
                    else
                        nJsvMap.put(paramName, paramType);
                } else if (paramTypes instanceof AstPairType) {
                    List<AstType> paramTypeList = ((AstPairType) paramTypes).getTypes();
                    for (int i = 0; i < paramTypeList.size(); i++) {
                        AstType paramType = paramTypeList.get(i);
                        String paramName = paramsNode.get(i).toText();
                        paramNames[i] = paramName;
                        if (paramType instanceof JSValueType)
                            jsvParamNames[nJsvTypes++] = paramName;
                        else
                            nJsvMap.put(paramName, paramType);
                    }
                }
                for(TypeMap map : dict){
                    for(String v : nJsvMap.keySet()){
                        AstType t = nJsvMap.get(v);
                        newSet.addAll(dict.getAddedSet(map, v, t));
                    }
                }
                if(newSet.isEmpty()){
                    newSet.add(new TypeMap());
                }
                /* add JSValue parameters (apply operand spec) */
                if (nJsvTypes > 0) {
                    String[] jsvParamNamesPacked = new String[nJsvTypes];
                    System.arraycopy(jsvParamNames, 0, jsvParamNamesPacked, 0, nJsvTypes);
                    VMDataTypeVecSet vtvs = opSpec.getAccept(name, jsvParamNamesPacked);
                    Set<VMDataType[]> tupleSet = vtvs.getTuples();
                    String[] variableStrings = vtvs.getVarNames();
                    int length = variableStrings.length;
                    Set<Map<String, AstType>> jsvMapSet = new HashSet<>();
                    if (tupleSet.isEmpty()) {
                        Map<String, AstType> tempMap = new HashMap<>();
                        for (int i = 0; i < length; i++) {
                            tempMap.put(variableStrings[i], AstType.BOT);
                        }
                        jsvMapSet.add(tempMap);
                    } else {
                        for (VMDataType[] vec : tupleSet) {
                            Map<String, AstType> tempMap = new HashMap<>();
                            for (int i = 0; i < length; i++) {
                                tempMap.put(variableStrings[i], AstType.get(vec[i]));
                            }
                            jsvMapSet.add(tempMap);
                        }
                    }
                    for(TypeMap map : newSet){
                        for(Map<String, AstType> jsvMap : jsvMapSet){
                            newSet2.addAll(dict.getAddedSet(map, jsvMap));
                        }
                    }
                }
            }else{
                newSet2 = Collections.emptySet();
            }
            TypeMapSet newDict = TYPE_MAP.clone();
            newDict.setTypeMapSet(newSet2);
            newDict.setDispatchSet(node.getRematchVarSet());
            SyntaxTree body = (SyntaxTree) definition.get(Symbol.unique("body"));
            saveFunction(body, funtype);
            dict = visit(body, newDict);
            save(nameNode, dict);
            save(nodeName, dict);
            save(paramsNode, dict);
            return dict.select(domain);
        }

        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }
    }

    public class Parameters extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            for (SyntaxTree chunk : node) {
                // TODO: update dict
                visit(chunk, dict);
                save(chunk, dict);
            }
            return dict;
        }

        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }
    }

    public class Block extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            Set<String> domain = new HashSet<String>(dict.getKeys());
            for (SyntaxTree seq : node) {
                dict = visit(seq, dict);
                save(seq, dict);
            }
            TypeMapSet result = dict.select(domain);
            return result;
        }

        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }

    }

    public class Match extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            MatchProcessor mp = new MatchProcessor(node);
            SyntaxTree labelNode = node.get(Symbol.unique("label"), null);
            String label = labelNode == null ? null : labelNode.toText();
            TypeMapSet outDict = dict.getBottomDict();
            TypeMapSet entryDict;
            TypeMapSet newEntryDict = dict;
            do {
                entryDict = newEntryDict;
                matchStack.enter(label, mp.getFormalParams(), entryDict);
                for (int i = 0; i < mp.size(); i++) {
                    TypeMapSet dictCaseIn = entryDict.enterCase(mp.getFormalParams(), mp.getVMDataTypeVecSet(i));
                    if (dictCaseIn.noInformationAbout(mp.getFormalParams())){
                        continue;
                    }
                    SyntaxTree body = mp.getBodyAst(i);
                    TypeMapSet dictCaseOut = visit(body, dictCaseIn);
                    outDict = outDict.combine(dictCaseOut);
                }
                newEntryDict = matchStack.pop();
            } while (!entryDict.equals(newEntryDict));
            node.setTypeMapSet(entryDict);
            SyntaxTree paramsNode = node.get(Symbol.unique("params"));
            save(paramsNode, outDict);
            return outDict;
        }
    }

    public class Rematch extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree labelNode = node.get(Symbol.unique("label"));
            String label = labelNode.toText();
            TypeMapSet matchDict = matchStack.getDict(label);
            if (matchDict == null){
                ErrorPrinter.error("Labeled Match not found: "+label, labelNode);
            }
            Set<String> domain = matchDict.getKeys();
            String[] matchParams = matchStack.getParams(label);

            String[] rematchArgs = new String[matchParams.length];
            for (int i = 1; i < node.size(); i++) {
                rematchArgs[i - 1] = node.get(i).toText();
            }

            TypeMapSet rematchDict = dict.rematch(matchParams, rematchArgs, domain);
            TypeMapSet newMatchDict = rematchDict.combine(matchDict);
            matchStack.updateDict(label, newMatchDict);

            return dict.getBottomDict();
        }
    }

    public class Return extends DefaultVisitor {
        AstProductType functionType;
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            if(functionType == null){
                throw new Error("Connnot solve functionType: null");
            }
            AstType rangeType = functionType.getRange();
            if(rangeType == null){
                throw new Error("function range typs null");
            }
            if(rangeType != AstType.get("void")){
                SyntaxTree valNode = node.get(0);
                for(TypeMap map : dict){
                    ExprTypeSet exprTypeSet = visit(valNode, map);
                    for(AstType t : exprTypeSet){
                        if(!(rangeType.isSuperOrEqual(t))){
                            ErrorPrinter.error("Return type "+t+", function types "+functionType.toString(), valNode);
                        }
                    }
                }
            }
            save(node.get(0), dict);
            return dict;
        }

        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }

        public void saveFunctionType(SyntaxTree node, AstProductType type) throws Exception {
            functionType = type;
        }
    }

    final static AstType tCint = AstType.get("cint");
    final static AstType tCdobule = AstType.get("cdouble");
    final static AstType tSubscript = AstType.get("Subscript");
    final static AstType tDisplacement = AstType.get("Displacement");
    final static Set<AstType> cNumbers = new HashSet<>();
    final static Set<AstType> cInts = new HashSet<>();
    static{
        cNumbers.add(tCint);
        cNumbers.add(tCdobule);
        cNumbers.add(tSubscript);
        cNumbers.add(tDisplacement);
        cInts.add(tCint);
        cInts.add(tCdobule);
        cInts.add(tSubscript);
        cInts.add(tDisplacement);

    }
    private AstType getAssignType(AstType type, AstType assign){
        if(type.isSuperOrEqual(assign)) return assign;
        if(type.equals(tCdobule) && cNumbers.contains(assign)) return tCdobule;
        if(cInts.contains(type) && cInts.contains(assign)) return tCint;
        return null;
    }

    public class Assignment extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree leftNode = node.get(Symbol.unique("left"));
            SyntaxTree rightNode = node.get(Symbol.unique("right"));
            String leftName = leftNode.toText();
            Set<TypeMap> newSet = new HashSet<>();
            for(TypeMap typeMap : dict){
                AstType leftType = typeMap.get(leftName);
                if(leftType instanceof JSValueType){
                    ErrorPrinter.error("JSValue variable cannot assign", leftNode);
                }
                ExprTypeSet exprTypeSet = visit(rightNode, typeMap);
                for(AstType type : exprTypeSet){
                    AstType assginedType = getAssignType(leftType, type);
                    if(assginedType == null){
                        ErrorPrinter.error("Expression types "+type+", need types "+leftType, rightNode);
                    }
                    TypeMap temp = typeMap.clone();
                    Set<TypeMap> assignedSet = dict.getAssignedSet(temp, leftName, assginedType);
                    newSet.addAll(assignedSet);
                }
            }
            TypeMapSet newTypeMapSet = TYPE_MAP.clone();
            newTypeMapSet.setTypeMapSet(newSet);
            return newTypeMapSet;
        }

        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }
    }

    public class Declaration extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            AstType varType = AstType.nodeToType(typeNode);
            if(varType==null){
                ErrorPrinter.error("Type not found: "+typeNode.toText(), typeNode);
            }
            String varName = varNode.toText();
            Set<TypeMap> newSet = new HashSet<>();
            for(TypeMap typeMap : dict){
                ExprTypeSet exprTypeSet = visit(exprNode, typeMap);
                for(AstType type : exprTypeSet){
                    AstType assginedType = getAssignType(varType, type);
                    if(assginedType == null){
                        ErrorPrinter.error("Expression types "+type+", need types "+varType, exprNode);
                    }
                    TypeMap temp = typeMap.clone();
                    Set<TypeMap> addedSet = dict.getAddedSet(temp, varName, assginedType);
                    newSet.addAll(addedSet);
                }
            }
            TypeMapSet newTypeMapSet = TYPE_MAP.clone();
            newTypeMapSet.setTypeMapSet(newSet);
            save(exprNode, dict);
            save(varNode, dict);
            return newTypeMapSet;
        }
        @Override
        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }
    }

    public class If extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            TypeMapSet copyDict = dict.clone();
            SyntaxTree thenNode = node.get(Symbol.unique("then"));
            TypeMapSet thenDict = visit(thenNode, dict);
            SyntaxTree elseNode = node.get(Symbol.unique("else"));
            TypeMapSet resultDict;
            if (elseNode == null) {
                resultDict = thenDict;
            } else {
                TypeMapSet elseDict = visit(elseNode, copyDict);
                if(!thenDict.getKeys().equals(elseDict.getKeys())){
                    ErrorPrinter.error("Both environment keys must be equal", node);
                }
                resultDict = thenDict.combine(elseDict);
            }
            SyntaxTree condNode = node.get(Symbol.unique("cond"));
            save(condNode, resultDict);

            return resultDict;
        }

        @Override
        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }
    }

    public class Do extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree initNode = node.get(Symbol.unique("init"));
            SyntaxTree limitNode = node.get(Symbol.unique("limit"));
            SyntaxTree stepNode = node.get(Symbol.unique("step"));
            SyntaxTree typeNode = initNode.get(Symbol.unique("type"));
            SyntaxTree varNode = initNode.get(Symbol.unique("var"));
            SyntaxTree exprNode = initNode.get(Symbol.unique("expr"));
            AstType varType = AstType.nodeToType(typeNode);
            String varName = varNode.toText();
            Set<TypeMap> doSet = new HashSet<>();
            for(TypeMap typeMap : dict){
                ExprTypeSet exprTypeSet = visit(exprNode, typeMap);
                ExprTypeSet limitExprTypeSet = visit(limitNode.get(0), typeMap);
                ExprTypeSet stepExprTypeSet = visit(stepNode.get(0), typeMap);
                for(AstType t : exprTypeSet){
                    if(!(varType.isSuperOrEqual(t))){
                        ErrorPrinter.error("Expression types "+t+", need types "+varType, exprNode);
                    }
                    doSet.addAll(dict.getAddedSet(typeMap, varName, t));
                }
                for(AstType t : limitExprTypeSet){
                    if(!(varType.isSuperOrEqual(t))){
                        ErrorPrinter.error("Expression types "+t+", need types "+varType, limitNode.get(0));
                    }
                }
                for(AstType t : stepExprTypeSet){
                    if(!(varType.isSuperOrEqual(t))){
                        ErrorPrinter.error("Expression types "+t+", need types "+varType, stepNode.get(0));
                    }
                }
            }
            TypeMapSet doDict = TYPE_MAP.clone();
            doDict.setTypeMapSet(doSet);
            TypeMapSet savedDict;
            do {
                savedDict = doDict.clone();
                SyntaxTree blockNode = node.get(Symbol.unique("block"));
                doDict = visit(blockNode, doDict);
            } while (!doDict.equals(savedDict));
            return dict;
        }
    }

    //**********************************
    // ExternCs
    //**********************************

    public class CTypeDef extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            SyntaxTree valueNode = node.get(Symbol.unique("value"));
            AstType type = AstType.nodeToType(typeNode);
            String typeName = varNode.toText();
            String cValue = valueNode.toText().replace("\\\"", "\"").replace("\\\\", "\\");
            if(!(type instanceof AstBaseType)){
                ErrorPrinter.error("Extern type must be basic type: "+type.toString(), typeNode);
            }
            AstType.addAlias(typeName, (AstBaseType)type);
            AstToCVisitor.addCType(typeName, cValue);
            return dict;
        }
    }

    public class CObjectmapping extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree mappingTypeNode = node.get(Symbol.unique("type"));
            SyntaxTree membersNode = node.get(Symbol.unique("members"));
            String mappingTypeName = mappingTypeNode.toText();
            AstMappingType mappingType = AstType.defineMappingType(mappingTypeName);
            for(SyntaxTree memberNode : membersNode){
                SyntaxTree typeNode = memberNode.get(Symbol.unique("type"));
                SyntaxTree varNode = memberNode.get(Symbol.unique("var"));
                Set<String> annotations = Collections.emptySet();
                if(memberNode.has(Symbol.unique("annotations"))){
                    SyntaxTree anotationsNode = memberNode.get(Symbol.unique("annotations"));
                    annotations = new HashSet<>();
                    annotations.add(anotationsNode.toText());
                }
                AstType type = AstType.nodeToType(typeNode);
                String name = varNode.toText();
                mappingType.addField(annotations, name, type);
            }
            return dict;
        }
    }

    public class CFunction extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            AstType domain = AstType.nodeToType(typeNode.get(0));
            AstType range = AstType.nodeToType(typeNode.get(1));
            SyntaxTree nameNode = node.get(Symbol.unique("name"));
            AstType type = new AstProductType(domain, range);
            String name = nameNode.toText();
            boolean isAdded = TypeMap.addGlobal(name, type);
            if(!isAdded){
                ErrorPrinter.error("Double define: "+name, node);
            }
            return dict;
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            SyntaxTree valueNode = node.get(Symbol.unique("value"));
            AstType type = AstType.nodeToType(typeNode);
            String varName = varNode.toText();
            String cValue = valueNode.toText().replace("\\\"", "\"").replace("\\\\", "\\");
            boolean isAdded = TypeMap.addGlobal(varName, type);
            if(!isAdded){
                ErrorPrinter.error("Double define: "+varName, node);
            }
            AstToCVisitor.addCConstant(varName, cValue);
            return dict;
        }
    }

    //**********************************
    // Expressions
    //**********************************

    //**********************************
    // TrinaryOperator
    //**********************************

    public class Trinary extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree condNode = node.get(Symbol.unique("cond"));
			SyntaxTree thenNode = node.get(Symbol.unique("then"));
            SyntaxTree elseNode = node.get(Symbol.unique("else"));
            ExprTypeSet condExprTypeSet = visit(condNode, dict);
            ExprTypeSet thenExprTypeSet = visit(thenNode, dict);
            ExprTypeSet elseExprTypeSet = visit(elseNode, dict);
            for(AstType type : condExprTypeSet){
                if(type != AstType.get("cint")){
                    ErrorPrinter.error("Condition must type cint: "+type.toString(), condNode);
                }
            }
            return thenExprTypeSet.combine(elseExprTypeSet);
        }
    }

    //**********************************
    // BiaryOperators
    //**********************************

    private ExprTypeSet biaryOperator(SyntaxTree node, TypeMap dict, OperatorTypeChecker checker) throws Exception{
        SyntaxTree leftNode  = node.get(Symbol.unique("left"));
        SyntaxTree rightNode = node.get(Symbol.unique("right"));
        ExprTypeSet leftExprTypeSet  = visit(leftNode, dict);
        ExprTypeSet rightExprTypeSet = visit(rightNode, dict);
        ExprTypeSet resultTypeSet = EXPR_TYPE.clone();
        for (AstType lt : leftExprTypeSet){
            for (AstType rt : rightExprTypeSet){
                AstType result = checker.typeOf(lt, rt);
                if(result == null){
                    ErrorPrinter.error("Illigal types given in operator: "
                        +lt.toString()+","+rt.toString(), node);
                }
                resultTypeSet.add(result);
            }
        }
		return resultTypeSet;
    }

    public class Or extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.OR);
        }
    }

    public class And extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.AND);
        }
    }

    public class BitwiseOr extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.BITWISE_OR);
        }
    }

    public class BitwiseXOr extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.BITWISE_XOR);
        }
    }

    public class BitwiseAnd extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.BITWISE_AND);
        }
    }

    public class Equals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.EQUALS);
        }
    }

    public class NotEquals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.NOT_EQUALS);
        }
    }

    public class LessThanEquals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.LESSTHAN_EQUALS);
        }
    }

    public class GreaterThanEquals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.GRATORTHAN_EQUALS);
        }
    }

    public class LessThan extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.LESSTHAN);
        }
    }

    public class GreaterThan extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.GRATORTHAN);
        }
    }

    public class LeftShift extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.LEFT_SHIFT);
        }
    }

    public class RightShift extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.RIGHT_SHIFT);
        }
    }

    public class Add extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.ADD);
        }
    }

    public class Sub extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.SUB);
        }
    }

    public class Mul extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.MUL);
        }
    }

    public class Div extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.DIV);
        }
    }

    public class Mod extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return biaryOperator(node, dict, OperatorTypeChecker.MOD);
        }
    }

    //**********************************
    // UnaryOperators
    //**********************************

    private ExprTypeSet unaryOperator(SyntaxTree node, TypeMap dict, OperatorTypeChecker checker) throws Exception{
        SyntaxTree exprNode = node.get(Symbol.unique("expr"));
            ExprTypeSet exprTypeSet = visit(exprNode, dict);
            ExprTypeSet resultTypeSet = EXPR_TYPE.clone();
            for (AstType t : exprTypeSet) {
                AstType result = checker.typeOf(t);
                if(result == null){
                    ErrorPrinter.error("Illigal types given in operator: "+t.toString(), node);
                }
                resultTypeSet.add(result);
            }
			return resultTypeSet;
    }

    public class Plus extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return unaryOperator(node, dict, OperatorTypeChecker.PLUS);
        }
    }

    public class Minus extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            return unaryOperator(node, dict, OperatorTypeChecker.MINUS);
        }
    }

    public class Compl extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            return unaryOperator(node, dict, OperatorTypeChecker.COMPL);
        }
    }

    public class Not extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            return unaryOperator(node, dict, OperatorTypeChecker.NOT);
        }
    }

    //*********************************
    // FunctionCall
    //*********************************

    public class FunctionCall extends DefaultVisitor {
        private void checkArguments(List<AstType> argTypes, SyntaxTree argsNode, TypeMap dict) throws Exception{
            SyntaxTree[] argNodes = (SyntaxTree[])argsNode.getSubTree();
            int length = argNodes.length;
            for(int i=0; i<length; i++){
                ExprTypeSet argRealTypes = visit(argNodes[i], dict);
                for(AstType t : argRealTypes){
                    if(!argTypes.get(i).isSuperOrEqual(t)){
                        ErrorPrinter.error("Argument types "+t.toString()
                            +", need types "+argTypes.get(i), argNodes[i]);
                    }
                }
            }
        }
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree recvNode = node.get(Symbol.unique("recv"));
            String functionName = recvNode.toText();
            AstType type = dict.get(functionName);
            if(type == null){
                ErrorPrinter.error("Function not found: "+functionName, recvNode);
            }
            if(!(type instanceof AstProductType)){
                ErrorPrinter.error("Non function refered as function: "+functionName, recvNode);
            }
            AstProductType functionType = (AstProductType)type;
            AstType domain = functionType.getDomain();
            List<AstType> argTypes;
            if(domain instanceof AstPairType){
                argTypes = ((AstPairType)domain).getTypes();
            }else{
                argTypes = new ArrayList<>();
                argTypes.add(domain);
            }
            SyntaxTree argsNode = node.get(Symbol.unique("args"));
            if(argsNode.size()==0){
                if(argTypes.size() != 1 || !argTypes.contains(AstType.get("void"))){
                    ErrorPrinter.error("Argument size does not match: "+functionName, argsNode);
                }
            }else{
                if(argsNode.size() != argTypes.size()){
                    ErrorPrinter.error("Argument size does not match: "+functionName, argsNode);
                }
                checkArguments(argTypes, argsNode, dict);
            }
            AstType range  = functionType.getRange();
            ExprTypeSet newSet = EXPR_TYPE.clone();
            newSet.add(range);
            return newSet;
        }
    }

    //*********************************
    // Others
    //*********************************

    public class ArrayIndex extends DefaultVisitor {
        private boolean isIndex(AstType type){
            return (type == AstType.get("cint") || type == AstType.get("Subscript"));
        }
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree recvNode = node.get(Symbol.unique("recv"));
            SyntaxTree indexNode = node.get(Symbol.unique("index"));
            ExprTypeSet recvTypeSet = visit(recvNode, dict);
            ExprTypeSet indexTypeSet = visit(indexNode, dict);
            ExprTypeSet newSet = EXPR_TYPE.clone();
            for(AstType type : indexTypeSet){
                if(!isIndex(type)){
                    ErrorPrinter.error("Array index must be cint or Subscript: "+type.toString(), indexNode);
                }
            }
            for(AstType type : recvTypeSet){
                if(!(type instanceof AstArrayType)){
                    ErrorPrinter.error("Non array refered as Array: "+type.toString(), recvNode);
                }
                newSet.add(((AstArrayType)type).getElementType());
            }
            return newSet;
        }
    }
    public class FieldAccess extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree recvNode = node.get(Symbol.unique("recv"));
            SyntaxTree fieldNode = node.get(Symbol.unique("field"));
            ExprTypeSet recvTypeSet = visit(recvNode, dict);
            String fieldName = fieldNode.toText();
            ExprTypeSet fieldTypeSet = EXPR_TYPE.clone();
            for(AstType type : recvTypeSet){
                if(!(type instanceof AstMappingType)){
                    ErrorPrinter.error("Non mappingobject refered as mappingobject: "+type.toString(), recvNode);
                }
                AstMappingType mappingType = (AstMappingType) type;
                AstType fieldType = mappingType.getFieldType(fieldName);
                if(fieldType == null){
                    ErrorPrinter.error("Field not found: "+fieldName, fieldNode);
                }
                fieldTypeSet.add(fieldType);
            }
            recvNode.setExprTypeSet(recvTypeSet);
            return fieldTypeSet;
        }
    }

    //*********************************
    // Constants
    //*********************************

    public class _Integer extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            ExprTypeSet newSet = EXPR_TYPE.clone();
            newSet.add(AstType.get("cint"));
			return newSet;
        }
    }

    public class _Float extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			ExprTypeSet newSet = EXPR_TYPE.clone();
            newSet.add(AstType.get("cdouble"));
			return newSet;
        }
    }

    public class _String extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			ExprTypeSet newSet = EXPR_TYPE.clone();
            newSet.add(AstType.get("cstring"));
			return newSet;
        }
    }

    public class _True extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			ExprTypeSet newSet = EXPR_TYPE.clone();
            newSet.add(AstType.get("cint"));
			return newSet;
        }
    }
    public class _False extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			ExprTypeSet newSet = EXPR_TYPE.clone();
            newSet.add(AstType.get("cint"));
			return newSet;
        }
    }

    //*********************************
    // Variables
    //*********************************

    public class Name extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            String name = node.toText();
            if (!dict.containsKey(name)) {
                ErrorPrinter.error("Name not found: "+name, node);
            }
            ExprTypeSet newSet = EXPR_TYPE.clone();
            newSet.add(dict.get(name));
			return newSet;
        }
        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
            String name = node.toText();
            if (dict.containsKey(name)) {
                ExprTypeSet newSet = new ExprTypeSetDetail();
                for(TypeMap map : dict){
                    newSet.add(map.get(name));
                }
                node.setExprTypeSet(newSet);
            }
        }
    }
}
