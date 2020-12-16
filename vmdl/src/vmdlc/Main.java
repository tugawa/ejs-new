/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import dispatch.DispatchProcessor;
import nez.ParserGenerator;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.parser.io.FileSource;
import nez.parser.io.StringSource;
import nez.ast.Source;
import nez.ast.SourceError;

import type.*;


public class Main {
    static final String VMDL_GRAMMAR = "ejsdsl.nez";
    static final String INLINE_FILE = "./inlines.inline";
    static String sourceFile;
    static String dataTypeDefFile;
    static String vmdlGrammarFile;
    static String operandSpecFile;
    static String insnDefFile;
    static String inlineExpansionFile;
    static String inlineExpansionWriteFile;
    static String functionDependencyFile = "./vmdl_workspace/dependency.ftd";
    static String functionExternFile;
    static String insnCallSpecFile;
    static String functionSpecFile;
    static int typeMapIndex = 1;
    static BehaviorMode behaviorMode = BehaviorMode.Compile;
    static CompileMode compileMode = null;
    static boolean generateArgumentSpecMode = false;
    static boolean doCaseSplit = false;

    static Option option = new Option();

    public static enum BehaviorMode{
        Compile, Preprocess, GenFuncSpec;
    }

    public static enum CompileMode{
        Instruction(false),
        Function(true),
        Builtin(true);

        private boolean functionMode;
        private CompileMode(boolean functionMode){
            this.functionMode = functionMode;
        }
        public boolean isFunctionMode(){
            return functionMode;
        }
    };

    static void parseOption(String[] args) {
        for (int i = 0; i < args.length; ) {
            String opt = args[i++];
            if (opt.equals("-d")) {
                dataTypeDefFile = args[i++];
            } else if (opt.equals("-g")) {
                vmdlGrammarFile = args[i++];
            } else if (opt.equals("-o")) {
                operandSpecFile = args[i++];
            } else if (opt.equals("-no-match-opt")) {
                option.mDisableMatchOptimisation = true;
            } else if(opt.matches("-T.")){
                Integer num = Integer.parseInt(opt.substring(2));
                if((num <= 0) || (num > TypeCheckVisitor.TypeCheckPolicy.values().length)){
                    ErrorPrinter.error("Type analysis option must be T1 to T3");
                }
                typeMapIndex = num;
            } else if (opt.equals("-preprocess")) {
                if(behaviorMode != BehaviorMode.Compile){
                    ErrorPrinter.error("Can not use \"-gen-funcspec\" and \"-preprocess\" option simultaneously");
                }
                behaviorMode = BehaviorMode.Preprocess;
            } else if (opt.equals("-write-fi")) {
                inlineExpansionWriteFile = args[i++];
            } else if (opt.equals("-write-ftd")) {
                functionDependencyFile = args[i++];
            } else if (opt.equals("-write-extern")) {
                functionExternFile = args[i++];
            } else if (opt.equals("-func-inline-opt")) {
                inlineExpansionFile = args[i++];
            } else if (opt.equals("-gen-funcspec")) {
                functionDependencyFile = args[i++];
                operandSpecFile = args[i++];
                if(behaviorMode != BehaviorMode.Compile){
                    ErrorPrinter.error("Can not use \"-gen-funcspec\" and \"-preprocess\" option simultaneously");
                }
                behaviorMode = BehaviorMode.GenFuncSpec;
            } else if (opt.equals("-update-funcspec")) {
                functionSpecFile = args[i++];
            } else if (opt.equals("-case-split")) {
                doCaseSplit = true;
                insnCallSpecFile = args[i++];
            } else if (opt.equals("-i")) {
                insnDefFile = args[i++];
            } else if (opt.startsWith("-X")) {
                i = option.addOption(opt, args, i);
                if (i == -1) {
                    break;
                }
            } else {
                sourceFile = opt;
                break;
            }
        }

        if ((dataTypeDefFile == null || sourceFile == null) && behaviorMode != BehaviorMode.GenFuncSpec) {
            System.out.println("vmdlc [option] source");
            System.out.println("   -d file                           [mandatory] datatype specification file");
            System.out.println("   -o file                           operand specification file");
            System.out.println("   -g file                           Nez grammar file (default: ejsdl.nez in jar file)");
            System.out.println("   -no-match-opt                     disable optimisation for match statement");
            System.out.println("   -i file                           instruction defs");
            System.out.println("   -TX                               type analysis option");
            System.out.println("                                       -T1: use Lub");
            System.out.println("                                       -T2: partly detail");
            System.out.println("                                       -T3: perfectly detail");
            System.out.println("   -preprocess                       Use preprocess mode");
            System.out.println("   -gen-funcspec                     ftdfile file Use generate function spec mode");
            System.out.println("   -func-inline-opt                  file Enable function-inline-expansion");
            System.out.println("   -write-fi file                    Generate function-inline-expansion file");
            System.out.println("                                     (Use with preprocess mode)");
            System.out.println("   -write-ftd file                   Generate function-type-dependency file");
            System.out.println("                                     (Use with preprocess mode)");
            System.out.println("   -write-extrn file                 Append extern declaration of function to file");
            System.out.println("                                     (Use with preprocess mode)");
            System.out.println("   -update-funcspec file             Append funcspec file");
            System.out.println("   -Xcmp:verify_diagram [true|false]");
            System.out.println("   -Xcmp:opt_pass [MR:S]");
            System.out.println("   -Xcmp:rand_seed n                 set random seed of dispatch processor");
            System.out.println("   -Xcmp:tree_layer p0:p1:h0:h1");
            System.out.println("   -Xgen:use_goto [true|false]");
            System.out.println("   -Xgen:pad_cases [true|false]");
            System.out.println("   -Xgen:use_default [true|false]");
            System.out.println("   -Xgen:magic_comment [true|false]");
            System.out.println("   -Xgen:debug_comment [true|false]");
            System.out.println("   -Xgen:label_prefix xxx            set xxx as goto label");
            System.out.println("   -Xgen:type_label [true|false]");
            System.exit(1);
        }
    }

