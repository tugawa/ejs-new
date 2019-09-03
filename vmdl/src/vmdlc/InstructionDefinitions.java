/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import java.util.Map;
import java.util.Scanner;
import java.util.HashMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class InstructionDefinitions {
    enum InsnType {
        SMALLPRIMITIVE,
        BIGPRIMITIVE,
        THREEOP,
        TWOOP,
        ONEOP,
        ZEROOP,
        UNCONDJUMP,
        CONDJUMP,
        GETVAR,
        SETVAR,
        MAKECLOSUREOP,
        CALLOP,
        UNKNOWNOP
    };
    public enum OperandKinds {
        Register("r"),
        JSValue("v"),
        Subscript("s"),
        Displacement("d"),
        Int("i"),
        LABELONLY("");
        private final String varPrefix;
        private OperandKinds(final String s) {
            this.varPrefix = s;
        }
        static public OperandKinds getValue(String s) {
            if (s.equals("int")) {
                return Int;
            } else {
                return valueOf(s);
            }
        }
        public String getVarPrefix() {
            return this.varPrefix;
        }
    }

    static class InstructionDefinitionRecord {
        String insnName;


        InsnType insnType;

        OperandKinds[] operandKinds;

        InstructionDefinitionRecord(String insnName, InsnType insnType, OperandKinds[] operandKinds) {
            this.insnName = insnName;
            this.insnType = insnType;
            this.operandKinds = operandKinds;
        }
    }

    Map<String, InstructionDefinitionRecord> map;

    void load(Scanner sc) {
        map = new HashMap<String, InstructionDefinitionRecord>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            String[] record = line.split("[\\s]+", 0);
            if (record[0].equals("//")) {
                continue;
            }

            String insnName = record[0];
            InsnType insnType = InsnType.valueOf(record[1]);

            OperandKinds[] operandKinds = new OperandKinds[record.length];
            for (int i = 2; i < record.length; i++) {
                operandKinds[i-2] = OperandKinds.getValue(record[i]);
            }
            map.put(record[0], new InstructionDefinitionRecord(insnName, insnType, operandKinds));
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

    public OperandKinds[] getKinds(String insnName) {
        return map.get(insnName).operandKinds;
    }
}