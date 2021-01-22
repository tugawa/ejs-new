package vmdlc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nez.lang.Grammar;
import type.FunctionAnnotation;
import type.FunctionTable;
import type.TypeDefinition;
import vmdlc.TypeCheckVisitor.TypeCheckOption;

public class Option {
    private OperandSpecifications argumentSpec = new OperandSpecifications();
    private OperandSpecifications requiringFunctionSpec;
    private OperandSpecifications caseSplittingSpec;
    private List<OperandSpecifications> mergeTargets;
    InstructionDefinitions instructionDefinitions = new InstructionDefinitions();
    private String vmdlGrammarFile;
    private String functionTypeDependencyFile;
    private int typeMapLevel = 1;
    private String inliningFile;
    private String functionExternFile;
    private String sourceFile;
    private String opSpecCRequireFile;
    private boolean typeDefinitionSetFlag;
    private String requiringFunctionSpecFile;
    private boolean functionInliningFlag;

    private ProcessMode processMode = ProcessMode.Compile;
    private CompileMode compileMode;

    private XOption xOption = new XOption();

    public static enum ProcessMode {
        Compile, Preprocess, GenFuncSpec, MergeFuncSpec;
    }

    public static enum CompileMode {
        Instruction(false, false), Function(true, false), Builtin(true, true);

        private boolean functionMode;
        private boolean builtinFunctionMode;

        private CompileMode(boolean functionMode, boolean builtinFunctionMode) {
            this.functionMode = functionMode;
            this.builtinFunctionMode = builtinFunctionMode;
        }

        public boolean isFunctionMode() {
            return functionMode;
        }

        public boolean isBuiltinFunctionMode() {
            return builtinFunctionMode;
        }
    };

    @FunctionalInterface
    static interface CheckedBiFunction<S, T, R> {
        R apply(S s, T t) throws IOException;
    }

    static class OptionItem {
        private CheckedBiFunction<List<String>, Option, Integer> function;
        private String form;
        private String[] descriptions;

        public OptionItem(String form, String[] descriptions,
                CheckedBiFunction<List<String>, Option, Integer> function) {
            this.form = form;
            this.descriptions = descriptions;
            this.function = function;
        }

        public OptionItem(String form, String description, CheckedBiFunction<List<String>, Option, Integer> function) {
            this(form, new String[] { description }, function);
        }

        public CheckedBiFunction<List<String>, Option, Integer> getFunction() {
            return function;
        }

        public String[] getDescriptions() {
            return descriptions;
        }

        public String getForm() {
            return form;
        }
    }

    /* Commandline arguments setting */

    private final static Map<String, OptionItem> options = new LinkedHashMap<>();

