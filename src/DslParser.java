import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class DslParser {

    static class WhenClause {
        Condition condition;
        String body;
        WhenClause(Condition condition, String body) {
            this.condition = condition;
            this.body = body;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("WhenClause:(");
            sb.append("condition:(" + condition + ")");
            sb.append(", body:(" + body + ")");
            sb.append(")");
            return sb.toString();
        }
    }
    static class InstDef {
        String id;
        String[] vars;
        List<WhenClause> whenClauses = new LinkedList<WhenClause>();
        String prologue;
        String epilogue;
        InstDef(String id, String[] vars) {
            this.id = id;
            this.vars = vars;
        }
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("InstDef:(");
            sb.append("id:" + id + ",");
            sb.append("vars:(");
            if (vars.length == 1) {
                sb.append(vars[0]);
            } else if (vars.length == 2) {
                sb.append(vars[0] + "," + vars[1]);
            }
            sb.append(")");
            if (prologue != null) {
                sb.append("prologue:(" + prologue + ")");
            }
            if (epilogue != null) {
                sb.append("epilogue:(" + epilogue + ")");
            }
            sb.append("whenClauses:{");
            for (WhenClause w : whenClauses) {
                sb.append("(" + w.toString() + ")");
            }
            sb.append("}");
            sb.append(")");
            return sb.toString();
        }
    }

    class Condition {}
    enum ConditionalOp { AND, OR, NOT }
    class CompoundCondition extends Condition {
        ConditionalOp op;
        Condition cond1, cond2;
        CompoundCondition(ConditionalOp op, Condition cond1, Condition cond2) {
            this.op = op;
            this.cond1 = cond1;
            this.cond2 = cond2;
        }
        public String toString() {
            return "CompoundCondition(" + op + "," + cond1 + "," + cond2 + ")";
        }
    }
    class AtomCondition extends Condition {
        String varName;
        int varIdx;
        VMDataType t;
        AtomCondition(String varName, String tname) {
            this(varName, VMDataType.get(tname));
        }
        AtomCondition(String varName, VMDataType t) {
            this.varName = varName;
            this.t = t;
        }
        public String toString() {
            return "AtomCondition(" + varName + "(" + varIdx + "):" + t.getName() + ")";
        }
    }

    class Idx { int n; Idx(int n) { this.n = n; } }

    void checkToken(Token tk, TokenId tkId) throws Exception {
        checkToken(tk, tkId, null);
    }
    void checkToken(Token tk, TokenId tkId, String raw) throws Exception {
        if (tk != null && tk.id == tkId) {
            if (raw == null) return;
            else if (raw.equals(tk.raw)) return;
        }
        System.out.println("parse error!!!!!");
        throw new Exception();
    }

    private void convertConditionToDNFStep2(Condition condition) {
        if (condition instanceof AtomCondition) return;
        CompoundCondition cond = (CompoundCondition) condition;
        if (cond.op == ConditionalOp.NOT) return;
        else if (cond.op == ConditionalOp.AND) {
            boolean b = true;
            if (cond.cond1 instanceof CompoundCondition) {
                CompoundCondition cond1 = (CompoundCondition) cond.cond1;
                if (cond1.op == ConditionalOp.OR) {
                    Condition c = cond.cond2;
                    cond.cond1 = new CompoundCondition(ConditionalOp.AND, cond1.cond1, c);
                    cond.cond2 = new CompoundCondition(ConditionalOp.AND, cond1.cond2, c);
                    cond.op = ConditionalOp.OR;
                }
            }
            if (cond.cond2 instanceof CompoundCondition) {
                CompoundCondition cond2 = (CompoundCondition) cond.cond2;
                if (b && cond2.op == ConditionalOp.OR) {
                    Condition c = cond.cond1;
                    cond.cond1 = new CompoundCondition(ConditionalOp.AND, c, cond2.cond1);
                    cond.cond2 = new CompoundCondition(ConditionalOp.AND, c, cond2.cond2);
                    cond.op = ConditionalOp.OR;
                }
            }
        }
        convertConditionToDNFStep2(cond.cond1);
        convertConditionToDNFStep2(cond.cond2);
    }

    void convertConditionToDNF(Condition cond) {
        convertConditionToDNFStep2(cond);
    }
