import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class JSTypePair {
    String left, right;
    JSTypePair(String left, String right) {
        this.left = left;
        this.right = right;
    }
    public String toString() {
        return "[" + left + "*" + right + "]";
    }
}


class TypeDispatchDefinition {

    class Condition {}
    enum ConditionalOperator { OR, AND, NOT }
    class CompoundCondition extends Condition {
        ConditionalOperator op;
        Condition cond1, cond2;
        CompoundCondition(ConditionalOperator op, Condition c1, Condition c2) {
            this.op = op;
            this.cond1 = c1;
            this.cond2 = c2;
        }
        public String toString() {
            return "(" + op + "," + cond1 + "," + cond2 + ")";
        }
    }
    class AtomCondition extends Condition {
        String varName, jsType;
        AtomCondition(String varName, String jsType) {
            this.varName = varName;
            this.jsType = jsType;
        }
        public String toString() {
            return "(" + varName + ":" + jsType + ")";
        }
    }

    int numOfVars;  // number of variables which will be checked jsType ( 2 or less )
    String[] vnames;  // variables which will be checked jsType

 // <<jsType,jsType>, c code lines>
    Map<Set<JSTypePair>, List<String>> actionsMap;
    List<Pair<JSTypePair,List<String>>> actionList;
    List<Pair<JSTypePair,List<String>>> actionListWildcardLeft;
    List<Pair<JSTypePair,List<String>>> actionListWildcardRight;
    List<String> otherwiseCProgram;

    TypeDispatchDefinition(String[] vnames) {
        numOfVars = vnames.length;
        this.vnames = vnames;

        actionList = new LinkedList<Pair<JSTypePair,List<String>>>();
        if (this.numOfVars == 2) {
            actionListWildcardLeft  = new LinkedList<Pair<JSTypePair,List<String>>>();
            actionListWildcardRight = new LinkedList<Pair<JSTypePair,List<String>>>();
        }
    }

    private int getVarIdx(String name) {
        for (int i = 0; i < vnames.length; i++) {
            if (name.equals(vnames[i])) {
                return i;
            }
        }
        return -1;
    }


    List<JSTypePair> tmpConditions;
    List<String> cProgram;
    // String rawCondition;

    void read(String line) {
        if (line.matches("^\\\\when .*")) {
            List<JSTypePair> conditions = conditionStrToJSTypePair(line.substring(6));
            if (cProgram != null) endWhenScope();
            nextWhenScope(conditions);
        } else if (line.matches("^\\\\otherwise\\s*")) {
            if (cProgram != null) endWhenScope();
            nextWhenScope(null);
        } else {
            cProgram.add(line);
        }
    }

