import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class InsnGen {
	public static void main(String[] args) throws FileNotFoundException {
		if (args.length != 3) {
			System.out.println("InsnGen <type definition> <insn definition> <out dir>");
			System.exit(1);
		}
		String typeDefFile = args[0];
		String insnDefFile = args[1];
		String outDir = args[2];

		TypeDefinition td = new TypeDefinition();
        td.load(typeDefFile);

        ProcDefinition procDef = new ProcDefinition();
        procDef.load(insnDefFile);

        for (ProcDefinition.InstDefinition insnDef: procDef.instDefs) {
        	Synthesiser synth = new SimpleSynthesiser();
        	StringBuilder sb = new StringBuilder();
			sb.append(insnDef.name + "_HEAD:\n");
            Plan p = new Plan(Arrays.stream(insnDef.dispatchVars).map(s -> s.substring(1, s.length())).collect(Collectors.toList()).toArray(new String[]{}), insnDef.tdDef.rules);
            sb.append(synth.synthesise(p));
            try {
            	File file = new File(outDir + "/" + insnDef.name.substring(2).toLowerCase() + ".inc");
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                pw.print(sb.toString());
                pw.close();
            }catch(IOException e){
                System.out.println(e);
            }
        }
	}
}
