/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmgen;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import vmgen.DslParser.WhenClause;


public class ProcDefinition {

    static class TypeDispatchDefinition {
        String[] vars;
        Set<RuleSet.Rule> rules;
        TypeDispatchDefinition(String[] vars, Set<RuleSet.Rule> rules) {
            this.vars = vars;
            this.rules = rules;
        }
    }

    static class InstDefinition {

        String name;
        String[] dispatchVars, otherVars;
        String prologue, epilogue;
        RuleSet rs;
        InstDefinition(String name, String[] dispatchVars, String[] otherVars, String prologue, String epilogue, RuleSet rs) {
            this.name = name;
            this.dispatchVars = dispatchVars;
            this.otherVars = otherVars;
            this.prologue = prologue;
            this.epilogue = epilogue;
            this.rs = rs;
        }
    }

    InstDefinition makeInstDefinitionFromParsedInst(DslParser.InstDef ins) {
        WhenClause otherwise = null;
        List<RuleSetBuilder.CaseActionPair> caps = new ArrayList<RuleSetBuilder.CaseActionPair>();
        RuleSetBuilder rsb = new RuleSetBuilder(ins.vars);
        for (WhenClause c: ins.whenClauses) {
            if (c.condition == null) {
                otherwise = c;
                continue;
            }
            RuleSetBuilder.Node root = convertToRSBAST(rsb, c.condition);
            RuleSetBuilder.CaseActionPair cap = new RuleSetBuilder.CaseActionPair(root, c.body);
            caps.add(cap);
        }
        if (otherwise != null) {
            RuleSetBuilder.CaseActionPair cap =
                    new RuleSetBuilder.CaseActionPair(new RuleSetBuilder.TrueNode(), otherwise.body);
            caps.add(cap);
        }
        RuleSet rs = rsb.createRuleSet(caps);     
        return new InstDefinition(ins.id, ins.vars, null, ins.prologue, ins.epilogue, rs);
    }

    private RuleSetBuilder.Node convertToRSBAST(RuleSetBuilder rsb, DslParser.Condition xn) {
        if (xn instanceof DslParser.CompoundCondition) {
            DslParser.CompoundCondition n = (DslParser.CompoundCondition) xn;
            RuleSetBuilder.Node l = convertToRSBAST(rsb, n.cond1);
            RuleSetBuilder.Node r = convertToRSBAST(rsb, n.cond2);
            if (n.op == DslParser.ConditionalOp.AND)
                return new RuleSetBuilder.AndNode(l, r);
            else if (n.op == DslParser.ConditionalOp.OR)
                return new RuleSetBuilder.OrNode(l, r);
            else
                throw new Error();
        } else if (xn instanceof DslParser.AtomCondition) {
            DslParser.AtomCondition n = (DslParser.AtomCondition) xn;
            return rsb.new AtomicNode(n.varName, n.t);
        } else
            throw new Error();
    }

    public InstDefinition load(String fname) {
        DslParser dslp = new DslParser();
        DslParser.InstDef parsedInst = dslp.run(fname);
        InstDefinition instDef = makeInstDefinitionFromParsedInst(parsedInst);
        return instDef;
    }
}
