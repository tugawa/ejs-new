package vmgen;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import vmgen.Plan.Rule;
import vmgen.dd.DDNode;
import vmgen.synth.SimpleSynthesiser;
import vmgen.synth.SwitchSynthesiser;
import vmgen.synth.Synthesiser;
import vmgen.synth.TagPairSynthesiser;
import vmgen.type.TypeDefinition;
import vmgen.type.VMDataType;

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
	
	static String synthesise(ProcDefinition.InstDefinition insnDef, Synthesiser synth, boolean verbose) {
    	Set<VMDataType[]> dontCareInput = new HashSet<VMDataType[]>();
    	dontCareInput.add(new VMDataType[]{VMDataType.get("string"), VMDataType.get("array")});
    	Set<VMDataType[]> errorInput = new HashSet<VMDataType[]>();
    	errorInput.add(new VMDataType[]{VMDataType.get("string"), VMDataType.get("string")});
    	String errorAction = "goto UNEXPECTED_OPERAND_TYPE_ERROR;";
    	
		Set<Rule> rules = new HashSet<Rule>();
		Set<Plan.Condition> removeSet = new HashSet<Plan.Condition>();
		Set<Plan.Condition> errorConditions = new HashSet<Plan.Condition>();
		for (VMDataType[] dts: dontCareInput)
			removeSet.add(new Plan.Condition(dts));
		for (VMDataType[] dts: errorInput) {
			Plan.Condition c = new Plan.Condition(dts);
			removeSet.add(c);
			errorConditions.add(c);
		}
    	for (Rule r: insnDef.tdDef.rules)
    		rules.add(r.filterConditions(removeSet));
    	if (errorConditions.size() > 0) {
    		rules.add(new Rule(errorAction, errorConditions));
    	}
    	
		DDNode.setLabelPrefix(insnDef.name);
        Plan p = new Plan(insnDef.dispatchVars, rules);
        String dispatchCode = synth.synthesise(p);
    	
    	StringBuilder sb = new StringBuilder();
    	if (insnDef.prologue != null)
    	    sb.append(insnDef.prologue + "\n");
		sb.append(insnDef.name + "_HEAD:\n");
        sb.append(dispatchCode);
        if (insnDef.epilogue != null)
            sb.append(insnDef.epilogue + "\n");
        return sb.toString();
	}

	public static void main(String[] args) throws FileNotFoundException {
		parseOption(args);

        TypeDefinition.load(typeDefFile);

        ProcDefinition procDef = new ProcDefinition();
        procDef.load(insnDefFile);
        
        for (ProcDefinition.InstDefinition insnDef: procDef.instDefs) {
        	boolean verbose = outDir != null;
        	Synthesiser synth =
        			isSimple ? new SimpleSynthesiser() :
        			insnDef.dispatchVars.length == 2 ? new TagPairSynthesiser() :
        					new SwitchSynthesiser();
        	String code = synthesise(insnDef, synth, verbose);
        	if (outDir == null)
        		System.out.println(code);
        	else {
        		try {
        			File file = new File(outDir + "/" + insnDef.name + ".inc");
	                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
	                pw.print(code);
	                pw.close();
	            }catch(IOException e){
	                System.out.println(e);
        		}
        	}
        }
	}
}
