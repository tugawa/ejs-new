package vmdlc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dispatch.RuleSetBuilder;
import nez.ast.Symbol;
import nez.ast.Tree;
import type.AstType;
import type.AstType.JSValueVMType;
import vmdlc.AstToCVisitor.MatchRecord;
import type.VMDataType;
import type.VMDataTypeVecSet;

public class MatchProcessor {
    static final boolean DEBUG = false;
    
    String[] formalParams;
    String label;
    // following two lists share index
    List<Set<VMDataType[]>> vmtVecCondList;
    List<SyntaxTree> caseBodyAsts;
    
    MatchProcessor(SyntaxTree node) {
        caseBodyAsts = new ArrayList<SyntaxTree>();
        
        /* retrieve formal parameters */
        SyntaxTree params = node.get(Symbol.unique("params"));
        formalParams = new String[params.size()];
        for (int i = 0; i < params.size(); i++)
            formalParams[i] = params.get(i).toText();
        
        /* retrieve label */
        Tree<?> labelNode = node.get(Symbol.unique("label"), null);
        if (labelNode != null)
            label = labelNode.toText();
        
        /* compute conditions */
        SyntaxTree cases = node.get(Symbol.unique("cases"));
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
        }
        if (DEBUG) {
            System.out.println("======== Condition Begin ========");
            for (RuleSetBuilder.Node n: condAstList) {
                System.out.println(n);
            }
            System.out.println("======== Condition End ========");
        }
        vmtVecCondList = rsb.computeVmtVecCondList(condAstList);
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
    
    SyntaxTree getBodyAst(int index) {
        return caseBodyAsts.get(index);
    }
    
    int size() {
        return vmtVecCondList.size();
    }
}
