/*
   OperandSpecifications.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
 */
package old.insngen;

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

import type.VMDataType;

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
        final String P_OPERANDS = "\\(\\s*([^)]+)\\s*\\)";
        final String P_BEHAVIOUR = "accept|error|unspecified";
        final Pattern splitter = Pattern.compile("("+P_SYMBOL+")\\s+"+P_OPERANDS+"\\s+("+P_BEHAVIOUR+")\\s*$");
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            Matcher matcher = splitter.matcher(line);
            if (matcher.matches()) {
                MatchResult m = matcher.toMatchResult();
                String insnName = m.group(1);
                String[] allOps = m.group(2).split("\\s*,\\s*");
                int n = 0;
                for (String s: allOps)
                    if (!s.equals("-"))
                        n++;
                String[] operandTypes = new String[n];
                int i = 0;
                for (String s: allOps)
                    if (!s.equals("-"))
                        operandTypes[i++] = s;
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

    boolean matchOperandTypes(String[] specTypes, VMDataType[] types) {
        if (specTypes.length != types.length)
            return false;
        for (int i = 0; i < specTypes.length; i++)
            if (!specTypes[i].equals("_") &&
                    !(specTypes[i].equals("object") && types[i].isObject()) &&
                    !specTypes[i].equals(types[i].getName()))
                return false;
        return true;
    }

    OperandSpecificationRecord findSpecificationRecord(String insnName, VMDataType[] types) {
        for (OperandSpecificationRecord rec: spec) {
            if (insnName.equals(rec.insnName) &&
                    matchOperandTypes(rec.operandTypes, types))
                return rec;
        }
        throw new Error("unexhaustive type specification for :"+insnName+"("+types[0].getName()+","+types[1].getName()+")");
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
}
