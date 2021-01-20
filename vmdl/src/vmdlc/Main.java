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

import vmdlc.Option.CompileMode;

public class Main {
    static final String VMDL_GRAMMAR = "ejsdsl.nez";

    static SyntaxTree ast;
    static Option option = new Option();

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
    static Grammar getGrammar() throws IOException {
        ParserGenerator pg = new ParserGenerator();
        Grammar grammar;
        if (option.isSetVMDLGrammarFile())
            grammar = pg.loadGrammar(option.getVMDLGrammarFile());
        else {
            StringSource grammarText = readDefaultGrammar();
            grammar = pg.newGrammar(grammarText, "nez");
        }
        return grammar;
    }
    public static BufferedReader openFileInJar(String path){
        InputStream is = Main.class.getClassLoader().getResourceAsStream(path);
        return new BufferedReader(new InputStreamReader(is));
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
    private final static void writeProfiledData(SyntaxTree ast) throws IOException{
        if(option.isSetInliningFile()){
            FileWriter fiWriter = new FileWriter(option.getInliningFile(), true);
            fiWriter.write(new InlineInfoVisitor().start(ast));
            fiWriter.close();
        }
        if(option.isSetFunctionExternFile()){
            FileWriter externWriter = new FileWriter(option.getFunctionExternFile(), true);
            externWriter.write(ExternDeclarationGenerator.generate(ast));
            externWriter.close();
        }
        if(option.isSetOpSpecCRequireFile()){
            FileWriter opSpecCRequireWriter = new FileWriter(option.getOpSpecCRequireFile(), true);
            opSpecCRequireWriter.write(ExternDeclarationGenerator.genereteOperandSpecCRequire(ast));
            opSpecCRequireWriter.close();
        }
        if(option.isSetFunctionTypeDependencyFile()){
            TypeDependencyProcessor.write(option.getFunctionTypeDependencyFile());
        }
    }
    private final static void optionCheck(){
        if (!option.isSetTypeDefinition())
            ErrorPrinter.error("no datatype definition file is specified (-d option)");
        if (!option.isSetSourceFile())
            ErrorPrinter.error("no source file is specified");
    }
    private final static void initialize() throws IOException{
        Integer seed = option.getXOption().getOption(XOption.AvailableOptions.CMP_RAND_SEED, 0);
        DispatchProcessor.srand(seed);
        String sourceFile = option.getSourceFile();
        ast = parse(sourceFile);
        ErrorPrinter.setSource(sourceFile);
        if(option.isEnableFunctionInlining()){
            option.setFunctionInlining(getGrammar());
        }
        if(option.isSetFunctionTypeDependencyFile()){
            TypeDependencyProcessor.load(option.getFunctionTypeDependencyFile());
        }
        String functionName = new ExternProcessVisitor().start(ast);
        option.setCompileMode(functionName);
    }
    private final static void preprocess(){
        new DesugarVisitor().start(ast);
        if(option.getCompileMode() == CompileMode.Instruction){
            new AlphaConvVisitor().start(ast, true, option.getInstructionDefinitions());
        }
        new DispatchVarCheckVisitor().start(ast);
    }
    private final static void typeCheck(){
        new TypeCheckVisitor().start(ast, option.getTypeCheckOption());
    }
    private final static void dataFlowAnalysis(){
        ControlFlowGraphNode enter = new ControlFlowGraphConstructVisitor().start(ast);
        new VarInitCheckVisitor().start(enter);
        new TriggerGCCheckVisitor().start(ControlFlowGraphNode.exit, option.getCompileMode());
        /* For Test */
        //ControlFlowGraphPrinter.print(enter);
    }
    private final static String generateCode(){
        return new AstToCVisitor().start(ast, option.getArgumentSpec(), option.getCompileMode());
    }
    private final static void updateRequiringFunctionSpec() throws IOException{
        if(option.isRequiredUpdatingFunctionSpec())
            option.getRequiringFunctionSpec().write(option.getRequiringFunctionSpecFile());
    }
    private final static void generateRequiringFunctionSpec() throws IOException{
        TypeDependencyProcessor.load(option.getFunctionTypeDependencyFile());
        OperandSpecifications argSpec = TypeDependencyProcessor.getExpandSpecifications(option.getArgumentSpec());
        argSpec.write(option.getSourceFile());
    }
    private final static void generateMergedFunctionSpec(){
        OperandSpecifications merged = OperandSpecifications.merge(option.getMergeTargets());
        merged.print(System.out);
    }
    private final static void compileMode() throws IOException{
        optionCheck();
        initialize();
        preprocess();
        typeCheck();
        updateRequiringFunctionSpec();
        dataFlowAnalysis();
        String program = generateCode();
        System.out.println(program);
    }
    private final static void preprocessMode() throws IOException{
        optionCheck();
        initialize();
        preprocess();
        typeCheck();
        writeProfiledData(ast);
    }
    private final static void genFuncSpecMode() throws IOException{
        generateRequiringFunctionSpec();
    }
    private final static void mergeFunctionSpecMode() throws IOException{
        generateMergedFunctionSpec();
    }
    public final static void main(String[] args) throws IOException {
        option.parseOption(args);
        switch(option.getProcessMode()){
            case GenFuncSpec:
                genFuncSpecMode();
                break;
            case Preprocess:
                preprocessMode();
                break;
            case Compile:
                compileMode();
                break;
            case MergeFuncSpec:
                mergeFunctionSpecMode();
                break;
            default:
                throw new Error("InternalError: Unknown mode: "+option.getProcessMode());
        }
    }
}
