/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import type.AstType;
import type.VMDataType;
import type.VMDataTypeVecSet;


public class OperandSpecifications {
    static final boolean DEBUG = false;
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
    List<OperandSpecificationRecord> spec;
    Map<String, Integer> arities;

    void load(Scanner sc) {
        final String P_SYMBOL = "[a-zA-Z_]+";
        final String P_OPERANDS = "\\(\\s*([^)]*)\\s*\\)";
        final String P_BEHAVIOUR = "accept|error|unspecified";
        final Pattern splitter = Pattern.compile("("+P_SYMBOL+")\\s*"+P_OPERANDS+"\\s*("+P_BEHAVIOUR+")\\s*$");

        spec = new ArrayList<OperandSpecificationRecord>();
        arities = new HashMap<String, Integer>();
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

                    Integer arity = arities.get(insnName);
                    if (arity == null)
                        arities.put(insnName, n);
                    else {
                        if (arity != n)
                            throw new Error("operand specification file error");
                    }
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

    public Set<VMDataType[]> getOperands(String insnName, OperandSpecificationRecord.Behaviour behaviour) {
        if (arities == null)
            return null;
        Integer xarity = arities.get(insnName);
        if (xarity == null)
            return null;
        int arity = xarity.intValue();
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
            if (spec == null)
                typess.add(types);
            else {
                OperandSpecificationRecord rec = findSpecificationRecord(insnName, types);
                if (rec.behaviour == behaviour)
                    typess.add(types);
            }
        }
        return typess;
    }


    private Set<VMDataType[]> getAcceptOperands(String insnName) {
        return getOperands(insnName, OperandSpecificationRecord.Behaviour.ACCEPT);
    }

    public Set<VMDataType[]> getUnspecifiedOperands(String insnName) {
        return getOperands(insnName, OperandSpecificationRecord.Behaviour.UNSPECIFIED);
    }

    public Set<VMDataType[]> getErrorOperands(String insnName) {
        return getOperands(insnName, OperandSpecificationRecord.Behaviour.ERROR);
    }

    static class OperandVMDataTypeVecSet extends VMDataTypeVecSet {
        OperandSpecifications opSpec;
        String insnName;
        OperandVMDataTypeVecSet(String[] paramNames, OperandSpecifications opSpec, String insnName) {
            super(paramNames);
            this.opSpec = opSpec;
            this.insnName = insnName;
        }

        @Override
        public AstType getMostSpecificType(String vn) {
            Set<VMDataType[]> dtss = opSpec.getAcceptOperands(insnName);
            return getMostSpecificTypeFromSet(dtss, vn);
        }

        @Override
        public Set<VMDataType[]> getTuples() {
            return opSpec.getAcceptOperands(insnName);
        }
    }

    public VMDataTypeVecSet getAccept(String insnName, String[] paramNames) {
        return new OperandVMDataTypeVecSet(paramNames, this, insnName);
    }

    private Set<String[]> getErrorOperandsString(String insnName) {
        Set<String[]> result = new HashSet<String[]>();
        for (OperandSpecificationRecord rec : spec) {
            if (insnName.equals(rec.insnName) &&
                    rec.behaviour == OperandSpecificationRecord.Behaviour.ERROR) {
                result.add(rec.operandTypes);
            }
        }
        return result;
    }
}