    void end() {
        endWhenScope();
        // actionWildcardLeft/Right
        actionsMap = new HashMap<Set<JSTypePair>,List<String>>();
        List<Pair<JSTypePair,List<String>>> tmpActionList = new LinkedList<Pair<JSTypePair,List<String>>>(actionList);
        if (vnames.length == 2) {
            for (Pair<JSTypePair,List<String>> e : this.actionListWildcardLeft) {
                List<Pair<JSTypePair,List<String>>> tmpActionList1 = new LinkedList<Pair<JSTypePair,List<String>>>();
                tmpActionList1.addAll(this.actionList);
                tmpActionList1.addAll(this.actionListWildcardRight);
                List<String> rightSames = getDefined(tmpActionList1, e.first(), 1);
                List<String> datatypeStrs = getSubDatatype(rightSames);
                for (String dts : datatypeStrs) {
                    JSTypePair jtp = new JSTypePair(dts, e.first().right);
                    Pair<JSTypePair,List<String>> pair = new Pair<JSTypePair,List<String>>(jtp, e.second());
                    tmpActionList.add(pair);
                }
            }
            for (Pair<JSTypePair,List<String>> e : this.actionListWildcardRight) {
                List<Pair<JSTypePair,List<String>>> tmpActionList1 = new LinkedList<Pair<JSTypePair,List<String>>>();
                tmpActionList1.addAll(this.actionList);
                tmpActionList1.addAll(this.actionListWildcardLeft);
                List<String> leftSames = getDefined(tmpActionList1, e.first(), 0);
                List<String> datatypeStrs = getSubDatatype(leftSames);
                for (String dts : datatypeStrs) {
                    JSTypePair jtp = new JSTypePair(e.first().left, dts);
                    Pair<JSTypePair,List<String>> pair = new Pair<JSTypePair,List<String>>(jtp, e.second());
                    tmpActionList.add(pair);
                }
            }
        }
        if (otherwiseCProgram != null) {
            List<Pair<JSTypePair,List<String>>> otherwiseList = new LinkedList<Pair<JSTypePair,List<String>>>();
            for (DataType leftDt : DataType.all()) {
                for (DataType rightDt : DataType.all()) {
                    boolean b = false;
                    for (Pair<JSTypePair,List<String>> e : tmpActionList) {
                        JSTypePair p = e.first();
                        if (leftDt.name.equals(p.left) && rightDt.name.equals(p.right)) {
                            // tmpActionList.add(index, element);
                            b = true;
                            break;
                        }
                    }
                    if (!b) {
                        Pair<JSTypePair,List<String>> o = new Pair<JSTypePair,List<String>>(
                                new JSTypePair(leftDt.name, rightDt.name), otherwiseCProgram);
                        otherwiseList.add(o);
                    }
                }
            }
            tmpActionList.addAll(otherwiseList);
        }

        while (!tmpActionList.isEmpty()) {
            List<JSTypePair> sameActions = new LinkedList<JSTypePair>();
            List<String> cProgram = null;
            List<Pair<JSTypePair,List<String>>> removeList = new LinkedList<Pair<JSTypePair,List<String>>>();
            for (Pair<JSTypePair,List<String>> e : tmpActionList) {
                if (cProgram == null) {
                    cProgram = e.second();
                    sameActions.add(e.first());
                    removeList.add(e);
                } else if (cProgram == e.second()) {
                    sameActions.add(e.first());
                    removeList.add(e);
                }
            }
            Set<JSTypePair> set = new HashSet<JSTypePair>();
            set.addAll(sameActions);
            actionsMap.put(set, cProgram);
            tmpActionList.removeAll(removeList);
        }


    }

    private List<String> getSubDatatype(List<String> typenames) {
        List<String> ret = new LinkedList<String>();
        for (DataType dt : DataType.all()) {
            ret.add(dt.name);
        }
        ret.removeAll(typenames);
        return ret;
    }

    // if check left, c -> 0     if check right, c -> 1
    private List<String> getDefined(List<Pair<JSTypePair,List<String>>> actionList, JSTypePair jtp, int c) {
        List<String> ret = new LinkedList<String>();
        for (Pair<JSTypePair,List<String>> e : actionList) {
            if (c == 0) {
                if (e.first().left == null) {
                    ret.add(e.first().right);
                } else if (jtp.left.equals(e.first().left)) {
                    ret.add(e.first().right);
                }
            } else if (c == 1) {
                if (e.first().right == null) {
                    ret.add(e.first().left);
                } else if (jtp.right.equals(e.first().right)) {
                    ret.add(e.first().left);
                }
            }
        }
        return ret;
    }

    private void endWhenScope() {
        List<JSTypePair> conditions = tmpConditions;
        tmpConditions = null;
        if (conditions == null) {
            otherwiseCProgram = cProgram;
            return;
        }
        if (numOfVars == 1) {
            for (JSTypePair jtp : conditions) {
                for (Pair<JSTypePair,List<String>> act : actionList) {
                    if (jtp.left.equals(act.first().left)) {
                        // throw new Exception();
                        System.out.println("error");
                    }
                }
                actionList.add(new Pair<JSTypePair,List<String>>(jtp, cProgram));
            }
        } else if (numOfVars == 2) {
            for (JSTypePair jtp : conditions) {
                if (jtp.left == null) {
                    for (Pair<JSTypePair,List<String>> act : actionListWildcardLeft) {
                        if (jtp.right.equals(act.first().right)) {
                            // throw new Exception();
                            System.out.println("error");
                        }
                    }
                    actionListWildcardLeft.add(new Pair<JSTypePair,List<String>>(jtp, cProgram));
                } else if (jtp.right == null) {
                    for (Pair<JSTypePair,List<String>> act : actionListWildcardRight) {
                        if (jtp.left.equals(act.first().left)) {
                            // throw new Exception();
                            System.out.println("error");
                        }
                    }
                    actionListWildcardRight.add(new Pair<JSTypePair,List<String>>(jtp, cProgram));
                } else {
                    for (Pair<JSTypePair,List<String>> act : actionList) {
                        if (jtp.left.equals(act.first().left) && jtp.right.equals(act.first().right)) {
                            // throw new Exception();
                            System.out.println("error");
                        }
                    }
                    actionList.add(new Pair<JSTypePair,List<String>>(jtp, cProgram));
                }
            }
        }
    }

