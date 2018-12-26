/*
   ProcDefinition.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Takafumi Kataoka, 2016-18
	 Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
*/
package old.insngen;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dispatch.RuleSet;
import type.VMDataType;

class JSTypePair {
    VMDataType left, right;
    JSTypePair(VMDataType left, VMDataType right) {
        this.left = left;
        this.right = right;
    }
    public String toString() {
        String l, r;
        l = (left == null) ? "***" : left.getName();
        r = (right == null) ? "***" : right.getName();
        return "(" + l + " : " + r + ")";
    }
}


public class InstructionDefinition {

    static class TypeDispatchDefinition {
        String[] vars;
        Set<RuleSet.Rule> rules;
        TypeDispatchDefinition(String[] vars, Set<RuleSet.Rule> rules) {
            this.vars = vars;
            this.rules = rules;
        }
    }

    InstructionDefinition.TypeDispatchDefinition build(String[] vars, List<Pair<JSTypePair,String>> raw) {
        List<Pair<JSTypePair,String>> twoOp = new LinkedList<Pair<JSTypePair,String>>();
        List<Pair<JSTypePair,String>> oneOpL = new LinkedList<Pair<JSTypePair,String>>();
        List<Pair<JSTypePair,String>> oneOpR = new LinkedList<Pair<JSTypePair,String>>();
        Pair<JSTypePair,String> otherwisep = new Pair<JSTypePair,String>(null, null);
        splitByOpType(raw, twoOp, oneOpL, oneOpR, otherwisep);

        String otherwise = otherwisep.second();
        List<Pair<JSTypePair,String>> result = new LinkedList<Pair<JSTypePair,String>>();
        if (vars.length == 1) {
            result.addAll(oneOpL);
            if (otherwise != null) {
                for (VMDataType dt : VMDataType.all()) {
                    boolean b = true;
                    for (Pair<JSTypePair,String> e : result) {
                        JSTypePair jtp = e.first();
                        if (jtp.left == dt) {
                            b = false;
                            break;
                        }
                    }
                    if (b) result.add(new Pair<JSTypePair,String>(new JSTypePair(dt, null), otherwise));
                }
            }
        } else if (vars.length == 2) {
            result.addAll(twoOp);
            for (Pair<JSTypePair,String> el : oneOpL) {
                VMDataType left = el.first().left;
                for (VMDataType right : VMDataType.all()) {
                    boolean willBeAdded = true;
                    for (Pair<JSTypePair,String> etwo : twoOp) {
                        if (etwo.first().left == left && etwo.first().right == right) {
                            willBeAdded = false;
                            break;
                        }
                    }
                    if (!willBeAdded) continue;
                    for (Pair<JSTypePair,String> er : oneOpR) {
                        if (er.first().right == right) {
                            willBeAdded = false;
                            break;
                        }
                    }
                    if (!willBeAdded) continue;
                    result.add(new Pair<JSTypePair,String>(new JSTypePair(left, right), el.second()));
                }
            }
            for (Pair<JSTypePair,String> er : oneOpR) {
                VMDataType right = er.first().right;
                for (VMDataType left : VMDataType.all()) {
                    boolean willBeAdded = true;
                    for (Pair<JSTypePair,String> etwo : twoOp) {
                        if (etwo.first().left == left && etwo.first().right == right) {
                            willBeAdded = false;
                            break;
                        }
                    }
                    if (!willBeAdded) continue;
                    for (Pair<JSTypePair,String> el : oneOpL) {
                        if (el.first().left == left) {
                            willBeAdded = false;
                            break;
                        }
                    }
                    if (!willBeAdded) continue;
                    result.add(new Pair<JSTypePair,String>(new JSTypePair(left, right), er.second()));
                }
            }
            if (otherwise != null) {
                for (VMDataType left : VMDataType.all()) {
                    for (VMDataType right : VMDataType.all()) {
                        boolean willBeAdded = true;
                        for (Pair<JSTypePair,String> e : result) {
                            if (e.first().left == left && e.first().right == right) {
                                willBeAdded = false;
                                break;
                            }
                        }
                        if (willBeAdded) {
                            result.add(new Pair<JSTypePair,String>(new JSTypePair(left, right), otherwise));
                        }
                    }
                }
            }
        }
        return new InstructionDefinition.TypeDispatchDefinition(vars, partsToRules(vars, result));
    }
    private Set<RuleSet.Rule> partsToRules(String[] vars, List<Pair<JSTypePair,String>> parts) {
        List<Pair<JSTypePair,String>> _parts = new LinkedList<Pair<JSTypePair,String>>(parts);
        Set<RuleSet.Rule> rules = new HashSet<RuleSet.Rule>();
        for (Pair<JSTypePair,String> e : parts) {
            if (_parts.contains(e)) {
                Set<RuleSet.OperandDataTypes> conditions = new HashSet<RuleSet.OperandDataTypes>();
                List<Pair<JSTypePair,String>> rmList = new LinkedList<Pair<JSTypePair,String>>();
                for (Pair<JSTypePair,String> _e : _parts) {
                    if (e.second() == _e.second()) {
                        if (vars.length == 1) {
                            conditions.add(new RuleSet.OperandDataTypes(_e.first().left.getName()));
                        } else if (vars.length == 2) {
                            conditions.add(new RuleSet.OperandDataTypes(_e.first().left.getName(), _e.first().right.getName()));
                        }
                        rmList.add(_e);
                    }
                }
                _parts.removeAll(rmList);
                rules.add(new RuleSet.Rule(e.second(), conditions));
            }
        }
        return rules;
    }

