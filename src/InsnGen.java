import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class InsnGen {
	static String typeDefFile;
	static String insnDefFile;
	static String outDir;
	static boolean isSimple;
	
	static void parseOption(String[] args) {
		int i = 0;
		
		isSimple = false;
		
		while (true) {
			if (args[i].equals("-simple")) {
				isSimple = true;
				i++;
			} else
				break;
		}

		try {
			typeDefFile = args[i++];
			insnDefFile = args[i++];
			outDir = args[i++];
		} catch (Exception e) {
			System.out.println("InsnGen [-simple] <type definition> <insn definition> <out dir>");
			System.exit(1);
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		parseOption(args);

		TypeDefinition td = new TypeDefinition();
        td.load(typeDefFile);

        ProcDefinition procDef = new ProcDefinition();
        procDef.load(insnDefFile);

        for (ProcDefinition.InstDefinition insnDef: procDef.instDefs) {
        	System.out.println(insnDef.name);
        	ActionNode.prefix = insnDef.name;
        	Synthesiser synth =
        			isSimple ? new SimpleSynthesiser() :
        			insnDef.dispatchVars.length == 2 ? new TagPairSynthesiser() :
        					new SwitchSynthesiser();
        	StringBuilder sb = new StringBuilder();
        	if (insnDef.prologue != null) {
        	    sb.append(insnDef.prologue + "\n");
        	}
			sb.append(insnDef.name + "_HEAD:\n");
            Plan p = new Plan(insnDef.dispatchVars, insnDef.tdDef.rules);
            sb.append(synth.synthesise(p));
            if (insnDef.epilogue != null) {
                sb.append(insnDef.epilogue + "\n");
            }
            try {
            	File file = new File(outDir + "/" + insnDef.name + ".inc");
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                pw.print(sb.toString());
                pw.close();
            }catch(IOException e){
                System.out.println(e);
            }
        }
	}
}