    private void nextWhenScope(List<JSTypePair> conditions) {
        cProgram = new LinkedList<String>();
        tmpConditions = conditions;
    }

    private JSTypePair seekOtherwise(List<JSTypePair> conditions) {
        for (JSTypePair jtp : conditions) {
            if (jtp.left == null && jtp.right == null)
                return jtp;
        }
        return null;
    }

    private boolean eatSpaces(final String rawCond, int[] idx) {
        for (; idx[0] < rawCond.length(); idx[0]++) {
            if (rawCond.charAt(idx[0]) != ' ') break;
        }
        return idx[0] >= rawCond.length();
    }

    private Condition parseParenthesizedCondition(final String rawCond, int[] idx, boolean last) {
        // System.out.println("parseParenthesizedCondition: " + rawCond + " # " + idx[0]);
        int tmp = idx[0];
        eatSpaces(rawCond, idx);
        if (idx[0] + 1 >=  rawCond.length()) return null;
        if (rawCond.charAt(idx[0]) != '(')
            return null;
        idx[0]++;
        Condition ret = parseCondition(rawCond, idx, false);
        if (eatSpaces(rawCond, idx)) {
            idx[0] = tmp;
            return null;
        }
        if (rawCond.charAt(idx[0]) != ')') {
            idx[0] = tmp;
            return null;
        }
        idx[0]++;
        eatSpaces(rawCond, idx);
        if (last && idx[0] < rawCond.length()) {
            idx[0] = tmp;
            return null;
        }
        // System.out.println("Parenthesized ret is " + ret);
        return ret;
    }

    private Condition parseAtomCondition(final String rawCond, int[] idx, boolean last) {
        // System.out.println("parseAtomCondition: " + rawCond + " # " + idx[0]);
        eatSpaces(rawCond, idx);
        final Pattern ptnAtom = Pattern.compile("^(\\$\\w+)\\s*:\\s*(\\w+)\\s*");
        Matcher m = ptnAtom.matcher(rawCond.substring(idx[0]));
        if (!m.find()) {
            return null;
        }
        Condition ret = new AtomCondition(m.group(1), m.group(2));
        int tmp = idx[0];
        idx[0] = idx[0] + m.end();
        eatSpaces(rawCond, idx);
        // System.out.println("Atom ret is " + ret + " " + idx[0]);
        return ret;
    }

    private Condition parseCompoundConditionNOT(final String rawCond, int[] idx, boolean last) {
        // System.out.println("parseCompoundConditionNOT: " + rawCond + " # " + idx[0]);
        eatSpaces(rawCond, idx);
        int tmp = idx[0];
        if (idx[0] + 1 >= rawCond.length()) return null;
        if (rawCond.charAt(idx[0]) != '!') return null;
        idx[0]++;
        Condition cond1 = parseCondition(rawCond, idx, last);
        eatSpaces(rawCond, idx);
        return new CompoundCondition(ConditionalOperator.NOT, cond1, null);
    }

