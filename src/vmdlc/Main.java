package vmdlc;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import dispatch.DispatchProcessor;
import nez.ParserGenerator;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
//import nez.parser.io.StringSource;
import nez.parser.io.FileSource;
import nez.parser.io.StringSource;
import nez.ast.Source;
import nez.ast.SourceError;


import type.*;
import vmdlc.AlphaConvVisitor;
import vmdlc.AstToCVisitor;
import vmdlc.DesugarVisitor;
import vmdlc.SyntaxTree;
import vmdlc.TypeCheckVisitor;

public class Main {
    static final String VMDL_GRAMMAR = "ejsdsl.nez";
    static String sourceFile;
    static String dataTypeDefFile;
    static String vmdlGrammarFile;
    static String operandSpecFile;
    
    static void parseOption(String[] args) {
        for (int i = 0; i < args.length; ) {
            String opt = args[i++];
            if (opt.equals("-d"))
                dataTypeDefFile = args[i++];
            else if (opt.equals("-g"))
                vmdlGrammarFile = args[i++];
            else if (opt.equals("-o"))
                operandSpecFile = args[i++];
            else if (opt.equals("-no-match-opt"))
                Option.mDisableMatchOptimisation = true;
            else if (opt.equals("-r")) {
                int seed = Integer.parseInt(args[i++]);
                DispatchProcessor.srand(seed);
            } else {
                sourceFile = opt;
                break;
            }
        }
        
        if (dataTypeDefFile == null || sourceFile == null) {
            System.out.println("vmdlc [option] source");
            System.out.println("   -d file   [mandatory] datatype specification file");
            System.out.println("   -o file   operand specification file");
            System.out.println("   -g file   Nez grammar file (default: ejsdl.nez in jar file)");
            System.out.println("   -no-match-opt  disable optimisation for match statement");
            System.out.println("   -r n      set random seed of dispatch processor");
            System.exit(1);
        }
    }
    
       
    static SyntaxTree parse(String sourceFile) throws IOException {
        ParserGenerator pg = new ParserGenerator();
        Grammar grammar = null;
        
        if (vmdlGrammarFile != null)
            grammar = pg.loadGrammar(vmdlGrammarFile);
        else {
            StringSource grammarText = readDefaultGrammar();
            grammar = pg.newGrammar(grammarText, "nez");
        }

        //grammar.dump();
        Parser parser = grammar.newParser(ParserStrategy.newSafeStrategy());

        //Source source = new StringSource("externC constant cint aaa = \"-1\";");
        Source source = new FileSource(sourceFile);
        SyntaxTree ast = (SyntaxTree) parser.parse(source, new SyntaxTree());

        if (parser.hasErrors()) {
            for (SourceError e: parser.getErrors()) {
                System.out.println(e);
            }
            throw new Error("parse error");
        }

        return ast;
    }
    
    public final static void main(String[] args) throws IOException {
        parseOption(args);
        
        if (dataTypeDefFile == null)
            throw new Error("no datatype definition file is specified (-d option)");
        TypeDefinition.load(dataTypeDefFile);
        
        OperandSpecifications opSpec = new OperandSpecifications();
        if (operandSpecFile != null) {
            System.err.println("operand specification file :"+operandSpecFile);
            opSpec.load(operandSpecFile);
        }
        
        if (sourceFile == null)
            throw new Error("no source file is specified");
        SyntaxTree ast = parse(sourceFile);
        
        new DesugarVisitor().start(ast);
        new AlphaConvVisitor().start(ast, true);
        new TypeCheckVisitor().start(ast, opSpec);
        String program = new AstToCVisitor().start(ast);
        
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
