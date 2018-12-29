package vmdlc;
import java.io.IOException;

import nez.ParserGenerator;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
//import nez.parser.io.StringSource;
import nez.parser.io.FileSource;
import nez.ast.Source;
import nez.ast.SourceError;
import nez.util.ConsoleUtils;


import dispatch.*;
import type.*;
import vmdlc.AlphaConvVisitor;
import vmdlc.AstToCVisitor;
import vmdlc.DesugarVisitor;
import vmdlc.SyntaxTree;
import vmdlc.TypeCheckVisitor;

public class Main {
    static final String VMDL_GRAMMAR = "ejsdl.nez";
    static String sourceFile;
    static String dataTypeDefFile;
    static String vmdlGrammarFile = VMDL_GRAMMAR;
    
    static void parseOption(String[] args) {
        for (int i = 0; i < args.length; ) {
            String opt = args[i++];
            if (opt.equals("-d"))
                dataTypeDefFile = args[i++];
            else if (opt.equals("-g"))
                vmdlGrammarFile = args[i++];
            else {
                sourceFile = opt;
                break;
            }
        }
        
        if (dataTypeDefFile == null || sourceFile == null) {
            System.out.println("vmdlc [option] source");
            System.out.println("   -d file   [mandatory] datatype specification file");
            System.out.println("   -g file   Nez grammar file (default: ejsdl.nez)");
            System.exit(1);
        }
    }
    
    static SyntaxTree parse(String sourceFile) throws IOException {
        ParserGenerator pg = new ParserGenerator();
        Grammar grammar = pg.loadGrammar(vmdlGrammarFile);
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
        
        if (sourceFile == null)
            throw new Error("no source file is specified");
        SyntaxTree ast = parse(sourceFile);
        
        new DesugarVisitor().start(ast);
        new AlphaConvVisitor().start(ast, true);
        new TypeCheckVisitor().start(ast);
        String program = new AstToCVisitor().start(ast);
        
        System.out.println(program);
    }
}