    private Condition parseCompoundConditionANDOR(final String rawCond, int[] idx, boolean last, ConditionalOperator op) {
        // System.out.println("parseCompoundCondition" + op + ": " + rawCond + " # " + idx[0]);
        eatSpaces(rawCond, idx);
        int tmp = idx[0];
        Condition left = parseParenthesizedCondition(rawCond, idx, false);
        if (left == null) {
            idx[0] = tmp;
            left = parseAtomCondition(rawCond, idx, false);
        }
        eatSpaces(rawCond, idx);
        if (left != null && idx[0] + 1 < rawCond.length()) {
            if (op == ConditionalOperator.AND) {
                if (!(rawCond.charAt(idx[0]) == '&' && rawCond.charAt(idx[0] + 1) == '&')) {
                    idx[0] = tmp;
                    return null;
                }
            } else if (op == ConditionalOperator.OR) {
                if (!(rawCond.charAt(idx[0]) == '|' && rawCond.charAt(idx[0] + 1) == '|')) {
                    idx[0] = tmp;
                    return null;
                }
            }
            idx[0] += 2;
        } else {
            idx[0] = tmp;
            return null;
        }
        eatSpaces(rawCond, idx);
        Condition right = parseCondition(rawCond, idx, last);
        if (right == null) {
            idx[0] = tmp;
            return null;
        }
        // System.out.println("CompoundCondition: " + op +", " + left + ", " + right);
        return new CompoundCondition(op, left, right);
    }

    private Condition parseCompoundCondition(final String rawCond, int[] idx, boolean last) {
        // System.out.println("parseCompoundCondition: " + rawCond + " # " + idx[0]);
        int tmp = idx[0];
        Condition cond = parseCompoundConditionNOT(rawCond, idx, last);
        if (cond != null)
            return cond;
        idx[0] = tmp;
        cond = parseCompoundConditionANDOR(rawCond, idx, last, ConditionalOperator.AND);
        if (cond != null) {
            return cond;
        }
        idx[0] = tmp;
        cond = parseCompoundConditionANDOR(rawCond, idx, last, ConditionalOperator.OR);
        if (cond != null)
            return cond;
        return null;
    }

    private Condition parseCondition(final String rawCond, int[] idx, boolean last) {
        // System.out.println("parseCondition: " + rawCond + " # " + idx[0]);
        int tmp = idx[0];
        Condition cond = null;
        cond = parseCompoundCondition(rawCond, idx, last);
        if (cond != null) return cond;
        idx[0] = tmp;
        cond = parseParenthesizedCondition(rawCond, idx, last);
        if (cond != null) return cond;
        idx[0] = tmp;
        cond = parseAtomCondition(rawCond, idx, last);
        if (cond != null) return cond;
        idx[0] = tmp;
        return null;
    }

    private void convertConditionToDNFStep1(Condition condition) {
        if (condition instanceof AtomCondition) return;
        // condition instance of CompoundCondition
        CompoundCondition cond = (CompoundCondition) condition;
        if (cond.op == ConditionalOperator.NOT) {
            convertConditionToDNFStep1(cond.cond1);
            if (cond.cond1 instanceof AtomCondition) return;
            CompoundCondition cond1 = (CompoundCondition) cond.cond1;
            if (cond1.op == ConditionalOperator.NOT) {
                cond.cond1 = cond1.cond1;
            } else if (cond1.op == ConditionalOperator.AND) {
                cond.cond1 = new CompoundCondition(ConditionalOperator.NOT, cond1.cond1, null);
                cond.cond2 = new CompoundCondition(ConditionalOperator.NOT, cond1.cond1, null);
                cond.op = ConditionalOperator.OR;
            } else if (cond1.op == ConditionalOperator.OR) {
                cond.cond1 = new CompoundCondition(ConditionalOperator.NOT, cond1.cond1, null);
                cond.cond2 = new CompoundCondition(ConditionalOperator.NOT, cond1.cond1, null);
                cond.op = ConditionalOperator.AND;
            }
        } else {
            convertConditionToDNFStep1(cond.cond1);
            convertConditionToDNFStep1(cond.cond2);
        }
    }

