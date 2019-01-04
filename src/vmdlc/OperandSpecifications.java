/*
   OperandSpecifications.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
 */
package vmdlc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import type.AstType;
import type.AstType.JSValueType;
import type.AstType.JSValueVMType;
import type.VMDataType;
import type.VMDataTypeVecSet;


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
    List<OperandSpecificationRecord> spec;
    Map<String, Integer> arities;
    
    void load(Scanner sc) {
        final String P_SYMBOL = "[a-zA-Z_]+";
        final String P_OPERANDS = "\\(\\s*([^)]+)\\s*\\)";
        final String P_BEHAVIOUR = "accept|error|unspecified";
        final Pattern splitter = Pattern.compile("("+P_SYMBOL+")\\s+"+P_OPERANDS+"\\s+("+P_BEHAVIOUR+")\\s*$");

        spec = new ArrayList<OperandSpecificationRecord>();
        arities = new HashMap<String, Integer>();
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
                Integer arity = arities.get(insnName);
                if (arity == null)
                    arities.put(insnName, n);
                else {
                    if (arity != n)
                        throw new Error("operand specification file error");
                }
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

    private Set<VMDataType[]> getUnspecifiedOperands(String insnName) {
        return getOperands(insnName, OperandSpecificationRecord.Behaviour.UNSPECIFIED);
    }

    private Set<VMDataType[]> getErrorOperands(String insnName) {
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
            final JSValueType jsv = JSValueType.get("JSValue");
            int index = lookup(vn);
            Set<VMDataType[]> dtss = opSpec.getAcceptOperands(vn);
            if (dtss == null)
                return jsv;
            AstType t = AstType.BOT;
            for (VMDataType[] dts: dtss) {
                JSValueVMType s = JSValueVMType.get(dts[index]);
                t = t.lub(s);
                if (t == jsv)
                    break;
            }
            return t;
        }
    }
    
    public VMDataTypeVecSet getAccept(String insnName, String[] paramNames) {
        return new OperandVMDataTypeVecSet(paramNames, this, insnName);
    }
}
