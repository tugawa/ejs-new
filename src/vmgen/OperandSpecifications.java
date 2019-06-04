/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmgen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vmgen.type.VMDataType;

public class OperandSpecifications {
    static class OperandSpecificationRecord {
        String insnName;
        String[] operandTypes;
        enum Behaviour {
            ACCEPT,
            UNSPECIFIED,
            ERROR
        };
        Behaviour behaviour;
        OperandSpecificationRecord(String insnName, String[] operandTypes, Behaviour behaviour) {
            this.insnName = insnName;
            this.operandTypes = operandTypes;
            this.behaviour = behaviour;
        }
    }
    List<OperandSpecificationRecord> spec = new ArrayList<OperandSpecificationRecord>();

    void load(Scanner sc) {
        final String P_SYMBOL = "[a-zA-Z_]+";
        final String P_OPERANDS = "\\(\\s*([^)]*)\\s*\\)";
        final String P_BEHAVIOUR = "accept|error|unspecified";
        final Pattern splitter = Pattern.compile("("+P_SYMBOL+")\\s*"+P_OPERANDS+"\\s*("+P_BEHAVIOUR+")\\s*$");
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("#"))
                continue;
            Matcher matcher = splitter.matcher(line);
            if (matcher.matches()) {
                MatchResult m = matcher.toMatchResult();
                String insnName = m.group(1);
                String[] operandTypes = null;

                if (!m.group(2).equals("")) {
                    int n = 0;
                    String[] allOps = m.group(2).split("\\s*,\\s*");
                    for (String s: allOps)
                        if (!s.equals("-"))
                            n++;
                    operandTypes = new String[n];
                    int i = 0;
                    for (String s: allOps)
                        if (!s.equals("-"))
                            operandTypes[i++] = s;
                } else {
                    operandTypes = new String[0];
                }

                OperandSpecificationRecord.Behaviour behaviour;
                if (m.group(3).equals("accept"))
                    behaviour = OperandSpecificationRecord.Behaviour.ACCEPT;
                else if (m.group(3).equals("error"))
                    behaviour = OperandSpecificationRecord.Behaviour.ERROR;
                else if (m.group(3).equals("unspecified"))
                    behaviour = OperandSpecificationRecord.Behaviour.UNSPECIFIED;
                else
                    throw new Error("operand specification syntax error:"+ m.group());
                OperandSpecificationRecord r = new OperandSpecificationRecord(insnName, operandTypes, behaviour);
                spec.add(r);
            } else
                throw new Error("operand specification syntax error:"+ line);
        }
    }

    public void load(String file) throws FileNotFoundException {
        Scanner sc = new Scanner(new FileInputStream(file));
        try {
            load(sc);
        } finally {
            sc.close();
        }
    }

    boolean matchOperandTypes(String[] specTypes, VMDataType[] types, String insnName) {
        if (specTypes.length != types.length)
            throw new Error("number of operands mismatch: "+insnName+" insndef:"+types.length+", opspec: "+specTypes.length);
        for (int i = 0; i < specTypes.length; i++) {
            if (specTypes[i].equals("_"))
                continue; // next operand
            if (specTypes[i].startsWith("!") &&
                    specTypes[i].substring(1).equals("object") &&
                    !types[i].isObject())
                continue; // next operand;
            if (specTypes[i].startsWith("!") &&
                    !specTypes[i].substring(1).equals(types[i].getName()))
                continue; // next operand;
            if (specTypes[i].equals("object") && types[i].isObject())
                continue; // next operand
            if (specTypes[i].equals(types[i].getName()))
                continue; // next operand
            return false;
        }
        return true;
    }

    OperandSpecificationRecord findSpecificationRecord(String insnName, VMDataType[] types) {
        for (OperandSpecificationRecord rec: spec) {
            if (insnName.equals(rec.insnName) &&
                    matchOperandTypes(rec.operandTypes, types, insnName))
                return rec;
        }
        /* construct error message */
        StringBuilder sb = new StringBuilder();
        sb.append("unexhaustive type specification for : ");
        sb.append(insnName);
        sb.append("(");
        for (int i = 0; i < types.length; i++) {
            if (i >= 1)
                sb.append(",");
            sb.append(types[i].getName());
        }
        sb.append(")");
        throw new Error(sb.toString());
    }

    public Set<VMDataType[]> getOperands(String insnName, int arity, OperandSpecificationRecord.Behaviour behaviour) {
        Set<VMDataType[]> typess = new HashSet<VMDataType[]>();
        int total = 1;
        for (int i = 0; i < arity; i++)
            total *= VMDataType.all().size();
        for (int i = 0; i < total; i++) {
            VMDataType[] types = new VMDataType[arity];
            int a = i;
            for (int j = 0; j < arity; j++) {
                types[j] = VMDataType.all().get(a % VMDataType.all().size());
                a /= VMDataType.all().size();
            }
            OperandSpecificationRecord rec = findSpecificationRecord(insnName, types);
            if (rec.behaviour == behaviour)
                typess.add(types);
        }
        return typess;
    }


    public Set<VMDataType[]> getUnspecifiedOperands(String insnName, int arity) {
        return getOperands(insnName, arity, OperandSpecificationRecord.Behaviour.UNSPECIFIED);
    }

    public Set<VMDataType[]> getErrorOperands(String insnName, int arity) {
        return getOperands(insnName, arity, OperandSpecificationRecord.Behaviour.ERROR);
    }

    public boolean isAccepted(String insnName, VMDataType[] types) {
        return findSpecificationRecord(insnName, types).behaviour == OperandSpecificationRecord.Behaviour.ACCEPT;
    }
    public boolean isUnspecified(String insnName, VMDataType[] types) {
        return findSpecificationRecord(insnName, types).behaviour == OperandSpecificationRecord.Behaviour.UNSPECIFIED;
    }
    public boolean isError(String insnName, VMDataType[] types) {
        return findSpecificationRecord(insnName, types).behaviour == OperandSpecificationRecord.Behaviour.ERROR;
    }

    public int numOfMatchingOperands(String insnName) {
        int ret = -1;
        for (OperandSpecificationRecord rc : spec) {
            if (rc.insnName.equals(insnName)) {
                if (ret == -1) ret = rc.operandTypes.length;
                else if (ret != rc.operandTypes.length) throw new Error();
            }
        }
        if (ret == -1) throw new Error();
        return ret;
    }
}