    private void convertConditionToDNFStep2(Condition condition) {
        if (condition instanceof AtomCondition) return;
        CompoundCondition cond = (CompoundCondition) condition;
        if (cond.op == ConditionalOperator.NOT) return;
        else if (cond.op == ConditionalOperator.AND) {
            boolean b = true;
            if (cond.cond1 instanceof CompoundCondition) {
                CompoundCondition cond1 = (CompoundCondition) cond.cond1;
                if (cond1.op == ConditionalOperator.OR) {
                    Condition c = cond.cond2;
                    cond.cond1 = new CompoundCondition(ConditionalOperator.AND, cond1.cond1, c);
                    cond.cond2 = new CompoundCondition(ConditionalOperator.AND, cond1.cond2, c);
                    cond.op = ConditionalOperator.OR;
                }
            }
            if (cond.cond2 instanceof CompoundCondition) {
                CompoundCondition cond2 = (CompoundCondition) cond.cond2;
                if (b && cond2.op == ConditionalOperator.OR) {
                    Condition c = cond.cond1;
                    cond.cond1 = new CompoundCondition(ConditionalOperator.AND, c, cond2.cond1);
                    cond.cond2 = new CompoundCondition(ConditionalOperator.AND, c, cond2.cond2);
                    cond.op = ConditionalOperator.OR;
                }
            }
        }
        convertConditionToDNFStep2(cond.cond1);
        convertConditionToDNFStep2(cond.cond2);
    }

    private void convertConditionToDNF(Condition condition) {
        convertConditionToDNFStep1(condition);
        convertConditionToDNFStep2(condition);
    }

    private List<JSTypePair> conditionToListOfJSTypePair(Condition condition) {
        List<JSTypePair> ret = new LinkedList<JSTypePair>();
        if (condition instanceof CompoundCondition) {
            CompoundCondition c = (CompoundCondition) condition;
            if (c.op == ConditionalOperator.AND) {
                AtomCondition atom1 = (AtomCondition) c.cond1;
                AtomCondition atom2 = (AtomCondition) c.cond2;
                if (atom1.varName.equals(atom2.varName)) { /* error */ }
                String[] t = new String[2];
                int idx1, idx2;
                if ((idx1 = getVarIdx(atom1.varName)) == -1) { System.out.println("error"); }
                if ((idx2 = getVarIdx(atom2.varName)) == -1) { System.out.println("error"); }
                t[idx1] = atom1.jsType;
                t[idx2] = atom2.jsType;
                ret.add(new JSTypePair(t[0], t[1]));
            } else if (c.op == ConditionalOperator.OR) {
                ret.addAll(conditionToListOfJSTypePair(c.cond1));
                ret.addAll(conditionToListOfJSTypePair(c.cond2));
            }
        } else if (condition instanceof AtomCondition) {
            AtomCondition atom = (AtomCondition) condition;
            int idx = this.getVarIdx(atom.varName);
            if (idx == 0) {
                ret.add(new JSTypePair(atom.jsType, null));
            } else if (idx == 1) {
                ret.add(new JSTypePair(null, atom.jsType));
            }
        }
        return ret;
    }

    private List<JSTypePair> conditionStrToJSTypePair(String rawCondition) {
        List<JSTypePair> jtps = new LinkedList<JSTypePair>();
        // Condition c = makeParseTree(rawCondition, new int[1]);
        System.out.println(rawCondition);
        Condition c = parseCondition(rawCondition, new int[1], true);
        System.out.println("raw: " + c);
        convertConditionToDNF(c);
        System.out.println("dnf: " + c);
        return conditionToListOfJSTypePair(c);
    }

    public String toString() {
        String ret = "";
        for (Map.Entry<Set<JSTypePair>, List<String>> pair : actionsMap.entrySet()) {
            Set<JSTypePair> set = pair.getKey();
            ret += "(";
            for (JSTypePair e : pair.getKey()) {
                ret += (", " + e);
            }
            ret += ") -> {\n";
            for (String line : pair.getValue()) {
                ret += line + "\n";
            }
            ret += "}\n";
        }
        return ret;
    }

    public Set<Plan.Rule> toRules() {
        Set<Plan.Rule> rules = new HashSet<Plan.Rule>();
        for (Map.Entry<Set<JSTypePair>, List<String>> e : this.actionsMap.entrySet()) {
            List<Plan.Condition> conditions = new LinkedList<Plan.Condition>();
            for (JSTypePair jtp : e.getKey()) {
                if (vnames.length == 1)
                    conditions.add(new Plan.Condition(jtp.left));
                else if (vnames.length == 2)
                    conditions.add(new Plan.Condition(jtp.left, jtp.right));
            }
            String action = "";
            for (String s : e.getValue()) {
                action += s;
            }
            rules.add(new Plan.Rule(action, conditions));
        }
        return rules;
    }
}


