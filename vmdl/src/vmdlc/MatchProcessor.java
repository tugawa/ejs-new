/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dispatch.RuleSetBuilder;
import nez.ast.Symbol;
import nez.ast.Tree;
import type.AstType;
import type.TypeMap;
import type.AstType.JSValueVMType;
import type.VMDataType;
import type.VMDataTypeVecSet;

public class MatchProcessor {
    static final boolean DEBUG = false;

    String[] formalParams;
    String label;
    // following two lists share index
    List<Set<VMDataType[]>> vmtVecCondList;
    Set<VMDataType[]> nonMatchCond;
    List<SyntaxTree> caseBodyAsts;
    List<SyntaxTree> originalCases;
    Map<Integer, Set<Set<TypeMap>>> caseExpansionConds = new HashMap<>();
    SyntaxTree matchNode;
    List<String> formalParamsList = null;

    MatchProcessor(SyntaxTree node) {
        matchNode = node;
        caseBodyAsts = new ArrayList<SyntaxTree>();

        /* retrieve formal parameters */
        SyntaxTree params = node.get(Symbol.unique("params"));
        formalParams = new String[params.size()];
        for (int i = 0; i < params.size(); i++)
            formalParams[i] = params.get(i).toText();
        formalParamsList = Arrays.asList(formalParams);

        /* retrieve label */
        Tree<?> labelNode = node.get(Symbol.unique("label"), null);
        if (labelNode != null)
            label = labelNode.toText();

        /* compute conditions */
        SyntaxTree cases = node.get(Symbol.unique("cases"));
        originalCases = new ArrayList<>(cases.countSubNodes());
        RuleSetBuilder rsb = new RuleSetBuilder(formalParams);
        List<RuleSetBuilder.Node> condAstList = new ArrayList<RuleSetBuilder.Node>();
        for (SyntaxTree k : cases) {
            if (k.is(Symbol.unique("AnyCase"))) {
                RuleSetBuilder.Node condAst = new RuleSetBuilder.TrueNode();
                condAstList.add(condAst);
            } else {
                SyntaxTree pat = k.get(Symbol.unique("pattern"));
                RuleSetBuilder.Node condAst = toRsbAst(pat, rsb);
                condAstList.add(condAst);
            }
            SyntaxTree bodyNode = k.get(Symbol.unique("body"));
            caseBodyAsts.add(bodyNode);
            originalCases.add(k);
        }
        if (DEBUG) {
            System.out.println("======== Condition Begin ========");
            for (RuleSetBuilder.Node n: condAstList) {
                System.out.println(n);
            }
            System.out.println("======== Condition End ========");
        }
        List<Set<VMDataType[]>> fullCondList = rsb.computeVmtVecCondList(condAstList);
        vmtVecCondList = fullCondList.subList(0, fullCondList.size()-1);
        nonMatchCond = fullCondList.get(fullCondList.size()-1);

        if (DEBUG) {
            System.out.println("======== Computed Condition Begin ========");
            {
                int i = 0;
                for (Set<VMDataType[]> vmtVecCond: vmtVecCondList) {
                    i++;
                    for (VMDataType[] vmts: vmtVecCond) {
                        System.out.print(i);
                        for (VMDataType vmt: vmts)
                            System.out.print(" "+vmt);
                        System.out.println();
                    }
                }
            }
            System.out.println("======== Computed Condition End ========");
        }
    }   

    private RuleSetBuilder.Node toRsbAst(Tree<?> n, RuleSetBuilder rsb) {
        if (n.is(Symbol.unique("AndPattern"))) {
            RuleSetBuilder.Node left = toRsbAst(n.get(0), rsb);
            RuleSetBuilder.Node right = toRsbAst(n.get(1), rsb);
            return new RuleSetBuilder.AndNode(left, right);
        } else if (n.is(Symbol.unique("OrPattern"))) {
            RuleSetBuilder.Node left = toRsbAst(n.get(0), rsb);
            RuleSetBuilder.Node right = toRsbAst(n.get(1), rsb);
            return new RuleSetBuilder.OrNode(left, right);
        } else if (n.is(Symbol.unique("NotPattern"))) {
            RuleSetBuilder.Node child = toRsbAst(n.get(0), rsb);
            return new RuleSetBuilder.NotNode(child);
        } else if (n.is(Symbol.unique("TypePattern"))) {
            String opName = n.get(Symbol.unique("var")).toText();
            String typeName = n.get(Symbol.unique("type")).toText();
            AstType t = AstType.get(typeName);
            if (!(t instanceof JSValueVMType))
                throw new Error(typeName +" is not a JSValueVMType");
            VMDataType dt = ((JSValueVMType) AstType.get(typeName)).getVMDataType();
            return rsb.new AtomicNode(opName, dt);
        }
        throw new Error("no such pattern");
    }

    String[] getFormalParams() {
        return formalParams;
    }

    String getLabel() {
        return label;
    }

    /* old interface */
    Set<VMDataType[]> getVmtVecCond(int index) {
        return vmtVecCondList.get(index);
    }

    /* new interface */
    VMDataTypeVecSet getVMDataTypeVecSet(int index) {
        return new VMDataTypeVecSet.BySet(formalParams, vmtVecCondList.get(index));
    }

    VMDataTypeVecSet getNonMatchCondVecSet(){
        return new VMDataTypeVecSet.BySet(formalParams, nonMatchCond);
    }

    SyntaxTree getBodyAst(int index) {
        return caseBodyAsts.get(index);
    }

    int size() {
        return vmtVecCondList.size();
    }