    static Grammar getGrammar() throws IOException {
        ParserGenerator pg = new ParserGenerator();
        Grammar grammar;
        if (vmdlGrammarFile != null)
            grammar = pg.loadGrammar(vmdlGrammarFile);
        else {
            StringSource grammarText = readDefaultGrammar();
            grammar = pg.newGrammar(grammarText, "nez");
        }
        return grammar;
    }

    static SyntaxTree parse(String sourceFile) throws IOException {
        Grammar grammar = getGrammar();
        Parser parser = grammar.newParser(ParserStrategy.newSafeStrategy());
        Source source = new FileSource(sourceFile);
        SyntaxTree ast = (SyntaxTree) parser.parse(source, new SyntaxTree());
        if (parser.hasErrors()) {
            for (SourceError e: parser.getErrors()) {
                System.err.println(e);
            }
            System.exit(-1);
        }
        return ast;
    }

    public final static void main(String[] args) throws IOException {
        parseOption(args);

        OperandSpecifications opSpec = new OperandSpecifications();
        if (operandSpecFile != null)
            opSpec.load(operandSpecFile);

        if(behaviorMode == BehaviorMode.GenFuncSpec){
            TypeDependencyProcessor.load(functionDependencyFile);
            OperandSpecifications argSpec = TypeDependencyProcessor.getExpandSpecifications(opSpec);
            argSpec.write(sourceFile);
            return;
        }

        if (dataTypeDefFile == null)
            ErrorPrinter.error("no datatype definition file is specified (-d option)");
        TypeDefinition.load(dataTypeDefFile);

        InstructionDefinitions insnDef = new InstructionDefinitions();
        if (insnDefFile != null)
            insnDef.load(insnDefFile);

        OperandSpecifications funcSpec = null;
        if (functionSpecFile != null){
            funcSpec = new OperandSpecifications();
            funcSpec.load(functionSpecFile);
        }

        OperandSpecifications insnCallSpec = null;
        if(doCaseSplit){
            insnCallSpec = new OperandSpecifications();
            insnCallSpec.load(insnCallSpecFile);
        }
        
        if (sourceFile == null)
            ErrorPrinter.error("no source file is specified");

        Integer seed = option.getOption(Option.AvailableOptions.CMP_RAND_SEED, 0);
        DispatchProcessor.srand(seed);

        SyntaxTree ast = parse(sourceFile);
        ErrorPrinter.setSource(sourceFile);
        
        if(inlineExpansionFile != null){
            InlineFileProcessor.read(inlineExpansionFile, getGrammar());
        }
        String functionName = new ExternProcessVisitor().start(ast);
        if(behaviorMode == BehaviorMode.Compile){
            if(FunctionTable.hasAnnotations(functionName, FunctionAnnotation.vmInstruction)){
                compileMode = CompileMode.Instruction;
            }else if(FunctionTable.hasAnnotations(functionName, FunctionAnnotation.builtinFunction)){
                compileMode = CompileMode.Builtin;
            }else{
                compileMode = CompileMode.Function;
            }
        }
        new DesugarVisitor().start(ast);
        if(compileMode == CompileMode.Instruction){
            new AlphaConvVisitor().start(ast, true, insnDef);
        }
        new DispatchVarCheckVisitor().start(ast);
        TypeCheckVisitor.TypeCheckOption typeCheckOption = new TypeCheckVisitor.TypeCheckOption()
            .setOperandSpec(opSpec).setFunctionSpec(funcSpec).setCaseSplitSpec(insnCallSpec)
            .setTypeCheckPolicy(TypeCheckVisitor.TypeCheckPolicy.values()[typeMapIndex-1])
            .setFunctionIniningFlag((inlineExpansionFile != null))
            .setUpdateFTDFlag((functionDependencyFile != null))
            .setCaseSplitFlag(doCaseSplit)
            .setCompileMode(compileMode);
        new TypeCheckVisitor().start(ast, typeCheckOption);
        if(behaviorMode == BehaviorMode.Preprocess){
            try{
                if(inlineExpansionWriteFile != null){
                    FileWriter fiWriter = new FileWriter(inlineExpansionWriteFile, true);
                    fiWriter.write(new InlineInfoVisitor().start(ast));
                    fiWriter.close();
                }
                if(functionExternFile != null){
                    FileWriter externWriter = new FileWriter(functionExternFile, true);
                    externWriter.write(new ExternDeclarationGenerator().generate(ast));
                    externWriter.close();
                }
                if(functionDependencyFile != null){
                    TypeDependencyProcessor.write(functionDependencyFile);
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return;
        }
        ControlFlowGraphNode enter = new ControlFlowGraphConstructVisitor().start(ast);
        new VarInitCheckVisitor().start(enter);
        new TriggerGCCheckVisitor().start(ControlFlowGraphNode.exit, compileMode);
        // For Test
        //ControlFlowGraphPrinter.print(enter);
        String program = new AstToCVisitor().start(ast, opSpec, compileMode);
        if(funcSpec != null){
            funcSpec.write(functionSpecFile);
        }
        System.out.println(program);
    }

    public static BufferedReader openFileInJar(String path){
        InputStream is = Main.class.getClassLoader().getResourceAsStream(path);
        return new BufferedReader(new InputStreamReader(is));
    }

    static StringSource readDefaultGrammar() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream(VMDL_GRAMMAR);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader r = new BufferedReader(isr);
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        String peg = sb.toString();
        return new StringSource(peg);
    }
}