/*
    Condition parseCompoundCondition_(Token[] tks, Idx idx, ConditionalOp op) throws Exception {
        Idx idx1 = new Idx(idx.n);
        Condition c1, c2;
        c1 = parseParenthesisCondition(tks, idx1);
        if (c1 == null) {
            c1 = parseAtomCondition(tks, idx1);
            if (c1 == null) return null;
        }
        if (idx1.n >= tks.length) return null;
        if (tks[idx1.n].id == TokenId.COND_OP) {
            if (!(op == ConditionalOp.AND && tks[idx1.n].raw.equals("&&") ||
                  op == ConditionalOp.OR  && tks[idx1.n].raw.equals("||"))) {
                return null;
            }
        } else return null;
        idx1.n++;
        c2 = parseCondition(tks, idx1);
        if (c2 == null) return null;
        idx.n = idx1.n;
        return new CompoundCondition(op, c1, c2);
    }

    Condition parseCompoundCondition(Token[] tks, Idx idx) throws Exception {
        Idx idx1 = new Idx(idx.n);
        Condition c = null;
        c = parseCompoundCondition_(tks, idx1, ConditionalOp.AND);
        if (c != null) {
            idx.n = idx1.n;
            return c;
        }
        c = parseCompoundCondition_(tks, idx1, ConditionalOp.OR);
        if (c != null) {
            idx.n = idx1.n;
            return c;
        }
        return null;
    }

    Condition parseParenthesisCondition(Token[] tks, Idx idx) throws Exception {
        if (idx.n >= tks.length) return null;
        Idx idx1 = new Idx(idx.n);
        if (tks[idx1.n].id != TokenId.PARENTHESES) return null;
        if (!tks[idx1.n].raw.equals("(")) return null;
        idx1.n++;
        Condition c = parseCondition(tks, idx1);
        if (c == null) return null;
        if (idx1.n >= tks.length) return null;
        if (tks[idx1.n].id != TokenId.PARENTHESES) return null;
        if (!tks[idx1.n].raw.equals(")")) return null;
        idx1.n++;
        idx.n = idx1.n;
        return c;
    }

    Condition parseAtomCondition(Token[] tks, Idx idx) throws Exception {
        int i = idx.n;
        if (tks[i].id != TokenId.STRING) return null;
        if (tks[i+1].id != TokenId.COLON) return null;
        if (tks[i+2].id != TokenId.STRING) return null;

        DataType dt = DataType.get(tks[i+2].raw);
        if (dt == null) { System.out.println("dt is null"); throw new Exception(); }
        idx.n = i + 3;
        return new AtomCondition(tks[i].raw, tks[i+2].raw);
    }

    Condition parseCondition(Token[] tks, Idx idx) throws Exception {
        Idx idx1 = new Idx(idx.n);
        Condition c;
        c = parseCompoundCondition(tks, idx1);
        if (c != null) { idx.n = idx1.n; return c; }
        c = parseParenthesisCondition(tks, idx1);
        if (c != null) { idx.n = idx1.n; return c; }
        c = parseAtomCondition(tks, idx1);
        if (c != null) { idx.n = idx1.n; return c; }
        return null;
    }
    */

    Condition parseConditionAtom(Token[] tks, Idx idx) throws Exception {
        if (idx.n >= tks.length) return null;
        if (tks[idx.n].id == TokenId.PARENTHESES) {
            Idx idx1 = new Idx(idx.n);
            if (!tks[idx1.n].raw.equals("(")) return null;
            idx1.n++;
            Condition c = parseCondition(tks, idx1);
            if (c == null) return null;
            if (idx1.n >= tks.length) return null;
            if (tks[idx1.n].id != TokenId.PARENTHESES) return null;
            if (!tks[idx1.n].raw.equals(")")) return null;
            idx1.n++;
            idx.n = idx1.n;
            return c;
        } else {
            int i = idx.n;
            if (tks[i].id != TokenId.STRING) return null;
            if (tks[i+1].id != TokenId.COLON) return null;
            if (tks[i+2].id != TokenId.STRING) return null;
            VMDataType dt = VMDataType.get(tks[i+2].raw);
            if (dt == null) { System.out.println("dt is null"); throw new Exception(); }
            idx.n = i + 3;
            return new AtomCondition(tks[i].raw, tks[i+2].raw);
        }
    }

    CompoundCondition parseConditionTerm_(Token[] tks, Idx idx) throws Exception {
        if (idx.n >= tks.length) return null;
        if (tks[idx.n].id == TokenId.COND_OP && tks[idx.n].raw.equals("&&")) {
            Idx idx1 = new Idx(idx.n);
            idx1.n++;
            Condition r = parseConditionTerm(tks, idx1);
            if (r == null) {
                System.out.println("condition parse error");
                throw new Exception();
            }
            idx.n = idx1.n;
            return new CompoundCondition(ConditionalOp.AND, null, r);
        } else {
            return null;
        }
    }

    Condition parseConditionTerm(Token[] tks, Idx idx) throws Exception {
        if (idx.n >= tks.length) return null;
        Idx idx1 = new Idx(idx.n);
        Condition a = parseConditionAtom(tks, idx1);
        CompoundCondition c = parseConditionTerm_(tks, idx1);
        if (c == null) {
            idx.n = idx1.n;
            return a;
        }
        else {
            c.cond1 = a;
            idx.n = idx1.n;
            return c;
        }
    }

    Condition parseCondition_(Token[] tks, Idx idx) throws Exception {
        if (idx.n >= tks.length) return null;
        if (tks[idx.n].id == TokenId.COND_OP && tks[idx.n].raw.equals("||")) {
            Idx idx1 = new Idx(idx.n);
            idx1.n++;
            Condition r = parseCondition(tks, idx1);
            if (r == null) {
                System.out.println("condition parse error");
                throw new Exception();
            }
            idx.n = idx1.n;
            return new CompoundCondition(ConditionalOp.OR, null, r);
        } else {
            return null;
        }
    }

    Condition parseCondition(Token[] tks, Idx idx) throws Exception {
        if (idx.n >= tks.length) return null;
        Idx idx1 = new Idx(idx.n);
        Condition t, c;
        t = parseConditionTerm(tks, idx1);
        c = parseCondition_(tks, idx1);
        if (c == null) {
            idx.n = idx1.n;
            return t;
        } else {
            if (c instanceof CompoundCondition) {
                CompoundCondition cc = (CompoundCondition) c;
                cc.cond1 = t;
                idx.n = idx1.n;
                return cc;
            } else {
                System.out.println("condition parse error");
                throw new Exception();
            }
        }
    }

    void assignVarIdx(String[] vars, Condition cond) {
        if (cond instanceof AtomCondition) {
            AtomCondition c = (AtomCondition) cond;
            c.varIdx = -1;
            for (int i = 0; i < vars.length; i++) {
                if (c.varName.equals(vars[i]))
                    c.varIdx = i;
            }
            if (c.varIdx == -1) System.out.println("assignVarIdx: error");
        } else if (cond instanceof CompoundCondition) {
            CompoundCondition c = (CompoundCondition) cond;
            if (c.op == ConditionalOp.AND || c.op == ConditionalOp.OR) {
                assignVarIdx(vars, c.cond1);
                assignVarIdx(vars, c.cond2);
            } else {
                assignVarIdx(vars, c.cond1);
            }
        }
    }

    Condition parseConditionTokens(String[] vars, LinkedList<Token> tks) {
        LinkedList<Token> conditionTks = new LinkedList<Token>();
        while (tks.getFirst().id != TokenId.CPROGRAM) {
            conditionTks.add(tks.pollFirst());
        }
        Condition c = null;
        Idx idx = new Idx(0);
        try {
            c = parseCondition(conditionTks.toArray(new Token[conditionTks.size()]), idx);
        } catch (Exception e) {
            System.out.println("condition parse error!!!");
        }
        convertConditionToDNF(c);
        assignVarIdx(vars, c);
        return c;
    }

    InstDef parse(List<Token> _tks) {
        InstDef instDef = null;
        LinkedList<Token> tks = new LinkedList<Token>(_tks);
        try {
            checkToken(tks.pollFirst(), TokenId.KEY_INST);
            Token id = tks.pollFirst();
            checkToken(id, TokenId.STRING);
            checkToken(tks.pollFirst(), TokenId.PARENTHESES, "(");
            Token op1 = tks.pollFirst();
            Token t = tks.pollFirst();
            String[] vars = null;
            if (t.id == TokenId.CAMMA) {
                Token op2 = tks.pollFirst();
                checkToken(op1, TokenId.STRING);
                checkToken(op2, TokenId.STRING);
                checkToken(tks.pollFirst(), TokenId.PARENTHESES, ")");
                vars = new String[] { op1.raw, op2.raw };
            } else if (t.id == TokenId.PARENTHESES) {
                checkToken(op1, TokenId.STRING);
                checkToken(t, TokenId.PARENTHESES, ")");
                vars = new String[] { op1.raw };
            }
            instDef = new InstDef(id.raw, vars);
            while (!tks.isEmpty()) {
                Token tk = tks.pollFirst();
                switch (tk.id) {
                case KEY_WHEN: {
                    Condition c = parseConditionTokens(vars, tks);
                    Token cprog = tks.pollFirst();
                    checkToken(cprog, TokenId.CPROGRAM);
                    instDef.whenClauses.add(new WhenClause(c, cprog.raw));
                } break;
                case KEY_OTHERWISE: {
                    Token cprog = tks.pollFirst();
                    checkToken(cprog, TokenId.CPROGRAM);
                    instDef.whenClauses.add(new WhenClause(null, cprog.raw));
                } break;
                case KEY_PROLOGUE: {
                    Token cprog = tks.pollFirst();
                    checkToken(cprog, TokenId.CPROGRAM);
                    instDef.prologue = cprog.raw;
                } break;
                case KEY_EPILOGUE: {
                    Token cprog = tks.pollFirst();
                    checkToken(cprog, TokenId.CPROGRAM);
                    instDef.epilogue = cprog.raw;
                } break;
                case KEY_INST: break;
                default: {
                    System.out.println("parse error!!!!" + tk.raw);
                }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return instDef;
    }

    enum TokenId {
        COLON,
        CAMMA,
        PARENTHESES,
        KEY_INST,
        KEY_PROLOGUE,
        KEY_EPILOGUE,
        KEY_WHEN,
        KEY_OTHERWISE,
        STRING,
        COND_OP,
        CPROGRAM,
    }
    class Token {
        TokenId id;
        String raw;
        Token(TokenId id, String raw) {
            this.id = id;
            this.raw = raw;
        }
        public String toString() {
            return "id: " + id + ", " + "raw: " + raw;
        }
    }

    class Tokenizer {
        static final String tks = "\\\\\\\\.*$|,|:|&&|\\|\\||\\(|\\)|\\\\\\{(.|\\n)*?\\\\\\}|\\\\inst|\\\\prologue|\\\\epilogue|\\\\when|\\\\otherwise|\\w+";
        final Pattern ptn = Pattern.compile(tks, Pattern.MULTILINE);
        List<Token> tokenize(String all) {
            List<Token> tks = new LinkedList<Token>();
            Matcher m = ptn.matcher(all);
            while (m.find()) {
                Token tk = null;
                String s = m.group();
                if (s.matches("\\\\\\\\.*")) {
                } else if (s.equals(",")) {
                    tk = new Token(TokenId.CAMMA, s);
                } else if (s.equals(":")) {
                    tk = new Token(TokenId.COLON, s);
                } else if (s.matches("\\(|\\)")) {
                    tk = new Token(TokenId.PARENTHESES, s);
                } else if (s.matches("&&|\\|\\|")) {
                    tk = new Token(TokenId.COND_OP, s);
                } else if (s.equals("\\inst")) {
                    tk = new Token(TokenId.KEY_INST, s);
                } else if (s.equals("\\prologue")) {
                    tk = new Token(TokenId.KEY_PROLOGUE, s);
                } else if (s.equals("\\epilogue")) {
                    tk = new Token(TokenId.KEY_EPILOGUE, s);
                } else if (s.equals("\\when")) {
                    tk = new Token(TokenId.KEY_WHEN, s);
                } else if (s.equals("\\otherwise")) {
                    tk = new Token(TokenId.KEY_OTHERWISE, s);
                } else if (s.matches("\\w+")) {
                    tk = new Token(TokenId.STRING, s);
                } else if (s.matches("\\\\\\{(.|\n)*\\\\\\}")) {
                    tk = new Token(TokenId.CPROGRAM, s.substring(2, s.length() - 2));
                } else {
                    System.out.println("error: " + s);
                }
                if (tk != null) tks.add(tk);
            }
            return tks;
        }
    }
    String readAll(String fname) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(fname))) {
            String string = reader.readLine();
            while (string != null){
                sb.append(string + System.getProperty("line.separator"));
                string = reader.readLine();
            }
        } catch (IOException e) {
            System.out.println(e);
            return null;
        }
        return sb.toString();
    }

    public InstDef run(String fname) {
        String all = readAll(fname);
        List<Token> tks = new Tokenizer().tokenize(all);
        InstDef instDef = parse(tks);
        return instDef;
    }

    public static void main(String[] args) {
        DslParser dslp = new DslParser();
        InstDef instDef = dslp.run("idefs/add.idef");
        // System.out.println(instDef);
        /*
        String all = dslp.readAll("idefs/sample.idef");
        System.out.println(all);
        System.out.println("============");
        List<Token> tks = new Tokenizer().tokenize(all);
        InstDef instDef = dslp.parse(tks);
        System.out.println("##############");
        System.out.println(instDef.toString());*/
    }
}
