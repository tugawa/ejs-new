package vmgen;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import vmgen.dd.DDNode;
import vmgen.synth.SimpleSynthesiser;
import vmgen.synth.SwitchSynthesiser;
import vmgen.synth.Synthesiser;
import vmgen.synth.TagPairSynthesiser;
import vmgen.type.TypeDefinition;

public class InsnGen {
	static String typeDefFile;
	static String insnDefFile;
	static String outDir;
	static boolean isSimple;
	public static boolean DEBUG = false;
	
	static void parseOption(String[] args) {
		int i = 0;
		
		if (args.length == 0) {
			typeDefFile = "datatype/genericfloat.def";
			insnDefFile = "idefs/div.idef";
			outDir = null;
			isSimple = false;
			DEBUG = true;
			return;
		}

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
			if (i < args.length)
				outDir = args[i++];
		} catch (Exception e) {
			System.out.println("InsnGen [-simple] <type definition> <insn definition> [<out dir>]");
			System.exit(1);
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		parseOption(args);

        TypeDefinition.load(typeDefFile);

        ProcDefinition procDef = new ProcDefinition();
        procDef.load(insnDefFile);

        for (ProcDefinition.InstDefinition insnDef: procDef.instDefs) {
        	if (outDir != null)
        		System.out.println(insnDef.name);
        	DDNode.setPrefix(insnDef.name);
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
            if (outDir == null) {
            	System.out.println(sb.toString());
            } else {
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
}
