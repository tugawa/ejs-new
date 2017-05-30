import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WhenClause {
    String condition;
    String body;
    WhenClause(String condition, String body) {
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
class InstDef {
    String id;
    String[] vars;
    List<WhenClause> whenClauses = new LinkedList<WhenClause>();
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
}
class AtomCondition extends Condition {
    int varIdx = -1;
    DataType t;
    AtomCondition(int varIdx, String tname) {
        this(varIdx, DataType.get(tname));
    }
    AtomCondition(int varIdx, DataType t) {
        this.varIdx = varIdx;
        this.t = t;
    }
}

class Parser {

    void checkToken(Token tk, Token.TokenId tkId) throws Exception {
        checkToken(tk, tkId, null);
    }
    void checkToken(Token tk, Token.TokenId tkId, String raw) throws Exception {
        if (tk != null && tk.id == tkId) {
            if (raw == null) return;
            else if (raw.equals(tk.raw)) return;
        }
        System.out.println("parse error!!!!!");
        throw new Exception();
    }

    Condition parseCondition(LinkedList<Token> tks) {

    }
    void parseConditionTokens(LinkedList<Token> tks) {
        LinkedList<Token> conditionTks = new LinkedList<Token>();
        while (tks.getFirst().id != Token.TokenId.CPROGRAM) {
            conditionTks.add(tks.pollFirst());
        }
        parseCondition(conditionTks);
    }

    InstDef parse(List<Token> _tks) {
        InstDef instDef = null;
        LinkedList<Token> tks = new LinkedList<Token>(_tks);
        try {
            checkToken(tks.pollFirst(), Token.TokenId.KEY_INST);
            Token id = tks.pollFirst();
            checkToken(id, Token.TokenId.STRING);
            checkToken(tks.pollFirst(), Token.TokenId.PARENTHESES, "(");
            Token op1 = tks.pollFirst();
            Token t = tks.pollFirst();
            String[] vars = null;
            if (t.id == Token.TokenId.CAMMA) {
                Token op2 = tks.pollFirst();
                checkToken(op1, Token.TokenId.OPERAND);
                checkToken(op2, Token.TokenId.OPERAND);
                checkToken(tks.pollFirst(), Token.TokenId.PARENTHESES, ")");
                vars = new String[] { op1.raw, op2.raw };
            } else if (t.id == Token.TokenId.PARENTHESES) {
                checkToken(t, Token.TokenId.PARENTHESES, ")");
                vars = new String[] { op1.raw };
            }
            instDef = new InstDef(id.raw, vars);
            while (!tks.isEmpty()) {
                Token tk = tks.pollFirst();
                switch (tk.id) {
                case KEY_WHEN: {
                    parseCondition(tks);
                    Token cprog = tks.pollFirst();
                    checkToken(cprog, Token.TokenId.CPROGRAM);
                    instDef.whenClauses.add(new WhenClause(null, cprog.raw));
                } break;
                case KEY_OTHERWISE: {
                    Token cprog = tks.pollFirst();
                    checkToken(cprog, Token.TokenId.CPROGRAM);
                    instDef.whenClauses.add(new WhenClause(null, cprog.raw));
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
}


class Token {
    enum TokenId {
        COLON,
        CAMMA,
        PARENTHESES,
        KEY_INST,
        KEY_WHEN,
        KEY_OTHERWISE,
        OPERAND,
        STRING,
        COND_OP,
        CPROGRAM,
    }
    TokenId id;
    String raw;
    Token(TokenId id, String raw) {
        this.id = id;
        this.raw = raw;
    }
}


class Tokenizer {
    static final String tks = "\\\\\\\\.*$|,|:|&&|\\|\\||\\(|\\)|\\\\\\{(.|\\n)*\\\\\\}|\\\\inst|\\\\when|\\\\otherwise|\\$\\w+|\\w+";
    static final Pattern ptn = Pattern.compile(tks, Pattern.MULTILINE);
    List<Token> tokenize(String all) {
        List<Token> tks = new LinkedList<Token>();
        Matcher m = ptn.matcher(all);
        while (m.find()) {
            Token tk = null;
            String s = m.group();
            if (s.matches("\\\\\\\\.*")) {
                System.out.println("COMMENT: " + s);
            } else if (s.equals(",")) {
                System.out.println("CAMMA");
                tk = new Token(Token.TokenId.CAMMA, s);
            } else if (s.equals(":")) {
                System.out.println("COLON");
                tk = new Token(Token.TokenId.COLON, s);
            } else if (s.matches("\\(|\\)")) {
                System.out.println("PARENTHESES: " + s);
                tk = new Token(Token.TokenId.PARENTHESES, s);
            } else if (s.matches("&&|\\|\\|")) {
                System.out.println("CONDITIONAL_OP: " + s);
                tk = new Token(Token.TokenId.COND_OP, s);
            } else if (s.equals("\\inst")) {
                System.out.println("KEY: " + s);
                tk = new Token(Token.TokenId.KEY_INST, s);
            } else if (s.equals("\\when")) {
                System.out.println("KEY: " + s);
                tk = new Token(Token.TokenId.KEY_WHEN, s);
            } else if (s.equals("\\otherwise")) {
                System.out.println("KEY: " + s);
                tk = new Token(Token.TokenId.KEY_OTHERWISE, s);
            } else if (s.matches("\\$\\w+")) {
                System.out.println("OPERAND: " + s);
                tk = new Token(Token.TokenId.OPERAND, s);
            } else if (s.matches("\\w+")) {
                System.out.println("STRING: " + s);
                tk = new Token(Token.TokenId.STRING, s);
            } else if (s.matches("\\\\\\{(.|\n)*\\\\\\}")) {
                System.out.println("C-PROGRAM: " + s.substring(2, s.length() - 2));
                tk = new Token(Token.TokenId.CPROGRAM, s);
            } else {
                System.out.println("error: " + s);
            }
            if (tk != null) tks.add(tk);
        }
        return tks;
    }
}


public class DslParser {

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

    public static void main(String[] args) {
        DslParser dslp = new DslParser();
        String all = dslp.readAll("idefs/sample.idef");
        System.out.println(all);
        System.out.println("============");
        List<Token> tks = new Tokenizer().tokenize(all);
        Parser parser = new Parser();
        InstDef instDef = parser.parse(tks);
        System.out.println("##############");
        System.out.println(instDef.toString());
    }

}
