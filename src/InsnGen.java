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
        	System.out.println(insnDef.name);
        	Synthesiser synth =
        			insnDef.dispatchVars.length == 2 ?
        					new TagPairSynthesiser() :
        					new SwitchSynthesiser();
        	StringBuilder sb = new StringBuilder();
        	if (insnDef.prologue != null) {
        	    sb.append(insnDef.prologue + "\n");
        	}
			sb.append(insnDef.name + "_HEAD:\n");
            Plan p = new Plan(Arrays.stream(insnDef.dispatchVars).collect(Collectors.toList()).toArray(new String[]{}), insnDef.tdDef.rules);
            sb.append(synth.synthesise(p));
            if (insnDef.epilogue != null) {
                sb.append(insnDef.epilogue + "\n");
            }
            try {
            	File file = new File(outDir + "/" + insnDef.name.toLowerCase() + ".inc");
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                pw.print(sb.toString());
                pw.close();
            }catch(IOException e){
                System.out.println(e);
            }
        }
	}
}