interface Definition {
    void read(String line);
    void start();
    void end();
    public Set<Plan.Rule> toRules();
}

class InstDefinition implements Definition {
    String instName;
    String[] dispatchVars, otherVars;
    TypeDispatchDefinition tdDef;
    InstDefinition(String instName, String[] dispatchVars, String[] otherVars, TypeDispatchDefinition tdDef) {
        this.instName = instName;
        this.dispatchVars = dispatchVars;
        this.otherVars = otherVars;
        this.tdDef = tdDef;
    }
    InstDefinition(String instDefLine) {
        Pattern ptnInst = Pattern.compile("^\\s*(\\w+)(\\s*\\[\\s*((\\$?\\w+)\\s*(,\\s*(\\$?\\w+)\\s*)?)?\\])?(\\s*\\((\\s*(\\$\\w+)(\\s*,\\s*(\\$\\w+)(\\s*,\\s*((\\$\\w+)))?)?)?\\s*\\))?\\s*$");
        Matcher m = ptnInst.matcher(instDefLine);
        if (m.find()) {
            // \inst INST_NAME [ x1 , x2 ] ( y1 , y2 , y3 )
            // (2, INST_NAME)  (4, x1)  (6, x2)  (9, y1)  (11, y2)  (13, y3)
            String instName = m.group(1);
            String x1 = m.group(4), x2 = m.group(6);
            String y1 = m.group(9), y2 = m.group(11), y3 = m.group(13);
            String[] xs, ys;
            if (x1 != null) {
                if (x2 != null) {
                    xs = new String[2];
                    xs[1] = x2;
                } else xs = new String[1];
                xs[0] = x1;
            } else xs = new String[0];
            if (y1 != null) {
                if (y2 != null) {
                    if (y3 != null) {
                        ys = new String[3];
                        ys[2] = y3;
                    } else ys = new String[2];
                    ys[1] = y2;
                } else ys = new String[1];
                ys[0] = y1;
            } else ys = new String[0];
            this.instName = instName;
            this.dispatchVars = xs;
            this.otherVars = ys;
            // this.tdDef = new TypeDispatchDefinition(this.dispatchVars);
        } else {
            // error
        }
    }
    public void read(String line) {
        tdDef.read(line);
    }
    public void start() {
        tdDef = new TypeDispatchDefinition(dispatchVars);
    }
    public void end() {
        tdDef.end();
    }
    public Set<Plan.Rule> toRules() {
        return tdDef.toRules();
    }
    public String toString() {
        String ret = "Instruction name: " + instName + "\n";
        ret += "dispatch vars:";
        for (String v : dispatchVars) {
            ret += " " + v;
        }
        ret += "\nother vars: ";
        for (String v : otherVars) {
            ret += " " + v;
        }
        ret += "\n";
        ret += tdDef.toString();
        ret += "\n";
        return ret;
    }
}

class FuncDefinition {
    List cSrcLines = new LinkedList<String>();
}


public class ProcDefinition {

    List<Definition> defs = new LinkedList<Definition>();

    void load(String fname) throws FileNotFoundException {
        Scanner sc = new Scanner(new FileInputStream(fname));
        Definition def = null;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.matches("^\\\\inst .+")) {
                if (def != null) {
                    def.end();
                    defs.add(def);
                }
                def = new InstDefinition(line.substring(6));
                def.start();
            } else if (line.matches("^\\\\func ")) {
                // func
            } else {
                def.read(line);
            }
        }
        if (def != null) {
            def.end();
            defs.add(def);
        }
    }

    public String toString() {
        String ret = "";
        for (Definition def : defs) {
            ret += def.toString() + "\n";
        }
        return ret;
    }

    public static void main(String[] args) throws FileNotFoundException {
        TypeDefinition td = new TypeDefinition();
        td.load("datatype/embstr.dtdef");
        System.out.println(td);
        ProcDefinition procDef = new ProcDefinition();
        procDef.load("inst.def");
        System.out.println(procDef);
        InstDefinition instDef = (InstDefinition) procDef.defs.get(0);
        Plan p = new Plan(instDef.dispatchVars.length, instDef.toRules());
        new TagPairSynthesiser().twoOperand(td, p.rules);
    }
}
