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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import specfile.SpecFile.DataType;

public class SuperinstructionSpec {
    public static class Superinstruction {
        Superinstruction(String iName, String siName, DataType[] opTypes) {
            this.iName = iName;
            this.siName = siName;
            this.opTypes = opTypes;
        }
        private String iName;
        private String siName;
        private DataType[] opTypes;
        public String getBaseName() {
            return iName;
        }
        public String getName() {
            return siName;
        }
        public DataType getOpType(int i) {
            return opTypes[i];
        }
    }

    static final Pattern SISPEC_LINE_PATTERN = Pattern.compile("^\\s*(\\w+)\\((.*)\\)\\s*:\\s*(\\w+)\\s*$");
    static public SuperinstructionSpec loadFromFile(String fileName) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(fileName));
        return parse(lines, 0, lines.size());
    }
    static public SuperinstructionSpec parse(List<String> lines, int start, int end) {
        List<Superinstruction> superinstructionSpec = new ArrayList<Superinstruction>();
        for (int lineNo = start; lineNo < end; lineNo++) {
            String line = lines.get(lineNo);
            line = line.replaceFirst("//.*", "");
            if (line.matches("\\s*"))
                continue;
            Matcher m = SISPEC_LINE_PATTERN.matcher(line);
            if (m.find()) {
                String iName = m.group(1);
                String[] operands = m.group(2).split("\\s*,\\s*");
                String siName = m.group(3);
                DataType[] opTypes = new DataType[operands.length];
                for (int i = 0; i < operands.length; i++)
                    opTypes[i] = SpecFile.dataTypeTable.get(operands[i]);
                Superinstruction si = new Superinstruction(iName, siName, opTypes);
                superinstructionSpec.add(si);
            } else
                throw new Error("Superinstruction spec parse error: "+lineNo+": "+lines.get(lineNo));
        }
        return new SuperinstructionSpec(superinstructionSpec);
    }

    private SuperinstructionSpec(List<Superinstruction> list) {
        this.list = list;
    }
    private List<Superinstruction> list;

    public String unparse() {
        StringBuffer sb = new StringBuffer();
        unparse(sb);
        return sb.toString();
    }
    public void unparse(StringBuffer sb) {
        for (Superinstruction si: list) {
            sb.append(si.iName).append("(");
            for (int i = 0; i < si.opTypes.length; i++) {
                if (i > 0)
                    sb.append(",");
                sb.append(si.opTypes[i].getName());
            }
            sb.append("): ").append(si.siName).append("\n");
        }
    }
    public List<Superinstruction> getList() {
        return list;
    }
    public int getOpcodeIndex(String name) {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).siName.equals(name))
                return i;
        return -1;
    }
    public int numInstructions() {
        return list.size();
    }
}