    private void splitByOpType(List<Pair<JSTypePair,String>> raw,
            List<Pair<JSTypePair,String>> twoOp,
            List<Pair<JSTypePair,String>> oneOpL,
            List<Pair<JSTypePair,String>> oneOpR,
            Pair<JSTypePair,String> otherwise) {
        for (Pair<JSTypePair,String> e : raw) {
            JSTypePair j = e.first();
            if (j == null) {
                otherwise.t = e.second();
            } else if (j.left != null) {
                if (j.right != null) {
                    twoOp.add(e);
                } else {
                    oneOpL.add(e);
                }
            } else {
                if (j.right != null) {
                    oneOpR.add(e);
                } else {
                    otherwise.t = e.second();
                }
            }
        }
    }

    private void conditionIntoParts(List<JSTypePair> result, DslParser.Condition arg) {
        if (arg instanceof DslParser.CompoundCondition) {
            DslParser.CompoundCondition cond = (DslParser.CompoundCondition) arg;
            if (cond.op == DslParser.ConditionalOp.AND) {
                if (cond.cond1 instanceof DslParser.AtomCondition && cond.cond2 instanceof DslParser.AtomCondition) {
                    DslParser.AtomCondition c1 = (DslParser.AtomCondition) cond.cond1;
                    DslParser.AtomCondition c2 = (DslParser.AtomCondition) cond.cond2;
                    if (c1.varIdx == 0 && c2.varIdx == 1) {
                        result.add(new JSTypePair(c1.t, c2.t));
                    } else if (c2.varIdx == 0 && c1.varIdx == 1) {
                        result.add(new JSTypePair(c2.t, c1.t));
                    } else {
                        // error
                        System.out.println("error: " + c1.varIdx + ", " + c2.varIdx);
                    }
                } else {
                    // error
                    System.out.println("error: cond1 and cond2 must be AtomCondition.");
                }
            } else if (cond.op == DslParser.ConditionalOp.OR) {
                conditionIntoParts(result, cond.cond1);
                conditionIntoParts(result, cond.cond2);
            }
        } else if (arg instanceof DslParser.AtomCondition) {
            DslParser.AtomCondition atom = (DslParser.AtomCondition) arg;
            if (atom.varIdx == 0) {
                result.add(new JSTypePair(atom.t, null));
            } else if (atom.varIdx == 1) {
                result.add(new JSTypePair(null, atom.t));
            } else {
                // error
                System.out.println("error: AtomCondition.varIdx must be '0' or '1'.");
            }
        }
    }
    // private List<Pair<JSTypePair,String>> actListToParts(List<Pair<DslParser.Condition,String>> actList) {
    private List<Pair<JSTypePair,String>> whenClausesToParts(List<DslParser.WhenClause> actList) {
        List<Pair<JSTypePair,String>> result = new LinkedList<Pair<JSTypePair,String>>();
        actList.forEach(act -> {
            if (act.condition == null) {
                result.add(new Pair<JSTypePair,String>(null, act.body));
            } else {
                List<JSTypePair> jtps = new LinkedList<JSTypePair>();
                conditionIntoParts(jtps, act.condition);
                for (JSTypePair j1 : jtps) {
                    boolean b = true;
                    for (Pair<JSTypePair,String> e : result) {
                        JSTypePair j2 = e.first();
                        if (j1.left == j2.left && j1.right == j2.right) {
                            // error
                            b = false;
                            System.out.println("error: same condition (" + j1 + ", " + j2 + ")");
                        }
                    }
                    if (b) result.add(new Pair<JSTypePair, String>(j1, act.body));
                }
            }
        });
        return result;
    }
    TypeDispatchDefinition makeDispatchDefFromInstDef(DslParser.InstDef idef) {
        List<Pair<JSTypePair,String>> actions = whenClausesToParts(idef.whenClauses);
        TypeDispatchDefinition td = build(idef.vars, actions);
        return td;
    }

    static class InstDefinition {
        String name;
        String[] dispatchVars, otherVars;
        String prologue, epilogue;
        TypeDispatchDefinition tdDef;
        InstDefinition(String name, String[] dispatchVars, String[] otherVars, String prologue, String epilogue, TypeDispatchDefinition tdDef) {
            this.name = name;
            this.dispatchVars = dispatchVars;
            this.otherVars = otherVars;
            this.prologue = prologue;
            this.epilogue = epilogue;
            this.tdDef = tdDef;
        }
    }

    List<InstDefinition> instDefs = new LinkedList<InstDefinition>();
    // List<FuncDefinition> funcDefs = new LinkedList<FuncDefinition>();

    private void addDef(InstDefinition def) {
        if (def instanceof InstDefinition) {
            instDefs.add((InstDefinition) def);
        }
    }

    InstDefinition makeInstDefinitionFromParsedInst(DslParser.InstDef ins) {
        TypeDispatchDefinition td = makeDispatchDefFromInstDef(ins);
        return new InstDefinition(ins.id, ins.vars, null, ins.prologue, ins.epilogue, td);
    }

    public InstDefinition load(String fname) {
        DslParser dslp = new DslParser();
        DslParser.InstDef parsedInst = dslp.run(fname);
        InstDefinition instDef = makeInstDefinitionFromParsedInst(parsedInst);
        addDef(instDef);
        return instDef;
    }

    public String toString() {
        String ret = "InstDefinition:\n";
        for (InstDefinition def : instDefs) {
            ret += def.toString() + "\n";
        }
        return ret;
    }
}
