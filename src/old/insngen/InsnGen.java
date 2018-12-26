/*
   InsnGen.java

   eJS Project
     Kochi University of Technology
     the University of Electro-communications

     Tomoharu Ugawa, 2016-18
     Hideya Iwasaki, 2016-18
 */
package old.insngen;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import dispatch.RuleSet;
import dispatch.RuleSet.Rule;
import type.TypeDefinition;
import type.VMDataType;


public class InsnGen {
    public static class Option {
        HashMap<AvailableOptions, Object> options = new HashMap<AvailableOptions, Object>();

        public enum AvailableOptions {
            CMP_VERIFY_DIAGRAM("cmp:verify_diagram", Boolean.class),
            CMP_USE_TAGPAIR("cmp:use_tagpair", Boolean.class),
            CMP_OPT_PASS("cmp:opt_pass", String.class),
            CMP_RAND_SEED("cmp:rand_seed", Integer.class),
            GEN_USE_GOTO("gen:use_goto", Boolean.class),
            GEN_PAD_CASES("gen:pad_cases", Boolean.class),
            GEN_USE_DEFAULT("gen:use_default", Boolean.class),
            GEN_MAGIC_COMMENT("gen:magic_comment", Boolean.class),
            GEN_DEBUG_COMMENT("gen:debug_comment", Boolean.class);

            String key;
            Class<?> cls;
            AvailableOptions(String key, Class<?> cls) {
                this.key = key;
                this.cls = cls;
            }
        };

        int addOption(String[] args, int index) {
            for (AvailableOptions os: AvailableOptions.values()) {
                if (args[index].equals("-X" + os.key)) {
                    index++;
                    if (os.cls == String.class)
                        options.put(os, args[index]);
                    else if (os.cls == Boolean.class)
                        options.put(os, Boolean.parseBoolean(args[index]));
                    else if (os.cls == Integer.class)
                        options.put(os, Integer.parseInt(args[index]));
                    return index + 1;
                }
            }
            return -1;
        }

        public <T> T getOption(AvailableOptions opt, T defaultValue) {
            Object val = options.get(opt);
            if (val == null)
                return defaultValue;
            return (T) val;
        }
    }

    static String typeDefFile;
    static String insnDefFile;
    static String operandSpecFile;
    static String outDir;
    static int compiler;
    static Option option = new Option();

    public static final int COMPILER_DEFAULT = 0;
    public static final int COMPILER_SIMPLE = 1;
    public static final int COMPILER_OLD = 2;

    public static boolean DEBUG = false;

    static void parseOption(String[] args) {
        int i = 0;

        compiler = COMPILER_DEFAULT;

        if (args.length == 0) {
            typeDefFile = "datatype/genericfloat.def";
            insnDefFile = "idefs/div.idef";
            operandSpecFile = null;
            outDir = null;
            DEBUG = true;
            return;
        }

        try {
            while (true) {
                if (args[i].equals("-simple")) {
                    compiler = COMPILER_SIMPLE;
                    i++;
                } else if (args[i].equals("-old")) {
                    compiler = COMPILER_OLD;
                    i++;
                } else if (args[i].startsWith("-X")){
                    i = option.addOption(args, i);
                    if (i == -1)
                        throw new Exception("invalid option");
                } else
                    break;
            }

            typeDefFile = args[i++];
            insnDefFile = args[i++];
            operandSpecFile = args[i++];
            if (i < args.length)
                outDir = args[i++];
        } catch (Exception e) {
            System.out.println("InsnGen [-simple|-old] <type definition> <insn definition> <operand spec> [<out dir>]");
            System.exit(1);
        }
    }

    static String generateInstructionCode(InstructionDefinition.InstDefinition insnDef, OperandSpecifications operandSpec, boolean verbose) {
        //
        // Step 1-2. Apply operand specifications
        //
        
        String errorAction = "LOG_EXIT(\"unexpected operand type\\n\");";
        
        Set<VMDataType[]> dontCareInput = operandSpec.getUnspecifiedOperands(insnDef.name, insnDef.dispatchVars.length);
        Set<VMDataType[]> errorInput = operandSpec.getErrorOperands(insnDef.name, insnDef.dispatchVars.length);
        
        Set<Rule> rules = new HashSet<Rule>();
        Set<String> unusedActions = new HashSet<String>();
        Set<RuleSet.OperandDataTypes> removeSet = new HashSet<RuleSet.OperandDataTypes>();
        Set<RuleSet.OperandDataTypes> errorConditions = new HashSet<RuleSet.OperandDataTypes>();
        for (VMDataType[] dts: dontCareInput)
            removeSet.add(new RuleSet.OperandDataTypes(dts));
        for (VMDataType[] dts: errorInput) {
            RuleSet.OperandDataTypes c = new RuleSet.OperandDataTypes(dts);
            removeSet.add(c);
            errorConditions.add(c);
        }
        for (Rule r: insnDef.tdDef.rules) {
            r = r.filterConditions(removeSet);
            rules.add(r);
            if (r.condition.size() == 0)
                unusedActions.add(r.action);
        }
        if (errorConditions.size() > 0) {
            rules.add(new Rule(errorAction, errorConditions));
        }

        
        //
        // Do code generate
        //
        RuleSet p = new RuleSet(insnDef.dispatchVars, rules);
        dispatch.DispatchProcessor dispatchProcessor = new dispatch.DispatchProcessor();
        dispatchProcessor.setLabelPrefix(insnDef.name);
        dispatch.DispatchPlan dispatchPlan = new dispatch.DispatchPlan(2, false);
        String dispatchCode = dispatchProcessor.translate(p, dispatchPlan);

        //
        // Add prologue, epilogue, and branches for unused operand datatypes
        //
        StringBuilder sb = new StringBuilder();
        if (insnDef.prologue != null)
            sb.append(insnDef.prologue + "\n");
        sb.append("INSN_COUNT"+insnDef.dispatchVars.length+"("+insnDef.name);
        for (String rand: insnDef.dispatchVars)
            sb.append(",").append(rand);
        sb.append(");");
        sb.append(insnDef.name + "_HEAD:\n");
        sb.append(dispatchCode);
        for (String a: unusedActions) {
            sb.append("if (0) {\n")
            .append(a)
            .append("}\n");
        }
        if (insnDef.epilogue != null)
            sb.append(insnDef.epilogue + "\n");
        
        return sb.toString();
    }

    public static void main(String[] args) throws FileNotFoundException {
        parseOption(args);

        TypeDefinition.load(typeDefFile);

        InstructionDefinition instructionDef = new InstructionDefinition();
        instructionDef.load(insnDefFile);

        OperandSpecifications operandSpec = new OperandSpecifications();
        operandSpec.load(operandSpecFile);

        for (InstructionDefinition.InstDefinition insnDef: instructionDef.instDefs) {
            boolean verbose = outDir != null;
            
            String code = generateInstructionCode(insnDef, operandSpec, verbose);
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
