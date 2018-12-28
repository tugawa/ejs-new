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
import type.VMDataType;

public class MatchProcessor {
    String[] formalParams;
    Map<SyntaxTree, Set<VMDataType[]>> vmtVecCondMap;
    
    MatchProcessor(SyntaxTree node) {
        /* retrieve formal parameters */
        SyntaxTree params = node.get(Symbol.unique("params"));
        formalParams = new String[params.size()];
        for (int i = 0; i < params.size(); i++)
            formalParams[i] = params.get(i).toText();
        
        /* compute conditions */
        SyntaxTree cases = node.get(Symbol.unique("cases"));
        RuleSetBuilder rsb = new RuleSetBuilder(formalParams);
        List<RuleSetBuilder.Node> condAstList = new ArrayList<RuleSetBuilder.Node>();
        for (SyntaxTree k : cases) {
            RuleSetBuilder.Node condAst = toRsbAst(k, rsb);
            condAstList.add(condAst);
        }
        List<Set<VMDataType[]>> vmtVecCondList = rsb.computeVmtVecCondList(condAstList);
        vmtVecCondMap = new HashMap<SyntaxTree, Set<VMDataType[]>>();
        for (int i = 0; i < cases.size(); i++) {
            SyntaxTree k = cases.get(i);
            Set<VMDataType[]> vmtVecCond = vmtVecCondList.get(i);
            vmtVecCondMap.put(k, vmtVecCond);
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
            VMDataType dt = ((JSValueVMType) AstType.get(typeName)).getVMDataType();
            return rsb.new AtomicNode(opName, dt);
        }
        throw new Error("no such pattern");
    }

    String[] getFormalParams() {
        return formalParams;
    }

    Set<VMDataType[]> getVmtVecCond(SyntaxTree caseNode) {
        return vmtVecCondMap.get(caseNode);
    }    
}
