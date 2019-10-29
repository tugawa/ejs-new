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
    static String insnDefFile;
    static int typeMapIndex = 1;

    static Option option = new Option();

    static final TypeMapBase[] TYPE_MAPS = {
        new TypeMapLub(),
        new TypeMapHalf(),
        new TypeMapFull()
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
                if((num <= 0) || (num > TYPE_MAPS.length)){
                    throw new Error("Illigal option");
                }
                typeMapIndex = num;
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

        if (dataTypeDefFile == null || sourceFile == null) {
            System.out.println("vmdlc [option] source");
            System.out.println("   -d file   [mandatory] datatype specification file");
            System.out.println("   -o file   operand specification file");
            System.out.println("   -g file   Nez grammar file (default: ejsdl.nez in jar file)");
            System.out.println("   -no-match-opt  disable optimisation for match statement");
            System.out.println("   -i file   instruction defs");
            System.out.println("   -TX       type analysis processing");
            System.out.println("              -T1: use Lub");
            System.out.println("              -T2: partly detail");
            System.out.println("              -T3: perfectly detail");
            System.out.println("   -Xcmp:verify_diagram [true|false]");
            System.out.println("   -Xcmp:opt_pass [MR:S]");
            System.out.println("   -Xcmp:rand_seed n    set random seed of dispatch processor");
            System.out.println("   -Xcmp:tree_layer p0:p1:h0:h1");
            System.out.println("   -Xgen:use_goto [true|false]");
            System.out.println("   -Xgen:pad_cases [true|false]");
            System.out.println("   -Xgen:use_default [true|false]");
            System.out.println("   -Xgen:magic_comment [true|false]");
            System.out.println("   -Xgen:debug_comment [true|false]");
            System.out.println("   -Xgen:label_prefix xxx   set xxx as goto label");
            System.out.println("   -Xgen:type_label [true|false]");
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
        if (operandSpecFile != null)
            opSpec.load(operandSpecFile);

        InstructionDefinitions insnDef = new InstructionDefinitions();
        if (insnDefFile != null)
            insnDef.load(insnDefFile);

        if (sourceFile == null)
            throw new Error("no source file is specified");

        Integer seed = option.getOption(Option.AvailableOptions.CMP_RAND_SEED, 0);
        DispatchProcessor.srand(seed);

        SyntaxTree ast = parse(sourceFile);

        new DesugarVisitor().start(ast);
        new DispatchVarCheckVisitor().start(ast);
        new AlphaConvVisitor().start(ast, true, insnDef);
        new TypeCheckVisitor().start(ast, opSpec, TYPE_MAPS[typeMapIndex-1]);

        String program = new AstToCVisitor().start(ast, opSpec);

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