    void setCaseExpansion(int pos, Set<Set<TypeMap>> cond){
        Set<Set<TypeMap>> oldCond = caseExpansionConds.get(pos);
        if(oldCond == null){
            caseExpansionConds.put(pos, cond);
        }else{
            System.err.println("Warning: caseExpansionCond is already exist.");
            oldCond.addAll(cond);
        }
    }

    private SyntaxTree getTypePatternSub(List<VMDataType> vmtVec, List<String> paramList){
        int dimension = vmtVec.size();
        if(dimension == 1){
            return ASTHelper.generateTypePattern(AstType.get(vmtVec.get(0)).toString(), paramList.get(0));
        }
        SyntaxTree recvLeft = getTypePatternSub(vmtVec.subList(0, dimension-1), paramList);
        SyntaxTree right = ASTHelper.generateTypePattern(AstType.get(vmtVec.get(dimension-1)).toString(), paramList.get(dimension-1));
        return ASTHelper.generateAndPattern(recvLeft, right);
    }


    private SyntaxTree getTypePattern(List<List<VMDataType>> vmtVecList, List<String> paramList){
        int dimension = vmtVecList.size();
        if(dimension == 1){
            return getTypePatternSub(vmtVecList.get(0), paramList);
        }
        SyntaxTree recvLeft = getTypePattern(vmtVecList.subList(0, dimension-1), paramList);
        SyntaxTree right = getTypePatternSub(vmtVecList.get(dimension-1), paramList);
        return ASTHelper.generateOrPattern(recvLeft, right);
    }

    private void addLabelSuffix(SyntaxTree target, String suffix, String exceptLabel){
        if(target.is(Symbol.unique("Match")) || target.is(Symbol.unique("Rematch"))){
            SyntaxTree labelNode = target.get(Symbol.unique("label"), null);
            if(labelNode != null && !labelNode.toText().equals(exceptLabel)){
                SyntaxTree addedSuffix = labelNode.dup();
                addedSuffix.setValue(labelNode.toText()+"_"+suffix);
                target.set(Symbol.unique("label"), addedSuffix);
            }
        }
        Tree<SyntaxTree>[] subTree = target.getSubTree();
        if(subTree == null) return;
        for(Tree<SyntaxTree> child : target.getSubTree()){
            addLabelSuffix((SyntaxTree)child, suffix, exceptLabel);
        }
    }

    private SyntaxTree getReplacedCaseNode(int number, Set<TypeMap> typeMaps, String labelSuffix){
        SyntaxTree caseBody = caseBodyAsts.get(number).dup();
        addLabelSuffix(caseBody, labelSuffix, label);
        List<List<VMDataType>> vmtVecLists = new ArrayList<>();
        for(TypeMap typeMap : typeMaps){
            List<VMDataType> vmtVecList = new ArrayList<>();
            TypeMap selectedMap = typeMap.select(formalParamsList);
            for(String var : formalParamsList){
                Set<VMDataType> types = selectedMap.get(var).getVMDataTypes();
                if(types.size() != 1){
                    ErrorPrinter.error("Case condition has non-VMDataType");
                }
                vmtVecList.add(types.iterator().next());
            }
            vmtVecLists.add(vmtVecList);
        }
        SyntaxTree caseCond = getTypePattern(vmtVecLists, formalParamsList);
        return ASTHelper.generateCase(caseCond, caseBody);
    }

    private static int additionalLabelSuffixNumber = 0;

    private SyntaxTree getCaseExpandedCasesNode(){
        int originalCaseSize = caseBodyAsts.size();
        List<SyntaxTree> newCases = new ArrayList<>(matchNode.get(Symbol.unique("cases")).countSubNodes()*2);
        for(int i=0; i<originalCaseSize; i++){
            Set<Set<TypeMap>> expansionCond = caseExpansionConds.get(i);
            /*
            System.err.println("Case number "+i+" recieves typemaps for:");
            System.err.print("{");
            for(VMDataType[] vec : vmtVecCondList.get(i)){
                System.err.print(Arrays.toString(vec)+" ");
            }
            System.err.println("}");
            */
            if(expansionCond != null){
                //System.err.println("Case number "+i+"is expanded for "+expansionCond.size()+" typeMap(s):");
                for(Set<TypeMap> typeMap : expansionCond){
                    //System.err.println(typeMap.toString());
                    SyntaxTree replacedNode = getReplacedCaseNode(i, typeMap, Integer.toString((additionalLabelSuffixNumber++)));
                    if(replacedNode != null){
                        newCases.add(replacedNode);
                    }
                }
            }
            newCases.add(originalCases.get(i).dup());
        }
        return ASTHelper.generateCases(newCases.toArray(new SyntaxTree[0]));
    }

    public SyntaxTree getCaseExpandedTree(){
        SyntaxTree expandedCasesNode = getCaseExpandedCasesNode();
        SyntaxTree expandedNode = matchNode.dup();
        expandedNode.set(Symbol.unique("cases"), expandedCasesNode);
        return expandedNode;
    }

    public boolean hasExpand(){
        return !caseExpansionConds.isEmpty();
    }

    public void printCaseCond(){
        System.err.println("--------");
        System.err.println("CASE SIZE:"+vmtVecCondList.size());
        System.err.println("--------");
        int size = vmtVecCondList.size();
        for(int i=0; i<size; i++){
            Set<VMDataType[]> set = vmtVecCondList.get(i);
        //for(Set<VMDataType[]> set : vmtVecCondList){
            for(VMDataType[] vec : set){
                System.err.println(Arrays.toString(vec));
            }
            System.err.println("---");
            System.err.println(caseBodyAsts.get(i).toString());
            System.err.println("--------");
        }
    }
}