    static {
        options.put("-d", new OptionItem("-d file", "[mandatory] Datatype specification file", (args, self) -> {
            final String dataTypeDefFile = args.get(1);
            TypeDefinition.load(dataTypeDefFile);
            self.typeDefinitionSetFlag = true;
            return 2;
        }));
        options.put("-o", new OptionItem("-o file", "Operand specification file", (args, self) -> {
            final String argumentSpecFile = args.get(1);
            self.argumentSpec.load(argumentSpecFile);
            return 2;
        }));
        options.put("-g",
                new OptionItem("-g file", "Nez grammar file (default: ejsdl.nez in jar file)", (args, self) -> {
                    self.vmdlGrammarFile = args.get(1);
                    return 2;
                }));
        options.put("-i", new OptionItem("-i file", "Instruction defs", (args, self) -> {
            final String insnDefFile = args.get(1);
            self.instructionDefinitions.load(insnDefFile);
            return 2;
        }));
        options.put("-no-match-opt",
                new OptionItem("-no-match-opt", "Disable optimisation for match statement", (args, self) -> {
                    self.xOption.mDisableMatchOptimisation = true;
                    return 1;
                }));
        options.put("-T1", new OptionItem("-T1", "Set type analysis level to 1 (Using LUB)", (args, self) -> {
            self.typeMapLevel = 1;
            return 1;
        }));
        options.put("-T2", new OptionItem("-T2", "Set type analysis level to 2 (Mixed)", (args, self) -> {
            self.typeMapLevel = 2;
            return 1;
        }));
        options.put("-T3", new OptionItem("-T3", "Set type analysis level to 3 (Strict)", (args, self) -> {
            self.typeMapLevel = 3;
            return 1;
        }));
        options.put("-preprocess", new OptionItem("-preprocess", "Use preprocess mode", (args, self) -> {
            if (self.processMode != ProcessMode.Compile) {
                ErrorPrinter.error("Can not use \"-gen-funcspec\" and \"-preprocess\" option simultaneously");
            }
            self.processMode = ProcessMode.Preprocess;
            return 1;
        }));
        /*
        options.put("-gen-funcspec", new OptionItem("-gen-funcspec ftdfile file",
            "Use generating function specification mode", (args, self) -> {
                if (self.processMode != ProcessMode.Compile) {
                    ErrorPrinter.error("Can not use \"-gen-funcspec\" and \"-preprocess\" option simultaneously");
                }
                self.processMode = ProcessMode.GenFuncSpec;
                self.functionTypeDependencyFile = args.get(1);
                final String argumentSpecFile = args.get(2);
                self.argumentSpec.load(argumentSpecFile);
                return 3;
            }));
        */
        options.put("-func-inline-opt", new OptionItem("-func-inline-opt file", "Enable function inlining",
            (args, self) -> {
                self.functionInliningFlag = true;
                self.inliningFile = args.get(1);
                return 2;
            }));
        options.put("-write-fi", new OptionItem("-write-fi file", new String[] { "Generate function inlining file", "(Use in preprocess mode)" },
            (args, self) -> {
                    self.inliningFile = args.get(1);
                    return 2;
                }));
        options.put("-write-ftd", new OptionItem("-write-ftd file", new String[] { "Generate function type dependency file", "(Use in preprocess mode)" },
            (args, self) -> {
                self.functionTypeDependencyFile = args.get(1);
                return 2;
            }));
        options.put("-write-extern", new OptionItem("-write-extrn file", new String[] { "Append function extern declaration", "(Use in preprocess mode)" },
            (args, self) -> {
                self.functionExternFile = args.get(1);
                return 2;
            }));
        options.put("-update-funcspec", new OptionItem("-update-funcspec file", "Update function specification file",
            (args, self) -> {
                self.requiringFunctionSpecFile = args.get(1);
                self.requiringFunctionSpec = new OperandSpecifications();
                self.requiringFunctionSpec.load(self.requiringFunctionSpecFile);
                return 2;
            }));
        options.put("-case-split", new OptionItem("-case-split file", "Enable case splitting",
            (args, self) -> {
                final String caseSplittingSpecFile = args.get(1);
                self.caseSplittingSpec = new OperandSpecifications();
                self.caseSplittingSpec.load(caseSplittingSpecFile);
                return 2;
            }));
        options.put("-gen-funcspec", new OptionItem("-gen-funcspec file1 file2 ...", "Generate merged function specification files",
            (args, self) -> {
                if (self.processMode != ProcessMode.Compile) {
                    ErrorPrinter.error("Cannot specify modes at the same time");
                }
                self.processMode = ProcessMode.GenFuncSpec;
                self.mergeTargets = new ArrayList<>(args.size()-1);
                List<String> files = args.subList(1, args.size());
                for(String file : files){
                    OperandSpecifications spec = new OperandSpecifications();
                    spec.load(file);
                    self.mergeTargets.add(spec);
                }
                return args.size();
            }));
        options.put("-write-opspec-creq", new OptionItem("-write-opspec-creq file", new String[] { "Write operand specification C requires", "(Use in preprocess mode)" },
            (args, self) -> {
                self.opSpecCRequireFile = args.get(1);
                return 2;
            }));
        options.put("-ftd", new OptionItem("-ftd file", new String[] { "Specify function type dependency file", "(Use in GenFuncSpec mode)" },
            (args, self) -> {
                self.functionTypeDependencyFile = args.get(1);
                return 2;
            }));
    }

    public void parseOption(String[] args) throws IOException {
        List<String> argList = Arrays.asList(args);
        while (argList.size() > 0) {
            String key = argList.get(0);
            int consumed;
            if (!options.containsKey(key)) {
                if (key.startsWith("-X")) {
                    consumed = xOption.addOption(key, argList.toArray(new String[0]), 1);
                    if (consumed == -1)
                        break;
                } else {
                    if (key.startsWith("-"))
                        ErrorPrinter.error("Unknown option: "+key);
                    sourceFile = key;
                    break;
                }
            } else {
                OptionItem item = options.get(key);
                consumed = item.getFunction().apply(argList, this);
            }
            argList = argList.subList(consumed, argList.size());
        }
        if(isIncorrectCommandlineArgument()){
            printDescription();
            System.exit(-1);
        }
    }

    private boolean isIncorrectCommandlineArgument(){
        return (!typeDefinitionSetFlag || sourceFile == null) && processMode != ProcessMode.GenFuncSpec;
    }

