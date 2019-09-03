/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package specfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import specfile.SpecFile.InstructionFormat;
import specfile.SpecFile.OperandType;

public class InstructionDef {
    public static class Instruction {
        String name;
        Instruction(String name) {
            this.name = name;
        }
    }
    public static class LabelInstruction extends Instruction {
        ActualInstruction synonym;
        LabelInstruction(String name, ActualInstruction synonym) {
            super(name);
            this.synonym = synonym;
        }
    }
    public static class ActualInstruction extends Instruction {
        InstructionFormat format;
        OperandType[] operands;
        ActualInstruction(String name, InstructionFormat format, OperandType[] operands) {
            super(name);
            this.format = format;
            this.operands = operands;
        }
    }
    static public InstructionDef loadFromFile(String fileName) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(fileName));
        return parse(lines, 0, lines.size());
    }
    static public InstructionDef parse(List<String> lines, int start, int end) {
        List<Instruction> instructionDef = new ArrayList<Instruction>();
        List<String> labelOnlyInsns = new ArrayList<String>();
        for (int lineNo = start; lineNo < end; lineNo++) {
            String line = lines.get(lineNo);
            line = line.replaceFirst("//.*", "");
            if (line.matches("\\s*"))
                continue;
            String[] fields = line.split("\\s+");
            String name = fields[0];
            InstructionFormat format = SpecFile.instructionFormatTable.get(fields[1]);
            if (fields.length == 3 && fields[2].equals("LABELONLY")) {
                labelOnlyInsns.add(name);
                continue;
            }
            if (fields.length != 2 + format.arity)
                throw new Error("Error on parsing instruction def: "+lineNo+": "+lines.get(lineNo));
            OperandType[] operands = new OperandType[fields.length - 2];
            for (int i = 2; i < fields.length; i++) {
                operands[i - 2] = SpecFile.operandTypeTable.get(fields[i]);
            }
            ActualInstruction insn = new ActualInstruction(name, format, operands);
            for (String loiName: labelOnlyInsns) {
                LabelInstruction loi = new LabelInstruction(loiName, insn);
                instructionDef.add(loi);
            }
            instructionDef.add(insn);
            labelOnlyInsns.clear();
        }
        return new InstructionDef(instructionDef);
    }

    private InstructionDef(List<Instruction> list) {
        this.list = list;
    }
    private List<Instruction> list;

    public String unparse() {
        StringBuffer sb = new StringBuffer();
        unparse(sb);
        return sb.toString();
    }
    public void unparse(StringBuffer sb) {
        for (Instruction insnx: list) {
            sb.append(insnx.name).append(" ");
            if (insnx instanceof LabelInstruction) {
                LabelInstruction insn = (LabelInstruction) insnx;
                sb.append(insn.synonym.format.getName());
                sb.append(" ").append("LABELONLY\n");
            } else if (insnx instanceof ActualInstruction) {
                ActualInstruction insn = (ActualInstruction) insnx;
                sb.append(insn.format.getName());
                for (OperandType ot: insn.operands)
                    sb.append(" ").append(ot.getName());
                sb.append("\n");
            } else
                throw new Error("Intgernal error: unknown type of instruction");
        }
    }
    public List<Instruction> getList() {
        return list;
    }
    public int getOpcodeIndex(String name) {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).name.equals(name))
                return i;
        return -1;
    }
    public int numInstructions() {
        return list.size();
    }
}