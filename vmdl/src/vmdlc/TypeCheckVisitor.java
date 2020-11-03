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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import vmdlc.TypeCheckVisitor.DefaultVisitor;
import type.AstType.*;
import type.AstType;
import type.AstTypeVec;
import type.ExprTypeSet;
import type.ExprTypeSetLub;
import type.FunctionAnnotation;
import type.FunctionTable;
import type.ExprTypeSetDetail;
import type.OperatorTypeChecker;
import type.TypeMap;
import type.TypeMapSet;
import type.TypeMapSetLub;
import type.TypeMapSetHalf;
import type.TypeMapSetFull;
import type.VMDataType;
import type.VMDataTypeVec;
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

    public static class TypeCheckOption{
        private OperandSpecifications opSpec;
        private OperandSpecifications funcSpec;
        private OperandSpecifications caseSplitSpec;
        private TypeCheckPolicy policy;
        private boolean functionInliningFlag;
        private boolean updateFTDFlag;
        private boolean updateFunctionSpecFlag;
        private boolean caseSplitFlag;

        public TypeCheckOption setOperandSpec(OperandSpecifications opSpec){
            this.opSpec = opSpec;
			return this;
        }
        public TypeCheckOption setFunctionSpec(OperandSpecifications funcSpec){
            this.funcSpec = funcSpec;
            if(funcSpec != null) this.updateFunctionSpecFlag = true;
			return this;
        }
        public TypeCheckOption setCaseSplitSpec(OperandSpecifications caseSplitSpec){
            this.caseSplitSpec = caseSplitSpec;
			return this;
        }
        public TypeCheckOption setTypeCheckPolicy(TypeCheckPolicy policy){
            this.policy = policy;
			return this;
        }
        public TypeCheckOption setFunctionIniningFlag(boolean flag){
            this.functionInliningFlag = flag;
			return this;
        }
        public TypeCheckOption setUpdateFTDFlag(boolean flag){
            this.updateFTDFlag = flag;
			return this;
        }
        public TypeCheckOption setCaseSplitFlag(boolean flag){
            this.caseSplitFlag = flag;
			return this;
        }
        public OperandSpecifications getOperandSpec(){
			return opSpec;
		}
        public OperandSpecifications getFunctionSpec(){
			return funcSpec;
		}
        public OperandSpecifications getCaseSplitSpec(){
			return caseSplitSpec;
		}
        public TypeCheckPolicy getTypeCheckPolicy(){
			return policy;
		}
        public boolean doFunctionInlining(){
			return functionInliningFlag;
		}
        public boolean doUpdateFTD(){
			return updateFTDFlag;
        }
        public boolean doUpdateFunctionSpec(){
			return updateFunctionSpecFlag;
		}
        public boolean doCaseSplit(){
			return caseSplitFlag;
		}
    }

    MatchStack matchStack;
    SplitCaseSelector splitCaseSelector;
    TypeCheckOption option;

    public static enum TypeCheckPolicy{
        Lub(new TypeMapSetLub(), new ExprTypeSetLub()),
        Half(new TypeMapSetHalf(), new ExprTypeSetDetail()),
        Full(new TypeMapSetFull(), new ExprTypeSetDetail());

        private TypeMapSet typeMap;
        private ExprTypeSet exprTypeSet;
        private TypeCheckPolicy(TypeMapSet typeMap, ExprTypeSet exprTypeSet){
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

    public void start(Tree<?> node, TypeCheckOption option) {
        this.option = option;
        try {
            TYPE_MAP  = option.getTypeCheckPolicy().getTypeMap();
            EXPR_TYPE = option.getTypeCheckPolicy().getExprTypeSet();
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

    // Visit method for statement
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

    // Visit method for expression
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
    }

    //**********************************
    // Statements
    //**********************************

    public class FunctionDefiningInformation{
        private String name;
        private List<String> paramNames;
        private AstProductType type;
        private boolean needContextFlag;

        public FunctionDefiningInformation(String name, List<String> paramNames, AstProductType type, boolean doNeedContext){
            this.name = name;
            this.paramNames = paramNames;
            this.type = type;
            this.needContextFlag = doNeedContext;
        }
        public String getName(){
            return name;
        }
        public List<String> getParamNames(){
            return paramNames;
        }
        public AstProductType getType(){
            return type;
        }
        public boolean doNeedContext(){
            return needContextFlag;
        }
    }

    FunctionDefiningInformation superFunction;
    
    public class FunctionMeta extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree type = node.get(Symbol.unique("type"));
            AstProductType funtype = (AstProductType) AstType.nodeToType((SyntaxTree) type);


            SyntaxTree definition = node.get(Symbol.unique("definition"));

            SyntaxTree nodeName = node.get(Symbol.unique("name"));
            SyntaxTree nameNode = definition.get(Symbol.unique("name"));
            String name = nameNode.toText();
            //Functions are already defined in FunctionCpnstructVisitor
            if(!FunctionTable.getType(name).equals(funtype)){
                throw new Error("FunctionTable is broken: FunctionTable types "
                    +FunctionTable.getType(name)+" real types " + funtype);
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
                if(option.doCaseSplit()){
                    splitCaseSelector = new SplitCaseSelector(name, paramNames, option.getCaseSplitSpec());
                }
                if(newSet.isEmpty()){
                    newSet.add(new TypeMap());
                }
                for(TypeMap map : newSet){
                    for(String v : nJsvMap.keySet()){
                        AstType t = nJsvMap.get(v);
                        map.add(v, t);
                    }
                }
                /* add JSValue parameters (apply operand spec) */
                if (nJsvTypes > 0) {
                    String[] jsvParamNamesPacked = new String[nJsvTypes];
                    System.arraycopy(jsvParamNames, 0, jsvParamNamesPacked, 0, nJsvTypes);
                    VMDataTypeVecSet vtvs = option.getOperandSpec().getAccept(name, jsvParamNamesPacked);
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
                }else{
                    newSet2 = newSet;
                }
            }else{
                newSet2 = Collections.emptySet();
            }
            TypeMapSet newDict = TYPE_MAP.clone();
            newDict.setTypeMapSet(newSet2);
            newDict.setDispatchSet(node.getRematchVarSet());
            SyntaxTree body = (SyntaxTree) definition.get(Symbol.unique("body"));
            List<String> argNames = new ArrayList<>(paramsNode.size());
            for(SyntaxTree param : paramsNode){
                argNames.add(param.toText());
            }
            superFunction = new FunctionDefiningInformation(name, argNames, funtype,
                    FunctionTable.hasAnnotations(name, FunctionAnnotation.needContext));
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

    public class FunctionExpansionPair{
        private SyntaxTree original;
        private SyntaxTree expanded;

        public FunctionExpansionPair(SyntaxTree original, SyntaxTree expanded){
            if(original == null || expanded == null){
                throw new Error("Illigal FunctionExpansionPair constructor call: original="
                    +original+" expanded="+expanded);
            }
            this.original = original;
            this.expanded = expanded;
        }

        public SyntaxTree getOriginal(){
            return original;
        }

        public SyntaxTree getExpanded(){
            return expanded;
        }

        @Override
        public String toString(){
            return "("+original.toString()+",\n "+expanded.toString()+")";
        }

        @Override
        public int hashCode(){
            return original.hashCode() + expanded.hashCode();
        }

        @Override
        public boolean equals(Object obj){
            if(!(obj instanceof FunctionExpansionPair)) return false;
            FunctionExpansionPair that = (FunctionExpansionPair)obj;
            return this.original == that.getOriginal() && 
                this.expanded.equals(that.getExpanded());
        }
    }

    static class InliningStatus{
        static enum Status{
            MultiplePass, SinglePass, SinglePassAndFail, AllFail;
        }
        Status status;
        SyntaxTree inliningTo;
        public InliningStatus(Status status, SyntaxTree to){
            this.status = status;
            inliningTo = to;
        }
        public void pass(SyntaxTree to){
            if(status == Status.AllFail){
                status = Status.SinglePassAndFail;
                inliningTo = to;
                return;
            }
            if(status == Status.SinglePass && to.equals(inliningTo)){
                status = Status.SinglePass;
                return;
            }
            status = Status.MultiplePass;
        }
        public void fail(){
            if(status == Status.SinglePass){
                status = Status.SinglePassAndFail;
            }
        }
        public boolean shouldCaseExpansion(){
            return (status == Status.MultiplePass || status == Status.SinglePassAndFail);
        }
        @Override
        public String toString(){
            return status.toString();
        }
    }

    static class CaseSplitData{
        Map<FunctionExpansionPair, Set<TypeMap>> caseExpansionMap;
        List<Set<Set<TypeMap>>> caseExpansionConds;
        Map<SyntaxTree, InliningStatus> funcsInliningStatusMap;

        public CaseSplitData(int caseSize){
            caseExpansionConds = new ArrayList<>(Collections.nCopies(caseSize, null));
            caseExpansionMap = new HashMap<>();
            funcsInliningStatusMap = new HashMap<>();
        }
        public List<Set<Set<TypeMap>>> getCaseExpansionConds(){
            return caseExpansionConds;
        }
        public Map<SyntaxTree, InliningStatus> getFuncsInliningStatusMap(){
            return funcsInliningStatusMap;
        }
        public Map<FunctionExpansionPair, Set<TypeMap>> getCaseExpansionMap(){
            return caseExpansionMap;
        }
        public Map<FunctionExpansionPair, Set<TypeMap>> resetCaseExpansionMap(){
            caseExpansionMap = new HashMap<>();
            return caseExpansionMap;
        }
        public Map<SyntaxTree, InliningStatus> resetFuncsInliningStatusMap(){
            funcsInliningStatusMap = new HashMap<>();
            return funcsInliningStatusMap;
        }
        public void updateInlinigStatusMap(SyntaxTree node, SyntaxTree expanded, InliningResult result){
            InliningStatus status = funcsInliningStatusMap.get(node);
            if(status==null){
                status = (result == InliningResult.Pass)
                     ? new InliningStatus(InliningStatus.Status.SinglePass, expanded)
                     : new InliningStatus(InliningStatus.Status.AllFail, null);
            }else{
                if(result == InliningResult.Pass) status.pass(expanded);
                else status.fail();
            }
            funcsInliningStatusMap.put(node, status);
        }
    }

    Stack<CaseSplitData> caseSplitDataStack = new Stack<>();

    public class Match extends DefaultVisitor {
        private boolean shouldCaseExpansion(Map<SyntaxTree, InliningStatus> map){
            for(InliningStatus status : map.values()){
                if(status.shouldCaseExpansion()) return true;
            }
            return false;
        }
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            MatchProcessor mp = new MatchProcessor(node);
            SyntaxTree labelNode = node.get(Symbol.unique("label"), null);
            String label = labelNode == null ? null : labelNode.toText();
            TypeMapSet outDict = dict.getBottomDict();
            TypeMapSet entryDict;
            TypeMapSet newEntryDict = dict;
            CaseSplitData caseSplitData = new CaseSplitData(mp.size());
            List<Set<Set<TypeMap>>> caseExpansionConds = caseSplitData.getCaseExpansionConds();
            Map<FunctionExpansionPair, Set<TypeMap>> caseExpansionMap = caseSplitData.getCaseExpansionMap();
            Map<SyntaxTree, InliningStatus> funcsInliningStatusMap = caseSplitData.getFuncsInliningStatusMap();
            caseSplitDataStack.push(caseSplitData);
            do {
                entryDict = newEntryDict;
                matchStack.enter(label, mp.getFormalParams(), entryDict);
                for (int i = 0; i < mp.size(); i++) {
                    TypeMapSet dictCaseIn = entryDict.enterCase(mp.getFormalParams(), mp.getVMDataTypeVecSet(i));
                    if (dictCaseIn.noInformationAbout(mp.getFormalParams())){
                        continue;
                    }
                    caseExpansionMap = caseSplitData.resetCaseExpansionMap();
                    funcsInliningStatusMap = caseSplitData.resetFuncsInliningStatusMap();
                    SyntaxTree body = mp.getBodyAst(i);
                    TypeMapSet dictCaseOut = visit(body, dictCaseIn);
                    if(option.doCaseSplit()){
                        Set<Set<TypeMap>> domains = new HashSet<>();
                        for(Set<TypeMap> v : caseExpansionMap.values()){
                            Set<TypeMap> domain = new HashSet<>(mp.getFormalParams().length);
                            for(TypeMap t : v){
                                domain.add(t.select(Arrays.asList(mp.getFormalParams())));
                            }
                            domains.add(domain);
                        }
                        Set<Set<TypeMap>> selected = splitCaseSelector.select(ExpandedCaseCondMaker.getExpandedCaseCond(domains));
                        if(shouldCaseExpansion(funcsInliningStatusMap) && !selected.isEmpty()){
                            caseExpansionConds.set(i, selected);
                        }
                    }
                    outDict = outDict.combine(dictCaseOut);
                }
                newEntryDict = matchStack.pop();
            } while (!entryDict.equals(newEntryDict));
            node.setTypeMapSet(entryDict);
            SyntaxTree paramsNode = node.get(Symbol.unique("params"));
            save(paramsNode, outDict);
            if(option.doCaseSplit()){
                for(int i=0; i<mp.size(); i++){
                    Set<Set<TypeMap>> cond = caseExpansionConds.get(i);
                    if(cond != null) mp.setCaseExpansion(i, cond);
                }
                if(mp.hasExpand()){
                    SyntaxTree expandedTree = mp.getCaseExpandedTree();
                    node.addExpandedTreeCandidate(expandedTree);
                    outDict = visit(expandedTree, dict);
                }
            }
            caseSplitDataStack.pop();
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
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            AstProductType superFunctionType = superFunction.getType();
            if(superFunctionType == null){
                throw new Error("Connnot solve functionType: null");
            }
            AstType rangeType = superFunctionType.getRange();
            if(rangeType == null){
                ErrorPrinter.error("Missing function type declaration");
            }
            if(rangeType != AstType.get("void")){
                SyntaxTree valNode = node.get(0);
                for(TypeMap map : dict){
                    ExprTypeSet exprTypeSet = visit(valNode, map);
                    for(AstType t : exprTypeSet){
                        if(!(rangeType.isSuperOrEqual(t))){
                            ErrorPrinter.error("Function must return "+rangeType.toString()+" type, but returning "+t+" type", valNode);
                        }
                    }
                }
            }
            save(node.get(0), dict);
            return dict;
        }
        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }
    }

    public class Assignment extends DefaultVisitor {
        private boolean isPureVariable(SyntaxTree varNode){
            return varNode.getTag().toString().equals("Name");
        }
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree leftNode = node.get(Symbol.unique("left"));
            SyntaxTree rightNode = node.get(Symbol.unique("right"));
            Set<TypeMap> jsAssigned = new HashSet<>();
            for(TypeMap typeMap : dict){
                ExprTypeSet leftTypeSet = visit(leftNode, typeMap);
                for(AstType leftType : leftTypeSet){
                    ExprTypeSet assignTypeSet = visit(rightNode, typeMap);
                    for(AstType type : assignTypeSet){
                        if(!leftType.isSuperOrEqual(type)){
                            ErrorPrinter.error("Expression types "+type+", need types "+leftType, rightNode);
                        }
                        if(isPureVariable(leftNode)){
                            jsAssigned.addAll(dict.getAssignedSet(typeMap, leftNode.toText(), type));
                        }
                    }
                }
            }
            if(jsAssigned.isEmpty()) return dict;
            TypeMapSet newSet = TYPE_MAP.clone();
            newSet.setTypeMapSet(jsAssigned);
            return newSet;
        }

        public void saveType(SyntaxTree node, TypeMapSet dict) throws Exception {
        }
    }

    public class Declaration extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            SyntaxTree typeNode = node.get(Symbol.unique("type"));
            SyntaxTree varNode = node.get(Symbol.unique("var"));
            SyntaxTree exprNode = (node.has(Symbol.unique("expr"))) ? node.get(Symbol.unique("expr")) : null;
            AstType varType = AstType.nodeToType(typeNode);
            if(varType==null){
                ErrorPrinter.error("Type not found: "+typeNode.toText(), typeNode);
            }
            String varName = varNode.toText();
            Set<TypeMap> newSet = new HashSet<>();
            for(TypeMap typeMap : dict){
                ExprTypeSet exprTypeSet;
                if(exprNode != null){
                    exprTypeSet = visit(exprNode, typeMap);
                }else{
                    exprTypeSet = EXPR_TYPE.clone();
                    exprTypeSet.add(varType);
                }
                for(AstType type : exprTypeSet){
                    if(!varType.isSuperOrEqual(type)){
                        ErrorPrinter.error("Expression types "+type+", need types "+varType, exprNode);
                    }
                    TypeMap temp = typeMap.clone();
                    Set<TypeMap> addedSet = dict.getAddedSet(temp, varName, type);
                    newSet.addAll(addedSet);
                }
            }
            TypeMapSet newTypeMapSet = dict.clone();
            newTypeMapSet.setTypeMapSet(newSet);
            if(exprNode != null){
                save(exprNode, dict);
            }
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
            TypeMapSet resultDict;
            if (node.has(Symbol.unique("else"))) {
                SyntaxTree elseNode = node.get(Symbol.unique("else"));
                TypeMapSet elseDict = visit(elseNode, copyDict);
                if(!thenDict.getKeys().equals(elseDict.getKeys())){
                    ErrorPrinter.error("Both environment keys must be equal", node);
                }
                resultDict = thenDict.combine(elseDict);
            } else {
                resultDict = thenDict;
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
            TypeMapSet doDict = dict.clone();
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

    public class CTypeDef extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            return dict;
        }
    }

    public class CObjectmapping extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            return dict;
        }
    }

    public class CFunction extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
            return dict;
        }
    }

    public class CConstantDef extends DefaultVisitor {
        @Override
        public TypeMapSet accept(SyntaxTree node, TypeMapSet dict) throws Exception {
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
    // BinaryOperators
    //**********************************

    private ExprTypeSet binaryOperator(SyntaxTree node, TypeMap dict, OperatorTypeChecker checker) throws Exception{
        SyntaxTree leftNode  = node.get(Symbol.unique("left"));
        SyntaxTree rightNode = node.get(Symbol.unique("right"));
        ExprTypeSet leftExprTypeSet  = visit(leftNode, dict);
        ExprTypeSet rightExprTypeSet = visit(rightNode, dict);
        ExprTypeSet resultTypeSet = EXPR_TYPE.clone();
        for (AstType lt : leftExprTypeSet){
            for (AstType rt : rightExprTypeSet){
                AstType result = checker.typeOf(lt, rt);
                if(result == null){
                    ErrorPrinter.errorForRecvNode("Illigal types given in operator: "
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
			return binaryOperator(node, dict, OperatorTypeChecker.OR);
        }
    }

    public class And extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.AND);
        }
    }

    public class BitwiseOr extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.BITWISE_OR);
        }
    }

    public class BitwiseXOr extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.BITWISE_XOR);
        }
    }

    public class BitwiseAnd extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.BITWISE_AND);
        }
    }

    public class Equals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.EQUALS);
        }
    }

    public class NotEquals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.NOT_EQUALS);
        }
    }

    public class LessThanEquals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.LESSTHAN_EQUALS);
        }
    }

    public class GreaterThanEquals extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.GRATORTHAN_EQUALS);
        }
    }

    public class LessThan extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.LESSTHAN);
        }
    }

    public class GreaterThan extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.GRATORTHAN);
        }
    }

    public class LeftShift extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.LEFT_SHIFT);
        }
    }

    public class RightShift extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.RIGHT_SHIFT);
        }
    }

    public class Add extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.ADD);
        }
    }

    public class Sub extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.SUB);
        }
    }

    public class Mul extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.MUL);
        }
    }

    public class Div extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.DIV);
        }
    }

    public class Mod extends DefaultVisitor {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
			return binaryOperator(node, dict, OperatorTypeChecker.MOD);
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

    private enum InliningResult{Pass, Fail;}

    public class FunctionCall extends DefaultVisitor {
        private final OperandSpecifications.OperandSpecificationRecord.Behaviour ACCEPT =
            OperandSpecifications.OperandSpecificationRecord.Behaviour.ACCEPT;
        private void typeDeclarationCheck(String functionName, SyntaxTree node){
            AstType type = FunctionTable.getType(functionName);
            if(type == null){
                ErrorPrinter.error("Function not found: "+functionName, node);
            }
            if(!(type instanceof AstProductType)){
                ErrorPrinter.error("Non function refered as function: "+functionName, node);
            }
        }
        private void needContextCheck(String functionName, SyntaxTree node){
            if(!FunctionTable.contains(functionName)){
                ErrorPrinter.error("Function is not found: "+functionName);
            }
            if(!superFunction.doNeedContext() && FunctionTable.hasAnnotations(functionName, FunctionAnnotation.needContext)){
                ErrorPrinter.errorForRecvNode("Call function that need context in function that does not need", node);
            }
        }
        private void argumentSizeCheck(AstTypeVec domain, SyntaxTree argNode, String functionName){
            if(argNode.size()==0){
                if(domain.size() != 1 || !domain.contains(AstType.get("void"))){
                    ErrorPrinter.error("Argument size does not match: "+functionName, argNode);
                }
            }else{
                if(domain.size() != argNode.size()){
                    ErrorPrinter.error("Argument size does not match: "+functionName, argNode);
                }
            }
        }
        private List<ExprTypeSet> getRealTypes(AstTypeVec argumentVec, SyntaxTree argumentNode, TypeMap dict) throws Exception{
            if(argumentNode.size()==0){
                return Collections.emptyList();
            }
            SyntaxTree[] arguments = (SyntaxTree[])argumentNode.getSubTree();
            int length = arguments.length;
            List<ExprTypeSet> realTypesList = new ArrayList<>(length);
            for(int i=0; i<length; i++){
                ExprTypeSet realTypes = visit(arguments[i], dict);
                realTypesList.add(realTypes);
                for(AstType t : realTypes){
                    if(!argumentVec.get(i).isSuperOrEqual(t)){
                        ErrorPrinter.error("Argument types "+t.toString()
                            +", need types "+realTypesList.get(i), arguments[i]);
                    }
                }
            }
            return realTypesList;
        }
        private void updateFTD(Set<AstTypeVec> argumentVecSet, TypeMap dict, String functionName){
            AstTypeVec superFunctionArgumetVec =  new AstTypeVec(dict.get(superFunction.getParamNames()));
            for(AstTypeVec argumentVec : argumentVecSet){
                TypeDependencyProcessor.addDependency(superFunction.getName(), superFunctionArgumetVec.getList(), functionName, argumentVec.getList());
            }
        }
        private void inliningRecord(SyntaxTree node, TypeMap dict, String functionName, List<ExprTypeSet> argTypeList) throws Exception{
            if(!InlineFileProcessor.isInlineExpandable(functionName)){
                return;
            }
            SyntaxTree expandedNode = InlineFileProcessor.inlineExpansion(node, argTypeList);
            if(expandedNode.equals(node)){
                node.setFailToExpansion();
                return;
            }
            node.addExpandedTreeCandidate(expandedNode);
            if(!caseSplitDataStack.empty()){
                CaseSplitData caseSplitData = caseSplitDataStack.peek();
                caseSplitData.updateInlinigStatusMap(node, expandedNode, InliningResult.Pass);
                Map<FunctionExpansionPair, Set<TypeMap>> caseExpansionMap = caseSplitData.getCaseExpansionMap();
                FunctionExpansionPair pair = new FunctionExpansionPair(node, expandedNode);
                Set<TypeMap> request = caseExpansionMap.get(pair);
                if(request == null){
                    request = new HashSet<TypeMap>();
                }
                request.add(dict);
                caseExpansionMap.put(pair, request);
            }
            visit(expandedNode, dict);
        }
        private void updateFunctionSpec(String functionName, Set<AstTypeVec> calledTypeVecSet){
            OperandSpecifications funcSpec = option.getFunctionSpec();
            if(!funcSpec.hasName(functionName)){
                return;
            }
             Set<VMDataTypeVec> vmdtVecSet = AstTypeVec.toVMDataTypeVecSet(calledTypeVecSet);
             for(VMDataTypeVec vmdtVec : vmdtVecSet){
                 List<String> vmdtNames = vmdtVec.toStringList();
                 if(funcSpec.findSpecificationRecord(functionName, vmdtVec.toArray()).behaviour != ACCEPT){
                     funcSpec.insertRecord(functionName, vmdtNames.toArray(new String[0]), ACCEPT);
                 }
             }
        }
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            SyntaxTree recvNode = node.get(Symbol.unique("recv"));
            SyntaxTree argsNode = node.get(Symbol.unique("args"));
            String functionName = recvNode.toText();
            typeDeclarationCheck(functionName, recvNode);
            needContextCheck(functionName, node);
            AstProductType functionType = (AstProductType)FunctionTable.getType(functionName);
            AstTypeVec argumentTypeVec = new AstTypeVec(functionType.getDomain());
            argumentSizeCheck(argumentTypeVec, argsNode, functionName);
            List<ExprTypeSet> argTypeList = getRealTypes(argumentTypeVec, argsNode, dict);
            Set<AstTypeVec> argumentVecSet = AstTypeVec.toAstTypeVecSet(argTypeList);
            if(option.doUpdateFTD()){
                updateFTD(argumentVecSet, dict, functionName);
            }
            if(option.doFunctionInlining()){
                inliningRecord(node, dict, functionName, argTypeList);
            }
            if(option.doUpdateFunctionSpec()){
                updateFunctionSpec(functionName, argumentVecSet);
            }
            AstType range = functionType.getRange();
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

    public class LeftHandIndex extends ArrayIndex {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            return super.accept(node, dict);
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

    public class LeftHandField extends FieldAccess {
        @Override
        public ExprTypeSet accept(SyntaxTree node, TypeMap dict) throws Exception {
            return super.accept(node, dict);
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