    private void printDescription(){
        final int LEFT_MARGIN = 3;
        final int MIN_MARGIN = 2;
        int maxFormLength = -1;
        for(OptionItem item : options.values()){
            String form = item.getForm();
            int formLength = form.length();
            if(formLength > maxFormLength)
                maxFormLength = formLength;
        }
        StringBuilder leftSpaceBuilder = new StringBuilder();
        for(int i=0; i<LEFT_MARGIN; i++)
        leftSpaceBuilder.append(' ');
        String leftSpace = leftSpaceBuilder.toString();
        StringBuilder spaceBuilder = new StringBuilder();
        for(int i=0; i<LEFT_MARGIN+maxFormLength+MIN_MARGIN; i++)
            spaceBuilder.append(' ');
        String space = spaceBuilder.toString();
        for(OptionItem item : options.values()){
            String form = item.getForm();
            int margin = maxFormLength - form.length() + MIN_MARGIN;
            String[] descriptions = item.getDescriptions();
            StringBuilder builder = new StringBuilder();
            builder.append(leftSpace);
            builder.append(form);
            for(int i=0; i<margin; i++)
                builder.append(' ');
            builder.append(descriptions[0]);
            System.err.println(builder.toString());
            for(int i=1; i<descriptions.length; i++)
                System.err.println(space+descriptions[i]);
        }
        /* Print xOption description */
        System.out.println(leftSpace+"-Xcmp:verify_diagram [true|false]");
        System.out.println(leftSpace+"-Xcmp:opt_pass [MR:S]");
        System.out.println(leftSpace+"-Xcmp:rand_seed n                 set random seed of dispatch processor");
        System.out.println(leftSpace+"-Xcmp:tree_layer p0:p1:h0:h1");
        System.out.println(leftSpace+"-Xgen:use_goto [true|false]");
        System.out.println(leftSpace+"-Xgen:pad_cases [true|false]");
        System.out.println(leftSpace+"-Xgen:use_default [true|false]");
        System.out.println(leftSpace+"-Xgen:magic_comment [true|false]");
        System.out.println(leftSpace+"-Xgen:debug_comment [true|false]");
        System.out.println(leftSpace+"-Xgen:label_prefix xxx            set xxx as goto label");
        System.out.println(leftSpace+"-Xgen:type_label [true|false]");
    }

    public void setCompileMode(String functionName) {
        if (processMode != ProcessMode.Compile)
            return;
        if (FunctionTable.hasAnnotations(functionName, FunctionAnnotation.vmInstruction)) {
            compileMode = CompileMode.Instruction;
        } else if (FunctionTable.hasAnnotations(functionName, FunctionAnnotation.builtinFunction)) {
            compileMode = CompileMode.Builtin;
        } else {
            compileMode = CompileMode.Function;
        }
    }

    public void setFunctionInlining(Grammar grammar) {
        InlineFileProcessor.read(inliningFile, grammar);
    }

    public boolean isSetTypeDefinition(){
        return typeDefinitionSetFlag;
    }

    public boolean isSetSourceFile(){
        return sourceFile != null;
    }

    public boolean isSetFunctionTypeDependencyFile(){
        return functionTypeDependencyFile != null;
    }

    public boolean isSetInliningFile(){
        return inliningFile != null;
    }

    public boolean isSetFunctionExternFile(){
        return functionExternFile != null;
    }

    public boolean isSetOpSpecCRequireFile(){
        return opSpecCRequireFile != null;
    }

    public boolean isSetVMDLGrammarFile(){
        return vmdlGrammarFile != null;
    }

    public boolean isRequiredUpdatingFunctionSpec(){
        return requiringFunctionSpec != null;
    }

    public boolean isEnableFunctionInlining(){
        return functionInliningFlag;
    }

    public boolean isEnableCaseSplitting(){
        return caseSplittingSpec != null;
    }

    public XOption getXOption(){
        return xOption;
    }

    public String getSourceFile(){
        return sourceFile;
    }

    public String getInliningFile(){
        return inliningFile;
    }

    public String getFunctionExternFile(){
        return functionExternFile;
    }

    public String getFunctionTypeDependencyFile(){
        return functionTypeDependencyFile;
    }

    public String getOpSpecCRequireFile(){
        return opSpecCRequireFile;
    }

    public String getVMDLGrammarFile(){
        return vmdlGrammarFile;
    }

    public String getRequiringFunctionSpecFile(){
        return requiringFunctionSpecFile;
    }

    public CompileMode getCompileMode(){
        return compileMode;
    }

    public ProcessMode getProcessMode(){
        return processMode;
    }

    public OperandSpecifications getArgumentSpec(){
        return argumentSpec;
    }

    public OperandSpecifications getRequiringFunctionSpec(){
        return requiringFunctionSpec;
    }

    public List<OperandSpecifications> getMergeTargets(){
        return mergeTargets;
    }

    public InstructionDefinitions getInstructionDefinitions(){
        return instructionDefinitions;
    }

    public TypeCheckOption getTypeCheckOption(){
        return new TypeCheckVisitor.TypeCheckOption()
            .setOperandSpec(argumentSpec)
            .setFunctionSpec(requiringFunctionSpec)
            .setCaseSplitSpec(caseSplittingSpec)
            .setTypeCheckPolicy(TypeCheckVisitor.TypeCheckPolicy.values()[typeMapLevel-1])
            .setFunctionIniningFlag(isEnableFunctionInlining())
            .setUpdateFTDFlag(isSetFunctionTypeDependencyFile())
            .setCaseSplitFlag(isEnableCaseSplitting())
            .setCompileMode(compileMode);
    }

}
